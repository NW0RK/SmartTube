package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;

/** Protects the phone's existing player while playing, including with the display off. */
public class MobilePlaybackService extends Service {
    private static final String CHANNEL = "mobile_playback";
    private static final String SESSION = "session";
    private static final int NOTIFICATION_ID = 1002;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(
                CHANNEL, getString(R.string.category_background_playback),
                NotificationManager.IMPORTANCE_LOW));
        wakeLock = getSystemService(PowerManager.class).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":mobile-playback");
        wakeLock.setReferenceCounted(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PendingIntent openPlayer = PendingIntent.getActivity(this, 0,
                new Intent(this, MobilePlaybackActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.icon_notification)
                .setContentTitle(getApplicationInfo().loadLabel(getPackageManager()))
                .setContentText(getString(R.string.category_background_playback))
                .setContentIntent(openPlayer)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setOngoing(true)
                .setSilent(true);
        MediaSessionCompat.Token token = intent == null ? null : intent.getParcelableExtra(SESSION);
        if (token != null) notification.setStyle(
                new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(token));
        startForeground(NOTIFICATION_ID, notification.build());
        if (!wakeLock.isHeld()) wakeLock.acquire();
        // The Activity owns playback; restarting this service alone cannot restore it.
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static boolean start(Context context, MediaSessionCompat.Token token) {
        try {
            context.getApplicationContext().startForegroundService(
                    new Intent(context, MobilePlaybackService.class).putExtra(SESSION, token));
            return true;
        } catch (IllegalStateException e) {
            // Android can reject a late callback after an external link opens on a locked phone.
            // The player retries when its Activity resumes.
            Log.w("MobilePlaybackService", "Playback service start deferred until resume", e);
            return false;
        }
    }

    static void stop(Context context) {
        context.getApplicationContext().stopService(new Intent(context, MobilePlaybackService.class));
    }
}
