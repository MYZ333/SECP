param(
    [string]$DataPath = (Join-Path $PSScriptRoot "..\data\rag\chroma"),
    [ValidateRange(1, 60)]
    [int]$WaitSeconds = 10
)

$ErrorActionPreference = "Stop"
$resolvedDataPath = [System.IO.Path]::GetFullPath($DataPath)
$pidPath = Join-Path $resolvedDataPath "chroma.pid"

if (-not (Test-Path -LiteralPath $pidPath -PathType Leaf)) {
    Write-Host "No managed Chroma PID file was found. It may be stopped or running in a foreground terminal."
    return
}

$processId = [int](Get-Content -LiteralPath $pidPath -Raw)
$process = Get-Process -Id $processId -ErrorAction SilentlyContinue
if (-not $process) {
    Remove-Item -LiteralPath $pidPath
    Write-Host "Removed stale PID file; Chroma was not running."
    return
}

Stop-Process -Id $processId
$deadline = (Get-Date).AddSeconds($WaitSeconds)
while ((Get-Date) -lt $deadline) {
    if (-not (Get-Process -Id $processId -ErrorAction SilentlyContinue)) {
        break
    }
    Start-Sleep -Milliseconds 200
}
if (Get-Process -Id $processId -ErrorAction SilentlyContinue) {
    throw "Chroma PID $processId did not stop within $WaitSeconds seconds."
}
Remove-Item -LiteralPath $pidPath
Write-Host "Chroma PID $processId stopped." -ForegroundColor Green
