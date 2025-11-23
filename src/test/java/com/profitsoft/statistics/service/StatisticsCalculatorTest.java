package com.profitsoft.statistics.service;

import com.profitsoft.statistics.io.BookJsonParser;
import com.profitsoft.statistics.util.Attribute;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsCalculatorTest {

  private StatisticsCalculator calculator;
  private Path dataDirectory;

  @BeforeEach
  void setUp() throws URISyntaxException {
    calculator = new StatisticsCalculator(new BookJsonParser());
    java.net.URL resource = getClass().getClassLoader().getResource("sample-data");
    assertNotNull(resource);
    dataDirectory = Path.of(resource.toURI());
  }

  @Test
  void calculatesGenreStatisticsAcrossFiles() {
    Map<String, Long> stats = calculator.calculate(dataDirectory, Attribute.GENRES, 2);

    assertEquals(3L, stats.get("Romance"));
    assertEquals(2L, stats.get("Tragedy"));
    assertEquals(1L, stats.get("Political Satire"));
    assertTrue(stats.containsKey("Political Fiction"));
  }

  @Test
  void calculatesAuthorStatisticsSingleThread() {
    Map<String, Long> stats = calculator.calculate(dataDirectory, Attribute.AUTHOR, 1);

    assertEquals(2L, stats.get("George Orwell"));
    assertEquals(2L, stats.get("Jane Austen"));
    assertEquals(2L, stats.get("William Shakespeare"));
  }

  @Test
  void sortingOrder_HighestCountFirst() {
    Map<String, Long> stats = calculator.calculate(dataDirectory, Attribute.GENRES, 2);

    var iterator = stats.entrySet().iterator();
    Map.Entry<String, Long> first = iterator.next();
    Map.Entry<String, Long> second = iterator.next();

    assertTrue(first.getValue() >= second.getValue(), "Entries must be sorted by count descending");
  }
}
