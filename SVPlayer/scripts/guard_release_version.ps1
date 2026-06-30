param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [string]$PackageName = 'com.smartvision.svplayer',
    [switch]$RequireBuildMetadata,
    [switch]$SkipRemote,
    [switch]$SkipAdb
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$issues = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

function Add-Issue([string]$Message) {
    $issues.Add($Message) | Out-Null
}

function Add-Warning([string]$Message) {
    $warnings.Add($Message) | Out-Null
}

function Get-JsonVersionCode($Object) {
    foreach ($name in @('version_code', 'versionCode', 'latest_version_code', 'latestVersionCode')) {
        $prop = $Object.PSObject.Properties[$name]
        if ($null -ne $prop -and $null -ne $prop.Value -and "$($prop.Value)" -match '^\d+$') {
            return [int]$prop.Value
        }
    }
    return $null
}

function Get-JsonVersionName($Object) {
    foreach ($name in @('version_name', 'versionName', 'latest_version_name', 'latestVersionName')) {
        $prop = $Object.PSObject.Properties[$name]
        if ($null -ne $prop -and $null -ne $prop.Value) {
            return [string]$prop.Value
        }
    }
    return ''
}

function Read-RemoteJson([string]$Url) {
    $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 20
    $raw = [string]$response.Content
    return ($raw.TrimStart([char]0xFEFF) | ConvertFrom-Json)
}

$gradlePath = Join-Path $ProjectRoot 'app/build.gradle.kts'
if (-not (Test-Path -LiteralPath $gradlePath)) {
    throw "Gradle file not found: $gradlePath"
}

$gradleText = Get-Content -Raw -LiteralPath $gradlePath
if ($gradleText -notmatch 'versionCode\s*=\s*(\d+)') {
    throw 'versionCode not found in app/build.gradle.kts'
}
$localVersionCode = [int]$Matches[1]

if ($gradleText -notmatch 'versionName\s*=\s*"([^"]+)"') {
    throw 'versionName not found in app/build.gradle.kts'
}
$localVersionName = $Matches[1]

Write-Host "Local Gradle version: $localVersionName ($localVersionCode)"

$metadataPath = Join-Path $ProjectRoot 'app/build/outputs/apk/release/output-metadata.json'
if (Test-Path -LiteralPath $metadataPath) {
    $metadata = Get-Content -Raw -LiteralPath $metadataPath | ConvertFrom-Json
    $element = $metadata.elements | Select-Object -First 1
    if ($null -ne $element) {
        $buildVersionCode = [int]$element.versionCode
        $buildVersionName = [string]$element.versionName
        Write-Host "Built APK metadata: $buildVersionName ($buildVersionCode)"
        if ($buildVersionCode -ne $localVersionCode -or $buildVersionName -ne $localVersionName) {
            $message = 'Built output-metadata.json does not match Gradle. Rebuild release after bumping version.'
            if ($RequireBuildMetadata) {
                Add-Issue $message
            } else {
                Add-Warning $message
            }
        }
    }
} else {
    $message = 'No release output-metadata.json found yet. Run this guard again after assembleRelease.'
    if ($RequireBuildMetadata) {
        Add-Issue $message
    } else {
        Add-Warning $message
    }
}

$comparisons = New-Object System.Collections.Generic.List[object]

if (-not $SkipRemote) {
    foreach ($url in @(
            'https://smartvisions.net/downloads/smartvision-tv.version.json',
            'https://smartvisions.net/api/app_update.php'
        )) {
        try {
            $json = Read-RemoteJson $url
            $remoteCode = Get-JsonVersionCode $json
            $remoteName = Get-JsonVersionName $json
            if ($null -ne $remoteCode) {
                $comparisons.Add([pscustomobject]@{
                        Source      = $url
                        VersionCode = [int]$remoteCode
                        VersionName = $remoteName
                    }) | Out-Null
                Write-Host "Remote version: $remoteName ($remoteCode) from $url"
            } else {
                Add-Warning "Remote versionCode not found in $url"
            }
        } catch {
            Add-Warning "Could not read $url : $($_.Exception.Message)"
        }
    }
}

if (-not $SkipAdb) {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -eq $adb) {
        Add-Warning 'adb not found in PATH; installed TV version was not checked.'
    } else {
        try {
            $deviceLines = adb devices | Select-Object -Skip 1
            $devices = @($deviceLines | ForEach-Object {
                    $parts = ($_ -split '\s+') | Where-Object { $_ }
                    if ($parts.Count -ge 2 -and $parts[1] -eq 'device') { $parts[0] }
                })
            if ($devices.Count -eq 0) {
                Add-Warning 'No adb device connected; installed TV version was not checked.'
            }
            foreach ($device in $devices) {
                $dump = adb -s $device shell dumpsys package $PackageName 2>$null
                $dumpText = ($dump -join "`n")
                if ($dumpText -match 'versionCode=(\d+)') {
                    $installedCode = [int]$Matches[1]
                    $installedName = ''
                    if ($dumpText -match 'versionName=([^\s]+)') {
                        $installedName = $Matches[1]
                    }
                    $comparisons.Add([pscustomobject]@{
                            Source      = "adb:$device"
                            VersionCode = $installedCode
                            VersionName = $installedName
                        }) | Out-Null
                    Write-Host "Installed version: $installedName ($installedCode) on $device"
                } else {
                    Add-Warning "Package $PackageName not found on adb device $device"
                }
            }
        } catch {
            Add-Warning "adb check failed: $($_.Exception.Message)"
        }
    }
}

foreach ($comparison in $comparisons) {
    if ($localVersionCode -le [int]$comparison.VersionCode) {
        Add-Issue "Local versionCode $localVersionCode must be greater than $($comparison.VersionCode) from $($comparison.Source)."
    }
}

if ($warnings.Count -gt 0) {
    Write-Host ''
    Write-Host 'Warnings:'
    foreach ($warning in $warnings) {
        Write-Host " - $warning"
    }
}

if ($issues.Count -gt 0) {
    Write-Host ''
    Write-Host 'Release version guard failed:'
    foreach ($issue in $issues) {
        Write-Host " - $issue"
    }
    Write-Host ''
    Write-Host 'Bump app/build.gradle.kts versionCode/versionName, rebuild release, then rerun this guard.'
    exit 1
}

Write-Host ''
Write-Host 'Release version guard passed.'
