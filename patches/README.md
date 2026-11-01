# Dependency reconstruction checkpoint

`mediaservicecore-reconstruction.patch` records the recovered publication-date
formatter and adapter integration, together with the live SABR safety checks
that undo upstream's experimental enablement. The formatter was recovered from
the existing NikoTube APK; decompiler aliases were replaced with Java constants.

The patch applies to upstream MediaServiceCore commit
`40fec843eb24bc264e9c8b429fcee25068e230e2`. It is intended for an isolated
checkout, such as `build/MediaServiceCore`, with dependency-root overrides:

```powershell
git -C build/MediaServiceCore apply ../../patches/mediaservicecore-reconstruction.patch
./gradlew.bat -PsharedModulesRoot=build/SharedModules -PmediaServiceCoreRoot=build/MediaServiceCore :smarttubetv:assembleStstableRelease
```

Do not apply this over the original submodule working trees containing staged
deletions. Do not treat this as recovery of the exact custom commit
`19132e0328e98d4933cda6e9d96199daaf4bc2e8`. That commit remains unavailable, and
the parent gitlink is deliberately unchanged. The accepted publication gate
therefore remains closed; this patch makes the alternative source reviewable
without creating an additional public dependency repository.
