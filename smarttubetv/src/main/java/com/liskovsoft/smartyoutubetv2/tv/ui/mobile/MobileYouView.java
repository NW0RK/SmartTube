package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.tv.R;
import java.util.List;
import java.util.function.Consumer;

/** Presentation of existing account and library destinations; no data requests. */
final class MobileYouView extends LinearLayout {
    MobileYouView(Context context, Account account, List<BrowseSection> sections,
            Runnable accountAction, Consumer<BrowseSection> select) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(MobileUi.background(context));
        setPadding(0, MobileUi.dp(context, 16), 0, MobileUi.dp(context, 24));
        LinearLayout items = this;
        LinearLayout profile = new LinearLayout(context);
        profile.setGravity(Gravity.CENTER_VERTICAL);
        profile.setPaddingRelative(MobileUi.dp(context, 16), MobileUi.dp(context, 8), MobileUi.dp(context, 16), MobileUi.dp(context, 16));
        ImageView avatar = new ImageView(context);
        avatar.setImageResource(R.drawable.mobile_account);
        avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        profile.addView(avatar, new LinearLayout.LayoutParams(MobileUi.dp(context, 64), MobileUi.dp(context, 64)));
        String avatarUrl = account == null ? null : account.getAvatarImageUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty())
            Glide.with(avatar).load(avatarUrl).circleCrop().placeholder(R.drawable.mobile_account)
                    .error(R.drawable.mobile_account).into(avatar);
        LinearLayout identity = MobileUi.column(context);
        TextView name = MobileUi.text(context, account == null ? context.getString(R.string.mobile_you)
                : account.getName() == null ? context.getString(R.string.mobile_account) : account.getName(), 24);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        identity.addView(name);
        Button accountButton = MobileUi.button(context,
                context.getString(account == null ? R.string.mobile_sign_in : R.string.mobile_switch_account),
                accountAction);
        MobileUi.menuControl(accountButton);
        accountButton.setTextSize(14);
        accountButton.setTextColor(context.getColor(R.color.mobile_text_secondary));
        identity.addView(accountButton);
        profile.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        items.addView(profile);
        LinearLayout shortcuts = MobileUi.column(context);
        boolean stacked = context.getResources().getConfiguration().screenWidthDp < 360
                || context.getResources().getConfiguration().fontScale >= 1.8f;
        shortcuts.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        shortcuts.setGravity(Gravity.CENTER_HORIZONTAL);
        shortcuts.setPaddingRelative(MobileUi.dp(context, 12), 0, MobileUi.dp(context, 12), MobileUi.dp(context, 8));
        for (int id : new int[]{MediaGroup.TYPE_HISTORY, MediaGroup.TYPE_USER_PLAYLISTS}) {
            for (BrowseSection s : sections) if (s.getId() == id && s.isEnabled()) {
                Button shortcut = MobileUi.menuRow(context, s.getTitle(), s.getResId(), () -> select.accept(s));
                android.graphics.drawable.Drawable icon = shortcut.getCompoundDrawablesRelative()[0];
                if (icon != null) icon.setBounds(0, 0, MobileUi.dp(context, 20), MobileUi.dp(context, 20));
                shortcut.setCompoundDrawablesRelative(icon, null, null, null);
                shortcut.setGravity(Gravity.CENTER);
                shortcut.setTextSize(14);
                shortcut.setMinHeight(MobileUi.dp(context, 48));
                shortcut.setMinimumHeight(MobileUi.dp(context, 48));
                shortcut.setPaddingRelative(MobileUi.dp(context, 12), MobileUi.dp(context, 10), MobileUi.dp(context, 12), MobileUi.dp(context, 10));
                shortcut.setCompoundDrawablePadding(MobileUi.dp(context, 8));
                shortcut.setBackground(new android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(context.getColor(R.color.mobile_feedback)),
                        MobileUi.rounded(context, context.getColor(R.color.mobile_surface)),
                        MobileUi.rounded(context, android.graphics.Color.WHITE)));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
                params.setMargins(MobileUi.dp(context, 4), MobileUi.dp(context, 4), MobileUi.dp(context, 4), MobileUi.dp(context, 4));
                shortcuts.addView(shortcut, params);
            }
        }
        if (shortcuts.getChildCount() > 0) items.addView(shortcuts);
        MobileUi.menuDivider(items);
        for (BrowseSection s : sections)
            if (s.isEnabled() && s.getId() != MediaGroup.TYPE_HOME
                    && s.getId() != MediaGroup.TYPE_SHORTS
                    && s.getId() != MediaGroup.TYPE_SUBSCRIPTIONS
                    && s.getId() != MediaGroup.TYPE_HISTORY
                    && s.getId() != MediaGroup.TYPE_USER_PLAYLISTS) {
                if (s.getId() == MediaGroup.TYPE_SETTINGS) MobileUi.menuDivider(items);
                items.addView(MobileUi.menuRow(context, s.getTitle(), s.getResId(), () -> select.accept(s)));
            }
    }
}
