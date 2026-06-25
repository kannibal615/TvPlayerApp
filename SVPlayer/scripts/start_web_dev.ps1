param(
    [ValidateRange(1, 65535)]
    [int]$Port = 8080,
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

function Read-LocalProperties {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Fichier local.properties introuvable: $Path"
    }

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([^#!][^=]*?)\s*=\s*(.*)$') {
            $properties[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
    return $properties
}

function Require-Property {
    param(
        [hashtable]$Properties,
        [string]$Name
    )

    $value = [string]$Properties[$Name]
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "La propriete $Name est absente de local.properties."
    }
    return $value
}

function Resolve-HostName {
    param([string]$Value)

    try {
        $hostName = ([Uri]$Value).Host
        if (-not [string]::IsNullOrWhiteSpace($hostName)) {
            return $hostName
        }
    } catch {}

    return ($Value -replace '^https?://', '').Split(':')[0].Split('/')[0]
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$localPropertiesPath = Join-Path $projectRoot "local.properties"
$documentRoot = Join-Path $projectRoot "server/public_html"
$properties = Read-LocalProperties -Path $localPropertiesPath

$mysqlHost = Require-Property -Properties $properties -Name "MYSQL_HOST"
if ($mysqlHost -in @("localhost", "127.0.0.1", "::1")) {
    $mysqlHost = Resolve-HostName -Value (Require-Property -Properties $properties -Name "CPANEL_HOST")
}

$env:SMARTVISION_ENV = "development"
$env:SMARTVISION_PUBLIC_BASE_URL = "http://127.0.0.1:$Port"
$env:SMARTVISION_DOWNLOAD_BASE_URL = "https://smartvisions.net"
$env:SMARTVISION_MYSQL_HOST = $mysqlHost
$env:SMARTVISION_MYSQL_USERNAME = Require-Property -Properties $properties -Name "MYSQL_USERNAME"
$env:SMARTVISION_MYSQL_PASSWORD = Require-Property -Properties $properties -Name "MYSQL_PASSWORD"
$env:SMARTVISION_MYSQL_DATABASE = Require-Property -Properties $properties -Name "MYSQL_DATABASE"
$env:SMARTVISION_ADMIN_USERNAME = Require-Property -Properties $properties -Name "SMARTVISION_ADMIN_USERNAME"
$env:SMARTVISION_CREDENTIALS_ENCRYPTION_KEY = Require-Property -Properties $properties -Name "SMARTVISION_CREDENTIALS_ENCRYPTION_KEY"
$env:SMARTVISION_CPANEL_HOST = Require-Property -Properties $properties -Name "CPANEL_HOST"
$env:SMARTVISION_CPANEL_TOKEN = Require-Property -Properties $properties -Name "CPANEL_API_KEY"
$env:SMARTVISION_SMTP_PASSWORD = [string]$properties["SMTP_PASSWORD"]
$env:SMARTVISION_HILLTOPADS_API_KEY = [string]$properties["HILLTOPADS_API_KEY"]
$env:SMARTVISION_HILLTOPADS_PUBLISHER_ID = [string]$properties["HILLTOPADS_PUBLISHER_ID"]
$env:SMARTVISION_HILLTOPADS_VAST_TAG_URL = [string]$properties["HILLTOPADS_VAST_TAG_URL"]

$adminPassword = Require-Property -Properties $properties -Name "SMARTVISION_ADMIN_PASSWORD"
$env:SMARTVISION_ADMIN_PASSWORD_HASH = $adminPassword |
    & php -r "echo password_hash(trim(stream_get_contents(STDIN)), PASSWORD_DEFAULT);"
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($env:SMARTVISION_ADMIN_PASSWORD_HASH)) {
    throw "Impossible de preparer l'authentification admin locale."
}

& php -r "try { `$pdo = new PDO('mysql:host=' . getenv('SMARTVISION_MYSQL_HOST') . ';dbname=' . getenv('SMARTVISION_MYSQL_DATABASE') . ';charset=utf8mb4', getenv('SMARTVISION_MYSQL_USERNAME'), getenv('SMARTVISION_MYSQL_PASSWORD'), [PDO::ATTR_TIMEOUT => 8]); `$pdo->query('SELECT 1')->fetchColumn(); echo 'Connexion base de production: OK'; } catch (Throwable `$exception) { fwrite(STDERR, 'Connexion base de production impossible.' . PHP_EOL); exit(1); }"
if ($LASTEXITCODE -ne 0) {
    throw "Le serveur web n'a pas ete demarre."
}

$devCacheDirectory = Join-Path ([System.IO.Path]::GetTempPath()) "smartvision-web-dev"
$manifestPath = Join-Path $devCacheDirectory "smartvision-tv.version.json"
New-Item -ItemType Directory -Path $devCacheDirectory -Force | Out-Null
try {
    Invoke-WebRequest `
        -Uri "https://smartvisions.net/downloads/smartvision-tv.version.json" `
        -OutFile $manifestPath `
        -UseBasicParsing
    $env:SMARTVISION_APK_MANIFEST_PATH = $manifestPath
} catch {
    Write-Warning "Manifeste APK distant indisponible. Le reste du site peut fonctionner."
}

$url = $env:SMARTVISION_PUBLIC_BASE_URL + "/"
Write-Host ""
Write-Host "SmartVision local: $url"
Write-Host "Base de donnees: production distante"
Write-Host "Arret: Ctrl+C"
Write-Host ""

if (-not $NoBrowser) {
    Start-Process $url
}

& php -S "127.0.0.1:$Port" -t $documentRoot
