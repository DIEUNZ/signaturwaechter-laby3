package de.bjarne.ahfakewarner.detect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class IcedOutDetectorTest {

  private static final List<String> FAKE = Arrays.asList(
      "*Iced Out* Schaf", "",
      "Du erhältst ein zufälliges Spawnei oder doch nicht?",
      "Wird per Rechtsklick direkt aktiviert!",
      "minecraft:sheep_spawn_egg", "17 component(s)");

  private static final List<String> REAL = Arrays.asList(
      "*Iced Out* Lohenei", "Luck of the Sea I",
      "minecraft:blaze_spawn_egg", "17 component(s)");

  @Test
  void itemWithoutEnchantmentIsFake() {
    assertTrue(IcedOutDetector.isFake(FAKE, false));
  }

  @Test
  void enchantmentDataMakesItReal() {
    assertFalse(IcedOutDetector.isFake(FAKE, true));
  }

  @Test
  void enchantmentLineMakesItReal() {
    assertFalse(IcedOutDetector.isFake(REAL, false));
  }

  @Test
  void germanEnchantmentLineMakesItReal() {
    assertFalse(IcedOutDetector.isFake(Arrays.asList("*Iced Out* Schaf", "Glück des Meeres III"), false));
  }

  @Test
  void otherItemsAreIgnored() {
    assertFalse(IcedOutDetector.isFake(Arrays.asList("Normales Spawnei", "Hallo"), false));
    assertFalse(IcedOutDetector.isFake(Collections.<String>emptyList(), false));
  }

  @Test
  void titleVariantsAreRecognized() {
    assertTrue(IcedOutDetector.isIcedOut(Arrays.asList("*ICED OUT* Schaf")));
    assertTrue(IcedOutDetector.isIcedOut(Arrays.asList("Iced-Out Kuh")));
  }
}
