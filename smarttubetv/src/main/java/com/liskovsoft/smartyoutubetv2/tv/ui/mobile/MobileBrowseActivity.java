package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.res.Configuration;
import android.os.*;
import android.widget.*;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.*;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.*;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.*;

public class MobileBrowseActivity extends MobileActivity implements BrowseView {
    private final List<BrowseSection> sections = new ArrayList<>();
    private final Map<Integer, Parcelable> scroll = new HashMap<>();
    private BrowsePresenter presenter;
    private MobileFeedView feed;
    private LinearLayout chips, content, settings;
    private final Map<Integer, Button> tabs = new HashMap<>();
    private Parcelable pendingScroll;
    private int selected = -1;
    private boolean you;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        presenter = BrowsePresenter.instance(this);
        presenter.setView(this);
        LinearLayout root = MobileUi.column(this), top = MobileUi.toolbar(this, "SmartTube", null);
        top.addView(
                MobileUi.iconButton(
                        this,
                        R.drawable.action_search,
                        getString(R.string.action_search),
                        () -> SearchPresenter.instance(this).startSearch(null)),
                new LinearLayout.LayoutParams(MobileUi.dp(this, 48), MobileUi.dp(this, 48)));
        top.addView(
                MobileUi.iconButton(
                        this,
                        R.drawable.ic_user,
                        getString(R.string.mobile_account),
                        () -> AccountSelectionPresenter.instance(this).show(true)),
                new LinearLayout.LayoutParams(MobileUi.dp(this, 48), MobileUi.dp(this, 48)));
        root.addView(top);
        HorizontalScrollView strip = new HorizontalScrollView(this);
        chips = new LinearLayout(this);
        strip.setHorizontalScrollBarEnabled(false);
        strip.addView(chips);
        root.addView(strip);
        content = MobileUi.column(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        feed =
                new MobileFeedView(
                        this,
                        presenter::onVideoItemClicked,
                        presenter::onVideoItemLongClicked,
                        presenter::onScrollEnd);
        content.addView(feed, new LinearLayout.LayoutParams(-1, -1));
        feed.setRetry(() -> presenter.refresh());
        LinearLayout tabs = new LinearLayout(this);
        tab(tabs, getString(R.string.header_home), MediaGroup.TYPE_HOME, R.drawable.icon_home);
        tab(
                tabs,
                getString(R.string.header_subscriptions),
                MediaGroup.TYPE_SUBSCRIPTIONS,
                R.drawable.icon_subscriptions);
        tabs.addView(
                MobileUi.navigationButton(
                        this, R.drawable.ic_user, getString(R.string.mobile_you), this::showYou),
                new LinearLayout.LayoutParams(0, MobileUi.dp(this, 64), 1));
        root.addView(tabs);
        setContentView(root);
        presenter.onViewInitialized();
        if (state != null) {
            int id = state.getInt("section", -1);
            if (id >= 0)
                presenter.selectSection(id == MediaGroup.TYPE_SHORTS ? MediaGroup.TYPE_HOME : id);
            Parcelable position = state.getParcelable("scroll");
            if (position != null) pendingScroll = position;
            if (state.getBoolean("you")) showYou();
        }
    }

    private void tab(LinearLayout row, String title, int id, int icon) {
        Button button =
                MobileUi.navigationButton(
                        this,
                        icon,
                        title,
                        () -> {
                            you = false;
                            presenter.selectSection(id);
                        });
        tabs.put(id, button);
        row.addView(button, new LinearLayout.LayoutParams(0, MobileUi.dp(this, 64), 1));
    }

    private void rebuildChips() {
        if (chips == null) return;
        chips.removeAllViews();
        for (Map.Entry<Integer, Button> entry : tabs.entrySet()) {
            boolean enabled =
                    sections.stream().anyMatch(section -> section.getId() == entry.getKey());
            entry.getValue().setEnabled(enabled);
            entry.getValue().setAlpha(enabled ? 1f : 0.4f);
            entry.getValue().setSelected(!you && selected == entry.getKey());
            entry.getValue()
                    .setTextColor(
                            !you && selected == entry.getKey()
                                    ? MobileUi.accent(this)
                                    : android.graphics.Color.WHITE);
        }
        for (BrowseSection s : sections) {
            if (s.getId() == MediaGroup.TYPE_SHORTS) continue;
            Button button =
                    MobileUi.button(
                            this,
                            s.getTitle(),
                            () -> {
                                you = false;
                                presenter.selectSection(s.getId());
                            });
            button.setTextColor(
                    selected == s.getId() ? MobileUi.accent(this) : android.graphics.Color.WHITE);
            button.setOnLongClickListener(
                    v -> {
                        presenter.onSectionLongPressed(s.getId());
                        return true;
                    });
            MobileUi.addButton(chips, button);
        }
    }

