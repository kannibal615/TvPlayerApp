param(
    [string]$AdbPath = "C:\Users\ONEDEV\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    [string]$Device = "192.168.1.33:5555",
    [string]$PackageName = "com.smartvision.svplayer",
    [int]$DurationSeconds = 180,
    [int]$SampleIntervalSeconds = 5,
    [string]$OutputRoot = "diagnostics"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "ADB introuvable: $AdbPath"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputDir = Join-Path $OutputRoot "firestick-sync-$timestamp"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$logcatPath = Join-Path $outputDir "logcat.txt"
$meminfoBeforePath = Join-Path $outputDir "meminfo-before.txt"
$meminfoSamplesPath = Join-Path $outputDir "meminfo-samples.txt"
$meminfoAfterPath = Join-Path $outputDir "meminfo-after.txt"
$devicesPath = Join-Path $outputDir "devices.txt"

Write-Host "Connexion ADB: $Device"
& $AdbPath connect $Device | Tee-Object -FilePath (Join-Path $outputDir "adb-connect.txt")
& $AdbPath devices -l | Tee-Object -FilePath $devicesPath

Write-Host "Nettoyage logcat et baseline memoire..."
& $AdbPath -s $Device logcat -c
& $AdbPath -s $Device shell dumpsys meminfo $PackageName | Out-File -Encoding utf8 $meminfoBeforePath

Write-Host "Capture logcat pendant $DurationSeconds secondes."
Write-Host "Lance maintenant la synchronisation Xtream depuis Info compte sur la Firestick."

$logcatProcess = Start-Process `
    -FilePath $AdbPath `
    -ArgumentList @("-s", $Device, "logcat", "-v", "time", "SVSyncMemory:I", "AndroidRuntime:E", "System.err:W", "*:S") `
    -RedirectStandardOutput $logcatPath `
    -NoNewWindow `
    -PassThru

try {
    $deadline = (Get-Date).AddSeconds($DurationSeconds)
    while ((Get-Date) -lt $deadline) {
        $now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        Add-Content -Path $meminfoSamplesPath -Value "===== $now ====="
        & $AdbPath -s $Device shell dumpsys meminfo $PackageName | Add-Content -Path $meminfoSamplesPath
        Start-Sleep -Seconds $SampleIntervalSeconds
    }
} finally {
    if (-not $logcatProcess.HasExited) {
        Stop-Process -Id $logcatProcess.Id -Force
    }
}

& $AdbPath -s $Device shell dumpsys meminfo $PackageName | Out-File -Encoding utf8 $meminfoAfterPath

Write-Host "Capture terminee: $outputDir"
