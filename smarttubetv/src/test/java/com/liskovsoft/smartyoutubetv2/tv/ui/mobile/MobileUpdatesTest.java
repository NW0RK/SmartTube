package com.liskovsoft.smartyoutubetv2.tv.ui.mobile;

import android.content.pm.Signature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = android.app.Application.class)
public class MobileUpdatesTest {
    private org.json.JSONObject manifest() throws Exception {
        return new org.json.JSONObject().put("schemaVersion", 1)
                .put("applicationId", org.robolectric.RuntimeEnvironment.getApplication().getPackageName())
                .put("minSdk", 26).put("versionCode", 2459).put("versionName", "32.69")
                .put("apkUrl", "https://github.com/NW0RK/SmartTube/releases/download/v32.69/NikoTube_stable_32.69_universal.apk")
                .put("apkSize", 1000000).put("apkSha256", new String(new char[64]).replace('\0', 'a'));
    }
    @Test public void matchingManifestIsAccepted() throws Exception {
        MobileUpdates.validateManifest(manifest(), org.robolectric.RuntimeEnvironment.getApplication());
    }
    @Test(expected = java.io.IOException.class) public void otherPackageIsRejected() throws Exception {
        MobileUpdates.validateManifest(manifest().put("applicationId", "other.package"), org.robolectric.RuntimeEnvironment.getApplication());
    }
    @Test(expected = java.io.IOException.class) public void unsupportedAndroidVersionIsRejected() throws Exception {
        MobileUpdates.validateManifest(manifest().put("minSdk", 99), org.robolectric.RuntimeEnvironment.getApplication());
    }
    @Test(expected = java.io.IOException.class) public void externalDownloadSourceIsRejected() throws Exception {
        MobileUpdates.validateManifest(manifest().put("apkUrl", "https://example.com/app.apk"), org.robolectric.RuntimeEnvironment.getApplication());
    }
    @Test(expected = java.io.IOException.class) public void missingChecksumIsRejected() throws Exception {
        MobileUpdates.validateManifest(manifest().put("apkSha256", ""), org.robolectric.RuntimeEnvironment.getApplication());
    }
    @Test public void mismatchedOrMissingSignaturesAreRejected() {
        assertFalse(MobileUpdates.sameSigners(null, null));
        assertFalse(MobileUpdates.sameSigners(new Signature[0], new Signature[0]));
        assertFalse(MobileUpdates.sameSigners(new Signature[]{new Signature("aabb")}, new Signature[]{new Signature("ccdd")}));
    }
    @Test public void signerOrderingDoesNotChangeIdentity() {
        Signature a = new Signature("aabb"), b = new Signature("ccdd");
        assertTrue(MobileUpdates.sameSigners(new Signature[]{a,b}, new Signature[]{b,a}));
    }
}
