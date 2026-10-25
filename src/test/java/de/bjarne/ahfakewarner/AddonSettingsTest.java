package de.bjarne.ahfakewarner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class AddonSettingsTest {

  @Test
  void standardIstAbgegrieftHD() {
    AddonSettings settings = new AddonSettings();
    assertEquals(Collections.singletonList("AbgegrieftHD"), settings.whitelistNames());
    assertEquals(2, settings.threshold);
  }

  @Test
  void whitelistMitVerschiedenenTrennern() {
    AddonSettings settings = new AddonSettings();
    settings.whitelist = " AbgegrieftHD, Notch;Steve  Alex ,";
    assertEquals(Arrays.asList("AbgegrieftHD", "Notch", "Steve", "Alex"),
        settings.whitelistNames());
  }

  @Test
  void leereWhitelistPrueftKeineSignatur() {
    AddonSettings settings = new AddonSettings();
    settings.whitelist = "  ";
    assertTrue(settings.whitelistNames().isEmpty());
    assertFalse(settings.createWarner().isWarning(
        Arrays.asList("Perk", "Signiert von AbgegriefHD am 01.06.2026"), false));
  }

  @Test
  void ausgeschaltetGibtKeinePruefung() {
    AddonSettings settings = new AddonSettings();
    settings.enabled = false;
    assertNull(settings.createWarner());
  }

  @Test
  void schwellwertWirdBegrenzt() {
    assertEquals(0, AddonSettings.clampThreshold(-3));
    assertEquals(4, AddonSettings.clampThreshold(9));
    assertEquals(3, AddonSettings.clampThreshold(3));
  }

  @Test
  void schwellwertWirktAufPruefung() {
    AddonSettings settings = new AddonSettings();
    settings.threshold = 0;
    // Ein fehlender Buchstabe ist bei 0 kein Verdacht mehr
    assertFalse(settings.createWarner().isWarning(
        Arrays.asList("Perk", "Signiert von AbgegriefHD am 01.06.2026"), false));
  }
}
