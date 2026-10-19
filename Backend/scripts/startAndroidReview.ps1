param(
    [string]$PythonPath = 'C:\Users\user\LPG_Capstone_jfsalivio\ML\venv\Scripts\python.exe'
)
$ErrorActionPreference = 'Stop'
$backendDirectory = Split-Path -Parent $PSScriptRoot
$projectDirectory = Split-Path -Parent $backendDirectory
$mlDirectory = Join-Path $projectDirectory 'ML'
$nodeExecutable = Join-Path $env:ProgramFiles 'nodejs\node.exe'
if (!(Test-Path -LiteralPath (Join-Path $backendDirectory '.env'))) { throw 'Backend/.env has not been configured.' }
if (!(Test-Path -LiteralPath $PythonPath)) { throw 'Supply -PythonPath with a Python environment containing ML/requirements.txt.' }
function Test-ReviewService([string]$Url) {
    try { return (Invoke-RestMethod -Uri $Url -TimeoutSec 2).status -eq 'ok' }
    catch { return $false }
}
if (!(Test-ReviewService 'http://127.0.0.1:4000/health')) {
    Start-Process -FilePath $nodeExecutable -ArgumentList 'server.js' -WorkingDirectory $backendDirectory -WindowStyle Hidden -RedirectStandardOutput (Join-Path $backendDirectory 'review-api.stdout.log') -RedirectStandardError (Join-Path $backendDirectory 'review-api.stderr.log') | Out-Null
}
if (!(Test-ReviewService 'http://127.0.0.1:5001/health')) {
    Start-Process -FilePath $PythonPath -ArgumentList '-u','serve.py' -WorkingDirectory $mlDirectory -WindowStyle Hidden -RedirectStandardOutput (Join-Path $mlDirectory 'review-ml.stdout.log') -RedirectStandardError (Join-Path $mlDirectory 'review-ml.stderr.log') | Out-Null
}
for ($attempt = 0; $attempt -lt 12; $attempt++) {
    if ((Test-ReviewService 'http://127.0.0.1:4000/health') -and (Test-ReviewService 'http://127.0.0.1:5001/health')) {
        Write-Output 'GasTrack API: http://localhost:4000; ML: http://localhost:5001; emulator API URL: http://10.0.2.2:4000/'
        exit 0
    }
    Start-Sleep -Milliseconds 500
}
throw 'A service did not start. Check Backend/review-api.stderr.log and ML/review-ml.stderr.log.'
