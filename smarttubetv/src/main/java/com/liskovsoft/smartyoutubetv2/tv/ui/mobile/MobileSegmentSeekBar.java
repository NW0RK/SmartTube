package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;

import java.util.*;

// Framework widgets are intentional: the phone frontend uses the native Material theme.
@android.annotation.SuppressLint("AppCompatCustomView")
public final class MobileSegmentSeekBar extends SeekBar {
    private List<SeekBarSegment> segments = Collections.emptyList();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private OnSeekBarChangeListener listener;
    private boolean tracking, touchChange, cancelled;
    private int originalProgress, pointerId;

    public MobileSegmentSeekBar(Context c) {
        super(c);
        setMax(10000);
        setMinimumHeight(MobileUi.dp(c, 48));
        setPadding(MobileUi.dp(c, 16), 0, MobileUi.dp(c, 16), 0);
        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        setThumb(null);
        setProgressDrawable(null);
        setBackground(null);
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                if (listener != null) listener.onProgressChanged(bar, progress, user || touchChange);
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        });
    }

    @Override public void setOnSeekBarChangeListener(OnSeekBarChangeListener value) {
        listener = value;
    }

    boolean wasCancelled() { return cancelled; }

    float progressX() {
        return getPaddingLeft() + (getWidth() - getPaddingLeft() - getPaddingRight())
                * getProgress() / (float) getMax();
    }

    private void track(float x) {
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        if (width <= 0) return;
        touchChange = true;
        setProgress(Math.round(getMax() * Math.max(0, Math.min(1, (x - getPaddingLeft()) / width))));
        touchChange = false;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelled = false;
                originalProgress = getProgress();
                pointerId = event.getPointerId(0);
                tracking = true;
                setPressed(true);
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                if (listener != null) listener.onStartTrackingTouch(this);
                track(event.getX());
                break;
            case MotionEvent.ACTION_MOVE:
                int index = event.findPointerIndex(pointerId);
                if (tracking && index >= 0) track(event.getX(index));
                break;
            case MotionEvent.ACTION_UP:
                if (!tracking) return false;
                track(event.getX());
                finishTracking(false);
                performClick();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (tracking) finishTracking(true);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (tracking && event.getPointerId(event.getActionIndex()) == pointerId)
                    finishTracking(false);
                break;
        }
        return true;
    }

    private void finishTracking(boolean cancel) {
        cancelled = cancel;
        tracking = false;
        setPressed(false);
        if (cancel) setProgress(originalProgress);
        if (listener != null) listener.onStopTrackingTouch(this);
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
    }

    @Override public boolean performClick() { return super.performClick(); }

    public void setSegments(List<SeekBarSegment> value) {
        segments = value == null ? Collections.emptyList() : new ArrayList<>(value);
        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        float left = getPaddingLeft(), right = getWidth() - getPaddingRight();
        float width = Math.max(0, right - left), y = getHeight() / 2f;
        float half = MobileUi.dp(getContext(), isPressed() ? 3 : 2);
        paint.setColor(isEnabled() ? 0x55ffffff : 0x33ffffff);
        canvas.drawRoundRect(left, y - half, right, y + half, half, half, paint);
        paint.setColor(0x99ffffff);
        canvas.drawRoundRect(left, y - half, left + width * getSecondaryProgress() / getMax(),
                y + half, half, half, paint);
        paint.setColor(MobileUi.accent(getContext()));
        paint.setAlpha(isEnabled() ? 255 : 90);
        canvas.drawRoundRect(left, y - half, progressX(), y + half, half, half, paint);
        // Segment markers sit below the timeline so played and buffered positions stay clear.
        for (SeekBarSegment s : segments) {
            paint.setColor(s.color);
            paint.setAlpha(isEnabled() ? 180 : 60);
            float start = left + Math.max(0, Math.min(1, s.startProgress)) * width;
            float end = left + Math.max(0, Math.min(1, s.endProgress)) * width;
            if (end > start) canvas.drawRoundRect(start, y + MobileUi.dp(getContext(), 6),
                    end, y + MobileUi.dp(getContext(), 8), half, half, paint);
        }
        paint.setColor(MobileUi.accent(getContext()));
        if (isPressed()) {
            paint.setAlpha(55);
            canvas.drawCircle(progressX(), y, MobileUi.dp(getContext(), 15), paint);
        }
        paint.setAlpha(isEnabled() ? 255 : 90);
        canvas.drawCircle(progressX(), y, MobileUi.dp(getContext(), isPressed() ? 8 : 6), paint);
    }
}
