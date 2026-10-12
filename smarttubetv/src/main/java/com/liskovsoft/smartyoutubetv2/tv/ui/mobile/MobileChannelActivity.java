package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView;
import com.liskovsoft.smartyoutubetv2.tv.R;

public class MobileChannelActivity extends MobileActivity implements ChannelView {
    private ChannelPresenter presenter;
    private MobileFeedView feed;
    private EditText channelSearch;
    private android.os.Parcelable pendingScroll;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        presenter = ChannelPresenter.instance(this);
        presenter.setView(this);
        if (state != null) {
            presenter.setChannel(
                    com.liskovsoft.smartyoutubetv2.common.app.models.data.Video.fromString(
                            state.getString("channel")));
            pendingScroll = state.getParcelable("scroll");
            presenter.setChannelId(state.getString("channelId"));
        }
        LinearLayout root = MobileUi.column(this);
        CharSequence title =
                presenter.getChannel() == null
                        ? getString(R.string.header_channels)
                        : presenter.getChannel().getTitleFull();
        LinearLayout toolbar = MobileUi.toolbar(this, title, this::finish);
        toolbar.addView(
                MobileUi.iconButton(
                        this,
                        R.drawable.action_search,
                        getString(R.string.action_search),
                        () -> {
                            channelSearch.setVisibility(View.VISIBLE);
                            channelSearch.requestFocus();
                            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                    .showSoftInput(channelSearch, InputMethodManager.SHOW_IMPLICIT);
                        }),
                new LinearLayout.LayoutParams(MobileUi.dp(this, 48), MobileUi.dp(this, 48)));
        MobileUi.addButton(
                toolbar,
                MobileUi.button(
                        this,
                        getString(R.string.search_sorting),
                        presenter::onSearchSettingsClicked));
        root.addView(toolbar);
        channelSearch = new EditText(this);
        channelSearch.setSingleLine(true);
        channelSearch.setHint(getString(R.string.action_search));
        channelSearch.setTextColor(android.graphics.Color.WHITE);
        channelSearch.setMinHeight(MobileUi.dp(this, 48));
        channelSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        channelSearch.setVisibility(View.GONE);
        channelSearch.setOnEditorActionListener(
                (view, action, event) -> {
                    if (action == EditorInfo.IME_ACTION_SEARCH) {
                        presenter.onSearchSubmit(channelSearch.getText().toString());
                        channelSearch.clearFocus();
                        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                .hideSoftInputFromWindow(channelSearch.getWindowToken(), 0);
                        return true;
                    }
                    return false;
                });
        root.addView(channelSearch);
        feed =
                new MobileFeedView(
                        this,
                        presenter::onVideoItemClicked,
                        presenter::onVideoItemLongClicked,
                        presenter::onScrollEnd);
        root.addView(feed, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        feed.setRetry(presenter::onViewInitialized);
        presenter.onViewInitialized();
    }

    @Override
    public void update(VideoGroup g) {
        feed.update(g);
        if (pendingScroll != null && !feed.adapter.isEmpty()) {
            feed.list.getLayoutManager().onRestoreInstanceState(pendingScroll);
            pendingScroll = null;
        }
    }

    public void setPosition(int i) {
        feed.list.scrollToPosition(Math.max(0, i));
    }

    @Override
    public void clear() {
        feed.clear();
    }

    @Override
    public void showProgressBar(boolean show) {
        runOnUiThread(() -> feed.loading(show));
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        feed.resize();
    }

    @Override
    public void finish() {
        presenter.onFinish();
        super.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) presenter.onViewResumed();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (presenter.getChannel() != null)
            state.putString("channel", presenter.getChannel().toString());
        state.putString("channelId", presenter.getChannelId());
        state.putParcelable("scroll", feed.list.getLayoutManager().onSaveInstanceState());
    }

    @Override
    protected void onDestroy() {
        if (presenter != null && presenter.getView() == this) {
            presenter.onViewDestroyed();
            presenter.setView(null);
        }
        super.onDestroy();
    }
}
