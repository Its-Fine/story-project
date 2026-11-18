# JWT and account security checks

These checks exercise `story-auth` and `story-back` from this branch. They do not
use the running development stack or existing PostgreSQL data.

## Verified on 2026-11-16

- `story-auth`: `mvn -B -ntp verify`, **71 tests**, zero failures/errors/skips, JAR packaged.
- `story-back`: `mvn -B -ntp verify`, **68 tests**, zero failures/errors/skips, JAR packaged.
- Java 21 in `maven:3.9-eclipse-temurin-21`; Spring Boot 3.5.11, JJWT 0.11.5.
- `Invoke-SecuritySmoke.ps1`: **33 HTTP checks passed** using both newly packaged
  services and isolated PostgreSQL 16. Test containers/network were cleaned up.
- Existing development containers/data were not changed. Postman and browser UI
  were not run. Production CORS/HTTPS/CSRF and MinIO operations are outside this suite.
- Backend compilation still reports a pre-existing MapStruct warning for
  `ApplicationMapper` fields `status` and `rejectionReason`; it does not fail this build.

## Files changed in this follow-up

Production code (relative to the service source package):

- `story-auth/src/main/java/com/example/story/auth/`:
  `config/SecurityConfig.java`, `controller/AuthController.java`,
  `security/JwtService.java`, `security/JwtAuthenticationFilter.java`,
  `service/AuthService.java`.
- `story-back/src/main/java/com/example/story/`:
  `security/SecurityConfig.java`, `security/JwtService.java`,
  `security/JwtAuthenticationFilter.java`, `exception/GlobalExceptionHandler.java`.
- Both service `pom.xml` files: test dependencies only.

Tests and instructions:

- `story-auth/src/test/java/com/example/story/auth/AuthSecurityIntegrationTest.java`
- `story-auth/src/test/java/com/example/story/auth/security/JwtServiceTest.java`
- `story-auth/src/test/java/com/example/story/testsupport/JwtTokens.java`
- `story-auth/src/test/resources/application-test.yaml`
- `story-back/src/test/java/com/example/story/StoryApplicationTests.java`
- `story-back/src/test/java/com/example/story/security/JwtServiceTest.java`
- `story-back/src/test/java/com/example/story/testsupport/JwtTokens.java`
- `story-back/src/test/resources/application-test.yaml`
- `tests/security/compose.yaml`, `Dockerfile`, `Dockerfile.dockerignore`,
  `Invoke-SecuritySmoke.ps1`, and this `README.md`.

## Maven verification

With Java 21 and Maven installed, run from the repository root:

```powershell
Set-Location C:\story-project
mvn -B -ntp -f story-auth/pom.xml verify
if ($LASTEXITCODE -ne 0) { throw 'Auth verification failed' }
mvn -B -ntp -f story-back/pom.xml verify
if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed' }
```

Alternatively, use Docker Desktop (no local Java/Maven installation needed):

