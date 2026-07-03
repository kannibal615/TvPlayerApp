param(
    [string]$AdbPath = "C:\Users\ONEDEV\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    [string]$Device = "192.168.1.33:5555",
    [string]$PackageName = "com.smartvision.svplayer",
    [string]$ApkPath = "app\build\outputs\apk\releaseDiagnostic\app-releaseDiagnostic.apk",
    [int]$DurationSeconds = 120,
    [int]$InteractiveSeconds = 75,
    [int]$SampleIntervalSeconds = 5,
    [string]$OutputRoot = "diagnostics"
)

# PERF_DIAG:
# Local Firestick capture script for the temporary Splash/Home diagnostic build.
# It is intentionally independent from deployment scripts and can be deleted with
# PerformanceDiagnosticRecorder + releaseDiagnostic once the investigation is over.

$ErrorActionPreference = "Stop"

function Invoke-Adb {
    param([string[]]$Arguments)
    & $AdbPath @Arguments
}

function Csv-Escape([string]$Value) {
    if ($null -eq $Value) {
        $Value = ""
    }
    return '"' + (($Value -replace '"', '""') -replace "`r", " " -replace "`n", " ") + '"'
}

function Add-DiagnosticRow {
    param(
        [string]$CsvPath,
        [string]$Sheet,
        [string]$Event,
        [hashtable]$Fields = @{}
    )
    if (-not (Test-Path -LiteralPath $CsvPath)) {
        "sheet,event,elapsed_ms,wall_time_ms,thread,message,details_json" | Out-File -Encoding utf8 $CsvPath
    }
    $json = [ordered]@{
        sheet = $Sheet
        event = $Event
        elapsed_ms = 0
        wall_time_ms = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
        thread = "capture-script"
    }
    foreach ($key in $Fields.Keys) {
        $json[$key] = [string]$Fields[$key]
    }
    $details = ($json | ConvertTo-Json -Compress -Depth 6)
    $line = @(
        Csv-Escape $Sheet
        Csv-Escape $Event
        Csv-Escape "0"
        Csv-Escape ([DateTimeOffset]::Now.ToUnixTimeMilliseconds().ToString())
        Csv-Escape "capture-script"
        Csv-Escape ($(if ($Fields.ContainsKey("message") -and $null -ne $Fields["message"]) { $Fields["message"].ToString() } else { "" }))
        Csv-Escape $details
    ) -join ","
    Add-Content -Path $CsvPath -Value $line -Encoding utf8
}

function ConvertTo-XlsxColumnName([int]$Index) {
    $name = ""
    $n = $Index
    while ($n -gt 0) {
        $rem = ($n - 1) % 26
        $name = [char](65 + $rem) + $name
        $n = [math]::Floor(($n - 1) / 26)
    }
    return $name
}

function Escape-Xml([string]$Value) {
    if ($null -eq $Value) {
        $Value = ""
    }
    return [System.Security.SecurityElement]::Escape($Value)
}

function New-XlsxFromCsvSet {
    param(
        [string]$CsvDir,
        [string]$XlsxPath
    )
    $sheets = @(
        "RunSummary",
        "StartupSteps",
        "LoadedData",
        "HomeState",
        "RowFocus",
        "MiniPlayer",
        "FrameStats",
        "Memory",
        "Errors"
    )
    $tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("sv-xlsx-" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force -Path $tmp | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $tmp "_rels") | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $tmp "xl") | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $tmp "xl\_rels") | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $tmp "xl\worksheets") | Out-Null

    $contentTypes = @(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
    )
    for ($i = 1; $i -le $sheets.Count; $i++) {
        $contentTypes += "<Override PartName=""/xl/worksheets/sheet$i.xml"" ContentType=""application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml""/>"
    }
    $contentTypes += '</Types>'
    $contentTypes -join "" | Out-File -Encoding utf8 -LiteralPath (Join-Path $tmp "[Content_Types].xml")

    @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>
