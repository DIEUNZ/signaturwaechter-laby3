package de.bjarne.ahfakewarner.detect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpawnEggDetectorTest {

  @Test
  void ghastEggNamedSkeletonIsMismatch() {
    assertTrue(SpawnEggDetector.isMismatch("minecraft:ghast_spawn_egg", "Erschaffe skelett"));
  }

  @Test
  void matchingNameIsFine() {
    assertFalse(SpawnEggDetector.isMismatch("minecraft:skeleton_spawn_egg", "Erschaffe skelett"));
    assertFalse(SpawnEggDetector.isMismatch("minecraft:sheep_spawn_egg", "*Iced Out* Schaf"));
    assertFalse(SpawnEggDetector.isMismatch("minecraft:blaze_spawn_egg", "*Iced Out* Lohenei"));
  }

  @Test
  void witherSkeletonIsNotPlainSkeleton() {
    assertFalse(SpawnEggDetector.isMismatch(
        "minecraft:wither_skeleton_spawn_egg", "Erschaffe Wither-Skelett"));
    assertTrue(SpawnEggDetector.isMismatch(
        "minecraft:skeleton_spawn_egg", "Erschaffe Wither-Skelett"));
  }

  @Test
  void compoundNameWithEggSuffixIsRecognized() {
    assertTrue(SpawnEggDetector.isMismatch("minecraft:ghast_spawn_egg", "*Iced Out* Lohenei"));
  }

  @Test
  void namesWithoutMobAreIgnored() {
    assertFalse(SpawnEggDetector.isMismatch("minecraft:ghast_spawn_egg", "Zufälliges Spawnei"));
  }

  @Test
  void otherItemsAreIgnored() {
    assertFalse(SpawnEggDetector.isMismatch("minecraft:player_head", "Erschaffe skelett"));
    assertFalse(SpawnEggDetector.isMismatch(null, "Erschaffe skelett"));
  }

  @Test
  void legacyEggDataIsMapped() {
    assertTrue("ghast".equals(SpawnEggDetector.mobFromLegacyData(56)));
    assertTrue(SpawnEggDetector.isMismatchForMob(
        SpawnEggDetector.mobFromLegacyData(56), "Erschaffe skelett"));
  }

  @Test
  void entityTagNamesAreNormalized() {
    assertTrue("ghast".equals(SpawnEggDetector.mobFromEntityTag("minecraft:ghast")));
    assertTrue("zombified_piglin".equals(SpawnEggDetector.mobFromEntityTag("PigZombie")));
    assertFalse(SpawnEggDetector.isMismatchForMob(
        SpawnEggDetector.mobFromEntityTag("minecraft:skeleton"), "Erschaffe skelett"));
  }

  @Test
  void eggMobIsReadFromId() {
    assertTrue("ghast".equals(SpawnEggDetector.eggMob("minecraft:ghast_spawn_egg")));
  }
}
