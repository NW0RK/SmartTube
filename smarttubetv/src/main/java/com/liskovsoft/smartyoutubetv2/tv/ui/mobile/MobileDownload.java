package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.liskovsoft.smartyoutubetv2.tv.R;

final class MobileDownload {
    static Intent intent(String videoId) {
        return new Intent(Intent.ACTION_SEND)
                .setPackage("com.deniscerri.ytdl")
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=" + Uri.encode(videoId));
    }

    static void open(Context context, String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            Toast.makeText(context, R.string.mobile_download_no_video, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            context.startActivity(intent(videoId));
        } catch (ActivityNotFoundException e) {
            new android.app.AlertDialog.Builder(context)
                    .setTitle(R.string.mobile_download_install_title)
                    .setMessage(R.string.mobile_download_install_message)
                    .setPositiveButton(R.string.mobile_download_install, (dialog, which) -> {
                        try {
                            context.startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/deniscerri/ytdlnis/releases/latest")));
                        } catch (ActivityNotFoundException noBrowser) {
                            Toast.makeText(context, R.string.mobile_download_install_message,
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null).show();
        }
    }
}
