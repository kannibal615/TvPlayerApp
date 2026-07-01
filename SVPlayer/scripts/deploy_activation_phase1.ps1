param(
    [switch]$SkipInstall,
    [switch]$SkipTests,
    [switch]$SkipApkUpload,
    [string]$QaDeviceId = "",
    [string]$QaShortCode = "",
    [int]$QaActiveHoldSeconds = 0
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

function New-SecureAdminPassword {
    param([int]$Length = 24)

    $alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@%_-"
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    } finally {
        $rng.Dispose()
    }

    $builder = New-Object System.Text.StringBuilder
    foreach ($value in $bytes) {
        [void]$builder.Append($alphabet[$value % $alphabet.Length])
    }
    return $builder.ToString()
}

function Ensure-AdminLocalProperties {
    param([string]$Path)

    $properties = Read-LocalProperties -Path $Path
    $lines = New-Object System.Collections.Generic.List[string]
    if (-not $properties.ContainsKey("SMARTVISION_ADMIN_USERNAME") -or [string]::IsNullOrWhiteSpace($properties["SMARTVISION_ADMIN_USERNAME"])) {
        $lines.Add("SMARTVISION_ADMIN_USERNAME=smartvision_admin")
    }
    if (-not $properties.ContainsKey("SMARTVISION_ADMIN_PASSWORD") -or [string]::IsNullOrWhiteSpace($properties["SMARTVISION_ADMIN_PASSWORD"])) {
        $lines.Add("SMARTVISION_ADMIN_PASSWORD=$(New-SecureAdminPassword)")
    }
    if (-not $properties.ContainsKey("SMARTVISION_CREDENTIALS_ENCRYPTION_KEY") -or [string]::IsNullOrWhiteSpace($properties["SMARTVISION_CREDENTIALS_ENCRYPTION_KEY"])) {
        $bytes = New-Object byte[] 32
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $rng.GetBytes($bytes)
        } finally {
            $rng.Dispose()
        }
        $lines.Add("SMARTVISION_CREDENTIALS_ENCRYPTION_KEY=$([Convert]::ToBase64String($bytes))")
    }

    if ($lines.Count -eq 0) {
        return
    }

    $content = [System.IO.File]::ReadAllText($Path)
    if ($content.Length -gt 0 -and -not $content.EndsWith("`n")) {
        $content += [Environment]::NewLine
    }
    $content += ($lines -join [Environment]::NewLine) + [Environment]::NewLine
    Write-Utf8NoBomFile -Path $Path -Content $content
    Write-Host "Identifiants admin generes dans local.properties."
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

function Resolve-CurlExe {
    $command = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @(
        (Join-Path $env:SystemRoot "System32\curl.exe"),
        (Join-Path $env:ProgramFiles "Git\mingw64\bin\curl.exe"),
        (Join-Path ${env:ProgramFiles(x86)} "Git\mingw64\bin\curl.exe")
    )
    foreach ($candidate in $candidates) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }

    throw "curl.exe introuvable. Installez curl ou ajoutez-le au PATH."
}

