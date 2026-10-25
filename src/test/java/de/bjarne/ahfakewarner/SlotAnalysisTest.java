package de.bjarne.ahfakewarner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import de.bjarne.ahfakewarner.reminder.ReminderService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlotAnalysisTest {

  private static final long NOW = 1_800_000_000_000L;

  private static final TooltipWarner WARNER = new TooltipWarner(
      Collections.singletonList("AbgegrieftHD"), 2, SignatureExtractor.DEFAULT_PATTERN, true);

  // Überbotenes Gebot aus dem Auktionshaus (mit Farbcodes wie aus getTooltip)
  private static final List<String> OUTBID = Arrays.asList(
      "§fApple (#0260)",
      "§5§o",
      "§5§o§dAnalplug",
      "§5§o§7Signiert von §aArinji §7am §e19.12.2020",
      "§5§o§6$ §8| §7Aktuelles Gebot:",
      "§5§o§63$",
      "§5§o§6$ §8| §7Dein Gebotslimit:",
      "§5§o§62$",
      "§5§o§cDu wurdest überboten!",
      "§5§o§7Verbleibende Zeit:",
      "§5§o§a104 Minuten");

  @Test
  void gebotslimitZaehltAlsEigenesGebot() {
    SlotAnalysis analysis = SlotAnalysis.of(OUTBID, false, WARNER, "Spieler", NOW);
    assertTrue(analysis.ownBid());
    assertFalse(analysis.isWarning());
    assertEquals(NOW + 104 * 60_000L, analysis.endsAtEstimate());
  }

  @Test
  void ohneGebotKeineMarkierung() {
    List<String> lines = Arrays.asList("Kopf", "Aktuelles Gebot:", "5$", "Verbleibende Zeit:",
        "3 Minuten");
    assertFalse(SlotAnalysis.of(lines, false, WARNER, "Spieler", NOW).ownBid());
  }

  @Test
  void keineAuktionKeinSchluessel() {
    SlotAnalysis analysis = SlotAnalysis.of(Arrays.asList("Stein"), false, WARNER, null, NOW);
    assertNull(analysis.auctionKey());
    assertEquals(-1, analysis.endsAtEstimate());
  }

  @Test
  void fakeOhneWarnerNichtRot() {
    List<String> lines = Arrays.asList("Perk", "Signiert von AbgegriefHD am 01.06.2026");
    assertTrue(SlotAnalysis.of(lines, false, WARNER, null, NOW).isWarning());
    assertFalse(SlotAnalysis.of(lines, false, null, null, NOW).isWarning());
  }

  @Test
  void timerFarbeFindetGesetzteErinnerung() {
    ReminderService reminders = new ReminderService();
    reminders.configure(true, 19, "R", 2, true);
    List<String> plain = Arrays.asList("Apple (#0260)", "Aktuelles Gebot:", "3$",
        "Verbleibende Zeit:", "104 Minuten");
    reminders.onTooltip(plain, NOW);
    reminders.onKey(19, NOW);

    // Slot-Analyse ein paar Sekunden später mit Farbcodes
    SlotAnalysis analysis = SlotAnalysis.of(Arrays.asList("§fApple (#0260)",
        "§7Aktuelles Gebot:", "§63$", "§7Verbleibende Zeit:", "§a104 Minuten"),
        false, WARNER, null, NOW + 3000);
    assertTrue(reminders.hasReminder(analysis.auctionKey(), analysis.endsAtEstimate()));

    reminders.configure(false, 19, "R", 2, true);
    assertFalse(reminders.hasReminder(analysis.auctionKey(), analysis.endsAtEstimate()));
  }

  @Test
  void timerFarbeAuchMitErweitertenTooltips() {
    // Hover-Tooltip mit F3+H (Item-ID am Namen), Slot-Analyse ohne
    ReminderService reminders = new ReminderService();
    reminders.configure(true, 19, "R", 2, true);
    reminders.onTooltip(Arrays.asList("Jhins Geburtstagsböller (#0401)", "Aktuelles Gebot:",
        "57.000.000$", "Verbleibende Zeit:", "12 Minuten", "minecraft:fireworks",
        "NBT: 4 tag(s)"), NOW);
    assertTrue(reminders.onKey(19, NOW).startsWith("Erinnerung gesetzt: \"Jhins Geburtstagsböller\""));

    SlotAnalysis analysis = SlotAnalysis.of(Arrays.asList("§cJhins Geburtstagsböller",
        "§7Aktuelles Gebot:", "§657.000.000$", "§7Verbleibende Zeit:", "§a12 Minuten"),
        false, WARNER, null, NOW + 2000);
    assertTrue(reminders.hasReminder(analysis.auctionKey(), analysis.endsAtEstimate()));
  }
}
