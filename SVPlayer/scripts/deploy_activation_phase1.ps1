param(
    [switch]$SkipInstall,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$localPropertiesPath = Join-Path $projectRoot "local.properties"
$publicHtmlPath = Join-Path $projectRoot "server/public_html"
$tempRoot = Join-Path $env:TEMP ("smartvision-activation-" + [Guid]::NewGuid().ToString("N"))

function Read-LocalProperties {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "local.properties introuvable."
    }

    $properties = @{}
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) {
            return
        }

        $index = $line.IndexOf("=")
        if ($index -gt 0) {
            $key = $line.Substring(0, $index).Trim()
            $value = $line.Substring($index + 1).Trim()
            $properties[$key] = $value
        }
    }

    return $properties
}

function Require-Property {
    param(
        [hashtable]$Properties,
        [string]$Name
    )

    if (-not $Properties.ContainsKey($Name) -or [string]::IsNullOrWhiteSpace($Properties[$Name])) {
        throw "Propriete manquante dans local.properties: $Name"
    }

    return [string]$Properties[$Name]
}

function Normalize-CpanelBaseUrl {
    param([string]$HostValue)

    $hostValue = $HostValue.Trim().TrimEnd("/")
    if ($hostValue -notmatch "^https?://") {
        $hostValue = "https://$hostValue"
    }

    $uri = [Uri]$hostValue
    if ($uri.IsDefaultPort) {
        return "https://$($uri.Host):2083"
    }

    return "$($uri.Scheme)://$($uri.Host):$($uri.Port)"
}

function Resolve-CpanelUsername {
    param([hashtable]$Properties)

    if ($Properties.ContainsKey("CPANEL_USERNAME") -and -not [string]::IsNullOrWhiteSpace($Properties["CPANEL_USERNAME"])) {
        return [string]$Properties["CPANEL_USERNAME"]
    }

    $apiKey = [string]$Properties["CPANEL_API_KEY"]
    if ($apiKey.Contains(":")) {
        return $apiKey.Split([char[]]@(":"), 2)[0]
    }

    $database = [string]$Properties["MYSQL_DATABASE"]
    if ($database.Contains("_")) {
        return $database.Split([char[]]@("_"), 2)[0]
    }

    throw "Impossible de determiner le username cPanel. Ajoute CPANEL_USERNAME dans local.properties."
}

function Resolve-CpanelToken {
    param([hashtable]$Properties)

    $apiKey = [string]$Properties["CPANEL_API_KEY"]
    if ($apiKey.Contains(":")) {
        return $apiKey.Split([char[]]@(":"), 2)[1]
    }

    return $apiKey
}

function New-CpanelHeaders {
    param(
        [string]$Username,
        [string]$Token
    )

    return @{
        Authorization = "cpanel ${Username}:$Token"
    }
}

function Invoke-Uapi {
    param(
        [string]$BaseUrl,
        [hashtable]$Headers,
        [string]$Module,
        [string]$Function,
        [hashtable]$Parameters = @{},
        [string]$Method = "Get"
    )

    $builder = [System.UriBuilder]"$BaseUrl/execute/$Module/$Function"
    if ($Parameters.Count -gt 0 -and $Method -eq "Get") {
        $query = [System.Web.HttpUtility]::ParseQueryString("")
        foreach ($key in $Parameters.Keys) {
            $query[$key] = [string]$Parameters[$key]
        }
        $builder.Query = $query.ToString()
    }

    if ($Method -eq "Post") {
        $response = Invoke-RestMethod -Method Post -Uri $builder.Uri.AbsoluteUri -Headers $Headers -Body $Parameters
    } else {
        $response = Invoke-RestMethod -Method Get -Uri $builder.Uri.AbsoluteUri -Headers $Headers
    }

    if ($null -ne $response.status -and [int]$response.status -ne 1) {
        $message = if ($response.errors) { ($response.errors -join "; ") } else { "appel UAPI refuse" }
        throw "${Module}/${Function}: $message"
    }

    return $response
}

