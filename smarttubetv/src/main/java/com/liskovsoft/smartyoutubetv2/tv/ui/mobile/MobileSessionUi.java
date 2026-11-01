package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import com.liskovsoft.smartyoutubetv2.common.app.views.PersistentPlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;

/** Presenter state belongs to the session; Views are attached only while visible. */
abstract class MobileSessionUi implements PersistentPlaybackView {
    private WeakReference<MobilePlaybackFragment> attached = new WeakReference<>(null);
    private final Map<String, Consumer<MobilePlaybackFragment>> state = new LinkedHashMap<>();
    private final Map<Integer, Integer> buttons = new HashMap<>();
    private final MobileFeedAdapter suggestions = new MobileFeedAdapter(v -> {}, v -> {}, v -> {});
    private boolean visible;

    final MobilePlaybackFragment ui() { return attached.get(); }
    final void attachUi(MobilePlaybackFragment view) { attached = new WeakReference<>(view); }
    final void detachUi(MobilePlaybackFragment view) {
        if (ui() == view) { attached.clear(); visible = false; }
    }
    final void setUiVisible(boolean value) { visible = value; }
    @Override public android.app.Activity getPlaybackActivity() { return ui() == null ? null : ui().getActivity(); }
    @Override public boolean isPlaybackUiVisible() { return visible && getPlaybackActivity() != null; }
    final void replayUi() {
        MobilePlaybackFragment view = ui();
        if (view == null) return;
        view.setVideo(getVideo());
        view.clearSuggestions();
        for (VideoGroup group : suggestions.snapshotGroups()) view.updateSuggestions(group);
        for (Consumer<MobilePlaybackFragment> update : new ArrayList<>(state.values())) update.accept(view);
        for (Map.Entry<Integer, Integer> button : buttons.entrySet()) view.setButtonState(button.getKey(), button.getValue());
    }
    final void remember(String key, Consumer<MobilePlaybackFragment> update) {
        state.put(key, update);
        if (ui() != null) update.accept(ui());
    }
    @Override public void updateSuggestions(VideoGroup group) { suggestions.update(group); if (ui() != null) ui().updateSuggestions(group); }
    @Override public void removeSuggestions(VideoGroup group) {
        if (group == null) return;
        VideoGroup removal = VideoGroup.from(group.getVideos());
        removal.setId(group.getId()); removal.setAction(VideoGroup.ACTION_REMOVE);
        suggestions.update(removal);
        if (ui() != null) ui().removeSuggestions(group);
    }
    @Override public int getSuggestionsIndex(VideoGroup group) { return suggestions.groupIndex(group); }
    @Override public VideoGroup getSuggestionsByIndex(int index) { return suggestions.groupAt(index); }
    @Override public boolean isSuggestionsEmpty() { return suggestions.isEmpty(); }
    @Override public void clearSuggestions() { suggestions.clear(); if (ui() != null) ui().clearSuggestions(); }
    @Override public void focusSuggestedItem(int index) { if (ui() != null) ui().focusSuggestedItem(index); }
    @Override public void focusSuggestedItem(Video video) { if (ui() != null) ui().focusSuggestedItem(video); }
    @Override public void resetSuggestedPosition() { if (ui() != null) ui().resetSuggestedPosition(); }
    @Override public void showOverlay(boolean show) { if (ui() != null) ui().showOverlay(show); }
    @Override public boolean isOverlayShown() { return isPlaybackUiVisible() && ui().isOverlayShown(); }
    @Override public void showSuggestions(boolean show) { if (isPlaybackUiVisible()) ui().showSuggestions(show); }
    @Override public boolean isSuggestionsShown() { return isPlaybackUiVisible() && ui().isSuggestionsShown(); }
    @Override public void showControls(boolean show) { if (ui() != null) ui().showControls(show); }
    @Override public boolean isControlsShown() { return isPlaybackUiVisible() && ui().isControlsShown(); }
    @Override public int getButtonState(int id) { return buttons.containsKey(id) ? buttons.get(id) : ui() == null ? BUTTON_OFF : ui().getButtonState(id); }
    @Override public void setButtonState(int id, int value) { buttons.put(id, value); if (ui() != null) ui().setButtonState(id, value); }
    @Override public void setChannelIcon(String value) { remember("channel", v -> v.setChannelIcon(value)); }
    @Override public void setSeekPreviewTitle(String value) { remember("seekTitle", v -> v.setSeekPreviewTitle(value)); }
    @Override public void setNextTitle(Video value) { remember("next", v -> v.setNextTitle(value)); }
    @Override public void showDebugInfo(boolean show) { if (isPlaybackUiVisible()) ui().showDebugInfo(show); }
    @Override public void loadStoryboard() { if (ui() != null) ui().loadStoryboard(); }
    @Override public void setTitle(String value) { remember("title", v -> v.setTitle(value)); }
    @Override public void showProgressBar(boolean show) { remember("progress", v -> v.showProgressBar(show)); }
    @Override public void setSeekBarSegments(List<SeekBarSegment> value) { remember("segments", v -> v.setSeekBarSegments(value)); }
    @Override public void updateEndingTime() { if (ui() != null) ui().updateEndingTime(); }
    @Override public void setChatReceiver(ChatReceiver value) { remember("chat", v -> v.setChatReceiver(value)); }
    @Override public void showBackground(String value) { remember("background", v -> v.showBackground(value)); }
    @Override public void showBackgroundColor(int value) { remember("backgroundColor", v -> v.showBackgroundColor(value)); }
    public void setQualityInfo(String value) { remember("quality", v -> v.setQualityInfo(value)); }
}
