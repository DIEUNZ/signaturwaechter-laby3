# Baut das Addon ohne Gradle: javac (Ziel Java 8) gegen die LabyMod-3-API-Jar.
# Aufruf: powershell -ExecutionPolicy Bypass -File build.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$apiUrl = 'https://dl.labymod.net/latest/api/files/lm_api_mc1.8.9.jar'
$apiJar = 'lib/lm_api_mc1.8.9.jar'
$outJar = 'build/ah-fake-warner-lm3.jar'

# API-Jar einmalig herunterladen (wird nicht eingecheckt)
if (-not (Test-Path $apiJar)) {
    New-Item -ItemType Directory -Force lib | Out-Null
    Write-Host "Lade $apiUrl"
    Invoke-WebRequest -Uri $apiUrl -OutFile $apiJar -UseBasicParsing
}

# Sauberer Neubau
if (Test-Path build) { Remove-Item -Recurse -Force build }
New-Item -ItemType Directory -Force build/classes | Out-Null

$sources = Get-ChildItem -Recurse -Filter *.java src/main/java | ForEach-Object { $_.FullName }
& javac --release 8 -encoding UTF-8 -nowarn -Xlint:-options -cp $apiJar -d build/classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Kompilieren fehlgeschlagen' }

Copy-Item -Recurse src/main/resources/* build/classes/
& jar --create --file $outJar -C build/classes .
if ($LASTEXITCODE -ne 0) { throw 'Jar bauen fehlgeschlagen' }

Write-Host "Fertig: $outJar"
