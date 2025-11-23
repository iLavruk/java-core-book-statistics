package com.profitsoft.statistics.io;

import com.profitsoft.statistics.service.StatisticsCalculator;
import com.profitsoft.statistics.service.StatisticsService;
import com.profitsoft.statistics.util.Attribute;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class XmlReportGeneratorTest {

  @Test
  void correctXmlStructure() throws IOException {
    StatisticsXmlWriter writer = new StatisticsXmlWriter();
    Path output = Files.createTempFile("statistics", ".xml");
    Map<String, Long> stats = new LinkedHashMap<>();
    stats.put("fantasy", 3L);
    stats.put("romance", 2L);

    Path written = writer.write(output, Attribute.GENRES, stats);
    String xml = Files.readString(written);

    assertTrue(xml.contains("<statistics attribute=\"genres\">"));
    assertTrue(xml.contains("<item>"));
    assertTrue(xml.contains("<value>fantasy</value>"));
    assertTrue(xml.contains("<count>3</count>"));
  }

  @Test
  void outputFileNamingConvention() throws IOException {
    StatisticsService service = new StatisticsService(
        new StatisticsCalculator(new BookJsonParser()),
        new StatisticsXmlWriter()
    );
    Path tempDir = Files.createTempDirectory("naming");
    copyResourceToDir("sample-data/books_1.json", tempDir.resolve("books_1.json"));

    Path result = service.generateStatistics(tempDir, Attribute.AUTHOR, 1);

    assertEquals("statistics_by_author.xml", result.getFileName().toString());
    assertTrue(Files.exists(result));
  }

  @Test
  void emptyStatisticsXmlGeneration_createsWellFormedFile() throws IOException {
    StatisticsXmlWriter writer = new StatisticsXmlWriter();
    Path output = Files.createTempFile("empty-stats", ".xml");

    Path written = writer.write(output, Attribute.GENRES, Collections.emptyMap());
    String xml = Files.readString(written);

    assertTrue(xml.contains("<statistics attribute=\"genres\">"));
    assertTrue(xml.contains("</statistics>"));
    assertFalse(xml.contains("<item>"));
  }

  private void copyResourceToDir(String resource, Path destination) throws IOException {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Resource not found: " + resource);
      }
      Files.copy(in, destination);
    }
  }
}
