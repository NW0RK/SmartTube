package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.PathInterpolator;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Interruptible control motion using the system animator duration scale. */
final class MobileMotion {
    private MobileMotion() {}

    static void enter(View view) {
        boolean interrupted = Boolean.TRUE.equals(view.getTag(R.id.mobile_enter_animating));
        view.animate().cancel();
        if (!ValueAnimator.areAnimatorsEnabled()) {
            view.setAlpha(1f);
            view.setTranslationY(0f);
            view.setTag(R.id.mobile_enter_animating, false);
            return;
        }
        if (!interrupted) {
            view.setAlpha(0f);
            view.setTranslationY(MobileUi.dp(view.getContext(), 8));
        }
        view.setTag(R.id.mobile_enter_animating, true);
        view.animate().alpha(1f).translationY(0f).setDuration(280)
                .setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f))
                .withEndAction(() -> view.setTag(R.id.mobile_enter_animating, false)).start();
    }

    static void controls(View view, boolean show) {
        view.setTag(R.id.mobile_controls_requested, show);
        view.animate().cancel();
        if (!ValueAnimator.areAnimatorsEnabled()) {
            view.setAlpha(show ? 1f : 0f);
            view.setVisibility(show ? View.VISIBLE : View.GONE);
            return;
        }
        if (show && view.getVisibility() != View.VISIBLE) {
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
        }
        view.animate().alpha(show ? 1f : 0f).setDuration(180)
                .setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f))
                .withEndAction(() -> {
                    if (!show) view.setVisibility(View.GONE);
                }).start();
    }
}
