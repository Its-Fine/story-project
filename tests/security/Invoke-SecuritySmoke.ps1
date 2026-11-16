param()

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$project = 'story-security-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$composeArgs = @('--project-name', $project, '--file', (Join-Path $PSScriptRoot 'compose.yaml'))
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.UseCookies = $false
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(10)
$script:checks = 0

function Invoke-Compose {
    param([string[]]$Arguments)
    & docker compose @composeArgs @Arguments
    if ($LASTEXITCODE -ne 0) { throw "docker compose failed: $($Arguments -join ' ')" }
}

function Send-Request {
    param([string]$Method, [string]$Url, $Body = $null, [string]$Token = '', [string]$Cookie = '')
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), $Url)
    if ($Token) { [void]$request.Headers.TryAddWithoutValidation('Authorization', "Bearer $Token") }
    if ($Cookie) { [void]$request.Headers.TryAddWithoutValidation('Cookie', "refreshToken=$Cookie") }
    if ($null -ne $Body) {
        $request.Content = [System.Net.Http.StringContent]::new(($Body | ConvertTo-Json -Compress), [Text.Encoding]::UTF8, 'application/json')
    }
    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        try {
            $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            $json = if ($content) { $content | ConvertFrom-Json } else { $null }
            $setCookie = if ($response.Headers.Contains('Set-Cookie')) { $response.Headers.GetValues('Set-Cookie') -join '; ' } else { '' }
            return [PSCustomObject]@{ Status = [int]$response.StatusCode; Json = $json; SetCookie = $setCookie }
        } finally { $response.Dispose() }
    } finally { $request.Dispose() }
}

function Assert-Status {
    param($Response, [int]$Expected, [string]$Label)
    if ($Response.Status -ne $Expected) { throw "$Label expected HTTP $Expected, received $($Response.Status)" }
    $script:checks++
    Write-Host "PASS $Label (HTTP $Expected)"
}

function Get-ServiceUrl {
    param([string]$Service, [int]$Port)
    $address = & docker compose @composeArgs port $Service $Port
    if ($LASTEXITCODE -ne 0) { throw "Cannot find test port for $Service" }
    return "http://$($address.Trim())/api/v1"
}

function Wait-Service {
    param([string]$Url)
    $deadline = [DateTime]::UtcNow.AddSeconds(120)
    while ([DateTime]::UtcNow -lt $deadline) {
        try { if ((Send-Request GET $Url).Status -eq 401) { return } } catch { }
        Start-Sleep -Seconds 2
    }
    throw "Test service did not become ready: $Url"
}

function Invoke-TestSql {
    param([string]$Sql)
    & docker compose @composeArgs exec -T postgresql psql -U security_test -d security_test -v ON_ERROR_STOP=1 -c $Sql | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Test database update failed' }
}

