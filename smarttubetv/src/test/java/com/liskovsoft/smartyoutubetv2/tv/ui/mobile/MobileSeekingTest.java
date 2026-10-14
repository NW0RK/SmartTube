package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;

import android.app.Application;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;

import com.liskovsoft.smartyoutubetv2.tv.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class MobileSeekingTest {
    private MobileSegmentSeekBar bar() {
        MobileSegmentSeekBar bar = new MobileSegmentSeekBar(new ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.MobileTheme));
        bar.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(48, View.MeasureSpec.EXACTLY));
        bar.layout(0, 0, 400, 48);
        return bar;
    }

    private boolean touch(MobileSegmentSeekBar bar, int action, float fraction) {
        float x = bar.getPaddingLeft() + fraction * (bar.getWidth()
                - bar.getPaddingLeft() - bar.getPaddingRight());
        MotionEvent event = MotionEvent.obtain(0, 10, action, x, 24, 0);
        boolean handled = bar.onTouchEvent(event);
        event.recycle();
        return handled;
    }

    @Test public void tapsAndDragsUpdateImmediatelyAndCommitOnce() {
        MobileSegmentSeekBar bar = bar();
        int[] callbacks = {0, 0, 0};
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seek) { callbacks[0]++; }
            public void onProgressChanged(SeekBar seek, int progress, boolean user) {
                if (user) callbacks[1]++;
            }
            public void onStopTrackingTouch(SeekBar seek) { callbacks[2]++; }
        });
        assertTrue(touch(bar, MotionEvent.ACTION_DOWN, .25f));
        assertEquals(2500, bar.getProgress());
        assertEquals(1, callbacks[0]);
        assertTrue(bar.isPressed());
        touch(bar, MotionEvent.ACTION_MOVE, .75f);
        assertEquals(7500, bar.getProgress());
        touch(bar, MotionEvent.ACTION_UP, .75f);
        assertEquals(1, callbacks[2]);
        assertEquals(2, callbacks[1]);
        assertFalse(bar.isPressed());
        assertFalse(bar.wasCancelled());
    }

    @Test public void cancellationRestoresPositionAndIsReportedBeforeRelease() {
        MobileSegmentSeekBar bar = bar();
        bar.setProgress(3000);
        boolean[] cancelled = {false};
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seek) {}
            public void onProgressChanged(SeekBar seek, int progress, boolean user) {}
            public void onStopTrackingTouch(SeekBar seek) { cancelled[0] = bar.wasCancelled(); }
        });
        touch(bar, MotionEvent.ACTION_DOWN, .8f);
        touch(bar, MotionEvent.ACTION_CANCEL, .8f);
        assertEquals(3000, bar.getProgress());
        assertTrue(cancelled[0]);
        assertFalse(bar.isPressed());
        touch(bar, MotionEvent.ACTION_DOWN, .4f);
        touch(bar, MotionEvent.ACTION_UP, .4f);
        assertFalse(bar.wasCancelled());
        assertEquals(4000, bar.getProgress());
    }

    @Test public void trackEdgesClampAndDisabledTimelineIgnoresTouches() {
        MobileSegmentSeekBar bar = bar();
        touch(bar, MotionEvent.ACTION_DOWN, -.2f);
        assertEquals(0, bar.getProgress());
        touch(bar, MotionEvent.ACTION_MOVE, 1.2f);
        assertEquals(10000, bar.getProgress());
        touch(bar, MotionEvent.ACTION_UP, 1.2f);
        bar.setEnabled(false);
        assertFalse(touch(bar, MotionEvent.ACTION_DOWN, .5f));
        assertEquals(10000, bar.getProgress());
    }

    @Test public void accessibilityCanAdjustPositionWithoutTouchingTheTimeline() {
        MobileSegmentSeekBar bar = bar();
        bar.setProgress(5000);
        boolean[] userChange = {false};
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seek) {}
            public void onProgressChanged(SeekBar seek, int progress, boolean user) { userChange[0] = user; }
            public void onStopTrackingTouch(SeekBar seek) {}
        });
        assertTrue(bar.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null));
        assertTrue(bar.getProgress() > 5000);
        assertTrue(userChange[0]);
    }
}
