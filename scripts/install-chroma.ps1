param(
    [string]$PythonExecutable = "",
    [string]$RuntimePath = (Join-Path $PSScriptRoot ".chroma-venv")
)

$ErrorActionPreference = "Stop"
$RuntimePath = [System.IO.Path]::GetFullPath($RuntimePath)
$requirementsPath = Join-Path $PSScriptRoot "requirements-chroma.txt"

$pythonArguments = @()
if (-not $PythonExecutable) {
    $launcher = Get-Command py -ErrorAction SilentlyContinue
    if ($launcher) {
        # Prefer Python versions with the broadest Chroma wheel support.
        foreach ($version in @("3.12", "3.11", "3.10")) {
            $previousErrorPreference = $ErrorActionPreference
            $ErrorActionPreference = "SilentlyContinue"
            & $launcher.Source "-$version" -c "import sys; print(sys.executable)" *> $null
            $candidateExitCode = $LASTEXITCODE
            $ErrorActionPreference = $previousErrorPreference
            if ($candidateExitCode -eq 0) {
                $PythonExecutable = $launcher.Source
                $pythonArguments = @("-$version")
                break
            }
        }
    }
    if (-not $PythonExecutable) {
        foreach ($name in @("python", "python3", "py")) {
            $command = Get-Command $name -ErrorAction SilentlyContinue
            if ($command) {
                $PythonExecutable = $command.Source
                break
            }
        }
    }
}

if (-not $PythonExecutable) {
    throw "Python 3.9 or newer was not found. Install Python, then rerun this script."
}

Write-Host "Creating/updating isolated Chroma runtime: $RuntimePath"
$runtimePython = Join-Path $RuntimePath "Scripts\python.exe"
$runtimeIsUsable = $false
if (Test-Path -LiteralPath $runtimePython -PathType Leaf) {
    & $runtimePython -m pip --version *> $null
    $runtimeIsUsable = $LASTEXITCODE -eq 0
}
if (-not $runtimeIsUsable) {
    & $PythonExecutable @pythonArguments -m venv $RuntimePath
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create the Python virtual environment."
    }
}

$installed = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {
    & $runtimePython -m pip install --disable-pip-version-check --no-cache-dir --timeout 60 --retries 5 --requirement $requirementsPath
    if ($LASTEXITCODE -eq 0) {
        $installed = $true
        break
    }
    if ($attempt -lt 3) {
        Write-Warning "Chroma download failed (attempt $attempt/3); retrying the complete install."
    }
}
if (-not $installed) {
    throw "Failed to install Chroma from $requirementsPath."
}

$version = & $runtimePython -c "import chromadb; print(chromadb.__version__)"
Write-Host "Chroma $version is installed successfully." -ForegroundColor Green
Write-Host "Start it with: .\scripts\start-chroma.ps1"
