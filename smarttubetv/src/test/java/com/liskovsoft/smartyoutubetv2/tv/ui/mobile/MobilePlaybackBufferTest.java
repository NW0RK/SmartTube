package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Context;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class MobilePlaybackBufferTest {
    @Test
    public void phoneStartsAtOneSecondButKeepsFiveSecondStallRecoveryForEveryBufferPreference()
            throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        PlayerData data = PlayerData.instance(context);
        int savedBuffer = data.getVideoBufferType();
        try {
            for (int buffer : new int[] {PlayerData.BUFFER_LOW, PlayerData.BUFFER_MEDIUM,
                    PlayerData.BUFFER_HIGH, PlayerData.BUFFER_HIGHEST}) {
                data.setVideoBufferType(buffer);
                assertThresholds(context, true);
                assertEquals(buffer, data.getVideoBufferType());
            }
        } finally {
            data.setVideoBufferType(savedBuffer);
        }
    }

    @Test
    public void existingInitializerStillWaitsForTwoAndAHalfSeconds() throws Exception {
        assertThresholds(RuntimeEnvironment.getApplication(), false);
    }

    private void assertThresholds(Context context, boolean phone) throws Exception {
        ExoPlayerInitializer initializer = new ExoPlayerInitializer(context);
        DefaultRenderersFactory renderers = new DefaultRenderersFactory(context);
        DefaultTrackSelector selector = new DefaultTrackSelector();
        SimpleExoPlayer player = phone
                ? initializer.createPlayer(context, renderers, selector, 1_000)
                : initializer.createPlayer(context, renderers, selector);
        try {
            // Inspect the load control actually wired into the created player's playback thread.
            LoadControl control = (LoadControl) field(field(field(player, "player"),
                    "internalPlayer"), "loadControl");
            // Track selection establishes the byte target; preparation alone leaves it at zero.
            Renderer renderer =
                    (Renderer) Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[] {Renderer.class},
                            (proxy, method, args) -> C.TRACK_TYPE_VIDEO);
            TrackGroup group =
                    new TrackGroup(
                            Format.createVideoSampleFormat(
                                    "avc", "video/avc", null, 4_000_000, -1,
                                    1920, 1080, 30, null, null));
            control.onTracksSelected(new Renderer[] {renderer}, new TrackGroupArray(group),
                    new TrackSelectionArray(new FixedTrackSelection(group, 0)));
            long startupUs = phone ? 1_000_000 : 2_500_000;
            assertFalse(control.shouldStartPlayback(startupUs - 1, 1f, false));
            assertTrue(control.shouldStartPlayback(startupUs, 1f, false));
            assertFalse(control.shouldStartPlayback(4_999_999, 1f, true));
            assertTrue(control.shouldStartPlayback(5_000_000, 1f, true));
            assertFalse(control.shouldStartPlayback(startupUs * 2 - 1_000, 2f, false));
            assertTrue(control.shouldStartPlayback(startupUs * 2, 2f, false));
            assertEquals(SeekParameters.EXACT, player.getSeekParameters());
        } finally {
            player.release();
            initializer.release();
        }
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }
}
