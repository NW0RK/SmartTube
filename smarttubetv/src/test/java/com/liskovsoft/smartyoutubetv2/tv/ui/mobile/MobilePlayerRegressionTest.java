package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Context;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection.Definition;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;
import com.liskovsoft.smartyoutubetv2.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Proxy;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class MobilePlayerRegressionTest {
    @Test
    public void commentsAndRepliesLeaveVideoOutsideWindowAndMenusRestoreModalWindow() {
        org.robolectric.android.controller.ActivityController<MobileDialogActivity> controller =
                org.robolectric.Robolectric.buildActivity(MobileDialogActivity.class).create();
        MobileDialogActivity activity = controller.get();
        com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory comments =
                com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory.from(
                        1, com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory.TYPE_COMMENTS,
                        "Comments", java.util.Collections.emptyList());
        activity.show(java.util.Collections.singletonList(comments), "Comments", false, true, true, 1);
        android.view.WindowManager.LayoutParams window = activity.getWindow().getAttributes();
        int sheetHeight = window.height;
        assertTrue(sheetHeight > 0);
        assertTrue(sheetHeight < activity.getResources().getDisplayMetrics().heightPixels);
        assertEquals(android.view.Gravity.BOTTOM, window.gravity);
        assertTrue((window.flags & android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) != 0);
        activity.show(java.util.Collections.singletonList(comments), "Replies", false, true, true, 2);
        activity.onBackPressed();
        assertEquals(sheetHeight, activity.getWindow().getAttributes().height);
        activity.onConfigurationChanged(activity.getResources().getConfiguration());
        assertEquals(sheetHeight, activity.getWindow().getAttributes().height);

        activity.show(java.util.Collections.emptyList(), "Settings", false, false, false, 3);
        window = activity.getWindow().getAttributes();
        assertEquals(android.view.WindowManager.LayoutParams.MATCH_PARENT, window.height);
        assertEquals(0, window.flags & android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        activity.onBackPressed();
        assertEquals(sheetHeight, activity.getWindow().getAttributes().height);
        controller.destroy();
    }

    @Test
    public void commentsRemainOnVideoTouchAndDismissWhenLeavingPlayerButOrdinaryMenusRemain() {
        org.robolectric.android.controller.ActivityController<MobileDialogActivity> controller =
                org.robolectric.Robolectric.buildActivity(MobileDialogActivity.class).create();
        MobileDialogActivity activity = controller.get();
        activity.show(java.util.Collections.emptyList(), "Settings", false, false, false, 3);
        MobileDialogActivity.dismissComments(activity);
        assertFalse(activity.isFinishing());
        com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory comments =
                com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory.from(
                        1, com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory.TYPE_COMMENTS,
                        "Comments", java.util.Collections.emptyList());
        activity.show(java.util.Collections.singletonList(comments), "Comments", false, true, true, 1);
        assertTrue((activity.getWindow().getAttributes().flags
                & android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH) != 0);
        android.view.MotionEvent outside = android.view.MotionEvent.obtain(
                0, 0, android.view.MotionEvent.ACTION_OUTSIDE, 0, -100, 0);
        activity.onTouchEvent(outside);
        outside.recycle();
        assertFalse(activity.isFinishing());
        activity.clearBackstack();
        activity.onBackPressed();
        assertTrue(activity.isFinishing());
        controller.destroy();
        controller = org.robolectric.Robolectric.buildActivity(MobileDialogActivity.class).create();
        activity = controller.get();
        activity.show(java.util.Collections.singletonList(comments), "Comments", false, true, true, 1);
        MobileDialogActivity.dismissComments(activity);
        assertTrue(activity.isFinishing());
        controller.destroy();
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land-mdpi")
    public void fullscreenCommentsAndRepliesStayOnRightAndRestoreAfterSettings() {
        org.robolectric.android.controller.ActivityController<MobileDialogActivity> controller =
                org.robolectric.Robolectric.buildActivity(MobileDialogActivity.class).create();
        MobileDialogActivity activity = controller.get();
        com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory comments =
                com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory.from(
                        1, com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory.TYPE_COMMENTS,
                        "Comments", java.util.Collections.emptyList());
        activity.show(java.util.Collections.singletonList(comments), "Comments", false, true, true, 1);
        android.view.WindowManager.LayoutParams window = activity.getWindow().getAttributes();
        int panelWidth = window.width;
        assertTrue(panelWidth > 0);
        assertTrue(panelWidth <= 400);
        assertEquals(android.view.Gravity.RIGHT | android.view.Gravity.TOP, window.gravity);
        assertEquals(android.view.WindowManager.LayoutParams.MATCH_PARENT, window.height);
        assertTrue((window.flags & android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL) != 0);
        activity.show(java.util.Collections.singletonList(comments), "Replies", false, true, true, 2);
        activity.onBackPressed();
        assertEquals(panelWidth, activity.getWindow().getAttributes().width);
        activity.show(java.util.Collections.emptyList(), "Settings", false, false, false, 3);
        assertEquals(android.view.WindowManager.LayoutParams.MATCH_PARENT, activity.getWindow().getAttributes().width);
        activity.onBackPressed();
        assertEquals(panelWidth, activity.getWindow().getAttributes().width);
        assertEquals(android.view.Gravity.RIGHT | android.view.Gravity.TOP, activity.getWindow().getAttributes().gravity);
        org.robolectric.RuntimeEnvironment.setQualifiers("w360dp-h800dp-port-mdpi");
        activity.onConfigurationChanged(activity.getResources().getConfiguration());
        assertEquals(android.view.Gravity.BOTTOM, activity.getWindow().getAttributes().gravity);
        assertEquals(android.view.WindowManager.LayoutParams.MATCH_PARENT, activity.getWindow().getAttributes().width);
        assertTrue(activity.getWindow().getAttributes().height > 0);
        controller.destroy();
    }

    @Test
    public void commentsWindowIsTranslucentFromLaunchSoPlayerRemainsVisible() throws Exception {
        Context app = RuntimeEnvironment.getApplication();
        android.content.pm.ActivityInfo info = app.getPackageManager().getActivityInfo(
                new android.content.ComponentName(app, MobileDialogActivity.class), 0);
        assertEquals(R.style.MobileDialogTheme, info.getThemeResource());
        Context themed = new ContextThemeWrapper(app, info.getThemeResource());
        android.content.res.TypedArray window = themed.obtainStyledAttributes(
                new int[] {android.R.attr.windowIsTranslucent, android.R.attr.backgroundDimEnabled});
        assertTrue(window.getBoolean(0, false));
        assertFalse(window.getBoolean(1, true));
        window.recycle();
    }

    @Test
    public void defaultZoomKeepsVideoFullSizeAndExplicitZoomAndFlipStillWork() {
        Context context =
                new ContextThemeWrapper(
                        RuntimeEnvironment.getApplication(),
                        android.R.style.Theme_Material_NoActionBar);
        View stage = LayoutInflater.from(context).inflate(R.layout.mobile_player_stage, null);
        MobilePlaybackFragment fragment = new MobilePlaybackFragment();
        fragment.surface = stage.findViewById(R.id.mobile_player_surface);
        View video = fragment.surface.getVideoSurfaceView();
        assertNotNull(video);
        fragment.setZoomPercents(-1);
        assertEquals(1f, video.getScaleX(), 0f);
        assertEquals(1f, video.getScaleY(), 0f);
        fragment.setZoomPercents(150);
        assertEquals(1.5f, video.getScaleX(), 0f);
        fragment.setVideoFlipEnabled(true);
        assertEquals(-1.5f, video.getScaleX(), 0f);
        fragment.setZoomPercents(-1);
        assertEquals(-1f, video.getScaleX(), 0f);
        assertEquals(1f, video.getScaleY(), 0f);
    }

    @Test
    public void destroyedPlayerIsDetachedBeforeAnotherVideoOpens() {
        MobilePlaybackFragment fragment = new MobilePlaybackFragment();
        fragment.mPlaybackPresenter =
                com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter.instance(
                        RuntimeEnvironment.getApplication());
        fragment.mPlaybackPresenter.setView(fragment);
        assertSame(fragment, fragment.mPlaybackPresenter.getPlayer());
        // Simulates teardown after the Activity is finishing: getView() already returns null.
        assertNull(fragment.mPlaybackPresenter.getView());
        fragment.onDestroy();
        assertNull(fragment.mPlaybackPresenter.getPlayer());
    }

    @Test
    public void oldPlayerTeardownDoesNotDetachReplacementPlayer() {
        MobilePlaybackFragment old = new MobilePlaybackFragment(),
                replacement = new MobilePlaybackFragment();
        old.mPlaybackPresenter =
                com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter.instance(
                        RuntimeEnvironment.getApplication());
        old.mPlaybackPresenter.setView(replacement);
        old.onDestroy();
        assertSame(replacement, old.mPlaybackPresenter.getPlayer());
        old.mPlaybackPresenter.setView(null);
    }

    @Test
    public void lateChannelIconUpdateDoesNotLoadFromDetachedFragment() throws Exception {
        MobilePlaybackFragment fragment = new MobilePlaybackFragment();
        java.lang.reflect.Field avatar = MobilePlaybackFragment.class.getDeclaredField("avatar");
        avatar.setAccessible(true);
        avatar.set(fragment, new android.widget.ImageView(RuntimeEnvironment.getApplication()));
        fragment.setChannelIcon("https://example.invalid/channel.png");
    }

    private static class Selector extends RestoreTrackSelector {
        Selector() {
            super(new AdaptiveTrackSelection.Factory());
            requireSupportedFormats();
        }

        Definition choose(TrackGroupArray groups, int[][] support) throws Exception {
            return selectVideoTrack(groups, support, 0, getParameters(), false);
        }
    }

    @Test
    public void unsupportedSavedFormatFallsBackBeforeDecoderStartupAndUpdatesSelection()
            throws Exception {
        TrackGroup group =
                new TrackGroup(
                        Format.createVideoSampleFormat(
                                "av1",
                                "video/av01",
                                null,
                                20000000,
                                -1,
                                3840,
                                2160,
                                60,
                                null,
                                null),
                        Format.createVideoSampleFormat(
                                "avc", "video/avc", null, 4000000, -1, 1920, 1080, 30, null, null));
        TrackGroupArray groups = new TrackGroupArray(group);
        int[][] support = {
            {RendererCapabilities.FORMAT_UNSUPPORTED_SUBTYPE, RendererCapabilities.FORMAT_HANDLED}
        };
        Selector selector = new Selector();
        Definition[] updated = {null};
        selector.setOnTrackSelectCallback(
                (RestoreTrackSelector.TrackSelectorCallback)
                        Proxy.newProxyInstance(
                                getClass().getClassLoader(),
                                new Class<?>[] {RestoreTrackSelector.TrackSelectorCallback.class},
                                (proxy, method, args) -> {
                                    if (method.getName().equals("onSelectVideoTrack"))
                                        return new Pair<>(new Definition(group, 0), null);
                                    if (method.getName().equals("updateVideoTrackSelection"))
                                        updated[0] = (Definition) args[2];
                                    return null;
                                }));
        Definition selected = selector.choose(groups, support);
        assertArrayEquals(new int[] {1}, selected.tracks);
        assertSame(selected, updated[0]);
        assertFalse(selector.getParameters().exceedRendererCapabilitiesIfNecessary);
    }

    @Test
    public void supportedSavedFormatIsRetainedIncludingCapabilityFlags() throws Exception {
        TrackGroup group =
                new TrackGroup(
                        Format.createVideoSampleFormat(
                                "avc", "video/avc", null, 4000000, -1, 1920, 1080, 30, null, null));
        TrackGroupArray groups = new TrackGroupArray(group);
        Definition saved = new Definition(group, 0);
        Selector selector = new Selector();
        selector.setOnTrackSelectCallback(
                (RestoreTrackSelector.TrackSelectorCallback)
                        Proxy.newProxyInstance(
                                getClass().getClassLoader(),
                                new Class<?>[] {RestoreTrackSelector.TrackSelectorCallback.class},
                                (proxy, method, args) ->
                                        method.getName().equals("onSelectVideoTrack")
                                                ? new Pair<>(saved, null)
                                                : null));
        assertSame(
                saved,
                selector.choose(
                        groups,
                        new int[][] {
                            {
                                RendererCapabilities.FORMAT_HANDLED
                                        | RendererCapabilities.ADAPTIVE_SEAMLESS
                            }
                        }));
    }
}
