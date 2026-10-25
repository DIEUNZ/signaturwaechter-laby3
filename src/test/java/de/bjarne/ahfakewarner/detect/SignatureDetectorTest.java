package de.bjarne.ahfakewarner.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SignatureDetectorTest {

  private static final String REAL = "AbgegrieftHD";

  private final SignatureDetector detector = new SignatureDetector(Arrays.asList(REAL, "Notch"), 2);

  @Test
  void echterNameIstEcht() {
    DetectionResult result = this.detector.detect(REAL);
    assertEquals(SignatureVerdict.ECHT, result.verdict());
    assertEquals(SignatureVerdict.ECHT, this.detector.detect("Notch").verdict());
  }

  @Test
  void fremderNameIstUnbekannt() {
    assertEquals(SignatureVerdict.UNBEKANNT, this.detector.detect("Steve").verdict());
    assertEquals(SignatureVerdict.UNBEKANNT, this.detector.detect("GrieferBoss2000").verdict());
  }

  @Test
  void fehlenderBuchstabeIstVerdacht() {
    // Beispiel aus dem Auktionshaus
    DetectionResult result = this.detector.detect("AbgegriefHD");
    assertEquals(SignatureVerdict.FAKE_VERDACHT, result.verdict());
    assertEquals(REAL, result.matchedName());
    assertEquals(1, result.distance());
  }

  @Test
  void vertauschteBuchstabenSindVerdacht() {
    DetectionResult result = this.detector.detect("AbgegrietfHD");
    assertEquals(SignatureVerdict.FAKE_VERDACHT, result.verdict());
    assertEquals(1, result.distance());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "AbgegrleftHD",   // l statt i
      "AbgegrIeftHD",   // I statt i
      "Abgegr1eftHD",   // 1 statt i
      "abgegriefthd",   // nur Groß-/Kleinschreibung
      "ABGEGRIEFTHD",
      "AbgеgrieftHD"    // kyrillisches е
  })
  void verwechselbareZeichenSindFake(String fake) {
    DetectionResult result = this.detector.detect(fake);
    assertEquals(SignatureVerdict.FAKE, result.verdict(), fake);
    assertEquals(REAL, result.matchedName());
  }

  @Test
  void nullUndOSindFake() {
    SignatureDetector detector = new SignatureDetector(Arrays.asList("Oskar_O1"), 2);
    assertEquals(SignatureVerdict.FAKE, detector.detect("0skar_01").verdict());
    assertEquals(SignatureVerdict.ECHT, detector.detect("Oskar_O1").verdict());
  }

  @Test
  void rnUndMSindFake() {
    SignatureDetector detector = new SignatureDetector(Arrays.asList("Hammer"), 2);
    assertEquals(SignatureVerdict.FAKE, detector.detect("Harnmer").verdict());
    assertEquals(SignatureVerdict.FAKE, detector.detect("Hamrner").verdict());
  }

  @Test
  void schwellwertWirktSich() {
    String twoOff = "AbgegrieHD"; // f und t fehlen
    assertEquals(SignatureVerdict.FAKE_VERDACHT,
        new SignatureDetector(Arrays.asList(REAL), 2).detect(twoOff).verdict());
    assertEquals(SignatureVerdict.UNBEKANNT,
        new SignatureDetector(Arrays.asList(REAL), 1).detect(twoOff).verdict());
    // Schwellwert 0: nur noch Homoglyph-Fakes
    SignatureDetector strict = new SignatureDetector(Arrays.asList(REAL), 0);
    assertEquals(SignatureVerdict.UNBEKANNT, strict.detect("AbgegriefHD").verdict());
    assertEquals(SignatureVerdict.FAKE, strict.detect("Abgegr1eftHD").verdict());
  }

  @Test
  void leereWhitelistWarntNie() {
    SignatureDetector empty = new SignatureDetector(Arrays.asList(" ", ""), 2);
    assertEquals(SignatureVerdict.UNBEKANNT, empty.detect("AbgegriefHD").verdict());
  }

  @Test
  void distanzBerechnung() {
    assertEquals(0, SignatureDetector.damerauLevenshtein("abc", "abc"));
    assertEquals(1, SignatureDetector.damerauLevenshtein("abc", "acb"));
    assertEquals(1, SignatureDetector.damerauLevenshtein("abc", "ab"));
    assertEquals(3, SignatureDetector.damerauLevenshtein("", "abc"));
  }
}
