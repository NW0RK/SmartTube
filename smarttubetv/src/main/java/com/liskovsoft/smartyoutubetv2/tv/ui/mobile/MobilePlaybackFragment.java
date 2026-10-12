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
    private LinearLayout root, controls, details, bar, chatList;
    private ChatReceiver chatReceiver;
    private FrameLayout stage, debug;
    private MobileFeedView suggestions;
    private TextView title, meta, time, quality, seekTitle;
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
                    play.setText(getPlayWhenReady() ? "\u2161" : "\u25b6");
                    root.setKeepScreenOn(isPlaying() && !isInPIPMode());
                    overlay.seekSeconds(getPlayerData().getSeekIncrementMs() / 1000);
                    if (isPlaying()) poster.setVisibility(View.GONE);
                    seek.setEnabled(mPlayer != null && mPlayer.isCurrentWindowSeekable());
                    updateBufferingIndicator();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        root = MobileUi.column(requireContext());
        bar =
                MobileUi.toolbar(
                        requireContext(), "SmartTube", () -> requireActivity().onBackPressed());
        root.addView(bar);
        stage = (FrameLayout) inflater.inflate(R.layout.mobile_player_stage, parent, false);
        surface = stage.findViewById(R.id.mobile_player_surface);
        surface.setUseController(false);
        poster = new ImageView(requireContext());
        poster.setScaleType(ImageView.ScaleType.FIT_CENTER);
        poster.setVisibility(View.GONE);
        stage.addView(poster, new FrameLayout.LayoutParams(-1, -1));
        root.addView(stage);
        controls = MobileUi.column(requireContext());
        controls.setBackgroundColor(0x66000000);
        stage.addView(controls, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout top = new LinearLayout(requireContext());
        top.setGravity(Gravity.END);
        actionButton(top, R.id.lb_control_closed_captioning, "CC");
        MobileUi.addButton(top, MobileUi.button(requireContext(), "\u2699", this::settings));
        controls.addView(top);
        FrameLayout middle = new FrameLayout(requireContext());
        controls.addView(middle, new LinearLayout.LayoutParams(-1, 0, 1));
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
        FrameLayout.LayoutParams center =
                new FrameLayout.LayoutParams(
                        MobileUi.dp(requireContext(), 56),
                        MobileUi.dp(requireContext(), 48),
                        Gravity.CENTER);
        middle.addView(play, center);
        LinearLayout bottom = new LinearLayout(requireContext());
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        time = MobileUi.text(requireContext(), "0:00", 12);
        bottom.addView(time, new LinearLayout.LayoutParams(0, -2, 1));
        fullscreen =
                MobileUi.button(
                        requireContext(),
                        "\u26f6",
                        () -> ((MobilePlaybackActivity) requireActivity()).toggleFullscreen());
        fullscreen.setContentDescription(getString(R.string.mobile_fullscreen));
        MobileUi.addButton(bottom, fullscreen);
        controls.addView(bottom);
        seek = new MobileSegmentSeekBar(requireContext());
        seek.setContentDescription("Video position");
        controls.addView(
                seek, new LinearLayout.LayoutParams(-1, MobileUi.dp(requireContext(), 48)));
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
        title = MobileUi.text(requireContext(), "", 20);
        details.addView(title);
        meta = MobileUi.text(requireContext(), "", 13);
        details.addView(meta);
        LinearLayout channel = new LinearLayout(requireContext());
        avatar = new ImageView(requireContext());
        channel.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        MobileUi.dp(requireContext(), 48), MobileUi.dp(requireContext(), 48)));
        Button author = actionButton(channel, R.id.action_channel, "Channel");
        author.setMaxLines(1);
        author.setEllipsize(android.text.TextUtils.TruncateAt.END);
        author.setLayoutParams(
                new LinearLayout.LayoutParams(0, MobileUi.dp(requireContext(), 48), 1));
        avatar.setOnClickListener(v -> action(R.id.action_channel));
        actionButton(channel, R.id.action_subscribe, "Subscribe");
        details.addView(channel);
        HorizontalScrollView strip = new HorizontalScrollView(requireContext());
        LinearLayout row = new LinearLayout(requireContext());
        strip.addView(row);
        details.addView(strip);
        actionButton(row, R.id.action_thumbs_up, "Like");
        actionButton(row, R.id.action_thumbs_down, "Dislike");
        actionButton(row, R.id.action_share, "Share");
        actionButton(row, R.id.action_playlist_add, "Save");
        actionButton(row, R.id.action_info, "Description");
        actionButton(row, R.id.action_chat, "Comments / chat");
        MobileUi.addButton(
                row,
                MobileUi.button(
                        requireContext(), getString(R.string.mobile_more), this::playbackOptions));
        chatList = MobileUi.column(requireContext());
        chatList.setVisibility(View.GONE);
        details.addView(chatList);
        quality = MobileUi.text(requireContext(), "", 12);
        details.addView(quality);
        suggestions =
                new MobileFeedView(
                        requireContext(),
                        mPlaybackPresenter::onSuggestionItemClicked,
                        mPlaybackPresenter::onSuggestionItemLongClicked,
                        mPlaybackPresenter::onScrollEnd);
        suggestions.adapter.setHeader(details);
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
        if (v != null && restoredPosition > 0 && Objects.equals(restoredId, v.videoId)) {
            v.pendingPosMs = restoredPosition;
            restoredPosition = 0;
        }
        mExoPlayerController.setVideo(v);
        if (title != null && v != null) {
            title.setText(v.getTitleFull());
            meta.setText(v.getSecondTitleFull());
            buttons.get(R.id.action_channel).setText(v.getAuthor());
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
        if (suggestions != null) suggestions.update(group);
    }

    @Override
    public void removeSuggestions(VideoGroup group) {
        if (group != null) {
            VideoGroup removal = VideoGroup.from(group.getVideos());
            removal.setId(group.getId());
            removal.setAction(VideoGroup.ACTION_REMOVE);
            suggestions.update(removal);
        }
    }

    @Override
    public int getSuggestionsIndex(VideoGroup group) {
        return suggestions.adapter.groupIndex(group);
    }

    @Override
    public VideoGroup getSuggestionsByIndex(int i) {
        return suggestions.adapter.groupAt(i);
    }

    @Override
    public void focusSuggestedItem(int i) {
        suggestions.list.scrollToPosition(Math.max(0, i));
    }

    @Override
    public void focusSuggestedItem(Video v) {
        int i = suggestions.adapter.indexOf(v);
        if (i >= 0) focusSuggestedItem(i);
    }

    @Override
    public void resetSuggestedPosition() {
        suggestions.list.scrollToPosition(0);
    }

    @Override
    public boolean isSuggestionsEmpty() {
        return suggestions == null || suggestions.adapter.isEmpty();
    }

    @Override
    public void clearSuggestions() {
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
        if (show) {
            ((MobilePlaybackActivity) requireActivity()).exitFullscreen();
            suggestions.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean isSuggestionsShown() {
        return suggestions != null && suggestions.getVisibility() == View.VISIBLE;
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
            } else if (a != null && a.getLabel1() != null && id != R.id.action_channel)
                b.setText(a.getLabel1());
            b.setTextColor(state == BUTTON_ON ? MobileUi.accent(requireContext()) : Color.WHITE);
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
        title = meta = time = quality = seekTitle = null;
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
