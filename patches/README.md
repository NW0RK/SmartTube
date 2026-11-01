# Pinned dependencies and NikoTube patches

`mediaservicecore-reconstruction.patch` records the recovered publication-date
formatter and adapter integration, together with the live SABR safety checks
that undo upstream's experimental enablement. The formatter was recovered from
the existing NikoTube APK; decompiler aliases were replaced with Java constants.

The approved replacement for the unavailable custom dependency uses public
upstream commits, recorded in `dependencies.properties` and the parent gitlinks:

- MediaServiceCore: `40fec843eb24bc264e9c8b429fcee25068e230e2`.
- SharedModules: `799cfae8a2f5c609b99e87fc673fc62de1c0199a`.

Clone with `--recurse-submodules`. Before loading dependency build scripts,
`settings.gradle` checks these revisions and applies the MediaServiceCore patch.
Repeated invocations recognize an already-applied patch and leave it untouched.
The patch also makes MediaServiceCore honor the selected SharedModules root.

If an existing checkout contains staged dependency deletions, leave those trees
alone and use separate copies of the pinned commits with root overrides:

```powershell
./gradlew.bat -PsharedModulesRoot=build/SharedModules -PmediaServiceCoreRoot=build/MediaServiceCore :smarttubetv:assembleStstableRelease
```

If the revision or patch context differs, configuration fails without resetting
the checkout or discarding edits. Correct the dependency setup in a clean or
isolated checkout before building.

This explicitly replaces the requirement to recover custom commit
`19132e0328e98d4933cda6e9d96199daaf4bc2e8`; it does not claim recovery of that
commit. No additional dependency fork is needed. The release workflow uses the
same pinned dependencies and automatic preparation as local builds.
