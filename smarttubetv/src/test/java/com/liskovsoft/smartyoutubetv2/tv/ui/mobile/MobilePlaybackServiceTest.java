package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.PowerManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPowerManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class MobilePlaybackServiceTest {
    @Test
    public void rejectedLateServiceStartDoesNotCrashPlayback() {
        Context backgroundContext = new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public ComponentName startForegroundService(Intent intent) {
                throw new IllegalStateException("Foreground service start rejected");
            }
        };
        assertFalse(MobilePlaybackService.start(backgroundContext, null));
    }

    @Test
    public void playbackProtectionSurvivesScreenOffAndReleasesOnStop() {
        ServiceController<MobilePlaybackService> controller =
                Robolectric.buildService(MobilePlaybackService.class).create();
        MobilePlaybackService service = controller.get();
        try {
            assertEquals(Service.START_NOT_STICKY,
                    service.onStartCommand(new Intent(), 0, 1));
            PowerManager.WakeLock lock = ShadowPowerManager.getLatestWakeLock();
            assertTrue(lock.isHeld());
            assertNotNull(shadowOf(service).getLastForegroundNotification());
            PowerManager power = RuntimeEnvironment.getApplication()
                    .getSystemService(PowerManager.class);
            shadowOf(power).setIsInteractive(false);
            assertTrue(lock.isHeld());
            service.onStartCommand(new Intent(), 0, 2);
            controller.destroy();
            assertFalse(lock.isHeld());
        } finally {
            shadowOf(RuntimeEnvironment.getApplication().getSystemService(PowerManager.class))
                    .setIsInteractive(true);
        }
    }
}
