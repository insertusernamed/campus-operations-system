# Console colors
$ColorBackend  = "Cyan"
$ColorFrontend = "Green"
$ColorError    = "Red"
$ColorShutdown = "Yellow"

$BackendDir  = Get-ChildItem -Directory -Filter "*backend"  | Select-Object -First 1 -ExpandProperty FullName
$FrontendDir = Get-ChildItem -Directory -Filter "*frontend" | Select-Object -First 1 -ExpandProperty FullName

# Validate dependencies
if (-not (Get-Command mvn -ErrorAction SilentlyContinue) -and
    -not (Test-Path "$BackendDir\mvnw.cmd")) {
    Write-Host "Error: Maven/mvnw not found" -ForegroundColor $ColorError
    exit 1
}

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Host "Error: npm not found" -ForegroundColor $ColorError
    exit 1
}

if (-not $BackendDir -or -not $FrontendDir) {
    Write-Host "Error: backend/frontend directories not found" -ForegroundColor $ColorError
    exit 1
}

# Kill existing processes on dev ports
Write-Host "Killing any existing services on ports 5173 and 8080..."
foreach ($port in @(5173, 8080)) {
    $pids = netstat -ano | Select-String ":$port\s.*LISTENING" | ForEach-Object {
        ($_ -split '\s+')[-1]
    }
    foreach ($p in $pids) {
        if ($p) { taskkill /PID $p /F 2>$null | Out-Null }
    }
}

Write-Host "Starting services..."

# Start backend
$Backend = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "cd /d `"$BackendDir`" && mvnw.cmd spring-boot:run 2>&1" `
    -NoNewWindow -PassThru -RedirectStandardOutput "$env:TEMP\backend.log"

# Start frontend
$Frontend = Start-Process -FilePath "cmd.exe" `
    -ArgumentList "/c", "cd /d `"$FrontendDir`" && npm run dev 2>&1" `
    -NoNewWindow -PassThru -RedirectStandardOutput "$env:TEMP\frontend.log"

# Stream logs with prefixes in parallel
$BackendLog  = "$env:TEMP\backend.log"
$FrontendLog = "$env:TEMP\frontend.log"

$BackendStream  = [System.IO.File]::Open($BackendLog,  'OpenOrCreate', 'Read', 'ReadWrite')
$FrontendStream = [System.IO.File]::Open($FrontendLog, 'OpenOrCreate', 'Read', 'ReadWrite')
$BackendReader  = New-Object System.IO.StreamReader($BackendStream)
$FrontendReader = New-Object System.IO.StreamReader($FrontendStream)

try {
    while ($true) {
        # Read available backend lines
        while (-not $BackendReader.EndOfStream) {
            $line = $BackendReader.ReadLine()
            Write-Host "[BACKEND] $line" -ForegroundColor $ColorBackend
        }
        # Read available frontend lines
        while (-not $FrontendReader.EndOfStream) {
            $line = $FrontendReader.ReadLine()
            Write-Host "[FRONTEND] $line" -ForegroundColor $ColorFrontend
        }

        # Exit if either process has stopped
        if ($Backend.HasExited -or $Frontend.HasExited) { break }

        Start-Sleep -Milliseconds 100
    }
} finally {
    Write-Host "`nShutting down services..." -ForegroundColor $ColorShutdown

    $BackendReader.Close()
    $FrontendReader.Close()

    foreach ($proc in @($Backend, $Frontend)) {
        if ($proc -and -not $proc.HasExited) {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
    }

    Write-Host "Done." -ForegroundColor $ColorShutdown
}
