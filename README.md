# AH-Fake-Warner für LabyMod 3

Nachbau des LabyMod-4-Addons [AH-Fake-Warner](https://github.com/DIEUNZ/signaturwaechter) für **LabyMod 3** (Minecraft 1.8.9).
Warnt im GrieferGames-Auktionshaus vor gefälschten Signaturen und gefälschten „Iced Out“-Items. Arbeitet nur lokal, es wird nichts an Server oder Dritte gesendet.

**Stand:** Etappe 1 (Spike). Das Addon lädt nur und schreibt beim Betreten einer Welt eine Begrüßung in den Chat.

## Installation

1. LabyMod 3 für Minecraft 1.8.9 installieren und einmal starten.
2. `ah-fake-warner-lm3.jar` in den Ordner `%APPDATA%\.minecraft\LabyMod\addons-1.8` legen (Ordner anlegen, falls er fehlt).
3. Minecraft mit dem Profil „LabyMod 3 (1.8.9)“ starten und einen Server oder eine Welt betreten.

Im Chat erscheint: `[AH-Fake-Warner] Hallo! Das Addon läuft in LabyMod 3.`

## Bauen

Benötigt ein JDK ab Version 9 (es wird für Java 8 kompiliert). Die LabyMod-3-API wird beim ersten Bauen nach `lib/` geladen.

```
powershell -ExecutionPolicy Bypass -File build.ps1
```

Ergebnis: `build/ah-fake-warner-lm3.jar`

## Lizenz

MIT
