package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Rational;
import android.view.*;
import android.widget.FrameLayout;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerConstants;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

public class MobilePlaybackActivity extends MobileActivity {
    private MobilePlaybackFragment fragment;
    private OrientationEventListener orientationListener;
    private boolean portraitLocked;
    private boolean forcedFullscreen, stopped;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setId(com.liskovsoft.smartyoutubetv2.tv.R.id.mobile_watch_container);
        setContentView(root);
        fragment =
                (MobilePlaybackFragment)
                        getSupportFragmentManager().findFragmentByTag("mobile-watch");
        if (fragment == null) {
            fragment = new MobilePlaybackFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(root.getId(), fragment, "mobile-watch")
                    .commitNow();
        }
        if (state != null) forcedFullscreen = state.getBoolean("fullscreen");
        orientationListener =
                new OrientationEventListener(this) {
                    @Override
                    public void onOrientationChanged(int angle) {
                        if (portraitLocked
                                && angle != ORIENTATION_UNKNOWN
                                && (angle < 30 || angle > 330)) {
                            portraitLocked = false;
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                        }
                    }
                };
        if (orientationListener.canDetectOrientation()) orientationListener.enable();
        updateWindow();
    }

    public boolean isFullscreen() {
        return forcedFullscreen
                || getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE;
    }

    public void toggleFullscreen() {
        if (isFullscreen()) exitFullscreen();
        else {
            portraitLocked = false;
            forcedFullscreen = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            updateWindow();
        }
    }

    public void exitFullscreen() {
        forcedFullscreen = false;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            portraitLocked = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        updateWindow();
    }

    private void updateWindow() {
        boolean full = isFullscreen() || isInPictureInPictureMode();
        getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        full
                                ? View.SYSTEM_UI_FLAG_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                : 0);
        if (fragment != null) fragment.resize();
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        updateWindow();
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen() && !isInPictureInPictureMode()) exitFullscreen();
        else finish();
    }

    @Override
    public void finish() {
        if (!stopped) {
            stopped = true;
            if (fragment != null) fragment.stopPlayback();
        }
        super.finish();
    }

    public void enterPictureInPicture() {
        if (!getPackageManager()
                .hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE))
            return;
        try {
            enterPictureInPictureMode(
                    new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(16, 9))
                            .build());
        } catch (IllegalStateException | IllegalArgumentException ignored) {
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean pip, Configuration c) {
        super.onPictureInPictureModeChanged(pip, c);
        if (fragment != null) {
            fragment.showControls(!pip);
            fragment.showSubtitles(!pip);
            if (pip) fragment.showDebugInfo(false);
        }
        updateWindow();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isFinishing() || isBackPressed() || getViewManager().isNewViewPending()) return;
        if (PlayerData.instance(this).getBackgroundMode() == PlayerConstants.BACKGROUND_MODE_PIP)
            enterPictureInPicture();
    }

    @Override
    protected void onDestroy() {
        if (orientationListener != null) orientationListener.disable();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("fullscreen", forcedFullscreen);
    }
}
