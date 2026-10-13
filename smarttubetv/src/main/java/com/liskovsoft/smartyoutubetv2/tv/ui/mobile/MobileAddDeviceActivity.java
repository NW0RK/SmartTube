package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.*;
import android.os.Bundle;
import android.widget.*;

import com.liskovsoft.smartyoutubetv2.common.app.presenters.AddDevicePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.AddDeviceView;

public class MobileAddDeviceActivity extends MobileActivity implements AddDeviceView {
    private AddDevicePresenter presenter;
    private TextView code;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = MobileUi.column(this);
        root.addView(MobileUi.toolbar(this, "Link device", this::finish));
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = MobileUi.column(this);
        page.setPadding(MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 24));
                    page.setPadding(MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 16), MobileUi.dp(this, 24));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        code = MobileUi.text(this, "…", 28);
        code.setTextIsSelectable(true);
        page.addView(code);
        page.addView(
                MobileUi.button(
                        this,
                        "Copy code",
                        () ->
                                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                                        .setPrimaryClip(
                                                ClipData.newPlainText(
                                                        "Device code", code.getText()))));
        page.addView(
                MobileUi.button(
                        this,
                        getString(com.liskovsoft.smartyoutubetv2.tv.R.string.mobile_retry),
                        () -> presenter.onViewInitialized()));
        page.addView(MobileUi.button(this, "Done", () -> presenter.onActionClicked()));
        setContentView(root);
        presenter = AddDevicePresenter.instance(this);
        presenter.setView(this);
        presenter.onViewInitialized();
    }

    @Override
    public void showCode(String text) {
        runOnUiThread(() -> code.setText(text));
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
