# Phone frontend

## Reference-led mobile redesign — October 13, 2026

The phone interface now shares a black background, charcoal surfaces, red accents, 16dp page gutters and thumbnail corners, 18sp feed titles, readable secondary text, and rounded touch controls. Home has a compact SmartTube brand header; Home, Subscriptions and You use outlined navigation icons, visible labels, account imagery when available, and a floating translucent capsule. Channels and uploads share this navigation. Content clearance follows the measured bar height and system insets; the bar hides during keyboard entry and returns afterward. Search, You, watch details, comments, settings, sign-in, device linking and browser toolbars use the shared styling.

Channel pages have a scrolling profile header. Identity comes from the channel item or existing video author metadata, including the parsed author when the service stores it in the subtitle. A video title or description is never used as a substitute for channel profile data. Available subscriber information and genuine channel-item imagery/description are shown; missing banners, feed author avatars, verification badges and unsupported channel tabs are omitted. Channel search, sorting, overflow actions, pagination, 16:9 imagery, the 600dp tablet breakpoint and existing presenter/service contracts remain intact. No dependencies or stored preferences were migrated. Existing playback and screen-off service work was preserved.

Validation: beta and stable debug builds passed, the separate F-Droid debug build passed, and all 30 mobile regression tests passed. The five redesign tests cover measured navigation clearance and repeated insets, keyboard clearance, 200% navigation text and touch targets, channel identity fallbacks, destination restoration, and the watch toolbar's back action. Beta lint completed with 0 errors and 289 warnings. XML parsing and Git whitespace checks passed. Missing cached F-Droid and lint artifacts were fetched through the existing repositories without changing dependency versions.

Installed the final universal beta APK over the existing Galaxy A52 app (Android 14) without clearing data. Visually inspected Home, channel headers, You, search and keyboard controls, video options, watch playback, and comments with the video visible above the sheet. Confirmed channel-to-You navigation, creator/subscriber identity, floating-bar hiding during channel search and restoration after closing the keyboard. Device checks caught and fixed a watch toolbar type mismatch and consumed keyboard visibility before the final build; no further crash entries appeared after those fixes. The screenshots and build/lint/device diagnostics are under `build/phone-validation/`; the new installable artifact is `SmartTube-phone-redesign-debug.apk`.

Remaining device acceptance: full tablet and landscape coverage, real Android 8 keyboard behavior, RTL and 200% text across every screen, signed-out/account-linking/browser flows, all subscription/filter/pagination actions, replies/live chat, fullscreen/PiP and extended screen-off playback. Existing automated playback/service coverage passed, but this visual pass does not establish those broader device scenarios.

## Screen-off playback — October 13, 2026

Locking the phone now keeps the current video playing regardless of the background-mode preference. Leaving the app with the screen on still follows that preference. A private media-playback foreground service shows a silent playback notification and holds a partial wake lock while playing or buffering. It reserves protection during initial source loading, releases it on pause, completion, error, or player teardown, and does not restart independently of the Activity. Late service starts rejected by Android are deferred until the player resumes instead of crashing.

The beta debug build and all 23 mobile regression tests passed, including wake-lock/notification lifecycle and rejected-service-start coverage. Installed the corrected universal APK on the connected Galaxy A52 (Android 14) without clearing data. Playback stayed active with the screen off and advanced from about 1:00 to 1:59. Media-session pause released the service and wake lock while locked; play restarted both successfully. Closing the watch page removed its media session, service, and wake lock. No new crashes occurred with the corrected build. This pass does not establish long-duration battery-saver or process-recreation behavior.

The updated APK and build/device diagnostics are under `build/phone-validation/`; the installable APK is `SmartTube-phone-debug.apk`.

## Browse and search polish — October 13, 2026

Mobile browsing now uses rounded section chips with selection and touch feedback, highlighted bottom-navigation icons and labels, an active You destination, and consistently spaced account/library rows. The section strip hides on You and scrolls the selected section into view. Browse thumbnails have rounded corners and consistent card spacing; the 16:9 aspect ratio, 600dp two-column breakpoint, overflow actions, and compact watch recommendation layout are retained.

Search has a rounded input, drawable back/microphone/clear actions with localized accessibility labels, a compact search-settings action, and styled suggestions. Clearing the query returns to idle and ignores late results while it remains blank. Existing keyboard submission, voice input, suggestion debounce, history actions, and presenter contracts are retained.

Feed states distinguish idle, initial loading, empty results, and errors. Loading and recovery actions appear below retained content during pagination. New requests hide stale errors; headers remain visible before recommendation or channel results arrive. Regression coverage was added for these transitions, retry callbacks, header visibility, selected/disabled controls, and narrow RTL search controls at 200% text size.

