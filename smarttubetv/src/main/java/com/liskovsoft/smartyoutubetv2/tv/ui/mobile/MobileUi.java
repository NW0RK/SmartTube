package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import com.liskovsoft.smartyoutubetv2.tv.R;

final class MobileUi {
    static GradientDrawable rounded(Context c, int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(c.getResources().getDimension(R.dimen.mobile_corner));
        return shape;
    }

    static void browseFeedback(View view, boolean selectable) {
        Context c = view.getContext();
        StateListDrawable states = new StateListDrawable();
        if (selectable) states.addState(new int[]{android.R.attr.state_selected},
                rounded(c, Color.argb(55, Color.red(accent(c)), Color.green(accent(c)), Color.blue(accent(c)))));
        states.addState(new int[]{}, rounded(c, c.getColor(R.color.mobile_surface)));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(c.getColor(R.color.mobile_feedback)),
                states, rounded(c, Color.WHITE)));
    }

    static Button browseButton(Context c, CharSequence title, Runnable action) {
        Button b = button(c, title, action);
        browseFeedback(b, true);
        b.setTextColor(new ColorStateList(new int[][]{
                new int[]{-android.R.attr.state_enabled}, new int[]{android.R.attr.state_selected}, new int[]{}},
                new int[]{c.getColor(R.color.mobile_text_secondary), accent(c), Color.WHITE}));
        return b;
    }

    static void navigationState(Button button, boolean selected) {
        button.setSelected(selected);
        int color = selected ? accent(button.getContext()) : Color.WHITE;
        button.setTextColor(color);
        if (button.getForeground() != null) button.getForeground().setTint(color);
        for (android.graphics.drawable.Drawable icon : button.getCompoundDrawables())
            if (icon != null && !Boolean.TRUE.equals(button.getTag(R.id.mobile_navigation_avatar))) icon.setTint(color);
    }

    static ImageButton browseIcon(Context c, int icon, String label, Runnable action) {
        ImageButton b = iconButton(c, icon, label, action);
        browseFeedback(b, false);
        b.setMinimumWidth(dp(c, 48));
        b.setMinimumHeight(dp(c, 48));
        return b;
    }

    static LinearLayout searchToolbar(Context c, EditText query, ImageButton clear,
            Runnable back, Runnable voice) {
        LinearLayout bar = new LinearLayout(c);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(browseIcon(c, R.drawable.mobile_back, c.getString(R.string.mobile_back), back),
                new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
        LinearLayout field = new LinearLayout(c);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setBackground(rounded(c, c.getColor(R.color.mobile_surface)));
        query.setSingleLine(true);
        query.setTextColor(Color.WHITE);
        query.setHintTextColor(c.getColor(R.color.mobile_text_secondary));
        query.setTextSize(16);
        query.setMinHeight(dp(c, 48));
        query.setPaddingRelative(dp(c, 12), dp(c, 8), 0, dp(c, 8));
        query.setBackgroundColor(Color.TRANSPARENT);
        field.addView(query, new LinearLayout.LayoutParams(0, -2, 1));
        field.addView(clear, new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
        bar.addView(field, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(browseIcon(c, R.drawable.mobile_mic, c.getString(R.string.mobile_voice_search), voice),
                new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
        bar.setPadding(0, dp(c, 8), 0, dp(c, 8));
        return bar;
    }
    static int background(Context c) {
        return c.getColor(R.color.mobile_background);
    }

    static int accent(Context c) {
        return c.getColor(R.color.mobile_accent);
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
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(false);
        b.setMinHeight(dp(c, 48));
        b.setMinWidth(dp(c, 48));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(c.getColor(R.color.mobile_surface));
        bg.setCornerRadius(dp(c, 28));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(c.getColor(R.color.mobile_feedback)), bg, null));
        b.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    static android.widget.ImageButton iconButton(
            Context c, int icon, String label, Runnable action) {
        android.widget.ImageButton button = new android.widget.ImageButton(c);
        if (icon == R.drawable.ic_user) icon = R.drawable.mobile_account;
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
        if (icon == R.drawable.icon_home) icon = R.drawable.mobile_home;
        else if (icon == R.drawable.icon_subscriptions) icon = R.drawable.mobile_subscriptions;
        else if (icon == R.drawable.ic_user) icon = R.drawable.mobile_account;
        Button button = button(c, label, action);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(0, dp(c, 4), 0, dp(c, 4));
        button.setTextSize(11);
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        button.setCompoundDrawablePadding(dp(c, 4));
        button.setMaxLines(1);
        android.graphics.drawable.Drawable drawable = c.getDrawable(icon).mutate();
        drawable.setTint(Color.WHITE);
        drawable.setBounds(0, 0, dp(c, 24), dp(c, 24));
        button.setCompoundDrawables(null, drawable, null, null);
        return button;
    }

    static void centerIcon(Button button, String name, int size) {
        int px = dp(button.getContext(), size);
        android.graphics.drawable.LayerDrawable icon = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[] {new MobilePlayerIcon(name)});
        icon.setLayerSize(0, px, px);
        icon.setLayerGravity(0, Gravity.CENTER);
        button.setText("");
        button.setCompoundDrawables(null, null, null, null);
        button.setPadding(0, 0, 0, 0);
        button.setForeground(icon);
        button.setForegroundGravity(Gravity.FILL);
    }

    static LinearLayout toolbar(Context c, CharSequence title, Runnable back) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPaddingRelative(dp(c, 8), dp(c, 4), dp(c, 8), dp(c, 4));
        row.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{c.getColor(R.color.mobile_surface), background(c)}));
        if (back != null)
            row.addView(iconButton(c, R.drawable.mobile_back, c.getString(R.string.mobile_back), back),
                    new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
        TextView name = text(c, title, 20);
        name.setMinHeight(dp(c, 56));
        name.setGravity(Gravity.CENTER_VERTICAL);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setMaxLines(2);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    static LinearLayout watchToolbar(Context c, Runnable back) {
        LinearLayout row = toolbar(c, "SmartTube", back);
        ImageButton collapse = (ImageButton) row.getChildAt(0);
        collapse.setImageResource(R.drawable.mobile_down);
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

    static void addBrowseButton(LinearLayout row, Button button) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                row.getOrientation() == LinearLayout.VERTICAL ? -1 : -2, -2);
        int space = dp(row.getContext(), 4);
        p.setMargins(space, space, space, space);
        row.addView(button, p);
    }

    static Button browseNavigation(Context c, int icon, String label, Runnable action) {
        Button button = navigationButton(c, icon, label, action);
        button.setContentDescription(label);
        button.setMinHeight(dp(c, 64));
        button.setMaxLines(2);
        GradientDrawable selected = rounded(c, 0x22ffffff);
        selected.setCornerRadius(dp(c, 32));
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_selected}, selected);
        states.addState(new int[]{}, rounded(c, Color.TRANSPARENT));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(c.getColor(R.color.mobile_feedback)), states, null));
        return button;
    }

    static void addNavigation(LinearLayout row, Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(dp(row.getContext(), 4), 0, dp(row.getContext(), 4), 0);
        row.addView(button, params);
    }

    static void styleInput(EditText input) {
        Context c = input.getContext();
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(c.getColor(R.color.mobile_text_secondary));
        input.setTextSize(16);
        input.setMinHeight(dp(c, 48));
        input.setPaddingRelative(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        input.setBackground(rounded(c, c.getColor(R.color.mobile_surface)));
    }
}
