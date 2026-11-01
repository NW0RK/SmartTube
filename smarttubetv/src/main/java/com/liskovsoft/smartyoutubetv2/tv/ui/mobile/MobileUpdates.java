package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Stable-only, user-initiated APK downloads. Android owns installation consent. */
public final class MobileUpdates {
    private static final String FEED = "https://github.com/NW0RK/SmartTube/releases/latest/download/nikotube-stable.json";
    private static final String DOWNLOAD_PREFIX = "https://github.com/NW0RK/SmartTube/releases/download/";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static boolean checking;
    private MobileUpdates() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("nikotube_updates", Context.MODE_PRIVATE);
    }

    public static void check(Context context, boolean manual) {
        if (context == null) return;
        if (!"org.smarttube.stable".equals(context.getPackageName())) {
            if (manual) Toast.makeText(context, "Updates are distributed through the stable NikoTube app.", Toast.LENGTH_LONG).show();
            return;
        }
        WeakReference<Activity> owner = new WeakReference<>(context instanceof Activity ? (Activity) context : null);
        Context app = context.getApplicationContext();
        SharedPreferences preferences = prefs(app);
        if (checking || (!manual && System.currentTimeMillis() - preferences.getLong("checked", 0) < 86_400_000L)) return;
        checking = true;
        preferences.edit().putLong("checked", System.currentTimeMillis()).apply();
        if (manual) Toast.makeText(context, "Checking for updates…", Toast.LENGTH_SHORT).show();
        IO.execute(() -> {
            JSONObject manifest = null;
            String error = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(FEED).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (bytes.size() + count > 131072) throw new IOException("Update description is too large");
                        bytes.write(buffer, 0, count);
                    }
                }
                manifest = new JSONObject(bytes.toString("UTF-8"));
                validateManifest(manifest, app);
                preferences.edit().putString("available", manifest.toString()).apply();
            } catch (Exception e) { error = "Unable to check for updates. Try again later."; }
            finally { if (connection != null) connection.disconnect(); }
            JSONObject result = manifest; String message = error;
            MAIN.post(() -> {
                checking = false;
                Activity activity = owner.get();
                if (!alive(activity)) return;
                if (message != null) {
                    if (manual) Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                } else if (result != null) showResult(activity, result, manual);
            });
        });
    }

    static void validateManifest(JSONObject m, Context c) throws Exception {
        if (m.getInt("schemaVersion") != 1 || !c.getPackageName().equals(m.getString("applicationId"))
                || m.getInt("minSdk") > android.os.Build.VERSION.SDK_INT
                || !m.getString("apkUrl").startsWith(DOWNLOAD_PREFIX)
                || !m.getString("apkSha256").matches("[a-fA-F0-9]{64}")
                || m.getLong("apkSize") <= 0 || m.getLong("versionCode") <= 0
                || m.getString("versionName").trim().isEmpty()) throw new IOException("Incompatible update");
    }

    private static void showResult(Activity activity, JSONObject manifest, boolean manual) {
        try {
            if (manifest.getLong("versionCode") <= activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode) {
                if (manual) Toast.makeText(activity, "NikoTube is up to date.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!manual) {
                Toast.makeText(activity, "NikoTube update available — open Settings → App updates.", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(activity).setTitle("NikoTube " + manifest.getString("versionName"))
                    .setMessage(manifest.optJSONArray("changelog") == null ? "An update is available." : manifest.getJSONArray("changelog").join("\n").replace("\"", ""))
                    .setNegativeButton("Later", null)
                    .setPositiveButton("Update", (d, w) -> download(activity, manifest)).show();
        } catch (Exception e) { Toast.makeText(activity, "Unable to read update details.", Toast.LENGTH_LONG).show(); }
    }

    public static void show(Activity activity) {
        try {
            JSONObject downloaded = new JSONObject(prefs(activity).getString("downloadManifest", "{}"));
            if (downloaded.optLong("versionCode", Long.MAX_VALUE) <= activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode)
                prefs(activity).edit().remove("downloadId").apply();
        } catch (Exception ignored) { }
        if (prefs(activity).getLong("downloadId", -1) != -1) watch(activity);
        else {
            String version = "";
            try { version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName; }
            catch (PackageManager.NameNotFoundException ignored) { }
            new AlertDialog.Builder(activity).setTitle("App updates")
                    .setMessage("Installed version: " + version)
                    .setPositiveButton("Check for updates", (d,w) -> check(activity, true))
                    .setNegativeButton("Close", null).show();
        }
    }

    static boolean available(Context context) {
        try {
            JSONObject cached = new JSONObject(prefs(context).getString("available", "{}"));
            return context.getPackageName().equals(cached.optString("applicationId"))
                    && cached.getLong("versionCode") > context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (Exception ignored) { return false; }
    }

    private static void download(Activity activity, JSONObject manifest) {
        try {
            DownloadManager manager = activity.getSystemService(DownloadManager.class);
            String name = "NikoTube-" + manifest.getLong("versionCode") + ".apk";
            File target = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates/" + name);
            File directory = target.getParentFile();
            if (directory == null || (!directory.isDirectory() && !directory.mkdirs())) throw new IOException("Cannot create update directory");
            if (target.exists() && !target.delete()) throw new IOException("Cannot replace earlier download");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(manifest.getString("apkUrl")))
                    .setTitle("NikoTube update").setMimeType("application/vnd.android.package-archive")
                    .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, "updates/" + name)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            long id = manager.enqueue(request);
            prefs(activity).edit().putLong("downloadId", id).putString("downloadManifest", manifest.toString()).putString("downloadPath", target.getAbsolutePath()).apply();
            watch(activity);
        } catch (Exception e) { Toast.makeText(activity, "Download could not start. Check storage and retry.", Toast.LENGTH_LONG).show(); }
    }

    private static void watch(Activity activity) {
        WeakReference<Activity> owner = new WeakReference<>(activity);
        Context app = activity.getApplicationContext();
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("App updates").setMessage("Checking download…")
                .setNegativeButton("Close", null).setNeutralButton("Cancel download", (d,w) -> {
                    app.getSystemService(DownloadManager.class).remove(prefs(app).getLong("downloadId", -1));
                    prefs(app).edit().remove("downloadId").apply();
                }).show();
        MAIN.post(new Runnable() {
            @Override public void run() {
                Activity current = owner.get();
                if (!alive(current) || !dialog.isShowing()) return;
                long id = prefs(app).getLong("downloadId", -1);
                try (Cursor cursor = app.getSystemService(DownloadManager.class).query(new DownloadManager.Query().setFilterById(id))) {
                    if (cursor == null || !cursor.moveToFirst()) throw new IOException("Download not found");
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_FAILED) throw new IOException("Download failed");
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        dialog.dismiss(); verify(current); return;
                    }
                    long received = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    long total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    dialog.setMessage(total > 0 ? "Downloading… " + (100 * received / total) + "%" : "Waiting for download…");
                    MAIN.postDelayed(this, 1000);
                } catch (Exception e) {
                    dialog.dismiss(); prefs(app).edit().remove("downloadId").apply();
                    new AlertDialog.Builder(current).setMessage("Download failed. Check your connection and storage.")
                            .setPositiveButton("Retry", (d,w) -> check(current, true)).setNegativeButton("Close", null).show();
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    private static void verify(Activity activity) {
        WeakReference<Activity> owner = new WeakReference<>(activity);
        Context app = activity.getApplicationContext();
        IO.execute(() -> {
            File apk = new File(prefs(app).getString("downloadPath", ""));
            boolean valid = false;
            try {
                JSONObject manifest = new JSONObject(prefs(app).getString("downloadManifest", "{}"));
                validateManifest(manifest, app);
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream input = new FileInputStream(apk)) {
                    byte[] bytes = new byte[32768]; int count;
                    while ((count = input.read(bytes)) != -1) digest.update(bytes, 0, count);
                }
                StringBuilder hash = new StringBuilder();
                for (byte b : digest.digest()) hash.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
                PackageManager packages = app.getPackageManager();
                PackageInfo installed = packages.getPackageInfo(app.getPackageName(), PackageManager.GET_SIGNATURES);
                PackageInfo update = packages.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNATURES);
                valid = update != null && app.getPackageName().equals(update.packageName)
                        && update.versionCode == manifest.getLong("versionCode") && update.versionCode > installed.versionCode
                        && apk.length() == manifest.getLong("apkSize") && hash.toString().equalsIgnoreCase(manifest.getString("apkSha256"))
                        && sameSigners(installed.signatures, update.signatures);
            } catch (Exception ignored) { }
            boolean accepted = valid;
            MAIN.post(() -> {
                Activity current = owner.get(); if (!alive(current)) return;
                if (!accepted) {
                    prefs(app).edit().remove("downloadId").apply();
                    Toast.makeText(current, "The update is incompatible or incomplete. Download it again.", Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(current).setTitle("Update ready")
                        .setMessage("Android will ask you to confirm installation. NikoTube may restart.")
                        .setNegativeButton("Later", null).setPositiveButton("Install", (d,w) -> install(current, apk)).show();
            });
        });
    }

    static boolean sameSigners(Signature[] a, Signature[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) return false;
        return new java.util.HashSet<>(Arrays.asList(a)).equals(new java.util.HashSet<>(Arrays.asList(b)));
    }

    private static void install(Activity activity, File apk) {
        try {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName())));
                Toast.makeText(activity, "Allow updates from NikoTube, then tap Install again in App updates.", Toast.LENGTH_LONG).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".nikotube_updates", apk);
            activity.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) { Toast.makeText(activity, "Android could not open the installer. Retry from App updates.", Toast.LENGTH_LONG).show(); }
    }

    private static boolean alive(Activity a) { return a != null && !a.isFinishing() && !a.isDestroyed(); }
}
