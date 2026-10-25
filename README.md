# AH-Fake-Warner für LabyMod 3

Nachbau des LabyMod-4-Addons [AH-Fake-Warner](https://github.com/DIEUNZ/signaturwaechter) für **LabyMod 3** (Minecraft 1.8.9).
Warnt im GrieferGames-Auktionshaus vor gefälschten Signaturen und gefälschten „Iced Out“-Items. Arbeitet nur lokal, es wird nichts an Server oder Dritte gesendet.

**Stand:** Etappe 5 (Erinnerung). Im Item-Tooltip erscheint eine rote Warnzeile bei gefälschter Signatur (Standard-Signierer `AbgegrieftHD`) und bei „Iced Out“-Items ohne Verzauberung; solche Items bekommen in Truhen und im Auktionshaus einen roten Rahmen.

**Auktions-Erinnerung:** Über einer Auktion `R` drücken – das Addon erinnert (Chat, Popup, Ton) kurz vor Ablauf; nochmal `R` entfernt die Erinnerung. Der Tooltip schlägt das vor. Im Auktionshaus werden Slots mit Timer blau und mit eigenem Gebot grün eingefärbt (beides: gestreift). `/aht` listet die Erinnerungen, `/aht clear` löscht sie (nur lokal, nichts geht an den Server).

**Einstellungen** (LabyMod-Einstellungen → Addons → AH-Fake-Warner): an/aus, Sprache der Meldungen (Deutsch, Englisch oder wie das Spiel), vertrauenswürdige Signierer (durch Komma getrennt, Standard `AbgegrieftHD`), Ähnlichkeits-Schwelle 0–4 (Standard 2), Iced-Out-Prüfung, roter Rahmen, einmalige Chat-Warnung (Standard aus), Warnton beim ersten Hovern über eine Fälschung (Standard an); Erinnerung an/aus, Taste, Minuten vor Ablauf (Standard 2), Vorschlag im Tooltip; Slot-Farben für Timer und eigenes Gebot. Gespeichert in `LabyMod/addons-1.8/config/AH-Fake-Warner.json`.

Tooltip, Slot-Rahmen und Timer-Taste werden per ASM-Transformer (`ItemStack.getTooltip`, `GuiContainer.drawSlot`, `GuiContainer.keyTyped`) eingebaut, weil die LabyMod-3-API dafür keine Events hat.

## Installation

1. LabyMod 3 für Minecraft 1.8.9 installieren und einmal starten.
2. Die neueste `ah-fake-warner-lm3.jar` unter [Releases](https://github.com/DIEUNZ/signaturwaechter-laby3/releases/latest) herunterladen und in den Ordner `%APPDATA%\.minecraft\LabyMod\addons-1.8` legen (Ordner anlegen, falls er fehlt).
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
