$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function Ensure-Dir([string]$path) {
  if (-not (Test-Path -LiteralPath $path)) {
    New-Item -ItemType Directory -Path $path | Out-Null
  }
}

function Save-Png([System.Drawing.Bitmap]$bmp, [string]$path) {
  $dir = Split-Path -Parent $path
  Ensure-Dir $dir
  if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Force }
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
}

function New-GradientBrush([int]$w, [int]$h, [System.Drawing.Color]$c1, [System.Drawing.Color]$c2) {
  $rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
  return New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, $c1, $c2, 45.0)
}

function Draw-RoundedRect([System.Drawing.Graphics]$g, [System.Drawing.Pen]$pen, [System.Drawing.Brush]$brush, [int]$x, [int]$y, [int]$w, [int]$h, [int]$r) {
  $path = New-Object System.Drawing.Drawing2D.GraphicsPath
  $diam = $r * 2
  $path.AddArc($x, $y, $diam, $diam, 180, 90) | Out-Null
  $path.AddArc($x + $w - $diam, $y, $diam, $diam, 270, 90) | Out-Null
  $path.AddArc($x + $w - $diam, $y + $h - $diam, $diam, $diam, 0, 90) | Out-Null
  $path.AddArc($x, $y + $h - $diam, $diam, $diam, 90, 90) | Out-Null
  $path.CloseFigure() | Out-Null
  if ($brush -ne $null) { $g.FillPath($brush, $path) }
  if ($pen -ne $null) { $g.DrawPath($pen, $path) }
  $path.Dispose()
}

function Generate-PlayIcon([string]$outPath) {
  $w = 512
  $h = 512
  $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

  $bg = New-GradientBrush $w $h ([System.Drawing.Color]::FromArgb(255, 12, 74, 110)) ([System.Drawing.Color]::FromArgb(255, 18, 140, 126))
  $g.FillRectangle($bg, 0, 0, $w, $h)
  $bg.Dispose()

  $pad = 64
  $cardW = $w - $pad * 2
  $cardH = $h - $pad * 2
  $cardBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(235, 255, 255, 255))
  Draw-RoundedRect $g $null $cardBrush $pad $pad $cardW $cardH 56
  $cardBrush.Dispose()

  $strokePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(110, 0, 0, 0), 6)
  Draw-RoundedRect $g $strokePen $null ($pad + 6) ($pad + 6) ($cardW - 12) ($cardH - 12) 50
  $strokePen.Dispose()

  $iconPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 12, 74, 110), 20)
  $iconPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $iconPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round

  $cx = [int]($w / 2)
  $top = $pad + 96
  $calcW = 220
  $calcH = 260
  $x0 = $cx - [int]($calcW / 2)
  $y0 = $top

  $calcBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 232, 246, 245))
  Draw-RoundedRect $g $null $calcBrush $x0 $y0 $calcW $calcH 40
  $calcBrush.Dispose()

  $g.DrawLine($iconPen, $x0 + 28, $y0 + 72, $x0 + $calcW - 28, $y0 + 72)
  $g.DrawLine($iconPen, $x0 + 28, $y0 + 120, $x0 + $calcW - 28, $y0 + 120)

  $dotBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 18, 140, 126))
  $r = 16
  $gridX = @([int]($x0 + 56), [int]($x0 + 110), [int]($x0 + 164))
  $gridY = @([int]($y0 + 156), [int]($y0 + 210))
  foreach ($yy in $gridY) {
    foreach ($xx in $gridX) {
      $g.FillEllipse($dotBrush, $xx - $r, $yy - $r, $r * 2, $r * 2)
    }
  }
  $dotBrush.Dispose()

  $bolt = New-Object System.Drawing.Drawing2D.GraphicsPath
  $bolt.StartFigure() | Out-Null
  $bolt.AddPolygon([System.Drawing.Point[]]@(
    [System.Drawing.Point]::new([int]($cx + 86), [int]($y0 + 26)),
    [System.Drawing.Point]::new([int]($cx + 34), [int]($y0 + 140)),
    [System.Drawing.Point]::new([int]($cx + 92), [int]($y0 + 140)),
    [System.Drawing.Point]::new([int]($cx + 22), [int]($y0 + 272)),
    [System.Drawing.Point]::new([int]($cx + 136), [int]($y0 + 140)),
    [System.Drawing.Point]::new([int]($cx + 78), [int]($y0 + 140))
  )) | Out-Null
  $boltBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 193, 7))
  $g.FillPath($boltBrush, $bolt)
  $boltBrush.Dispose()
  $bolt.Dispose()

  $iconPen.Dispose()

  Save-Png $bmp $outPath
  $g.Dispose()
  $bmp.Dispose()
}

