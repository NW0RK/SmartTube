package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelUploadsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView;
import com.liskovsoft.smartyoutubetv2.tv.R;

public class MobileUploadsActivity extends MobileActivity implements ChannelUploadsView {
    private ChannelUploadsPresenter presenter;
    private MobileFeedView feed;
    private android.os.Parcelable pendingScroll;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        presenter = ChannelUploadsPresenter.instance(this);
        presenter.setView(this);
        if (state != null) {
            presenter.setChannel(
                    com.liskovsoft.smartyoutubetv2.common.app.models.data.Video.fromString(
                            state.getString("channel")));
            pendingScroll = state.getParcelable("scroll");
        }
        LinearLayout root = MobileUi.column(this);
        root.addView(MobileUi.toolbar(this, getString(R.string.header_playlists), this::finish));
        feed =
                new MobileFeedView(
                        this,
                        presenter::onVideoItemClicked,
                        presenter::onVideoItemLongClicked,
                        presenter::onScrollEnd);
        root.addView(feed, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(new MobileNavigationShell(this, root, MobileNavigationShell.destinations(this)));
        feed.setRetry(presenter::refresh);
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
