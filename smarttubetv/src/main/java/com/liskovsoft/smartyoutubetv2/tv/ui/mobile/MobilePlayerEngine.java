package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.*;
import android.os.*;
import androidx.fragment.app.Fragment;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.prefs.*;
import java.io.InputStream;
import java.util.List;

/** Watch-screen adapter; the bound service owns the engine and presenter state. */
public abstract class MobilePlayerEngine extends Fragment implements PlaybackView,
        com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.PlayerView {
    protected PlaybackPresenter mPlaybackPresenter;
    protected ExoPlayerController mExoPlayerController;
    protected ExoPlayerInitializer mPlayerInitializer;
    protected SimpleExoPlayer mPlayer;
    protected PlayerView surface;
    private MobilePlaybackSession session;
    private boolean bound;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            if (!isAdded() || getView() == null) return;
            session = ((MobilePlaybackService.LocalBinder) binder).session();
            session.attach((MobilePlaybackFragment) MobilePlayerEngine.this, surface);
            if (mPlaybackPresenter.getVideo() == null) ((MobilePlaybackFragment) MobilePlayerEngine.this).restoreVideoIfNeeded();
        }
        @Override public void onServiceDisconnected(ComponentName name) { session = null; mPlayer = null; }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        mPlaybackPresenter = PlaybackPresenter.instance(requireContext());
    }
    protected PlayerData getPlayerData() { return PlayerData.instance(requireContext()); }
    protected PlayerTweaksData getPlayerTweaksData() { return PlayerTweaksData.instance(requireContext()); }
    protected void initializePlayer() {
        if (!bound) bound = requireContext().bindService(new Intent(requireContext(), MobilePlaybackService.class), connection, Context.BIND_AUTO_CREATE);
    }
    final void sessionPlayerCreated(MobilePlaybackSession value) {
        boolean changed = mPlayer != value.mPlayer;
        mPlayer = value.mPlayer;
        mPlayerInitializer = value.mPlayerInitializer;
        mExoPlayerController = value.mExoPlayerController;
        if (changed) onPlayerCreated();
    }
    protected void onPlayerCreated() {}
    protected void resumePlaybackService() {
        if (session != null) session.attach((MobilePlaybackFragment) this, surface);
    }
    protected void releasePlayer() { if (session != null) session.finish(); }
    protected void resetMobileQuality() { if (session != null) session.resetMobileQuality(); }
    @Override public void onStop() {
        if (session != null && !isInPIPMode()) session.setVisible((MobilePlaybackFragment) this, false);
        super.onStop();
    }
    @Override public void onDestroyView() {
        if (session != null) session.detach((MobilePlaybackFragment) this);
        if (bound) { requireContext().unbindService(connection); bound = false; }
        session = null; mPlayer = null; mExoPlayerController = null; mPlayerInitializer = null;
        super.onDestroyView();
    }
    @Override public boolean usesPerVideoQuality() { return session == null ? false : session.usesPerVideoQuality(); }

    @Override public void selectUserFormat(FormatItem format) { if (session != null) session.selectUserFormat(format); }

    @Override public void restorePerVideoQuality() { if (session != null) session.restorePerVideoQuality(); }

    @Override public void showSubtitles(boolean show) { if (session != null) session.showSubtitles(show); }

    @Override public void restartEngine() { if (session != null) session.restartEngine(); }

    @Override public void reloadPlayback() { if (session != null) session.reloadPlayback(); }

    @Override public Video getVideo() { return session == null ? null : session.getVideo(); }

    @Override public void resetPlayerState() { if (session != null) session.resetPlayerState(); }

    @Override public boolean isEmbed() { return session == null ? false : session.isEmbed(); }

    @Override public void finish() { if (session != null) session.finish(); }

    @Override public void finishReally() { if (session != null) session.finishReally(); }

    @Override public void openSabr(MediaItemFormatInfo formatInfo) { if (session != null) session.openSabr(formatInfo); }

    @Override public void openDash(MediaItemFormatInfo formatInfo) { if (session != null) session.openDash(formatInfo); }

    @Override public void openDash(InputStream dashManifest) { if (session != null) session.openDash(dashManifest); }

    @Override public void openDashUrl(String dashManifestUrl) { if (session != null) session.openDashUrl(dashManifestUrl); }

    @Override public void openHlsUrl(String hlsPlaylistUrl) { if (session != null) session.openHlsUrl(hlsPlaylistUrl); }

    @Override public void openUrlList(List<String> urlList) { if (session != null) session.openUrlList(urlList); }

    @Override public void openMerged(MediaItemFormatInfo formatInfo, String hlsPlaylistUrl) { if (session != null) session.openMerged(formatInfo, hlsPlaylistUrl); }

    @Override public void openMerged(InputStream dashManifest, String hlsPlaylistUrl) { if (session != null) session.openMerged(dashManifest, hlsPlaylistUrl); }

    @Override public long getPositionMs() { return session == null ? 0 : session.getPositionMs(); }

    @Override public void setPositionMs(long positionMs) { if (session != null) session.setPositionMs(positionMs); }

    @Override public long getDurationMs() { return session == null ? 0 : session.getDurationMs(); }

    @Override public void setPlayWhenReady(boolean play) { if (session != null) session.setPlayWhenReady(play); }

    @Override public boolean getPlayWhenReady() { return session == null ? false : session.getPlayWhenReady(); }

    @Override public boolean isPlaying() { return session == null ? false : session.isPlaying(); }

    @Override public boolean isLoading() { return session == null ? false : session.isLoading(); }

    @Override public List<FormatItem> getVideoFormats() { return session == null ? null : session.getVideoFormats(); }

    @Override public List<FormatItem> getAudioFormats() { return session == null ? null : session.getAudioFormats(); }

    @Override public List<FormatItem> getSubtitleFormats() { return session == null ? null : session.getSubtitleFormats(); }

    @Override public void setFormat(FormatItem formatItem) { if (session != null) session.setFormat(formatItem); }

    @Override public FormatItem getVideoFormat() { return session == null ? null : session.getVideoFormat(); }

    @Override public FormatItem getAudioFormat() { return session == null ? null : session.getAudioFormat(); }

    @Override public FormatItem getSubtitleFormat() { return session == null ? null : session.getSubtitleFormat(); }

    @Override public boolean isEngineInitialized() { return session == null ? false : session.isEngineInitialized(); }

    @Override public void blockEngine(boolean block) { if (session != null) session.blockEngine(block); }

    @Override public boolean isEngineBlocked() { return session == null ? false : session.isEngineBlocked(); }

    @Override public boolean isInPIPMode() { return getActivity() != null && getActivity().isInPictureInPictureMode(); }

    @Override public boolean containsMedia() { return session == null ? false : session.containsMedia(); }

    @Override public float getSpeed() { return session == null ? 0 : session.getSpeed(); }

    @Override public void setSpeed(float speed) { if (session != null) session.setSpeed(speed); }

    @Override public float getPitch() { return session == null ? 0 : session.getPitch(); }

    @Override public void setPitch(float pitch) { if (session != null) session.setPitch(pitch); }

    @Override public float getVolume() { return session == null ? 0 : session.getVolume(); }

    @Override public void setVolume(float volume) { if (session != null) session.setVolume(volume); }

    @Override public int getResizeMode() { return session == null ? 0 : session.getResizeMode(); }

    @Override public void setResizeMode(int mode) { if (session != null) session.setResizeMode(mode); }

    @Override public void setZoomPercents(int percents) { if (session != null) session.setZoomPercents(percents); }

    @Override public void setAspectRatio(float ratio) { if (session != null) session.setAspectRatio(ratio); }

    @Override public void setRotationAngle(int angle) { if (session != null) session.setRotationAngle(angle); }

    @Override public void setVideoFlipEnabled(boolean enabled) { if (session != null) session.setVideoFlipEnabled(enabled); }

    @Override public void setVideoGravity(int gravity) { if (session != null) session.setVideoGravity(gravity); }

}