```powershell
Set-Location C:\story-project
$repoRoot = (Get-Location).Path
foreach ($module in 'story-auth', 'story-back') {
    docker run --rm `
        --mount "type=bind,source=$repoRoot\$module,target=/workspace" `
        --mount type=volume,source=story-security-maven-cache,target=/root/.m2 `
        -w /workspace maven:3.9-eclipse-temurin-21 mvn -B -ntp verify
    if ($LASTEXITCODE -ne 0) { throw "Verification failed: $module" }
}
```

The tests run against H2 databases in memory through the `test` profile, with
actual repositories, controllers, password encoders and the Spring Security
filter chains. Test properties and H2 are excluded from production JARs.
Surefire reports are in each module's `target/surefire-reports` directory.

Covered cases:

- Access/refresh purpose separation and issued-token lifetimes.
- Missing, expired, unsigned, malformed and incorrectly signed tokens;
  absent/invalid subjects, missing expiration, invalid date types, future `nbf`,
  and legacy tokens without `token_type`.
- `/auth/me`, `/auth/reset`, `/auth/refresh`, login/signup/logout and `/account/data`.
- Existing sessions after account ban/unban/deletion and database role changes.
- Current-password verification and rejected requests leaving the password unchanged.
- Current database permissions in an actual `@PreAuthorize` admin endpoint.

## Two-service PostgreSQL HTTP verification

After **both** Maven verifications above succeed, run:

```powershell
Set-Location C:\story-project
powershell -NoProfile -ExecutionPolicy Bypass -File .\tests\security\Invoke-SecuritySmoke.ps1
```

The script requires Docker Compose v2 and Windows PowerShell 5.1 (or PowerShell 7).
It builds runtime images from the just-verified JARs and starts a randomly named
Compose project with PostgreSQL 16 in `tmpfs`. It binds the two API ports only to
localhost on automatically allocated ports. It creates its own test account,
checks HTTP responses and real shared-database changes, restores the test user's
password/ban/role on the successful path, and removes its test containers in
`finally`. Ban/role restoration is also attempted if a ban check fails.

No production volumes are mounted and no existing application containers are
restarted. This is a security integration suite, not a migration test against a
copy of the production schema. File uploads/MinIO and browser UI are not exercised.
The public JWT key and database credentials in this test stack are test fixtures.
The standard application Dockerfiles are not changed by this stage.

## Postman requests for a separately rebuilt local stack

These routes match the controllers. Set `authUrl=http://localhost:8001/api/v1/auth`
and `backUrl=http://localhost:8000/api/v1`. The existing running stack must first
be rebuilt from this branch for these requests to test the changes.

1. `POST {{authUrl}}/signup`, JSON:

   ```json
   {
     "email": "your-unique-test-account@example.com",
     "password": "TestPassword123!",
     "first_name": "Security",
     "last_name": "Test",
     "birth_date": "2000-01-01",
     "consent": true,
     "parent_consent": false
   }
   ```

2. `POST {{authUrl}}/login`, JSON with the same `email` and `password`.
   Save the response's `access_token` as `accessToken`; Postman stores the
   `refreshToken` cookie. The refresh token value is in `Set-Cookie`, not the JSON.
3. Send `Authorization: Bearer {{accessToken}}` to `GET {{backUrl}}/account/data`
   and `GET {{authUrl}}/me`: expect **200**.
4. Copy the refresh cookie value into Bearer authorization for those two requests:
   expect **401**. Restore the access token afterward.
5. `POST {{authUrl}}/refresh` with the stored refresh cookie: expect **200** and
   an access token accepted by `GET {{backUrl}}/account/data`.
6. Replace the refresh cookie with an access token: expect **401**. Remove the
   cookie entirely: also expect **401**. Restore the valid refresh cookie.
   Ensure the cookie jar does not silently add a second cookie with the same name.
7. `PUT {{authUrl}}/reset` with access Bearer authorization and JSON:

   ```json
   {"current_password":"TestPassword123!","new_password":"NewTestPassword123!"}
   ```

   Expect **200**. Missing/wrong `current_password` gives **400**; refresh Bearer
   or missing Bearer gives **401**. Login with the old password then gives **401**,
   with the new password **200**. Restore the test password using the same route.
8. For ban/role tests, prefer the isolated script above. If testing manually on a
   disposable database, record the test account's original state before updating
   only that account's `banned`/`role`. With `banned=true`, login, refresh, me,
   reset and account/data must all return **401**. Restore the original state.
   A participant calling `GET {{backUrl}}/admin/users/count` receives **403**;
   demoting an admin must revoke that access even with an already-issued token.

## Deployment and remaining scope

- Old JWTs without `token_type` are rejected; users must sign in again.
- Deploy both services with the same Base64 JWT key. Account data and password
  hashes are not migrated or changed by deployment.
- `/auth/reset` now requires `current_password`; the current frontend DTO already
  includes it. Response field names, route paths and refresh cookie scope remain
  compatible. `/auth/me` reports the current database role.
- Banned or deleted users are treated as unauthenticated (**401**). Authenticated
  users without the required role receive **403**.
- Logout clears the cookie only. Refresh rotation/revocation and invalidation of
  other sessions after password changes remain future work.
- The next configuration stage should remove fallback secrets, validate required
  JWT/DB/MinIO settings, and review production CORS, HTTPS, SameSite and CSRF.
  Flyway and changes to existing database schemas require a separate migration stage.
