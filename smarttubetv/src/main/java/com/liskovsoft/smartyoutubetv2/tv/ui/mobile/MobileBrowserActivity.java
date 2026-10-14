package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.os.Bundle;
import android.webkit.*;
import android.widget.LinearLayout;

import com.liskovsoft.smartyoutubetv2.common.app.presenters.WebBrowserPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.WebBrowserView;

public class MobileBrowserActivity extends MobileActivity implements WebBrowserView {
    private WebView web;
    private WebBrowserPresenter presenter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = MobileUi.column(this);
        root.addView(MobileUi.toolbar(this, getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_brand_name), this::finish));
        web = new WebView(this);
        web.setWebViewClient(new WebViewClient());
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        presenter = WebBrowserPresenter.instance(this);
        presenter.setView(this);
        presenter.onViewInitialized();
    }

    @Override
    public void loadUrl(String url) {
        if (url != null) runOnUiThread(() -> web.loadUrl(url));
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        if (presenter.getView() == this) {
            presenter.onViewDestroyed();
            presenter.setView(null);
        }
        super.onDestroy();
    }
}