function Invoke-CpanelApi2 {
    param(
        [string]$BaseUrl,
        [hashtable]$Headers,
        [string]$Username,
        [string]$Module,
        [string]$Function,
        [hashtable]$Parameters = @{}
    )

    $builder = [System.UriBuilder]"$BaseUrl/json-api/cpanel"
    $query = [System.Web.HttpUtility]::ParseQueryString("")
    $query["cpanel_jsonapi_user"] = $Username
    $query["cpanel_jsonapi_apiversion"] = "2"
    $query["cpanel_jsonapi_module"] = $Module
    $query["cpanel_jsonapi_func"] = $Function

    foreach ($key in $Parameters.Keys) {
        $query[$key] = [string]$Parameters[$key]
    }

    $builder.Query = $query.ToString()
    return Invoke-RestMethod -Method Get -Uri $builder.Uri.AbsoluteUri -Headers $Headers
}

function Ensure-RemoteDirectory {
    param(
        [string]$BaseUrl,
        [hashtable]$Headers,
        [string]$Username,
        [string]$Parent,
        [string]$Name
    )

    try {
        Invoke-Uapi -BaseUrl $BaseUrl -Headers $Headers -Module "Fileman" -Function "mkdir" -Parameters @{
            path = $Parent
            name = $Name
            permissions = "0755"
        } | Out-Null
    } catch {
        try {
            Invoke-CpanelApi2 -BaseUrl $BaseUrl -Headers $Headers -Username $Username -Module "Fileman" -Function "mkdir" -Parameters @{
                path = $Parent
                name = $Name
                permissions = "0755"
            } | Out-Null
        } catch {
            # Directory may already exist. Upload verification will fail later if it does not.
        }
    }
}

function Upload-File {
    param(
        [string]$BaseUrl,
        [hashtable]$Headers,
        [string]$Directory,
        [string]$FilePath
    )

    $curl = Get-Command curl.exe -ErrorAction Stop
    $auth = $Headers.Authorization
    $arguments = @(
        "-sS",
        "-H", "Authorization: $auth",
        "-F", "dir=$Directory",
        "-F", "file-1=@$FilePath;filename=$(Split-Path -Leaf $FilePath)",
        "$BaseUrl/execute/Fileman/upload_files"
    )

    $result = & $curl.Source @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Echec upload cPanel pour $(Split-Path -Leaf $FilePath)."
    }

    $json = $result | ConvertFrom-Json
    if ($null -ne $json.status -and [int]$json.status -ne 1) {
        $message = if ($json.errors) { ($json.errors -join "; ") } else { "upload refuse" }
        throw "Upload $(Split-Path -Leaf $FilePath): $message"
    }
}

