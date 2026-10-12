package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.Context;
import android.graphics.*;
import android.widget.SeekBar;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;

import java.util.*;

// Framework widgets are intentional: the phone frontend uses the native Material theme.
@android.annotation.SuppressLint("AppCompatCustomView")
public final class MobileSegmentSeekBar extends SeekBar {
    private List<SeekBarSegment> segments = Collections.emptyList();
    private final Paint paint = new Paint();

    public MobileSegmentSeekBar(Context c) {
        super(c);
        setMax(10000);
        setMinimumHeight(MobileUi.dp(c, 48));
    }

    public void setSegments(List<SeekBarSegment> value) {
        segments = value == null ? Collections.emptyList() : new ArrayList<>(value);
        invalidate();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        for (SeekBarSegment s : segments) {
            paint.setColor(s.color);
            canvas.drawRect(
                    getPaddingLeft() + s.startProgress * width,
                    getHeight() / 2f - 3,
                    getPaddingLeft() + s.endProgress * width,
                    getHeight() / 2f + 3,
                    paint);
        }
    }
}
