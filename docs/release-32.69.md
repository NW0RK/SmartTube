# NikoTube 32.69 release checkpoint

Package: `org.smarttube.stable`; version code: `2459`.

The stable release variant assembled successfully. Its universal APK is signed
with the persistent NikoTube release key and has a generated update manifest and
SHA-256 checksum file. Local assets are under `build/release-assets/`.
No additional unit tests or device checks ran after the instruction to stop.
APK signing and package metadata inspection are part of packaging.

The source implementation is on `codex/nikotube-redesign-release`. Dedicated
GitHub signing secrets are configured on `NW0RK/SmartTube`. The original
submodule staged deletions are excluded from the implementation commits.

## Included in the APK

- Opaque dark surfaces, full-width labeled navigation, flatter watch details,
  corrected spacing, and interruptible eased control fades.
- Supported 1080p selection ranked by advertised video bitrate, with lower-tier
  fallback and per-video manual quality choices.
- Home/screen-lock audio continuation and persistent user pause intent when
  returning to the existing watch task.
- Stable-only update checks, user-initiated downloads, APK validation and Android
  installer handoff through Settings → App updates.

## Dependency replacement and remaining work

The user approved replacing the unavailable custom dependency on November 2,
2026. The parent gitlinks and `dependencies.properties` now pin public upstream
MediaServiceCore `40fec843` and SharedModules `799cfae8`. Gradle checks those
revisions and automatically applies the versioned NikoTube patch before loading
dependency build scripts. Local builds and release CI use the same preparation.
The signed candidate already used these upstream revisions and the recovered
customizations. The replacement removes the missing-commit publication gate;
it does not claim recovery of custom commit `19132e03`.

The suggested `codex/integrate-smarttube-2026-11-02` branch was inspected. Its
source matches the integration included in this branch; the remaining tree
difference is `build-validation.log`. That historical branch retains the old
dependency pins; this implementation branch carries the approved replacements.

Service ownership of player/controller/session, playback through watch
destruction/Back/browsing, UI reattachment, background rendering suspension and
the complete updater upgrade exercise are unfinished. There is no public
`v32.69` release or live update manifest. Do not describe this checkpoint as the
complete accepted implementation.

Beta users will install stable separately. Stable installations signed with a
different key require a one-time reinstall. Keep the new persistent signing key
for subsequent in-place updates.
