package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;

import android.app.Application;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.FragmentActivity;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.*;
import com.liskovsoft.smartyoutubetv2.tv.R;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, qualifiers = "w360dp-h800dp-port-mdpi")
public class MobileVideoInteractionsTest {
    // Exercise the real action buttons and video binding without starting network playback.
    public static class TestPlayer extends MobilePlaybackFragment {
        Button like, share;
        int panelWidth;
        int videoBottom;
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
            surface = new com.google.android.exoplayer2.ui.PlayerView(requireContext());
            ReflectionHelpers.setField(this, "overlay", new com.github.vkay94.dtpv.youtube.YouTubeOverlay(requireContext(), null));
            ReflectionHelpers.setField(this, "storyboard", new com.liskovsoft.smartyoutubetv2.tv.ui.playback.previewtimebar.StoryboardManager(requireContext()));
            LinearLayout row = new LinearLayout(requireContext());
            like = addButton(row, R.id.action_thumbs_up, "Like");
            share = addButton(row, R.id.action_share, "Share");
            return row;
        }
        private Button addButton(LinearLayout row, int id, String label) {
            return ReflectionHelpers.callInstanceMethod(this, "actionButton",
                    ReflectionHelpers.ClassParameter.from(LinearLayout.class, row),
                    ReflectionHelpers.ClassParameter.from(int.class, id),
                    ReflectionHelpers.ClassParameter.from(String.class, label));
        }
        @Override public void onActivityCreated(Bundle state) {
            // Complete Fragment's lifecycle without initializing network playback.
            ReflectionHelpers.setField(this, "mCalled", true);
        }
        @Override void setCommentsPanelWidth(int width) { panelWidth = width; }
        @Override int getVideoBottomOnScreen() { return videoBottom; }
        @Override public boolean isInPIPMode() { return false; }
    }

    private org.robolectric.android.controller.ActivityController<FragmentActivity> host(TestPlayer player) {
        org.robolectric.android.controller.ActivityController<FragmentActivity> host =
                Robolectric.buildActivity(FragmentActivity.class).setup();
        host.get().setTheme(R.style.MobileTheme);
        host.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content, player).commitNow();
        player.getView().measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
        player.getView().layout(0, 0, 360, 100);
        return host;
    }

    @Test public void likeCountsRefreshWithoutBeingOverwrittenByLikeStateOrTvPreferences() {
        TestPlayer player = new TestPlayer();
        org.robolectric.android.controller.ActivityController<FragmentActivity> host = host(player);
        Video video = Video.from("first");
        player.setVideo(video);
        assertEquals("Like", player.like.getText().toString());
        video.likeCount = "12.3K";
        player.setVideo(video);
        player.setButtonState(R.id.action_thumbs_up, PlayerUI.BUTTON_ON);
        assertEquals("12.3K", player.like.getText().toString());
        assertTrue(player.like.isSelected());
        assertTrue(player.like.getContentDescription().toString().contains("12.3K"));
        video.likeCount = null;
        player.setVideo(video);
        player.setButtonState(R.id.action_thumbs_up, PlayerUI.BUTTON_OFF);
        assertEquals("12.3K", player.like.getText().toString());
        assertFalse(player.like.isSelected());
        video.likeCount = "0";
        player.setVideo(video);
        assertEquals("0", player.like.getText().toString());
        Video next = Video.from("next");
        next.likeCount = " ";
        player.setVideo(next);
        assertEquals("Like", player.like.getText().toString());
        next.likeCount = "42";
        player.setVideo(next);
        assertEquals("42", player.like.getText().toString());
        player.setVideo(null);
        assertEquals("Like", player.like.getText().toString());
        host.destroy();
    }

    @Test public void shareButtonRequestsBoundedPopupAndPreservesAllChoiceCallbacks() {
        TestPlayer player = new TestPlayer();
        org.robolectric.android.controller.ActivityController<FragmentActivity> host = host(player);
        Video video = Video.from("first");
        video.title = "Video title";
        player.setVideo(video);
        ReflectionHelpers.setField(player.mPlaybackPresenter, "mVideo", new java.lang.ref.WeakReference<>(video));
        player.share.performClick();
        org.robolectric.android.controller.ActivityController<MobileDialogActivity> dialog =
                Robolectric.buildActivity(MobileDialogActivity.class).create();
        MobileDialogActivity popup = dialog.get();
        int[] clicks = {0, 0, 0};
        List<OptionItem> choices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int index = i;
            choices.add(UiOptionItem.from(new String[]{"Share link", "QR code", "Embed link"}[i],
                    option -> clicks[index]++));
        }
        popup.show(Collections.singletonList(OptionCategory.stringList(null, choices)),
                video.getTitle(), false, false, false, 1);
        WindowManager.LayoutParams window = popup.getWindow().getAttributes();
        assertTrue(window.width > 0 && window.width < 360);
        assertTrue(window.height > 0 && window.height < 520);
        assertTrue(window.x >= 0 && window.x + window.width <= 360);
        assertTrue(window.y >= 0 && window.y + window.height <= 800);
        assertTrue(popup.isOverlay());
        LinearLayout body = ReflectionHelpers.getField(popup, "body");
        for (int i = 0; i < body.getChildCount(); i++) body.getChildAt(i).performClick();
        assertArrayEquals(new int[]{1, 1, 1}, clicks);
        android.view.MotionEvent outside = android.view.MotionEvent.obtain(0, 0,
                android.view.MotionEvent.ACTION_OUTSIDE, -10, -10, 0);
        popup.onTouchEvent(outside);
        outside.recycle();
        assertTrue(popup.isFinishing());
        dialog.destroy();
        host.destroy();
    }

    @Test public void commentsPreserveScrollAndResizePlayerAcrossFullscreenThenCloseOnVideoChange() {
        TestPlayer player = new TestPlayer();
        org.robolectric.android.controller.ActivityController<FragmentActivity> host = host(player);
        player.setVideo(Video.from("first"));
        org.robolectric.android.controller.ActivityController<MobileDialogActivity> dialog =
                Robolectric.buildActivity(MobileDialogActivity.class).create();
        MobileDialogActivity sheet = dialog.get();
        OptionCategory comments = OptionCategory.from(1, OptionCategory.TYPE_COMMENTS,
                "Comments", Collections.emptyList());
        sheet.show(Collections.singletonList(comments), "Comments", false, true, true, 1);
        player.videoBottom = 260;
        MobileDialogActivity.updateCommentsLayout(sheet);
        assertEquals(540, sheet.getWindow().getAttributes().height);
        ScrollView scroll = ReflectionHelpers.getField(sheet, "scroller");
        LinearLayout body = ReflectionHelpers.getField(sheet, "body");
        TextView longComment = new TextView(sheet);
        longComment.setMinHeight(2000);
        body.addView(longComment);
        scroll.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY));
        scroll.layout(0, 0, 360, 500);
        scroll.scrollTo(0, 250);
        MotionEvent outside = MotionEvent.obtain(0, 0, MotionEvent.ACTION_OUTSIDE, 0, -100, 0);
        sheet.onTouchEvent(outside);
        outside.recycle();
        assertFalse(sheet.isFinishing());
        assertEquals(250, scroll.getScrollY());
        sheet.onPause();
        assertFalse(sheet.isFinishing());
        RuntimeEnvironment.setQualifiers("w800dp-h360dp-land-mdpi");
        sheet.onConfigurationChanged(sheet.getResources().getConfiguration());
        sheet.onResume();
        assertEquals(sheet.getWindow().getAttributes().width, player.panelWidth);
        assertEquals(250, scroll.getScrollY());
        assertSame(scroll, ReflectionHelpers.getField(sheet, "scroller"));
        RuntimeEnvironment.setQualifiers("w360dp-h800dp-port-mdpi");
        sheet.onConfigurationChanged(sheet.getResources().getConfiguration());
        assertEquals(0, player.panelWidth);
        assertEquals(250, scroll.getScrollY());
        player.setVideo(Video.from("next"));
        assertTrue(sheet.isFinishing());
        assertEquals(0, player.panelWidth);
        dialog.destroy();
        host.destroy();
    }
}
