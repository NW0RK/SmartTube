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
    private MobileFeedView suggestions;
    private TextView title, meta, time, quality, seekTitle, description;
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
                    }
                    setIcon(play, getPlayWhenReady() ? "pause" : "play", 30);
                    root.setKeepScreenOn(isPlaying() && !isInPIPMode());
                    overlay.seekSeconds(getPlayerData().getSeekIncrementMs() / 1000);
                    if (isPlaying()) poster.setVisibility(View.GONE);
                    seek.setEnabled(mPlayer != null && mPlayer.isCurrentWindowSeekable());
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
        b.setBackgroundColor(Color.TRANSPARENT);
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
        button.setBackground(round(requireContext().getColor(R.color.mobile_surface), 24));
    }

    private void skip(long delta) {
        if (mPlayer == null || !mPlayer.isCurrentWindowSeekable()) return;
        long position = Math.max(0, Math.min(getDurationMs(), getPositionMs() + delta));
        setPositionMs(position);
        mPlaybackPresenter.onSeekPositionChanged(position);
        mPlaybackPresenter.onSeekEnd();
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
            android.graphics.drawable.GradientDrawable border = round(requireContext().getColor(R.color.mobile_surface), 8);
            if (i == selected) border.setStroke(MobileUi.dp(requireContext(), 2), MobileUi.accent(requireContext()));
            chapterCards.get(i).setBackground(border);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        root = MobileUi.column(requireContext());
        root.setBackgroundColor(MobileUi.background(requireContext()));
        bar =
                MobileUi.watchToolbar(
                        requireContext(), () -> requireActivity().onBackPressed());
        root.addView(bar);
        bar.setBackgroundColor(MobileUi.background(requireContext()));
        TextView brand = (TextView) bar.getChildAt(1);
        brand.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        brand.setTypeface(null, android.graphics.Typeface.BOLD);
        MobileUi.addButton(bar, iconButton("search", "Search", () -> action(R.id.action_search)));
        MobileUi.addButton(bar, iconButton("more", "Playback options", this::playbackOptions));
        stage = (FrameLayout) inflater.inflate(R.layout.mobile_player_stage, parent, false);
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
        stage.addView(controls, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout top = new LinearLayout(requireContext());
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(MobileUi.dp(requireContext(), 8), 0, MobileUi.dp(requireContext(), 8), 0);
        MobileUi.addButton(top, iconButton("down", "Close player", () -> requireActivity().onBackPressed()));
        top.addView(new View(requireContext()), new LinearLayout.LayoutParams(0, 1, 1));
        Button captions = actionButton(top, R.id.lb_control_closed_captioning, "CC");
        captions.setBackgroundColor(Color.TRANSPARENT);
        captions.setTypeface(null, android.graphics.Typeface.BOLD);
        Button settings = iconButton("settings", "Playback settings", this::settings);
        MobileUi.addButton(top, settings);
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
        play.setBackground(round(0x55000000, 36));
        LinearLayout transport = new LinearLayout(requireContext());
        transport.setGravity(Gravity.CENTER);
        Button rewind = iconButton("back10", getString(R.string.mobile_watch_rewind), () -> skip(-10000));
        Button forward = iconButton("forward10", getString(R.string.mobile_watch_forward), () -> skip(10000));
        setIcon(rewind, "back10", 32);
        setIcon(forward, "forward10", 32);
        transport.addView(rewind, new LinearLayout.LayoutParams(MobileUi.dp(requireContext(), 64), -1));
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                MobileUi.dp(requireContext(), 64), MobileUi.dp(requireContext(), 64));
        playParams.setMargins(MobileUi.dp(requireContext(), 20), 0, MobileUi.dp(requireContext(), 20), 0);
        transport.addView(play, playParams);
        transport.addView(forward, new LinearLayout.LayoutParams(MobileUi.dp(requireContext(), 64), -1));
        controls.addView(transport, new FrameLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 64), Gravity.CENTER));
        LinearLayout bottom = new LinearLayout(requireContext());
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        time = MobileUi.text(requireContext(), "0:00 / 0:00", 12);
        time.setPadding(MobileUi.dp(requireContext(), 16), 0, 0, 0);
        bottom.addView(time, new LinearLayout.LayoutParams(0, -2, 1));
        fullscreen =
                MobileUi.button(
                        requireContext(),
                        "\u26f6",
                        () -> ((MobilePlaybackActivity) requireActivity()).toggleFullscreen());
        fullscreen.setContentDescription(getString(R.string.mobile_fullscreen));
        fullscreen.setBackgroundColor(Color.TRANSPARENT);
        setIcon(fullscreen, "fullscreen", 22);
        MobileUi.addButton(bottom, fullscreen);
        fullscreen.setLayoutParams(new LinearLayout.LayoutParams(MobileUi.dp(requireContext(), 48), MobileUi.dp(requireContext(), 48)));
        seek = new MobileSegmentSeekBar(requireContext());
        seek.setContentDescription("Video position");
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(MobileUi.accent(requireContext())));
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(MobileUi.accent(requireContext())));
        LinearLayout footer = new LinearLayout(requireContext());
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.addView(seek, new LinearLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 32)));
        footer.addView(bottom, new LinearLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 48)));
        controls.addView(footer, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        buffering = new ProgressBar(requireContext());
        buffering.setContentDescription("Buffering video");
        bufferingIndicator = new MobileBufferingIndicator(buffering, handler);
        stage.addView(
                buffering,
                new FrameLayout.LayoutParams(
                        MobileUi.dp(requireContext(), 48),
                        MobileUi.dp(requireContext(), 48),
                        Gravity.CENTER));
        overlay = new YouTubeOverlay(requireContext(), null);
        stage.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        overlay.setVisibility(View.GONE);
        preview = new ImageView(requireContext());
        stage.addView(
                preview,
                new FrameLayout.LayoutParams(
                        MobileUi.dp(requireContext(), 120),
                        MobileUi.dp(requireContext(), 68),
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        preview.setVisibility(View.GONE);
        seekTitle = MobileUi.text(requireContext(), "", 12);
        stage.addView(seekTitle, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START));
        seekTitle.setVisibility(View.GONE);
        debug = new FrameLayout(requireContext());
        stage.addView(debug, new FrameLayout.LayoutParams(-1, -1));
        details = MobileUi.column(requireContext());
        details.setBackgroundColor(MobileUi.background(requireContext()));
        title = MobileUi.text(requireContext(), "", 18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        details.addView(title);
        meta = MobileUi.text(requireContext(), "", 13);
        meta.setTextColor(requireContext().getColor(R.color.mobile_text_secondary));
        meta.setPadding(MobileUi.dp(requireContext(), 16), 0, MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 12));
        meta.setOnClickListener(v -> action(R.id.action_info));
        details.addView(meta);
        LinearLayout channel = new LinearLayout(requireContext());
        channel.setGravity(Gravity.CENTER_VERTICAL);
        channel.setPadding(MobileUi.dp(requireContext(), 16), 0, MobileUi.dp(requireContext(), 12), MobileUi.dp(requireContext(), 8));
        avatar = new ImageView(requireContext());
        channel.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        MobileUi.dp(requireContext(), 48), MobileUi.dp(requireContext(), 48)));
        Button author = actionButton(channel, R.id.action_channel, "Channel");
        author.setBackgroundColor(Color.TRANSPARENT);
        author.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        author.setTypeface(null, android.graphics.Typeface.BOLD);
        author.setMaxLines(1);
        author.setEllipsize(android.text.TextUtils.TruncateAt.END);
        author.setLayoutParams(
                new LinearLayout.LayoutParams(0, MobileUi.dp(requireContext(), 48), 1));
        avatar.setOnClickListener(v -> action(R.id.action_channel));
        Button subscribe = actionButton(channel, R.id.action_subscribe, "Subscribe");
        subscribe.setBackground(round(Color.WHITE, 24));
        subscribe.setTextColor(Color.BLACK);
        details.addView(channel);
        HorizontalScrollView strip = new HorizontalScrollView(requireContext());
        strip.setHorizontalScrollBarEnabled(false);
        strip.setPadding(MobileUi.dp(requireContext(), 12), 0, MobileUi.dp(requireContext(), 12), MobileUi.dp(requireContext(), 8));
        LinearLayout row = new LinearLayout(requireContext());
        strip.addView(row);
        details.addView(strip);
        LinearLayout reactions = new LinearLayout(requireContext());
        reactions.setGravity(Gravity.CENTER_VERTICAL);
        reactions.setBackground(round(requireContext().getColor(R.color.mobile_surface), 24));
        LinearLayout.LayoutParams reactionParams = new LinearLayout.LayoutParams(-2, MobileUi.dp(requireContext(), 48));
        reactionParams.setMargins(MobileUi.dp(requireContext(), 4), MobileUi.dp(requireContext(), 4), MobileUi.dp(requireContext(), 4), MobileUi.dp(requireContext(), 4));
        row.addView(reactions, reactionParams);
        Button like = actionButton(reactions, R.id.action_thumbs_up, "Like");
        decorate(like, "like");
        like.setBackgroundColor(Color.TRANSPARENT);
        like.setLayoutParams(new LinearLayout.LayoutParams(-2, -1));
        View divider = new View(requireContext());
        divider.setBackgroundColor(0xff3a3c40);
        reactions.addView(divider, new LinearLayout.LayoutParams(MobileUi.dp(requireContext(), 1), MobileUi.dp(requireContext(), 24)));
        Button dislike = actionButton(reactions, R.id.action_thumbs_down, "");
        dislike.setContentDescription("Dislike");
        dislike.setBackgroundColor(Color.TRANSPARENT);
        dislike.setLayoutParams(new LinearLayout.LayoutParams(MobileUi.dp(requireContext(), 48), -1));
        setIcon(dislike, "dislike", 22);
        decorate(actionButton(row, R.id.action_share, "Share"), "share");
        Button download = MobileUi.button(requireContext(), getString(R.string.mobile_download),
                () -> MobileDownload.open(requireContext(), getVideo() == null ? null : getVideo().videoId));
        decorate(download, "download");
        MobileUi.addButton(row, download);
        actionButton(row, R.id.action_playlist_add, "Save");
        MobileUi.addButton(
                row,
                MobileUi.button(
                        requireContext(), getString(R.string.mobile_more), this::playbackOptions));
        description = MobileUi.text(requireContext(), getString(R.string.mobile_watch_description), 14);
        description.setMaxLines(3);
        description.setEllipsize(android.text.TextUtils.TruncateAt.END);
        description.setBackground(MobileUi.rounded(requireContext(), requireContext().getColor(R.color.mobile_surface)));
        description.setOnClickListener(v -> action(R.id.action_info));
        LinearLayout.LayoutParams card = new LinearLayout.LayoutParams(-1, -2);
        card.setMargins(MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 4), MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 12));
        details.addView(description, card);
        Button comments = actionButton(details, R.id.action_chat, getString(R.string.mobile_watch_comments) + "  ›");
        comments.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        comments.setBackground(MobileUi.rounded(requireContext(), requireContext().getColor(R.color.mobile_surface)));
        LinearLayout.LayoutParams commentCard = new LinearLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 52));
        commentCard.setMargins(MobileUi.dp(requireContext(), 16), 0, MobileUi.dp(requireContext(), 16), MobileUi.dp(requireContext(), 12));
        comments.setLayoutParams(commentCard);
        chapters = MobileUi.column(requireContext());
        chapters.setBackgroundColor(Color.TRANSPARENT);
        chapters.setVisibility(View.GONE);
        details.addView(chapters);
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
                        wasPlaying = getPlayWhenReady();
                        setPlayWhenReady(false);
                        mPlaybackPresenter.onKeyDown(-1);
                    }

                    public void onProgressChanged(SeekBar bar, int value, boolean user) {
                        if (!user) return;
                        long position = getDurationMs() * value / 10000;
                        time.setText(clock(position) + " / " + clock(getDurationMs()));
                        seekTitle.setText(clock(position));
                        seekTitle.setVisibility(View.VISIBLE);
                        long[] positions = storyboard.getSeekPositions();
                        if (positions != null && positions.length > 0) {
                            int index = Arrays.binarySearch(positions, position);
                            index =
                                    index >= 0
                                            ? index
                                            : Math.min(
                                                    positions.length - 1, Math.max(0, -index - 2));
                            storyboard.getBitmap(
                                    index,
                                    bitmap -> {
                                        if (dragging && preview != null) {
                                            preview.setImageBitmap(bitmap);
                                            preview.setVisibility(View.VISIBLE);
                                        }
                                    });
                        }
                    }

                    public void onStopTrackingTouch(SeekBar bar) {
                        long position = getDurationMs() * bar.getProgress() / 10000;
                        setPositionMs(position);
                        mPlaybackPresenter.onSeekPositionChanged(position);
                        dragging = false;
                        setPlayWhenReady(wasPlaying);
                        mPlaybackPresenter.onSeekEnd();
                        preview.setVisibility(View.GONE);
                        seekTitle.setVisibility(View.GONE);
                    }
                });
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
            for (FormatItem saved : getPlayerData().getLastSubtitleFormats()) {
                for (FormatItem available : formats) {
                    if (!available.isDefault() && saved.equals(available)) next = available;
                }
                if (!next.isDefault()) break;
            }
            if (next.isDefault()) {
                for (FormatItem available : formats) {
                    if (!available.isDefault()) {
                        next = available;
                        break;
                    }
                }
            }
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

    public void resize() {
        if (stage == null || getActivity() == null) return;
        boolean full = ((MobilePlaybackActivity) getActivity()).isFullscreen() || isInPIPMode();
        bar.setVisibility(full ? View.GONE : View.VISIBLE);
        suggestions.setVisibility(full ? View.GONE : View.VISIBLE);
        stage.setLayoutParams(
                new LinearLayout.LayoutParams(
                        -1, full ? 0 : Math.max(1, root.getWidth()) * 9 / 16, full ? 1 : 0));
        suggestions.resize();
        fullscreen.setContentDescription(
                getString(full ? R.string.mobile_exit_fullscreen : R.string.mobile_fullscreen));
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        root.post(this::resize);
    }

    @Override
    public void setVideo(Video v) {
        Video previous = getVideo();
        if (v == null || previous == null || !Objects.equals(previous.videoId, v.videoId))
            suggestionsRequested = false;
        if (v != null && restoredPosition > 0 && Objects.equals(restoredId, v.videoId)) {
            v.pendingPosMs = restoredPosition;
            restoredPosition = 0;
        }
        mExoPlayerController.setVideo(v);
        if (title != null && v != null) {
            title.setText(v.getTitleFull());
            meta.setText(v.getSecondTitleFull());
            buttons.get(R.id.action_channel).setText(v.getAuthor());
            description.setText(v.description == null || v.description.trim().isEmpty()
                    ? getString(R.string.mobile_watch_description) : v.description);
        }
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
                card.setBackgroundColor(Color.TRANSPARENT);
                ImageView image = new ImageView(requireContext());
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                android.graphics.drawable.GradientDrawable border = round(requireContext().getColor(R.color.mobile_surface), 8);
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
        suggestions.list.scrollToPosition(Math.max(0, i));
    }

    @Override
    public void focusSuggestedItem(Video v) {
        if (v != null && v.isChapter && chapterGroup != null) {
            focusSuggestedItem(chapterGroup.getVideos().indexOf(v));
            return;
        }
        int i = suggestions.adapter.indexOf(v);
        if (i >= 0) suggestions.list.scrollToPosition(i);
    }

    @Override
    public void resetSuggestedPosition() {
        suggestions.list.scrollToPosition(0);
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
            } else if (id == R.id.action_chat) {
                b.setText(getString(R.string.mobile_watch_comments) + "  ›");
            } else if (id == R.id.action_subscribe) {
                b.setText(getString(state == BUTTON_ON ? R.string.subscribed_to_channel : R.string.action_subscribe));
            } else if (a != null && a.getLabel1() != null && id != R.id.action_channel)
                b.setText(a.getLabel1());
            b.setTextColor(id == R.id.action_subscribe ? Color.BLACK
                    : state == BUTTON_ON ? MobileUi.accent(requireContext()) : Color.WHITE);
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
        if (surface.getSubtitleView() != null)
            surface.getSubtitleView().setVisibility(show ? View.VISIBLE : View.GONE);
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
        title = meta = time = quality = seekTitle = description = null;
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
