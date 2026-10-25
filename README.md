# AH-Fake-Warner für LabyMod 3

Nachbau des LabyMod-4-Addons [AH-Fake-Warner](https://github.com/DIEUNZ/signaturwaechter) für **LabyMod 3** (Minecraft 1.8.9).
Warnt im GrieferGames-Auktionshaus vor gefälschten Signaturen und gefälschten „Iced Out“-Items. Arbeitet nur lokal, es wird nichts an Server oder Dritte gesendet.

**Stand:** Etappe 4. Im Item-Tooltip erscheint eine rote Warnzeile bei gefälschter Signatur (Standard-Signierer `AbgegrieftHD`) und bei „Iced Out“-Items ohne Verzauberung; solche Items bekommen in Truhen und im Auktionshaus einen roten Rahmen.

**Einstellungen** (LabyMod-Einstellungen → Addons → AH-Fake-Warner): an/aus, vertrauenswürdige Signierer (durch Komma getrennt, Standard `AbgegrieftHD`), Ähnlichkeits-Schwelle 0–4 (Standard 2), Iced-Out-Prüfung, roter Rahmen. Gespeichert in `LabyMod/addons-1.8/config/AH-Fake-Warner.json`.

Tooltip und Slot-Rahmen werden per ASM-Transformer (`ItemStack.getTooltip`, `GuiContainer.drawSlot`) eingebaut, weil die LabyMod-3-API dafür keine Events hat.

## Installation

1. LabyMod 3 für Minecraft 1.8.9 installieren und einmal starten.
2. `ah-fake-warner-lm3.jar` in den Ordner `%APPDATA%\.minecraft\LabyMod\addons-1.8` legen (Ordner anlegen, falls er fehlt).
3. Minecraft mit dem Profil „LabyMod 3 (1.8.9)“ starten und einen Server oder eine Welt betreten.

Im Chat erscheint nur dann eine Meldung, wenn sich etwas nicht einbauen ließ.

**Update:** Alte Jar im Ordner `addons-1.8` durch die neue ersetzen (nur eine Jar des Addons darf dort liegen).

## Bauen

Benötigt ein JDK ab Version 9 (es wird für Java 8 kompiliert). LabyMod-3-API, LaunchWrapper, ASM und JUnit werden beim ersten Bauen nach `lib/` geladen; die Tests laufen automatisch mit (`-SkipTests` zum Überspringen).

```
powershell -ExecutionPolicy Bypass -File build.ps1
```

Ergebnis: `build/ah-fake-warner-lm3.jar`

## Lizenz

MIT