'@ | Out-File -Encoding utf8 -LiteralPath (Join-Path $tmp "_rels\.rels")

    $workbookSheets = @()
    $workbookRels = @(
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
    )

    for ($i = 0; $i -lt $sheets.Count; $i++) {
        $sheetName = $sheets[$i]
        $sheetId = $i + 1
        $workbookSheets += "<sheet name=""$sheetName"" sheetId=""$sheetId"" r:id=""rId$sheetId""/>"
        $workbookRels += "<Relationship Id=""rId$sheetId"" Type=""http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"" Target=""worksheets/sheet$sheetId.xml""/>"

        $csvPath = Join-Path $CsvDir "$sheetName.csv"
        $rows = if (Test-Path -LiteralPath $csvPath) { @(Import-Csv -LiteralPath $csvPath) } else { @() }
        $headers = @("sheet", "event", "elapsed_ms", "wall_time_ms", "thread", "message", "details_json")
        $xmlRows = New-Object System.Collections.Generic.List[string]
        $rowNumber = 1
        $headerCells = for ($c = 0; $c -lt $headers.Count; $c++) {
            $cellRef = "$(ConvertTo-XlsxColumnName ($c + 1))$rowNumber"
            "<c r=""$cellRef"" t=""inlineStr""><is><t>$(Escape-Xml $headers[$c])</t></is></c>"
        }
        $xmlRows.Add("<row r=""$rowNumber"">$($headerCells -join '')</row>")
        foreach ($row in $rows) {
            $rowNumber++
            $cells = for ($c = 0; $c -lt $headers.Count; $c++) {
                $header = $headers[$c]
                $value = [string]$row.$header
                $cellRef = "$(ConvertTo-XlsxColumnName ($c + 1))$rowNumber"
                "<c r=""$cellRef"" t=""inlineStr""><is><t>$(Escape-Xml $value)</t></is></c>"
            }
            $xmlRows.Add("<row r=""$rowNumber"">$($cells -join '')</row>")
        }
        $worksheet = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
            '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">' +
            '<sheetData>' + ($xmlRows -join '') + '</sheetData></worksheet>'
        $worksheet | Out-File -Encoding utf8 -LiteralPath (Join-Path $tmp "xl\worksheets\sheet$sheetId.xml")
    }

    $workbookRels += '</Relationships>'
    ($workbookRels -join '') | Out-File -Encoding utf8 -LiteralPath (Join-Path $tmp "xl\_rels\workbook.xml.rels")
    ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">' +
        '<sheets>' + ($workbookSheets -join '') + '</sheets></workbook>') |
        Out-File -Encoding utf8 -LiteralPath (Join-Path $tmp "xl\workbook.xml")

    if (Test-Path -LiteralPath $XlsxPath) {
        Remove-Item -LiteralPath $XlsxPath -Force
    }
    $xlsxZipPath = "$XlsxPath.zip"
    if (Test-Path -LiteralPath $xlsxZipPath) {
        Remove-Item -LiteralPath $xlsxZipPath -Force
    }
    Compress-Archive -Path (Join-Path $tmp "*") -DestinationPath $xlsxZipPath -Force
    Move-Item -LiteralPath $xlsxZipPath -Destination $XlsxPath -Force
    Remove-Item -LiteralPath $tmp -Recurse -Force
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "ADB introuvable: $AdbPath"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outputDir = Join-Path $OutputRoot "firestick-splash-home-perf-$timestamp"
$rawDir = Join-Path $outputDir "raw"
$csvDir = Join-Path $outputDir "xlsx-source"
New-Item -ItemType Directory -Force -Path $rawDir, $csvDir | Out-Null

$logcatPath = Join-Path $rawDir "logcat.txt"
$logcatFullAfterPath = Join-Path $rawDir "logcat-full-after.txt"
$meminfoSamplesPath = Join-Path $rawDir "meminfo-samples.txt"
$meminfoBeforePath = Join-Path $rawDir "meminfo-before.txt"
$meminfoAfterPath = Join-Path $rawDir "meminfo-after.txt"
$gfxinfoPath = Join-Path $rawDir "gfxinfo.txt"
$gfxinfoFramestatsPath = Join-Path $rawDir "gfxinfo-framestats.txt"
$packageDumpPath = Join-Path $rawDir "package-dumpsys.txt"
$windowDumpPath = Join-Path $rawDir "window-dumpsys.txt"
$screenshotsDir = Join-Path $rawDir "screenshots"
New-Item -ItemType Directory -Force -Path $screenshotsDir | Out-Null

Write-Host "Connexion ADB: $Device"
Invoke-Adb @("connect", $Device) | Tee-Object -FilePath (Join-Path $rawDir "adb-connect.txt")
Invoke-Adb @("devices", "-l") | Tee-Object -FilePath (Join-Path $rawDir "adb-devices.txt")

if ($ApkPath -and (Test-Path -LiteralPath $ApkPath)) {
    Write-Host "Installation APK diagnostic: $ApkPath"
    Invoke-Adb @("-s", $Device, "install", "-r", $ApkPath) | Tee-Object -FilePath (Join-Path $rawDir "adb-install.txt")
} else {
    Write-Host "APK diagnostic non fourni/trouve, capture de l'installation actuelle."
}

Invoke-Adb @("-s", $Device, "shell", "dumpsys", "package", $PackageName) | Out-File -Encoding utf8 $packageDumpPath
Invoke-Adb @("-s", $Device, "shell", "dumpsys", "meminfo", $PackageName) | Out-File -Encoding utf8 $meminfoBeforePath
Invoke-Adb @("-s", $Device, "logcat", "-c") | Out-Null
Invoke-Adb @("-s", $Device, "shell", "dumpsys", "gfxinfo", $PackageName, "reset") | Out-Null

