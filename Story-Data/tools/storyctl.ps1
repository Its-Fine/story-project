[CmdletBinding()]
param(
    [Parameter(Position = 0, Mandatory = $true)]
    [ValidateSet('init-env', 'check-config', 'status', 'logs', 'restart', 'update')]
    [string]$Command,

    [Parameter(Position = 1)]
    [ValidateSet('frontend', 'story-auth', 'story-back', 'story-admin', 'postgresql', 'minio', 'minio-init')]
    [string]$Service,

    [string]$EnvFile = '.env',
    [string]$DockerContext,
    [int]$Tail = 100,
    [switch]$Follow
)

$ErrorActionPreference = 'Stop'
$dataDirectory = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $dataDirectory 'docker-compose.yml'
$templateFile = Join-Path $dataDirectory '.env.example'
$resolvedEnvFile = if ([IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile
} else {
    Join-Path $dataDirectory $EnvFile
}

$requiredVariables = @(
    'POSTGRES_DB', 'POSTGRES_USER', 'POSTGRES_PASSWORD',
    'STORY_MIGRATION_DB_PASSWORD', 'STORY_AUTH_DB_PASSWORD',
    'STORY_BACK_DB_PASSWORD', 'STORY_ADMIN_DB_PASSWORD',
    'MINIO_ROOT_USER', 'MINIO_ROOT_PASSWORD', 'MINIO_MC_BUCKET',
    'STORY_JWT_SECRET', 'STORY_COOKIE_SECURE', 'STORY_COOKIE_SAME_SITE',
    'STORY_CORS_ALLOWED_ORIGINS'
)

function New-RandomSecret([int]$ByteCount = 32) {
    $bytes = New-Object byte[] $ByteCount
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

function Read-EnvironmentFile([string]$Path) {
    $values = [ordered]@{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $values
    }
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
        $name, $value = $line -split '=', 2
        $values[$name.Trim()] = $value.Trim()
    }
    return $values
}

function Set-MissingEnvironmentValues([string]$Path, [hashtable]$Defaults) {
    $lines = [Collections.Generic.List[string]](Get-Content -LiteralPath $Path)
    $known = Read-EnvironmentFile $Path
    foreach ($entry in $Defaults.GetEnumerator()) {
        if ($known.Contains($entry.Key) -and -not [string]::IsNullOrWhiteSpace($known[$entry.Key])) {
            continue
        }
        $replacement = "$($entry.Key)=$($entry.Value)"
        $index = -1
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i] -match "^$([regex]::Escape($entry.Key))=") {
                $index = $i
                break
            }
        }
        if ($index -ge 0) { $lines[$index] = $replacement } else { $lines.Add($replacement) }
    }
    Set-Content -LiteralPath $Path -Value $lines -Encoding utf8
}

function Invoke-Compose([string[]]$Arguments) {
    $dockerArguments = @()
    if ($DockerContext) {
        $dockerArguments += @('--context', $DockerContext)
    }
    $dockerArguments += @('compose', '--file', $composeFile, '--env-file', $resolvedEnvFile)
    $dockerArguments += $Arguments
    & docker @dockerArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose finished with exit code $LASTEXITCODE"
    }
}

function Invoke-Docker([string[]]$Arguments) {
    $dockerArguments = @()
    if ($DockerContext) {
        $dockerArguments += @('--context', $DockerContext)
    }
    $dockerArguments += $Arguments
    & docker @dockerArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker finished with exit code $LASTEXITCODE"
    }
}

function Wait-ServiceReady([string]$ServiceName, [int]$TimeoutSeconds = 90) {
    $containerId = (Invoke-Compose @('ps', '--quiet', $ServiceName) | Select-Object -First 1).Trim()
    if (-not $containerId) { throw "No container was created for $ServiceName." }

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $state = (Invoke-Docker @(
            'inspect', '--format',
            '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}',
            $containerId
        ) | Select-Object -First 1).Trim()
        if ($state -eq 'running healthy' -or $state -eq 'running none') {
            Write-Host "$ServiceName is ready ($state)."
            return
        }
        if ($ServiceName -eq 'minio-init' -and $state -eq 'exited none') {
            $exitCode = (Invoke-Docker @('inspect', '--format', '{{.State.ExitCode}}', $containerId) |
                    Select-Object -First 1).Trim()
            if ($exitCode -eq '0') {
                Write-Host 'minio-init completed successfully.'
                return
            }
        }
        if ($state -match '^(exited|dead|removing) ' -or $state -eq 'running unhealthy') {
            throw "$ServiceName failed readiness check: $state"
        }
        Start-Sleep -Seconds 3
    } while ([DateTime]::UtcNow -lt $deadline)

    throw "$ServiceName did not become ready within $TimeoutSeconds seconds."
}

