param(
    [string]$DataPath = (Join-Path $PSScriptRoot "..\data\rag\chroma"),
    [string]$BindAddress = "127.0.0.1",
    [string]$ChromaExecutable = "",
    [ValidateRange(1, 65535)]
    [int]$Port = 8000,
    [switch]$Background,
    [ValidateRange(1, 120)]
    [int]$WaitSeconds = 30
)

# Use IPv4 by default because localhost can resolve to IPv6 only on some Windows systems.
# ChromaExecutable is normally auto-detected; pass it only when discovery fails.

$ErrorActionPreference = "Stop"
$resolvedDataPath = [System.IO.Path]::GetFullPath($DataPath)
$runtimePath = Join-Path $PSScriptRoot ".chroma-venv"
$runtimeExecutable = Join-Path $runtimePath "Scripts\chroma.exe"
$pidPath = Join-Path $resolvedDataPath "chroma.pid"
$stdoutPath = Join-Path $resolvedDataPath "chroma.stdout.log"
$stderrPath = Join-Path $resolvedDataPath "chroma.stderr.log"
$heartbeatUrl = "http://${BindAddress}:$Port/api/v2/heartbeat"
New-Item -ItemType Directory -Path $resolvedDataPath -Force | Out-Null


function Test-ChromaReady {
    try {
        $response = Invoke-RestMethod -Uri $heartbeatUrl -Method Get -TimeoutSec 2
        $version = Invoke-RestMethod -Uri "http://${BindAddress}:$Port/api/v2/version" -Method Get -TimeoutSec 2
        return $null -ne $response.PSObject.Properties["nanosecond heartbeat"].Value -and $null -ne $version
    } catch {
        return $false
    }
}

function Find-ChromaExecutable {
    # Prefer the project runtime created by install-chroma.ps1.
    if (Test-Path -LiteralPath $runtimeExecutable -PathType Leaf) {
        return $runtimeExecutable
    }

    $command = Get-Command chroma -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    return $null
}

if ($ChromaExecutable) {
    $ChromaExecutable = [System.IO.Path]::GetFullPath($ChromaExecutable)
    if (-not (Test-Path -LiteralPath $ChromaExecutable -PathType Leaf)) {
        throw "The specified Chroma executable does not exist: $ChromaExecutable"

    }
} else {
    $ChromaExecutable = Find-ChromaExecutable
}

if (-not $ChromaExecutable) {
    $requirementsPath = Join-Path $PSScriptRoot "requirements-chroma.txt"
    throw @"
Cannot find Chroma in PATH or the available Python environments.
Install the project-pinned version, then run this script again:
  & "$PSScriptRoot\install-chroma.ps1"
You can also pass -ChromaExecutable with the full path to chroma.exe.
"@
}

if (Test-ChromaReady) {
    Write-Host "Chroma is already ready: $heartbeatUrl" -ForegroundColor Green
    return
}

$listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    throw "Port $Port is already occupied by PID $($listener.OwningProcess), but it is not a healthy Chroma v2 server."
}

Write-Host "Chroma data: $resolvedDataPath"
Write-Host "Chroma URL: http://${BindAddress}:$Port"
Write-Host "Chroma executable: $ChromaExecutable"

$arguments = @("run", "--path", $resolvedDataPath, "--host", $BindAddress, "--port", $Port)

if ($Background) {
    $process = Start-Process -FilePath $ChromaExecutable -ArgumentList $arguments -PassThru `
        -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath -WindowStyle Hidden
    Set-Content -LiteralPath $pidPath -Value $process.Id -Encoding ascii

    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($process.HasExited) {
            Remove-Item -LiteralPath $pidPath -ErrorAction SilentlyContinue
            throw "Chroma exited before becoming ready (code $($process.ExitCode)). See $stderrPath"
        }
        if (Test-ChromaReady) {
            Write-Host "Chroma is ready (PID $($process.Id)): $heartbeatUrl" -ForegroundColor Green
            return
        }
        Start-Sleep -Milliseconds 500
    }
    Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $pidPath -ErrorAction SilentlyContinue
    throw "Chroma did not become ready within $WaitSeconds seconds. See $stderrPath"
}

Write-Host "Keep this window running, then start Spring Boot in another terminal."

& $ChromaExecutable @arguments
if ($LASTEXITCODE -ne 0) {
    throw "Chroma exited with code $LASTEXITCODE."
}
