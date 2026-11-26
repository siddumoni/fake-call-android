$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root "app/src/main/java"
$matches = Get-ChildItem $source -Recurse -Filter *.kt |
    Where-Object { $_.FullName -notlike "*ui/theme/Theme.kt" } |
    Select-String -Pattern '\bColor\s*\(|\bColor\.[A-Za-z_]+'
if ($matches) {
    $matches | ForEach-Object { Write-Error $_ }
    throw "Direct Compose color values are forbidden outside the central theme."
}
Write-Output "UI color-token policy passed."