function Upload-File {
    param(
        [string]$BaseUrl,
        [hashtable]$Headers,
        [string]$Directory,
        [string]$FilePath
    )

    $curl = Resolve-CurlExe
    $auth = $Headers.Authorization
    $arguments = @(
        "-sS",
        "-H", "Authorization: $auth",
        "-F", "dir=$Directory",
        "-F", "overwrite=1",
        "-F", "file-1=@$FilePath;filename=$(Split-Path -Leaf $FilePath)",
        "$BaseUrl/execute/Fileman/upload_files"
    )

    $result = & $curl @arguments
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

function Get-PhpPasswordHash {
    param([string]$Password)

    $php = Get-Command php -ErrorAction Stop
    $hash = $Password | & $php.Source -r 'echo password_hash(trim(stream_get_contents(STDIN)), PASSWORD_DEFAULT);'
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace([string]$hash)) {
        throw "Impossible de generer le hash du mot de passe admin."
    }

    return ([string]$hash).Trim()
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
    $adminUsername = [string]$Properties["SMARTVISION_ADMIN_USERNAME"]
    $adminPasswordHash = Get-PhpPasswordHash -Password ([string]$Properties["SMARTVISION_ADMIN_PASSWORD"])
    $credentialsEncryptionKey = [string]$Properties["SMARTVISION_CREDENTIALS_ENCRYPTION_KEY"]
    $cpanelHost = [string]$Properties["CPANEL_HOST"]
    $cpanelUsername = Resolve-CpanelUsername -Properties $Properties
    $cpanelToken = Resolve-CpanelToken -Properties $Properties
    $smtpPassword = [string]$Properties["SMTP_PASSWORD"]
    $hilltopAdsApiKey = [string]$Properties["HILLTOPADS_API_KEY"]
    $hilltopAdsPublisherId = [string]$Properties["HILLTOPADS_PUBLISHER_ID"]
    $hilltopAdsVastTagUrl = [string]$Properties["HILLTOPADS_VAST_TAG_URL"]

    $content = @"
<?php
declare(strict_types=1);

return [
    'mysql_host' => '$(Escape-PhpString $mysqlHost)',
    'mysql_username' => '$(Escape-PhpString $mysqlUsername)',
    'mysql_password' => '$(Escape-PhpString $mysqlPassword)',
    'mysql_database' => '$(Escape-PhpString $mysqlDatabase)',
    'admin_username' => '$(Escape-PhpString $adminUsername)',
    'admin_password_hash' => '$(Escape-PhpString $adminPasswordHash)',
    'credentials_encryption_key' => '$(Escape-PhpString $credentialsEncryptionKey)',
    'cpanel_host' => '$(Escape-PhpString $cpanelHost)',
    'cpanel_username' => '$(Escape-PhpString $cpanelUsername)',
    'cpanel_token' => '$(Escape-PhpString $cpanelToken)',
    'smtp_password' => '$(Escape-PhpString $smtpPassword)',
    'hilltopads_api_key' => '$(Escape-PhpString $hilltopAdsApiKey)',
    'hilltopads_publisher_id' => '$(Escape-PhpString $hilltopAdsPublisherId)',
    'hilltopads_vast_tag_url' => '$(Escape-PhpString $hilltopAdsVastTagUrl)',
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

function New-AdminBootstrapFile {
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
$bootstrapToken = '__BOOTSTRAP_TOKEN__';

if (!hash_equals($bootstrapToken, (string) ($_GET['token'] ?? ''))) {
    http_response_code(404);
    echo json_encode(['success' => false, 'error' => 'Not found.']);
    exit;
}

register_shutdown_function(static function (): void {
    @unlink(__FILE__);
});

try {
    $config = load_database_config();
    $username = trim((string) ($config['admin_username'] ?? ''));
    $adminPassword = (string) ($_POST['admin_password'] ?? '');
    if ($username === '' || $adminPassword === '') {
        throw new RuntimeException('Admin bootstrap configuration missing.');
    }
    $passwordHash = password_hash($adminPassword, PASSWORD_DEFAULT);
    if (!is_string($passwordHash) || $passwordHash === '') {
        throw new RuntimeException('Admin password hash generation failed.');
    }

    $pdo = db();
    $pdo->exec(
        "CREATE TABLE IF NOT EXISTS admin_users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(100) NOT NULL UNIQUE,
            password_hash VARCHAR(255) NOT NULL,
            role ENUM('super_admin', 'admin', 'support') NOT NULL DEFAULT 'admin',
            status ENUM('active', 'blocked') NOT NULL DEFAULT 'active',
            failed_login_attempts INT UNSIGNED NOT NULL DEFAULT 0,
            locked_until DATETIME NULL,
            last_login_at DATETIME NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            INDEX (status),
            INDEX (last_login_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    $statement = $pdo->prepare(
        "INSERT INTO admin_users
            (username, password_hash, role, status, failed_login_attempts, locked_until)
         VALUES
            (:username, :password_hash, 'super_admin', 'active', 0, NULL)
         ON DUPLICATE KEY UPDATE
            password_hash = VALUES(password_hash),
            role = 'super_admin',
            status = 'active',
            failed_login_attempts = 0,
            locked_until = NULL"
    );
    $statement->execute([
        'username' => $username,
        'password_hash' => $passwordHash,
    ]);

    $privateConfigPath = (string) ($config['_private_config_path'] ?? '');
    if ($privateConfigPath !== '' && is_file($privateConfigPath)) {
        unset($config['_private_config_path']);
        $config['admin_password_hash'] = $passwordHash;
        $temporaryConfigPath = $privateConfigPath . '.tmp';
        $privateConfigContents = "<?php\ndeclare(strict_types=1);\n\nreturn "
            . var_export($config, true)
            . ";\n";
        if (file_put_contents($temporaryConfigPath, $privateConfigContents, LOCK_EX) === false
            || !rename($temporaryConfigPath, $privateConfigPath)) {
            @unlink($temporaryConfigPath);
            throw new RuntimeException('Private admin configuration update failed.');
        }
        @chmod($privateConfigPath, 0600);
    }

    echo json_encode([
        'success' => true,
        'message' => 'Admin account synchronized.',
    ], JSON_UNESCAPED_SLASHES);
} catch (Throwable $exception) {
    error_log('SmartVision admin bootstrap failed.');
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'error' => 'Admin account synchronization failed.',
    ], JSON_UNESCAPED_SLASHES);
}
'@
    $content = $content.Replace('__BOOTSTRAP_TOKEN__', $escapedToken)

    Write-Utf8NoBomFile -Path $OutputPath -Content $content
}

function Get-Sha256Hex {
    param([string]$Value)

    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Value)
        return -join ($sha.ComputeHash($bytes) | ForEach-Object { $_.ToString("x2") })
    } finally {
        $sha.Dispose()
    }
}

