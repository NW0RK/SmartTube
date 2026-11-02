# NikoTube 32.76

Stable Android package: `org.smarttube.stable`. Requires Android 8.0 or newer.

- Fix Like, Dislike, and Subscribe actions so the playback presenter receives the current selection and applies the requested toggle.
- Shorten the fullscreen swipe exit fade and downward motion from 200ms to 100ms, starting the portrait orientation request alongside the animation.
- The fullscreen swipe gesture remains disabled in picture-in-picture mode.

The stable signing identity is retained. Beta installations are unchanged.

Validation: installed on a Galaxy A52. Like toggled selected/unselected and Subscribe changed to Subscribed and back; both account states were restored. The display reported portrait on the first sample 40ms after the swipe completed. Automated tests were not run.
