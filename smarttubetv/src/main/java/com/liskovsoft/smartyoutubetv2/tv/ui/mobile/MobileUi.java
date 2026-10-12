package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;

final class MobileUi {
    static final int BG = Color.rgb(15, 15, 15),
            SURFACE = Color.rgb(35, 35, 35),
            ACCENT = Color.rgb(255, 61, 61);

    static int background(Context c) {
        return schemeColor(c, com.liskovsoft.smartyoutubetv2.tv.R.attr.shelfBackground, BG, false);
    }

    static int accent(Context c) {
        return schemeColor(
                c,
                com.liskovsoft.smartyoutubetv2.tv.R.attr.playbackControlsIconHighlightColor,
                ACCENT,
                true);
    }

    private static int schemeColor(Context c, int attr, int fallback, boolean player) {
        com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData.ColorScheme scheme =
                com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData.instance(c).getColorScheme();
        int theme = player ? scheme.playerThemeResId : scheme.browseThemeResId;
        if (theme <= 0) return fallback;
        android.util.TypedValue value = new android.util.TypedValue();
        android.view.ContextThemeWrapper themed = new android.view.ContextThemeWrapper(c, theme);
        if (!themed.getTheme().resolveAttribute(attr, value, true)) return fallback;
        if (value.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) return value.data;
        return fallback;
    }

    static int dp(Context c, float n) {
        return Math.round(n * c.getResources().getDisplayMetrics().density);
    }

    static LinearLayout column(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setBackgroundColor(background(c));
        return v;
    }

    static TextView text(Context c, CharSequence text, int size) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
        return v;
    }

    static Button button(Context c, CharSequence title, Runnable action) {
        Button b = new Button(c);
        b.setText(title);
        String glyph = String.valueOf(title);
        if ("‹".equals(glyph)) b.setContentDescription("Back");
        else if ("⚙".equals(glyph)) b.setContentDescription("Playback settings");
        else if ("◀".equals(glyph)) b.setContentDescription("Previous video");
        else if ("▶|".equals(glyph)) b.setContentDescription("Next video");
        else if ("🎙".equals(glyph)) b.setContentDescription("Voice search");
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setMinHeight(dp(c, 48));
        b.setMinWidth(dp(c, 48));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(SURFACE);
        bg.setCornerRadius(dp(c, 20));
        b.setBackground(bg);
        b.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    static android.widget.ImageButton iconButton(
            Context c, int icon, String label, Runnable action) {
        android.widget.ImageButton button = new android.widget.ImageButton(c);
        button.setImageResource(icon);
        button.setColorFilter(Color.WHITE);
        button.setContentDescription(label);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(c, 12), dp(c, 12), dp(c, 12), dp(c, 12));
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    static Button navigationButton(Context c, int icon, String label, Runnable action) {
        Button button = button(c, label, action);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(0, dp(c, 4), 0, dp(c, 4));
        button.setTextSize(11);
        button.setMaxLines(1);
        android.graphics.drawable.Drawable drawable = c.getDrawable(icon).mutate();
        drawable.setTint(Color.WHITE);
        drawable.setBounds(0, 0, dp(c, 24), dp(c, 24));
        button.setCompoundDrawables(null, drawable, null, null);
        return button;
    }

    static LinearLayout toolbar(Context c, CharSequence title, Runnable back) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (back != null)
            row.addView(button(c, "‹", back), new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
        TextView name = text(c, title, 20);
        name.setMinHeight(dp(c, 56));
        name.setGravity(Gravity.CENTER_VERTICAL);
        name.setMaxLines(2);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    static void addButton(LinearLayout row, Button button) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(row.getContext(), 48));
        p.setMargins(
                dp(row.getContext(), 4),
                dp(row.getContext(), 4),
                dp(row.getContext(), 4),
                dp(row.getContext(), 4));
        row.addView(button, p);
    }
}
