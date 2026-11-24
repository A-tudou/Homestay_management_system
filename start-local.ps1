param([switch]$NoBrowser)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

function Test-Port {
    param([int]$Port, [int]$TimeoutMs = 800)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if (-not $iar.AsyncWaitHandle.WaitOne($TimeoutMs)) { return $false }
        $client.EndConnect($iar)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Wait-Http {
    param([string]$Url, [int]$TimeoutSeconds)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3 | Out-Null
            return $true
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

Write-Host '=========================================='
Write-Host ' 山居小筑 · 本地一键启动'
Write-Host '=========================================='

if (-not (Test-Port 3306)) {
    Write-Host '[!] 未检测到 MySQL（3306），请先启动 MySQL 服务后重试。' -ForegroundColor Yellow
    exit 1
}
Write-Host '[ok] MySQL 3306 已就绪'

$backendDir = Join-Path $root 'homestay-booking'
$frontendDir = Join-Path $root 'homestay-web'

if (Test-Port 8080) {
    Write-Host '[skip] 后端已在运行：http://localhost:8080'
} else {
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    $jar = Join-Path $backendDir 'target\homestay-booking-0.0.1-SNAPSHOT.jar'
    if ($mvn) {
        Start-Process powershell -ArgumentList '-NoExit', '-Command', 'mvn spring-boot:run' -WorkingDirectory $backendDir
    } elseif (Test-Path $jar) {
        Start-Process powershell -ArgumentList '-NoExit', '-Command', "java -jar '$jar'" -WorkingDirectory $backendDir
    } else {
        Write-Host '[!] 未找到 Maven，也没有已打包的 jar，无法启动后端。' -ForegroundColor Red
        exit 1
    }
    Write-Host '[..] 正在启动后端，首次编译可能需要一些时间...'
    if (Wait-Http 'http://localhost:8080/api/public/room-types' 180) {
        Write-Host '[ok] 后端已就绪：http://localhost:8080'
    } else {
        Write-Host '[!] 后端启动超时，请查看后端窗口日志。' -ForegroundColor Yellow
    }
}

if (Test-Port 5173) {
    Write-Host '[skip] 前端已在运行：http://localhost:5173'
} else {
    $frontCommand = 'npm run dev'
    if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
        Write-Host '[..] 首次运行，先安装前端依赖...'
        $frontCommand = 'npm install; npm run dev'
    }
    Start-Process powershell -ArgumentList '-NoExit', '-Command', $frontCommand -WorkingDirectory $frontendDir
    if (Wait-Http 'http://localhost:5173/' 120) {
        Write-Host '[ok] 前端已就绪：http://localhost:5173'
    } else {
        Write-Host '[!] 前端启动超时，请查看前端窗口日志。' -ForegroundColor Yellow
    }
}

Write-Host ''
Write-Host '房客端：http://localhost:5173/'
Write-Host '管理端：http://localhost:5173/admin  （admin / admin123）'
Write-Host '接口文档：http://localhost:8080/swagger-ui/index.html'
Write-Host '提示：关闭弹出的后端 / 前端窗口即可停止对应服务。'

if (-not $NoBrowser) {
    Start-Process 'http://localhost:5173'
}
