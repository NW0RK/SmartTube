package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.PlaybackControlsRow.MultiAction;

import com.bumptech.glide.Glide;
import com.github.vkay94.dtpv.DoubleTapPlayerAdapter;
import com.github.vkay94.dtpv.DoubleTapPlayerView;
import com.github.vkay94.dtpv.youtube.YouTubeOverlay;
import com.google.android.exoplayer2.Player;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.*;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.*;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.DebugInfoManager;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.prefs.*;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions.*;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.previewtimebar.StoryboardManager;

import java.util.*;

public class MobilePlaybackFragment extends MobilePlayerEngine {
    private LinearLayout root, details, bar, chatList;
    private FrameLayout controls;
    private ChatReceiver chatReceiver;
    private FrameLayout stage, debug;
    private LinearLayout seekPreview;
    private MobileFeedView suggestions;
    private TextView title, meta, time, quality, seekTitle;
    private LinearLayout chapters;
    private VideoGroup chapterGroup;
    private HorizontalScrollView chapterScroll;
    private final List<View> chapterCards = new ArrayList<>();
    private int activeChapter = -1;
    private boolean suggestionsRequested;
    private ImageView avatar, preview, poster;
    private Button play, fullscreen;
    private MobileSegmentSeekBar seek;
    private ProgressBar buffering;
    private MobileBufferingIndicator bufferingIndicator;
    private boolean progressRequested;
    private YouTubeOverlay overlay;
    private DoubleTapPlayerAdapter taps;
    private StoryboardManager storyboard;
    private DebugInfoManager debugManager;
    private final Map<Integer, Action> actions = new LinkedHashMap<>();
    private final Map<Integer, Button> buttons = new HashMap<>();
    private final Map<Integer, Integer> states = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean dragging, wasPlaying, playbackFinished;
    private long seekDuration;
    private int previewRequest, previewIndex = -1;
    private String restoredId;
    private Video restoredVideo;
    private long restoredPosition;
    private final Runnable update =
            new Runnable() {
                public void run() {
                    if (getView() == null) return;
                    long duration = getDurationMs();
                    if (!dragging) {
                        seek.setProgress(
                                duration > 0 ? (int) (getPositionMs() * 10000 / duration) : 0);
                        time.setText(clock(getPositionMs()) + " / " + clock(duration));
                        seek.setSecondaryProgress(duration > 0 && mPlayer != null
                                ? (int) Math.min(10000, Math.max(0, mPlayer.getBufferedPosition()) * 10000 / duration) : 0);
                    }
                    setIcon(play, getPlayWhenReady() ? "pause" : "play", 30);
                    root.setKeepScreenOn(isPlaying() && !isInPIPMode());
                    overlay.seekSeconds(getPlayerData().getSeekIncrementMs() / 1000);
                    if (isPlaying()) poster.setVisibility(View.GONE);
                    if (!dragging) seek.setEnabled(duration > 0 && mPlayer != null && mPlayer.isCurrentWindowSeekable());
                    updateBufferingIndicator();
                    updateChapterSelection();
                    handler.postDelayed(this, 500);
                }
            };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            restoredVideo = Video.fromString(state.getString("videoData"));
            restoredId = restoredVideo == null ? state.getString("video") : restoredVideo.videoId;
            restoredPosition = state.getLong("position");
        }
        initActions();
    }

    private void initActions() {
        ThumbsUpAction thumbsUpAction = new ThumbsUpAction(requireContext());
        ThumbsDownAction thumbsDownAction = new ThumbsDownAction(requireContext());
        thumbsUpAction.setBoundAction(thumbsDownAction);
        thumbsDownAction.setBoundAction(thumbsUpAction);
        putAction(thumbsUpAction);
        putAction(thumbsDownAction);
        putAction(new HighQualityAction(requireContext()));
        putAction(new PlaybackModeAction(requireContext()));
        putAction(new ChannelAction(requireContext()));
        putAction(new ClosedCaptioningAction(requireContext()));
        putAction(new PlaylistAddAction(requireContext()));
        putAction(new SubscribeAction(requireContext()));
        putAction(new VideoInfoAction(requireContext()));
        putAction(new VideoSpeedAction(requireContext()));
        putAction(new VideoStatsAction(requireContext()));
        putAction(new SearchAction(requireContext()));
        putAction(new PipAction(requireContext()));
        putAction(new AFRAction(requireContext()));
        putAction(new ScreenDimmingAction(requireContext()));
        putAction(new ChatAction(requireContext()));
        putAction(new PlaybackQueueAction(requireContext()));
        putAction(new ContentBlockAction(requireContext()));
        putAction(new ShareAction(requireContext()));
        putAction(new SeekIntervalAction(requireContext()));
        putAction(new VideoZoomAction(requireContext()));
        putAction(new RotateAction(requireContext()));
        putAction(new FlipAction(requireContext()));
        putAction(new SoundOffAction(requireContext()));
    }

    private void putAction(Action a) {
        actions.put((int) a.getId(), a);
    }

    private void action(int id) {
        if (overlay != null) overlay.seekSeconds(getPlayerData().getSeekIncrementMs() / 1000);
        if (id == R.id.action_pip) {
            ((MobilePlaybackActivity) requireActivity()).enterPictureInPicture();
            return;
        }
        if (getButtonState(id) == BUTTON_DISABLED) return;
        if (id == R.id.action_share && getVideo() != null && buttons.get(id) != null)
            MobileDialogActivity.requestPopup(buttons.get(id), getVideo().getTitle());
        if (id == R.id.lb_control_closed_captioning) {
            toggleCaptions();
            return;
        }
        Action a = actions.get(id);
        if (a instanceof MultiAction) {
            ((MultiAction) a).nextIndex();
            setButtonState(id, ((MultiAction) a).getIndex());
        }
        mPlaybackPresenter.onButtonClicked(id, getButtonState(id));
    }

    private Button actionButton(LinearLayout row, int id, String label) {
        Button button = MobileUi.button(requireContext(), label, () -> action(id));
        button.setOnLongClickListener(
                v -> {
                    mPlaybackPresenter.onButtonLongClicked(id, getButtonState(id));
                    return true;
                });
        buttons.put(id, button);
        MobileUi.addButton(row, button);
        return button;
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radius) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(MobileUi.dp(requireContext(), radius));
        return bg;
    }

    private void setIcon(Button button, String name, int size) {
        MobileUi.centerIcon(button, name, size);
    }

    private Button iconButton(String icon, String label, Runnable click) {
        Button b = MobileUi.button(requireContext(), "", click);
        b.setMinimumWidth(MobileUi.dp(requireContext(), 48));
        b.setContentDescription(label);
        MobileUi.playerFeedback(b);
        setIcon(b, icon, 24);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                MobileUi.dp(requireContext(), 48), MobileUi.dp(requireContext(), 48)));
        return b;
    }

    private void decorate(Button button, String name) {
        MobilePlayerIcon icon = new MobilePlayerIcon(name);
        int size = MobileUi.dp(requireContext(), 22);
        icon.setBounds(0, 0, size, size);
        button.setCompoundDrawables(icon, null, null, null);
        button.setCompoundDrawablePadding(MobileUi.dp(requireContext(), 8));
        MobileUi.browseFeedback(button, true);
    }

    private void updateChapterSelection() {
        if (chapterGroup == null) return;
        int selected = -1;
        List<Video> videos = chapterGroup.getVideos();
        for (int i = 0; i < videos.size(); i++) {
            if (videos.get(i).startTimeMs <= getPositionMs()) selected = i;
            else break;
        }
        if (selected == activeChapter) return;
        activeChapter = selected;
        for (int i = 0; i < chapterCards.size(); i++) {
            android.graphics.drawable.GradientDrawable border = round(requireContext().getColor(R.color.mobile_surface), 16);
            if (i == selected) border.setStroke(MobileUi.dp(requireContext(), 2), MobileUi.accent(requireContext()));
            chapterCards.get(i).setBackground(border);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        root = MobileUi.column(requireContext());
        root.setBackgroundColor(MobileUi.background(requireContext()));
        bar =
                MobileUi.watchToolbar(requireContext());
        root.addView(bar);
        TextView brand = bar.findViewById(R.id.mobile_toolbar_title);
        brand.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        brand.setTypeface(null, android.graphics.Typeface.BOLD);
        MobileUi.addButton(bar, iconButton("search", "Search", () -> action(R.id.action_search)));
        MobileUi.addSeparator(bar);
        MobileUi.addButton(bar, iconButton("more", "Playback options", this::playbackOptions));
        stage = (FrameLayout) inflater.inflate(R.layout.mobile_player_stage, parent, false);
        stage.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        MobileUi.dp(view.getContext(), 20));
            }
        });
        stage.setClipToOutline(true);
        surface = stage.findViewById(R.id.mobile_player_surface);
        surface.setUseController(false);
        poster = new ImageView(requireContext());
        poster.setScaleType(ImageView.ScaleType.FIT_CENTER);
        poster.setVisibility(View.GONE);
        stage.addView(poster, new FrameLayout.LayoutParams(-1, -1));
        root.addView(stage);
        controls = new FrameLayout(requireContext());
        android.graphics.drawable.GradientDrawable shade = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x66000000, 0x22000000, 0x99000000});
        controls.setBackground(shade);
        controls.setOnApplyWindowInsetsListener((view, insets) -> {
            updateControlInsets(insets);
            return insets;
        });
        stage.addView(controls, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout top = new LinearLayout(requireContext());
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPaddingRelative(MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 4),
                MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 4));
        top.addView(iconButton("down", "Close player", () -> requireActivity().onBackPressed()));
        top.addView(new View(requireContext()), new LinearLayout.LayoutParams(0, 1, 1));
        Button captions = actionButton(top, R.id.lb_control_closed_captioning, "CC");
        MobileUi.playerFeedback(captions);
        captions.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(
                MobileUi.dp(requireContext(), 48), MobileUi.dp(requireContext(), 48));
        captionParams.setMarginEnd(MobileUi.dp(requireContext(), 8));
        captions.setLayoutParams(captionParams);
        captions.setTypeface(null, android.graphics.Typeface.BOLD);
        Button settings = iconButton("settings", "Playback settings", this::settings);
        top.addView(settings);
        controls.addView(top, new FrameLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 56), Gravity.TOP));
        play =
                MobileUi.button(
                        requireContext(),
                        "\u25b6",
                        () -> {
                            setPlayWhenReady(!getPlayWhenReady());
                            if (getPlayWhenReady()) mPlaybackPresenter.onPlayClicked();
                            else mPlaybackPresenter.onPauseClicked();
                        });
        play.setContentDescription(getString(R.string.mobile_play_pause));
        setIcon(play, "play", 30);
        MobileUi.playerFeedback(play);
        controls.addView(play, new FrameLayout.LayoutParams(
                MobileUi.dp(requireContext(), 56), MobileUi.dp(requireContext(), 56), Gravity.CENTER));
        LinearLayout bottom = new LinearLayout(requireContext());
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPaddingRelative(MobileUi.dp(requireContext(), 16), 0,
                MobileUi.dp(requireContext(), 16), 0);
        time = MobileUi.text(requireContext(), "0:00 / 0:00", 12);
        time.setSingleLine(true);
        time.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        bottom.addView(time, new LinearLayout.LayoutParams(0, -2, 1));
        fullscreen =
                MobileUi.button(
                        requireContext(),
                        "\u26f6",
                        () -> ((MobilePlaybackActivity) requireActivity()).toggleFullscreen());
        fullscreen.setContentDescription(getString(R.string.mobile_fullscreen));
        MobileUi.playerFeedback(fullscreen);
        setIcon(fullscreen, "fullscreen", 22);
        bottom.addView(fullscreen, new LinearLayout.LayoutParams(
                MobileUi.dp(requireContext(), 48), MobileUi.dp(requireContext(), 48)));
        seek = new MobileSegmentSeekBar(requireContext());
        seek.setContentDescription("Video position");
        seek.setEnabled(false);
        LinearLayout footer = new LinearLayout(requireContext());
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.addView(seek, new LinearLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 48)));
        footer.addView(bottom, new LinearLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 48)));
        controls.addView(footer, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        buffering = new ProgressBar(requireContext());
        buffering.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        buffering.setContentDescription("Buffering video");
        bufferingIndicator = new MobileBufferingIndicator(buffering, handler);
        stage.addView(
                buffering,
                new FrameLayout.LayoutParams(
                        MobileUi.dp(requireContext(), 72),
                        MobileUi.dp(requireContext(), 72),
                        Gravity.CENTER));
        overlay = new YouTubeOverlay(requireContext(), null);
        stage.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        overlay.setVisibility(View.GONE);
        seekPreview = new LinearLayout(requireContext());
        seekPreview.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable previewSurface = round(0xee202020, 14);
        previewSurface.setStroke(MobileUi.dp(requireContext(), 1), 0x44ffffff);
        seekPreview.setBackground(previewSurface);
        seekPreview.setClipToOutline(true);
        seekPreview.setPadding(MobileUi.dp(requireContext(), 4), MobileUi.dp(requireContext(), 4),
                MobileUi.dp(requireContext(), 4), 0);
        seekPreview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        preview = new ImageView(requireContext());
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        seekPreview.addView(preview, new LinearLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 54)));
        preview.setVisibility(View.GONE);
        seekTitle = MobileUi.text(requireContext(), "", 14);
        seekTitle.setGravity(Gravity.CENTER);
        seekTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        seekTitle.setPadding(0, MobileUi.dp(requireContext(), 4), 0, MobileUi.dp(requireContext(), 6));
        seekPreview.addView(seekTitle, new LinearLayout.LayoutParams(-1, -2));
        stage.addView(seekPreview, new FrameLayout.LayoutParams(MobileUi.dp(requireContext(), 104), -2));
        seekPreview.setVisibility(View.GONE);
        debug = new FrameLayout(requireContext());
        stage.addView(debug, new FrameLayout.LayoutParams(-1, -1));
        details = MobileUi.column(requireContext());
        details.setBackgroundColor(MobileUi.background(requireContext()));
        title = MobileUi.text(requireContext(), "", 18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout information = MobileUi.column(requireContext());
        information.setBackground(MobileUi.pillSurface(requireContext()));
        information.setOnClickListener(v -> action(R.id.action_info));
        title.setOnClickListener(v -> action(R.id.action_info));
        information.addView(title);
        meta = MobileUi.text(requireContext(), "", 13);
        meta.setTextColor(requireContext().getColor(R.color.mobile_text_secondary));
        meta.setPadding(MobileUi.dp(requireContext(), 16), 0, MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 12));
        meta.setOnClickListener(v -> action(R.id.action_info));
        information.addView(meta);
        LinearLayout.LayoutParams informationParams = new LinearLayout.LayoutParams(-1, -2);
        informationParams.setMargins(MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 12),
                MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 12));
        details.addView(information, informationParams);
        LinearLayout channel = new LinearLayout(requireContext());
        channel.setGravity(Gravity.CENTER_VERTICAL);
        channel.setPadding(MobileUi.dp(requireContext(), 12), MobileUi.dp(requireContext(), 8),
                MobileUi.dp(requireContext(), 8), MobileUi.dp(requireContext(), 8));
        channel.setBackground(MobileUi.pillSurface(requireContext()));
        avatar = new ImageView(requireContext());
        channel.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        MobileUi.dp(requireContext(), 48), MobileUi.dp(requireContext(), 48)));
        Button author = actionButton(channel, R.id.action_channel, "Channel");
        MobileUi.pillTouchFeedback(author);
        author.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        author.setTypeface(null, android.graphics.Typeface.BOLD);
        author.setMaxLines(1);
        author.setEllipsize(android.text.TextUtils.TruncateAt.END);
        author.setLayoutParams(
                new LinearLayout.LayoutParams(0, MobileUi.dp(requireContext(), 48), 1));
        avatar.setOnClickListener(v -> action(R.id.action_channel));
        Button subscribe = actionButton(channel, R.id.action_subscribe, "Subscribe");
        MobileUi.browseFeedback(subscribe, true);
        subscribe.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams channelParams = new LinearLayout.LayoutParams(-1, -2);
        channelParams.setMargins(MobileUi.dp(requireContext(), 16), 0, MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 8));
        details.addView(channel, channelParams);
        HorizontalScrollView strip = new HorizontalScrollView(requireContext());
        strip.setHorizontalScrollBarEnabled(false);
        strip.setPadding(MobileUi.dp(requireContext(), 12), 0, MobileUi.dp(requireContext(), 12), MobileUi.dp(requireContext(), 8));
        LinearLayout row = new LinearLayout(requireContext());
        strip.addView(row);
        details.addView(strip);
        LinearLayout reactions = new LinearLayout(requireContext());
        reactions.setGravity(Gravity.CENTER_VERTICAL);
        reactions.setBackground(MobileUi.pillSurface(requireContext()));
        reactions.setClipToOutline(true);
        LinearLayout.LayoutParams reactionParams = new LinearLayout.LayoutParams(-2, MobileUi.dp(requireContext(), 48));
        reactionParams.setMargins(MobileUi.dp(requireContext(), 4), MobileUi.dp(requireContext(), 4), MobileUi.dp(requireContext(), 4), MobileUi.dp(requireContext(), 4));
        row.addView(reactions, reactionParams);
        Button like = actionButton(reactions, R.id.action_thumbs_up, getString(R.string.action_like));
        decorate(like, "like");
        MobileUi.pillTouchFeedback(like);
        like.setLayoutParams(new LinearLayout.LayoutParams(-2, -1));
        View divider = new View(requireContext());
        divider.setBackgroundColor(requireContext().getColor(R.color.mobile_divider));
        reactions.addView(divider, new LinearLayout.LayoutParams(MobileUi.dp(requireContext(), 1), MobileUi.dp(requireContext(), 24)));
        Button dislike = actionButton(reactions, R.id.action_thumbs_down, "");
        dislike.setContentDescription("Dislike");
        MobileUi.pillTouchFeedback(dislike);
        dislike.setLayoutParams(new LinearLayout.LayoutParams(MobileUi.dp(requireContext(), 48), -1));
        setIcon(dislike, "dislike", 22);
        decorate(actionButton(row, R.id.action_share, "Share"), "share");
        Button download = MobileUi.button(requireContext(), getString(R.string.mobile_download),
                () -> MobileDownload.open(requireContext(), getVideo() == null ? null : getVideo().videoId));
        decorate(download, "download");
        MobileUi.addButton(row, download);
        decorate(actionButton(row, R.id.action_playlist_add, "Save"), "save");
        MobileUi.addButton(
                row,
                MobileUi.browseButton(
                        requireContext(), getString(R.string.mobile_more), this::playbackOptions));
        chapters = MobileUi.column(requireContext());
        chapters.setBackgroundColor(Color.TRANSPARENT);
        chapters.setVisibility(View.GONE);
        details.addView(chapters);
        Button comments = actionButton(details, R.id.action_chat, getString(R.string.mobile_watch_comments) + "  ›");
        comments.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        MobileUi.browseFeedback(comments, false);
        LinearLayout.LayoutParams commentCard = new LinearLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 52));
        commentCard.setMargins(MobileUi.dp(requireContext(), 16), 0, MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 12));
        comments.setLayoutParams(commentCard);
        chatList = MobileUi.column(requireContext());
        chatList.setVisibility(View.GONE);
        details.addView(chatList);
        quality = MobileUi.text(requireContext(), "", 12);
        quality.setVisibility(View.GONE);
        details.addView(quality);
        suggestions =
                new MobileFeedView(
                        requireContext(),
                        mPlaybackPresenter::onSuggestionItemClicked,
                        mPlaybackPresenter::onSuggestionItemLongClicked,
                        mPlaybackPresenter::onScrollEnd);
        suggestions.adapter.setHeader(details);
        suggestions.adapter.setCompactCards(true);
        suggestions.setBackgroundColor(MobileUi.background(requireContext()));
        root.addView(suggestions, new LinearLayout.LayoutParams(-1, 0, 1));
        storyboard = new StoryboardManager(requireContext());
        setupSeek();
        root.addOnLayoutChangeListener(
                (view,
                        left,
                        layoutTop,
                        right,
                        layoutBottom,
                        oldLeft,
                        oldTop,
                        oldRight,
                        oldBottom) -> {
                    if (right - left != oldRight - oldLeft) resize();
                });
        stage.post(this::resize);
        stage.addOnLayoutChangeListener((view, left, stageTop, right, stageBottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (left != oldLeft || stageTop != oldTop || right != oldRight || stageBottom != oldBottom)
                MobileDialogActivity.updateCommentsLayout(view.getContext());
        });
        return root;
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);
        mPlaybackPresenter.onViewInitialized();
        initializePlayer();
        root.setKeepScreenOn(true);
        if (restoredId != null && mPlaybackPresenter.getVideo() == null)
            mPlaybackPresenter.openVideo(
                    restoredVideo == null ? Video.from(restoredId) : restoredVideo);
        handler.post(update);
    }

    @Override
    protected void onPlayerCreated() {
        setupTaps();
    }

    private void setupTaps() {
        taps = new DoubleTapPlayerAdapter(stage);
        taps.onSingleTap(
                event -> {
                    showControls(!isControlsShown());
                    mPlaybackPresenter.onTickle();
                });
        taps.controller(overlay);
        overlay.player(mPlayer)
                .playerView(taps)
                .seekSeconds(getPlayerData().getSeekIncrementMs() / 1000)
                .performListener(
                        new YouTubeOverlay.PerformListener() {
                            public void onAnimationStart() {
                                overlay.setVisibility(View.VISIBLE);
                            }

                            public void onAnimationEnd() {
                                overlay.setVisibility(View.GONE);
                                mPlaybackPresenter.onSeekEnd();
                            }

                            public Boolean shouldForward(
                                    @NonNull Player p, @NonNull DoubleTapPlayerView v, float x) {
                                if (!p.isCurrentWindowSeekable()
                                        || x > v.getPlayerWidth() * 0.35f
                                                && x < v.getPlayerWidth() * 0.65f) return null;
                                return x > v.getPlayerWidth() / 2f;
                            }
                        });
        surface.setOnTouchListener(
                (v, event) -> {
                    taps.onTouchEvent(event);
                    return true;
                });
    }

    private void setupSeek() {
        seek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    public void onStartTrackingTouch(SeekBar bar) {
                        dragging = true;
                        seekDuration = Math.max(0, getDurationMs());
                        wasPlaying = getPlayWhenReady();
                        setPlayWhenReady(false);
                        play.setVisibility(View.INVISIBLE);
                        previewIndex = -1;
                        previewRequest++;
                        preview.setImageDrawable(null);
                        preview.setVisibility(View.GONE);
                        mPlaybackPresenter.onKeyDown(-1);
                        seekTitle.setText(clock(seekDuration * bar.getProgress() / 10000));
                        showSeekPreview();
                    }

                    public void onProgressChanged(SeekBar bar, int value, boolean user) {
                        if (!user) return;
                        long position = (dragging ? seekDuration : getDurationMs()) * value / 10000;
                        time.setText(clock(position) + " / " + clock(getDurationMs()));
                        if (!dragging) {
                            // Keyboard and accessibility adjustments have no touch release callback.
                            if (getDurationMs() > 0) {
                                setPositionMs(position);
                                mPlaybackPresenter.onSeekPositionChanged(position);
                                mPlaybackPresenter.onSeekEnd();
                                mPlaybackPresenter.onTickle();
                            }
                            return;
                        }
                        seekTitle.setText(clock(position));
                        showSeekPreview();
                        long[] positions = storyboard.getSeekPositions();
                        if (positions != null && positions.length > 0) {
                            int index = Arrays.binarySearch(positions, position);
                            index =
                                    index >= 0
                                            ? index
                                            : Math.min(
                                                    positions.length - 1, Math.max(0, -index - 2));
                            if (index == previewIndex) return;
                            previewIndex = index;
                            int request = ++previewRequest;
                            storyboard.getBitmap(
                                    index,
                                    bitmap -> {
                                        if (dragging && preview != null && request == previewRequest) {
                                            preview.setImageBitmap(bitmap);
                                            preview.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
                                            showSeekPreview();
                                        }
                                    });
                        }
                    }

                    public void onStopTrackingTouch(SeekBar bar) {
                        long position = seekDuration * bar.getProgress() / 10000;
                        if (!seek.wasCancelled() && seekDuration > 0) {
                            setPositionMs(position);
                            mPlaybackPresenter.onSeekPositionChanged(position);
                        } else position = getPositionMs();
                        dragging = false;
                        previewRequest++;
                        setPlayWhenReady(wasPlaying);
                        play.setVisibility(View.VISIBLE);
                        mPlaybackPresenter.onSeekEnd();
                        mPlaybackPresenter.onTickle();
                        time.setText(clock(position) + " / " + clock(getDurationMs()));
                        seekPreview.setVisibility(View.GONE);
                    }
                });
    }

    private void showSeekPreview() {
        seekPreview.setVisibility(View.VISIBLE);
        int width = MobileUi.dp(requireContext(), stage.getHeight() < MobileUi.dp(requireContext(), 260) ? 104 : 152);
        preview.getLayoutParams().height = (width - MobileUi.dp(requireContext(), 8)) * 9 / 16;
        seekPreview.getLayoutParams().width = width;
        seekPreview.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        android.graphics.Rect bounds = new android.graphics.Rect(0, 0, seek.getWidth(), seek.getHeight());
        stage.offsetDescendantRectToMyCoords(seek, bounds);
        int margin = MobileUi.dp(requireContext(), 8);
        seekPreview.setX(Math.max(margin, Math.min(stage.getWidth() - width - margin,
                bounds.left + seek.progressX() - width / 2f)));
        seekPreview.setY(Math.max(margin, bounds.centerY() - seekPreview.getMeasuredHeight()
                - MobileUi.dp(requireContext(), 16)));
        seekPreview.requestLayout();
    }

    private static String clock(long ms) {
        long s = Math.max(0, ms) / 1000;
        return s >= 3600
                ? String.format(Locale.US, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                : String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    private void toggleCaptions() {
        List<FormatItem> formats = getSubtitleFormats();
        if (formats == null) return;
        FormatItem selected = getSubtitleFormat();
        FormatItem next = FormatItem.SUBTITLE_NONE;
        if (selected == null || selected.isDefault()) {
            next = MobileCaptions.preferred(formats, getPlayerData().getLastSubtitleFormats());
        }
        setFormat(next);
        getPlayerData().setFormat(next);
        boolean enabled = !next.isDefault();
        Video video = getVideo();
        if (video != null) {
            if (enabled) getPlayerData().enableSubtitlesPerChannel(video.channelId);
            else getPlayerData().disableSubtitlesPerChannel(video.channelId);
        }
        setButtonState(R.id.lb_control_closed_captioning, enabled ? BUTTON_ON : BUTTON_OFF);
    }

    private void settings() {
        AppDialogPresenter dialog = AppDialogPresenter.instance(requireContext());
        dialog.appendRadioCategory(
                getString(R.string.title_video_formats),
                UiOptionItem.from(
                        getVideoFormats(),
                        option -> {
                            FormatItem format = UiOptionItem.toFormat(option);
                            setFormat(format);
                            if (getPlayerData().getFormat(FormatItem.TYPE_VIDEO).isPreset()) {
                                getPlayerData().setTempVideoFormat(format);
                            } else {
                                getPlayerData().setFormat(format);
                            }
                        },
                        getString(R.string.option_disabled)));
        dialog.showDialog(getString(R.string.title_video_formats));
    }

    private void playbackOptions() {
        AppDialogPresenter dialog = AppDialogPresenter.instance(requireContext());
        for (Action a : actions.values()) {
            int id = (int) a.getId();
            dialog.appendSingleButton(
                    UiOptionItem.from(
                            a.getLabel1() == null ? "Playback" : a.getLabel1(),
                            item -> {
                                if (id == R.id.action_pip) dialog.closeDialog();
                                action(id);
                            }));
        }
        int[] advanced = {
            R.id.action_repeat,
            R.id.action_video_speed,
            R.id.action_content_block,
            R.id.action_screen_dimming,
            R.id.action_subscribe,
            R.id.action_sound_off,
            R.id.action_afr,
            R.id.lb_control_closed_captioning,
            R.id.action_chat
        };
        for (int id : advanced) {
            Action action = actions.get(id);
            dialog.appendSingleButton(
                    UiOptionItem.from(
                            action.getLabel1() + " \u2022 " + getString(R.string.mobile_more),
                            item ->
                                    mPlaybackPresenter.onButtonLongClicked(
                                            id, getButtonState(id))));
        }
        dialog.showDialog(getString(R.string.playback_settings));
    }

    private void updateControlInsets(WindowInsets insets) {
        if (controls == null || getActivity() == null) return;
        boolean full = ((MobilePlaybackActivity) getActivity()).isFullscreen() && !isInPIPMode();
        int side = 0, top = 0;
        // Keep the watch-page footer clear of the rounded bottom edge.
        int bottom = isInPIPMode() ? 0 : MobileUi.dp(requireContext(), 8);
        if (full) {
            side = top = bottom = MobileUi.dp(requireContext(), 8);
            if (Build.VERSION.SDK_INT >= 28 && insets != null && insets.getDisplayCutout() != null) {
                DisplayCutout cutout = insets.getDisplayCutout();
                // Equal side clearance keeps the controls centered in either landscape direction.
                side += Math.max(cutout.getSafeInsetLeft(), cutout.getSafeInsetRight());
                top += cutout.getSafeInsetTop();
                bottom += cutout.getSafeInsetBottom();
            }
        }
        controls.setPadding(side, top, side, bottom);
        if (buffering != null && play != null) {
            // The button centers inside the padded controls; the ring centers inside the stage.
            buffering.setTranslationX(play.getTranslationX()
                    + (controls.getPaddingLeft() - controls.getPaddingRight()) / 2f);
            buffering.setTranslationY(play.getTranslationY()
                    + (controls.getPaddingTop() - controls.getPaddingBottom()) / 2f);
        }
    }

    private int commentsPanelWidth;
    private String videoLikeCount;

    void setCommentsPanelWidth(int width) {
        if (commentsPanelWidth == width) return;
        commentsPanelWidth = width;
        resize();
    }

    int getVideoBottomOnScreen() {
        if (stage == null || stage.getHeight() == 0) return 0;
        int[] position = new int[2];
        stage.getLocationOnScreen(position);
        return position[1] + stage.getHeight();
    }

    public void resize() {
        if (stage == null || getActivity() == null) return;
        boolean full = ((MobilePlaybackActivity) getActivity()).isFullscreen() || isInPIPMode();
        stage.setClipToOutline(!full);
        // Leave the enlarged timeline touch area clear on short portrait video frames.
        play.setTranslationY(root.getWidth() * 9 / 16 < MobileUi.dp(requireContext(), 260) && !full
                ? -MobileUi.dp(requireContext(), 24) : 0);
        bar.setVisibility(full ? View.GONE : View.VISIBLE);
        suggestions.setVisibility(full ? View.GONE : View.VISIBLE);
        stage.setLayoutParams(
                new LinearLayout.LayoutParams(
                        full && commentsPanelWidth > 0 ? Math.max(1, root.getWidth() - commentsPanelWidth) : -1,
                        full ? 0 : Math.max(1, root.getWidth()) * 9 / 16, full ? 1 : 0));
        suggestions.resize();
        fullscreen.setContentDescription(
                getString(full ? R.string.mobile_exit_fullscreen : R.string.mobile_fullscreen));
        setIcon(fullscreen, full ? "fullscreen-exit" : "fullscreen", 22);
        updateControlInsets(controls.getRootWindowInsets());
        controls.requestApplyInsets();
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        root.post(this::resize);
    }

    @Override
    public void setVideo(Video v) {
        Video previous = getVideo();
        boolean changed = v == null || previous == null || !Objects.equals(previous.videoId, v.videoId);
        if (changed) {
            suggestionsRequested = false;
            videoLikeCount = null;
            resetSuggestedPosition();
            if (getContext() != null) MobileDialogActivity.dismissComments(getContext());
        }
        // Shared TV counter preferences can clear the model after metadata arrives.
        // Keep the latest available count for this video in the mobile UI.
        if (v != null && v.likeCount != null && !v.likeCount.trim().isEmpty()) videoLikeCount = v.likeCount;
        if (v != null && restoredPosition > 0 && Objects.equals(restoredId, v.videoId)) {
            v.pendingPosMs = restoredPosition;
            restoredPosition = 0;
        }
        mExoPlayerController.setVideo(v);
        updateLikeButton();
        if (title != null && v != null) {
            title.setText(v.getTitleFull());
            meta.setText(videoMetadata(v));
            buttons.get(R.id.action_channel).setText(v.getAuthor());
        }
    }

    private void updateLikeButton() {
        Button button = buttons.get(R.id.action_thumbs_up);
        if (button == null) return;
        String like = button.getContext().getString(R.string.action_like);
        button.setText(videoLikeCount == null ? like : videoLikeCount);
        button.setContentDescription(videoLikeCount == null ? like
                : like + ", " + button.getContext().getString(R.string.mobile_video_like_count, videoLikeCount));
    }

    private static String videoMetadata(Video video) {
        CharSequence subtitle = video.getSecondTitleFull();
        if (subtitle == null) return "";
        String author = video.getAuthor();
        List<String> parts = new ArrayList<>();
        for (String part : subtitle.toString().split("[•·]")) {
            String text = part.trim();
            if (!text.isEmpty() && (author == null || !text.equals(author.trim()))) parts.add(text);
        }
        return android.text.TextUtils.join(" · ", parts);
    }

    @Override
    public void setQualityInfo(String text) {
        if (quality != null) quality.setText(text);
    }

    @Override
    public void setTitle(String text) {
        if (title != null) title.setText(text);
    }

    @Override
    public void showBackground(String url) {
        if (poster != null) {
            Glide.with(this).load(url).into(poster);
            poster.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showBackgroundColor(int res) {
        if (stage != null) stage.setBackgroundResource(res);
    }

    @Override
    public void showProgressBar(boolean show) {
        progressRequested = show;
        updateBufferingIndicator();
    }

    private void updateBufferingIndicator() {
        if (bufferingIndicator == null) return;
        if (playbackFinished) {
            bufferingIndicator.release();
            return;
        }
        bufferingIndicator.update(
                mPlayer == null ? Player.STATE_IDLE : mPlayer.getPlaybackState(),
                progressRequested);
    }

    @Override
    public void setSeekBarSegments(List<SeekBarSegment> segments) {
        if (seek != null) seek.setSegments(segments);
    }

    @Override
    public void loadStoryboard() {
        if (storyboard != null) storyboard.init(getVideo(), getDurationMs());
    }

    @Override
    public void updateEndingTime() {}

    @Override
    public void setChatReceiver(ChatReceiver receiver) {
        if (chatReceiver != null) chatReceiver.setCallback(null);
        chatReceiver = receiver;
        if (chatList == null) return;
        chatList.removeAllViews();
        chatList.setVisibility(receiver == null ? View.GONE : View.VISIBLE);
        if (receiver != null)
            receiver.setCallback(
                    item -> {
                        if (getActivity() != null)
                            getActivity()
                                    .runOnUiThread(
                                            () -> {
                                                if (chatReceiver != receiver || chatList == null)
                                                    return;
                                                chatList.addView(
                                                        MobileUi.text(
                                                                requireContext(),
                                                                item.getAuthorName()
                                                                        + "\n"
                                                                        + item.getMessage(),
                                                                14));
                                                while (chatList.getChildCount() > 30)
                                                    chatList.removeViewAt(0);
                                            });
                    });
    }

    @Override
    public void updateSuggestions(VideoGroup group) {
        if (group != null && group.isChapters() && chapters != null) {
            chapterGroup = group;
            chapterCards.clear();
            activeChapter = -1;
            chapters.removeAllViews();
            chapters.setVisibility(View.VISIBLE);
            TextView heading = MobileUi.text(requireContext(), getString(R.string.mobile_watch_chapters) + "  " + group.getVideos().size(), 16);
            heading.setTypeface(null, android.graphics.Typeface.BOLD);
            chapters.addView(heading);
            HorizontalScrollView scroll = new HorizontalScrollView(requireContext());
            chapterScroll = scroll;
            scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout row = new LinearLayout(requireContext());
            row.setPadding(MobileUi.dp(requireContext(), 12), 0, MobileUi.dp(requireContext(), 12), MobileUi.dp(requireContext(), 12));
            for (Video chapter : group.getVideos()) {
                LinearLayout card = MobileUi.column(requireContext());
                MobileUi.browseFeedback(card, false);
                card.setPadding(MobileUi.dp(requireContext(), 10), MobileUi.dp(requireContext(), 10),
                        MobileUi.dp(requireContext(), 10), MobileUi.dp(requireContext(), 10));
                ImageView image = new ImageView(requireContext());
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                android.graphics.drawable.GradientDrawable border = round(requireContext().getColor(R.color.mobile_surface), 16);
                image.setBackground(border);
                image.setClipToOutline(true);
                image.setPadding(MobileUi.dp(requireContext(), 2), MobileUi.dp(requireContext(), 2), MobileUi.dp(requireContext(), 2), MobileUi.dp(requireContext(), 2));
                chapterCards.add(image);
                card.addView(image, new LinearLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 72)));
                Glide.with(this).load(chapter.cardImageUrl).into(image);
                TextView label = MobileUi.text(requireContext(), clock(chapter.startTimeMs) + "\n" + chapter.getTitle(), 12);
                label.setPadding(0, MobileUi.dp(requireContext(), 6), 0, 0);
                label.setMaxLines(3);
                card.addView(label);
                card.setContentDescription(label.getText());
                card.setOnClickListener(v -> mPlaybackPresenter.onSuggestionItemClicked(chapter));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MobileUi.dp(requireContext(), 128), -2);
                params.setMargins(MobileUi.dp(requireContext(), 4), 0, MobileUi.dp(requireContext(), 8), 0);
                row.addView(card, params);
            }
            scroll.addView(row);
            chapters.addView(scroll);
            return;
        }
        if (suggestions != null) suggestions.update(group);
    }

    @Override
    public void removeSuggestions(VideoGroup group) {
        if (group != null && chapterGroup != null && group.getId() == chapterGroup.getId()) {
            chapterGroup = null;
            chapterCards.clear();
            chapterScroll = null;
            activeChapter = -1;
            chapters.removeAllViews();
            chapters.setVisibility(View.GONE);
            return;
        }
        if (group != null) {
            VideoGroup removal = VideoGroup.from(group.getVideos());
            removal.setId(group.getId());
            removal.setAction(VideoGroup.ACTION_REMOVE);
            suggestions.update(removal);
        }
    }

    @Override
    public int getSuggestionsIndex(VideoGroup group) {
        if (chapterGroup != null && group != null && chapterGroup.getId() == group.getId()) return 0;
        int index = suggestions.adapter.groupIndex(group);
        return index < 0 || chapterGroup == null ? index : index + 1;
    }

    @Override
    public VideoGroup getSuggestionsByIndex(int i) {
        return chapterGroup == null ? suggestions.adapter.groupAt(i)
                : i == 0 ? chapterGroup : suggestions.adapter.groupAt(i - 1);
    }

    @Override
    public void focusSuggestedItem(int i) {
        if (chapterGroup != null && chapterScroll != null && i >= 0 && i < chapterCards.size()) {
            chapterScroll.smoothScrollTo(MobileUi.dp(requireContext(), 140) * i, 0);
            return;
        }
        // TV focus callbacks arrive as recommendations load. Keep the phone watch
        // page at its current vertical position instead of revealing a suggestion.
    }

    @Override
    public void focusSuggestedItem(Video v) {
        if (v != null && v.isChapter && chapterGroup != null) {
            focusSuggestedItem(chapterGroup.getVideos().indexOf(v));
            return;
        }
        // Recommendation focus must not move the phone watch page after loading.
    }

    @Override
    public void resetSuggestedPosition() {
        if (suggestions != null) {
            androidx.recyclerview.widget.GridLayoutManager layout =
                    (androidx.recyclerview.widget.GridLayoutManager) suggestions.list.getLayoutManager();
            // The details header can be taller than the viewport; align its top.
            if (layout != null) layout.scrollToPositionWithOffset(0, 0);
        }
    }

    @Override
    public boolean isSuggestionsEmpty() {
        return chapterGroup == null && (suggestions == null || suggestions.adapter.isEmpty());
    }

    @Override
    public void clearSuggestions() {
        chapterGroup = null;
        chapterScroll = null;
        chapterCards.clear();
        activeChapter = -1;
        if (chapters != null) {
            chapters.removeAllViews();
            chapters.setVisibility(View.GONE);
        }
        if (suggestions != null) suggestions.clear();
    }

    @Override
    public void showOverlay(boolean show) {
        showControls(show);
    }

    @Override
    public boolean isOverlayShown() {
        return isControlsShown();
    }

    @Override
    public void showSuggestions(boolean show) {
        suggestionsRequested = show;
        if (show) {
            ((MobilePlaybackActivity) requireActivity()).exitFullscreen();
            suggestions.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean isSuggestionsShown() {
        return suggestionsRequested && suggestions != null && suggestions.getVisibility() == View.VISIBLE;
    }

    @Override
    public void showControls(boolean show) {
        if (!show && dragging) return;
        if (controls != null) {
            controls.setVisibility(show ? View.VISIBLE : View.GONE);
            mPlaybackPresenter.onControlsShown(show);
        }
    }

    @Override
    public boolean isControlsShown() {
        return controls != null && controls.getVisibility() == View.VISIBLE;
    }

    @Override
    public int getButtonState(int id) {
        Action action = actions.get(id);
        return states.containsKey(id)
                ? states.get(id)
                : action instanceof MultiAction ? ((MultiAction) action).getIndex() : BUTTON_OFF;
    }

    @Override
    public void setButtonState(int id, int state) {
        states.put(id, state);
        Action a = actions.get(id);
        if (a instanceof MultiAction && state >= 0) ((MultiAction) a).setIndex(state);
        Button b = buttons.get(id);
        if (b != null) {
            b.setEnabled(state != BUTTON_DISABLED);
            if (id == R.id.lb_control_closed_captioning) {
                b.setText("CC");
                b.setSelected(state == BUTTON_ON);
                b.setContentDescription(
                        state == BUTTON_ON ? "Turn captions off" : "Turn captions on");
            } else if (id == R.id.action_thumbs_down) {
                b.setContentDescription(a == null ? "Dislike" : a.getLabel1());
            } else if (id == R.id.action_thumbs_up) {
                updateLikeButton();
            } else if (id == R.id.action_chat) {
                b.setText(getString(R.string.mobile_watch_comments) + "  ›");
            } else if (id == R.id.action_subscribe) {
                b.setText(getString(state == BUTTON_ON ? R.string.subscribed_to_channel : R.string.action_subscribe));
            } else if (a != null && a.getLabel1() != null && id != R.id.action_channel)
                b.setText(a.getLabel1());
            b.setSelected(state == BUTTON_ON);
            int color = state == BUTTON_ON ? MobileUi.accent(requireContext()) : Color.WHITE;
            b.setTextColor(color);
            for (android.graphics.drawable.Drawable icon : b.getCompoundDrawables())
                if (icon != null) icon.setTint(color);
            if (b.getForeground() != null)
                b.getForeground().setTint(state == BUTTON_ON ? MobileUi.accent(requireContext()) : Color.WHITE);
        }
    }

    @Override
    public void setChannelIcon(String url) {
        if (avatar != null && isAdded() && getView() != null)
            Glide.with(this).load(url).circleCrop().into(avatar);
    }

    @Override
    public void setSeekPreviewTitle(String text) {
        if (seekTitle != null) seekTitle.setText(text);
    }

    @Override
    public void setNextTitle(Video video) {}

    @Override
    public void showDebugInfo(boolean show) {
        if (debugManager == null && mPlayer != null)
            debugManager = new DebugInfoManager(debug, mPlayer, mPlayerInitializer);
        if (debugManager != null) debugManager.show(show);
    }

    @Override
    public void showSubtitles(boolean show) {
        super.showSubtitles(show);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPlayer != null) {
            resumePlaybackService();
            mPlaybackPresenter.setView(this);
            mPlaybackPresenter.onViewResumed();
        }
    }

    @Override
    public void onPause() {
        mPlaybackPresenter.onViewPaused();
        super.onPause();
    }

    @Override
    public void restartEngine() {
        if (debugManager != null) {
            debugManager.show(false);
            debugManager = null;
        }
        super.restartEngine();
    }

    public void stopPlayback() {
        if (playbackFinished) return;
        playbackFinished = true;
        if (bufferingIndicator != null) bufferingIndicator.release();
        if (debugManager != null) {
            debugManager.show(false);
            debugManager = null;
        }
        releasePlayer();
        if (mPlaybackPresenter.getPlayer() == this) mPlaybackPresenter.onFinish();
        detachPresenter();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        if (getVideo() != null) {
            state.putString("video", getVideo().videoId);
            state.putString("videoData", getVideo().toString());
        }
        state.putLong("position", getPositionMs());
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        if (chatReceiver != null) {
            chatReceiver.setCallback(null);
            chatReceiver = null;
        }
        if (taps != null) taps.controller(null);
        overlay.player(null).playerView(null).performListener(null);
        storyboard.init(null, 0);
        stopPlayback();
        buttons.clear();
        avatar = preview = poster = null;
        seekPreview = null;
        dragging = false;
        previewRequest++;
        title = meta = time = quality = seekTitle = null;
        chapters = null;
        chapterGroup = null;
        chapterScroll = null;
        chapterCards.clear();
        super.onDestroyView();
    }

    private void detachPresenter() {
        // getView() deliberately returns null for a finishing Activity. Controllers retain
        // getPlayer(), so compare that reference when releasing ownership of this fragment.
        if (mPlaybackPresenter != null && mPlaybackPresenter.getPlayer() == this) {
            mPlaybackPresenter.onViewDestroyed();
            mPlaybackPresenter.setView(null);
        }
    }

    @Override
    public void onDestroy() {
        detachPresenter();
        super.onDestroy();
    }
}