    private void showYou() {
        rememberScroll();
        you = true;
        rebuildChips();
        content.removeAllViews();
        ScrollView view = new ScrollView(this);
        LinearLayout items = MobileUi.column(this);
        view.addView(items);
        content.addView(view);
        items.addView(MobileUi.text(this, getString(R.string.mobile_you), 24));
        items.addView(
                MobileUi.button(
                        this,
                        getString(R.string.mobile_account),
                        () -> AccountSelectionPresenter.instance(this).show(true)));
        for (BrowseSection s : sections)
            if (s.getId() != MediaGroup.TYPE_HOME
                    && s.getId() != MediaGroup.TYPE_SHORTS
                    && s.getId() != MediaGroup.TYPE_SUBSCRIPTIONS)
                items.addView(
                        MobileUi.button(
                                this,
                                s.getTitle(),
                                () -> {
                                    you = false;
                                    presenter.selectSection(s.getId());
                                }));
    }

    private void rememberScroll() {
        if (selected >= 0 && feed.list.getLayoutManager() != null)
            scroll.put(selected, feed.list.getLayoutManager().onSaveInstanceState());
    }

    @Override
    public void addSection(int index, BrowseSection s) {
        removeSection(s);
        sections.add(index >= 0 && index <= sections.size() ? index : sections.size(), s);
        rebuildChips();
    }

    @Override
    public void removeSection(BrowseSection s) {
        if (s != null) sections.removeIf(x -> x.getId() == s.getId());
        rebuildChips();
    }

    @Override
    public void removeAllSections() {
        sections.clear();
        rebuildChips();
    }

    @Override
    public void selectSection(int index, boolean focus) {
        if (sections.isEmpty() || index < 0) return;
        BrowseSection s = sections.get(Math.min(index, sections.size() - 1));
        if (s.getId() == MediaGroup.TYPE_SHORTS) {
            presenter.selectSection(MediaGroup.TYPE_HOME);
            return;
        }
        rememberScroll();
        boolean changed = selected != s.getId();
        selected = s.getId();
        you = false;
        content.removeAllViews();
        content.addView(feed, new LinearLayout.LayoutParams(-1, -1));
        if (changed) feed.clear();
        rebuildChips();
        presenter.onSectionFocused(selected);
        Parcelable position = scroll.get(selected);
        pendingScroll = position;
    }

    @Override
    public void updateSection(VideoGroup g) {
        if (g == null
                || g.getType() == MediaGroup.TYPE_SHORTS
                || g.isShorts()
                || g.getSection() != null && g.getSection().getId() != selected) return;
        feed.update(g);
        if (pendingScroll != null && !feed.adapter.isEmpty()) {
            feed.list.getLayoutManager().onRestoreInstanceState(pendingScroll);
            pendingScroll = null;
        }
    }

    @Override
    public void updateSection(SettingsGroup g) {
        if (g == null || you) return;
        content.removeAllViews();
        ScrollView view = new ScrollView(this);
        settings = MobileUi.column(this);
        view.addView(settings);
        content.addView(view);
        for (SettingsItem i : g.getItems())
            settings.addView(MobileUi.button(this, i.title, i.onClick));
    }

    @Override
    public void clearSection(BrowseSection s) {
        feed.clear();
    }

    @Override
    public void selectSectionItem(int index) {
        if (index >= 0) feed.list.scrollToPosition(index);
    }

    @Override
    public void selectSectionItem(Video v) {
        int i = feed.adapter.indexOf(v);
        if (i >= 0) selectSectionItem(i);
    }

    @Override
    public void showError(ErrorFragmentData error) {
        feed.error(error.getMessage(), error.getActionText(), error::onAction);
    }

    @Override
    public void showProgressBar(boolean show) {
        runOnUiThread(() -> feed.loading(show));
    }

    @Override
    public boolean isProgressBarShowing() {
        return feed.loading();
    }

    @Override
    public void focusOnContent() {}

    @Override
    public boolean isEmpty() {
        return feed.adapter.isEmpty();
    }

    @Override
    public void updateBadge() {}

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        feed.resize();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("section", selected);
        state.putBoolean("you", you);
        state.putParcelable("scroll", feed.list.getLayoutManager().onSaveInstanceState());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter != null) presenter.onViewResumed();
    }

    @Override
    protected void onPause() {
        if (presenter != null) presenter.onViewPaused();
        super.onPause();
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
