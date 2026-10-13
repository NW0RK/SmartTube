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
    private final LinearLayout status;
    private boolean busy, completed, failed;
    private String emptyText;

    public MobileFeedView(
            Context c, Consumer<Video> click, Consumer<Video> menu, Consumer<Video> more) {
        super(c);
        setOrientation(VERTICAL);
        setBackgroundColor(MobileUi.background(c));
        emptyText = c.getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_empty);
        status = new LinearLayout(c);
        status.setOrientation(VERTICAL);
        status.setGravity(android.view.Gravity.CENTER);
        int padding = c.getResources().getDimensionPixelSize(com.liskovsoft.smartyoutubetv2.tv.R.dimen.mobile_page_padding);
        status.setPadding(padding, padding, padding, padding);
        progress = new ProgressBar(c);
        progress.setContentDescription(c.getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_loading));
        status.addView(progress, new LayoutParams(MobileUi.dp(c, 48), MobileUi.dp(c, 48)));
        progress.setVisibility(GONE);
        message =
                MobileUi.text(
                        c,
                        c.getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_empty),
                        16);
        message.setGravity(android.view.Gravity.CENTER);
        message.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        status.addView(message);
        retry =
                MobileUi.browseButton(
                        c,
                        c.getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_retry),
                        () -> {});
        status.addView(retry);
        retry.setVisibility(GONE);
        list = new RecyclerView(c);
        adapter = new MobileFeedAdapter(click, menu, more);
        list.setAdapter(adapter);
        list.setItemAnimator(null);
        resize();
        addView(list, new LayoutParams(-1, 0, 1));
        addView(status, new LayoutParams(-1, 0, 1));
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() { render(); }
        });
        render();
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
        completed = true;
        failed = false;
        render();
    }

    public void clear() {
        adapter.clear();
        completed = failed = false;
        message.setText(
                getContext().getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_empty));
        message.setVisibility(GONE);
        retry.setVisibility(GONE);
        retry.setText(
                getContext().getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_retry));
        if (retryAction != null) retry.setOnClickListener(v -> retryAction.run());
        render();
    }

    public void loading(boolean value) {
        if (busy && !value) completed = true;
        busy = value;
        if (value) failed = false;
        render();
    }

    public void idle() {
        busy = false;
        clear();
    }

    public boolean loading() {
        return busy;
    }

    public void error(String text, String action, Runnable callback) {
        busy = false;
        failed = completed = true;
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
        render();
    }

    public void setEmptyText(String text) {
        emptyText = text;
        render();
    }

    private void render() {
        boolean empty = adapter.isEmpty();
        boolean visible = busy || failed || empty && completed;
        // Keep header-bearing feeds (watch details and channel banners) visible.
        boolean hasRows = adapter.getItemCount() > 0;
        list.setVisibility(hasRows ? VISIBLE : GONE);
        status.setVisibility(visible ? VISIBLE : GONE);
        status.setLayoutParams(new LayoutParams(-1, hasRows ? -2 : 0, hasRows ? 0 : 1));
        progress.setVisibility(busy ? VISIBLE : GONE);
        message.setVisibility(!busy && (failed || empty && completed) ? VISIBLE : GONE);
        if (!failed) message.setText(emptyText);
        if (!failed) retry.setVisibility(GONE);
    }
}
