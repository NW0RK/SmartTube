package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.os.Handler;
import android.view.View;

import com.google.android.exoplayer2.Player;

/** Shows prolonged playback waits, without flashing for brief stalls or background prefetch. */
final class MobileBufferingIndicator {
    private static final long SHOW_DELAY_MS = 750;
    private final View indicator;
    private final Handler handler;
    private boolean waiting;
    private final Runnable show;

    MobileBufferingIndicator(View indicator, Handler handler) {
        this.indicator = indicator;
        this.handler = handler;
        show =
                () -> {
                    if (waiting) this.indicator.setVisibility(View.VISIBLE);
                };
        indicator.setVisibility(View.GONE);
    }

    void update(int playbackState, boolean initialLoading) {
        boolean next =
                playbackState == Player.STATE_BUFFERING
                        || playbackState == Player.STATE_IDLE && initialLoading;
        if (next == waiting) return;
        waiting = next;
        handler.removeCallbacks(show);
        if (waiting) handler.postDelayed(show, SHOW_DELAY_MS);
        else indicator.setVisibility(View.GONE);
    }

    void release() {
        waiting = false;
        handler.removeCallbacks(show);
        indicator.setVisibility(View.GONE);
    }
}