function New-QaSeedFile {
    param(
        [string]$OutputPath,
        [string]$Token,
        [string]$CodeHash
    )

    $content = @'
<?php
declare(strict_types=1);

require_once __DIR__ . '/api/config.php';

header('Content-Type: application/json; charset=utf-8');
$token = '__TOKEN__';

if (!hash_equals($token, (string) ($_GET['token'] ?? ''))) {
    http_response_code(404);
    echo json_encode(['success' => false, 'error' => 'Not found.']);
    exit;
}

register_shutdown_function(static function (): void {
    @unlink(__FILE__);
});

try {
    $statement = db()->prepare(
        "INSERT INTO activation_codes
            (code_hash, label, duration_days, max_devices, used_devices, status, valid_until)
         VALUES
            (:code_hash, 'Automated deployment QA', 1, 1, 0, 'active', DATE_ADD(NOW(), INTERVAL 1 DAY))
         ON DUPLICATE KEY UPDATE
            label = VALUES(label),
            duration_days = 1,
            max_devices = 1,
            used_devices = 0,
            status = 'active',
            valid_until = DATE_ADD(NOW(), INTERVAL 1 DAY)"
    );
    $statement->execute(['code_hash' => '__CODE_HASH__']);
    echo json_encode(['success' => true]);
} catch (Throwable $exception) {
    error_log('SmartVision QA seed failed.');
    http_response_code(500);
    echo json_encode(['success' => false, 'error' => 'QA seed failed.']);
}
'@
    $content = $content.Replace("__TOKEN__", (Escape-PhpString $Token))
    $content = $content.Replace("__CODE_HASH__", (Escape-PhpString $CodeHash))
    Write-Utf8NoBomFile -Path $OutputPath -Content $content
}

function New-QaCleanupFile {
    param(
        [string]$OutputPath,
        [string]$Token,
        [string]$CodeHash,
        [string]$DeviceId
    )

    $content = @'
<?php
declare(strict_types=1);

require_once __DIR__ . '/api/config.php';

header('Content-Type: application/json; charset=utf-8');
$token = '__TOKEN__';

if (!hash_equals($token, (string) ($_GET['token'] ?? ''))) {
    http_response_code(404);
    echo json_encode(['success' => false, 'error' => 'Not found.']);
    exit;
}

register_shutdown_function(static function (): void {
    @unlink(__FILE__);
});

$pdo = db();
try {
    $pdo->beginTransaction();
    foreach ([
        'DELETE FROM device_playlist_configs WHERE device_id = :device_id',
        'DELETE FROM device_activations WHERE device_id = :device_id',
        'DELETE FROM activation_sessions WHERE device_id = :device_id',
        'DELETE FROM devices WHERE device_id = :device_id',
    ] as $sql) {
        $statement = $pdo->prepare($sql);
        $statement->execute(['device_id' => '__DEVICE_ID__']);
    }

    $deleteCode = $pdo->prepare('DELETE FROM activation_codes WHERE code_hash = :code_hash');
    $deleteCode->execute(['code_hash' => '__CODE_HASH__']);
    $pdo->commit();
    echo json_encode(['success' => true]);
} catch (Throwable $exception) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('SmartVision QA cleanup failed.');
    http_response_code(500);
    echo json_encode(['success' => false, 'error' => 'QA cleanup failed.']);
}
'@
    $content = $content.Replace("__TOKEN__", (Escape-PhpString $Token))
    $content = $content.Replace("__CODE_HASH__", (Escape-PhpString $CodeHash))
    $content = $content.Replace("__DEVICE_ID__", (Escape-PhpString $DeviceId))
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

function New-AdsQaCleanupFile {
    param(
        [string]$OutputPath,
        [string]$Token,
        [string]$DeviceHash
    )

    $content = @'
<?php
declare(strict_types=1);

require_once __DIR__ . '/api/config.php';

header('Content-Type: application/json; charset=utf-8');
if (!hash_equals('__TOKEN__', (string) ($_GET['token'] ?? ''))) {
    http_response_code(404);
    echo json_encode(['success' => false]);
    exit;
}

register_shutdown_function(static function (): void { @unlink(__FILE__); });

try {
    $statement = db()->prepare('DELETE FROM ads_events WHERE device_id_hash = :device_id_hash');
    $statement->execute(['device_id_hash' => '__DEVICE_HASH__']);
    echo json_encode(['success' => true, 'deleted' => $statement->rowCount()], JSON_UNESCAPED_SLASHES);
} catch (Throwable $exception) {
    error_log('Ads QA cleanup failed.');
    http_response_code(500);
    echo json_encode(['success' => false, 'error' => 'Cleanup failed.']);
}
'@
    $content = $content.Replace('__TOKEN__', (Escape-PhpString $Token))
    $content = $content.Replace('__DEVICE_HASH__', (Escape-PhpString $DeviceHash))
    Write-Utf8NoBomFile -Path $OutputPath -Content $content
}

