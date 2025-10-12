# Maven Helper Script
# Usage: .\maven.ps1 [command] [options]
# Example: .\maven.ps1 clean install -DskipTests

param(
    [Parameter(Position=0)]
    [string]$Command = "help",

    [Parameter(Position=1, ValueFromRemainingArguments=$true)]
    [string[]]$MavenArgs
)

function Show-Help {
    Write-Host "`nMaven Helper Script" -ForegroundColor Cyan
    Write-Host "==================`n" -ForegroundColor Cyan
    Write-Host "Usage: .\maven.ps1 [command] [maven-options]`n"

    Write-Host "Common Commands:" -ForegroundColor Yellow
    Write-Host "  clean          - mvn clean"
    Write-Host "  compile        - mvn compile"
    Write-Host "  test           - mvn test"
    Write-Host "  package        - mvn package"
    Write-Host "  install        - mvn install"
    Write-Host "  deploy         - mvn deploy"
    Write-Host "  verify         - mvn verify"
    Write-Host "  ci             - mvn clean install"
    Write-Host "  cis            - mvn clean install -DskipTests"
    Write-Host "  run            - mvn spring-boot:run (or exec:java)"
    Write-Host "  tree           - mvn dependency:tree"
    Write-Host "  update         - mvn clean install -U"

    Write-Host "`nExamples:" -ForegroundColor Yellow
    Write-Host "  .\maven.ps1 ci"
    Write-Host "  .\maven.ps1 package -DskipTests"
    Write-Host "  .\maven.ps1 test -Dtest=MyTest"
    Write-Host "  .\maven.ps1 install -P production`n"
}

# Check if Maven is installed
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: Maven (mvn) is not found in PATH" -ForegroundColor Red
    Write-Host "Please install Maven or add it to your PATH" -ForegroundColor Red
    exit 1
}

# Build Maven command based on input
$mavenCommand = switch ($Command.ToLower()) {
    "clean"   { "mvn clean" }
    "compile" { "mvn compile" }
    "test"    { "mvn test" }
    "package" { "mvn package" }
    "install" { "mvn install" }
    "deploy"  { "mvn deploy" }
    "verify"  { "mvn verify" }
    "ci"      { "mvn clean install" }
    "cis"     { "mvn clean install -DskipTests" }
    "run"     {
        # Try to detect Spring Boot, otherwise use exec:java
        if (Test-Path "pom.xml") {
            $pomContent = Get-Content "pom.xml" -Raw
            if ($pomContent -match "spring-boot") {
                "mvn spring-boot:run"
            } else {
                "mvn exec:java"
            }
        } else {
            "mvn spring-boot:run"
        }
    }
    "tree"    { "mvn dependency:tree" }
    "update"  { "mvn clean install -U" }
    "help"    { Show-Help; exit 0 }
    default   { "mvn $Command" }
}

# Add any additional arguments
if ($MavenArgs) {
    $mavenCommand += " " + ($MavenArgs -join " ")
}

# Display the command being run
Write-Host "`nExecuting: " -NoNewline -ForegroundColor Green
Write-Host $mavenCommand -ForegroundColor White
Write-Host ("=" * 80) -ForegroundColor Gray
Write-Host ""

# Execute the Maven command
Invoke-Expression $mavenCommand

# Capture exit code
$exitCode = $LASTEXITCODE

if ($exitCode -eq 0) {
    Write-Host "`n✓ Maven command completed successfully" -ForegroundColor Green
} else {
    Write-Host "`n✗ Maven command failed with exit code: $exitCode" -ForegroundColor Red
}

exit $exitCode