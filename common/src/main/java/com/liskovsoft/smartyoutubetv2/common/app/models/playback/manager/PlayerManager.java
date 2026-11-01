package com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

// is paused, position, tracks (audio, video, subs), codecs, aspect, speed
// title, subtitle (description), subscribed/liked nums, published date, toggle buttons, simple buttons
public interface PlayerManager extends PlayerEngine, PlayerUI {
    void setVideo(Video item);
    Video getVideo();
    void finish();
    void finishReally();
    void showBackground(String url);
    void showBackgroundColor(int colorResId);
    void resetPlayerState();
    boolean isEmbed();
    default boolean usesPerVideoQuality() { return false; }
    default void restorePerVideoQuality() { }
    default void selectUserFormat(com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem format) { setFormat(format); }
}