function New-ReleaseNotificationFile {
    param(
        [string]$OutputPath,
        [string]$Token,
        [int]$VersionCode,
        [string]$VersionName
    )

    $title = "Update available"
    $message = "SmartVision $VersionName ($VersionCode) is available. Open this notification to install the update."
    $content = @'
<?php
declare(strict_types=1);

require_once __DIR__ . '/api/config.php';
require_once __DIR__ . '/api/helpers.php';

header('Content-Type: application/json; charset=utf-8');
if (!hash_equals('__TOKEN__', (string) ($_GET['token'] ?? ''))) {
    http_response_code(404);
    echo json_encode(['success' => false]);
    exit;
}

register_shutdown_function(static function (): void { @unlink(__FILE__); });

try {
    $pdo = db();
    ensure_app_notifications_table($pdo);
    $title = '__TITLE__';
    $message = '__MESSAGE__';
    $statement = $pdo->prepare(
        "INSERT INTO app_notifications
            (title, message, target_scope, target_value, priority, status, created_by, expires_at)
         SELECT :title, :message, 'all', NULL, 'important', 'active', 'deploy_script', DATE_ADD(NOW(), INTERVAL 45 DAY)
         WHERE NOT EXISTS (
             SELECT 1 FROM app_notifications
             WHERE title = :title_check
               AND message = :message_check
               AND created_by = 'deploy_script'
               AND created_at > DATE_SUB(NOW(), INTERVAL 45 DAY)
         )"
    );
    $statement->execute([
        'title' => $title,
        'message' => $message,
        'title_check' => $title,
        'message_check' => $message,
    ]);
    echo json_encode(['success' => true, 'inserted' => $statement->rowCount()], JSON_UNESCAPED_SLASHES);
} catch (Throwable $exception) {
    error_log('SmartVision release notification failed.');
    http_response_code(500);
    echo json_encode(['success' => false, 'error' => 'Release notification failed.']);
}
'@
    $content = $content.Replace('__TOKEN__', (Escape-PhpString $Token))
    $content = $content.Replace('__TITLE__', (Escape-PhpString $title))
    $content = $content.Replace('__MESSAGE__', (Escape-PhpString $message))
    Write-Utf8NoBomFile -Path $OutputPath -Content $content
}

function Test-AdsApi {
    param(
        [string]$Domain,
        [string]$CpanelBaseUrl,
        [hashtable]$Headers,
        [string]$RemoteRoot,
        [string]$TempRoot
    )

    $config = Invoke-RestMethod -Method Get -Uri "https://$Domain/api/app/ads-config"
    Assert-ApiResult -Result $config -Label "ads-config"
    if ($null -eq $config.configVersion -or $config.adsOnlyInsidePlayer -ne $true) {
        throw "Configuration publicitaire API incomplete."
    }

    $deviceId = "qa-ads-" + [Guid]::NewGuid().ToString("N")
    $deviceHash = Get-Sha256Hex -Value $deviceId
    $cleanupToken = [Guid]::NewGuid().ToString("N")
    $cleanupPath = Join-Path $TempRoot "ads_qa_cleanup.php"
    New-AdsQaCleanupFile -OutputPath $cleanupPath -Token $cleanupToken -DeviceHash $deviceHash

    try {
        $payload = @{
            deviceId = $deviceId
            appVersion = "deploy-qa"
            platform = "ANDROID_TV"
            userStatus = "FREE_WITH_ADS"
            contentType = "LIVE_TV"
            provider = "HILLTOPADS_VAST"
            eventType = "AD_REQUESTED"
        } | ConvertTo-Json
        $event = Invoke-RestMethod -Method Post -Uri "https://$Domain/api/app/ads-events" -ContentType "application/json" -Body $payload
        Assert-ApiResult -Result $event -Label "ads-events"
        if ([int]$event.event_id -lt 1) {
            throw "Evenement publicitaire QA non persiste."
        }
    } finally {
        Upload-File -BaseUrl $CpanelBaseUrl -Headers $Headers -Directory $RemoteRoot -FilePath $cleanupPath
        $cleanup = Invoke-RestMethod -Method Get -Uri "https://$Domain/ads_qa_cleanup.php?token=$cleanupToken"
        Assert-ApiResult -Result $cleanup -Label "ads_qa_cleanup"
    }
}

function Get-CsrfTokenFromHtml {
    param([string]$Html)

    $match = [Regex]::Match($Html, 'name="csrf_token"\s+value="([^"]+)"')
    if (-not $match.Success) {
        throw "Token CSRF admin introuvable."
    }
    return $match.Groups[1].Value
}

function Get-HiddenValueFromHtml {
    param(
        [string]$Html,
        [string]$Name
    )

    $pattern = 'name="' + [Regex]::Escape($Name) + '"\s+value="([^"]+)"'
    $match = [Regex]::Match($Html, $pattern)
    if (-not $match.Success) {
        throw "Champ cache $Name introuvable."
    }
    return [System.Net.WebUtility]::HtmlDecode($match.Groups[1].Value)
}

