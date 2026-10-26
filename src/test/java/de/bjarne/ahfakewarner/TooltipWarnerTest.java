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

  @Test
  void falschBenanntesSpawnEiOrange() {
    // Ghast-Ei (1.8.9: Schadenswert 56), das "Erschaffe skelett" heißt; Name mit F3+H-ID
    List<String> lines = Arrays.asList("§fErschaffe skelett (#0383/56)");
    List<TooltipWarner.Finding> findings = this.warner.findings(lines, false, "ghast");
    assertEquals(1, findings.size());
    assertEquals(false, findings.get(0).fake);
    assertEquals("Erschaffe skelett: Das Ei enthält ein anderes Mob (ghast)",
        findings.get(0).chatText);
    assertEquals(Arrays.asList("", "§6! ACHTUNG - das Ei enthält ein anderes Mob (ghast)"),
        TooltipWarner.tooltipLines(findings));
  }

  @Test
  void passendesSpawnEiUndAbschaltbar() {
    assertTrue(this.warner.findings(Arrays.asList("Erschaffe Ghast"), false, "ghast").isEmpty());
    assertTrue(this.warner.findings(Arrays.asList("Erschaffe skelett"), false, null).isEmpty());
    TooltipWarner off = new TooltipWarner(Collections.singletonList("AbgegrieftHD"), 2,
        SignatureExtractor.DEFAULT_PATTERN, true, false);
    assertTrue(off.findings(Arrays.asList("Erschaffe skelett"), false, "ghast").isEmpty());
  }
}
