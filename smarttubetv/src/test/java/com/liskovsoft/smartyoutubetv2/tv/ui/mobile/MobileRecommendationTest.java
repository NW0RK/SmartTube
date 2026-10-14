package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.tv.R;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, qualifiers = "w360dp-h800dp-port-mdpi")
public class MobileRecommendationTest {
    @Test public void loadingRecommendationsDoesNotScrollWatchDetailsOrOverrideManualScroll() {
        org.robolectric.android.controller.ActivityController<Activity> host = Robolectric.buildActivity(Activity.class).setup();
        Activity activity = host.get();
        activity.setTheme(R.style.MobileTheme);
        MobileFeedView feed = new MobileFeedView(activity, v -> {}, v -> {}, v -> {});
        View details = new View(activity);
        details.setMinimumHeight(1200);
        feed.adapter.setHeader(details);
        activity.setContentView(feed);
        MobilePlaybackFragment player = new MobilePlaybackFragment();
        org.robolectric.util.ReflectionHelpers.setField(player, "suggestions", feed);
        com.liskovsoft.smartyoutubetv2.common.app.models.data.Video current =
                com.liskovsoft.smartyoutubetv2.common.app.models.data.Video.from("current");
        com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup group =
                com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup.from(Collections.singletonList(current));
        player.updateSuggestions(group);
        layout(feed, 360, 600);
        player.focusSuggestedItem(current);
        player.focusSuggestedItem(1);
        layout(feed, 360, 600);
        androidx.recyclerview.widget.GridLayoutManager manager =
                (androidx.recyclerview.widget.GridLayoutManager) feed.list.getLayoutManager();
        assertEquals(0, manager.findFirstVisibleItemPosition());
        assertEquals(0, manager.findViewByPosition(0).getTop());

        feed.list.scrollBy(0, 250);
        int scrolledTop = manager.findViewByPosition(0).getTop();
        assertTrue(scrolledTop < 0);
        player.updateSuggestions(group);
        player.focusSuggestedItem(current);
        player.focusSuggestedItem(1);
        layout(feed, 360, 600);
        assertEquals(scrolledTop, manager.findViewByPosition(0).getTop());
        host.destroy();
    }

    @Test public void resetWatchPositionAlignsTopOfDetailsTallerThanViewport() {
        org.robolectric.android.controller.ActivityController<Activity> host = Robolectric.buildActivity(Activity.class).setup();
        Activity activity = host.get();
        activity.setTheme(R.style.MobileTheme);
        MobileFeedView feed = new MobileFeedView(activity, v -> {}, v -> {}, v -> {});
        View details = new View(activity);
        details.setMinimumHeight(1200);
        feed.adapter.setHeader(details);
        activity.setContentView(feed);
        layout(feed, 360, 600);
        feed.list.scrollBy(0, 300);
        androidx.recyclerview.widget.GridLayoutManager manager =
                (androidx.recyclerview.widget.GridLayoutManager) feed.list.getLayoutManager();
        assertTrue(manager.findViewByPosition(0).getTop() < 0);
        MobilePlaybackFragment player = new MobilePlaybackFragment();
        org.robolectric.util.ReflectionHelpers.setField(player, "suggestions", feed);
        player.resetSuggestedPosition();
        layout(feed, 360, 600);
        assertEquals(0, manager.findViewByPosition(0).getTop());
        host.destroy();
    }

