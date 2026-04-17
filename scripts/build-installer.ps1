param(
    [switch]$SkipTests,
    [string]$DownloadBaseUrl = "https://seudominio.com.br/downloads",
    [string]$ReleaseNotes = "Correcoes de bugs e melhorias gerais."
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$wixDir = Join-Path $repoRoot ".build-tools\wix314"
$wixZip = Join-Path $repoRoot ".build-tools\wix314-binaries.zip"
$wixUrl = "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip"

Set-Location $repoRoot

if (-not (Test-Path (Join-Path $wixDir "candle.exe")) -or -not (Test-Path (Join-Path $wixDir "light.exe"))) {
    Write-Host "Baixando WiX Toolset 3.14.1 portatil..."
    New-Item -ItemType Directory -Force -Path (Split-Path $wixZip) | Out-Null
    Invoke-WebRequest -Uri $wixUrl -OutFile $wixZip

    if (Test-Path $wixDir) {
        Remove-Item -Recurse -Force $wixDir
    }

    New-Item -ItemType Directory -Force -Path $wixDir | Out-Null
    Expand-Archive -Path $wixZip -DestinationPath $wixDir -Force
}

$env:PATH = "$wixDir;$env:PATH"

if ($SkipTests) {
    mvn clean package -DskipTests
} else {
    mvn clean package
}

mvn jpackage:jpackage -DskipTests

$exe = Get-ChildItem (Join-Path $repoRoot "target\dist") -Filter "*.exe" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $exe) {
    throw "Instalador .exe nao encontrado em target\dist."
}

$projectVersion = ([xml](Get-Content (Join-Path $repoRoot "pom.xml"))).project.version
$hash = (Get-FileHash -Path $exe.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
$baseUrl = $DownloadBaseUrl.TrimEnd("/")
$downloadUrl = "$baseUrl/$([Uri]::EscapeDataString($exe.Name))"

$updateManifest = [ordered]@{
    latestVersion = $projectVersion
    downloadUrl = $downloadUrl
    sha256 = $hash
    releaseNotes = $ReleaseNotes
    mandatory = $false
    publishedAt = (Get-Date -Format "yyyy-MM-dd")
}

$updateJsonPath = Join-Path $repoRoot "target\dist\update.json"
$updateManifest | ConvertTo-Json | Set-Content -Path $updateJsonPath -Encoding UTF8

Write-Host ""
Write-Host "Instalador gerado em:"
$exe | Select-Object FullName, Length, LastWriteTime

Write-Host ""
Write-Host "Manifest de atualizacao gerado em:"
Get-Item $updateJsonPath | Select-Object FullName, Length, LastWriteTime
