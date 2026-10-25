package de.bjarne.ahfakewarner.reminder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReminderServiceTest {

  private static final long MINUTE = 60_000;
  private static final long NOW = 1_800_000_000_000L;
  private static final int KEY_R = 19;

  private static final List<String> AUCTION = Arrays.asList(
      "Hauptgewinn-Ticket (#0403)",
      "Signiert von AbgegrieftHD am 24.10.2026",
      "$ | Aktuelles Gebot:",
      "114.780.500$",
      "Verbleibende Zeit:",
      "12 Minuten");

  private ReminderService service() {
    ReminderService service = new ReminderService();
    service.configure(true, KEY_R, "R", 2, true);
    return service;
  }

  @Test
  void hinweisSchlaegtTasteVor() {
    List<String> hint = service().onTooltip(AUCTION, NOW);
    assertEquals(2, hint.size());
    assertEquals("", hint.get(0));
    assertTrue(hint.get(1).contains("[R] Erinnern 2 Min. vor Ende (endet ca. "), hint.get(1));
  }

  @Test
  void tasteSetztUndEntferntErinnerung() {
    ReminderService service = service();
    service.onTooltip(AUCTION, NOW);
    String set = service.onKey(KEY_R, NOW + 50);
    assertTrue(set.startsWith("Erinnerung gesetzt: \"Hauptgewinn-Ticket (#0403)\" endet um "), set);

    // Tooltip zeigt jetzt die gesetzte Erinnerung
    assertTrue(service.onTooltip(AUCTION, NOW + 100).get(1).contains("[R] zum Entfernen"));

    // Eine Sekunde später, Restzeit gerundet gleich: dieselbe Auktion wird entfernt
    service.onTooltip(AUCTION, NOW + 1000);
    assertEquals("Erinnerung für \"Hauptgewinn-Ticket (#0403)\" entfernt.",
        service.onKey(KEY_R, NOW + 1050));
  }

  @Test
  void andereTasteOderKeinHoverTutNichts() {
    ReminderService service = service();
    service.onTooltip(AUCTION, NOW);
    assertNull(service.onKey(20, NOW + 10));
    // Tooltip älter als 250 ms gilt als nicht mehr gehovert
    assertNull(service.onKey(KEY_R, NOW + 1000));
  }

  @Test
  void itemOhneRestzeit() {
    ReminderService service = service();
    service.onTooltip(Arrays.asList("Stein"), NOW);
    assertEquals("Keine Restzeit im Tooltip von \"Stein\" gefunden.", service.onKey(KEY_R, NOW));
  }

  @Test
  void erinnerungWirdZweiMinutenVorEndeFaellig() {
    ReminderService service = service();
    service.onTooltip(AUCTION, NOW);
    service.onKey(KEY_R, NOW);
    assertTrue(service.pollDue(NOW + 9 * MINUTE).isEmpty());
    List<String> due = service.pollDue(NOW + 10 * MINUTE);
    assertEquals(Arrays.asList(
        "» Auktion \"Hauptgewinn-Ticket (#0403)\" endet in 2 Min. (Gebot 114.780.500$)"), due);
    // nur einmal
    assertTrue(service.pollDue(NOW + 11 * MINUTE).isEmpty());
  }

  @Test
  void ausgeschaltetVerwirftFaelligeStill() {
    ReminderService service = service();
    service.onTooltip(AUCTION, NOW);
    service.onKey(KEY_R, NOW);
    service.configure(false, KEY_R, "R", 2, true);
    assertTrue(service.pollDue(NOW + 11 * MINUTE).isEmpty());
    service.configure(true, KEY_R, "R", 2, true);
    assertTrue(service.pollDue(NOW + 12 * MINUTE).isEmpty());
  }

  @Test
  void befehlListetUndLoescht() {
    ReminderService service = service();
    assertNull(service.onCommand("/ah", true));
    assertNull(service.onCommand("/ahtest", true));
    assertEquals(Arrays.asList("Keine Erinnerungen aktiv."), service.onCommand("/aht", true));

    service.onTooltip(AUCTION, NOW);
    service.onKey(KEY_R, NOW);
    assertTrue(service.onCommand("/aht list", true).get(0)
        .startsWith("- Hauptgewinn-Ticket (#0403) - endet "));
    assertEquals(Arrays.asList("1 Erinnerung(en) gelöscht."), service.onCommand("/AHT clear", true));
    assertEquals(Arrays.asList("Benutzung: /aht list | /aht clear"),
        service.onCommand("/aht foo", true));
    assertEquals(Arrays.asList("Das Addon ist deaktiviert."), service.onCommand("/aht", false));
  }

  @Test
  void keinHinweisOhneTasteOderAbgeschaltet() {
    ReminderService service = new ReminderService();
    service.configure(true, ReminderService.NO_KEY, "", 2, true);
    assertTrue(service.onTooltip(AUCTION, NOW).isEmpty());
    service.configure(true, KEY_R, "R", 2, false);
    assertTrue(service.onTooltip(AUCTION, NOW).isEmpty());
  }

  @Test
  void endetGleichUndBeiAblauf() {
    ReminderService service = service();
    List<String> soon = Arrays.asList("Kopf", "Aktuelles Gebot: 5$", "Verbleibende Zeit: 1 Minute");
    assertTrue(service.onTooltip(soon, NOW).get(1).contains("Endet gleich (ca. "));
    service.configure(true, KEY_R, "R", 0, true);
    assertTrue(service.onTooltip(AUCTION, NOW).get(1).contains("[R] Erinnern bei Ablauf"));
  }
}
