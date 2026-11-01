package com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs;

import android.content.Context;

/** App-owned update routing without exposing the legacy updater dependency. */
public final class AppUpdateRoute {
    public interface Handler { void check(Context context, boolean manual); }
    private static Handler handler;
    private AppUpdateRoute() {}
    public static void setHandler(Handler value) { handler = value; }
    static boolean dispatch(Context context, boolean manual) {
        if (handler == null) return false;
        handler.check(context, manual);
        return true;
    }
}
