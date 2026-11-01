package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.os.Bundle;

import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Shared phone window policy; presenter, locale and permission support stays in MotherActivity. */
public abstract class MobileActivity extends MotherActivity {
    private boolean navigationFinish;

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
        getViewManager().addTop(this);
    }

    // Playback intent belongs to the user; Home and screen lock never toggle it.
    private void stopWatchForBrowsing() {
        if (this instanceof MobileBrowseActivity
                || this instanceof MobileSearchActivity
                || this instanceof MobileChannelActivity
                || this instanceof MobileUploadsActivity) {
            MobileDialogActivity.dismissComments(this);
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
