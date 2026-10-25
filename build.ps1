# Baut das Addon ohne Gradle: javac (Ziel Java 8) gegen die LabyMod-3-API-Jar.
# Aufruf: powershell -ExecutionPolicy Bypass -File build.ps1 [-SkipTests]  (unter Linux: pwsh build.ps1)
param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$outJar = 'build/ah-fake-warner-lm3.jar'

# Abhängigkeiten nur zum Kompilieren/Testen; LabyMod bringt sie zur Laufzeit selbst mit
$libs = [ordered]@{
    'lib/lm_api_mc1.8.9.jar'   = 'https://dl.labymod.net/latest/api/files/lm_api_mc1.8.9.jar'
    'lib/launchwrapper-1.12.jar' = 'https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar'
    'lib/asm-debug-all-5.0.3.jar' = 'https://repo1.maven.org/maven2/org/ow2/asm/asm-debug-all/5.0.3/asm-debug-all-5.0.3.jar'
    'lib/lwjgl-2.9.4-nightly-20150209.jar' = 'https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar'
    'lib/gson-2.2.4.jar' = 'https://libraries.minecraft.net/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar'
    'lib/junit-platform-console-standalone-1.11.4.jar' = 'https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar'
}
New-Item -ItemType Directory -Force lib | Out-Null
foreach ($entry in $libs.GetEnumerator()) {
    if (-not (Test-Path $entry.Key)) {
        Write-Host "Lade $($entry.Value)"
        Invoke-WebRequest -Uri $entry.Value -OutFile $entry.Key -UseBasicParsing
    }
}
# Klassenpfad-Trenner ist unter Windows ';' und unter Linux ':'
$sep = [IO.Path]::PathSeparator
$compileCp = @('lib/lm_api_mc1.8.9.jar', 'lib/launchwrapper-1.12.jar', 'lib/asm-debug-all-5.0.3.jar',
    'lib/lwjgl-2.9.4-nightly-20150209.jar', 'lib/gson-2.2.4.jar') -join $sep
$junit = 'lib/junit-platform-console-standalone-1.11.4.jar'

# Sauberer Neubau
if (Test-Path build) { Remove-Item -Recurse -Force build }
New-Item -ItemType Directory -Force build/classes | Out-Null

# Platzhalter für Minecraft-Klassen, die javac nur zur Überladungsauflösung braucht (nicht in der Jar)
New-Item -ItemType Directory -Force build/stubs | Out-Null
$stubs = Get-ChildItem -Recurse -Filter *.java src/stubs/java | ForEach-Object { $_.FullName }
& javac --release 8 -encoding UTF-8 -nowarn -Xlint:-options -d build/stubs $stubs
if ($LASTEXITCODE -ne 0) { throw 'Platzhalter kompilieren fehlgeschlagen' }

$sources = Get-ChildItem -Recurse -Filter *.java src/main/java | ForEach-Object { $_.FullName }
& javac --release 8 -encoding UTF-8 -nowarn -Xlint:-options -cp "$compileCp${sep}build/stubs" -d build/classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Kompilieren fehlgeschlagen' }

# Tests der Erkennungslogik (ohne Minecraft)
if (-not $SkipTests) {
    New-Item -ItemType Directory -Force build/test-classes | Out-Null
    $testSources = Get-ChildItem -Recurse -Filter *.java src/test/java | ForEach-Object { $_.FullName }
    & javac --release 8 -encoding UTF-8 -nowarn -Xlint:-options -cp "build/classes${sep}$junit${sep}$compileCp" -d build/test-classes $testSources
    if ($LASTEXITCODE -ne 0) { throw 'Tests kompilieren fehlgeschlagen' }
    & java -jar $junit execute --class-path "build/classes${sep}build/test-classes${sep}$compileCp" --scan-class-path build/test-classes --disable-banner --details=summary
    if ($LASTEXITCODE -ne 0) { throw 'Tests fehlgeschlagen' }
}

Copy-Item -Recurse src/main/resources/* build/classes/
& jar --create --file $outJar -C build/classes .
if ($LASTEXITCODE -ne 0) { throw 'Jar bauen fehlgeschlagen' }

Write-Host "Fertig: $outJar"
