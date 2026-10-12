package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.Context;
import android.widget.*;

import androidx.recyclerview.widget.*;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.*;

import java.util.function.Consumer;

public final class MobileFeedView extends LinearLayout {
    public final RecyclerView list;
    public final MobileFeedAdapter adapter;
    private final ProgressBar progress;
    private final TextView message;
    private final Button retry;
    private Runnable retryAction;

    public MobileFeedView(
            Context c, Consumer<Video> click, Consumer<Video> menu, Consumer<Video> more) {
        super(c);
        setOrientation(VERTICAL);
        setBackgroundColor(MobileUi.background(c));
        progress = new ProgressBar(c);
        addView(progress, new LayoutParams(-1, MobileUi.dp(c, 48)));
        progress.setVisibility(GONE);
        message =
                MobileUi.text(
                        c,
                        c.getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_empty),
                        16);
        addView(message);
        retry =
                MobileUi.button(
                        c,
                        c.getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_retry),
                        () -> {});
        addView(retry);
        retry.setVisibility(GONE);
        list = new RecyclerView(c);
        adapter = new MobileFeedAdapter(click, menu, more);
        list.setAdapter(adapter);
        list.setItemAnimator(null);
        resize();
        addView(list, new LayoutParams(-1, 0, 1));
    }

    public void setRetry(Runnable action) {
        retryAction = action;
        retry.setOnClickListener(v -> action.run());
    }

    public void resize() {
        GridLayoutManager layout =
                new GridLayoutManager(
                        getContext(),
                        getResources().getConfiguration().screenWidthDp >= 600 ? 2 : 1);
        layout.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int p) {
                        return adapter.isHeading(p) ? layout.getSpanCount() : 1;
                    }
                });
        android.os.Parcelable state =
                list.getLayoutManager() == null
                        ? null
                        : list.getLayoutManager().onSaveInstanceState();
        list.setLayoutManager(layout);
        if (state != null) layout.onRestoreInstanceState(state);
    }

    public void update(VideoGroup g) {
        adapter.update(g);
        message.setVisibility(adapter.isEmpty() ? VISIBLE : GONE);
        retry.setVisibility(GONE);
    }

    public void clear() {
        adapter.clear();
        message.setText(
                getContext().getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_empty));
        message.setVisibility(GONE);
        retry.setVisibility(GONE);
        retry.setText(
                getContext().getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_retry));
        if (retryAction != null) retry.setOnClickListener(v -> retryAction.run());
    }

    public void loading(boolean value) {
        progress.setVisibility(value ? VISIBLE : GONE);
        if (!value && adapter.isEmpty()) {
            message.setVisibility(VISIBLE);
            retry.setVisibility(retryAction == null ? GONE : VISIBLE);
        }
    }

    public boolean loading() {
        return progress.getVisibility() == VISIBLE;
    }

    public void error(String text, String action, Runnable callback) {
        message.setText(text);
        message.setVisibility(VISIBLE);
        Runnable actionCallback = callback == null ? retryAction : callback;
        retry.setText(
                action == null
                        ? getContext()
                                .getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_retry)
                        : action);
        retry.setOnClickListener(
                v -> {
                    if (actionCallback != null) actionCallback.run();
                });
        retry.setVisibility(actionCallback == null ? GONE : VISIBLE);
    }
}
