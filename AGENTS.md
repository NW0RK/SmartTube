# Repository guidance

Before changing mobile UI, read DESIGN.md and inspect MobileUi plus the mobile XML resources. Reuse shared components and semantic tokens. Update DESIGN.md and .impeccable/design.json together when changing the design system.

Preserve existing playback/service work and presenter contracts. Do not reset dirty dependency submodules. Build with the isolated dependency overrides described in patches/README.md and Java 11.

Verify native UI using Android tests and emulator/device captures. Keep system font scaling, accessibility targets, keyboard/inset behavior, pagination, and state restoration. Report unverified scenarios explicitly.

Use stable releases and the stable package for future builds/installations; do not update the beta installation.
