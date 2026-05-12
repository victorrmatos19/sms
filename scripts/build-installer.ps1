# ============================================================
# build-installer.ps1
# Gera o instalador .exe + update-manifest.json do SMS.
#
# Uso:
#   .\scripts\build-installer.ps1
#   .\scripts\build-installer.ps1 -ReleaseNotes "Descricao da versao" -Mandatory $true
#
# O script:
#   1. Le a versao atual do pom.xml
#   2. Garante que WiX esta no PATH
#   3. Compila e empacota com Maven + jpackage
#   4. Calcula SHA256 do .exe gerado
#   5. Grava target\dist\update-manifest.json
# ============================================================
param(
    [string]$ReleaseNotes = "",
    [bool]$Mandatory = $false
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot

# 1. Ler versao do pom.xml
Write-Host ""
Write-Host "==> Lendo versao do pom.xml..."
[xml]$pom = Get-Content "$projectRoot\pom.xml" -Encoding UTF8
$version = $pom.project.version
Write-Host "    Versao: $version"

# 2. Garantir WiX no PATH
$wixDir = "$env:LOCALAPPDATA\wix314"
if (Test-Path "$wixDir\candle.exe") {
    if ($env:PATH -notlike "*wix314*") {
        $env:PATH = "$env:PATH;$wixDir"
        Write-Host "==> WiX adicionado ao PATH da sessao."
    }
} else {
    Write-Host ""
    Write-Host "[AVISO] WiX nao encontrado em $wixDir."
    exit 1
}

# 3. Build Maven + jpackage
Write-Host ""
Write-Host "==> Compilando e empacotando (Maven + jpackage)..."
Push-Location $projectRoot
try {
    mvn clean package -DskipTests jpackage:jpackage
    if ($LASTEXITCODE -ne 0) { throw "Maven falhou com codigo $LASTEXITCODE" }
} finally {
    Pop-Location
}
Write-Host "==> Build concluido."

# 4. Localizar o .exe gerado
$distDir = "$projectRoot\target\dist"
$exeFile = Get-ChildItem $distDir -Filter "*.exe" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $exeFile) { throw "Nenhum .exe encontrado em $distDir" }
Write-Host ""
Write-Host "==> Instalador: $($exeFile.Name)  ($([math]::Round($exeFile.Length/1MB,1)) MB)"

# 5. SHA256
Write-Host "==> Calculando SHA256..."
$sha256 = (Get-FileHash $exeFile.FullName -Algorithm SHA256).Hash.ToLower()
Write-Host "    $sha256"

# 6. Nome do asset no GitHub (espacos -> pontos)
$assetName = $exeFile.Name -replace " ", "."
$downloadUrl = "https://github.com/victorrmatos19/sms/releases/download/v$version/$assetName"

# 7. Release notes default
if (-not $ReleaseNotes) {
    $ReleaseNotes = "Versao $version do SMS - Simple Manage System."
}

# 8. Gerar update-manifest.json
$today = Get-Date -Format "yyyy-MM-dd"
$manifest = "{`n  `"latestVersion`": `"$version`",`n  `"downloadUrl`": `"$downloadUrl`",`n  `"sha256`": `"$sha256`",`n  `"releaseNotes`": `"$ReleaseNotes`",`n  `"mandatory`": $($Mandatory.ToString().ToLower()),`n  `"publishedAt`": `"$today`"`n}"
$manifestPath = "$distDir\update-manifest.json"
[System.IO.File]::WriteAllText($manifestPath, $manifest, [System.Text.Encoding]::UTF8)

Write-Host ""
Write-Host "==> update-manifest.json:"
Write-Host $manifest
Write-Host ""
Write-Host "==> Arquivos em target\dist\:"
Get-ChildItem $distDir | ForEach-Object { Write-Host "    $($_.Name)  ($([math]::Round($_.Length/1MB,1)) MB)" }
Write-Host ""
Write-Host "==> Concluido!"