$traceBase = "splash-home-$timestamp.pftrace"
$traceDevice = "/sdcard/$traceBase"
$traceLocal = Join-Path $rawDir $traceBase
$perfettoPid = ""
try {
    $perfettoConfigLocal = Join-Path $rawDir "perfetto-config.pbtx"
    $perfettoConfigDevice = "/sdcard/sv_splash_home_perfetto.pbtx"
    @"
duration_ms: $($DurationSeconds * 1000)
buffers {
  size_kb: 32768
  fill_policy: RING_BUFFER
}
data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      ftrace_events: "sched/sched_switch"
      ftrace_events: "sched/sched_wakeup"
      ftrace_events: "power/cpu_frequency"
      atrace_categories: "am"
      atrace_categories: "wm"
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "binder_driver"
      atrace_categories: "dalvik"
      atrace_apps: "$PackageName"
    }
  }
}
"@ | Out-File -Encoding ascii -LiteralPath $perfettoConfigLocal
    Invoke-Adb @("-s", $Device, "push", $perfettoConfigLocal, $perfettoConfigDevice) | Out-File -Encoding utf8 (Join-Path $rawDir "perfetto-config-push.txt")
    $perfettoPid = (Invoke-Adb @(
        "-s", $Device, "shell", "perfetto",
        "--background",
        "--out", $traceDevice,
        "--config", $perfettoConfigDevice
    ) | Select-Object -Last 1)
    $perfettoPid | Out-File -Encoding utf8 (Join-Path $rawDir "perfetto-pid.txt")
} catch {
    $_ | Out-String | Out-File -Encoding utf8 (Join-Path $rawDir "perfetto-start-error.txt")
}

