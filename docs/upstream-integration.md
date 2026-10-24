# Upstream integration — October 24, 2026

Integrated `yuliskov/SmartTube`'s development branch, `master`, through
`168e48483` (53 commits since this fork's common ancestor). The app now carries
upstream version 32.68 / code 2458.

## Included changes

- SABR network failover with a dedicated four-second read timeout, avoidance of
  recently failed networks, and retries at the requested seek position.
- Updated SABR reload/backoff handling and buffering recovery; transient decoder
  failures no longer overwrite the saved video quality.
- Additional hardware decoder recognition for MStar, Qualcomm, MediaTek, and
  Rockchip devices.
- Lazy WorkManager initialization and guarded backup/channel scheduling to avoid
  direct-boot crashes.
- MediaServiceCore search retry, caption, null-safety, and remote-control fixes,
  together with the matching SharedModules update.
- Upstream translations, including Georgian, documentation, and compatibility
  source changes.

## Fork-specific decisions

The mobile activities, NikoTube branding, API 26 minimum, startup buffering,
decoder-capability fallback, and relative publication dates are retained.

Upstream MediaServiceCore commit `05feeb7e` is explicitly marked
`temporarily unlock live streams for testing [DO NOT RELEASE]`. Its enablement is
reverted in this integration. The preceding safety checks remain in force so
experimental SABR live streams fall back to another client. The related player
implementation remains available for a future upstream fix, but the mobile UI
has not been adapted or validated for experimental SABR live timelines.

MediaServiceCore contains a local merge preserving the relative-date changes and
an explicit revert of the experimental enablement. These dependency commits
must be published to an accessible dependency remote before sharing a parent
repository commit that references them.

The integration is on `codex/integrate-upstream-32.68` in a separate checkout at
`build/upstream-integration-checkout`. The main checkout was switched back to
`master` by another process during validation, so it is left on that branch.

## Validation

Validation passed in the isolated checkout using Java 11:

- Beta debug APK assembly, including the universal APK.
- All 58 mobile regression tests.
- All 23 SABR module tests, including network-failover coverage.
- Nine targeted MediaServiceCore search-retry and relative-publication-date tests.
- Merge-conflict and whitespace checks (respecting the repository's CRLF files).

The Gradle output is saved in the original checkout at
`build/phone-validation/upstream-isolated-validation.log`. The universal APK is
`build/upstream-integration-checkout/smarttubetv/build/outputs/apk/stbeta/debug/NikoTube_beta_32.68_universal.apk`
relative to the original checkout. Stable/F-Droid builds and physical-device
playback were not validated in this integration.
