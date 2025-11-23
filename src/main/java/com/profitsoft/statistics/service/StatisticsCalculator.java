package com.profitsoft.statistics.service;

import com.profitsoft.statistics.io.BookJsonParser;
import com.profitsoft.statistics.model.Book;
import com.profitsoft.statistics.util.Attribute;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;

/**
 * Aggregates statistics using a thread pool where each file is parsed in its own task.
 */
public class StatisticsCalculator {

  private final BookJsonParser parser;

  public StatisticsCalculator(BookJsonParser parser) {
    this.parser = parser;
  }

  public Map<String, Long> calculate(Path directory, Attribute attribute, int threadCount) {
    if (threadCount < 1) {
      throw new IllegalArgumentException("Thread count must be at least 1");
    }
    List<Path> files = listJsonFiles(directory);
    if (files.isEmpty()) {
      throw new IllegalArgumentException("No JSON files found in directory: " + directory);
    }

    ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (Path file : files) {
        futures.add(executor.submit(() -> parseFile(attribute, counters, file)));
      }

      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof UncheckedIOException io) {
            throw io;
          }
          throw new RuntimeException("Failed to parse files", cause);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Interrupted while parsing files", e);
        }
      }
    } finally {
      executor.shutdown();
    }

    return toImmutableMap(counters);
  }

  private void parseFile(Attribute attribute, ConcurrentHashMap<String, LongAdder> counters, Path file) {
    try {
      parser.parse(file, book -> accumulate(attribute, counters, book));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to parse file: " + file, e);
    }
  }

  private void accumulate(Attribute attribute, ConcurrentHashMap<String, LongAdder> counters, Book book) {
    attribute.extractValues(book).stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .forEach(value -> counters.computeIfAbsent(value, key -> new LongAdder()).increment());
  }

  private List<Path> listJsonFiles(Path directory) {
    try {
      if (!Files.isDirectory(directory)) {
        throw new IllegalArgumentException("Provided path is not a directory: " + directory);
      }
      try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
        return stream
            .filter(path -> !Files.isDirectory(path))
            .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
            .toList();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read directory: " + directory, e);
    }
  }

  private Map<String, Long> toImmutableMap(ConcurrentHashMap<String, LongAdder> counters) {
    if (counters.isEmpty()) {
      return Collections.emptyMap();
    }
    return counters.entrySet().stream()
        .sorted(Comparator
            .<Map.Entry<String, LongAdder>>comparingLong(entry -> entry.getValue().longValue())
            .reversed()
            .thenComparing(Map.Entry::getKey))
        .collect(java.util.stream.Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().longValue(),
            (a, b) -> a,
            LinkedHashMap::new
        ));
  }
}
