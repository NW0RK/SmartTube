package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.speech.RecognizerIntent;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.MediaServiceSearchTagProvider;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.*;

public class MobileSearchActivity extends MobileActivity implements SearchView {
    private SearchPresenter presenter;
    private MobileFeedView feed;
    private EditText query;
    private LinearLayout tags;
    private MediaServiceSearchTagProvider provider;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable suggest = this::suggest;
    private int revision;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        presenter = SearchPresenter.instance(this);
        presenter.setView(this);
        LinearLayout root = MobileUi.column(this);
        query = new EditText(this);
        query.setSingleLine(true);
        query.setTextColor(android.graphics.Color.WHITE);
        query.setHint(getString(R.string.action_search));
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        ImageButton clear = MobileUi.browseIcon(this, R.drawable.mobile_clear,
                getString(R.string.mobile_clear_search), () -> {
                    query.setText("");
                    feed.idle();
                    startSearch(null);
                });
        clear.setVisibility(View.GONE);
        LinearLayout bar = MobileUi.searchToolbar(this, query, clear, this::finish, this::startVoiceRecognition);
        root.addView(bar);
        LinearLayout filters = new LinearLayout(this);
        MobileUi.addBrowseButton(filters,
                MobileUi.browseButton(
                        this,
                        getString(R.string.settings_search),
                        presenter::onSearchSettingsClicked));
        root.addView(filters);
        HorizontalScrollView strip = new HorizontalScrollView(this);
        strip.setHorizontalScrollBarEnabled(false);
        tags = new LinearLayout(this);
        strip.addView(tags);
        root.addView(strip);
        feed =
                new MobileFeedView(
                        this,
                        presenter::onVideoItemClicked,
                        presenter::onVideoItemLongClicked,
                        presenter::onScrollEnd);
        root.addView(feed, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        feed.setRetry(this::submit);
        feed.setEmptyText(getString(R.string.mobile_search_empty));
        query.setOnEditorActionListener(
                (v, action, event) -> {
                    if (action == EditorInfo.IME_ACTION_SEARCH
                            || event != null
                                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                                    && event.getAction() == KeyEvent.ACTION_UP) {
                        submit();
                        return true;
                    }
                    return false;
                });
        query.addTextChangedListener(
                new TextWatcher() {
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        clear.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
                        if (s.toString().trim().isEmpty()) feed.idle();
                        revision++;
                        handler.removeCallbacks(suggest);
                        handler.postDelayed(suggest, 250);
                    }

                    public void afterTextChanged(Editable e) {}
                });
        presenter.onViewInitialized();
        if (state != null) startSearch(state.getString("query"));
    }

    private void submit() {
        String text = getSearchText().trim();
        if (text.isEmpty()) return;
        tags.removeAllViews();
        revision++;
        handler.removeCallbacks(suggest);
        query.clearFocus();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(query.getWindowToken(), 0);
        presenter.onSearch(text);
    }

    private void suggest() {
        if (provider == null) return;
        int current = revision;
        provider.search(
                getSearchText(),
                results ->
                        runOnUiThread(
                                () -> {
                                    if (isFinishing() || current != revision) return;
                                    tags.removeAllViews();
                                    if (results != null)
                                        for (Tag tag : results) {
                                            Button button =
                                                    MobileUi.browseButton(
                                                            this,
                                                            tag.tag,
                                                            () -> {
                                                                query.setText(tag.tag);
                                                                submit();
                                                            });
                                            button.setTag(tag);
                                            button.setOnLongClickListener(
                                                    v -> {
                                                        presenter.onTagLongClicked(tag);
                                                        return true;
                                                    });
                                            MobileUi.addBrowseButton(tags, button);
                                        }
                                }));
    }

    @Override
    public void startSearch(String text) {
        query.setText(text == null ? "" : text);
        if (text != null && !text.trim().isEmpty()) submit();
        else {
            query.requestFocus();
            query.post(
                    () ->
                            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                                    .showSoftInput(query, InputMethodManager.SHOW_IMPLICIT));
        }
    }

    @Override
    public String getSearchText() {
        return query.getText().toString();
    }

    @Override
    public void updateSearch(VideoGroup g) {
        if (getSearchText().trim().isEmpty()) return;
        feed.update(g);
    }

    @Override
    public void clearSearch() {
        feed.clear();
    }

    @Override
    public void clearSearchTags() {
        revision++;
        tags.removeAllViews();
    }

    @Override
    public void removeSearchTag(Tag tag) {
        for (int i = tags.getChildCount() - 1; i >= 0; i--)
            if (tag.equals(tags.getChildAt(i).getTag())) tags.removeViewAt(i);
    }

    @Override
    public void setTagsProvider(MediaServiceSearchTagProvider p) {
        provider = p;
        suggest();
    }

    @Override
    public void showProgressBar(boolean show) {
        runOnUiThread(() -> {
            if (!getSearchText().trim().isEmpty()) feed.loading(show);
        });
    }

    @Override
    public void startVoiceRecognition() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, 71);
        } catch (ActivityNotFoundException e) {
            query.requestFocus();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 71 && result == RESULT_OK && data != null) {
            ArrayList<String> words = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (words != null && !words.isEmpty()) startSearch(words.get(0));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        feed.resize();
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("query", getSearchText());
    }

    @Override
    public void finish() {
        presenter.onFinish();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (presenter != null && presenter.getView() == this) {
            presenter.onViewDestroyed();
            presenter.setView(null);
        }
        super.onDestroy();
    }
}
