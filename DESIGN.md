---
name: NikoTube
description: Familiar video browsing with NikoTube identity, designed for Android phones.
colors:
  background: "#0F0F0F"
  navigation: "#000000"
  surface: "#212121"
  surface-raised: "#272727"
  text-primary: "#F1F1F1"
  text-secondary: "#AAAAAA"
  text-inverse: "#0F0F0F"
  accent: "#FF0033"
typography:
  title: {fontFamily: sans-serif-medium, fontSize: 16sp, fontWeight: 500}
  watch-title: {fontFamily: sans-serif, fontSize: 18sp, fontWeight: 700}
  headline: {fontFamily: sans-serif, fontSize: 24sp, fontWeight: 700}
  body: {fontFamily: sans-serif, fontSize: 16sp, fontWeight: 400}
  metadata: {fontFamily: sans-serif, fontSize: 13sp, fontWeight: 400}
  label: {fontFamily: sans-serif, fontSize: 14sp, fontWeight: 400}
rounded: {thumbnail: 12dp, action: 28dp}
spacing: {small: 8dp, page: 16dp, section: 24dp}
---

# NikoTube design system

## Overview
Guidance follows [Impeccable’s native Android design workflow](https://github.com/pbakaus/impeccable).

YouTube-first, dark-only, phones first. Content leads; chrome stays quiet. Retain the NikoTube name, supplied orange/red logo, launcher artwork, and red accent. Use familiar YouTube layout and control patterns with native Android interactions.

This is a native Java/View frontend for watching, finding, and organizing videos. Keep its presenter/service contracts, existing destinations, and real metadata. The conceptual architecture article is context, not a requirement to migrate to Compose or add services.

## Colors
The executable palette lives in smarttubetv/src/main/res/values/mobile_browse.xml and mobile.xml. Frontmatter records its design contract; change the resources and this document together.

| Role | Android resource | Use |
|---|---|---|
| Background | mobile_background | Pages and flat content sections |
| Navigation | mobile_navigation_bottom | OLED black bottom navigation |
| Surface | mobile_surface | Chips, actions, placeholders, dialogs |
| Raised surface | mobile_surface_raised | Reserved tonal layer for raised controls |
| Primary text | mobile_text_primary | Titles and strong labels |
| Secondary text | mobile_text_secondary | Creator, dates, supporting information |
| Inverse text | mobile_text_inverse | Text on white Subscribe/selected chips |
| Accent | mobile_accent | Playback progress, active preferences, semantic emphasis |

White Subscribe becomes charcoal with white text when subscribed. Navigation selection uses filled icons and primary text. Keep accent coverage restrained. Video scrims and duration badges may use black translucency; menus and comments remain opaque.

## Typography
Use system sans, sp units, and Android font scaling. Feed titles use mobile_title_size (16sp), metadata mobile_meta_size (13sp), watch titles mobile_watch_title_size (18sp), and page headings mobile_heading_size (24sp). Body copy is 16sp; control labels 14sp. Bottom navigation uses centered icons without visible labels; retain descriptive accessibility labels.

Feed and recommendation titles truncate after two lines. Creator and statistics remain separate. Wrapping settings labels and descriptions must not truncate. Never lower system font scale to fit a screenshot.

## Layout
Use mobile_page_padding (16dp) for page gutters and mobile_space (8dp) for related content. Separate groups by 16–24dp. Preserve 16:9 thumbnails through MobileThumbnailView. Metadata and overflow live below the thumbnail. Avoid additional card wrappers.

Remove the persistent browse branding toolbar. Compact phones use Home, History, Search, and You icons; Search moves to bottom navigation so it remains accessible. MobileNavigationShell owns measured clearance, repeated insets, and keyboard visibility. Use an OLED black (#000000) bottom bar. Slide it down on downward user scrolling and restore it upward with interruptible 220ms ease-in/ease-out motion. Animate the reserved clearance with the bar; respect disabled system animations. Remove the top section chip strip. Music, Subscriptions, Playlists, and other enabled sections remain accessible under You. Retain the existing two-column feed at 600dp and above and full-span headings; tablet rail redesign is deferred. Preserve scroll and selected destinations through recreation.

Watch starts with the video, without a persistent branding header. In portrait, place the video and overlay controls together 8dp below the safe top edge; omit this gap in fullscreen and PiP. Swipe down at least 96dp in fullscreen to animate out to portrait in 100ms; do not trigger this gesture in PiP. Like and Dislike icons use secondary text color at rest and turn white when selected; the Like count uses the accent color when selected. Player overlay buttons are transparent at rest, with selected-state cues and pressed/focused feedback. Collapse, captions, and settings remain on its overlay. Search is available in More options. Keep existing fullscreen, PiP, comments side-panel, and playback continuity behavior.

## Elevation & Depth
Flat by default. Use tonal surfaces for grouping, native dialog/menu elevation only where needed. No decorative shadows or borders around each content section.

## Shapes
12dp corners for thumbnails and content-sized panels. Small action buttons use pill silhouettes. Player video uses square, unclipped corners. Icon hit regions stay at least 48dp with 24dp artwork. Comments retain rounded sheet corners and their existing window geometry.

## Components
MobileUi owns reusable buttons, icon buttons, navigation, menu rows, input styling, and feedback. Use drawable artwork rather than Unicode symbols. Pressed, focused, and hovered fills must match the resting corner radius across the entire button; never use an expanding circular ripple on text actions. Functional secondary-screen toolbars retain their back/actions; browsing has no branding toolbar.

- Chips: charcoal idle, white selected with inverse text; preserve service-backed section meaning.
- Cards: shared browse/recommendation geometry, two-line title, separate metadata, accessible drawable overflow. Clear missing recycled metadata.
- Search: rounded field with clear/voice controls; vertical, scrollable suggestion rows bounded to half the available viewport. Preserve long press and stale-response protection.
- You/channel: real account/channel imagery when available; accurate fallback identity. Keep account actions and existing library destinations.
- Settings: flat, full-width rows with native switches/radio/checkbox controls. MobileAlertDialogTheme scopes dark alerts to mobile.
- Loading: static initial placeholders; existing rows remain during pagination with inline progress, except Home loads further recommendations silently without a spinner or reserved status space. Errors keep loaded content and expose Retry where supported. Empty search explains how to recover.
- Description: use the comments-style player sheet below the actual video bottom in portrait and beside the video in landscape. Keep video controls usable, links and timestamps active, and Close/Back dismissal. Other long-text dialogs retain normal presentation.
- Comments: save the loaded list and scroll offset before opening replies; restore the same position when returning to the parent comments.
- Feedback: whole-shape pressed highlights, visible focus, selected/disabled state. Like and Subscribe selections use an interruptible 220ms squash-and-return pulse; exiting fullscreen swipes down and fades over 200ms. MobileMotion respects system animation settings for entry, controls, navigation, selection feedback, and fullscreen exit.

## Do's and Don'ts
- Do consult this file before changing mobile UI, then inspect the relevant resources and shared helpers.
- Do update these tokens and .impeccable/design.json with intentional system changes.
- Do verify 320dp phones, typical phones, 100/150/200% text, RTL, IME, fullscreen, and the retained tablet grid.
- Do keep TalkBack labels and at least 48dp interactive targets; use wrapping height when labels grow.
- Do preserve callbacks, nested Back behavior, comment/player interaction, pagination, and playback state.
- Don't fabricate avatars, subscriber counts, banners, badges, tabs, or unsupported YouTube features.
- Don't add light mode, new backend services, Google Play dependencies, or a UI-framework migration as visual polish.
- Don't run the Impeccable HTML/CSS detector against native Java/XML and claim native verification. Inspect emulator/device renders and native tests instead.
