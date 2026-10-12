package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.*;

import com.liskovsoft.mediaserviceinterfaces.data.*;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.*;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView;

import java.util.*;

/** Touch renderer for existing option categories, including nested menus and comment threads. */
public class MobileDialogActivity extends MobileActivity implements AppDialogView {
    private AppDialogPresenter presenter;
    private LinearLayout root, body;
    private ScrollView scroller;
    private final Deque<Page> backstack = new ArrayDeque<>();
    private Page page;
    private boolean paused, closing;
    private CommentsReceiver comments;
    private ChatReceiver chat;
    private final List<CommentItem> messages = new ArrayList<>();
    private CommentGroup currentGroup;
    private final Map<OptionCategory, OptionItem> selectedRadios = new IdentityHashMap<>();
    private final Map<OptionItem, CompoundButton> optionButtons = new IdentityHashMap<>();
    private final Map<String, TextView> commentViews = new HashMap<>();

    private static final class Page {
        final List<OptionCategory> categories;
        final CharSequence title;
        final boolean transparent, overlay;
        final int id;

        Page(List<OptionCategory> c, CharSequence t, boolean transparent, boolean overlay, int id) {
            categories = c;
            title = t;
            this.transparent = transparent;
            this.overlay = overlay;
            this.id = id;
        }
    }

    private static final class Backup implements CommentsReceiver.Backup {
        final List<CommentItem> items;
        final CommentGroup group;

        Backup(List<CommentItem> items, CommentGroup group) {
            this.items = new ArrayList<>(items);
            this.group = group;
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        root = MobileUi.column(this);
        setContentView(root);
        presenter = AppDialogPresenter.instance(this);
        presenter.setView(this);
        presenter.onViewInitialized();
    }

    @Override
    public void show(
            List<OptionCategory> categories,
            CharSequence title,
            boolean expandable,
            boolean transparent,
            boolean overlay,
            int id) {
        if (page != null) backstack.push(page);
        detachReceivers();
        page =
                new Page(
                        categories == null ? Collections.emptyList() : new ArrayList<>(categories),
                        title,
                        transparent,
                        overlay,
                        id);
        render();
    }

    private void render() {
        optionButtons.clear();
        root.removeAllViews();
        root.addView(
                MobileUi.toolbar(
                        this, page.title == null ? "SmartTube" : page.title, this::onBackPressed));
        scroller = new ScrollView(this);
        body = MobileUi.column(this);
        scroller.addView(body);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        for (OptionCategory category : page.categories) {
            if (category.title != null) body.addView(MobileUi.text(this, category.title, 18));
            if (category.options == null) continue;
            if (category.type == OptionCategory.TYPE_RADIO_LIST) {
                RadioGroup radio = new RadioGroup(this);
                body.addView(radio);
                for (OptionItem item : category.options) {
                    RadioButton button = new RadioButton(this);
                    button.setText(label(item));
                    button.setTextColor(Color.WHITE);
                    button.setMinHeight(MobileUi.dp(this, 48));
                    button.setId(android.view.View.generateViewId());
                    radio.addView(button);
                    optionButtons.put(item, button);
                    button.setChecked(
                            selectedRadios.containsKey(category)
                                    ? selectedRadios.get(category) == item
                                    : item.isSelected());
                    button.setOnClickListener(
                            v -> {
                                selectedRadios.put(category, item);
                                select(item, true);
                            });
                }
            } else
                for (OptionItem item : category.options) {
                    if (category.type == OptionCategory.TYPE_CHECKBOX_LIST
                            || category.type == OptionCategory.TYPE_SINGLE_SWITCH) {
                        CheckBox button = new CheckBox(this);
                        button.setText(label(item));
                        button.setTextColor(Color.WHITE);
                        button.setMinHeight(MobileUi.dp(this, 48));
                        optionButtons.put(item, button);
                        button.setChecked(item.isSelected());
                        body.addView(button);
                        button.setOnClickListener(
                                v -> {
                                    if (!select(item, button.isChecked()))
                                        optionButtons.put(item, button);
                                    button.setChecked(item.isSelected());
                                });
                    } else if (category.type == OptionCategory.TYPE_LONG_TEXT) {
                        TextView text = MobileUi.text(this, item.getTitle(), 16);
                        text.setTextIsSelectable(true);
                        text.setMovementMethod(LinkMovementMethod.getInstance());
                        body.addView(text);
                    } else if (category.type == OptionCategory.TYPE_COMMENTS
                            && item.getCommentsReceiver() != null)
                        bindComments(item.getCommentsReceiver());
                    else if (category.type == OptionCategory.TYPE_CHAT
                            && item.getChatReceiver() != null) bindChat(item.getChatReceiver());
                    else body.addView(MobileUi.button(this, label(item), () -> select(item, true)));
                }
        }
    }

    private CharSequence label(OptionItem item) {
        return item.getDescription() == null
                ? item.getTitle()
                : android.text.TextUtils.concat(item.getTitle(), "\n", item.getDescription());
    }

    private boolean select(OptionItem item, boolean checked) {
        if (checked && item.getRequired() != null)
            for (OptionItem required : item.getRequired())
                if (!required.isSelected()) {
                    Toast.makeText(
                                    this,
                                    getString(
                                            com.liskovsoft.smartyoutubetv2.tv.R.string
                                                    .require_checked,
                                            required.getTitle()),
                                    Toast.LENGTH_LONG)
                            .show();
                }
        item.onSelect(checked);
        if (checked && item.getRadio() != null)
            for (OptionItem other : item.getRadio())
                if (other != item && other.isSelected()) other.onSelect(false);
        for (Map.Entry<OptionItem, CompoundButton> entry : optionButtons.entrySet())
            if (!(entry.getValue() instanceof RadioButton))
                entry.getValue().setChecked(entry.getKey().isSelected());
        return true;
    }

