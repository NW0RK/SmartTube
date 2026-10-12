package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/** Measures thumbnails at 16:9 on the first layout, including after a width change. */
// Framework widgets are intentional: the phone frontend uses the native Material theme.
@android.annotation.SuppressLint("AppCompatCustomView")
public final class MobileThumbnailView extends ImageView {
    public MobileThumbnailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        super.onMeasure(
                widthSpec, MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY));
    }
}
