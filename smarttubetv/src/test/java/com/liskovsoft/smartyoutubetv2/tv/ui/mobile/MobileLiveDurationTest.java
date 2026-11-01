package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class MobileLiveDurationTest {
    @Test public void missingStartTimeLeavesDurationUnknown() {
        assertEquals(-1, new Video().getLiveDurationMs());
    }

    @Test public void scheduledLiveStreamLeavesDurationUnknown() {
        Video video = new Video();
        video.isLive = true;
        video.startTimeMs = System.currentTimeMillis() + 60_000;
        assertEquals(-1, video.getLiveDurationMs());
    }

    @Test public void activeLiveStreamUsesElapsedTime() {
        Video video = new Video();
        video.isLive = true;
        video.startTimeMs = System.currentTimeMillis() - 10_000;
        long duration = video.getLiveDurationMs();
        assertTrue(duration >= 10_000);
        assertTrue(duration <= System.currentTimeMillis() - video.startTimeMs);
    }

    @Test public void endedLiveStreamPrefersActualDuration() {
        Video video = new Video();
        video.startTimeMs = System.currentTimeMillis() - 60_000;
        ReflectionHelpers.setField(video, "durationMs", 45_000L);
        ReflectionHelpers.setField(video, "liveDurationMs", 60_000L);
        assertEquals(45_000, video.getLiveDurationMs());
    }

    @Test public void endedLiveStreamRetainsLastKnownDurationUntilMetadataArrives() {
        Video video = new Video();
        video.startTimeMs = System.currentTimeMillis() - 60_000;
        ReflectionHelpers.setField(video, "liveDurationMs", 60_000L);
        assertEquals(60_000, video.getLiveDurationMs());
    }
}
