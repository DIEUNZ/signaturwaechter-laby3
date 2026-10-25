package de.bjarne.ahfakewarner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TooltipWarnerTest {

  private final TooltipWarner warner = new TooltipWarner(
      Collections.singletonList("AbgegrieftHD"), 2, SignatureExtractor.DEFAULT_PATTERN, true);

  @Test
  void echteSignaturBleibtOhneWarnung() {
    // Lore-Zeilen kommen in 1.8.9 mit §5§o-Präfix aus getTooltip
    List<String> lines = Arrays.asList("§f+1 Clan-Home (Item)",
        "§5§o§7Signiert von §aAbgegrieftHD §7am §e01.06.2026");
    assertTrue(this.warner.warningsFor(lines, false).isEmpty());
  }

  @Test
  void gefaelschteSignaturWirdRotMarkiert() {
    List<String> lines = Arrays.asList("§fFrost-Schrift",
        "§5§oSigniert von AbgegriefHD am 24.10.2026");
    assertEquals(Arrays.asList("", "§c! Mögliche Fälschung - ähnlich zu AbgegrieftHD"),
        this.warner.warningsFor(lines, false));
  }

  @Test
  void homoglyphIstFaelschung() {
    List<String> lines = Arrays.asList("Perk", "Signiert von AbgegrIeftHD am 01.06.2026");
    assertEquals(Arrays.asList("", "§c! FÄLSCHUNG - imitiert AbgegrieftHD"),
        this.warner.warningsFor(lines, false));
  }

  @Test
  void icedOutOhneVerzauberung() {
    List<String> lines = Arrays.asList("§o*Iced Out* Schaf", "§5§oDu erhältst ein Spawnei");
    assertEquals(Arrays.asList("", "§c! FÄLSCHUNG - Iced Out ohne Verzauberung"),
        this.warner.warningsFor(lines, false));
    assertTrue(this.warner.warningsFor(lines, true).isEmpty());
  }

  @Test
  void icedOutPruefungAbschaltbar() {
    TooltipWarner off = new TooltipWarner(
        Collections.singletonList("AbgegrieftHD"), 2, SignatureExtractor.DEFAULT_PATTERN, false);
    assertTrue(off.warningsFor(Arrays.asList("*Iced Out* Schaf"), false).isEmpty());
  }
}
