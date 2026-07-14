param(
    [string]$HostName = "127.0.0.1",
    [ValidateRange(1, 65535)]
    [int]$Port = 8000
)

$ErrorActionPreference = "Stop"
$startScript = Join-Path $PSScriptRoot "start-chroma.ps1"

& $startScript -BindAddress $HostName -Port $Port -Background
Write-Host "Chroma is running." -ForegroundColor Green
Write-Host "Start Spring Boot to create the collections, then import and publish seed knowledge from the admin knowledge page."