$logcatProcess = Start-Process `
    -FilePath $AdbPath `
    -ArgumentList @("-s", $Device, "logcat", "-v", "time", "SVPerf:I", "SVStartup:I", "SVHomeFocus:I", "SVHomePlayer:I", "AndroidRuntime:E", "System.err:W", "vision.svplaye:W", "*:S") `
    -RedirectStandardOutput $logcatPath `
    -WindowStyle Hidden `
    -PassThru

try {
    Write-Host "Cold start de l'application."
    Invoke-Adb @("-s", $Device, "shell", "am", "force-stop", $PackageName) | Out-Null
    Start-Sleep -Seconds 1
    Invoke-Adb @("-s", $Device, "shell", "monkey", "-p", $PackageName, "1") | Out-File -Encoding utf8 (Join-Path $rawDir "launch.txt")

    Write-Host "Pendant $InteractiveSeconds secondes, navigue sur la Firestick: Home, historique, tendances, dernier element, mini-player."
    $deadline = (Get-Date).AddSeconds($DurationSeconds)
    $automationDone = $false
    $screenshotIndex = 0
    while ((Get-Date) -lt $deadline) {
        $now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        Add-Content -Path $meminfoSamplesPath -Value "===== $now ====="
        Invoke-Adb @("-s", $Device, "shell", "dumpsys", "meminfo", $PackageName) | Add-Content -Path $meminfoSamplesPath

        if (-not $automationDone -and ((Get-Date) -gt $deadline.AddSeconds(-($DurationSeconds - $InteractiveSeconds)))) {
            Write-Host "Parcours D-pad automatise court."
            foreach ($key in @("20", "20", "22", "22", "22", "22", "22", "21", "21", "20", "22", "22", "22", "22", "22", "23")) {
                Invoke-Adb @("-s", $Device, "shell", "input", "keyevent", $key) | Out-Null
                Start-Sleep -Milliseconds 450
            }
            $automationDone = $true
        }

        if ($screenshotIndex -lt 4) {
            $shotDevice = "/sdcard/sv_perf_$screenshotIndex.png"
            $shotLocal = Join-Path $screenshotsDir "screen-$screenshotIndex.png"
            Invoke-Adb @("-s", $Device, "shell", "screencap", "-p", $shotDevice) | Out-Null
            Invoke-Adb @("-s", $Device, "pull", $shotDevice, $shotLocal) | Out-Null
            Invoke-Adb @("-s", $Device, "shell", "rm", "-f", $shotDevice) | Out-Null
            $screenshotIndex++
        }

        Start-Sleep -Seconds $SampleIntervalSeconds
    }
} finally {
    if ($logcatProcess -and -not $logcatProcess.HasExited) {
        Stop-Process -Id $logcatProcess.Id -Force
    }
}

Invoke-Adb @("-s", $Device, "shell", "dumpsys", "meminfo", $PackageName) | Out-File -Encoding utf8 $meminfoAfterPath
Invoke-Adb @("-s", $Device, "shell", "dumpsys", "gfxinfo", $PackageName) | Out-File -Encoding utf8 $gfxinfoPath
Invoke-Adb @("-s", $Device, "shell", "dumpsys", "gfxinfo", $PackageName, "framestats") | Out-File -Encoding utf8 $gfxinfoFramestatsPath
Invoke-Adb @("-s", $Device, "shell", "dumpsys", "window") | Out-File -Encoding utf8 $windowDumpPath
Invoke-Adb @("-s", $Device, "logcat", "-d", "-v", "time") | Out-File -Encoding utf8 $logcatFullAfterPath

try {
    Start-Sleep -Seconds 3
    Invoke-Adb @("-s", $Device, "pull", $traceDevice, $traceLocal) | Tee-Object -FilePath (Join-Path $rawDir "perfetto-pull.txt")
} catch {
    $_ | Out-String | Out-File -Encoding utf8 (Join-Path $rawDir "perfetto-pull-error.txt")
}

$deviceDiagnosticsRoot = "/sdcard/Android/data/$PackageName/files/diagnostics"
$pulledDiagnostics = Join-Path $rawDir "app-diagnostics"
New-Item -ItemType Directory -Force -Path $pulledDiagnostics | Out-Null
try {
    Invoke-Adb @("-s", $Device, "pull", $deviceDiagnosticsRoot, $pulledDiagnostics) | Tee-Object -FilePath (Join-Path $rawDir "app-diagnostics-pull.txt")
} catch {
    $_ | Out-String | Out-File -Encoding utf8 (Join-Path $rawDir "app-diagnostics-pull-error.txt")
}

$latestAppDiag = Get-ChildItem -Path $pulledDiagnostics -Recurse -Directory -Filter "splash-home-*" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($latestAppDiag) {
    Get-ChildItem -LiteralPath $latestAppDiag.FullName -Filter "*.csv" | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $csvDir $_.Name) -Force
    }
    if (Test-Path -LiteralPath (Join-Path $latestAppDiag.FullName "events.jsonl")) {
        Copy-Item -LiteralPath (Join-Path $latestAppDiag.FullName "events.jsonl") -Destination (Join-Path $rawDir "events.jsonl") -Force
    }
}

Add-DiagnosticRow -CsvPath (Join-Path $csvDir "RunSummary.csv") -Sheet "RunSummary" -Event "capture_completed" -Fields @{
    message = "Capture Firestick Splash/Home terminee"
    outputDir = $outputDir
    device = $Device
    package = $PackageName
    durationSeconds = $DurationSeconds
    interactiveSeconds = $InteractiveSeconds
    trace = $traceLocal
}

Get-Content -LiteralPath $gfxinfoPath -ErrorAction SilentlyContinue |
    Select-String -Pattern "Total frames rendered|Janky frames|90th percentile|95th percentile|99th percentile|Number Slow" |
    ForEach-Object {
        Add-DiagnosticRow -CsvPath (Join-Path $csvDir "FrameStats.csv") -Sheet "FrameStats" -Event "gfxinfo_summary" -Fields @{ message = $_.Line.Trim() }
    }

Get-Content -LiteralPath $meminfoAfterPath -ErrorAction SilentlyContinue |
    Select-String -Pattern "TOTAL|Java Heap|Native Heap|Graphics|Views:|Activities:" |
    ForEach-Object {
        Add-DiagnosticRow -CsvPath (Join-Path $csvDir "Memory.csv") -Sheet "Memory" -Event "meminfo_after" -Fields @{ message = $_.Line.Trim() }
    }

Get-Content -LiteralPath $logcatFullAfterPath -ErrorAction SilentlyContinue |
    Select-String -Pattern "AndroidRuntime|FATAL EXCEPTION|Exception|ANR|SVPerf|SVStartup|SVHomeFocus" |
    Select-Object -First 300 |
    ForEach-Object {
        $sheet = if ($_.Line -match "AndroidRuntime|FATAL EXCEPTION|Exception|ANR") { "Errors" } else { "RunSummary" }
        Add-DiagnosticRow -CsvPath (Join-Path $csvDir "$sheet.csv") -Sheet $sheet -Event "logcat_excerpt" -Fields @{ message = $_.Line.Trim() }
    }

$xlsxPath = Join-Path $outputDir "perf-diagnostics.xlsx"
New-XlsxFromCsvSet -CsvDir $csvDir -XlsxPath $xlsxPath

$zipPath = "$outputDir.zip"
if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}
Compress-Archive -Path (Join-Path $outputDir "*") -DestinationPath $zipPath -Force

Write-Host "Capture terminee."
Write-Host "Dossier: $outputDir"
Write-Host "XLSX: $xlsxPath"
Write-Host "ZIP: $zipPath"
