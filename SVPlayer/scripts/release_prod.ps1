param(
    [switch]$SkipAdb,
    [switch]$RunDeployTests,
    [switch]$RunSqlInstall,
    [switch]$SkipPublicVerification
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$gradleWrapper = Join-Path $projectRoot 'gradlew.bat'
$guardScript = Join-Path $projectRoot 'scripts/guard_release_version.ps1'
$deployScript = Join-Path $projectRoot 'scripts/deploy_activation_phase1.ps1'
$gradlePath = Join-Path $projectRoot 'app/build.gradle.kts'
$apkPath = Join-Path $projectRoot 'app/build/outputs/apk/release/app-release.apk'
$metadataPath = Join-Path $projectRoot 'app/build/outputs/apk/release/output-metadata.json'
$publicBaseUrl = 'https://smartvisions.net'
$scriptStartedAt = Get-Date

function Write-ReleaseHeader {
    param([string]$Title)

    Write-Host ''
    Write-Host '============================================================' -ForegroundColor DarkCyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host '============================================================' -ForegroundColor DarkCyan
}

function Write-ReleaseProgress {
    param(
        [int]$Percent,
        [string]$Step,
        [string]$Status
    )

    $elapsed = [int]((Get-Date) - $scriptStartedAt).TotalSeconds
    $filled = [Math]::Floor($Percent / 5)
    $empty = 20 - $filled
    $bar = ('#' * $filled) + ('-' * $empty)
    Write-Host ("[{0}] {1,3}% | {2} - {3} ({4}s)" -f $bar, $Percent, $Step, $Status, $elapsed) -ForegroundColor Cyan
}

function Invoke-NativeStep {
    param(
        [int]$Percent,
        [string]$Label,
        [string]$FilePath,
        [string[]]$Arguments = @()
    )

    Write-ReleaseProgress -Percent $Percent -Step $Label -Status 'en cours'
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE."
    }
    Write-ReleaseProgress -Percent ($Percent + 5) -Step $Label -Status 'OK'
}

function Invoke-PowerShellStep {
    param(
        [int]$Percent,
        [string]$Label,
        [string]$ScriptPath,
        [string[]]$Arguments = @()
    )

    Write-ReleaseProgress -Percent $Percent -Step $Label -Status 'en cours'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $ScriptPath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE."
    }
    Write-ReleaseProgress -Percent ($Percent + 5) -Step $Label -Status 'OK'
}

function Get-GradleVersion {
    $gradleText = Get-Content -Raw -LiteralPath $gradlePath
    if ($gradleText -notmatch 'versionCode\s*=\s*(\d+)') {
        throw 'versionCode not found in app/build.gradle.kts.'
    }
    $versionCode = [int]$Matches[1]
    if ($gradleText -notmatch 'versionName\s*=\s*"([^"]+)"') {
        throw 'versionName not found in app/build.gradle.kts.'
    }
    [pscustomobject]@{
        VersionCode = $versionCode
        VersionName = $Matches[1]
    }
}

function Read-JsonUrl {
    param([string]$Url)

    $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 30 -Headers @{
        'Cache-Control' = 'no-cache'
        'User-Agent' = 'SmartVisionReleaseScript/1.0'
    }
    $raw = [string]$response.Content
    return ($raw.TrimStart([char]0xFEFF) | ConvertFrom-Json)
}

function Get-JsonInt {
    param(
        [object]$Object,
        [string[]]$Names
    )

    foreach ($name in $Names) {
        $property = $Object.PSObject.Properties[$name]
        if ($null -ne $property -and "$($property.Value)" -match '^\d+$') {
            return [int]$property.Value
        }
    }
    return $null
}

function Get-JsonString {
    param(
        [object]$Object,
        [string[]]$Names
    )

    foreach ($name in $Names) {
        $property = $Object.PSObject.Properties[$name]
        if ($null -ne $property -and $null -ne $property.Value -and "$($property.Value)" -ne '') {
            return [string]$property.Value
        }
    }
    return ''
}

function Get-RemoteVersionCode {
    $remoteCodes = New-Object System.Collections.Generic.List[int]
    foreach ($url in @(
            "$publicBaseUrl/downloads/smartvision-tv.version.json",
            "$publicBaseUrl/api/app_update.php"
        )) {
        try {
            $json = Read-JsonUrl -Url $url
            $code = Get-JsonInt -Object $json -Names @('version_code', 'versionCode', 'latest_version_code', 'latestVersionCode')
            if ($null -ne $code) {
                $remoteCodes.Add($code) | Out-Null
            }
        } catch {
            Write-Warning "Version prod non lisible depuis $url : $($_.Exception.Message)"
        }
    }

    if ($remoteCodes.Count -eq 0) {
        return $null
    }
    return ($remoteCodes | Measure-Object -Maximum).Maximum
}

