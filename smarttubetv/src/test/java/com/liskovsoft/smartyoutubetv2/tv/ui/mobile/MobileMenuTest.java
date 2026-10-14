package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.*;
import com.liskovsoft.smartyoutubetv2.tv.R;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, qualifiers = "w360dp-h800dp-port-mdpi")
public class MobileMenuTest {
    private Context context() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.MobileTheme);
    }

    private TextView text(View root, String label) {
        if (root instanceof TextView && label.contentEquals(((TextView) root).getText())) return (TextView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = text(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
    }

    private BrowseSection section(int id, String label) {
        return new BrowseSection(id, label, BrowseSection.TYPE_GRID, R.drawable.icon_history);
    }

    @Test public void youRoutesShortcutsAndRowsAndHidesExcludedDestinations() {
        BrowseSection disabled = section(12345, "Disabled");
        disabled.setEnabled(false);
        List<BrowseSection> sections = Arrays.asList(section(MediaGroup.TYPE_HOME, "Home"),
                section(MediaGroup.TYPE_SUBSCRIPTIONS, "Subscriptions"), section(MediaGroup.TYPE_SHORTS, "Shorts"),
                section(MediaGroup.TYPE_HISTORY, "History"), section(MediaGroup.TYPE_USER_PLAYLISTS, "Playlists"),
                section(MediaGroup.TYPE_SETTINGS, "Settings"), disabled);
        List<Integer> clicked = new ArrayList<>();
        int[] accountClicks = {0};
        MobileYouView view = new MobileYouView(context(), null, sections, () -> accountClicks[0]++,
                s -> clicked.add(s.getId()));
        for (String hidden : new String[]{"Home", "Subscriptions", "Shorts", "Disabled"}) assertNull(text(view, hidden));
        for (String label : new String[]{"History", "Playlists", "Settings"}) text(view, label).performClick();
        assertEquals(Arrays.asList(MediaGroup.TYPE_HISTORY, MediaGroup.TYPE_USER_PLAYLISTS, MediaGroup.TYPE_SETTINGS), clicked);
        text(view, context().getString(R.string.mobile_sign_in)).performClick();
        assertEquals(1, accountClicks[0]);
        LinearLayout shortcuts = (LinearLayout) view.getChildAt(1);
        assertEquals(2, shortcuts.getChildCount());
        assertEquals(LinearLayout.HORIZONTAL, shortcuts.getOrientation());
    }

    @Test @Config(sdk = 26, qualifiers = "w320dp-h640dp-port-mdpi")
    public void largeTextRtlMenuWrapsAndStacksShortcuts() {
        Context c = context();
        Configuration config = new Configuration(c.getResources().getConfiguration());
        config.fontScale = 2;
        c = new ContextThemeWrapper(c.createConfigurationContext(config), R.style.MobileTheme);
        String label = "A long translated playlist destination with several words";
        BrowseSection playlistSection = section(MediaGroup.TYPE_USER_PLAYLISTS, label);
        MobileYouView view = new MobileYouView(c, null,
                Arrays.asList(section(MediaGroup.TYPE_HISTORY, "History"), playlistSection),
                () -> {}, s -> {});
        view.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        view.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(3000, View.MeasureSpec.AT_MOST));
        view.layout(0, 0, 320, view.getMeasuredHeight());
        LinearLayout shortcuts = (LinearLayout) view.getChildAt(1);
        assertEquals(LinearLayout.VERTICAL, shortcuts.getOrientation());
        TextView playlist = text(view, playlistSection.getTitle());
        assertTrue(playlist.getTextSize() >= 28);
        assertEquals(Integer.MAX_VALUE, playlist.getMaxLines());
        assertNull(playlist.getEllipsize());
        assertTrue(playlist.getHeight() >= playlist.getLayout().getHeight() + playlist.getCompoundPaddingTop() + playlist.getCompoundPaddingBottom());
        assertTrue(playlist.getHeight() >= 48);
        assertTrue(playlist.getRight() <= shortcuts.getWidth());
    }

    @Test public void accountReplacementAndMissingAvatarKeepIdentityAndAction() {
        for (String name : new String[]{"First account", "Second account"}) {
            Account account = (Account) Proxy.newProxyInstance(Account.class.getClassLoader(), new Class[]{Account.class},
                    (proxy, method, args) -> method.getName().equals("getName") ? name : null);
            int[] clicks = {0};
            MobileYouView view = new MobileYouView(context(), account, Collections.emptyList(), () -> clicks[0]++, s -> {});
            assertNotNull(text(view, name));
            ImageView avatar = (ImageView) ((LinearLayout) view.getChildAt(0)).getChildAt(0);
            assertNotNull(avatar.getDrawable());
            assertEquals(64, avatar.getLayoutParams().width);
            text(view, context().getString(R.string.mobile_switch_account)).performClick();
            assertEquals(1, clicks[0]);
        }
    }

    @Test @Config(qualifiers = "w411dp-h800dp-port-mdpi")
    public void shortcutsStayCompactAndSideBySideAt150PercentText() {
        Context c = context();
        Configuration config = new Configuration(c.getResources().getConfiguration());
        config.fontScale = 1.5f;
        c = new ContextThemeWrapper(c.createConfigurationContext(config), R.style.MobileTheme);
        MobileYouView view = new MobileYouView(c, null,
                Arrays.asList(section(MediaGroup.TYPE_HISTORY, "History"), section(MediaGroup.TYPE_USER_PLAYLISTS, "Playlists")),
                () -> {}, s -> {});
        view.measure(View.MeasureSpec.makeMeasureSpec(411, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST));
        view.layout(0, 0, 411, view.getMeasuredHeight());
        LinearLayout shortcuts = (LinearLayout) view.getChildAt(1);
        assertEquals(LinearLayout.HORIZONTAL, shortcuts.getOrientation());
        for (int i = 0; i < shortcuts.getChildCount(); i++) {
            Button shortcut = (Button) shortcuts.getChildAt(i);
            assertTrue(shortcut.getHeight() >= 48);
            assertTrue(shortcut.getHeight() <= 60);
            assertEquals(android.view.Gravity.CENTER, shortcut.getGravity());
            assertTrue(shortcut.getWidth() < 190);
        }
    }

    @Test public void submenuSwitchRadioAndCheckboxRetainCallbacksAndBack() {
        org.robolectric.android.controller.ActivityController<MobileDialogActivity> controller =
                Robolectric.buildActivity(MobileDialogActivity.class).create();
        MobileDialogActivity activity = controller.get();
        int[] changes = {0};
        OptionItem toggle = UiOptionItem.from("Enable feature", "Description", item -> changes[0]++, false);
        OptionItem check = UiOptionItem.from("Multi select", item -> changes[0]++, false);
        OptionItem first = UiOptionItem.from("First choice", item -> changes[0]++, true);
        OptionItem second = UiOptionItem.from("Second choice", item -> changes[0]++, false);
        first.setRadio(first, second);
        second.setRadio(first, second);
        activity.show(Arrays.asList(OptionCategory.singleSwitch(toggle),
                OptionCategory.checkedList("Features", Collections.singletonList(check)),
                OptionCategory.radioList("Choices", Arrays.asList(first, second))), "Settings", false, false, false, 1);
        View root = activity.getWindow().getDecorView();
        TextView switchView = text(root, "Enable feature\nDescription");
        assertTrue(switchView instanceof Switch);
        switchView.performClick();
        assertTrue(toggle.isSelected());
        assertTrue(((Switch) switchView).isChecked());
        TextView checkbox = text(root, "Multi select");
        assertTrue(checkbox instanceof CheckBox);
        checkbox.performClick();
        assertTrue(check.isSelected());
        TextView radio = text(root, "Second choice");
        assertTrue(radio instanceof RadioButton);
        radio.performClick();
        assertTrue(second.isSelected());
        assertFalse(first.isSelected());
        assertFalse(((RadioButton) text(root, "First choice")).isChecked());
        assertEquals(4, changes[0]);
        activity.show(Collections.emptyList(), "Nested", false, false, false, 2);
        activity.onBackPressed();
        assertTrue(((Switch) text(root, "Enable feature\nDescription")).isChecked());
        assertTrue(((RadioButton) text(root, "Second choice")).isChecked());
        controller.destroy();
    }

    @Test public void sectionRefreshKeepsYouVisibleAndRemovesDisabledRows() {
        org.robolectric.android.controller.ActivityController<MobileBrowseActivity> controller =
                Robolectric.buildActivity(MobileBrowseActivity.class).create();
        MobileBrowseActivity activity = controller.get();
        activity.removeAllSections();
        activity.addSection(0, section(MediaGroup.TYPE_HISTORY, "History"));
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(activity, "showYou");
        activity.removeAllSections();
        activity.addSection(0, section(MediaGroup.TYPE_USER_PLAYLISTS, "Playlists"));
        activity.selectSection(0, false);
        View root = activity.getWindow().getDecorView();
        assertNotNull(text(root, "Playlists"));
        assertNull(text(root, "History"));
        assertTrue(org.robolectric.util.ReflectionHelpers.<Boolean>getField(activity, "you"));
        controller.destroy();
    }

    @Test public void topTabsPrioritizeLibraryAndKeepCategoryDestinationsOnlyInYou() {
        org.robolectric.android.controller.ActivityController<MobileBrowseActivity> controller =
                Robolectric.buildActivity(MobileBrowseActivity.class).create();
        MobileBrowseActivity activity = controller.get();
        activity.removeAllSections();
        List<BrowseSection> sections = Arrays.asList(
                section(MediaGroup.TYPE_KIDS_HOME, "Kids"), section(MediaGroup.TYPE_SPORTS, "Sports"),
                section(MediaGroup.TYPE_LIVE, "LIVE"), section(MediaGroup.TYPE_GAMING, "Gaming"),
                section(MediaGroup.TYPE_NEWS, "News"), section(MediaGroup.TYPE_CHANNEL_UPLOADS, "Channels"),
                section(MediaGroup.TYPE_HISTORY, "History"), section(MediaGroup.TYPE_USER_PLAYLISTS, "Playlists"),
                section(MediaGroup.TYPE_SUBSCRIPTIONS, "Subscriptions"), section(MediaGroup.TYPE_MUSIC, "Music"),
                section(MediaGroup.TYPE_HOME, "Home"));
        for (BrowseSection s : sections) activity.addSection(-1, s);
        LinearLayout chips = org.robolectric.util.ReflectionHelpers.getField(activity, "chips");
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < chips.getChildCount(); i++) labels.add(((TextView) chips.getChildAt(i)).getText().toString());
        assertEquals(Arrays.asList("Home", "Music", "Subscriptions", "Playlists", "History"), labels);
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(activity, "showYou");
        View root = activity.getWindow().getDecorView();
        for (String label : new String[]{"Kids", "Sports", "LIVE", "Gaming", "News", "Channels"})
            assertNotNull(text(root, label));
        controller.destroy();
    }
}
