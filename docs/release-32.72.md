# NikoTube 32.72

Stable Android package: org.smarttube.stable. Requires Android 8.0 or newer.

- Removed the top Home/Music/Subscriptions/Playlists strip; destinations remain available under You.
- Home keeps loading recommendations as you scroll without the loading spinner or its reserved space.
- Added an 8dp gap above portrait video, with no extra gap in fullscreen or PiP.
- Made all player overlay button backgrounds transparent, preserving icons and interaction feedback.
- Video descriptions now open below the video in portrait and beside it in landscape, using the comments-style panel with links, timestamps, Close, and Back.

Signed with the existing NikoTube stable key for in-place updates. The beta installation is unchanged.

Validation: Java 11 stable release build and signature verification. Seven focused UI tests passed against the completed UI build before this version-only rebuild. Broader tests have existing playback-fixture failures and an obsolete service-test API. Emulator/device checks were skipped at the user's request; runtime visual behavior remains unverified.
