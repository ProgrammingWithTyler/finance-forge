# flyway.ps1 - Load .env and run flyway
param(
    [Parameter(Mandatory=$true)]
    [string]$Command
)

# Load .env file
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)\s*=\s*(.*)\s*$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        Set-Item -Path "env:$name" -Value $value
        Write-Host "Loaded $name"
    }
}

# Run flyway command
Write-Host "Running: mvn flyway:$Command"
mvn "flyway:$Command"