    private void layout(View view, int width, int height) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    private Button findButton(View view, String text) {
        if (view instanceof Button && text.contentEquals(((Button) view).getText())) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button button = findButton(group.getChildAt(i), text);
                if (button != null) return button;
            }
        }
        return null;
    }

    @Test public void recommendationsUseLargeThumbnailsWithMetadataBelowAndOverflowBeside() {
        org.robolectric.android.controller.ActivityController<Activity> host = Robolectric.buildActivity(Activity.class).setup();
        Activity activity = host.get();
        activity.setTheme(R.style.MobileTheme);
        FrameLayout parent = new FrameLayout(activity);
        MobileFeedAdapter adapter = new MobileFeedAdapter(v -> {}, v -> {}, v -> {});
        adapter.setCompactCards(true);
        MobileFeedAdapter.Card card = (MobileFeedAdapter.Card) adapter.onCreateViewHolder(parent, 1);
        card.title.setText("A long recommendation title that wraps underneath the thumbnail without occupying the overflow column");
        card.meta.setText("Creator · 10K views · Yesterday");
        layout(card.itemView, 328, 800);
        assertTrue(card.image.getWidth() >= 250);
        assertEquals(card.image.getWidth() * 9 / 16, card.image.getHeight());
        assertTrue(card.menu.getWidth() >= 48);
        assertEquals(card.image.getParent().getParent(), card.menu.getParent());
        android.graphics.Rect thumbnail = new android.graphics.Rect(), title = new android.graphics.Rect();
        card.image.getDrawingRect(thumbnail);
        card.title.getDrawingRect(title);
        ((ViewGroup) card.itemView).offsetDescendantRectToMyCoords(card.image, thumbnail);
        ((ViewGroup) card.itemView).offsetDescendantRectToMyCoords(card.title, title);
        assertTrue(title.top >= thumbnail.bottom);
        assertEquals(thumbnail.left, title.left);
        assertEquals(thumbnail.right, title.right);
        assertTrue(card.meta.getTop() >= card.title.getBottom());
        host.destroy();
    }

    @Test public void channelIsSeparateFromViewsAndUploadDateAndRecyclingClearsMissingMetadata() {
        org.robolectric.android.controller.ActivityController<Activity> host = Robolectric.buildActivity(Activity.class).setup();
        Activity activity = host.get();
        activity.setTheme(R.style.MobileTheme);
        MobileFeedAdapter adapter = new MobileFeedAdapter(v -> {}, v -> {}, v -> {});
        adapter.setCompactCards(true);
        MobileFeedAdapter.Card card = (MobileFeedAdapter.Card) adapter.onCreateViewHolder(new FrameLayout(activity), 1);
        com.liskovsoft.smartyoutubetv2.common.app.models.data.Video video = new com.liskovsoft.smartyoutubetv2.common.app.models.data.Video();
        video.videoId = "recommendation";
        video.author = "A long channel name";
        video.secondTitle = "A long channel name • 2.1M views • 3 days ago";
        MobileFeedAdapter.bindRecommendationMetadata(card, video);
        assertEquals("A long channel name", card.channel.getText().toString());
        assertEquals("2.1M views · 3 days ago", card.meta.getText().toString());
        assertEquals(View.VISIBLE, card.channel.getVisibility());
        video.author = null;
        video.secondTitle = null;
        MobileFeedAdapter.bindRecommendationMetadata(card, video);
        assertEquals(View.GONE, card.channel.getVisibility());
        assertEquals(View.GONE, card.meta.getVisibility());
        assertEquals("", card.meta.getText().toString());
        host.destroy();
    }

    @Test public void overflowPopupPreservesActionsAndDismissesOutsideWithoutReplacingWatchLayout() {
        org.robolectric.android.controller.ActivityController<Activity> host = Robolectric.buildActivity(Activity.class).setup();
        Activity activity = host.get();
        FrameLayout parent = new FrameLayout(activity);
        parent.setMinimumHeight(800);
        Button anchor = new Button(activity);
        FrameLayout.LayoutParams anchorParams = new FrameLayout.LayoutParams(48, 48);
        anchorParams.leftMargin = 280;
        anchorParams.topMargin = 450;
        parent.addView(anchor, anchorParams);
        activity.setContentView(parent);
        layout(parent, 360, 800);
        org.robolectric.android.controller.ActivityController<MobileDialogActivity> dialog = Robolectric.buildActivity(MobileDialogActivity.class).create();
        MobileDialogActivity popup = dialog.get();
        MobileDialogActivity.requestPopup(anchor, "Video");
        int[] clicks = {0};
        OptionCategory actions = OptionCategory.stringList(null,
                Collections.singletonList(UiOptionItem.from("Save", option -> clicks[0]++)));
        popup.show(Collections.singletonList(actions), "Video", false, false, false, 1);
        WindowManager.LayoutParams window = popup.getWindow().getAttributes();
        assertTrue(window.width > 0 && window.width < 360);
        assertTrue(window.height > 0 && window.height < 800);
        assertTrue(popup.isOverlay());
        assertTrue(popup.isTransparent());
        assertTrue((window.flags & WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH) != 0);
        Button save = findButton(popup.getWindow().getDecorView(), "Save");
        assertNotNull(save);
        save.performClick();
        assertEquals(1, clicks[0]);
        android.view.MotionEvent outside = android.view.MotionEvent.obtain(0, 0,
                android.view.MotionEvent.ACTION_OUTSIDE, -10, -10, 0);
        popup.onTouchEvent(outside);
        outside.recycle();
        assertTrue(popup.isFinishing());
        assertFalse(activity.isFinishing());
        dialog.destroy();
        host.destroy();
    }
}
