package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.graphics.Color;
import android.os.Bundle;
import android.content.res.Configuration;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import com.bumptech.glide.Glide;
import com.liskovsoft.smartyoutubetv2.tv.R;
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
    private final Map<String, Button> commentLikes = new HashMap<>();
    private final Map<String, CommentItem> commentItems = new HashMap<>();

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
    protected void initTheme() {
        setTheme(R.style.MobileDialogTheme);
    }

    private boolean hasComments() {
        for (OptionCategory category : page.categories)
            if (category.type == OptionCategory.TYPE_COMMENTS) return true;
        return false;
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
        boolean commentSheet = hasComments();
        updateWindow(commentSheet);
        root.setBackgroundColor(commentSheet ? Color.TRANSPARENT : MobileUi.background(this));
        LinearLayout panel = root;
        if (commentSheet) {
            panel = MobileUi.column(this);
            android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
            background.setColor(getColor(R.color.mobile_surface));
            float radius = MobileUi.dp(this, 24);
            background.setCornerRadii(new float[] {radius, radius, radius, radius, 0, 0, 0, 0});
            panel.setBackground(background);
            panel.setClipToOutline(true);
            root.addView(panel, new LinearLayout.LayoutParams(-1, 0, 1));
            View handle = new View(this);
            handle.setBackground(MobileUi.rounded(this, 0xff5a5e66));
            LinearLayout.LayoutParams grip = new LinearLayout.LayoutParams(MobileUi.dp(this, 36), MobileUi.dp(this, 4));
            grip.gravity = Gravity.CENTER_HORIZONTAL;
            grip.setMargins(0, MobileUi.dp(this, 10), 0, MobileUi.dp(this, 6));
            panel.addView(handle, grip);
            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView heading = MobileUi.text(this, getString(R.string.mobile_watch_comments), 20);
            heading.setTypeface(null, android.graphics.Typeface.BOLD);
            header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
            Button close = MobileUi.button(this, "", this::onBackPressed);
            MobileUi.centerIcon(close, "close", 24);
            close.setContentDescription(getString(R.string.mobile_comments_close));
            close.setBackgroundColor(Color.TRANSPARENT);
            header.addView(close, new LinearLayout.LayoutParams(MobileUi.dp(this, 48), MobileUi.dp(this, 48)));
            panel.addView(header);
            View divider = new View(this);
            divider.setBackgroundColor(getColor(R.color.mobile_feedback));
            panel.addView(divider, new LinearLayout.LayoutParams(-1, MobileUi.dp(this, 1)));
        } else panel.addView(
                MobileUi.toolbar(
                        this, page.title == null ? "SmartTube" : page.title, this::onBackPressed));
        scroller = new ScrollView(this);
        body = MobileUi.column(this);
        body.setPadding(MobileUi.dp(this, 12), MobileUi.dp(this, 8), MobileUi.dp(this, 12), MobileUi.dp(this, 16));
        body.setPadding(MobileUi.dp(this, 12), MobileUi.dp(this, 8), MobileUi.dp(this, 12), MobileUi.dp(this, 16));
        if (commentSheet) {
            body.setBackgroundColor(Color.TRANSPARENT);
            body.setPadding(0, MobileUi.dp(this, 8), 0, MobileUi.dp(this, 16));
            scroller.setVerticalScrollBarEnabled(false);
        }
        scroller.addView(body);
        panel.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));
        for (OptionCategory category : page.categories) {
            if (category.title != null && category.type != OptionCategory.TYPE_COMMENTS)
                body.addView(MobileUi.text(this, category.title, 18));
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

    private void updateWindow(boolean commentSheet) {
        // Only the sheet belongs to this window. Android delivers touches above it
        // directly to the watch window, including complete seek gestures.
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = commentSheet ? Gravity.BOTTOM : Gravity.TOP;
        if (commentSheet) {
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int videoHeight = Math.min(screenHeight / 2,
                    getResources().getDisplayMetrics().widthPixels * 9 / 16 + MobileUi.dp(this, 56));
            params.height = screenHeight - videoHeight;
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        } else {
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        }
        getWindow().setAttributes(params);
        setFinishOnTouchOutside(false);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (page != null) updateWindow(hasComments());
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
        commentLikes.clear();
        commentItems.clear();
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
                                        if (item == null || android.text.TextUtils.isEmpty(item.getMessage())) continue;
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
                                        appendLoadMore(receiver, group);
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
                                        if (comments != receiver || closing) return;
                                        Backup backup = (Backup) saved;
                                        currentGroup = backup.group;
                                        status.setVisibility(android.view.View.GONE);
                                        messages.addAll(backup.items);
                                        for (CommentItem item : backup.items)
                                            appendComment(item, receiver);
                                        if (currentGroup != null
                                                && currentGroup.getNextCommentsKey() != null)
                                            appendLoadMore(receiver, currentGroup);
                                    });
                        else receiver.onStart();
                    }

                    public void onSync(CommentItem item) {
                        runOnUiThread(
                                () -> {
                                    if (comments != receiver || closing) return;
                                    TextView text = commentViews.get(item.getId());
                                    if (text != null) text.setText(commentLabel(item));
                                    commentItems.put(item.getId(), item);
                                    Button like = commentLikes.get(item.getId());
                                    if (like != null) updateCommentLike(like, item);
                                });
                    }
                });
        receiver.onStart();
    }

    private void appendLoadMore(CommentsReceiver receiver, CommentGroup group) {
        Button more = MobileUi.button(this, getString(R.string.mobile_comments_more), () -> {});
        more.setTextColor(0xff83baff);
        more.setBackground(MobileUi.rounded(this, 0xff252932));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, MobileUi.dp(this, 48));
        params.setMargins(MobileUi.dp(this, 16), MobileUi.dp(this, 8), MobileUi.dp(this, 16), 0);
        body.addView(more, params);
        more.setOnClickListener(v -> {
            body.removeView(more);
            receiver.onLoadMore(group);
        });
    }

    private CharSequence commentLabel(CommentItem i) {
        String author = i.getAuthorName() == null ? "" : i.getAuthorName();
        String label = author
                + (i.getPublishedDate() == null ? "" : "  ·  " + i.getPublishedDate())
                + "\n"
                + (i.getMessage() == null ? "" : i.getMessage());
        android.text.SpannableString text = new android.text.SpannableString(label);
        int authorEnd = author.length();
        text.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, authorEnd, 0);
        int dateEnd = label.indexOf('\n');
        text.setSpan(new android.text.style.ForegroundColorSpan(0xffaaaaaa), authorEnd, dateEnd, 0);
        text.setSpan(new android.text.style.RelativeSizeSpan(0.85f), 0, dateEnd, 0);
        return text;
    }

    private void updateCommentLike(Button button, CommentItem item) {
        button.setText((item.isLiked() ? "♥" : "♡")
                + (item.getLikeCount() == null ? "" : "  " + item.getLikeCount()));
        button.setTextColor(item.isLiked() ? 0xffff7890 : 0xffb8bdc7);
        button.setSelected(item.isLiked());
    }

    private void appendComment(CommentItem item, CommentsReceiver receiver) {
        commentItems.put(item.getId(), item);
        TextView text = MobileUi.text(this, commentLabel(item), 15);
        text.setPadding(0, 0, 0, 0);
        text.setLineSpacing(MobileUi.dp(this, 5), 1);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 8));
        ImageView photo = new ImageView(this);
        photo.setBackground(MobileUi.rounded(this, 0xff30343c));
        photo.setClipToOutline(true);
        row.addView(photo, new LinearLayout.LayoutParams(MobileUi.dp(this, 40), MobileUi.dp(this, 40)));
        Glide.with(this).load(item.getAuthorPhoto()).circleCrop().into(photo);
        text.setMinHeight(MobileUi.dp(this, 48));
        text.setOnClickListener(v -> receiver.onCommentClicked(commentItems.get(item.getId())));
        text.setOnLongClickListener(
                v -> {
                    receiver.onCommentLongClicked(commentItems.get(item.getId()));
                    return true;
                });
        LinearLayout content = MobileUi.column(this);
        content.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0, -2, 1);
        contentParams.setMarginStart(MobileUi.dp(this, 12));
        row.addView(content, contentParams);
        content.addView(text);
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(actions);
        Button like = MobileUi.button(this, "", () -> receiver.onCommentLongClicked(commentItems.get(item.getId())));
        updateCommentLike(like, item);
        like.setContentDescription(getString(R.string.mobile_comments_like));
        like.setBackgroundColor(Color.TRANSPARENT);
        actions.addView(like, new LinearLayout.LayoutParams(-2, MobileUi.dp(this, 48)));
        if (item.getNestedCommentsKey() != null && item.getReplyCount() != null) {
            Button replies = MobileUi.button(this, item.getReplyCount() + "  ›",
                    () -> receiver.onCommentClicked(commentItems.get(item.getId())));
            replies.setTextColor(0xff83baff);
            replies.setBackgroundColor(Color.TRANSPARENT);
            actions.addView(replies, new LinearLayout.LayoutParams(-2, MobileUi.dp(this, 48)));
        }
        body.addView(row);
        commentViews.put(item.getId(), text);
        commentLikes.put(item.getId(), like);
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
        return page != null && (page.transparent || hasComments());
    }

    @Override
    public boolean isOverlay() {
        // The portrait player is already above the sheet; TV dialog fitting would shrink it.
        return page != null && (page.overlay || hasComments());
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
