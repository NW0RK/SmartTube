package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MobileBrowsePolishTest {
    private Context context() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.MobileTheme);
    }

    private boolean visibleText(View view, String text) {
        if (view.getVisibility() != View.VISIBLE) return false;
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
                if (visibleText(group.getChildAt(i), text)) return true;
        }
        return false;
    }

    @Test
    public void idleLoadingEmptyAndRetryRecovery() {
        Context c = context();
        MobileFeedView feed = new MobileFeedView(c, v -> {}, v -> {}, v -> {});
        feed.setEmptyText(c.getString(R.string.mobile_search_empty));
        assertFalse(visibleText(feed, c.getString(R.string.mobile_search_empty)));
        feed.loading(true);
        // SearchPresenter starts loading before it clears the previous results.
        feed.clear();
        assertTrue(feed.loading());
        assertFalse(visibleText(feed, c.getString(R.string.mobile_search_empty)));
        feed.update(VideoGroup.from(Collections.emptyList()));
        feed.loading(false);
        assertTrue(visibleText(feed, c.getString(R.string.mobile_search_empty)));
        int[] retries = {0};
        feed.error("Connection lost", "Reconnect", () -> { retries[0]++; feed.loading(true); });
        assertTrue(visibleText(feed, "Connection lost"));
        LinearLayout status = (LinearLayout) feed.getChildAt(1);
        ((Button) status.getChildAt(2)).performClick();
        assertEquals(1, retries[0]);
        assertFalse(visibleText(feed, "Connection lost"));
        Video video = new Video();
        video.videoId = "loaded";
        feed.update(VideoGroup.from(Collections.singletonList(video)));
        feed.loading(false);
        assertEquals(View.VISIBLE, feed.list.getVisibility());
        assertFalse(visibleText(feed, c.getString(R.string.mobile_search_empty)));
    }

    @Test
    public void paginationFailurePreservesContentAndErrorUntilRetry() {
        Context c = context();
        MobileFeedView feed = new MobileFeedView(c, v -> {}, v -> {}, v -> {});
        Video video = new Video();
        video.videoId = "retained";
        feed.update(VideoGroup.from(Collections.singletonList(video)));
        feed.loading(true);
        assertEquals(View.VISIBLE, feed.list.getVisibility());
        feed.error("Could not load more", null, () -> {});
        feed.loading(false);
        assertEquals(1, feed.adapter.getItemCount());
        assertEquals(View.VISIBLE, feed.list.getVisibility());
        assertTrue(visibleText(feed, "Could not load more"));
        feed.loading(true);
        assertFalse(visibleText(feed, "Could not load more"));
        feed.idle();
        assertFalse(feed.loading());
        assertFalse(visibleText(feed, c.getString(R.string.mobile_empty)));
    }

    @Test
    public void headerRemainsVisibleDuringLoading() {
        MobileFeedView feed = new MobileFeedView(context(), v -> {}, v -> {}, v -> {});
        feed.adapter.setHeader(new TextView(context()));
        assertEquals(View.VISIBLE, feed.list.getVisibility());
        feed.loading(true);
        assertEquals(View.VISIBLE, feed.list.getVisibility());
    }

    @Test
    @Config(sdk = 26, qualifiers = "w320dp-h640dp-port-mdpi")
    public void narrowSearchWithLargeTextAndRtlKeepsActionsReachable() {
        Context c = context();
        android.content.res.Configuration config = new android.content.res.Configuration(c.getResources().getConfiguration());
        config.fontScale = 2;
        c = new ContextThemeWrapper(c.createConfigurationContext(config), R.style.MobileTheme);
        EditText query = new EditText(c);
        ImageButton clear = MobileUi.browseIcon(c, R.drawable.mobile_clear, c.getString(R.string.mobile_clear_search), () -> {});
        LinearLayout bar = MobileUi.searchToolbar(c, query, clear, () -> {}, () -> {});
        bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int width = MobileUi.dp(c, 320);
        bar.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(MobileUi.dp(c, 640), View.MeasureSpec.AT_MOST));
        bar.layout(0, 0, width, bar.getMeasuredHeight());
        assertTrue(query.getWidth() >= MobileUi.dp(c, 100));
        assertTrue(query.getHeight() >= query.getLineHeight());
        assertTrue(clear.getWidth() >= MobileUi.dp(c, 48));
        assertTrue(clear.getHeight() >= MobileUi.dp(c, 48));
        assertNotNull(clear.getContentDescription());
        assertTrue(bar.getChildAt(bar.getChildCount() - 1).getRight() <= width);
    }

    @Test
    public void navigationAndChipsExposeSelectionAndDisabledState() {
        Context c = context();
        Button tab = MobileUi.browseNavigation(c, R.drawable.icon_home, "Home", () -> {});
        assertEquals("", tab.getText().toString());
        assertEquals("Home", tab.getContentDescription());
        assertNotNull(tab.getForeground());
        LinearLayout navigation = new LinearLayout(c);
        MobileUi.addNavigation(navigation, tab);
        LinearLayout.LayoutParams spacing = (LinearLayout.LayoutParams) tab.getLayoutParams();
        assertTrue(spacing.leftMargin > 0);
        assertTrue(spacing.rightMargin > 0);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, spacing.height);
        assertTrue(tab.getMinHeight() >= MobileUi.dp(c, 48));
        MobileUi.navigationState(tab, true);
        assertTrue(tab.isSelected());
        assertEquals(MobileUi.accent(c), tab.getCurrentTextColor());
        MobileUi.navigationState(tab, false);
        assertFalse(tab.isSelected());
        tab.setEnabled(false);
        assertFalse(tab.isEnabled());
        Button chip = MobileUi.browseButton(c, "Subscriptions", () -> {});
        chip.setSelected(true);
        assertEquals(MobileUi.accent(c), chip.getCurrentTextColor());
        chip.setEnabled(false);
        assertEquals(c.getColor(R.color.mobile_text_secondary), chip.getCurrentTextColor());
    }
}
