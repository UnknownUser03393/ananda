param(
    [string]$SkiaImage = "build\text-comparison\skia-text.png",
    [string]$OutputImage = "build\text-comparison\skia-vs-gdiplus.png"
)

Add-Type -AssemblyName System.Drawing

$width = 920
$halfHeight = 320
$height = $halfHeight * 2

$outputDir = Split-Path -Parent $OutputImage
New-Item -ItemType Directory -Force $outputDir | Out-Null

$bitmap = New-Object System.Drawing.Bitmap $width, $height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::White)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit

$skia = [System.Drawing.Image]::FromFile((Resolve-Path $SkiaImage))
$graphics.DrawImage($skia, 0, 0, $width, $halfHeight)
$skia.Dispose()

$dividerPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(230, 230, 230)), 1
$graphics.DrawLine($dividerPen, 0, $halfHeight, $width, $halfHeight)

$brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(32, 38, 48))
$labelFont = New-Object System.Drawing.Font "Segoe UI", 18, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$titleFont = New-Object System.Drawing.Font "Segoe UI", 32, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$bodyFont = New-Object System.Drawing.Font "Segoe UI", 20, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$cnFont = New-Object System.Drawing.Font "Segoe UI", 24, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$smallFont = New-Object System.Drawing.Font "Segoe UI", 12, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$cnText = [string]::Concat(
    [char]0x4E2D, [char]0x6587, [char]0x5B57, [char]0x4F53,
    [char]0x6E32, [char]0x67D3, [char]0x5BF9, [char]0x6BD4,
    [char]0xFF1A,
    [char]0x6E05, [char]0x6670, [char]0x5EA6, [char]0x3001,
    [char]0x7070, [char]0x9636, [char]0x3001,
    [char]0x8FB9, [char]0x7F18
)
$smallText = [string]::Concat("12px: ABC xyz 123 ", [char]0x4E2D, [char]0x6587)

$y = $halfHeight
$graphics.DrawString("GDI+: ClearTypeGridFit", $labelFont, $brush, 32, ($y + 20))
$graphics.DrawString("Ananda Visual 1234567890", $titleFont, $brush, 32, ($y + 68))
$graphics.DrawString("The quick brown fox jumps over ClearType.", $bodyFont, $brush, 32, ($y + 126))
$graphics.DrawString($cnText, $cnFont, $brush, 32, ($y + 166))
$graphics.DrawString($smallText, $smallFont, $brush, 32, ($y + 228))

$bitmap.Save((Join-Path (Get-Location) $OutputImage), [System.Drawing.Imaging.ImageFormat]::Png)

$smallFont.Dispose()
$cnFont.Dispose()
$bodyFont.Dispose()
$titleFont.Dispose()
$labelFont.Dispose()
$brush.Dispose()
$dividerPen.Dispose()
$graphics.Dispose()
$bitmap.Dispose()

Write-Output (Resolve-Path $OutputImage)