Validation in this session: the six new/changed XML resources parsed successfully and Git whitespace checks passed. Beta/stable builds and unit tests, the separate F-Droid build, and beta lint were attempted but could not start because Java is absent from PATH and JAVA_HOME is unset. No Android SDK or connected-device tooling was found in the standard locations. Compilation, test execution, and visual/device acceptance (320–360dp phones, 720dp tablet, portrait/landscape, 200% text, RTL, keyboard/insets, navigation restoration, and watch-page regression) remain pending. Earlier validation results below do not validate this polish pass.

Installation follow-up: an existing portable Java/Android toolchain was discovered outside the standard locations. The fresh beta debug build and all 21 mobile regression tests passed, including the five browse-polish tests. The new APK's signing certificate matched the installed app. Installed the updated universal APK over `org.smarttube.beta` on the connected Galaxy A52 without clearing app data; confirmed Home resumed, visually checked the rounded feed cards, selected section chip, and highlighted bottom navigation, and found no new crash-buffer entries during launch. The refreshed APK, build log, and Home screenshot are under `build/phone-validation/`. Stable/F-Droid builds, lint, and the broader visual/device acceptance matrix remain pending for this polish pass.

## Watch page reference update — October 13, 2026

Follow-up layout adjustment: icon-only controls and the comments close button now draw their icons at the center of their touch areas. Rewind, play/pause, and forward share a centered row over the video; fullscreen has one placement beside the timeline time with a 48dp target. Recommendation rows reserve the overflow target separately from the thumbnail and text, align to the 16dp page gutter, and cap metadata at two lines. Bottom navigation uses explicit centered alignment and consistent icon-to-label spacing. XML parsing and whitespace checks passed; Android tests could not start because Java is unavailable, and device verification remains pending.

The watch page follows the supplied dark mobile reference: gradient video controls, outlined icons, direct 10-second rewind/forward, a red timeline above elapsed/total time, a white subscription button, engagement pills, a description card, chapter thumbnails with an active chapter border, and compact recommendation rows. Comments and replies use a rounded sheet with author photos and leave the player visible above it. Comment likes and pagination continue through the existing service; unsupported posting and clip controls are not added.

Download shares the current YouTube video URL directly to the separately installed YTDLnis package (`com.deniscerri.ytdl`) through `ACTION_SEND` with `text/plain`, following its [documented integration](https://github.com/deniscerri/ytdlnis#-connect-with-third-party-apps-using-intents). If the app is missing, an installation prompt offers its official releases page. SmartTube does not bundle a downloader.

Java syntax parsing, XML parsing, and whitespace checks passed for this update. A Java/Android build toolchain and connected phone were unavailable in this session, so APK compilation, automated Android tests, and visual/device acceptance of this update remain pending. Existing verification results below apply to earlier builds.

The app now routes the existing browse, search, channel, uploads, account-linking, browser, dialog, and playback presenter contracts to native phone activities in `smarttubetv/.../ui/mobile`. Existing service APIs, application IDs, account storage, and playback dependencies are retained. Old TV classes remain as compatibility source but are not the registered frontend.

## Navigation and playback

### Video interaction with comments — October 13, 2026

Comments and reply threads now use a bottom-aligned window bounded to the sheet, with outside touches delivered directly to the watch window. Removed the transparent spacer that intercepted video taps and dismissed comments. Video controls and seek gestures remain usable while comments are open; Close and Back retain their existing behavior. Window bounds update on configuration changes, and ordinary option pages restore full-screen modal behavior.

Validation: beta debug build and all 25 mobile regression tests passed. Added coverage for sheet bounds, outside-touch dismissal, replies/Back, configuration updates, and restoring ordinary menus. Installed over the existing Galaxy A52 app without clearing data and verified revealing controls, play/pause, timeline dragging from 7:43 to 3:03, double-tap seeking in replies, returning to comments, and closing the sheet. No new crash-buffer entries during verification. APK and evidence are under `build/phone-validation/`.

### Comments and icon navigation follow-up — October 13, 2026

The mobile dialog theme is now declared in the manifest so Android knows the window is translucent at launch. Applying it only after Activity creation left Android treating the comments screen as opaque and hiding the underlying watch window. Comments and reply threads now retain the visible, playing video above the sheet. The sheet has softer rounded corners, a compact drag handle, a header divider, clearer author/date typography, spaced avatars, and separate like-count and reply actions. Empty service rows no longer display null text. Pagination uses a styled full-width action, including after returning from a thread.

Home, Subscriptions, and You now use centered icons with accessible destination labels and 20dp gaps between their touch targets. Selection feedback remains visible.

Validation: beta debug build and all 24 mobile regression tests passed, including launch-time translucency and icon navigation coverage. Installed the matching-signature APK over the existing Galaxy A52 app without clearing data. Reproduced the old blank video, then verified changing video frames above comments and replies with the new build; Android reports both the sheet and watch windows as visible. Visually checked the new comments layout and spaced navigation, checked destination selection, and found no new crash-buffer entries. The refreshed APK and screenshots are under `build/phone-validation/`. The broader tablet, rotation, large-text, and Android 8 acceptance matrix remains pending.

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
