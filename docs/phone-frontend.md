# Phone frontend

The app now routes the existing browse, search, channel, uploads, account-linking, browser, dialog, and playback presenter contracts to native phone activities in `smarttubetv/.../ui/mobile`. Existing service APIs, application IDs, account storage, and playback dependencies are retained. Old TV classes remain as compatibility source but are not the registered frontend.

## Navigation and playback

- Home, Subscriptions, and You are the primary destinations. The section strip exposes enabled and pinned browse sections; You exposes the other enabled destinations and accounts.
- Feed cards use full-width 16:9 thumbnails, metadata, and touch overflow actions. RecyclerView retains pagination groups and uses one column below 600dp and two at wider widths.
- Search supports the keyboard, suggestions, history actions, voice input, and existing filters. Channels retain keyboard search and sorting; sign-in and device linking include retry actions. Existing options are rendered in scrolling touch dialogs, including nested settings, comments, replies, and live chat.
- The portrait watch page contains player controls and scrolling video details, engagement actions, and recommendations. The existing ExoPlayer controller and media session are coordinated by `MobilePlayerEngine`, independently of Leanback rendering.
- Configuration changes resize the same player. Back exits fullscreen before closing the watch page. Closing the watch page or navigating to browsing releases playback. There is no mini-player. Explicit PiP and configured background playback remain available.
- Device density and system font scale are used. Obsolete density, forced-landscape, overscan, and TV navigation preferences are retained in storage but ignored by the mobile frontend. All app flavors require API 26.

## Automated checks

Beta, stable, and F-Droid debug builds passed. All sixteen regression tests passed. Beta Android lint completed with no errors; warnings remain. Device acceptance checks below remain pending.

The regression tests cover cumulative pagination, per-group replacement and prepending, watched-state synchronization, author removal across groups, narrow-phone thumbnail sizing and overflow targets, tablet columns and group headings, Android 8 with large text, default playback zoom and explicit zoom/flip, and supported format restoration with decoder-capability fallback.

Use Java 11 for the existing Robolectric 4.6.1 tests. Java 17 is needed to load the WorkManager dependency's custom lint registry. Dependencies have not been upgraded.

```powershell
./gradlew.bat :smarttubetv:assembleStbetaDebug :smarttubetv:assembleStstableDebug :smarttubetv:testStbetaDebugUnitTest
./gradlew.bat :smarttubetv:assembleStfdroidDebug
./gradlew.bat :smarttubetv:lintStbetaDebug
```

Build F-Droid separately: the existing Google Services plugin is enabled for the entire invocation when beta or stable tasks are requested, and its configuration has no F-Droid client.

## Device acceptance checks still required

Initial implementation used automated checks. Follow-up testing on an attached Samsung Galaxy A52 with Android 14 confirmed the player checks below. The broader acceptance matrix, including Android 8 and upgrade coverage, still needs completion before release:

| Area | Device checks |
| --- | --- |
| Browsing | Pagination; enabled/pinned sections; startup destination; search and keyboard; channels; subscriptions; history; playlists; queue; account switching; settings; all overflow actions; signed-out and unavailable states. |
| Playback | Streaming; seeking and double taps; captions; quality/audio tracks; speed; queue; comments/replies/chat; SponsorBlock segments and menus; statistics; media controls; explicit PiP; background playback. |
| Lifecycle | Repeated rotation and fullscreen transitions without rebuffering or duplicate players; Back through dialogs/fullscreen/watch; stopped audio after browsing; process recreation; cold and warm external links. |
| Appearance | Narrow and large phones and tablets in both orientations; large text; keyboard insets; system bars and display cutouts. |
| Upgrade | Install over an existing installation using its signing key; verify accounts, history, playlists, playback preferences, themes, startup destinations, and stored obsolete TV preferences. |

## Player follow-up fixes

The phone engine treats the saved zoom sentinel (`-1`) as normal size, preserving explicit zoom and flip preferences. Phone format restoration checks ExoPlayer decoder capabilities before choosing a saved video or audio track, selects a supported alternative when needed, and enables decoder initialization fallback. This avoids the unsupported-format recovery cycle for tracks the device reports it cannot decode; genuine source and decoder failures still use existing error handling.

The watch-page gear opens video quality/codec variants directly. Other playback options remain under More. CC keeps the short label, shows its enabled state with the accent color, and toggles captions directly using the last available selection or the first available caption track. Long press retains caption track selection.

## Physical device verification — October 13, 2026

Tested the installed debug build on Samsung Galaxy A52 (SM-A525F), Android 14 / API 34, through Android debugging without clearing app data.

- Opened three public video links; playback rendered at normal size in portrait and landscape. Inspected app-specific logs: no player errors, decoder initialization errors, or crashes during these checks.
- Verified the gear opens video format choices only, and switched Big Buck Bunny from 720p/30 AVC to 1080p/60 AVC.
- Verified fullscreen changes to landscape and Back returns to the portrait watch page.
- Verified CC remains labeled CC, toggles its selected state on/off, and English captions render on the TED test video. Long press still opens caption track choices.
- Dragged the timeline from 2:19 to 8:21 on the 20:03 test video.
- Returned to Home with Back; device logs confirmed ExoPlayer, video codecs, and the audio track were released. SmartTube entries remaining in the system media dump were historical event records.
- Found and corrected incorrectly encoded playback icon characters during device testing, rebuilt, reinstalled, and visually verified the corrected controls. All ten regression tests passed again.

Screenshots, UI dumps, device logs, and the refreshed universal APK are available locally under `build/phone-validation/` (ignored build output). These checks do not cover every account, service, advanced playback, background/PiP, or recreation case in the acceptance matrix.

## Crash and navigation follow-up — October 13, 2026

The phone crash buffer identified repeated crashes in `MobilePlaybackFragment.setChannelIcon` when opening a feed video after a previous player had closed. The presenter retained its controller-facing player reference because teardown compared `getView()`, which returns null once the Activity is finishing. Teardown now checks player ownership directly, detaches the presenter when playback stops, preserves a replacement player's ownership, clears destroyed view references, and guards late channel image loads.

Three lifecycle regression tests cover detached-player cleanup, replacement-player ownership, and late channel icon updates. All thirteen tests passed. After installation, the Galaxy A52 completed three Home → watch → Home → watch cycles in the same process, with no new entries in the crash buffer.

Shorts is removed from the bottom navigation, section selector, and Shorts groups in browse feeds. Previously saved Shorts destinations redirect to Home; stored settings are retained. Bottom navigation now contains Home, Subscriptions, and You.

## Simplified player controls and buffering — October 13, 2026

Removed the previous and next buttons from the watch-page control row. Elapsed/total time now has more room beside fullscreen.

The buffering overlay uses playback waiting states instead of general loading/prefetch activity. It starts hidden, waits 750 ms before showing a sustained playback wait, and cancels when playback is ready or stops. Loading recommendations while video is ready does not display the central spinner. Three regression tests cover background loading and brief stalls, prolonged buffering and recovery, and initial loading/teardown cancellation; all sixteen tests passed.

Installed the updated debug build on the Galaxy A52 and verified the controls, normal playback advancing to 0:59, and no buffering overlay after the video was ready. Returned to browsing after testing. The refreshed APK is in `build/phone-validation/SmartTube-phone-debug.apk`.
