package de.bjarne.ahfakewarner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bjarne.ahfakewarner.Texts.Language;
import de.bjarne.ahfakewarner.extract.SignatureExtractor;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TextsTest {

  @AfterEach
  void zuruecksetzen() {
    Texts.setLanguage(Language.GERMAN);
    Texts.setGameLocale("");
  }

  @Test
  void standardIstDeutsch() {
    assertEquals("FÄLSCHUNG - imitiert AbgegrieftHD", Texts.get("warning.fake", "AbgegrieftHD"));
  }

  @Test
  void englischMitPlatzhaltern() {
    Texts.setLanguage(Language.ENGLISH);
    assertEquals("Reminder set: \"Apfel\" ends at 23:05, reminder at 23:03.",
        Texts.get("reminder.set", "Apfel", "23:05", "23:03"));
  }

  @Test
  void wieSpielspracheRichtetSichNachSprachcode() {
    Texts.setLanguage(Language.GAME);
    Texts.setGameLocale("de_DE");
    assertTrue(Texts.isGerman());
    Texts.setGameLocale("en_US");
    assertEquals("No active reminders.", Texts.get("reminder.none"));
    Texts.setGameLocale("fr_FR");
    assertEquals("No active reminders.", Texts.get("reminder.none"));
  }

  @Test
  void tooltipWarnungAufEnglisch() {
    Texts.setLanguage(Language.ENGLISH);
    TooltipWarner warner = new TooltipWarner(Collections.singletonList("AbgegrieftHD"), 2,
        SignatureExtractor.DEFAULT_PATTERN, true);
    assertEquals(Arrays.asList("", "§c! Possible fake - similar to AbgegrieftHD"),
        warner.warningsFor(Arrays.asList("Perk", "Signiert von AbgegriefHD am 01.06.2026"),
            false));
  }

  @Test
  void jederSchluesselGibtEsInBeidenSprachen() {
    for (String key : Arrays.asList("disabled", "notInstalled", "warning.fake", "warning.suspect",
        "warning.icedOut", "warning.chatIcedOut", "warning.chat", "hint.set", "hint.endingSoon",
        "hint.offerAtEnd", "hint.offer", "reminder.title", "reminder.noTime", "reminder.removed",
        "reminder.set", "reminder.due", "reminder.dueWithBid", "reminder.cleared",
        "reminder.none", "reminder.entry", "reminder.usage")) {
      Texts.setLanguage(Language.GERMAN);
      String german = Texts.get(key);
      Texts.setLanguage(Language.ENGLISH);
      String english = Texts.get(key);
      assertTrue(!german.equals(key) && !english.equals(key), key);
    }
  }
}