function Test-AdminPanel {
    param(
        [string]$Domain,
        [string]$Username,
        [string]$Password
    )

    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $baseUrl = "https://$Domain/admin/"
    $codeId = $null
    $csrf = $null

    try {
        $loginPage = Invoke-WebRequest -UseBasicParsing -WebSession $session -Method Get -Uri $baseUrl
        if ($loginPage.Content -notmatch "Administration") {
            throw "Page login admin inattendue."
        }
        $csrf = Get-CsrfTokenFromHtml -Html $loginPage.Content

        $dashboard = Invoke-WebRequest -UseBasicParsing -WebSession $session -Method Post -Uri $baseUrl -Body @{
            action = "login"
            csrf_token = $csrf
            username = $Username
            password = $Password
        }
        if ($dashboard.Content -notmatch "Administration" -or $dashboard.Content -notmatch "Licences" -or $dashboard.Content -notmatch "Diagnostics") {
            throw "Connexion admin echouee."
        }
        $licensesPage = Invoke-WebRequest -UseBasicParsing -WebSession $session -Method Get -Uri "${baseUrl}?page=licenses"
        if ($licensesPage.Content -notmatch "Licences et codes") {
            throw "Page licences admin indisponible."
        }
        $adsPage = Invoke-WebRequest -UseBasicParsing -WebSession $session -Method Get -Uri "${baseUrl}?page=ads"
        if ($adsPage.Content -notmatch "Configuration des pubs" -or $adsPage.Content -notmatch "Provider / VAST" -or $adsPage.Content -notmatch "Evenements recents") {
            throw "Page publicites admin indisponible."
        }
        $csrf = Get-CsrfTokenFromHtml -Html $licensesPage.Content

        $label = "Automated Admin QA " + [Guid]::NewGuid().ToString("N").Substring(0, 8)
        $generated = Invoke-WebRequest -UseBasicParsing -WebSession $session -Method Post -Uri $baseUrl -Body @{
            action = "generate_code"
            redirect_page = "licenses"
            csrf_token = $csrf
            label = $label
            duration_days = "1"
            max_devices = "1"
            quantity = "1"
            valid_until = ""
        }
        if ($generated.Content -notmatch '(SV-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}|[A-Z2-9]{10})') {
            throw "Code admin QA non affiche."
        }

        $rowPattern = 'data-code-id="(\d+)"\s+data-label="' + [Regex]::Escape($label) + '"'
        $rowMatch = [Regex]::Match($generated.Content, $rowPattern)
        if (-not $rowMatch.Success) {
            throw "Code admin QA introuvable dans la liste."
        }
        $codeId = $rowMatch.Groups[1].Value
        $csrf = Get-CsrfTokenFromHtml -Html $generated.Content

        $disabled = Invoke-WebRequest -UseBasicParsing -WebSession $session -Method Post -Uri $baseUrl -Body @{
            action = "set_code_status"
            redirect_page = "licenses"
            csrf_token = $csrf
            code_id = $codeId
            status = "disabled"
        }
        $disabledPattern = 'data-code-id="' + [Regex]::Escape($codeId) + '"[\s\S]*?<span class="admin-state disabled">disabled</span>'
        if ($disabled.Content -notmatch $disabledPattern) {
            throw "Desactivation admin QA non confirmee."
        }
        $csrf = Get-CsrfTokenFromHtml -Html $disabled.Content

        $deleted = Invoke-WebRequest -UseBasicParsing -WebSession $session -Method Post -Uri $baseUrl -Body @{
            action = "delete_code"
            redirect_page = "licenses"
            csrf_token = $csrf
            code_id = $codeId
        }
        if ($deleted.Content -match ('data-code-id="' + [Regex]::Escape($codeId) + '"')) {
            throw "Suppression admin QA non confirmee."
        }
        $codeId = $null
        $csrf = Get-CsrfTokenFromHtml -Html $deleted.Content

        $logout = Invoke-WebRequest -UseBasicParsing -WebSession $session -Method Post -Uri "https://$Domain/admin/logout.php" -Body @{
            csrf_token = $csrf
        }
        if ($logout.Content -notmatch "Se connecter") {
            throw "Deconnexion admin non confirmee."
        }
    } finally {
        if ($null -ne $codeId -and $null -ne $csrf) {
            try {
                Invoke-WebRequest -UseBasicParsing -WebSession $session -Method Post -Uri $baseUrl -Body @{
                    action = "delete_code"
                    csrf_token = $csrf
                    code_id = $codeId
                } | Out-Null
            } catch {
            }
        }
    }
}

Add-Type -AssemblyName System.Web