function Update-VersionCode {
    param(
        [int]$CurrentVersionCode,
        [Nullable[int]]$RemoteVersionCode
    )

    $nextVersionCode = $CurrentVersionCode + 1
    if ($null -ne $RemoteVersionCode -and $RemoteVersionCode -ge $CurrentVersionCode) {
        $nextVersionCode = $RemoteVersionCode + 1
    }

    $gradleText = Get-Content -Raw -LiteralPath $gradlePath
    $updatedText = [regex]::Replace(
        $gradleText,
        'versionCode\s*=\s*\d+',
        "versionCode = $nextVersionCode",
        1
    )
    if ($updatedText -eq $gradleText) {
        throw 'versionCode replacement did not change app/build.gradle.kts.'
    }
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($gradlePath, $updatedText, $utf8NoBom)
    return $nextVersionCode
}

function Test-UrlHead {
    param([string]$Url)

    try {
        $response = Invoke-WebRequest -Uri $Url -Method Head -UseBasicParsing -TimeoutSec 30 -Headers @{
            'Cache-Control' = 'no-cache'
            'User-Agent' = 'SmartVisionReleaseScript/1.0'
        }
        Write-Host "OK HEAD $Url -> HTTP $([int]$response.StatusCode)" -ForegroundColor Green
    } catch {
        throw "Public URL check failed for $Url : $($_.Exception.Message)"
    }
}

function Test-PublicRelease {
    param(
        [int]$ExpectedVersionCode,
        [string]$ExpectedVersionName,
        [string]$ExpectedApkHash,
        [long]$ExpectedApkSize
    )

    Write-ReleaseProgress -Percent 90 -Step 'Verification publique' -Status 'manifest + update + APK'
    $cacheBust = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $manifestUrl = "$publicBaseUrl/downloads/smartvision-tv.version.json?cb=$cacheBust"
    $manifest = Read-JsonUrl -Url $manifestUrl

    $manifestCode = Get-JsonInt -Object $manifest -Names @('version_code', 'versionCode', 'latest_version_code', 'latestVersionCode')
    $manifestName = Get-JsonString -Object $manifest -Names @('version_name', 'versionName', 'latest_version_name', 'latestVersionName')
    $manifestApk = Get-JsonString -Object $manifest -Names @('apk_file', 'apkFile')
    $manifestHash = (Get-JsonString -Object $manifest -Names @('apk_sha256', 'apkSha256')).ToLowerInvariant()
    $manifestSize = Get-JsonInt -Object $manifest -Names @('apk_size', 'apkSize')

    if ($manifestCode -ne $ExpectedVersionCode -or $manifestName -ne $ExpectedVersionName) {
        throw "Public manifest version mismatch. Expected $ExpectedVersionName ($ExpectedVersionCode), got $manifestName ($manifestCode)."
    }
    if ($manifestHash -ne $ExpectedApkHash.ToLowerInvariant()) {
        throw "Public manifest hash mismatch. Expected $ExpectedApkHash, got $manifestHash."
    }
    if ($manifestSize -ne $ExpectedApkSize) {
        throw "Public manifest size mismatch. Expected $ExpectedApkSize, got $manifestSize."
    }
    if ([string]::IsNullOrWhiteSpace($manifestApk)) {
        throw 'Public manifest does not contain apk_file.'
    }
    Write-Host "OK manifest: $manifestName ($manifestCode), $manifestApk" -ForegroundColor Green

    $updateUrl = "$publicBaseUrl/api/app_update.php?version_code=0&version_name=qa&cb=$cacheBust"
    $update = Read-JsonUrl -Url $updateUrl
    $updateCode = Get-JsonInt -Object $update -Names @('version_code', 'versionCode', 'latest_version_code', 'latestVersionCode')
    $updateName = Get-JsonString -Object $update -Names @('version_name', 'versionName', 'latest_version_name', 'latestVersionName')
    if ($updateCode -ne $ExpectedVersionCode -or $updateName -ne $ExpectedVersionName) {
        throw "app_update.php version mismatch. Expected $ExpectedVersionName ($ExpectedVersionCode), got $updateName ($updateCode)."
    }
    Write-Host "OK app_update.php: $updateName ($updateCode)" -ForegroundColor Green

    $versionedApkUrl = "$publicBaseUrl/downloads/$manifestApk"
    $stableApkUrl = "$publicBaseUrl/downloads/smartvision-tv.apk?cb=$cacheBust"
    Test-UrlHead -Url $versionedApkUrl
    Test-UrlHead -Url $stableApkUrl

    $tempApk = Join-Path $env:TEMP ("smartvision-release-verify-$ExpectedVersionCode.apk")
    try {
        Invoke-WebRequest -Uri $versionedApkUrl -OutFile $tempApk -UseBasicParsing -TimeoutSec 180 -Headers @{
            'Cache-Control' = 'no-cache'
            'User-Agent' = 'SmartVisionReleaseScript/1.0'
        }
        $downloadedHash = (Get-FileHash -LiteralPath $tempApk -Algorithm SHA256).Hash.ToLowerInvariant()
        $downloadedSize = (Get-Item -LiteralPath $tempApk).Length
        if ($downloadedHash -ne $ExpectedApkHash.ToLowerInvariant()) {
            throw "Downloaded versioned APK hash mismatch. Expected $ExpectedApkHash, got $downloadedHash."
        }
        if ($downloadedSize -ne $ExpectedApkSize) {
            throw "Downloaded versioned APK size mismatch. Expected $ExpectedApkSize, got $downloadedSize."
        }
        Write-Host "OK versioned APK hash: $downloadedHash" -ForegroundColor Green
    } finally {
        if (Test-Path -LiteralPath $tempApk) {
            Remove-Item -LiteralPath $tempApk -Force
        }
    }
    Write-ReleaseProgress -Percent 95 -Step 'Verification publique' -Status 'OK'
}

