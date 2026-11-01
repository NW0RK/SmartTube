package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import com.google.android.exoplayer2.Format;
import org.junit.Test;
import static org.junit.Assert.*;

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 28, application = android.app.Application.class)
public class MobileTrackSelectorTest {
    private Format video(String codec, int bitrate, int width, int height, float fps) {
        return Format.createVideoSampleFormat(codec, "video/avc", codec, bitrate, -1, width, height, fps, null, null);
    }
    @Test public void resolutionWinsBeforeSize() {
        assertTrue(MobileTrackSelector.compare(video("avc", 4000000, 1920, 1080, 30), video("avc", 500000, 1280, 720, 30)) < 0);
    }
    @Test public void smallest1080StreamWinsAcrossCodecsAndFrameRates() {
        assertTrue(MobileTrackSelector.compare(video("avc", 1000000, 1920, 1080, 30), video("av01", 2000000, 1920, 1080, 60)) < 0);
    }
    @Test public void missingBitrateIsNotTreatedAsFreeData() {
        assertTrue(MobileTrackSelector.compare(video("avc", 1000000, 1920, 1080, 30), video("av01", -1, 1920, 1080, 30)) < 0);
    }
    @Test public void fallbackPrefersHighestLowerResolutionOverHigherResolution() {
        assertTrue(MobileTrackSelector.compare(video("avc", 1000000, 1280, 720, 30), video("avc", 1000000, 2560, 1440, 30)) < 0);
        assertTrue(MobileTrackSelector.compare(video("avc", 1000000, 1280, 720, 30), video("avc", 1000000, 640, 360, 30)) < 0);
    }
    @Test public void portrait1080MatchesLandscape1080() {
        assertEquals(0, MobileTrackSelector.compare(video("avc", 1000000, 1080, 1920, 30), video("avc", 1000000, 1920, 1080, 30)));
    }
}
