package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Opaque navigation reserves measured space, including font scaling and window insets. */
final class MobileNavigationShell extends FrameLayout {
    static final String DESTINATION = "mobile_destination";
    static final int YOU = -2;

    static int requestedDestination(android.os.Bundle savedState, Intent intent) {
        // Restored selection belongs to the user; a consumed launch request must not override it.
        return savedState == null ? intent.getIntExtra(DESTINATION, -1) : -1;
    }
    private final View content;
    private final LinearLayout navigation;
    private final int contentBottom;
    private final java.util.Map<View, Integer> scrollPadding = new java.util.WeakHashMap<>();
    private boolean keyboard, scrollHidden;
    private final java.util.Set<View> observedScrollers = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    MobileNavigationShell(Context c, View content, LinearLayout navigation) {
        super(c);
        this.content = content;
        this.navigation = navigation;
        contentBottom = content.getPaddingBottom();
        setBackgroundColor(MobileUi.background(c));
        addView(content, new LayoutParams(-1, -1));
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        navigation.setPadding(MobileUi.dp(c, 8), 0, MobileUi.dp(c, 8), 0);
        navigation.setBackgroundColor(c.getColor(R.color.mobile_surface));
        navigation.setMinimumHeight(MobileUi.dp(c, 64));
        navigation.setElevation(0);
        LayoutParams params = new LayoutParams(-1, -2, Gravity.BOTTOM);
        addView(navigation, params);
        navigation.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updateClearance());
        setOnApplyWindowInsetsListener((v, insets) -> {
            int bottom = insets.getSystemWindowInsetBottom();
            if (android.os.Build.VERSION.SDK_INT >= 30)
                bottom = insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom;
            else if (bottom > MobileUi.dp(c, 160)) bottom = 0;
            setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), bottom);
            if (android.os.Build.VERSION.SDK_INT >= 30)
                setKeyboardVisible(insets.isVisible(android.view.WindowInsets.Type.ime()));
            return insets.consumeSystemWindowInsets();
        });
        // adjustResize supplies a smaller window on Android 8–10. Hide navigation above the IME.
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            updateClearance();
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                // The decor fits system windows and can consume IME visibility before it reaches us.
                android.view.WindowInsets window = getRootWindowInsets();
                if (window != null) setKeyboardVisible(window.isVisible(android.view.WindowInsets.Type.ime()));
            } else {
                android.graphics.Rect visible = new android.graphics.Rect();
                getWindowVisibleDisplayFrame(visible);
                setKeyboardVisible(getRootView().getHeight() - visible.bottom > MobileUi.dp(c, 160));
            }
        });
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        padScrollingContent(content, 0);
        if (!keyboard && !scrollHidden) measureChildWithMargins(navigation, widthSpec, 0, heightSpec, 0);
        LayoutParams params = (LayoutParams) content.getLayoutParams();
        int clearance = keyboard || scrollHidden ? 0 : navigation.getMeasuredHeight();
        if (params.bottomMargin != clearance) {
            params.bottomMargin = clearance;
            content.setLayoutParams(params);
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestApplyInsets();
        if (getContext() instanceof MobileActivity && navigation.getChildCount() > 0)
            accountAvatar((Button) navigation.getChildAt(navigation.getChildCount() - 1));
    }

    void setKeyboardVisible(boolean visible) {
        keyboard = visible;
        navigation.setVisibility(visible || scrollHidden ? GONE : VISIBLE);
        updateClearance();
    }

    void revealNavigation() {
        scrollHidden = false;
        navigation.setVisibility(keyboard ? GONE : VISIBLE);
        updateClearance();
    }

    void onUserScroll(int dy) {
        if (dy == 0 || keyboard) return;
        boolean hide = dy > 0;
        if (scrollHidden == hide) return;
        scrollHidden = hide;
        navigation.setVisibility(hide ? GONE : VISIBLE);
        updateClearance();
    }

    private void updateClearance() {
        int clearance = keyboard || scrollHidden ? 0 : navigation.getHeight();
        LayoutParams params = (LayoutParams) content.getLayoutParams();
        if (params.bottomMargin != clearance) {
            params.bottomMargin = clearance;
            content.setLayoutParams(params);
        }
        padScrollingContent(content, 0);
        content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(), contentBottom);
    }

    private boolean padScrollingContent(View view, int clearance) {
        if (view instanceof ScrollView || view instanceof androidx.recyclerview.widget.RecyclerView) {
            if (observedScrollers.add(view)) {
                if (view instanceof androidx.recyclerview.widget.RecyclerView) {
                    ((androidx.recyclerview.widget.RecyclerView) view).addOnScrollListener(
                        new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                            @Override public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView list, int dx, int dy) {
                                if (list.getScrollState() != androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE)
                                    onUserScroll(dy);
                            }
                        });
                } else {
                    final boolean[] userScroll = {false};
                    final Runnable idle = () -> userScroll[0] = false;
                    view.setOnTouchListener((v, event) -> {
                        if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN
                                || event.getActionMasked() == android.view.MotionEvent.ACTION_MOVE) {
                            v.removeCallbacks(idle);
                            userScroll[0] = true;
                        } else if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP
                                || event.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) {
                            v.postDelayed(idle, 250);
                        }
                        return false;
                    });
                    view.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
                        if (!userScroll[0]) return;
                        onUserScroll(y - oldY);
                        v.removeCallbacks(idle);
                        v.postDelayed(idle, 250);
                    });
                }
            }
            if (!scrollPadding.containsKey(view)) scrollPadding.put(view, view.getPaddingBottom());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                    scrollPadding.get(view) + clearance);
            ((ViewGroup) view).setClipToPadding(false);
            return true;
        }
        boolean found = false;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
                found |= padScrollingContent(group.getChildAt(i), clearance);
        }
        return found;
    }

    static LinearLayout destinations(MobileActivity activity) {
        LinearLayout row = new LinearLayout(activity);
        addDestination(row, activity, R.drawable.icon_home, R.string.header_home, MediaGroup.TYPE_HOME);
        addDestination(row, activity, R.drawable.mobile_history, R.string.header_history, MediaGroup.TYPE_HISTORY);
        addDestination(row, activity, R.drawable.ic_user, R.string.mobile_you, YOU);
        return row;
    }

    private static void addDestination(LinearLayout row, MobileActivity activity, int icon, int label, int destination) {
        MobileUi.addNavigation(row, MobileUi.browseNavigation(activity, icon, activity.getString(label), () -> {
            Intent intent = new Intent(activity, MobileBrowseActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(DESTINATION, destination);
            activity.startActivity(intent);
        }));
    }

    static void accountAvatar(Button button) {
        Account account = MediaServiceManager.instance().getSelectedAccount();
        String url = account == null ? null : account.getAvatarImageUrl();
        if (url == null || url.isEmpty()) {
            Glide.with(button).clear(button);
            button.setTag(R.id.mobile_navigation_avatar, false);
            Drawable fallback = button.getContext().getDrawable(R.drawable.mobile_account).mutate();
            fallback.setBounds(0, 0, MobileUi.dp(button.getContext(), 24), MobileUi.dp(button.getContext(), 24));
            MobileUi.navigationIcon(button, fallback);
            MobileUi.navigationState(button, button.isSelected());
            return;
        }
        Glide.with(button).load(url).circleCrop().into(new CustomViewTarget<Button, Drawable>(button) {
            @Override public void onResourceReady(@NonNull Drawable drawable, @Nullable Transition<? super Drawable> transition) {
                button.setTag(R.id.mobile_navigation_avatar, true);
                drawable.setBounds(0, 0, MobileUi.dp(button.getContext(), 24), MobileUi.dp(button.getContext(), 24));
                MobileUi.navigationIcon(button, drawable);
            }
            @Override protected void onResourceCleared(@Nullable Drawable placeholder) { }
            @Override public void onLoadFailed(@Nullable Drawable error) { }
        });
    }
}