function Get-ObjectPropertyValue {
    param(
        [object]$Object,
        [string]$Name
    )

    if ($null -eq $Object) {
        return $null
    }

    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function Resolve-DomainDocumentRoot {
    param(
        [string]$BaseUrl,
        [hashtable]$Headers,
        [string]$Domain
    )

    try {
        $single = Invoke-Uapi -BaseUrl $BaseUrl -Headers $Headers -Module "DomainInfo" -Function "single_domain_data" -Parameters @{ domain = $Domain }
        $data = $single.data
        foreach ($property in @("documentroot", "document_root", "docroot", "dir")) {
            if ($data.PSObject.Properties.Name -contains $property -and -not [string]::IsNullOrWhiteSpace([string]$data.$property)) {
                return [string]$data.$property
            }
        }
    } catch {
    }

    $domains = Invoke-Uapi -BaseUrl $BaseUrl -Headers $Headers -Module "DomainInfo" -Function "domains_data"
    foreach ($entry in @($domains.data.main_domain) + @($domains.data.addon_domains) + @($domains.data.sub_domains)) {
        if ($null -eq $entry) {
            continue
        }

        $candidateDomain = [string](Get-ObjectPropertyValue -Object $entry -Name "domain")
        if ([string]::IsNullOrWhiteSpace($candidateDomain)) {
            $candidateDomain = [string](Get-ObjectPropertyValue -Object $entry -Name "servername")
        }

        if ($candidateDomain -eq $Domain) {
            foreach ($property in @("documentroot", "document_root", "docroot", "dir")) {
                if ($entry.PSObject.Properties.Name -contains $property -and -not [string]::IsNullOrWhiteSpace([string]$entry.$property)) {
                    return [string]$entry.$property
                }
            }
        }
    }

    return "public_html"
}

function Convert-ToRemoteRelativePath {
    param(
        [string]$Path,
        [string]$Username
    )

    $normalized = $Path.Replace("\", "/").TrimEnd("/")
    $homePrefix = "/home/$Username/"

    if ($normalized.StartsWith($homePrefix)) {
        return $normalized.Substring($homePrefix.Length)
    }

    if ($normalized.StartsWith("home:")) {
        return $normalized.Substring(5).TrimStart("/")
    }

    return $normalized.TrimStart("/")
}

function Escape-PhpString {
    param([string]$Value)
    return $Value.Replace("\", "\\").Replace("'", "\'")
}

function Write-Utf8NoBomFile {
    param(
        [string]$Path,
        [string]$Content
    )

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

function New-PrivateConfigFile {
    param(
        [hashtable]$Properties,
        [string]$OutputPath
    )

    $mysqlHost = [string]$Properties["MYSQL_HOST"]
    $mysqlUsername = [string]$Properties["MYSQL_USERNAME"]
    $mysqlPassword = [string]$Properties["MYSQL_PASSWORD"]
    $mysqlDatabase = [string]$Properties["MYSQL_DATABASE"]

    $content = @"
<?php
declare(strict_types=1);

return [
    'mysql_host' => '$(Escape-PhpString $mysqlHost)',
    'mysql_username' => '$(Escape-PhpString $mysqlUsername)',
    'mysql_password' => '$(Escape-PhpString $mysqlPassword)',
    'mysql_database' => '$(Escape-PhpString $mysqlDatabase)',
];
"@

    Write-Utf8NoBomFile -Path $OutputPath -Content $content
}

function New-InstallFile {
    param(
        [string]$OutputPath,
        [string]$Token
    )

    $escapedToken = Escape-PhpString $Token
    $content = @'
<?php
declare(strict_types=1);

require_once __DIR__ . '/api/config.php';

header('Content-Type: application/json; charset=utf-8');
$installToken = '__INSTALL_TOKEN__';

if (!hash_equals($installToken, (string) ($_GET['token'] ?? ''))) {
    http_response_code(404);
    echo json_encode([
        'success' => false,
        'error' => 'Not found.',
    ], JSON_UNESCAPED_SLASHES);
    exit;
}

register_shutdown_function(static function (): void {
    @unlink(__FILE__);
});

try {
    $pdo = db();
    $sqlPath = __DIR__ . '/sql/init_activation_tables.sql';
    if (!is_file($sqlPath)) {
        throw new RuntimeException('SQL file missing.');
    }

    $sql = file_get_contents($sqlPath);
    if ($sql === false) {
        throw new RuntimeException('Unable to read SQL file.');
    }

    $pdo->exec($sql);

    echo json_encode([
        'success' => true,
        'message' => 'Activation tables installed.',
    ], JSON_UNESCAPED_SLASHES);
} catch (Throwable $exception) {
    error_log('SmartVision install failed.');
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'error' => 'Installation failed.',
    ], JSON_UNESCAPED_SLASHES);
}
'@
    $content = $content.Replace("__INSTALL_TOKEN__", $escapedToken)

    Write-Utf8NoBomFile -Path $OutputPath -Content $content
}

function Assert-ApiResult {
    param(
        [object]$Result,
        [string]$Label
    )

    if ($null -eq $Result -or $Result.success -ne $true) {
        throw "Test echoue: $Label"
    }
}

Add-Type -AssemblyName System.Web

$properties = Read-LocalProperties -Path $localPropertiesPath
foreach ($name in @("CPANEL_API_KEY", "CPANEL_HOST", "DOMAINE_SERVER", "MYSQL_HOST", "MYSQL_USERNAME", "MYSQL_PASSWORD", "MYSQL_DATABASE")) {
    Require-Property -Properties $properties -Name $name | Out-Null
}

$cpanelBaseUrl = Normalize-CpanelBaseUrl -HostValue (Require-Property -Properties $properties -Name "CPANEL_HOST")
$cpanelUsername = Resolve-CpanelUsername -Properties $properties
$cpanelToken = Resolve-CpanelToken -Properties $properties
$headers = New-CpanelHeaders -Username $cpanelUsername -Token $cpanelToken
$domain = Require-Property -Properties $properties -Name "DOMAINE_SERVER"

New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

try {
    Write-Host "Connexion cPanel et detection du document root..."
    $docRoot = Resolve-DomainDocumentRoot -BaseUrl $cpanelBaseUrl -Headers $headers -Domain $domain
    $remoteRoot = Convert-ToRemoteRelativePath -Path $docRoot -Username $cpanelUsername
    $remotePrivate = "smartvision_private"

    Write-Host "Preparation des dossiers distants..."
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "api"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "activate"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "sql"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent "." -Name $remotePrivate

    $privateConfigPath = Join-Path $tempRoot "config.php"
    $installPath = Join-Path $tempRoot "install.php"
    $installToken = [Guid]::NewGuid().ToString("N")
    New-PrivateConfigFile -Properties $properties -OutputPath $privateConfigPath
    New-InstallFile -OutputPath $installPath -Token $installToken

    Write-Host "Upload des fichiers PHP/SQL..."
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/config.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/helpers.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/create_activation_session.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/device_status.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/activate" -FilePath (Join-Path $publicHtmlPath "activate/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/sql" -FilePath (Join-Path $publicHtmlPath "sql/init_activation_tables.sql")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remotePrivate -FilePath $privateConfigPath

    if (-not $SkipInstall) {
        Write-Host "Installation SQL temporaire..."
        Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath $installPath
        $installUrl = "https://$domain/install.php?token=$installToken"
        $installResponse = Invoke-RestMethod -Method Get -Uri $installUrl
        Assert-ApiResult -Result $installResponse -Label "install.php"
    }

    if (-not $SkipTests) {
        Write-Host "Tests publics..."
        $activateHtml = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "https://$domain/activate/"
        if ($activateHtml.Content -notmatch "SmartVision Activation" -or $activateHtml.Content -notmatch "Backend") {
            throw "La page /activate/ ne retourne pas le contenu attendu."
        }

        $sessionBody = @{
            device_id = "test-device-001"
            device_name = "Test Android TV"
            app_version = "1.0.0"
        } | ConvertTo-Json
        $session = Invoke-RestMethod -Method Post -Uri "https://$domain/api/create_activation_session.php" -ContentType "application/json" -Body $sessionBody
        Assert-ApiResult -Result $session -Label "create_activation_session"
        if ([string]::IsNullOrWhiteSpace([string]$session.short_code) -or [string]::IsNullOrWhiteSpace([string]$session.qr_url)) {
            throw "Session d activation incomplete."
        }

        $status = Invoke-RestMethod -Method Get -Uri "https://$domain/api/device_status.php?device_id=test-device-001"
        Assert-ApiResult -Result $status -Label "device_status"
        if ($status.status -ne "pending" -or $status.activated -ne $false) {
            throw "Statut appareil inattendu."
        }

        Write-Host "Tests OK: /activate/, create_activation_session, device_status."
    }

    Write-Host "Deploiement phase 1 termine."
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}