Ensure-AdminLocalProperties -Path $localPropertiesPath
$properties = Read-LocalProperties -Path $localPropertiesPath
foreach ($name in @("CPANEL_API_KEY", "CPANEL_HOST", "DOMAINE_SERVER", "MYSQL_HOST", "MYSQL_USERNAME", "MYSQL_PASSWORD", "MYSQL_DATABASE", "SMARTVISION_ADMIN_USERNAME", "SMARTVISION_ADMIN_PASSWORD", "SMARTVISION_CREDENTIALS_ENCRYPTION_KEY")) {
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
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "_includes"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent "$remoteRoot/api" -Name "app"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent "$remoteRoot/api" -Name "devices"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent "$remoteRoot/api" -Name "licenses"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "activate"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "activation"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "xtream"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "playlist"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "account"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "success"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "admin"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "sql"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "assets"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent "$remoteRoot/assets" -Name "images"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "downloads"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "privacy-policy"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "terms-of-use"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "contact"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "payment-callback"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "verify-email"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "legal-notice"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent $remoteRoot -Name "legal-iptv-player"
    Ensure-RemoteDirectory -BaseUrl $cpanelBaseUrl -Headers $headers -Username $cpanelUsername -Parent "." -Name $remotePrivate

    $privateConfigPath = Join-Path $tempRoot "config.php"
    $installPath = Join-Path $tempRoot "install.php"
    $installToken = [Guid]::NewGuid().ToString("N")
    $adminBootstrapPath = Join-Path $tempRoot "admin_bootstrap.php"
    $adminBootstrapToken = [Guid]::NewGuid().ToString("N")
    New-PrivateConfigFile -Properties $properties -OutputPath $privateConfigPath
    New-InstallFile -OutputPath $installPath -Token $installToken
    New-AdminBootstrapFile -OutputPath $adminBootstrapPath -Token $adminBootstrapToken

    Write-Host "Upload des fichiers PHP/SQL..."
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath (Join-Path $publicHtmlPath ".htaccess")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath (Join-Path $publicHtmlPath "index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath (Join-Path $publicHtmlPath "legal_page.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath (Join-Path $publicHtmlPath "download.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/_includes" -FilePath (Join-Path $publicHtmlPath "_includes/site_layout.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/config.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/helpers.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/ads_service.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/anomaly_service.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/behavior_service.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/device_diagnostics_service.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/notifications.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/monetization_rules.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/device_state.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/commerce.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/gammal-webhook.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/mail_service.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/create_activation_session.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/device_status.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/validate_activation.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/start_trial.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/save_playlist_config.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/create_playlist_setup_session.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/home_slides.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/app_update.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api" -FilePath (Join-Path $publicHtmlPath "api/app_config.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/app" -FilePath (Join-Path $publicHtmlPath "api/app/ads-config.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/app" -FilePath (Join-Path $publicHtmlPath "api/app/ads-events.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/app" -FilePath (Join-Path $publicHtmlPath "api/app/ads-vast.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/app" -FilePath (Join-Path $publicHtmlPath "api/app/anomaly-events.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/app" -FilePath (Join-Path $publicHtmlPath "api/app/behavior-events.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/app" -FilePath (Join-Path $publicHtmlPath "api/app/device-diagnostics.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/devices" -FilePath (Join-Path $publicHtmlPath "api/devices/register.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/devices" -FilePath (Join-Path $publicHtmlPath "api/devices/start_trial.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/devices" -FilePath (Join-Path $publicHtmlPath "api/devices/enable_free_with_ads.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/api/licenses" -FilePath (Join-Path $publicHtmlPath "api/licenses/activate.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/activate" -FilePath (Join-Path $publicHtmlPath "activate/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/activation" -FilePath (Join-Path $publicHtmlPath "activation/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/xtream" -FilePath (Join-Path $publicHtmlPath "xtream/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/playlist" -FilePath (Join-Path $publicHtmlPath "playlist/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/account" -FilePath (Join-Path $publicHtmlPath "account/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/success" -FilePath (Join-Path $publicHtmlPath "success/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/privacy-policy" -FilePath (Join-Path $publicHtmlPath "privacy-policy/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/terms-of-use" -FilePath (Join-Path $publicHtmlPath "terms-of-use/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/contact" -FilePath (Join-Path $publicHtmlPath "contact/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/payment-callback" -FilePath (Join-Path $publicHtmlPath "payment-callback/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/verify-email" -FilePath (Join-Path $publicHtmlPath "verify-email/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/legal-notice" -FilePath (Join-Path $publicHtmlPath "legal-notice/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/legal-iptv-player" -FilePath (Join-Path $publicHtmlPath "legal-iptv-player/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/admin" -FilePath (Join-Path $publicHtmlPath "admin/bootstrap.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/admin" -FilePath (Join-Path $publicHtmlPath "admin/index.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/admin" -FilePath (Join-Path $publicHtmlPath "admin/logout.php")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/sql" -FilePath (Join-Path $publicHtmlPath "sql/init_activation_tables.sql")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/site.css")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/site-overrides.css")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/account.css")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/admin.css")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/admin-overrides.css")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/mobile.css")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/activation.js")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/site.js")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/account.js")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/payment-callback.js")
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets" -FilePath (Join-Path $publicHtmlPath "assets/admin.js")
    Get-ChildItem -LiteralPath (Join-Path $publicHtmlPath "assets/images") -File | Where-Object {
        $_.Extension -match '^\.(png|jpg|jpeg|webp|svg)$'
    } | ForEach-Object {
        Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/assets/images" -FilePath $_.FullName
    }
    $releaseApk = Join-Path $projectRoot "app/build/outputs/apk/release/app-release.apk"
    if ((-not $SkipApkUpload) -and (Test-Path -LiteralPath $releaseApk)) {
        $buildGradlePath = Join-Path $projectRoot "app/build.gradle.kts"
        $buildGradle = Get-Content -LiteralPath $buildGradlePath -Raw
        if ($buildGradle -notmatch "versionCode\s*=\s*(\d+)") {
            throw "versionCode introuvable dans app/build.gradle.kts."
        }
        $apkVersionCode = [int]$Matches[1]
        if ($buildGradle -notmatch 'versionName\s*=\s*"([^"]+)"') {
            throw "versionName introuvable dans app/build.gradle.kts."
        }
        $apkVersionName = $Matches[1]
        $apkInfo = Get-Item -LiteralPath $releaseApk
        $apkHash = (Get-FileHash -LiteralPath $releaseApk -Algorithm SHA256).Hash.ToLowerInvariant()
        $apkHashShort = $apkHash.Substring(0, 8)
        $versionedApkName = "smartvision-tv-v$apkVersionCode-$apkHashShort.apk"
        $downloadApk = Join-Path $tempRoot "smartvision-tv.apk"
        $versionedApk = Join-Path $tempRoot $versionedApkName
        $versionManifest = Join-Path $tempRoot "smartvision-tv.version.json"
        Copy-Item -LiteralPath $releaseApk -Destination $downloadApk
        Copy-Item -LiteralPath $releaseApk -Destination $versionedApk
        [ordered]@{
            version_code = $apkVersionCode
            version_name = $apkVersionName
            apk_file = $versionedApkName
            apk_sha256 = $apkHash
            apk_size = $apkInfo.Length
            mandatory = $false
            release_notes = "Nouvelle version SmartVision avec correctifs et ameliorations."
            generated_at = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
        } | ConvertTo-Json | Set-Content -LiteralPath $versionManifest -Encoding UTF8
        Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/downloads" -FilePath $versionedApk
        Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/downloads" -FilePath $versionManifest
        $releaseNotificationToken = [Guid]::NewGuid().ToString("N")
        $releaseNotificationPath = Join-Path $tempRoot "release_notification.php"
        New-ReleaseNotificationFile `
            -OutputPath $releaseNotificationPath `
            -Token $releaseNotificationToken `
            -VersionCode $apkVersionCode `
            -VersionName $apkVersionName
        Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath $releaseNotificationPath
        $releaseNotification = Invoke-RestMethod -Method Get -Uri "https://$domain/release_notification.php?token=$releaseNotificationToken"
        Assert-ApiResult -Result $releaseNotification -Label "release_notification"
        try {
            Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory "$remoteRoot/downloads" -FilePath $downloadApk
        } catch {
            Write-Warning "Upload optionnel de smartvision-tv.apk echoue. L APK versionnee reste disponible pour la mise a jour in-app."
        }
    }
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remotePrivate -FilePath $privateConfigPath

    Write-Host "Synchronisation securisee du compte admin..."
    Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath $adminBootstrapPath
    $adminBootstrapUrl = "https://$domain/admin_bootstrap.php?token=$adminBootstrapToken"
    $adminBootstrapResponse = Invoke-RestMethod -Method Post -Uri $adminBootstrapUrl -Body @{
        admin_password = [string]$properties["SMARTVISION_ADMIN_PASSWORD"]
    }
    Assert-ApiResult -Result $adminBootstrapResponse -Label "admin_bootstrap.php"

    if (-not $SkipInstall) {
        Write-Host "Installation SQL temporaire..."
        Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath $installPath
        $installUrl = "https://$domain/install.php?token=$installToken"
        $installResponse = Invoke-RestMethod -Method Get -Uri $installUrl
        Assert-ApiResult -Result $installResponse -Label "install.php"
    }

    if (-not $SkipTests) {
        Write-Host "Tests publics site et activation..."
        $homeHtml = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "https://$domain/"
        if ($homeHtml.Content -notmatch "SmartVision IPTV Player" -or $homeHtml.Content -notmatch "Acheter une licence" -or $homeHtml.Content -notmatch "Aucun contenu inclus") {
            throw "La page d accueil ne retourne pas le contenu attendu."
        }
        $legalPages = @(
            @{ Path = "privacy-policy"; Expected = "privacy-policy|Politique" },
            @{ Path = "terms-of-use"; Expected = "terms-of-use|Conditions" },
            @{ Path = "contact"; Expected = "Contact" },
            @{ Path = "legal-notice"; Expected = "legal-notice|Mentions" },
            @{ Path = "legal-iptv-player"; Expected = "legal-iptv-player|Lecteur IPTV" }
        )
        foreach ($legalPage in $legalPages) {
            $legalHtml = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "https://$domain/$($legalPage.Path)/"
            if ($legalHtml.Content -notmatch $legalPage.Expected -or $legalHtml.Content -notmatch "/privacy-policy/") {
                throw "La page /$($legalPage.Path)/ ne retourne pas le contenu attendu."
            }
        }
        $accountHtml = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "https://$domain/account/?plan=year_1"
        if ($accountHtml.Content -notmatch "Cr.er mon compte" -or $accountHtml.Content -notmatch "Connexion") {
            throw "La page compte/commande ne retourne pas le contenu attendu."
        }
        $activateHtml = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "https://$domain/activate/"
        if ($activateHtml.Content -notmatch "Activer mon appareil") {
            throw "La page /activate/ ne retourne pas le contenu attendu."
        }
        $updateStatus = Invoke-RestMethod -Method Get -Uri "https://$domain/api/app_update.php?version_code=0&version_name=qa"
        Assert-ApiResult -Result $updateStatus -Label "app_update"
        if ([int]$updateStatus.latest_version_code -lt 0) {
            throw "Le manifest de mise a jour est invalide."
        }
        Test-AdsApi `
            -Domain $domain `
            -CpanelBaseUrl $cpanelBaseUrl `
            -Headers $headers `
            -RemoteRoot $remoteRoot `
            -TempRoot $tempRoot
        Write-Host "Tests pubs OK: configuration, evenement et nettoyage."

        $useExistingQaSession = -not [string]::IsNullOrWhiteSpace($QaDeviceId) -and -not [string]::IsNullOrWhiteSpace($QaShortCode)
        $qaDeviceId = if ($useExistingQaSession) { $QaDeviceId } else { "qa-phase2-" + [Guid]::NewGuid().ToString("N") }
        $qaActivationCode = [Guid]::NewGuid().ToString("N").Substring(0, 16).ToUpperInvariant()
        $qaCodeHash = Get-Sha256Hex -Value $qaActivationCode
        $qaToken = [Guid]::NewGuid().ToString("N")
        $qaSeedPath = Join-Path $tempRoot "qa_seed.php"
        $qaCleanupPath = Join-Path $tempRoot "qa_cleanup.php"
        New-QaSeedFile -OutputPath $qaSeedPath -Token $qaToken -CodeHash $qaCodeHash
        New-QaCleanupFile -OutputPath $qaCleanupPath -Token $qaToken -CodeHash $qaCodeHash -DeviceId $qaDeviceId

        try {
            if ($useExistingQaSession) {
                $session = [PSCustomObject]@{
                    success = $true
                    short_code = $QaShortCode.Replace(" ", "").ToUpperInvariant()
                    qr_url = "https://$domain/activate/?device_id=$([Uri]::EscapeDataString($qaDeviceId))&code=$([Uri]::EscapeDataString($QaShortCode.Replace(' ', '').ToUpperInvariant()))"
                }
            } else {
                $sessionBody = @{
                    device_id = $qaDeviceId
                    device_name = "Automated Android TV QA"
                    app_version = "phase-2"
                } | ConvertTo-Json
                $session = Invoke-RestMethod -Method Post -Uri "https://$domain/api/create_activation_session.php" -ContentType "application/json" -Body $sessionBody
                Assert-ApiResult -Result $session -Label "create_activation_session"
                if ([string]::IsNullOrWhiteSpace([string]$session.short_code) -or [string]::IsNullOrWhiteSpace([string]$session.qr_url)) {
                    throw "Session d activation incomplete."
                }
                if ([string]::IsNullOrWhiteSpace([string]$session.device_token)) {
                    throw "Token appareil absent de la session."
                }
            }

            $portal = Invoke-WebRequest -UseBasicParsing -Method Get -Uri $session.qr_url
            if ($portal.Content -notmatch "Activez votre TV" -or $portal.Content -notmatch [Regex]::Escape([string]$session.short_code)) {
                throw "Le portail ne reconnait pas la session QR."
            }

            Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath $qaSeedPath
            $seed = Invoke-RestMethod -Method Get -Uri "https://$domain/qa_seed.php?token=$qaToken"
            Assert-ApiResult -Result $seed -Label "qa_seed"

            $validationBody = @{
                device_id = $qaDeviceId
                short_code = [string]$session.short_code
                activation_code = $qaActivationCode
            } | ConvertTo-Json
            $validation = Invoke-RestMethod -Method Post -Uri "https://$domain/api/validate_activation.php" -ContentType "application/json" -Body $validationBody
            Assert-ApiResult -Result $validation -Label "validate_activation"
            if ($validation.status -ne "active" -or $validation.activated -ne $true) {
                throw "Validation SmartVision inattendue."
            }

            $setupBody = @{
                device_id = $qaDeviceId
                device_token = [string]$session.device_token
            } | ConvertTo-Json
            $setupSession = Invoke-RestMethod -Method Post -Uri "https://$domain/api/create_playlist_setup_session.php" -ContentType "application/json" -Body $setupBody
            Assert-ApiResult -Result $setupSession -Label "create_playlist_setup_session"
            if ([string]::IsNullOrWhiteSpace([string]$setupSession.qr_url) -or $setupSession.qr_url -notmatch "/xtream/\?") {
                throw "Lien QR Xtream inattendu."
            }

            $playlistBody = @{
                device_id = $qaDeviceId
                short_code = [string]$setupSession.short_code
                host = "https://demo.invalid"
                username = "qa-user"
                password = "qa-password"
            } | ConvertTo-Json
            $playlist = Invoke-RestMethod -Method Post -Uri "https://$domain/api/save_playlist_config.php" -ContentType "application/json" -Body $playlistBody
            Assert-ApiResult -Result $playlist -Label "save_playlist_config"

            $statusUrl = "https://$domain/api/device_status.php?device_id=$([Uri]::EscapeDataString($qaDeviceId))&device_token=$([Uri]::EscapeDataString([string]$session.device_token))"
            $status = Invoke-RestMethod -Method Get -Uri $statusUrl
            Assert-ApiResult -Result $status -Label "device_status active"
            if ($status.status -ne "active" -or $status.activated -ne $true) {
                throw "Statut appareil actif inattendu."
            }
            if ($status.playlist_configured -ne $true -or $status.playlist_config.username -ne "qa-user") {
                throw "Configuration Xtream securisee non restituee."
            }

            if ($useExistingQaSession -and $QaActiveHoldSeconds -gt 0) {
                Start-Sleep -Seconds $QaActiveHoldSeconds
            }

            Write-Host "Tests OK: site public, portail, validation et configuration Xtream chiffree."
        } finally {
            Upload-File -BaseUrl $cpanelBaseUrl -Headers $headers -Directory $remoteRoot -FilePath $qaCleanupPath
            $cleanup = Invoke-RestMethod -Method Get -Uri "https://$domain/qa_cleanup.php?token=$qaToken"
            Assert-ApiResult -Result $cleanup -Label "qa_cleanup"
        }

        Write-Host "Tests administration..."
        Test-AdminPanel `
            -Domain $domain `
            -Username ([string]$properties["SMARTVISION_ADMIN_USERNAME"]) `
            -Password ([string]$properties["SMARTVISION_ADMIN_PASSWORD"])
        Write-Host "Tests admin OK: login, generation, desactivation, suppression et logout."
    }

    Write-Host "Deploiement activation phase 3 termine."
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}
