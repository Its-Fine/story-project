[CmdletBinding()]
param(
    [string]$EnvFile = '.env',
    [string]$KeycloakUrl = 'http://localhost:8080',
    [string]$Realm = 'story'
)

$ErrorActionPreference = 'Stop'
$dataDirectory = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $dataDirectory 'docker-compose.yml'
$resolvedEnvFile = if ([IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile
} else {
    Join-Path $dataDirectory $EnvFile
}

function Read-EnvironmentFile([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Environment file not found: $Path"
    }
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
        $name, $value = $line -split '=', 2
        $values[$name.Trim()] = $value.Trim()
    }
    return $values
}

function Require-Value([hashtable]$Values, [string]$Name) {
    if (-not $Values.ContainsKey($Name) -or [string]::IsNullOrWhiteSpace($Values[$Name])) {
        throw "$Name is missing in $resolvedEnvFile"
    }
    return $Values[$Name]
}

$environment = Read-EnvironmentFile $resolvedEnvFile
$bootstrapUsername = Require-Value $environment 'KEYCLOAK_ADMIN_USERNAME'
$bootstrapPassword = Require-Value $environment 'KEYCLOAK_ADMIN_PASSWORD'
$applicationAdminEmail = Require-Value $environment 'STORY_ADMIN_USERNAME'
$applicationAdminPassword = Require-Value $environment 'STORY_ADMIN_PASSWORD'
$postgresUser = Require-Value $environment 'POSTGRES_USER'
$postgresDatabase = Require-Value $environment 'POSTGRES_DB'

try {
    [void][Net.Mail.MailAddress]$applicationAdminEmail
} catch {
    throw 'STORY_ADMIN_USERNAME must contain a valid email address.'
}

$tokenResponse = Invoke-RestMethod -Method Post `
    -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" `
    -ContentType 'application/x-www-form-urlencoded' `
    -Body @{
        client_id = 'admin-cli'
        grant_type = 'password'
        username = $bootstrapUsername
        password = $bootstrapPassword
    }
$headers = @{ Authorization = "Bearer $($tokenResponse.access_token)" }

$encodedEmail = [Uri]::EscapeDataString($applicationAdminEmail)
$usersResponse = Invoke-RestMethod -Method Get `
    -Uri "$KeycloakUrl/admin/realms/$Realm/users?email=$encodedEmail&exact=true" `
    -Headers $headers
$users = @($usersResponse | Where-Object { $_ -and $_.id })

if ($users.Count -eq 0) {
    $newUser = @{
        username = $applicationAdminEmail
        email = $applicationAdminEmail
        enabled = $true
        emailVerified = $true
        requiredActions = @('UPDATE_PASSWORD')
        credentials = @(
            @{
                type = 'password'
                value = $applicationAdminPassword
                temporary = $true
            }
        )
    } | ConvertTo-Json -Depth 6

    Invoke-RestMethod -Method Post `
        -Uri "$KeycloakUrl/admin/realms/$Realm/users" `
        -Headers $headers -ContentType 'application/json' -Body $newUser
    $usersResponse = Invoke-RestMethod -Method Get `
        -Uri "$KeycloakUrl/admin/realms/$Realm/users?email=$encodedEmail&exact=true" `
        -Headers $headers
    $users = @($usersResponse | Where-Object { $_ -and $_.id })
    Write-Host "Created Keycloak user for $applicationAdminEmail with a temporary password."
} else {
    Write-Host "Keycloak user for $applicationAdminEmail already exists; password was not changed."
}

if ($users.Count -ne 1) {
    throw "Expected exactly one Keycloak user for $applicationAdminEmail, found $($users.Count)."
}
$keycloakUserId = $users[0].id
if ([string]::IsNullOrWhiteSpace($keycloakUserId) -or $keycloakUserId -notmatch '^[0-9a-fA-F-]{36}$') {
    throw "Keycloak returned an invalid user id: $keycloakUserId"
}
$normalizedKeycloakUrl = $KeycloakUrl.TrimEnd('/')
$encodedRealm = [Uri]::EscapeDataString($Realm)
$encodedUserId = [Uri]::EscapeDataString([string]$keycloakUserId)

$adminRole = Invoke-RestMethod -Method Get `
    -Uri "$normalizedKeycloakUrl/admin/realms/$encodedRealm/roles/ADMIN" -Headers $headers
$roleBody = ConvertTo-Json -InputObject @($adminRole) -Depth 5
Invoke-RestMethod -Method Post `
    -Uri "$normalizedKeycloakUrl/admin/realms/$encodedRealm/users/$encodedUserId/role-mappings/realm" `
    -Headers $headers -ContentType 'application/json' -Body $roleBody

$issuer = "$normalizedKeycloakUrl/realms/$Realm"
$sql = @'
UPDATE users
SET oidc_issuer = :'issuer', oidc_subject = :'subject'
WHERE email = :'admin_email'
  AND (oidc_subject IS NULL OR (oidc_issuer = :'issuer' AND oidc_subject = :'subject'));
SELECT CASE WHEN COUNT(*) = 1 THEN 'LINK_OK' ELSE 'LINK_FAILED' END
FROM users
WHERE email = :'admin_email'
  AND oidc_issuer = :'issuer'
  AND oidc_subject = :'subject';
'@

$databaseResult = $sql | & docker compose `
    --file $composeFile `
    --env-file $resolvedEnvFile `
    exec -T postgresql psql `
    --username $postgresUser `
    --dbname $postgresDatabase `
    --set ON_ERROR_STOP=1 `
    --set "admin_email=$applicationAdminEmail" `
    --set "issuer=$issuer" `
    --set "subject=$keycloakUserId" `
    --tuples-only --no-align

if ($LASTEXITCODE -ne 0 -or $databaseResult -notcontains 'LINK_OK') {
    throw 'Keycloak user was created, but linking the existing Story profile failed.'
}

Write-Host "Assigned ADMIN and linked the existing Story profile to Keycloak subject $keycloakUserId."
Write-Host 'The temporary application password must be changed at the first Keycloak login.'
