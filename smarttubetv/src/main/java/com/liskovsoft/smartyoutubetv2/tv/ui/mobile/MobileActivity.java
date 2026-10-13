package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.os.Bundle;

import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Shared phone window policy; presenter, locale and permission support stays in MotherActivity. */
public abstract class MobileActivity extends MotherActivity {
    private boolean navigationFinish;
    private com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView backgroundPlayer;
    private boolean backgroundPlayWhenReady;
    private String backgroundVideoId;

    @Override
    protected void initTheme() {
        setTheme(R.style.MobileTheme);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        stopWatchForBrowsing();
        getWindow().setStatusBarColor(MobileUi.background(this));
        getWindow().setNavigationBarColor(MobileUi.background(this));
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().getDecorView().setFitsSystemWindows(true);
        getScreensaverManager().suspend();
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopWatchForBrowsing();
        if (backgroundPlayer != null) {
            com.liskovsoft.smartyoutubetv2.common.app.models.data.Video video =
                    backgroundPlayer.getVideo();
            if (backgroundPlayer.isEngineInitialized()
                    && backgroundPlayer
                            == com.liskovsoft.smartyoutubetv2.common.app.presenters
                                    .PlaybackPresenter.instance(this)
                                    .getView()
                    && java.util.Objects.equals(
                            backgroundVideoId, video == null ? null : video.videoId))
                backgroundPlayer.setPlayWhenReady(backgroundPlayWhenReady);
            backgroundPlayer = null;
        }
        getViewManager().addTop(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // The departing top screen can be a menu or account screen above the watch page.
        // Internal navigation replaces the registered top before the previous screen stops.
        if (isFinishing()
                || com.liskovsoft.smartyoutubetv2.common.utils.Utils.isHardScreenOff(this)
                || getViewManager().getTopView() != getViewManager().getView(getClass())
                || com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData.instance(this)
                                .getBackgroundMode()
                        != com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager
                                .PlayerConstants.BACKGROUND_MODE_DEFAULT) return;
        com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView player =
                com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter.instance(
                                this)
                        .getView();
        if (player instanceof MobilePlaybackFragment
                && player.isEngineInitialized()
                && !player.isInPIPMode()) {
            backgroundPlayer = player;
            backgroundPlayWhenReady = player.getPlayWhenReady();
            com.liskovsoft.smartyoutubetv2.common.app.models.data.Video video = player.getVideo();
            backgroundVideoId = video == null ? null : video.videoId;
            player.setPlayWhenReady(false);
        }
    }

    private void stopWatchForBrowsing() {
        if (this instanceof MobileBrowseActivity
                || this instanceof MobileSearchActivity
                || this instanceof MobileChannelActivity
                || this instanceof MobileUploadsActivity) {
            com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView player =
                    com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter.instance(
                                    this)
                            .getView();
            if (player instanceof MobilePlaybackFragment && player.isEngineInitialized()) {
                com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter.instance(
                                this)
                        .closeDialog();
                android.app.Activity activity = ((MobilePlaybackFragment) player).getActivity();
                if (activity instanceof MobileActivity)
                    ((MobileActivity) activity).finishForNavigation();
                else player.finish();
            }
        }
    }

    final void finishForNavigation() {
        navigationFinish = true;
        finish();
    }

    @Override
    public void finish() {
        finishReally();
    }

    @Override
    public void finishReally() {
        if (!navigationFinish
                && isTaskRoot()
                && !(this instanceof MobileBrowseActivity)
                && getViewManager().hasParentView(this)) getViewManager().startParentView(this);
        getViewManager().removeTop(this);
        super.finishReally();
    }
}
