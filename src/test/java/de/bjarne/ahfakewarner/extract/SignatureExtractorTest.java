package de.bjarne.ahfakewarner.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bjarne.ahfakewarner.extract.SignatureExtractor.Signature;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignatureExtractorTest {

  private final SignatureExtractor extractor =
      new SignatureExtractor(SignatureExtractor.DEFAULT_PATTERN);

  @Test
  void fakeAusAuktionshaus() {
    List<String> lore = Arrays.asList(
        "Du erhältst die Frost-Schrift! (Klicken)",
        "a4Wwn2ZYFcQVn6Yw",
        "Signiert von AbgegriefHD am 24.10.2026",
        "$ | Aktuelles Gebot:");
    Signature signature = this.extractor.extract(lore).get();
    assertEquals("AbgegriefHD", signature.name());
    assertEquals("24.10.2026", signature.date());
  }

  @Test
  void echteMitFarbcodes() {
    List<String> lore = Arrays.asList(
        "§7Aktiviert §a+1 weiteres Clan-Home §7per Rechtsklick!",
        "§7Signiert von §aAbgegrieftHD §7am §e01.06.2026",
        "§d70h5lIJ0209aAFKa");
    assertEquals("AbgegrieftHD", this.extractor.extract(lore).get().name());
  }

  @Test
  void ohneSignatur() {
    assertFalse(this.extractor.extract(Arrays.asList("Ein normales Item")).isPresent());
  }

  @Test
  void ungueltigeRegexFaelltZurueck() {
    SignatureExtractor broken = new SignatureExtractor("([");
    assertEquals("AbgegrieftHD",
        broken.extract(Arrays.asList("Signiert von AbgegrieftHD am 1.6.2026")).get().name());
  }
}
