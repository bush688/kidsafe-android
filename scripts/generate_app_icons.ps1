param(
  [string]$OutRoot = (Join-Path $PSScriptRoot "..\\store-assets\\app-icons"),
  [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function Ensure-Dir([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path)) { New-Item -ItemType Directory -Path $Path | Out-Null }
}

function Color([string]$Hex) {
  return [System.Drawing.ColorTranslator]::FromHtml($Hex)
}

function New-RoundedRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
  $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
  $d = $r * 2.0
  $path.AddArc($x, $y, $d, $d, 180.0, 90.0)
  $path.AddArc($x + $w - $d, $y, $d, $d, 270.0, 90.0)
  $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0.0, 90.0)
  $path.AddArc($x, $y + $h - $d, $d, $d, 90.0, 90.0)
  $path.CloseFigure()
  return $path
}

function New-Canvas([int]$Size) {
  $bmp = [System.Drawing.Bitmap]::new($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
  $g.Clear([System.Drawing.Color]::Transparent)
  return @{ Bitmap = $bmp; Graphics = $g }
}

function Draw-OptionA($g, [int]$Size) {
  $pad = [float]($Size * 0.08)
  $r = [float]($Size * 0.22)
  $x = $pad
  $y = $pad
  $w = [float]($Size - 2 * $pad)
  $h = $w

  $bg1 = Color "#0EA5A5"
  $bg2 = Color "#0B1220"
  $accent = Color "#F59E0B"
  $white = Color "#FFFFFF"

  $path = New-RoundedRectPath $x $y $w $h $r
  $brush = [System.Drawing.Drawing2D.LinearGradientBrush]::new([System.Drawing.RectangleF]::new($x, $y, $w, $h), $bg1, $bg2, 90.0)
  $g.FillPath($brush, $path)
  $brush.Dispose()
  $path.Dispose()

  $cx = [float]($Size / 2.0)
  $cy = [float]($Size / 2.0)
  $dialR = [float]($Size * 0.28)
  $stroke = [float]([Math]::Max(2.0, $Size * 0.06))
  $pen = [System.Drawing.Pen]::new($white, $stroke)
  $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  $g.DrawArc($pen, $cx - $dialR, $cy - $dialR, $dialR * 2.0, $dialR * 2.0, 210.0, 240.0)
  $pen.Dispose()

  $needlePen = [System.Drawing.Pen]::new($accent, [float]([Math]::Max(2.0, $Size * 0.05)))
  $needlePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $needlePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  $ang = [float](-35.0 * [Math]::PI / 180.0)
  $nx = $cx + [float]([Math]::Cos($ang) * $dialR * 0.95)
  $ny = $cy + [float]([Math]::Sin($ang) * $dialR * 0.95)
  $g.DrawLine($needlePen, $cx, $cy, $nx, $ny)
  $needlePen.Dispose()

  $dotR = [float]([Math]::Max(2.0, $Size * 0.045))
  $dotBrush = [System.Drawing.SolidBrush]::new($white)
  $g.FillEllipse($dotBrush, $cx - $dotR, $cy - $dotR, $dotR * 2.0, $dotR * 2.0)
  $dotBrush.Dispose()
}

function Draw-OptionB($g, [int]$Size) {
  $bg = Color "#1D4ED8"
  $bg2 = Color "#0B1220"
  $accent = Color "#22C55E"
  $white = Color "#FFFFFF"

  $pad = [float]($Size * 0.08)
  $x = $pad
  $y = $pad
  $w = [float]($Size - 2 * $pad)
  $h = $w
  $r = [float]($Size * 0.24)

  $path = New-RoundedRectPath $x $y $w $h $r
  $brush = [System.Drawing.Drawing2D.LinearGradientBrush]::new([System.Drawing.RectangleF]::new($x, $y, $w, $h), $bg, $bg2, 45.0)
  $g.FillPath($brush, $path)
  $brush.Dispose()
  $path.Dispose()

  $cx = [float]($Size / 2.0)
  $cy = [float]($Size / 2.0)
  $pen = [System.Drawing.Pen]::new($white, [float]([Math]::Max(2.0, $Size * 0.055)))
  $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  $g.DrawLine($pen, $cx - $Size * 0.18, $cy - $Size * 0.06, $cx + $Size * 0.18, $cy - $Size * 0.06)
  $g.DrawLine($pen, $cx - $Size * 0.22, $cy + $Size * 0.02, $cx + $Size * 0.22, $cy + $Size * 0.02)
  $g.DrawLine($pen, $cx - $Size * 0.18, $cy + $Size * 0.10, $cx + $Size * 0.18, $cy + $Size * 0.10)
  $pen.Dispose()

  $accentPen = [System.Drawing.Pen]::new($accent, [float]([Math]::Max(2.0, $Size * 0.06)))
  $accentPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $accentPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  $g.DrawArc($accentPen, $cx - $Size * 0.20, $cy - $Size * 0.20, $Size * 0.40, $Size * 0.40, 220.0, 100.0)
  $accentPen.Dispose()
}

function Draw-OptionC($g, [int]$Size) {
  $bg1 = Color "#111827"
  $bg2 = Color "#0EA5A5"
  $accent = Color "#F97316"
  $white = Color "#FFFFFF"

  $pad = [float]($Size * 0.08)
  $x = $pad
  $y = $pad
  $w = [float]($Size - 2 * $pad)
  $h = $w
  $r = [float]($Size * 0.18)

  $path = New-RoundedRectPath $x $y $w $h $r
  $brush = [System.Drawing.Drawing2D.LinearGradientBrush]::new([System.Drawing.RectangleF]::new($x, $y, $w, $h), $bg1, $bg2, 135.0)
  $g.FillPath($brush, $path)
  $brush.Dispose()
  $path.Dispose()

  $pen = [System.Drawing.Pen]::new($white, [float]([Math]::Max(2.0, $Size * 0.055)))
  $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round

  $cx = [float]($Size / 2.0)
  $cy = [float]($Size / 2.0)
  $g.DrawEllipse($pen, $cx - $Size * 0.22, $cy - $Size * 0.22, $Size * 0.44, $Size * 0.44)
  $pen.Dispose()

  $bolt = [System.Drawing.Drawing2D.GraphicsPath]::new()
  $bolt.AddPolygon([System.Drawing.PointF[]]@(
    [System.Drawing.PointF]::new($cx - $Size * 0.06, $cy - $Size * 0.22),
    [System.Drawing.PointF]::new($cx + $Size * 0.02, $cy - $Size * 0.22),
    [System.Drawing.PointF]::new($cx - $Size * 0.02, $cy - $Size * 0.02),
    [System.Drawing.PointF]::new($cx + $Size * 0.08, $cy - $Size * 0.02),
    [System.Drawing.PointF]::new($cx - $Size * 0.03, $cy + $Size * 0.24),
    [System.Drawing.PointF]::new($cx - $Size * 0.01, $cy + $Size * 0.02),
    [System.Drawing.PointF]::new($cx - $Size * 0.10, $cy + $Size * 0.02)
  ))
  $boltBrush = [System.Drawing.SolidBrush]::new($accent)
  $g.FillPath($boltBrush, $bolt)
  $boltBrush.Dispose()
  $bolt.Dispose()
}

function Render([string]$OptionName, [int]$Size) {
  $c = New-Canvas $Size
  $bmp = $c.Bitmap
  $g = $c.Graphics
  if ($OptionName -eq "option-a") { Draw-OptionA $g $Size }
  elseif ($OptionName -eq "option-b") { Draw-OptionB $g $Size }
  elseif ($OptionName -eq "option-c") { Draw-OptionC $g $Size }
  else { throw "Unknown option: $OptionName" }
  $g.Dispose()
  return $bmp
}

function Png-Bytes([System.Drawing.Bitmap]$Bmp) {
  $ms = [System.IO.MemoryStream]::new()
  $Bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
  $bytes = $ms.ToArray()
  $ms.Dispose()
  return $bytes
}

function Write-Ico([string]$Path, [hashtable]$PngBySize) {
  $sizes = @($PngBySize.Keys | Sort-Object)
  $count = $sizes.Count

  $ms = [System.IO.MemoryStream]::new()
  $bw = [System.IO.BinaryWriter]::new($ms)
  $bw.Write([uint16]0)
  $bw.Write([uint16]1)
  $bw.Write([uint16]$count)

  $offset = 6 + 16 * $count
  foreach ($s in $sizes) {
    $data = $PngBySize[$s]
    $w = if ($s -ge 256) { [byte]0 } else { [byte]$s }
    $h = if ($s -ge 256) { [byte]0 } else { [byte]$s }
    $bw.Write($w)
    $bw.Write($h)
    $bw.Write([byte]0)
    $bw.Write([byte]0)
    $bw.Write([uint16]1)
    $bw.Write([uint16]32)
    $bw.Write([uint32]$data.Length)
    $bw.Write([uint32]$offset)
    $offset += $data.Length
  }

  foreach ($s in $sizes) {
    $bw.Write($PngBySize[$s])
  }

  $bw.Flush()
  [System.IO.File]::WriteAllBytes($Path, $ms.ToArray())
  $bw.Dispose()
  $ms.Dispose()
}

function Be-Int32([int]$Value) {
  $b = [BitConverter]::GetBytes([int]$Value)
  [Array]::Reverse($b)
  return $b
}

function Write-Icns([string]$Path, [hashtable]$PngBySize) {
  $map = @{
    16 = "icp4"
    32 = "icp5"
    64 = "icp6"
    128 = "ic07"
    256 = "ic08"
    512 = "ic09"
    1024 = "ic10"
  }

  $chunks = New-Object System.Collections.Generic.List[object]
  foreach ($k in $map.Keys) {
    if ($PngBySize.ContainsKey($k)) {
      $chunks.Add(@{ Type = $map[$k]; Data = $PngBySize[$k] })
    }
  }

  $total = 8
  foreach ($c in $chunks) { $total += 8 + $c.Data.Length }

  $ms = [System.IO.MemoryStream]::new()
  $bw = [System.IO.BinaryWriter]::new($ms)
  $bw.Write([System.Text.Encoding]::ASCII.GetBytes("icns"))
  $bw.Write((Be-Int32 $total))
  foreach ($c in $chunks) {
    $bw.Write([System.Text.Encoding]::ASCII.GetBytes($c.Type))
    $bw.Write((Be-Int32 (8 + $c.Data.Length)))
    $bw.Write($c.Data)
  }
  $bw.Flush()
  [System.IO.File]::WriteAllBytes($Path, $ms.ToArray())
  $bw.Dispose()
  $ms.Dispose()
}

function Write-Svg([string]$Path, [string]$OptionName) {
  if ($OptionName -eq "option-a") {
    $svg = @"
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#0EA5A5"/>
      <stop offset="100%" stop-color="#0B1220"/>
    </linearGradient>
  </defs>
  <rect x="82" y="82" width="860" height="860" rx="210" fill="url(#bg)"/>
  <path d="M340 632a260 260 0 1 1 344 0" fill="none" stroke="#FFFFFF" stroke-width="64" stroke-linecap="round"/>
  <path d="M512 512 L744 380" fill="none" stroke="#F59E0B" stroke-width="56" stroke-linecap="round"/>
  <circle cx="512" cy="512" r="44" fill="#FFFFFF"/>
</svg>
"@
  } elseif ($OptionName -eq "option-b") {
    $svg = @"
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#1D4ED8"/>
      <stop offset="100%" stop-color="#0B1220"/>
    </linearGradient>
  </defs>
  <rect x="82" y="82" width="860" height="860" rx="240" fill="url(#bg)"/>
  <path d="M300 460h424M260 512h504M300 564h424" fill="none" stroke="#FFFFFF" stroke-width="64" stroke-linecap="round"/>
  <path d="M332 608a240 240 0 0 0 360 0" fill="none" stroke="#22C55E" stroke-width="72" stroke-linecap="round"/>
</svg>
"@
  } elseif ($OptionName -eq "option-c") {
    $svg = @"
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#111827"/>
      <stop offset="100%" stop-color="#0EA5A5"/>
    </linearGradient>
  </defs>
  <rect x="82" y="82" width="860" height="860" rx="180" fill="url(#bg)"/>
  <circle cx="512" cy="512" r="240" fill="none" stroke="#FFFFFF" stroke-width="64"/>
  <path d="M470 292h72l-40 230h92L474 760l28-212h-92z" fill="#F97316"/>
</svg>
"@
  } else {
    throw "Unknown option: $OptionName"
  }

  [System.IO.File]::WriteAllText($Path, $svg, [System.Text.Encoding]::UTF8)
}

$sizesPng = @(16, 24, 32, 48, 64, 96, 128, 256, 512, 1024)
$sizesIcns = @(16, 32, 64, 128, 256, 512, 1024)

$options = @("option-a", "option-b", "option-c")
foreach ($opt in $options) {
  $dir = Join-Path $OutRoot $opt
  if ((Test-Path -LiteralPath $dir) -and (-not $Force)) {
    Write-Host "Skip existing: $dir (use -Force to overwrite)"
    continue
  }

  Ensure-Dir $dir
  Ensure-Dir (Join-Path $dir "png")

  Write-Svg (Join-Path $dir "icon.svg") $opt

  foreach ($s in $sizesPng) {
    $bmp = Render $opt $s
    $outPng = Join-Path $dir ("png\\icon-$s.png")
    $bmp.Save($outPng, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
  }

  $icoPng = @{}
  foreach ($s in @(16, 24, 32, 48, 64, 128, 256)) {
    $bmp = Render $opt $s
    $icoPng[$s] = Png-Bytes $bmp
    $bmp.Dispose()
  }
  Write-Ico (Join-Path $dir "app.ico") $icoPng

  $icnsPng = @{}
  foreach ($s in $sizesIcns) {
    $bmp = Render $opt $s
    $icnsPng[$s] = Png-Bytes $bmp
    $bmp.Dispose()
  }
  Write-Icns (Join-Path $dir "app.icns") $icnsPng
}

Write-Host "Done. Output: $OutRoot"