try {
    foreach ($module in 'story-auth', 'story-back') {
        if (-not (Get-ChildItem (Join-Path $repoRoot "$module/target/*.jar") -ErrorAction SilentlyContinue)) {
            throw "Run Maven verify for $module first; its packaged JAR is missing."
        }
    }
    Write-Host "Starting isolated project $project"
    Invoke-Compose -Arguments @('up', '-d', '--build', 'back')
    $back = Get-ServiceUrl back 8000
    Wait-Service "$back/account/data"
    # Start auth after back has initialized the shared, disposable schema.
    Invoke-Compose -Arguments @('up', '-d', '--build', 'auth')
    $auth = (Get-ServiceUrl auth 8001) + '/auth'
    Wait-Service "$auth/me"

    $signup = @{ email = 'security-smoke@example.com'; password = 'Password123!'; first_name = 'Security'; last_name = 'Smoke'; birth_date = '2000-01-01'; consent = $true; parent_consent = $false }
    $result = Send-Request POST "$auth/signup" $signup
    Assert-Status $result 200 'signup'
    if (-not $result.Json.access_token -or $result.SetCookie -notmatch 'HttpOnly' -or $result.SetCookie -notmatch 'Path=/api/v1/auth/refresh' -or $result.SetCookie -notmatch 'SameSite=Lax') {
        throw 'Signup token/cookie contract changed'
    }
    $login = @{ email = $signup.email; password = $signup.password }
    $result = Send-Request POST "$auth/login" $login
    Assert-Status $result 200 'login'
    $access = $result.Json.access_token
    if ($result.SetCookie -notmatch 'refreshToken=([^;]+)') { throw 'Missing refresh cookie' }
    $refresh = $Matches[1]
    $reset = @{ current_password = $signup.password; new_password = 'NewPassword123!' }

    Assert-Status (Send-Request GET "$back/account/data" -Token $access) 200 'account/access'
    Assert-Status (Send-Request GET "$back/account/data" -Token $refresh) 401 'account/refresh rejected'
    Assert-Status (Send-Request GET "$back/account/data") 401 'account/no token'
    Assert-Status (Send-Request GET "$auth/me" -Token $access) 200 'me/access'
    Assert-Status (Send-Request GET "$auth/me" -Token $refresh) 401 'me/refresh rejected'
    Assert-Status (Send-Request GET "$auth/me") 401 'me/no token'
    Assert-Status (Send-Request PUT "$auth/reset" $reset -Token $refresh) 401 'reset/refresh rejected'
    Assert-Status (Send-Request PUT "$auth/reset" $reset) 401 'reset/no token'
    Assert-Status (Send-Request PUT "$auth/reset" @{ current_password = 'wrong'; new_password = 'NewPassword123!' } -Token $access) 400 'reset/wrong password'
    $renewed = Send-Request POST "$auth/refresh" -Cookie $refresh
    Assert-Status $renewed 200 'refresh/refresh cookie'
    Assert-Status (Send-Request GET "$back/account/data" -Token $renewed.Json.access_token) 200 'backend accepts refreshed token'
    Assert-Status (Send-Request POST "$auth/refresh" -Cookie $access) 401 'refresh/access cookie rejected'
    Assert-Status (Send-Request POST "$auth/refresh") 401 'refresh/no cookie'

    Assert-Status (Send-Request GET "$back/admin/users/count" -Token $access) 403 'participant/admin denied'
    Invoke-TestSql "UPDATE users SET role = 'ADMIN' WHERE email = 'security-smoke@example.com'"
    Assert-Status (Send-Request GET "$back/admin/users/count" -Token $access) 200 'promotion uses database role'
    $me = Send-Request GET "$auth/me" -Token $access
    Assert-Status $me 200 'me after promotion'
    if ($me.Json.role -ne 'ADMIN') { throw 'Auth returned stale role after promotion' }
    $renewedAdmin = Send-Request POST "$auth/refresh" -Cookie $refresh
    Assert-Status $renewedAdmin 200 'refresh after promotion'
    Invoke-TestSql "UPDATE users SET role = 'PARTICIPANT' WHERE email = 'security-smoke@example.com'"
    Assert-Status (Send-Request GET "$back/admin/users/count" -Token $renewedAdmin.Json.access_token) 403 'demotion rejects stale admin token'
    $me = Send-Request GET "$auth/me" -Token $renewedAdmin.Json.access_token
    Assert-Status $me 200 'me after demotion'
    if ($me.Json.role -ne 'PARTICIPANT') { throw 'Auth returned stale role after demotion' }

    try {
        Invoke-TestSql "UPDATE users SET banned = true WHERE email = 'security-smoke@example.com'"
        Assert-Status (Send-Request GET "$back/account/data" -Token $access) 401 'banned/account'
        Assert-Status (Send-Request GET "$auth/me" -Token $access) 401 'banned/me'
        Assert-Status (Send-Request PUT "$auth/reset" $reset -Token $access) 401 'banned/reset'
        Assert-Status (Send-Request POST "$auth/refresh" -Cookie $refresh) 401 'banned/refresh'
        Assert-Status (Send-Request POST "$auth/login" $login) 401 'banned/login'
    } finally {
        Invoke-TestSql "UPDATE users SET banned = false, role = 'PARTICIPANT' WHERE email = 'security-smoke@example.com'"
    }
    Assert-Status (Send-Request GET "$back/account/data" -Token $access) 200 'unbanned/account'
    Assert-Status (Send-Request POST "$auth/login" $login) 200 'unbanned/login'
    Assert-Status (Send-Request PUT "$auth/reset" $reset -Token $access) 200 'reset/correct password'
    Assert-Status (Send-Request POST "$auth/login" $login) 401 'old password rejected'
    Assert-Status (Send-Request POST "$auth/login" @{ email = $signup.email; password = $reset.new_password }) 200 'new password accepted'
    Assert-Status (Send-Request PUT "$auth/reset" @{ current_password = $reset.new_password; new_password = $signup.password } -Token $access) 200 'restore test password'
    $logout = Send-Request POST "$auth/logout"
    Assert-Status $logout 200 'logout'
    if ($logout.SetCookie -notmatch 'Max-Age=0') { throw 'Logout did not clear the refresh cookie' }
    Write-Host "SUCCESS: $script:checks HTTP checks passed against two services and isolated PostgreSQL."
} catch {
    & docker compose @composeArgs logs --tail 80
    throw
} finally {
    $client.Dispose()
    # This random test project has only tmpfs data. Never target Story-Data or remove volumes.
    & docker compose @composeArgs down
    if ($LASTEXITCODE -ne 0) { Write-Warning "Could not clean up test project $project" }
}
