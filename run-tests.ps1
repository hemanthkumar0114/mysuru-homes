# Runs the whole automated test suite - backend and frontend - with one command.
# Backend tests run against an in-memory database (see backend/src/test/resources),
# so nothing here needs MySQL running or any environment variable set.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "==> Backend tests (JUnit + Spring Boot)"
Push-Location (Join-Path $root "backend")
try {
    & .\mvnw.cmd test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "==> Frontend tests (Vitest)"
Push-Location (Join-Path $root "frontend")
try {
    npm test
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "All tests passed."
