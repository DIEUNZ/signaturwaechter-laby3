package de.bjarne.ahfakewarner.reminder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuctionTooltipParserTest {

  private static final List<String> TOOLTIP = Arrays.asList(
      "+1 Clan-Home (Item)",
      "Signiert von AbgegrieftHD am 01.06.2026",
      "$ | Aktuelles Gebot:",
      "55.556$",
      "⌚ | Verbleibende Zeit:",
      "4 Minuten",
      "Klicke, um die Auktion zu öffnen.");

  @Test
  void zeitAusFolgezeile() {
    assertEquals(Duration.ofMinutes(4), AuctionTooltipParser.parseRemaining(TOOLTIP).get());
  }

  @Test
  void gebotAusFolgezeile() {
    assertEquals("55.556$", AuctionTooltipParser.parseBid(TOOLTIP).get());
  }

  @Test
  void zeitInDerselbenZeile() {
    assertEquals(Duration.ofMinutes(15),
        AuctionTooltipParser.parseRemaining(Arrays.asList("Verbleibende Zeit: §a15 Minuten")).get());
  }

  @Test
  void verschiedeneEinheiten() {
    assertEquals(Duration.ofMinutes(1), AuctionTooltipParser.parseDuration("1 Minute").get());
    assertEquals(Duration.ofHours(2).plusMinutes(5),
        AuctionTooltipParser.parseDuration("2 Stunden 5 Minuten").get());
    assertEquals(Duration.ofSeconds(30), AuctionTooltipParser.parseDuration("30 Sekunden").get());
    assertEquals(Duration.ofDays(1).plusHours(3),
        AuctionTooltipParser.parseDuration("1 Tag 3 Std").get());
    assertEquals(Duration.ofHours(1).plusMinutes(2).plusSeconds(3),
        AuctionTooltipParser.parseDuration("1h 2m 3s").get());
  }

  @Test
  void kennungBleibtBeiAblaufenderZeitGleich() {
    List<String> later = Arrays.asList(
        "+1 Clan-Home (Item)",
        "Signiert von AbgegrieftHD am 01.06.2026",
        "$ | Aktuelles Gebot:",
        "60.000$",
        "⌚ | Verbleibende Zeit:",
        "3 Minuten");
    assertEquals(AuctionTooltipParser.stableKey(TOOLTIP), AuctionTooltipParser.stableKey(later));
    assertEquals("+1 Clan-Home (Item)\nSigniert von AbgegrieftHD am 01.06.2026\n",
        AuctionTooltipParser.stableKey(TOOLTIP));
  }

  @Test
  void auktionErkennen() {
    assertTrue(AuctionTooltipParser.isAuction(TOOLTIP));
    assertTrue(!AuctionTooltipParser.isAuction(Arrays.asList("Stein")));
  }

  @Test
  void eigenesGebotErkennen() {
    List<String> withBid = Arrays.asList(
        "+1 Clan-Home (Item)",
        "Signiert von DIEUNZ am 01.06.2026",
        "$ | Aktuelles Gebot:",
        "60.000$",
        "Höchstbietender: §aDIEUNZ");
    assertTrue(AuctionTooltipParser.mentionsPlayerInAuction(withBid, "dieunz"));
    // Name nur in der Signatur → kein Gebot
    assertTrue(!AuctionTooltipParser.mentionsPlayerInAuction(withBid.subList(0, 4), "DIEUNZ"));
    // Teilwort zählt nicht
    assertTrue(!AuctionTooltipParser.mentionsPlayerInAuction(
        Arrays.asList("Aktuelles Gebot: 5$", "Höchstbietender: DIEUNZ_2"), "DIEUNZ"));
    assertTrue(!AuctionTooltipParser.mentionsPlayerInAuction(TOOLTIP, ""));
  }

  @Test
  void eigenesGebotOhneNamenErkennen() {
    // Echter GG-Tooltip: der Spielername steht nirgends
    List<String> lavaBucket = Arrays.asList(
        "Magischer Lavaeimer",
        "Mit diesem Eimer kannst du 100 Blöcke",
        "Lava platzieren!",
        "",
        "Verbleibende Haltbarkeit: 100",
        "$ | Aktuelles Gebot:",
        "1$",
        "$ | Dein Gebotslimit:",
        "12$",
        "✓ Du bist aktuell Höchstbietender!",
        "⌚ | Verbleibende Zeit:",
        "85 Minuten");
    assertTrue(AuctionTooltipParser.hasOwnBid(lavaBucket, "DIEUNZ"));
    // Nur Gebotslimit (z. B. überboten) zählt ebenfalls als "geboten"
    assertTrue(AuctionTooltipParser.hasOwnBid(Arrays.asList("Aktuelles Gebot:", "5$",
        "Dein Gebotslimit:", "9$", "Verbleibende Zeit:", "3 Minuten"), "DIEUNZ"));
    // Ohne eigenes Gebot
    assertTrue(!AuctionTooltipParser.hasOwnBid(TOOLTIP, "DIEUNZ"));
    assertEquals(Duration.ofMinutes(85), AuctionTooltipParser.parseRemaining(lavaBucket).get());
    assertEquals("1$", AuctionTooltipParser.parseBid(lavaBucket).get());
  }

  @Test
  void keineZeit() {
    assertFalse(AuctionTooltipParser.parseRemaining(Arrays.asList("Frost-Schrift")).isPresent());
    assertFalse(AuctionTooltipParser.parseDuration("bald").isPresent());
  }
}
