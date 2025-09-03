<#
.SYNOPSIS
    Run Flyway migrations through Maven with environment variables loaded from .env.
.PARAMETER Command
    The Flyway command to run (e.g., migrate, info, clean, validate).
.PARAMETER Debug
    Optional switch to enable Maven debug output.
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$Command,

    [switch]$EnableDebug
)


# -----------------------------
# Load .env file
# -----------------------------
$envFile = Join-Path $PSScriptRoot "../../.env"
if (-not (Test-Path $envFile)) {
    throw "Cannot find .env file at $envFile"
}

$envVars = @{ }
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)\s*=\s*(.*)\s*$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim('"').Trim()  # strip quotes and whitespace
        $envVars[$name] = $value
        Write-Host "Loaded $name"
    }
}

# -----------------------------
# Validate required variables
# -----------------------------
$requiredVars = @('DB_URL','DB_USERNAME','DB_PASSWORD','DB_SCHEMA')
foreach ($var in $requiredVars) {
    if (-not $envVars.ContainsKey($var)) {
        throw "Missing required .env variable: $var"
    }
}

# -----------------------------
# Prepare Maven command
# -----------------------------
$mvnCmd = "mvn"

# Use DB_URL from .env directly (no replacement)
$flywayUrl = $envVars['DB_URL']

# Build Maven arguments
$mvnArgs = @("flyway:$Command")

if ($EnableDebug) { $mvnArgs += "-X" }

$mvnArgs += @(
    "-Dflyway.url=$flywayUrl",
    "-Dflyway.user=$($envVars['DB_USERNAME'])",
    "-Dflyway.password=$($envVars['DB_PASSWORD'])",
    "-Dflyway.schemas=$($envVars['DB_SCHEMA'])"
)

# -----------------------------
# Show command and execute
# -----------------------------
Write-Host "Running Maven command:"
Write-Host "$mvnCmd $($mvnArgs -join ' ')"
Write-Host ""

$process = Start-Process $mvnCmd -ArgumentList $mvnArgs -NoNewWindow -Wait -PassThru

if ($process.ExitCode -ne 0) {
    throw "Maven command failed with exit code $($process.ExitCode)"
}