    private void bindComments(CommentsReceiver receiver) {
        comments = receiver;
        messages.clear();
        commentViews.clear();
        currentGroup = null;
        TextView status = MobileUi.text(this, receiver.getLoadingMessage(), 16);
        body.addView(status);
        receiver.setCallback(
                new CommentsReceiver.Callback() {
                    public void onCommentGroup(CommentGroup group) {
                        runOnUiThread(
                                () -> {
                                    if (comments != receiver || closing) return;
                                    if (group == null || group.getComments() == null) {
                                        status.setText(receiver.getErrorMessage());
                                        return;
                                    }
                                    status.setVisibility(android.view.View.GONE);
                                    currentGroup = group;
                                    for (CommentItem item : group.getComments()) {
                                        if (messages.stream()
                                                .noneMatch(
                                                        x ->
                                                                Objects.equals(
                                                                        x.getId(), item.getId()))) {
                                            messages.add(item);
                                            appendComment(item, receiver);
                                        }
                                    }
                                    if (group.getNextCommentsKey() != null) {
                                        Button more =
                                                MobileUi.button(
                                                        MobileDialogActivity.this,
                                                        "Load more",
                                                        () -> receiver.onLoadMore(group));
                                        body.addView(more);
                                        more.setOnClickListener(
                                                v -> {
                                                    body.removeView(more);
                                                    receiver.onLoadMore(group);
                                                });
                                    } else if (messages.isEmpty()) {
                                        status.setVisibility(android.view.View.VISIBLE);
                                        status.setText(receiver.getErrorMessage());
                                    }
                                });
                    }

                    public void onBackup(CommentsReceiver.Backup saved) {
                        if (saved instanceof Backup)
                            runOnUiThread(
                                    () -> {
                                        Backup backup = (Backup) saved;
                                        currentGroup = backup.group;
                                        status.setVisibility(android.view.View.GONE);
                                        messages.addAll(backup.items);
                                        for (CommentItem item : backup.items)
                                            appendComment(item, receiver);
                                        if (currentGroup != null
                                                && currentGroup.getNextCommentsKey() != null)
                                            body.addView(
                                                    MobileUi.button(
                                                            MobileDialogActivity.this,
                                                            "Load more",
                                                            () ->
                                                                    receiver.onLoadMore(
                                                                            currentGroup)));
                                    });
                        else receiver.onStart();
                    }

                    public void onSync(CommentItem item) {
                        runOnUiThread(
                                () -> {
                                    TextView text = commentViews.get(item.getId());
                                    if (text != null) text.setText(commentLabel(item));
                                });
                    }
                });
        receiver.onStart();
    }

    private String commentLabel(CommentItem i) {
        return String.valueOf(i.getAuthorName())
                + " • "
                + String.valueOf(i.getPublishedDate())
                + "\n"
                + String.valueOf(i.getMessage())
                + "\n"
                + (i.getLikeCount() == null ? "" : i.getLikeCount())
                + (i.getReplyCount() == null ? "" : " • " + i.getReplyCount());
    }

    private void appendComment(CommentItem item, CommentsReceiver receiver) {
        TextView text = MobileUi.text(this, commentLabel(item), 16);
        text.setMinHeight(MobileUi.dp(this, 48));
        text.setOnClickListener(v -> receiver.onCommentClicked(item));
        text.setOnLongClickListener(
                v -> {
                    receiver.onCommentLongClicked(item);
                    return true;
                });
        body.addView(text);
        commentViews.put(item.getId(), text);
    }

    private void bindChat(ChatReceiver receiver) {
        chat = receiver;
        LinearLayout list = MobileUi.column(this);
        body.addView(list);
        receiver.setCallback(
                item ->
                        runOnUiThread(
                                () -> {
                                    if (chat != receiver || closing) return;
                                    list.addView(
                                            MobileUi.text(
                                                    this,
                                                    item.getAuthorName() + "\n" + item.getMessage(),
                                                    16));
                                    while (list.getChildCount() > 100) list.removeViewAt(0);
                                }));
    }

    private void detachReceivers() {
        if (comments != null) {
            CommentsReceiver receiver = comments;
            comments = null;
            receiver.onFinish(new Backup(messages, currentGroup));
            receiver.setCallback(null);
        }
        if (chat != null) {
            chat.setCallback(null);
            chat = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (canGoBack()) goBack();
        else finish();
    }

    @Override
    public void goBack() {
        if (!backstack.isEmpty()) {
            detachReceivers();
            page = backstack.pop();
            render();
        } else finish();
    }

    @Override
    public void clearBackstack() {
        backstack.clear();
    }

    @Override
    public boolean canGoBack() {
        return !backstack.isEmpty();
    }

    @Override
    public boolean isShown() {
        return !isFinishing() && !paused;
    }

    @Override
    public boolean isTransparent() {
        return page != null && page.transparent;
    }

    @Override
    public boolean isOverlay() {
        return page != null && page.overlay;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public int getViewId() {
        return page == null ? 0 : page.id;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
    }

    @Override
    protected void onPause() {
        paused = true;
        super.onPause();
    }

    @Override
    public void finish() {
        if (closing) return;
        closing = true;
        detachReceivers();
        presenter.onFinish();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        detachReceivers();
        if (presenter != null && presenter.getView() == this) {
            presenter.onViewDestroyed();
            presenter.setView(null);
        }
        super.onDestroy();
    }
}
