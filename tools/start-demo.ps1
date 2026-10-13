# Run from PowerShell: .\tools\start-demo.ps1
param([switch]$Repack)
$ErrorActionPreference = 'Stop'
$demoCredential = Get-Credential -Message 'Enter the same verified demo account used in the Android app'
if ($null -eq $demoCredential) { return }
try {
    $env:FLOOD_EMAIL = $demoCredential.UserName
    $env:FLOOD_PASSWORD = $demoCredential.GetNetworkCredential().Password
    if ($Repack) { node "$PSScriptRoot/demo-feed.mjs" --repack }
    else { node "$PSScriptRoot/demo-feed.mjs" }
} finally {
    Remove-Item Env:FLOOD_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:FLOOD_PASSWORD -ErrorAction SilentlyContinue
    $demoCredential = $null
}
