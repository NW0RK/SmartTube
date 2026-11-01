# Upstream integration — November 2, 2026

Reviewed the 19 new commits on `yuliskov/SmartTube`'s main development branch,
`master`, from `168e48483` through `8759c6dda`. This is a selective source
integration; the version and distributed 32.68 APK are unchanged.

## Included changes

- Honor short SABR server backoffs rather than immediately retrying; retain
  format reloads for long backoffs and the empty-live-response delay.
- Restore seek positions while the player duration is still unknown, including
  live and previously live videos.
- Centralize live-duration fallback in the player controller so mobile, TV, and
  embedded players use the same duration. Preserve unknown durations as `-1`.
- Guard the new fallback against an absent or cleared video reference.
- Move DNS selection and automatic network recovery to General > Network, and
  restore the device-appropriate network engine when disabling the proxy.
- Include upstream translation updates, including Georgian and Swedish.

## Preserved fork behavior

NikoTube's mobile interface, branding, API 26 minimum, buffering and decoder
customizations, relative-date dependency pin, and existing dependency working
trees are preserved. SABR live playback remains excluded by
`VideoLoaderController`; the experimental developer toggle is not exposed.

TV DIAL receiver discovery, its dependency update, the promotion video, and
TV preference-layout changes are excluded from this mobile-focused update.
The upstream playback implementation changes are retained without enabling
experimental live playback.

## Validation and limitations

- All 49 changed XML resources parse and contain no duplicate resource names
  within the same resource type and file.
- Git whitespace checks pass with `core.whitespace=cr-at-eol` for the existing
  CRLF source files. No unresolved conflicts remain in the selected sources.
- Added five `MobileLiveDurationTest` cases covering missing metadata,
  scheduled streams, active streams, and ended streams with and without final
  duration metadata. These tests have not been executed.
- Attempted beta unit tests, SABR unit tests, and beta APK assembly. Gradle 7.5
  was downloaded successfully. The exact working-tree build fails during
  settings evaluation because `SharedModules/core_settings.gradle` is absent.
  The log is `build/exact-validation-unsandboxed.log`.
- An exact build also requires restoring the pinned custom MediaServiceCore
  commit `19132e0328e98d4933cda6e9d96199daaf4bc2e8`, which is absent from the
  local dependency repository. The original dependency folders contain only
  Git metadata with pre-existing staged file deletions; they were not changed.

No new APK has been produced and no physical-device verification is claimed.

---

# Previous integration — October 24, 2026

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
