package de.bjarne.ahfakewarner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bjarne.ahfakewarner.TooltipWarner.Finding;
import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FirstSeenTest {

  private final TooltipWarner warner = new TooltipWarner(
      Collections.singletonList("AbgegrieftHD"), 2, SignatureExtractor.DEFAULT_PATTERN, true);

  private List<String> fresh(FirstSeen seen, String... lines) {
    List<String> texts = new ArrayList<String>();
    for (Finding finding : seen.newFindings(this.warner.findings(Arrays.asList(lines), false))) {
      texts.add(finding.chatText);
    }
    return texts;
  }

  @Test
  void signaturFakeNurEinmal() {
    FirstSeen seen = new FirstSeen();
    assertEquals(Arrays.asList("Hauptgewinn-Ticket: signiert von AbgegriefHD"),
        fresh(seen, "§cHauptgewinn-Ticket (#0403)",
            "§5§o§7Signiert von §aAbgegriefHD §7am §e24.10.2026"));
    // Erneutes Hovern (auch ohne F3+H) meldet nichts mehr
    assertTrue(fresh(seen, "Hauptgewinn-Ticket", "Signiert von AbgegriefHD am 24.10.2026")
        .isEmpty());
  }

  @Test
  void anderesDatumIstNeuesItem() {
    FirstSeen seen = new FirstSeen();
    fresh(seen, "Perk", "Signiert von AbgegriefHD am 24.10.2026");
    assertEquals(1, fresh(seen, "Perk", "Signiert von AbgegriefHD am 25.10.2026").size());
  }

  @Test
  void icedOutMeldung() {
    assertEquals(Arrays.asList("*Iced Out* Schaf: Iced Out ohne Verzauberung, vermutlich gefälscht"),
        fresh(new FirstSeen(), "*Iced Out* Schaf"));
  }

  @Test
  void echteSignaturMeldetNichts() {
    assertTrue(fresh(new FirstSeen(), "Perk", "Signiert von AbgegrieftHD am 24.10.2026")
        .isEmpty());
  }
}
