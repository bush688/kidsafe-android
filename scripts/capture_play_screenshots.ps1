$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$outDir = Join-Path $root "store-assets\\screenshots\\raw"
if (-not (Test-Path -LiteralPath $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }

function Require-Adb {
  $adb = Get-Command adb -ErrorAction SilentlyContinue
  if (-not $adb) { throw "未找到 adb，请先安装 Android SDK Platform Tools 并把 adb 加入 PATH" }
}

function Capture([string]$name) {
  $ts = Get-Date -Format "yyyyMMdd_HHmmss"
  $file = Join-Path $outDir ("{0}_{1}.png" -f $ts, $name)
  adb exec-out screencap -p > $file
  Write-Host "Saved: $file"
}

Require-Adb

Write-Host "请先在手机/模拟器中打开 app，并手动切到要截图的页面，然后按提示回车采集。"
Write-Host "输出目录：$outDir"

Read-Host "回车采集：主页（功能导航）" | Out-Null
Capture "home"

Read-Host "回车采集：热电偶页" | Out-Null
Capture "thermocouple"

Read-Host "回车采集：热电阻页" | Out-Null
Capture "rtd"

Read-Host "回车采集：探头页" | Out-Null
Capture "probe"

Read-Host "回车采集：差压液位计页" | Out-Null
Capture "dp_level"

Read-Host "回车采集：电缆线阻/压降页" | Out-Null
Capture "cable_drop"

Write-Host "完成。建议从 raw 目录筛选 3-5 张最佳截图，放到 screenshots/phone 与 screenshots/tablet。"