function Assert-EnvironmentConfiguration {
    if (-not (Test-Path -LiteralPath $resolvedEnvFile)) {
        throw "Environment file not found: $resolvedEnvFile. Run init-env first."
    }
    $values = Read-EnvironmentFile $resolvedEnvFile
    $missing = @($requiredVariables | Where-Object {
        -not $values.Contains($_) -or [string]::IsNullOrWhiteSpace($values[$_])
    })
    if ($missing.Count -gt 0) {
        throw "Required variables are missing or empty: $($missing -join ', ')"
    }

    try {
        $jwtBytes = [Convert]::FromBase64String($values['STORY_JWT_SECRET'])
    } catch {
        throw 'STORY_JWT_SECRET must be valid Base64.'
    }
    if ($jwtBytes.Length -lt 32) {
        throw 'STORY_JWT_SECRET must decode to at least 32 bytes.'
    }
    if ($values['STORY_COOKIE_SAME_SITE'] -eq 'None' -and $values['STORY_COOKIE_SECURE'] -ne 'true') {
        throw 'SameSite=None requires STORY_COOKIE_SECURE=true.'
    }
    if ($values['STORY_CORS_ALLOWED_ORIGINS'] -match '(^|,)\s*\*\s*(,|$)') {
        throw 'STORY_CORS_ALLOWED_ORIGINS must not contain a wildcard.'
    }
}

switch ($Command) {
    'init-env' {
        if (-not (Test-Path -LiteralPath $resolvedEnvFile)) {
            Copy-Item -LiteralPath $templateFile -Destination $resolvedEnvFile
            Write-Host "Created $resolvedEnvFile from the production template."
        }
        $generated = @{
            POSTGRES_DB = 'story'
            POSTGRES_USER = 'story_owner'
            POSTGRES_PASSWORD = New-RandomSecret
            STORY_MIGRATION_DB_PASSWORD = New-RandomSecret
            STORY_AUTH_DB_PASSWORD = New-RandomSecret
            STORY_BACK_DB_PASSWORD = New-RandomSecret
            STORY_ADMIN_DB_PASSWORD = New-RandomSecret
            KEYCLOAK_DB_PASSWORD = New-RandomSecret
            KEYCLOAK_ADMIN_USERNAME = 'keycloak-admin'
            KEYCLOAK_ADMIN_PASSWORD = New-RandomSecret
            MINIO_ROOT_USER = 'story_minio'
            MINIO_ROOT_PASSWORD = New-RandomSecret
            MINIO_MC_BUCKET = 'story-works'
            STORY_JWT_SECRET = New-RandomSecret 48
        }
        Set-MissingEnvironmentValues $resolvedEnvFile $generated
        Write-Host 'Filled only missing generated values. Existing values and secrets were preserved.'
        Write-Host 'Set STORY_CORS_ALLOWED_ORIGINS and review cookie/HTTPS settings, then run check-config.'
    }
    'check-config' {
        Assert-EnvironmentConfiguration
        Invoke-Compose @('config', '--quiet')
        Write-Host 'Configuration is complete and Docker Compose accepted it.'
    }
    'status' {
        Assert-EnvironmentConfiguration
        Invoke-Compose @('ps')
    }
    'logs' {
        Assert-EnvironmentConfiguration
        $arguments = @('logs', '--tail', $Tail.ToString())
        if ($Follow) { $arguments += '--follow' }
        if ($Service) { $arguments += $Service }
        Invoke-Compose $arguments
    }
    'restart' {
        if (-not $Service) { throw 'restart requires a service name.' }
        Assert-EnvironmentConfiguration
        Invoke-Compose @('restart', $Service)
        Wait-ServiceReady $Service
    }
    'update' {
        if (-not $Service) { throw 'update requires a service name.' }
        Assert-EnvironmentConfiguration
        $oldContainerId = (Invoke-Compose @('ps', '--quiet', $Service) | Select-Object -First 1).Trim()
        $oldImageName = $null
        $rollbackImage = $null
        if ($oldContainerId) {
            $oldImageName = (Invoke-Docker @('inspect', '--format', '{{.Config.Image}}', $oldContainerId) |
                    Select-Object -First 1).Trim()
            $rollbackImage = "story-rollback-$Service`:$([DateTime]::UtcNow.ToString('yyyyMMddHHmmss'))"
            Invoke-Docker @('image', 'tag', $oldImageName, $rollbackImage)
        }

        try {
            Invoke-Compose @('build', '--pull', $Service)
            Invoke-Compose @('up', '--detach', '--no-deps', $Service)
            Wait-ServiceReady $Service
        } catch {
            if ($rollbackImage -and $oldImageName) {
                Write-Warning "Update failed. Restoring the previous image for $Service."
                Invoke-Docker @('image', 'tag', $rollbackImage, $oldImageName)
                Invoke-Compose @('up', '--detach', '--no-deps', $Service)
                Wait-ServiceReady $Service
            }
            throw
        }
    }
}
