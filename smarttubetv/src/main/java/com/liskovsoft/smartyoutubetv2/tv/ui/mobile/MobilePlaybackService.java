package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.os.*;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.core.app.NotificationCompat;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Owns the phone player independently of the watch Activity and its surface. */
public class MobilePlaybackService extends Service {
    private static final String CHANNEL = "mobile_playback";
    private static final int NOTIFICATION_ID = 1002;
    private static MobilePlaybackService current;
    private MobilePlaybackSession playback;
    private PowerManager.WakeLock wakeLock;
    private MediaSessionCompat.Token token;
    private boolean active, started;
    private final LocalBinder binder = new LocalBinder();
    private final BroadcastReceiver noisy = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (playback != null) playback.setPlayWhenReady(false);
        }
    };

    public final class LocalBinder extends Binder {
        MobilePlaybackSession session() {
            if (playback == null) playback = new MobilePlaybackSession(MobilePlaybackService.this);
            return playback;
        }
    }

    static boolean hasSession() {
        return current != null && current.playback != null && current.playback.isEngineInitialized()
                && current.playback.getVideo() != null;
    }

    @Override public void onCreate() {
        super.onCreate();
        current = this;
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(
                CHANNEL, getString(R.string.category_background_playback), NotificationManager.IMPORTANCE_LOW));
        wakeLock = getSystemService(PowerManager.class).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":mobile-playback");
        wakeLock.setReferenceCounted(false);
        registerReceiver(noisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    @Override public IBinder onBind(Intent intent) { return binder; }

    void updateProtection(boolean playingOrBuffering, MediaSessionCompat.Token sessionToken) {
        active = playingOrBuffering;
        token = sessionToken;
        if (active && !started) {
            startForegroundService(new Intent(this, MobilePlaybackService.class));
            started = true;
        }
        if (active && !wakeLock.isHeld()) wakeLock.acquire();
        if (!active && wakeLock.isHeld()) wakeLock.release();
        if (started) startForeground(NOTIFICATION_ID, notification());
    }

    private PendingIntent command(String action) {
        return PendingIntent.getService(this, action.hashCode(),
                new Intent(this, MobilePlaybackService.class).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    void refreshNotification() {
        if (started) getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification());
    }

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MobilePlaybackActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean playing = playback != null && playback.getPlayWhenReady();
        CharSequence title = playback != null && playback.getVideo() != null
                ? playback.getVideo().getTitleFull() : getApplicationInfo().loadLabel(getPackageManager());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.icon_notification).setContentTitle(title)
                .setContentText(playback != null && playback.getVideo() != null ? playback.getVideo().getAuthor() : null)
                .setContentIntent(open).setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setOnlyAlertOnce(true).setSilent(true).setOngoing(active)
                .setDeleteIntent(command("stop"))
                .addAction(android.R.drawable.ic_media_previous, "Previous", command("previous"))
                .addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        playing ? "Pause" : "Play", command(playing ? "pause" : "play"))
                .addAction(android.R.drawable.ic_media_next, "Next", command("next"))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", command("stop"));
        if (token != null) builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(token).setShowActionsInCompactView(0, 1, 2));
        return builder.build();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (playback == null || !playback.isEngineInitialized()) { stopSelf(); return START_NOT_STICKY; }
        String action = intent == null ? null : intent.getAction();
        if ("stop".equals(action)) { stopPlayback(); return START_NOT_STICKY; }
        if ("pause".equals(action)) playback.setPlayWhenReady(false);
        if ("play".equals(action)) playback.setPlayWhenReady(true);
        if ("previous".equals(action)) playback.mPlaybackPresenter.onPreviousClicked();
        if ("next".equals(action)) playback.mPlaybackPresenter.onNextClicked();
        started = true;
        startForeground(NOTIFICATION_ID, notification());
        return START_NOT_STICKY;
    }

    void stopPlayback() {
        MobilePlaybackSession old = playback;
        android.app.Activity activity = old == null ? null : old.getPlaybackActivity();
        playback = null;
        if (old != null) old.close();
        active = started = false;
        if (wakeLock.isHeld()) wakeLock.release();
        stopForeground(true);
        stopSelf();
        if (activity != null && !activity.isFinishing()) activity.finish();
    }

    @Override public void onTaskRemoved(Intent rootIntent) { stopPlayback(); }

    @Override public void onDestroy() {
        if (playback != null) { playback.close(); playback = null; }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        unregisterReceiver(noisy);
        stopForeground(true);
        if (current == this) current = null;
        super.onDestroy();
    }
}
