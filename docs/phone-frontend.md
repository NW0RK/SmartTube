# Phone frontend

## Watch page scroll position — October 14, 2026

The phone watch page starts at the top of its video details and no longer follows shared TV recommendation-focus callbacks when suggestions finish loading. Late recommendation updates preserve manual scrolling. Opening a different video resets the details header to an explicit zero offset, including when large text makes it taller than the viewport. Horizontal chapter focusing is retained.

Validation: beta debug build and all 56 mobile tests passed, including regressions for late recommendation focus, manual scroll preservation, and resetting an oversized details header. Installed over the connected Galaxy A52 without clearing data; the loaded Big Buck Bunny page retained its title at the top with metadata, like count, and suggestions present. Manual scrolling worked; opening Me at the zoo afterward returned to the top and remained there with metadata and chapters loaded. APK and evidence are under `build/phone-validation/watch-top-*` and `build/phone-validation/NikoTube-watch-top-debug.apk`.

## Video likes, share popup and comment interaction — October 14, 2026

The mobile watch Like button shows the service's formatted count, including zero, with a localized accessibility label. Late metadata and like-state changes preserve the count; replacing the video clears it. The latest available count is retained for the current video even when the shared TV counter preference clears the model. Share uses the existing anchored popup and original link, QR and embed callbacks, with height sized to its content and bounded to the visible screen.

Comments no longer dismiss on video touches or the temporary Activity pause during fullscreen transitions. The portrait panel starts below the measured video frame, and landscape reserves room beside the player so controls stay accessible. Layout changes preserve the comment view and scroll position. Back, close, watch navigation and video replacement still dismiss comments.

Validation: all 54 mobile tests and the beta debug build passed with the bundled Java 11 runtime. Installed over the existing Galaxy A52 app without clearing data. At its existing 150% text size, verified formatted like counts, all three share choices, popup Back/outside dismissal, comment scrolling, play/pause, portrait and landscape seeking, and entering/exiting fullscreen with the same scrolled comments visible. A complete landscape seek moved playback from about 2:07 to 15:41. Video buffering during this pass does not establish sustained streaming reliability. Restored the phone's charging-screen setting after testing. Build/device logs and screenshots are under `build/phone-validation/video-interactions-*`; the final APK is `NikoTube-phone-interactions-debug.apk`.

Build prerequisite: corrected an existing media-module compilation error by checking `tvMusicVideoRenderer` directly instead of calling the private `getMusicItem` helper; this preserves the existing music-date behavior and changes no public API.

## Fullscreen control alignment - October 14, 2026

Player controls now share 16dp horizontal gutters: the collapse button aligns with the timeline and timestamp, and settings aligns with fullscreen. Top-row buttons use consistent 48dp targets and an 8dp gap between captions and settings. Fullscreen adds 8dp edge clearance and symmetric side clearance for display cutouts in either landscape direction. Play/pause remains centered, and the fullscreen icon switches to inward corners when exiting fullscreen.

Validation: Git whitespace checks passed for the changed player files. Beta build and unit tests were attempted but could not start because Java is absent from PATH and JAVA_HOME is unset. Compilation and visual/device verification remain pending; previously saved APKs and screenshots do not include this change.

## You and Settings menus — October 14, 2026

You now shows a 64dp circular account avatar, account name, and account-switch/sign-in action. History and Playlists use compact 48dp shortcuts with inline icons, side by side on ordinary phones, including at 150% text size. Narrow screens and text scaling of 180% or greater stack the shortcuts. Other enabled destinations retain their order as flat icon rows, with Settings separated visually. Account and section refreshes retain the You page and update its destinations.

Settings has a page heading and full-width icon rows with separators. Ordinary submenus use muted category headings, wrapping labels and descriptions, trailing switches for single-switch choices, and native checkbox/radio controls. Comment sheets, chat, and anchored video popups retain their specialized styling. Existing callbacks, nested Back behavior, and stored preferences are preserved.

Validation: beta debug build and all 50 mobile tests passed, including six menu regressions covering routing, account fallback, section refresh, 320dp/200% text/RTL layout, compact shortcuts at 150% text, and switch/checkbox/radio callbacks and nested Back. Installed over the existing Galaxy A52 app without clearing data. Visually checked the profile, compact 48dp shortcuts, Settings rows, and account submenu at the phone's existing 150% text size. Saved the APK, screenshots, and test logs under `build/phone-validation/`. Physical tablet, landscape, RTL, and 200% text checks remain pending; the automated suite covers narrow and tablet layouts.

## Recommendations and video-action popup — October 14, 2026

Recommended videos now use large 16:9 thumbnails with only the 48dp overflow target to their right. Titles and creator/view/date metadata appear underneath, inside the existing rounded recommendation surface. The three-dot action opens a bounded floating menu anchored to the selected card, with a compact title, Close button, scrollable options, and outside-touch dismissal. Existing service-backed video actions and nested choices remain available; the watch page stays visible and the player is not resized. Long-press and ordinary browse menus keep their existing behavior.

