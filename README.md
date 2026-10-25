# AH-Fake-Warner für LabyMod 3

Nachbau des LabyMod-4-Addons [AH-Fake-Warner](https://github.com/DIEUNZ/signaturwaechter) für **LabyMod 3** (Minecraft 1.8.9).
Warnt im GrieferGames-Auktionshaus vor gefälschten Signaturen und gefälschten „Iced Out“-Items. Arbeitet nur lokal, es wird nichts an Server oder Dritte gesendet.

**Stand:** Etappe 2. Im Item-Tooltip erscheint eine rote Warnzeile bei gefälschter Signatur (Standard-Signierer `AbgegrieftHD`) und bei „Iced Out“-Items ohne Verzauberung. Einstellungen gibt es noch nicht.

Der Tooltip wird per ASM-Transformer in `ItemStack.getTooltip` ergänzt, weil die LabyMod-3-API kein Tooltip-Event hat.

## Installation

1. LabyMod 3 für Minecraft 1.8.9 installieren und einmal starten.
2. `ah-fake-warner-lm3.jar` in den Ordner `%APPDATA%\.minecraft\LabyMod\addons-1.8` legen (Ordner anlegen, falls er fehlt).
3. Minecraft mit dem Profil „LabyMod 3 (1.8.9)“ starten und einen Server oder eine Welt betreten.

Im Chat erscheint: `[AH-Fake-Warner] Tooltip-Warnung aktiv.`

**Update:** Alte Jar im Ordner `addons-1.8` durch die neue ersetzen (nur eine Jar des Addons darf dort liegen).

## Bauen

Benötigt ein JDK ab Version 9 (es wird für Java 8 kompiliert). LabyMod-3-API, LaunchWrapper, ASM und JUnit werden beim ersten Bauen nach `lib/` geladen; die Tests laufen automatisch mit (`-SkipTests` zum Überspringen).

```
powershell -ExecutionPolicy Bypass -File build.ps1
```

Ergebnis: `build/ah-fake-warner-lm3.jar`

## Lizenz

MIT
