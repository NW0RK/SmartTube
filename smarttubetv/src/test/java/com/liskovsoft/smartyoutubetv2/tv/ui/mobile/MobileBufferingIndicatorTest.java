package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.google.android.exoplayer2.Player;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class MobileBufferingIndicatorTest {
    @Test
    public void backgroundLoadingAndBriefStallsNeverCoverPlayingVideo() {
        View view = new View(RuntimeEnvironment.getApplication());
        MobileBufferingIndicator indicator =
                new MobileBufferingIndicator(view, new Handler(Looper.getMainLooper()));
        indicator.update(Player.STATE_READY, true);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
        assertEquals(View.GONE, view.getVisibility());
        indicator.update(Player.STATE_BUFFERING, false);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));
        indicator.update(Player.STATE_READY, true);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(View.GONE, view.getVisibility());
    }

    @Test
    public void prolongedStallShowsOnceAndHidesImmediatelyOnRecovery() {
        View view = new View(RuntimeEnvironment.getApplication());
        MobileBufferingIndicator indicator =
                new MobileBufferingIndicator(view, new Handler(Looper.getMainLooper()));
        indicator.update(Player.STATE_BUFFERING, false);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));
        indicator.update(Player.STATE_BUFFERING, false);
        assertEquals(View.GONE, view.getVisibility());
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250));
        assertEquals(View.VISIBLE, view.getVisibility());
        indicator.update(Player.STATE_READY, false);
        assertEquals(View.GONE, view.getVisibility());
    }

    @Test
    public void initialLoadIsDelayedAndTeardownCancelsPendingIndicator() {
        View view = new View(RuntimeEnvironment.getApplication());
        MobileBufferingIndicator indicator =
                new MobileBufferingIndicator(view, new Handler(Looper.getMainLooper()));
        indicator.update(Player.STATE_IDLE, true);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(750));
        assertEquals(View.VISIBLE, view.getVisibility());
        indicator.update(Player.STATE_ENDED, true);
        assertEquals(View.GONE, view.getVisibility());
        indicator.update(Player.STATE_BUFFERING, false);
        indicator.release();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(View.GONE, view.getVisibility());
    }
}
