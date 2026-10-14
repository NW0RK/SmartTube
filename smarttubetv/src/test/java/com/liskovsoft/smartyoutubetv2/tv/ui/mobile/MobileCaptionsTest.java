package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;

import android.app.Application;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class MobileCaptionsTest {
    private final FormatItem english = FormatItem.fromLanguage("en");
    private final FormatItem french = FormatItem.fromLanguage("fr");

    @Test
    public void preparesPreferredTrackOnceWithoutEnablingCaptions() {
        MobileCaptions captions = new MobileCaptions();
        assertSame(french, captions.prepare(Arrays.asList(FormatItem.SUBTITLE_NONE, english, french),
                Collections.singletonList(french)));
        assertFalse(captions.isEnabled());
        assertNull(captions.prepare(Collections.singletonList(english), Collections.emptyList()));
        captions.select(french);
        assertTrue(captions.isEnabled());
        captions.select(FormatItem.SUBTITLE_NONE);
        assertFalse(captions.isEnabled());
        assertNull(captions.prepare(Collections.singletonList(french), Collections.emptyList()));
    }

    @Test
    public void waitsForTracksAndResetsForNextVideo() {
        MobileCaptions captions = new MobileCaptions();
        assertNull(captions.prepare(null, Collections.emptyList()));
        assertNull(captions.prepare(Collections.singletonList(FormatItem.SUBTITLE_NONE), null));
        assertSame(english, captions.prepare(Collections.singletonList(english),
                Collections.singletonList(french)));
        captions.select(english);
        captions.reset();
        assertFalse(captions.isEnabled());
        assertSame(french, captions.prepare(Collections.singletonList(french), Collections.emptyList()));
    }

    @Test
    public void restoredEnabledTrackDoesNotGetReplacedByPreloading() {
        MobileCaptions captions = new MobileCaptions();
        captions.select(french);
        assertNull(captions.prepare(Collections.singletonList(english), Collections.singletonList(english)));
        assertTrue(captions.isEnabled());
    }

    @Test
    public void disablingCaptionsKeepsRendererAndCurrentCueAvailableForImmediateEnable() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        final java.util.List<FormatItem> selections = new java.util.ArrayList<>();
        com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController controller =
                new com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController(context, null) {
                    @Override public void selectFormat(FormatItem format) { selections.add(format); }
                    @Override public FormatItem getSubtitleFormat() { return english; }
                    @Override public java.util.List<FormatItem> getSubtitleFormats() {
                        return Arrays.asList(FormatItem.SUBTITLE_NONE,
                                com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.ExoFormatItem
                                        .fromWithSelection(english.getTrack(), true));
                    }
                };
        MobilePlaybackFragment player = new MobilePlaybackFragment();
        player.mExoPlayerController = controller;
        player.surface = new com.google.android.exoplayer2.ui.PlayerView(context);
        com.google.android.exoplayer2.ui.SubtitleView view = player.surface.getSubtitleView();
        player.setFormat(english);
        assertEquals(android.view.View.VISIBLE, view.getVisibility());
        player.setFormat(FormatItem.SUBTITLE_NONE);
        assertEquals(1, selections.size());
        assertEquals(android.view.View.INVISIBLE, view.getVisibility());
        assertSame(FormatItem.SUBTITLE_NONE, player.getSubtitleFormat());
        assertTrue(player.getSubtitleFormats().get(0).isSelected());
        assertFalse(player.getSubtitleFormats().get(1).isSelected());
        player.showSubtitles(true);
        assertEquals(android.view.View.INVISIBLE, view.getVisibility());
        player.setFormat(english);
        assertEquals(android.view.View.VISIBLE, view.getVisibility());
        assertSame(english, player.getSubtitleFormat());
        player.showSubtitles(false);
        assertEquals(android.view.View.INVISIBLE, view.getVisibility());
        player.showSubtitles(true);
        assertEquals(android.view.View.VISIBLE, view.getVisibility());
    }
}
