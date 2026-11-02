package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;
import static org.junit.Assert.*;
import android.app.Application;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.*;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.*;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk=28)
public class MobileDesignRefreshTest {
    private Context context() { return new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.MobileTheme); }
    @Test public void playerButtonsAreTransparentAtRestAndKeepTouchFeedback() {
        Button button = MobileUi.button(context(), "CC", () -> {});
        MobileUi.playerFeedback(button);
        android.graphics.drawable.StateListDrawable states =
                (android.graphics.drawable.StateListDrawable) button.getBackground();
        org.robolectric.shadows.ShadowStateListDrawable shadow = org.robolectric.Shadows.shadowOf(states);
        for (boolean selected : new boolean[]{false, true}) {
            int[] resting = selected ? new int[]{android.R.attr.state_selected} : new int[]{};
            int[] pressedState = selected
                    ? new int[]{android.R.attr.state_selected, android.R.attr.state_pressed}
                    : new int[]{android.R.attr.state_pressed};
            android.graphics.drawable.GradientDrawable normal =
                    (android.graphics.drawable.GradientDrawable) shadow.getDrawableForState(resting);
            assertEquals(android.graphics.Color.TRANSPARENT,
                    org.robolectric.Shadows.shadowOf(normal).getLastSetColor());
            android.graphics.drawable.GradientDrawable pressed =
                    (android.graphics.drawable.GradientDrawable) shadow.getDrawableForState(pressedState);
            assertNotEquals(android.graphics.Color.TRANSPARENT,
                    org.robolectric.Shadows.shadowOf(pressed).getLastSetColor());
        }
    }

    @Test public void silentPaginationKeepsBusyStateAndRowsWithoutStatusSpace() {
        MobileFeedView feed = new MobileFeedView(context(), v -> {}, v -> {}, v -> {});
        feed.setPaginationIndicatorVisible(false);
        LinearLayout status = (LinearLayout) feed.getChildAt(1);
        feed.loading(true);
        assertEquals(View.VISIBLE, status.getChildAt(3).getVisibility());
        Video video = new Video(); video.videoId = "loaded";
        feed.update(VideoGroup.from(Collections.singletonList(video)));
        assertTrue(feed.loading());
        assertEquals(View.VISIBLE, feed.list.getVisibility());
        assertEquals(View.GONE, status.getVisibility());
        assertEquals(View.GONE, status.getChildAt(0).getVisibility());
        feed.error("Offline", null, () -> {});
        assertEquals(View.VISIBLE, status.getVisibility());
        assertEquals(View.VISIBLE, status.getChildAt(2).getVisibility());
        assertEquals(1, feed.adapter.getItemCount());
        feed.loading(true);
        feed.setPaginationIndicatorVisible(true);
        assertEquals(View.VISIBLE, status.getChildAt(0).getVisibility());
    }

    @Test public void initialLoadingUsesPlaceholderAndPaginationRetainsVideos() {
        MobileFeedView feed = new MobileFeedView(context(), v -> {}, v -> {}, v -> {});
        LinearLayout status = (LinearLayout) feed.getChildAt(1);
        feed.loading(true);
        assertEquals(View.VISIBLE, status.getChildAt(3).getVisibility());
        assertEquals(View.GONE, status.getChildAt(0).getVisibility());
        Video video = new Video(); video.videoId = "real";
        feed.update(VideoGroup.from(Collections.singletonList(video)));
        assertEquals(View.VISIBLE, feed.list.getVisibility());
        assertEquals(View.GONE, status.getChildAt(3).getVisibility());
        assertEquals(View.VISIBLE, status.getChildAt(0).getVisibility());
        feed.error("Connection lost", null, () -> {});
        assertEquals(View.GONE, status.getChildAt(3).getVisibility());
        assertEquals(1, feed.adapter.getItemCount());
    }
    @Test public void scrollingHidesNavigationAndRestoresMeasuredSpaceWithoutPaddingDrift() {
        Context c=context(); LinearLayout content=MobileUi.column(c), tabs=new LinearLayout(c);
        ScrollView scroll=new ScrollView(c); TextView items=new TextView(c); items.setMinHeight(2000);
        scroll.addView(items); scroll.setPadding(0,0,0,7); content.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        MobileUi.addNavigation(tabs,MobileUi.browseNavigation(c,R.drawable.mobile_history,"History",()->{}));
        MobileNavigationShell shell=new MobileNavigationShell(c,content,tabs);
        shell.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY));
        shell.layout(0,0,320,640);
        int reserved=((FrameLayout.LayoutParams)content.getLayoutParams()).bottomMargin;
        assertTrue(reserved>0);
        scroll.scrollTo(0,100);
        assertEquals(View.VISIBLE,tabs.getVisibility()); // Restoring position is not a gesture.
        shell.onUserScroll(100);
        assertEquals(View.GONE,tabs.getVisibility());
        assertEquals(0,((FrameLayout.LayoutParams)content.getLayoutParams()).bottomMargin);
        shell.setKeyboardVisible(true); shell.onUserScroll(-1);
        assertEquals(View.GONE,tabs.getVisibility());
        shell.setKeyboardVisible(false); shell.onUserScroll(-100);
        assertEquals(View.VISIBLE,tabs.getVisibility());
        assertEquals(reserved,((FrameLayout.LayoutParams)content.getLayoutParams()).bottomMargin);
        assertEquals(7,scroll.getPaddingBottom());
        shell.onUserScroll(50); shell.revealNavigation();
        assertEquals(View.VISIBLE,tabs.getVisibility());
        assertEquals(reserved,((FrameLayout.LayoutParams)content.getLayoutParams()).bottomMargin);
    }
    @Test public void shareFeedbackFillsTheSameShapeAndKeepsItsClickAction() {
        Context c=context(); int[] clicks={0}; Button button=MobileUi.button(c,"Share",()->clicks[0]++);
        MobileUi.browseFeedback(button,true);
        android.graphics.drawable.StateListDrawable states=(android.graphics.drawable.StateListDrawable)button.getBackground();
        android.graphics.drawable.GradientDrawable normal=(android.graphics.drawable.GradientDrawable)states.getCurrent();
        button.setPressed(true);
        android.graphics.drawable.GradientDrawable pressed=(android.graphics.drawable.GradientDrawable)states.getCurrent();
        assertEquals(normal.getCornerRadius(),pressed.getCornerRadius(),0f);
        assertNotEquals(normal.getColor().getDefaultColor(),pressed.getColor().getDefaultColor());
        button.performClick(); assertEquals(1,clicks[0]);
        button.setPressed(false); button.setSelected(true);
        normal=(android.graphics.drawable.GradientDrawable)states.getCurrent();
        button.setPressed(true); pressed=(android.graphics.drawable.GradientDrawable)states.getCurrent();
        assertEquals(normal.getCornerRadius(),pressed.getCornerRadius(),0f);
        assertNotEquals(normal.getColor().getDefaultColor(),pressed.getColor().getDefaultColor());
    }
    @Test public void subscriptionTextTracksSelectedSurface() {
        Context c=context(); Button button=MobileUi.button(c,"Subscribe",()->{});
        MobileUi.subscriptionFeedback(button);
        assertEquals(c.getColor(R.color.mobile_text_inverse),button.getCurrentTextColor());
        button.setSelected(true);
        assertEquals(c.getColor(R.color.mobile_text_primary),button.getCurrentTextColor());
    }
    @Test public void navigationKeepsLabelAndActionAcrossSelection() {
        int[] clicks={0}; Button button=MobileUi.browseNavigation(context(),R.drawable.icon_home,"Home",()->clicks[0]++);
        for(boolean selected:new boolean[]{true,false,true}) { MobileUi.navigationState(button,selected); button.performClick(); }
        assertEquals(3,clicks[0]); assertEquals("Home",button.getText().toString());
        assertNotNull(button.getCompoundDrawables()[1]);
    }
}
