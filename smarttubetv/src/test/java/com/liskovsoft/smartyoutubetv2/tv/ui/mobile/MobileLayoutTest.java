package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MobileLayoutTest {
    private Context context() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.MobileTheme);
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-port-mdpi")
    public void phoneFeedUsesOneColumnAndReachableCardMenu() {
        Context c = context();
        MobileFeedView feed = new MobileFeedView(c, v -> {}, v -> {}, v -> {});
        assertEquals(1, ((GridLayoutManager) feed.list.getLayoutManager()).getSpanCount());
        Video v = new Video();
        v.videoId = "test";
        v.title =
                "A long video title that still leaves the overflow action reachable on a narrow"
                        + " phone";
        v.cardImageUrl = "file:///missing-thumbnail";
        feed.update(VideoGroup.from(Collections.singletonList(v)));
        int width = MobileUi.dp(c, 360), height = MobileUi.dp(c, 800);
        feed.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        feed.layout(0, 0, width, height);
        shadowOf(Looper.getMainLooper()).idle();
        feed.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        feed.layout(0, 0, width, height);
        MobileFeedAdapter.Card card =
                (MobileFeedAdapter.Card) feed.list.findViewHolderForAdapterPosition(0);
        assertNotNull(card);
        assertEquals(card.image.getWidth() * 9 / 16, card.image.getHeight());
        assertTrue(card.menu.getWidth() >= MobileUi.dp(c, 48));
        assertTrue(card.menu.getHeight() >= MobileUi.dp(c, 48));
        assertTrue(card.menu.getRight() <= width);
    }

    @Test
    @Config(sdk = 26, qualifiers = "w320dp-h640dp-port-mdpi")
    public void androidEightWithLargeTextKeepsTouchTargetsAndScalesToolbar() {
        Context c = context();
        android.content.res.Configuration config =
                new android.content.res.Configuration(c.getResources().getConfiguration());
        config.fontScale = 2;
        c.getResources().updateConfiguration(config, c.getResources().getDisplayMetrics());
        android.widget.LinearLayout toolbar = MobileUi.toolbar(c, "SmartTube", () -> {});
        toolbar.measure(
                View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.AT_MOST));
        toolbar.layout(0, 0, 320, toolbar.getMeasuredHeight());
        assertTrue(toolbar.getChildAt(0).getHeight() >= MobileUi.dp(c, 48));
        android.widget.TextView title = (android.widget.TextView) toolbar.getChildAt(2);
        assertTrue(title.getTextSize() >= 40);
        assertTrue(
                title.getHeight()
                        >= title.getLineHeight()
                                + title.getPaddingTop()
                                + title.getPaddingBottom());
    }

    @Test
    @Config(qualifiers = "w720dp-h1024dp-port-mdpi")
    public void tabletFeedUsesTwoColumnsAndFullWidthHeadings() {
        Context c = context();
        MobileFeedView feed = new MobileFeedView(c, v -> {}, v -> {}, v -> {});
        GridLayoutManager manager = (GridLayoutManager) feed.list.getLayoutManager();
        assertEquals(2, manager.getSpanCount());
        Video v = new Video();
        v.videoId = "test";
        VideoGroup group = VideoGroup.from(Collections.singletonList(v));
        group.setTitle("Videos");
        feed.update(group);
        assertEquals(2, manager.getSpanSizeLookup().getSpanSize(0));
        assertEquals(1, manager.getSpanSizeLookup().getSpanSize(1));
    }
}
