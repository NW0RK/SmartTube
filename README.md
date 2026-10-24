# NikoTube

A mobile-focused fork of [SmartTube](https://github.com/yuliskov/SmartTube), with a native touch interface for Android phones and tablets.

[**Download NikoTube APK**](https://github.com/NW0RK/SmartTube/raw/refs/heads/master/downloads/NikoTube.apk) · [Report an issue](https://github.com/NW0RK/SmartTube/issues) · [Mobile development notes](docs/phone-frontend.md)

## Download and install

1. [Download the NikoTube 32.68 universal APK](https://github.com/NW0RK/SmartTube/raw/refs/heads/master/downloads/NikoTube.apk) from this repository.
2. Open the downloaded file on your Android phone or tablet.
3. If prompted, allow your browser or file manager to install unknown apps, then complete installation.

Requires **Android 8.0 / API 26 or newer**. The universal APK includes ARMv7, ARM64, and x86 builds. This is a **beta debug build**.

For updates, download the APK from the same link and install it over your existing NikoTube installation. Android requires a matching signing key to update an installed app. This fork retains SmartTube's application IDs, so it may conflict with an existing SmartTube installation.

## Features

- Touch-friendly Home, Subscriptions, and You navigation, with search, channels, and account switching.
- Portrait watch pages, fullscreen landscape playback, comments, replies, and recommendations.
- Timeline seeking with thumbnail previews, double-tap seeking, captions, quality selection, and playback speed controls.
- SponsorBlock integration, picture-in-picture, and background playback.
- Relative publication dates and NikoTube branding.
- Video-download handoff to a separately installed YTDLnis app.
- No Google Play Services requirement.

## Compatibility and testing

This fork's frontend is designed for Android phones and tablets. It does not support iOS, Samsung Tizen, LG webOS, or Amazon VegaOS.

The 32.68 beta APK builds successfully, and all 90 integration tests pass: 58 mobile tests, 23 SABR streaming tests, and nine search/relative-date tests. Physical-device playback validation of this integration is still pending. Experimental SABR live-stream enablement remains disabled.

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

On Windows, use `gradlew.bat`. APKs are written to `smarttubetv/build/outputs/apk/stbeta/debug/`.

Run the mobile regression tests with:

```sh
./gradlew :smarttubetv:testStbetaDebugUnitTest
```

## Credits and license

NikoTube is maintained in [NW0RK/SmartTube](https://github.com/NW0RK/SmartTube) and builds on [SmartTube by yuliskov](https://github.com/yuliskov/SmartTube) and its contributors. Upstream contributors include [@pixkk](https://github.com/pixkk) for SABR streaming and Innertube internals, and [@Jupiops](https://github.com/Jupiops) for SABR CDN support.

For the original TV app and its documentation, visit the [upstream SmartTube project](https://github.com/yuliskov/SmartTube). NikoTube downloads are provided by this repository.

Released under the [MIT License](LICENSE). See [PRIVACY.md](PRIVACY.md) for privacy information.
