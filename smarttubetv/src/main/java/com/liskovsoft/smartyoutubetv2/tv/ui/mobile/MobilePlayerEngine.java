package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.trackselection.*;
import com.google.android.exoplayer2.ui.PlayerView;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.manager.PlayerUI;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.ExoFormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;
import com.liskovsoft.smartyoutubetv2.common.prefs.*;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.other.BackboneQueueNavigator;

import java.io.InputStream;
import java.util.List;

/** Playback coordination shared by the phone UI, independent of Leanback fragments and glue. */
public abstract class MobilePlayerEngine extends Fragment
        implements PlaybackView,
                com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.PlayerView {
    private static final String TAG = "MobilePlayerEngine";
    protected PlaybackPresenter mPlaybackPresenter;
    protected ExoPlayerController mExoPlayerController;
    protected ExoPlayerInitializer mPlayerInitializer;
    protected SimpleExoPlayer mPlayer;
    protected PlayerView surface;
    private MediaSessionCompat mMediaSession;
    private MediaSessionConnector mMediaSessionConnector;
    private boolean mIsEngineBlocked;
    private boolean playbackServiceStarted;
    private Player.EventListener playbackServiceListener;
    private int zoom = 100, rotation;
    private float aspect;
    private boolean flip;
    private final MobileCaptions captions = new MobileCaptions();
    private boolean subtitlesVisible = true;

    protected PlayerData getPlayerData() {
        return PlayerData.instance(requireContext());
    }

    protected PlayerTweaksData getPlayerTweaksData() {
        return PlayerTweaksData.instance(requireContext());
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mPlaybackPresenter = PlaybackPresenter.instance(requireContext());
        mPlaybackPresenter.setView(this);
        mExoPlayerController = new ExoPlayerController(requireContext(), mPlaybackPresenter);
        mExoPlayerController.setPlayerView(this);
        mPlayerInitializer = new ExoPlayerInitializer(requireContext());
    }

    protected void initializePlayer() {
        if (mPlayer != null) return;
        RestoreTrackSelector selector =
                new RestoreTrackSelector(new AdaptiveTrackSelection.Factory());
        selector.requireSupportedFormats();
        mExoPlayerController.setTrackSelector(selector);
        CustomOverridesRenderersFactory renderers =
                new CustomOverridesRenderersFactory(requireContext());
        renderers.setEnableDecoderFallback(true);
        mPlayer = mPlayerInitializer.createPlayer(requireContext(), renderers, selector, 1_000);
        playbackServiceListener = new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int state) {
                // Reserve protection while the initial source is still being resolved.
                if (state == Player.STATE_IDLE) return;
                boolean active = playWhenReady
                        && (state == Player.STATE_BUFFERING || state == Player.STATE_READY);
                updatePlaybackService(active);
                if (state == Player.STATE_READY) prepareCaptions();
            }

            @Override
            public void onTracksChanged(com.google.android.exoplayer2.source.TrackGroupArray groups,
                    TrackSelectionArray selections) {
                if (mPlayer.getPlaybackState() == Player.STATE_READY) prepareCaptions();
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                updatePlaybackService(false);
            }
        };
        mPlayer.addListener(playbackServiceListener);
        mExoPlayerController.setPlayerView(this);
        mExoPlayerController.setPlayer(mPlayer);
        surface.setPlayer(mPlayer);
        createMediaSession();
        updatePlaybackService(true);
        onPlayerCreated();
        mPlaybackPresenter.setView(this);
        mPlaybackPresenter.onEngineInitialized();
    }

    protected void onPlayerCreated() {}

    private void prepareCaptions() {
        FormatItem preferred = captions.prepare(mExoPlayerController.getSubtitleFormats(),
                getPlayerData().getLastSubtitleFormats());
        if (preferred != null) mExoPlayerController.selectFormat(preferred);
        updateCaptionVisibility();
    }

    private void updateCaptionVisibility() {
        if (surface != null && surface.getSubtitleView() != null) {
            surface.getSubtitleView().setVisibility(subtitlesVisible && captions.isEnabled()
                    ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
        }
    }

    @Override
    public void showSubtitles(boolean show) {
        subtitlesVisible = show;
        updateCaptionVisibility();
    }

    protected void resumePlaybackService() {
        if (mPlayer != null && mPlayer.getPlayWhenReady()
                && mPlayer.getPlaybackError() == null) updatePlaybackService(true);
    }

    private void updatePlaybackService(boolean active) {
        if (active == playbackServiceStarted) return;
        if (active) {
            playbackServiceStarted = MobilePlaybackService.start(requireContext(),
                    mMediaSession == null ? null : mMediaSession.getSessionToken());
        } else {
            MobilePlaybackService.stop(requireContext());
            playbackServiceStarted = false;
        }
    }

    protected void releasePlayer() {
        if (mPlayer == null) return;
        mPlayer.removeListener(playbackServiceListener);
        playbackServiceListener = null;
        // Stop protection before releasing the player, including while it is buffering.
        MobilePlaybackService.stop(requireContext());
        playbackServiceStarted = false;
        mPlaybackPresenter.onEngineReleased();
        if (mMediaSessionConnector != null) {
            mMediaSessionConnector.setPlayer(null);
            mMediaSessionConnector.setQueueNavigator(null);
            mMediaSessionConnector.setMediaMetadataProvider(null);
            mMediaSessionConnector = null;
        }
        if (mMediaSession != null) {
            mMediaSession.setActive(false);
            mMediaSession.release();
            mMediaSession = null;
        }
        surface.setPlayer(null);
        mPlayerInitializer.release();
        mExoPlayerController.release();
        mPlayer = null;
        captions.reset();
        updateCaptionVisibility();
    }

    @Override
    public void restartEngine() {
        releasePlayer();
        initializePlayer();
    }

    @Override
    public void reloadPlayback() {
        if (mPlayer != null) {
            mPlaybackPresenter.onEngineReleased();
            mPlaybackPresenter.onEngineInitialized();
        }
    }

    @Override
    public Video getVideo() {
        return mExoPlayerController == null ? null : mExoPlayerController.getVideo();
    }

    @Override
    public void resetPlayerState() {
        captions.reset();
        updateCaptionVisibility();
        mExoPlayerController.resetPlayerState();
    }

    @Override
    public boolean isEmbed() {
        return false;
    }

    @Override
    public void finish() {
        if (getActivity() != null) getActivity().finish();
    }

    @Override
    public void finishReally() {
        finish();
    }

    private void applyTransform() {
        if (surface == null) return;
        android.view.View video = surface.getVideoSurfaceView();
        if (video != null) {
            float scale = zoom > 0 ? zoom / 100f : 1f;
            video.setScaleX(flip ? -scale : scale);
            video.setScaleY(scale);
            video.setRotation(rotation);
        }
        if (aspect > 0) {
            com.google.android.exoplayer2.ui.AspectRatioFrameLayout frame =
                    surface.findViewById(com.google.android.exoplayer2.ui.R.id.exo_content_frame);
            if (frame != null) frame.setAspectRatio(aspect);
        }
    }

    private void createMediaSession() {
        if (VERSION.SDK_INT <= 19 || getContext() == null) {
            // Fix Android 4.4 bug: java.lang.IllegalArgumentException: MediaButtonReceiver
            // component may not be null
            return;
        }
        // NOTE: No way to disable only a notifications. We need to disable the media session
        // instead.
        boolean disableNotifications = getPlayerTweaksData().isPlaybackNotificationsDisabled();
        mMediaSession =
                new MediaSessionCompat(
                        getContext().getApplicationContext(),
                        getContext()
                                .getPackageName()); // NOTE: mem leak fix (SegmentTimelineElement)
        mMediaSession.setActive(!disableNotifications);
        mMediaSessionConnector = new MediaSessionConnector(mMediaSession);
        try {
            mMediaSessionConnector.setPlayer(mPlayer);
        } catch (NoSuchMethodError e) {
            // Android 9, Sony
            // No virtual method setState(IJFJ)Landroid/media/session/PlaybackState$Builder;
            // in class Landroid/media/session/PlaybackState$Builder;
            return;
        }
        // NOTE: Don't set to null. This won't disable a notifications but makes them empty.
        mMediaSessionConnector.setMediaMetadataProvider(
                player -> {
                    if (getVideo() == null) {
                        return null;
                    }
                    MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
                    metadataBuilder.putString(
                            MediaMetadataCompat.METADATA_KEY_MEDIA_ID, getVideo().videoId);
                    metadataBuilder.putString(
                            MediaMetadataCompat.METADATA_KEY_TITLE, getVideo().getTitleFull());
                    metadataBuilder.putString(
                            MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                            getVideo().getTitleFull());
                    metadataBuilder.putString(
                            MediaMetadataCompat.METADATA_KEY_ARTIST, getVideo().getAuthor());
                    metadataBuilder.putString(
                            MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                            Helpers.toString(getVideo().getSecondTitleFull()));
                    metadataBuilder.putString(
                            MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                            getVideo().getCardImageUrl());
                    metadataBuilder.putLong(
                            MediaMetadataCompat.METADATA_KEY_DURATION, getDurationMs());
                    return metadataBuilder.build();
                });
        mMediaSessionConnector.setQueueNavigator(
                new BackboneQueueNavigator() {
                    @Override
                    public long getSupportedQueueNavigatorActions(Player player) {
                        return PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
                    }

                    @Override
                    public void onSkipToPrevious(
                            Player player, ControlDispatcher controlDispatcher) {
                        mPlaybackPresenter.onPreviousClicked();
                    }

                    @Override
                    public void onSkipToNext(Player player, ControlDispatcher controlDispatcher) {
                        mPlaybackPresenter.onNextClicked();
                    }
                });
        // Fix exoplayer pause when switching AFR. The code seems buggy.
        mMediaSessionConnector.setControlDispatcher(
                new DefaultControlDispatcher() {
                    @Override
                    public boolean dispatchSetPlayWhenReady(Player player, boolean playWhenReady) {
                        // Fix exoplayer pause after activity is resumed (AFR switching).
                        // It's tied to activity state transitioning because window has different
                        // mode.
                        // NOTE: may be a problems with background playback or bluetooth button
                        // events
                        if (System.currentTimeMillis() - getPlayerData().getAfrSwitchTimeMs()
                                < 5_000) {
                            return false;
                        }
                        return super.dispatchSetPlayWhenReady(player, playWhenReady);
                    }
                });
    }

    // Begin Engine Events
    @Override
    public void openSabr(MediaItemFormatInfo formatInfo) {
        mExoPlayerController.openSabr(formatInfo);
    }

    @Override
    public void openDash(MediaItemFormatInfo formatInfo) {
        mExoPlayerController.openDash(formatInfo);
    }

    @Override
    public void openDash(InputStream dashManifest) {
        mExoPlayerController.openDash(dashManifest);
    }

    @Override
    public void openDashUrl(String dashManifestUrl) {
        mExoPlayerController.openDashUrl(dashManifestUrl);
    }

    @Override
    public void openHlsUrl(String hlsPlaylistUrl) {
        mExoPlayerController.openHlsUrl(hlsPlaylistUrl);
    }

    @Override
    public void openUrlList(List<String> urlList) {
        mExoPlayerController.openUrlList(urlList);
    }

    @Override
    public void openMerged(MediaItemFormatInfo formatInfo, String hlsPlaylistUrl) {
        mExoPlayerController.openMerged(formatInfo, hlsPlaylistUrl);
    }

    @Override
    public void openMerged(InputStream dashManifest, String hlsPlaylistUrl) {
        mExoPlayerController.openMerged(dashManifest, hlsPlaylistUrl);
    }

    @Override
    public long getPositionMs() {
        return mExoPlayerController.getPositionMs();
    }

    @Override
    public void setPositionMs(long positionMs) {
        mExoPlayerController.setPositionMs(positionMs);
    }

    @Override
    public long getDurationMs() {
        long durationMs = mExoPlayerController.getDurationMs();
        long liveDurationMs = getVideo() != null ? getVideo().getLiveDurationMs() : 0;
        if (durationMs > Video.MAX_LIVE_DURATION_MS && liveDurationMs != 0) {
            durationMs = liveDurationMs;
        }
        return durationMs;
    }

    @Override
    public void setPlayWhenReady(boolean play) {
        mExoPlayerController.setPlayWhenReady(play);
        // An explicit pause can arrive before the initial source leaves STATE_IDLE.
        if (!play) updatePlaybackService(false);
    }

    @Override
    public boolean getPlayWhenReady() {
        return mExoPlayerController.getPlayWhenReady();
    }

    @Override
    public boolean isPlaying() {
        return mExoPlayerController.isPlaying();
    }

    @Override
    public boolean isLoading() {
        return mExoPlayerController.isLoading();
    }

    @Override
    public List<FormatItem> getVideoFormats() {
        return mExoPlayerController.getVideoFormats();
    }

    @Override
    public List<FormatItem> getAudioFormats() {
        return mExoPlayerController.getAudioFormats();
    }

    @Override
    public List<FormatItem> getSubtitleFormats() {
        List<FormatItem> formats = mExoPlayerController.getSubtitleFormats();
        if (formats == null || captions.isEnabled()) return formats;
        List<FormatItem> result = new java.util.ArrayList<>();
        for (FormatItem format : formats) {
            result.add(ExoFormatItem.fromWithSelection(format.getTrack(), format.isDefault()));
        }
        return result;
    }

    @Override
    public void setFormat(FormatItem formatItem) {
        if (formatItem != null && formatItem.getType() == FormatItem.TYPE_SUBTITLE) {
            captions.select(formatItem);
            updateCaptionVisibility();
            // Keep the text renderer running while hidden so CC can reveal the current cue.
            if (formatItem.isDefault()) return;
        }
        // Android 4.4 fix for format selection dialog (player destroyed when dialog is focused)
        mExoPlayerController.selectFormat(formatItem);
    }

    @Override
    public FormatItem getVideoFormat() {
        return mExoPlayerController.getVideoFormat();
    }

    @Override
    public FormatItem getAudioFormat() {
        return mExoPlayerController.getAudioFormat();
    }

    @Override
    public FormatItem getSubtitleFormat() {
        return captions.isEnabled() ? mExoPlayerController.getSubtitleFormat() : FormatItem.SUBTITLE_NONE;
    }

    @Override
    public boolean isEngineInitialized() {
        return mPlayer != null;
    }

    @Override
    public void blockEngine(boolean block) {
        mIsEngineBlocked = block;
    }

    @Override
    public boolean isEngineBlocked() {
        return mIsEngineBlocked;
    }

    @Override
    public boolean isInPIPMode() {
        MobilePlaybackActivity playbackActivity = (MobilePlaybackActivity) getActivity();
        if (playbackActivity == null) {
            return false;
        }
        // Old api fix
        return playbackActivity.isInPictureInPictureMode();
    }

    @Override
    public boolean containsMedia() {
        return mExoPlayerController.containsMedia();
    }

    @Override
    public float getSpeed() {
        return mExoPlayerController.getSpeed();
    }

    @Override
    public void setSpeed(float speed) {
        mExoPlayerController.setSpeed(speed);
        // NOTE: Real speed isn't changed immediately, so use supplied speed data
        setButtonState(
                R.id.action_video_speed, speed != 1.0f ? PlayerUI.BUTTON_ON : PlayerUI.BUTTON_OFF);
    }

    @Override
    public float getPitch() {
        return mExoPlayerController.getPitch();
    }

    @Override
    public void setPitch(float pitch) {
        mExoPlayerController.setPitch(pitch);
    }

    @Override
    public float getVolume() {
        return mExoPlayerController.getVolume();
    }

    @Override
    public void setVolume(float volume) {
        mExoPlayerController.setVolume(volume);
    }

    @Override
    public int getResizeMode() {
        return surface.getResizeMode();
    }

    @Override
    public void setResizeMode(int mode) {
        surface.setResizeMode(mode);
    }

    @Override
    public void setZoomPercents(int percents) {
        zoom = percents;
        applyTransform();
    }

    @Override
    public void setAspectRatio(float ratio) {
        aspect = ratio;
        applyTransform();
    }

    @Override
    public void setRotationAngle(int angle) {
        rotation = angle;
        applyTransform();
    }

    @Override
    public void setVideoFlipEnabled(boolean enabled) {
        flip = enabled;
        applyTransform();
    }

    @Override
    public void setVideoGravity(int gravity) {
        android.view.View frame =
                surface.findViewById(com.google.android.exoplayer2.ui.R.id.exo_content_frame);
        if (frame != null
                && frame.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams) {
            android.widget.FrameLayout.LayoutParams params =
                    (android.widget.FrameLayout.LayoutParams) frame.getLayoutParams();
            params.gravity = gravity;
            frame.setLayoutParams(params);
        }
    }
}
