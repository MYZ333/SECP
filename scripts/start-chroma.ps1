param(
    [string]$DataPath = (Join-Path $PSScriptRoot "..\data\rag\chroma"),
    # On this Windows setup, "localhost" binds Chroma to IPv6 (::1) only,
    # while the Spring AI client resolves it to IPv4. Bind explicitly to IPv4.
    [string]$HostName = "127.0.0.1",
    [int]$Port = 8000
)

$ErrorActionPreference = "Stop"
$resolvedDataPath = [System.IO.Path]::GetFullPath($DataPath)
New-Item -ItemType Directory -Path $resolvedDataPath -Force | Out-Null

$chromaCommand = Get-Command chroma -ErrorAction SilentlyContinue
if (-not $chromaCommand) {
    $knownPath = "D:\Chroma\venv\Scripts\chroma.exe"
    if (Test-Path -LiteralPath $knownPath) {
        $chromaExecutable = $knownPath
    } else {
        throw "Cannot find chroma. Run 'pip install chromadb' and add the Python Scripts directory to PATH."
    }
} else {
    $chromaExecutable = $chromaCommand.Source
}

Write-Host "Chroma data: $resolvedDataPath"
Write-Host "Chroma URL: http://${HostName}:$Port"
Write-Host "Keep this window running, then start Spring Boot in another terminal."

& $chromaExecutable run --path $resolvedDataPath --host $HostName --port $Port
