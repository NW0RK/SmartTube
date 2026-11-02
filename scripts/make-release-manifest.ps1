param(
    [Parameter(Mandatory=$true)][string]$Apk,
    [Parameter(Mandatory=$true)][string]$Aapt,
    [Parameter(Mandatory=$true)][string]$Tag,
    [Parameter(Mandatory=$true)][string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
$package = & $Aapt dump badging $Apk
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect release APK' }
$identity = [regex]::Match(($package -join "`n"), "package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'")
if (!$identity.Success -or $identity.Groups[1].Value -ne 'org.smarttube.stable') { throw 'Release must contain the stable package' }
$version = $identity.Groups[3].Value
if ($Tag -ne "v$version") { throw 'Release tag and APK version differ' }
$asset = Split-Path -Leaf $Apk
if ($asset -notmatch '^NikoTube_stable_[0-9.]+_universal\.apk$') { throw 'Expected the universal stable APK' }
$minimum = [regex]::Match(($package -join "`n"), "sdkVersion:'([0-9]+)'")
if (!$minimum.Success) { throw 'Missing minimum SDK in APK' }
$hash = (Get-FileHash -LiteralPath $Apk -Algorithm SHA256).Hash.ToLowerInvariant()
$manifest = [ordered]@{
    schemaVersion = 1
    applicationId = 'org.smarttube.stable'
    versionCode = [long]$identity.Groups[2].Value
    versionName = $version
    minSdk = [int]$minimum.Groups[1].Value
    apkUrl = "https://github.com/NW0RK/SmartTube/releases/download/$Tag/$asset"
    apkSize = (Get-Item -LiteralPath $Apk).Length
    apkSha256 = $hash
    releaseUrl = "https://github.com/NW0RK/SmartTube/releases/tag/$Tag"
    changelog = @('YouTube-first dark design and a player-first watch page', 'Home, History and You navigation that hides while scrolling', 'Button feedback that follows the button shape', 'Vertical search suggestions and consistent menus')
}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$manifest | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 -LiteralPath (Join-Path $OutputDirectory 'nikotube-stable.json')
[IO.File]::WriteAllText((Join-Path $OutputDirectory 'SHA256SUMS.txt'), "$hash  $asset`n", [Text.Encoding]::ASCII)
