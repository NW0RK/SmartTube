package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowInsets;
import android.graphics.Rect;
import android.widget.*;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, qualifiers = "w320dp-h640dp-port-mdpi")
public class MobileRedesignTest {
    private Context context() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.MobileTheme);
    }

    private void layout(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 320, 640);
    }

    @Test public void navigationReservesMeasuredHeightAndInsetsWithoutAccumulatingPadding() {
        Context c = context();
        LinearLayout content = MobileUi.column(c), tabs = new LinearLayout(c);
        content.setPadding(0, 0, 0, 7);
        MobileUi.addNavigation(tabs, MobileUi.browseNavigation(c, R.drawable.icon_home, "Home", () -> {}));
        MobileNavigationShell shell = new MobileNavigationShell(c, content, tabs);
        WindowInsets insets = ReflectionHelpers.callConstructor(WindowInsets.class,
                ReflectionHelpers.ClassParameter.from(Rect.class, new Rect(4, 24, 4, 20)));
        shell.dispatchApplyWindowInsets(insets);
        layout(shell);
        assertEquals(20, shell.getPaddingBottom());
        assertEquals(tabs.getHeight() + 24 + 7, content.getPaddingBottom());
        shell.dispatchApplyWindowInsets(insets);
        layout(shell);
        assertEquals(tabs.getHeight() + 24 + 7, content.getPaddingBottom());
        shell.setKeyboardVisible(true);
        assertEquals(View.GONE, tabs.getVisibility());
        assertEquals(7, content.getPaddingBottom());
        shell.setKeyboardVisible(false);
        assertEquals(View.VISIBLE, tabs.getVisibility());
        assertEquals(tabs.getHeight() + 24 + 7, content.getPaddingBottom());
    }

    @Test public void largeTextNavigationRetainsLabelsAndClickTargets() {
        Context c = context();
        android.content.res.Configuration config = new android.content.res.Configuration(c.getResources().getConfiguration());
        config.fontScale = 2;
        c = c.createConfigurationContext(config);
        LinearLayout content = MobileUi.column(c), tabs = new LinearLayout(c);
        int[] clicks = {0};
        for (String label : new String[]{"Home", "Subscriptions", "You"})
            MobileUi.addNavigation(tabs, MobileUi.browseNavigation(c, R.drawable.icon_home, label, () -> clicks[0]++));
        MobileNavigationShell shell = new MobileNavigationShell(c, content, tabs);
        layout(shell);
        for (int i = 0; i < tabs.getChildCount(); i++) {
            Button tab = (Button) tabs.getChildAt(i);
            assertTrue(tab.getWidth() >= 48);
            assertTrue(tab.getHeight() >= 48);
            assertFalse(tab.getText().toString().isEmpty());
            tab.performClick();
        }
        assertEquals(3, clicks[0]);
        assertTrue(content.getPaddingBottom() >= tabs.getHeight());
    }

    @Test public void channelHeaderDoesNotPresentVideoTitleOrDescriptionAsChannelIdentity() {
        Context c = context();
        Video video = new Video();
        video.videoId = "video";
        video.channelId = "UC123";
        video.title = "Video title";
        video.description = "Video description";
        video.author = "Creator";
        video.subscriberCount = "240K subscribers";
        assertEquals("Creator", MobileChannelHeader.name(c, video, "UC123"));
        LinearLayout header = MobileChannelHeader.create(c, video, "UC123");
        assertEquals(2, header.getChildCount());
        assertEquals("Creator", ((TextView) header.getChildAt(0)).getText().toString());
        video.author = null;
        assertEquals("@creator", MobileChannelHeader.name(c, video, "@creator"));
        assertEquals(c.getString(R.string.header_channels), MobileChannelHeader.name(c, video, "UC123"));
        video.secondTitle = "Creator • 10 views";
        assertEquals("Creator", MobileChannelHeader.name(c, video, "UC123"));
        video.secondTitle = null;
        video.itemType = MediaItem.TYPE_CHANNEL;
        video.videoId = null;
        assertEquals("Video title", MobileChannelHeader.name(c, video, "UC123"));
        assertEquals(3, MobileChannelHeader.create(c, video, "UC123").getChildCount());
    }

    @Test public void recreationUsesSavedSelectionRatherThanOldLaunchDestination() {
        Intent intent = new Intent().putExtra(MobileNavigationShell.DESTINATION, MobileNavigationShell.YOU);
        assertEquals(MobileNavigationShell.YOU, MobileNavigationShell.requestedDestination(null, intent));
        Bundle restored = new Bundle();
        restored.putInt("section", 0);
        assertEquals(-1, MobileNavigationShell.requestedDestination(restored, intent));
        assertEquals(-1, MobileNavigationShell.requestedDestination(null, new Intent()));
    }

    @Test public void watchToolbarUsesImageControlAndKeepsBackAction() {
        int[] clicks = {0};
        LinearLayout toolbar = MobileUi.watchToolbar(context(), () -> clicks[0]++);
        assertTrue(toolbar.getChildAt(0) instanceof ImageButton);
        assertNotNull(((ImageButton) toolbar.getChildAt(0)).getDrawable());
        assertNotNull(toolbar.getChildAt(0).getContentDescription());
        toolbar.getChildAt(0).performClick();
        assertEquals(1, clicks[0]);
    }
}
