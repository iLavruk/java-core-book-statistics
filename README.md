# Book Statistics CLI

Консольна утиліта на Java 21, що рахує статистику по книгах у JSON-файлах і зберігає результат у XML. Основна сутність — `Book`, другорядна (many-to-one) — `Author`. Атрибут `genres` багатозначний (жанри через кому).

## Як запустити
1. Зібрати проєкт:
   ```bash
   mvn -q clean package
   ```
2. Запустити (папка з JSON, атрибут, опційно — кількість потоків):
   ```bash
   mvn -q exec:java "-Dexec.args=<path_to_dir> <attribute> [threads]"
   ```
   - `path_to_dir` — шлях до папки з JSON-файлами (кожен файл містить масив книг).
   - `attribute` — `author` | `year_published` | `genres` (також синоніми `year`, `yearpublished`).
   - `threads` — розмір пулу (необовʼязково; за замовчуванням = кількість ядер).
   - Результат: `statistics_by_{attribute}.xml` у цій же папці.

   Приклади (дефолтні потоки):
   ```bash
   mvn -q exec:java "-Dexec.args=sample-data genres"
   mvn -q exec:java "-Dexec.args=sample-data author"
   mvn -q exec:java "-Dexec.args=sample-data year_published"
   ```
   Приклади (фіксована кількість потоків для експериментів):
   ```bash
   mvn -q exec:java "-Dexec.args=sample-data genres 1"
   mvn -q exec:java "-Dexec.args=sample-data genres 2"
   mvn -q exec:java "-Dexec.args=sample-data genres 4"
   mvn -q exec:java "-Dexec.args=sample-data genres 8"
   ```
   В IntelliJ можна створити Application run configuration (Main class: `com.profitsoft.statistics.Application`, Program arguments: `sample-data genres`).

## Формат вхідних/вихідних даних
Вхід (`sample-data/books_1.json`):
```json
[
  {
    "title": "1984",
    "author": "George Orwell",
    "year_published": 1949,
    "genres": "Dystopian, Political Fiction"
  }
]
```

Вихід (`statistics_by_genres.xml`):
```xml
<statistics attribute="genres">
  <item>
    <value>Dystopian</value>
    <count>1</count>
  </item>
  <item>
    <value>Political Fiction</value>
    <count>1</count>
  </item>
</statistics>
```

## Структура
- `src/main/java/com/profitsoft/statistics/model` — `Book`, `Author`
- `src/main/java/com/profitsoft/statistics/io` — `BookJsonParser`, `StatisticsXmlWriter`
- `src/main/java/com/profitsoft/statistics/service` — `StatisticsCalculator`, `StatisticsService`
- `src/main/java/com/profitsoft/statistics/util` — `Attribute`, `XmlEscaper`
- `src/test/java` — unit-тести парсингу та агрегації
- `sample-data` — приклади; `sample-data/benchmark` — файли для вимірювань

## Експерименти (genres, 4 файли ~5k записів)

| Потоків | Час, мс |
|---------|---------|
| 1       | 193     |
| 2       | 221     |
| 4       | 244     |
| 8       | 269     |

На цьому обсязі приріст від >2 потоків невеликий через IO-навантаження.

## Тести
```bash
mvn test
```

## Нотатки
- Парсинг стрімінговий (`JsonParser`), файли не вантажаться цілком у памʼять.
- Агрегація: `ConcurrentHashMap` + `LongAdder`.
- Кожен файл обробляється окремим завданням у пулі; `genres` розділяються по комі.
