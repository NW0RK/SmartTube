package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

import com.liskovsoft.smartyoutubetv2.common.app.presenters.SignInPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView;

public class MobileSignInActivity extends MobileActivity implements SignInView {
    private SignInPresenter presenter;
    private LinearLayout content;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        content = MobileUi.column(this);
        content.addView(
                MobileUi.toolbar(
                        this,
                        getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_account),
                        this::finish));
        content.addView(new ProgressBar(this));
        setContentView(content);
        presenter = SignInPresenter.instance(this);
        presenter.setView(this);
        presenter.onViewInitialized();
    }

    @Override
    public void showCode(String code, String url) {
        showCode(code, url, url);
    }

    @Override
    public void showCode(String code, String url, String fullUrl) {
        runOnUiThread(
                () -> {
                    while (content.getChildCount() > 1) content.removeViewAt(1);
                    ScrollView scroll = new ScrollView(this);
                    LinearLayout page = MobileUi.column(this);
        page.setPadding(MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 24));
                    page.setPadding(MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 24));
                    scroll.addView(page);
                    content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
                    TextView label = MobileUi.text(this, code, 28);
                    label.setTextIsSelectable(true);
                    page.addView(label);
                    page.addView(MobileUi.text(this, url, 16));
                    page.addView(
                            MobileUi.button(
                                    this,
                                    "Copy code",
                                    () ->
                                            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                                                    .setPrimaryClip(
                                                            ClipData.newPlainText(
                                                                    "Sign-in code", code))));
                    page.addView(
                            MobileUi.button(
                                    this,
                                    "Open sign-in page",
                                    () -> {
                                        try {
                                            startActivity(
                                                    new Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(
                                                                    fullUrl == null
                                                                            ? url
                                                                            : fullUrl)));
                                        } catch (ActivityNotFoundException e) {
                                            Toast.makeText(this, url, Toast.LENGTH_LONG).show();
                                        }
                                    }));
                    page.addView(
                            MobileUi.button(
                                    this,
                                    getString(
                                            com.liskovsoft.smartyoutubetv2.tv.R.string
                                                    .mobile_retry),
                                    presenter::onViewInitialized));
                    page.addView(
                            MobileUi.button(
                                    this,
                                    getString(
                                            com.liskovsoft.smartyoutubetv2.tv.R.string
                                                    .signin_view_action_text),
                                    presenter::onActionClicked));
                });
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    protected void onDestroy() {
        if (presenter != null) {
            presenter.onViewDestroyed();
            presenter.setView(null);
        }
        super.onDestroy();
    }
}