if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    throw "Gradle wrapper not found: $gradleWrapper"
}
if (-not (Test-Path -LiteralPath $guardScript)) {
    throw "Release guard not found: $guardScript"
}
if (-not (Test-Path -LiteralPath $deployScript)) {
    throw "Deploy script not found: $deployScript"
}

$previousLocation = (Get-Location).Path
try {
    Set-Location -LiteralPath $projectRoot
    Write-ReleaseHeader -Title 'SmartVision build release + deploiement production'
    Write-Host 'Defaut: build release, deploy prod, sans install SQL, sans tests deploy.' -ForegroundColor DarkGray

    Write-ReleaseProgress -Percent 0 -Step 'Initialisation' -Status 'lecture version locale/prod'
    $initialVersion = Get-GradleVersion
    $remoteVersionCode = Get-RemoteVersionCode
    $remoteLabel = if ($null -eq $remoteVersionCode) { 'inconnue' } else { [string]$remoteVersionCode }
    Write-Host "Version locale avant bump: $($initialVersion.VersionName) ($($initialVersion.VersionCode))" -ForegroundColor Gray
    Write-Host "VersionCode prod detecte: $remoteLabel" -ForegroundColor Gray

    Write-ReleaseProgress -Percent 10 -Step 'Increment versionCode' -Status 'mise a jour Gradle'
    $newVersionCode = Update-VersionCode -CurrentVersionCode $initialVersion.VersionCode -RemoteVersionCode $remoteVersionCode
    $version = Get-GradleVersion
    Write-Host "Version release cible: $($version.VersionName) ($newVersionCode)" -ForegroundColor Green
    Write-ReleaseProgress -Percent 15 -Step 'Increment versionCode' -Status 'OK'

    $guardArgs = @()
    if ($SkipAdb) {
        $guardArgs += '-SkipAdb'
    }
    Invoke-PowerShellStep -Percent 20 -Label 'Garde-fou version avant build' -ScriptPath $guardScript -Arguments $guardArgs

    Invoke-NativeStep `
        -Percent 35 `
        -Label 'Build Android release' `
        -FilePath $gradleWrapper `
        -Arguments @(':app:assembleRelease', '--no-daemon', '--max-workers=1', '--console=plain')

    if (-not (Test-Path -LiteralPath $apkPath)) {
        throw "Release APK not found after build: $apkPath"
    }
    if (-not (Test-Path -LiteralPath $metadataPath)) {
        throw "Release metadata not found after build: $metadataPath"
    }

    Invoke-PowerShellStep -Percent 60 -Label 'Garde-fou metadata apres build' -ScriptPath $guardScript -Arguments ($guardArgs + @('-RequireBuildMetadata'))

    $apkInfo = Get-Item -LiteralPath $apkPath
    $apkHash = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-Host "APK local: $apkPath" -ForegroundColor Gray
    Write-Host "APK local SHA256: $apkHash" -ForegroundColor Gray
    Write-Host "APK local size: $($apkInfo.Length)" -ForegroundColor Gray

    $deployArgs = @()
    if (-not $RunSqlInstall) {
        $deployArgs += '-SkipInstall'
    }
    if (-not $RunDeployTests) {
        $deployArgs += '-SkipTests'
    }
    Invoke-PowerShellStep -Percent 75 -Label 'Deploiement production' -ScriptPath $deployScript -Arguments $deployArgs

    if (-not $SkipPublicVerification) {
        Test-PublicRelease `
            -ExpectedVersionCode $version.VersionCode `
            -ExpectedVersionName $version.VersionName `
            -ExpectedApkHash $apkHash `
            -ExpectedApkSize $apkInfo.Length
    }

    Write-ReleaseProgress -Percent 100 -Step 'Termine' -Status "prod $($version.VersionName) ($($version.VersionCode))"
    Write-ReleaseHeader -Title 'Release production terminee'
    Write-Host "Version publiee: $($version.VersionName) ($($version.VersionCode))" -ForegroundColor Green
    Write-Host "APK SHA256: $apkHash" -ForegroundColor Green
} catch {
    Write-Host ''
    Write-Host "ECHEC RELEASE: $($_.Exception.Message)" -ForegroundColor Red
    throw
} finally {
    Set-Location -LiteralPath $previousLocation
}
