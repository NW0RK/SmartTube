# NikoTube

A mobile-focused fork of [SmartTube](https://github.com/yuliskov/SmartTube), with a native touch interface for Android phones and tablets.

[**Download NikoTube APK**](https://github.com/NW0RK/SmartTube/releases/latest) · [Report an issue](https://github.com/NW0RK/SmartTube/issues) · [Mobile development notes](docs/phone-frontend.md)

## Download and install

1. [Download the NikoTube 32.69 universal APK](https://github.com/NW0RK/SmartTube/releases/download/v32.69/NikoTube_stable_32.69_universal.apk).
2. Open the downloaded file on your Android phone or tablet.
3. If prompted, allow your browser or file manager to install unknown apps, then complete installation.

Requires **Android 8.0 / API 26 or newer**. The signed stable universal APK includes ARMv7, ARM64, and x86 builds. Its package is `org.smarttube.stable`.

Future stable updates are available from **Settings → App updates**. Checks do not download automatically; tap Update and then confirm installation with Android. You can also install an APK from the releases page.

Existing beta users install this stable package separately. Older stable installations signed with another key require a one-time reinstall. Subsequent stable releases use the same NikoTube signing identity and update in place. The stable package may conflict with an existing SmartTube stable installation.

## Features

- Touch-friendly Home, Subscriptions, and You navigation, with search, channels, and account switching.
- Portrait watch pages, fullscreen landscape playback, comments, replies, and recommendations.
- Timeline seeking with thumbnail previews, double-tap seeking, captions, quality selection, and playback speed controls.
- SponsorBlock integration, picture-in-picture, and service-owned background audio while browsing or leaving the app.
- Per-video 1080p defaults that prefer smaller advertised bitrates, plus manual quality selection.
- In-app stable updates with checksum and signing-certificate validation.
- Relative publication dates and NikoTube branding.
- Video-download handoff to a separately installed YTDLnis app.
- No Google Play Services requirement.

## Compatibility and testing

This fork's frontend is designed for Android phones and tablets. It does not support iOS, Samsung Tizen, LG webOS, or Amazon VegaOS.

The 32.69 stable release variant builds successfully. Earlier mobile UI, quality,
and updater checks passed 75 tests and were exercised on a Galaxy A52. The final
service ownership migration was compiled without another automated or device
test pass. Experimental SABR live-stream enablement remains disabled.

See [mobile development and device-test notes](docs/phone-frontend.md) and [upstream integration details](docs/upstream-integration.md) for the scope of verification and remaining checks.

## Build

Clone with dependencies:

```sh
git clone --recurse-submodules https://github.com/NW0RK/SmartTube.git
cd SmartTube
```

Use Java 11 and an Android SDK, then build the beta debug APK:

```sh
./gradlew :smarttubetv:assembleStbetaDebug
```

Build configuration checks the public upstream revisions in
`dependencies.properties` and automatically applies the versioned NikoTube
patches. Repeated builds preserve an already-patched dependency tree. See
[dependency setup](patches/README.md) for the pins and isolated-checkout options.

On Windows, use `gradlew.bat`. APKs are written to `smarttubetv/build/outputs/apk/stbeta/debug/`.

Run the mobile regression tests with:

```sh
./gradlew :smarttubetv:testStbetaDebugUnitTest
```

## Credits and license

NikoTube is maintained in [NW0RK/SmartTube](https://github.com/NW0RK/SmartTube) and builds on [SmartTube by yuliskov](https://github.com/yuliskov/SmartTube) and its contributors. Upstream contributors include [@pixkk](https://github.com/pixkk) for SABR streaming and Innertube internals, and [@Jupiops](https://github.com/Jupiops) for SABR CDN support.

For the original TV app and its documentation, visit the [upstream SmartTube project](https://github.com/yuliskov/SmartTube). NikoTube downloads are provided by this repository.

Released under the [MIT License](LICENSE). See [PRIVACY.md](PRIVACY.md) for privacy information.
