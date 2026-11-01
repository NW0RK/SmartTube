# NikoTube 32.69

Stable package: `org.smarttube.stable`, version code `2459`. Requires Android 8.0
or newer. Install `NikoTube_stable_32.69_universal.apk` from this release.

- Opaque YouTube-style dark interface, full-width labeled navigation, flatter
  watch details and recommendations, improved spacing and smoother controls.
- Service-owned background audio through Home, Back, browsing and screen lock,
  notification playback controls, and a Return to player toolbar action.
- Each new video prefers the smallest supported 1080p video track by advertised
  bitrate. Manual quality choices stay with the current video.
- Settings → App updates checks NikoTube's stable release feed. Downloads begin
  only when requested; APK checksums, package, version and signature are checked
  before Android asks for installation confirmation.

Beta users install this stable package separately once. Older stable installs
signed with another key need a one-time reinstall. Subsequent stable releases
retain the new NikoTube signing identity and update in place from App updates.
Keep your existing beta installation until any data you need has been migrated.

Public update metadata: [nikotube-stable.json](https://github.com/NW0RK/SmartTube/releases/latest/download/nikotube-stable.json).
Use `SHA256SUMS.txt` to check the APK download.

The release variant compiles and is signed with the persistent NikoTube release
key. The final service migration did not receive another automated or device
test pass. Earlier UI/quality/updater checks passed 75 mobile tests and were
exercised on a Galaxy A52; those checks predate the service migration.

The unavailable custom dependency has been replaced by pinned public upstream
MediaServiceCore `40fec843` and SharedModules `799cfae8`, with versioned NikoTube
patches applied automatically during build configuration. Original local
submodule deletions remain untouched. The release workflow is manually triggered
for a version tag, with regression tests enabled by default for future releases.