The text column aligns exactly with the thumbnail edges and reserves the overflow column underneath as well. Titles wrap to three lines with end ellipsis; the channel has its own first metadata line, followed by views/upload date. Channel and statistics have separate spacing and typography, and missing metadata is cleared when recommendation cards are recycled.

Validation: final beta debug build and all 40 mobile regression tests passed, including recommendation text/thumbnail alignment, metadata separation and recycling, popup action callbacks, and outside dismissal. Installed over the existing Galaxy A52 app without clearing data. Device inspection confirmed large thumbnails, channel/statistics separation, the compact floating menu, and dismissal to the same watch page. No new crash entries appeared during these checks. APK and screenshots are under `build/phone-validation/`.

## Watch page surface styling — October 14, 2026

The non-video portion of the watch page now shares the navigation and settings design: subtle shaded pill surfaces, thin borders, consistent touch feedback, and red selected-state accents. This includes video title/metadata, the channel and subscription row, Like/Dislike, Share, Download, Save, More, description, and comments entry. Recommendations use separate rounded cards with inset thumbnails and touchable overflow controls; chapters use matching surfaces with the existing active-chapter accent. Video controls and playback behavior remain unchanged.

Validation: beta debug build and all 33 mobile regression tests passed. Installed over the Galaxy A52 app without clearing data, opened Big Buck Bunny, and visually checked the title/metadata, channel/subscription row, action pills, description/comments entry, and scrolling recommendation cards. No new crash-buffer entries appeared during the check. Final APK and screenshots are under `build/phone-validation/`.

## NikoTube navigation and menus — October 14, 2026

Visible mobile branding and app labels now use NikoTube. Home, Subscriptions, and You retain the translucent rounded capsule, red selection accent, and accessible destination names, with centered 24dp icons in 48dp targets and no visible subtext. The selected surface and touch ripple use the same pill outline. Scrollable content extends behind the capsule; internal bottom padding keeps the last items accessible without reserving an empty strip. Keyboard hiding and system insets remain supported.

Settings and browse actions share shaded rounded surfaces, subtle borders, consistent spacing, and clearer section headings and descriptions. Settings entries include their existing icons and a forward chevron. Toolbar action separators distinguish back, search, voice, account, and playback actions.

Comments have an opaque charcoal panel with rounded corners, author avatars, row dividers, outlined/filled drawn hearts, and pill-shaped reply actions. Portrait comments remain a bottom sheet; landscape/fullscreen comments use a bounded panel on the physical right. Rotation updates the same window. Outside taps dismiss comments, and leaving the player or opening browse/search/channel/uploads closes any remaining comments panel. Ordinary settings menus retain modal behavior.

Validation: final beta debug APK built successfully and all 33 mobile regression tests passed, including content scrolling behind navigation, fullscreen/right-panel rotation and reply navigation, outside dismissal, and comments cleanup. Installed the final build over the existing Galaxy A52 beta app without clearing data. Earlier device inspection verified content visible behind the capsule and identified the toolbar-index crash; the final title lookup uses a stable view ID and the corrected player opens successfully. Full visual acceptance of final settings, fullscreen comments, and every device/font-scale combination remains pending. The current installable artifact is `build/phone-validation/NikoTube-phone-debug.apk`.

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

## Rounded player and controls — October 14, 2026

The watch-page video frame clips video and overlays to 20dp rounded corners, including while details scroll beneath it. Fullscreen and PiP keep their full rectangular frame. Player buttons use compact pill surfaces, centered icons, red pressed feedback, and a red border for selected captions. The buffering wheel is explicitly white. At the user's request, rewind and forward buttons are removed; play/pause sits alone in the center. Timeline dragging and double-tap seeking remain available.

Validation of the initial styling: beta debug build and all 34 mobile regression tests passed. Installed over the existing Galaxy A52 app without clearing data; confirmed rounded corners while scrolling and fullscreen/Back. Screenshots, layout dumps, build logs, and the updated `NikoTube-phone-debug.apk` are under `build/phone-validation/`.

### Seeking polish

The timeline has a 48dp touch area with a rounded 4dp track, a separate buffered track, and small segment markers below it. During dragging the track and thumb expand, a thumbnail/time card follows the thumb and stays within the player edges, and play/pause hides to make room. Taps seek immediately; dragging commits once on release and preserves the previous playback state. Canceling restores the original position. Controls remain visible throughout a drag, outdated thumbnail callbacks are ignored, and keyboard/accessibility position adjustments commit without requiring a touch release.

Validation: beta debug build and all 40 mobile tests passed, including four new tests for immediate touch updates, commit callbacks, cancellation, bounds, disabled controls, and accessibility adjustment. On the Galaxy A52, verified an off-track tap within the enlarged target seeks to 2:38, dragging commits at 7:25, a thumbnail preview tracks the finger, a five-second hold keeps controls visible, and cancellation restores 7:25. The updated app is installed without clearing data; evidence and APK remain under `build/phone-validation/`.