function Generate-FeatureGraphic([string]$outPath) {
  $w = 1024
  $h = 500
  $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

  $bg = New-GradientBrush $w $h ([System.Drawing.Color]::FromArgb(255, 12, 74, 110)) ([System.Drawing.Color]::FromArgb(255, 18, 140, 126))
  $g.FillRectangle($bg, 0, 0, $w, $h)
  $bg.Dispose()

  $titleFont = New-Object System.Drawing.Font("Segoe UI", 54, [System.Drawing.FontStyle]::Bold)
  $subFont = New-Object System.Drawing.Font("Segoe UI", 24, [System.Drawing.FontStyle]::Regular)
  $white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
  $muted = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230, 235, 245, 245))

  $g.DrawString("Instrument Calc", $titleFont, $white, 54, 56)
  $g.DrawString("Thermocouple/RTD  -  Probe  -  DP Level  -  Cable (DC/AC)", $subFont, $muted, 58, 140)

  $pillBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(180, 255, 255, 255))
  $pillPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(90, 0, 0, 0), 2)
  $pillX = 58
  $pillY = 210
  $pillW = 620
  $pillH = 60
  Draw-RoundedRect $g $pillPen $pillBrush $pillX $pillY $pillW $pillH 30
  $pillBrush.Dispose()
  $pillPen.Dispose()

  $tagFont = New-Object System.Drawing.Font("Segoe UI", 24, [System.Drawing.FontStyle]::Bold)
  $dark = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 12, 74, 110))
  $g.DrawString("Offline  -  Fast  -  Field-ready", $tagFont, $dark, $pillX + 22, $pillY + 12)

  $dark.Dispose()
  $tagFont.Dispose()
  $titleFont.Dispose()
  $subFont.Dispose()
  $white.Dispose()
  $muted.Dispose()

  Generate-PlayIcon (Join-Path ([IO.Path]::GetTempPath()) "tmp_play_icon.png")
  $icon = [System.Drawing.Image]::FromFile((Join-Path ([IO.Path]::GetTempPath()) "tmp_play_icon.png"))
  $g.DrawImage($icon, 760, 80, 200, 200)
  $icon.Dispose()
  Remove-Item -LiteralPath (Join-Path ([IO.Path]::GetTempPath()) "tmp_play_icon.png") -Force -ErrorAction SilentlyContinue

  Save-Png $bmp $outPath
  $g.Dispose()
  $bmp.Dispose()
}

function Generate-PlaceholderScreenshot([string]$outPath, [int]$w, [int]$h, [string]$title, [string[]]$bullets) {
  $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

  $bg = New-GradientBrush $w $h ([System.Drawing.Color]::FromArgb(255, 10, 40, 60)) ([System.Drawing.Color]::FromArgb(255, 18, 140, 126))
  $g.FillRectangle($bg, 0, 0, $w, $h)
  $bg.Dispose()

  $cardPad = [int]($w * 0.06)
  $cardX = $cardPad
  $cardY = [int]($h * 0.12)
  $cardW = $w - $cardPad * 2
  $cardH = $h - $cardY - $cardPad
  $cardBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(235, 255, 255, 255))
  Draw-RoundedRect $g $null $cardBrush $cardX $cardY $cardW $cardH 48
  $cardBrush.Dispose()

  $tFont = New-Object System.Drawing.Font("Segoe UI", [float]($w * 0.05), [System.Drawing.FontStyle]::Bold)
  $bFont = New-Object System.Drawing.Font("Segoe UI", [float]($w * 0.035), [System.Drawing.FontStyle]::Regular)
  $dark = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 12, 74, 110))
  $muted = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 50, 70, 80))

  $g.DrawString("Instrument Calc", $tFont, $dark, $cardX + 40, $cardY + 36)
  $g.DrawString($title, $bFont, $muted, $cardX + 40, $cardY + 120)

  $y = $cardY + 200
  foreach ($b in $bullets) {
    $g.DrawString("* " + $b, $bFont, $muted, $cardX + 50, $y)
    $y += [int]($w * 0.055)
  }

  $noteFont = New-Object System.Drawing.Font("Segoe UI", [float]($w * 0.028), [System.Drawing.FontStyle]::Italic)
  $noteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(180, 0, 0, 0))
  $g.DrawString("Note: replace template with real app screenshots", $noteFont, $noteBrush, $cardX + 40, $cardY + $cardH - 90)

  $noteBrush.Dispose()
  $noteFont.Dispose()
  $tFont.Dispose()
  $bFont.Dispose()
  $dark.Dispose()
  $muted.Dispose()

  Save-Png $bmp $outPath
  $g.Dispose()
  $bmp.Dispose()
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$assets = Join-Path $root "store-assets"

Ensure-Dir (Join-Path $assets "icon")
Ensure-Dir (Join-Path $assets "feature-graphic")
Ensure-Dir (Join-Path $assets "screenshots\\phone")
Ensure-Dir (Join-Path $assets "screenshots\\tablet")

Generate-PlayIcon (Join-Path $assets "icon\\play-icon-512.png")
Generate-FeatureGraphic (Join-Path $assets "feature-graphic\\feature-graphic-1024x500.png")

Generate-PlaceholderScreenshot (Join-Path $assets "screenshots\\phone\\01-home.png") 1080 1920 "Home" @("Thermocouple/RTD","Probe","DP Level","Cable (DC/AC)")
Generate-PlaceholderScreenshot (Join-Path $assets "screenshots\\phone\\02-thermocouple.png") 1080 1920 "Thermocouple (ITS-90)" @("Temp <-> mV","Cold junction compensation","Clear unit hints")
Generate-PlaceholderScreenshot (Join-Path $assets "screenshots\\phone\\03-rtd.png") 1080 1920 "RTD" @("Temp <-> Ohm","2/3/4-wire compensation","Common Pt/Cu types")
Generate-PlaceholderScreenshot (Join-Path $assets "screenshots\\phone\\04-dp-level.png") 1080 1920 "DP Level" @("Zero shift","Range check","Unit conversions")
Generate-PlaceholderScreenshot (Join-Path $assets "screenshots\\phone\\05-cable.png") 1080 1920 "Cable" @("DC/AC models","PF / Frequency / Inductance","Report export")

Generate-PlaceholderScreenshot (Join-Path $assets "screenshots\\tablet\\01-home.png") 1920 1200 "Tablet: Home" @("Bigger screen","Offline ready")
Generate-PlaceholderScreenshot (Join-Path $assets "screenshots\\tablet\\02-cable.png") 1920 1200 "Tablet: Cable" @("DC/AC models","Clear key values")

Write-Host "Generated Play Store assets under: $assets"
