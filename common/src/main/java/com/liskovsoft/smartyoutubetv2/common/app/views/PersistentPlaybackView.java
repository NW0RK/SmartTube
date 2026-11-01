package com.liskovsoft.smartyoutubetv2.common.app.views;

import android.app.Activity;

/** A service-owned player remains valid when no watch Activity exists. */
public interface PersistentPlaybackView extends PlaybackView {
    Activity getPlaybackActivity();
    boolean isPlaybackUiVisible();
}
