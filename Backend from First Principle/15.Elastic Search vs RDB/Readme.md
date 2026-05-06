# Full-Text Search & Elasticsearch — Complete Guide

## I. The Problem with Relational Database Search

Standard SQL is the backbone of most backends — but it has a fundamental weakness when it comes to search.

### The "Librarian" Analogy

A relational database is like a librarian who knows exactly where every book is shelved. But when asked *"find me every book that mentions this phrase,"* they must walk **every single aisle and scan every single shelf** — one by one.

| Dataset Size | `ILIKE` Query Time |
|---|---|
| ~5,000 records | Milliseconds — acceptable |
| Millions of records | 30+ seconds — unusable |

---

### Three Core Failures of SQL Search

**1. Performance at Scale**
`LIKE` and `ILIKE` queries cannot use standard B-tree indexes. They perform a **full table scan** — reading every row — which degrades linearly as data grows.

```sql
-- This scans every row in the table
SELECT * FROM products WHERE description ILIKE '%laptop%';
```

**2. No Concept of Relevance**
SQL matching is **binary** — a row either matches or it doesn't. It cannot distinguish between:

```
Document A: Title is "Machine Learning"             ← highly relevant
Document B: Mentions "machine learning" on page 412 ← barely relevant
```

Both return as identical matches. There is no ranking, no scoring, no sense of *how well* something matches.

**3. No Typo Tolerance**
A user searching `"lpatop"` instead of `"laptop"` gets zero results. In high-traffic consumer applications, typos are not edge cases — they are the norm.

```sql
-- Zero results for a common typo
SELECT * FROM products WHERE name ILIKE '%lpatop%';
→ 0 rows
```

---

## II. The Inverted Index: A Revolutionary Idea

Search engines solve the performance problem with a fundamentally different data structure: the **Inverted Index**.

### The Concept

Instead of mapping **Document → Words** (how a database stores data), an inverted index maps **Word → Documents**.

**Traditional storage (forward index):**

```
Document 1: "Machine Learning is powerful"
Document 2: "Learning new machines is fun"
Document 3: "Powerful tools for engineers"
```

**Inverted index (built at index time):**

```
"machine"    → [Document 1, Document 2]
"learning"   → [Document 1, Document 2]
"powerful"   → [Document 1, Document 3]
"tools"      → [Document 3]
"engineers"  → [Document 3]
```

When a user searches for `"machine"`, the engine **immediately looks up the word in the index** and returns the document list — no scanning required.

```
Search: "machine"
     │
     ▼
Inverted Index lookup: O(1)
     │
     ▼
Result: [Document 1, Document 2]  ← instant ✅
```

### Underlying Technology

Most modern search engines — including **Elasticsearch** — are built on **[Apache Lucene](https://lucene.apache.org/)**, a Java-based library that implements inverted index technology. Elasticsearch is essentially a distributed, REST-accessible wrapper around Lucene.

```
Your Application
      │
      ▼
  Elasticsearch  (REST API, clustering, replication)
      │
      ▼
  Apache Lucene  (inverted index, scoring, tokenization)
```

---

## III. Relevance Scoring and the BM25 Algorithm

Elasticsearch doesn't just find matches — it **ranks** them. Every result gets a relevance score computed by the **BM25 algorithm** (Best Match 25), the industry standard for full-text search ranking.

### Scoring Factors

**1. Term Frequency (TF)**
How many times the search term appears in a specific document. More appearances → higher relevance.

```
Query: "laptop"

Document A: "laptop" appears 12 times  → high TF → higher score
Document B: "laptop" appears 1 time    → low TF  → lower score
```

**2. Document Frequency (DF) — Inverse Document Frequency (IDF)**
How common the term is *across all documents*. Rare words carry more weight than common ones.

```
"the"    → appears in 98% of documents → low weight (not useful)
"Kevlar" → appears in 0.01% of documents → high weight (very specific)
```

A document matching a rare, specific term scores higher than one matching a generic word.

**3. Field Boosting**
You can configure the engine to weight matches in certain fields more heavily than others.

```json
// A match in "title" is 10x more important than in "description"
{
  "multi_match": {
    "query": "machine learning",
    "fields": ["title^10", "description^1", "content^0.5"]
  }
}
```

### BM25 at a Glance

```
Score = IDF(term) × TF(term, document) × field_boost
              │              │
         Rare = high    Frequent = high
         Common = low   Rare = low
```

> **Why not TF-IDF?** BM25 improves on classic TF-IDF by adding a saturation function — a term appearing 100 times is not 100x more relevant than one appearing 10 times. BM25 dampens extreme frequencies for more natural results.

---

## IV. Advanced Search Features

### 1. Typo Tolerance (Fuzzy Search)

Elasticsearch uses **edit distance** (Levenshtein distance) to match terms that are close to — but not exactly — the query.

```
User types:  "treading today"
Engine infers: "trending today"  ← 1 character edit distance

User types:  "lpatop"
Engine returns: results for "laptop" ✅
```

Fuzzy search is configurable — you control the maximum number of character edits allowed (`fuzziness: 1` or `fuzziness: 2`).

---

### 2. Type-Ahead (Autocomplete)

Because search is based on **pre-indexed tokens**, results can be served in real time as a user types — no waiting for a database scan.

```
User types: "mac"
              │
              ▼
Instant index lookup:
  → "machine learning"
  → "macbook"
  → "macaroni recipe"
```

This is implemented using **edge n-gram tokenizers** — at index time, words are broken into progressively longer prefixes and stored in the inverted index.

---

### 3. Log Management — The ELK Stack

Elasticsearch is a core component of the **ELK Stack**, widely used for observability and debugging at scale:

| Component | Role |
|---|---|
| **E**lasticsearch | Stores and searches log data |
| **L**ogstash | Ingests and transforms logs from various sources |
| **K**ibana | Visualizes logs and metrics via dashboards |

```
Application Logs
      │
      ▼
  Logstash (parse + transform)
      │
      ▼
  Elasticsearch (index + store)
      │
      ▼
  Kibana (search + visualize)
```

This enables engineers to search through **billions of log lines** in seconds for system auditing, error investigation, and performance debugging.

---

## V. Real-World Benchmark: Postgres vs. Elasticsearch

A live test with **50,000 reviews**, searching for the keyword `"laptop"`:

| | Postgres (`ILIKE`) | Elasticsearch |
|---|---|---|
| **Query Type** | Full table scan | Inverted index lookup |
| **Results Returned** | ~8,000 | ~8,000 |
| **Time Taken** | ~7.5 seconds | ~500 milliseconds |
| **Speed Difference** | — | **~15x faster** |

```
Postgres  ████████████████████████████████████░  7.5s
Elastic   ██░                                     0.5s
```

> And that's with only 50,000 records. At millions of records, the gap widens dramatically — Elasticsearch remains near-constant while Postgres degrades linearly.

### When to Reach for Elasticsearch

| Use Case | Use Postgres | Use Elasticsearch |
|---|---|---|
| Exact ID lookup | ✅ | ❌ |
| Simple filter queries | ✅ | ❌ |
| Full-text search on large datasets | ❌ | ✅ |
| Autocomplete / type-ahead | ❌ | ✅ |
| Typo-tolerant search | ❌ | ✅ |
| Ranked / relevant results | ❌ | ✅ |
| Log aggregation & search | ❌ | ✅ |

> **Final advice:** Mastering relational databases is your #1 priority. But knowing *when* to reach for Elasticsearch is what separates good backend engineers from great ones.

---

## VI. Elasticsearch in Spring Boot

### A. Setup — Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic       # if security is enabled
    password: changeme
```

---

### B. Define an Elasticsearch Document

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Document(indexName = "products")   // maps to an ES index
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "english")
    private String title;

    @Field(type = FieldType.Text, analyzer = "english")
    private String description;

    @Field(type = FieldType.Keyword)  // exact match — not analyzed
    private String status;

    @Field(type = FieldType.Double)
    private double price;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
}
```

> **`FieldType.Text` vs `FieldType.Keyword`:**
> - `Text` — tokenized and analyzed. Used for full-text search fields like `title`, `description`.
> - `Keyword` — stored as-is. Used for exact-match fields like `status`, `category`, `id`.

---

### C. Repository — Simple Queries

```java
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository
        extends ElasticsearchRepository<ProductDocument, String> {

    // Spring Data derives the query from the method name
    List<ProductDocument> findByTitle(String title);
    List<ProductDocument> findByStatus(String status);
    List<ProductDocument> findByPriceBetween(double min, double max);
}
```

---

### D. Full-Text Search with Field Boosting

For advanced queries — fuzzy search, multi-field boosting, pagination — use `ElasticsearchOperations`:

```java
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.NativeQuery;

@Service
public class ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchHits<ProductDocument> search(String keyword, int page, int size) {

        Query multiMatchQuery = MultiMatchQuery.of(m -> m
            .query(keyword)
            .fields("title^10", "description^1")  // title boosted 10x
            .fuzziness("AUTO")                     // typo tolerance
        )._toQuery();

        NativeQuery query = NativeQuery.builder()
            .withQuery(multiMatchQuery)
            .withPageable(PageRequest.of(page, size))
            .build();

        return elasticsearchOperations.search(query, ProductDocument.class);
    }
}
```

---

### E. Autocomplete with Edge N-Gram

**1. Define a custom analyzer in the index settings:**

```java
@Setting(settingPath = "elasticsearch/product-settings.json")
@Document(indexName = "products")
public class ProductDocument { ... }
```

```json
// resources/elasticsearch/product-settings.json
{
  "analysis": {
    "analyzer": {
      "autocomplete_analyzer": {
        "type": "custom",
        "tokenizer": "standard",
        "filter": ["lowercase", "autocomplete_filter"]
      }
    },
    "filter": {
      "autocomplete_filter": {
        "type": "edge_ngram",
        "min_gram": 2,
        "max_gram": 20
      }
    }
  }
}
```

**2. Apply the analyzer to the autocomplete field:**

```java
@Field(type = FieldType.Text, analyzer = "autocomplete_analyzer",
       searchAnalyzer = "standard")
private String titleAutocomplete;
```

---

### F. Fuzzy Search

```java
Query fuzzyQuery = FuzzyQuery.of(f -> f
    .field("title")
    .value(keyword)
    .fuzziness("2")           // allow up to 2 character edits
    .prefixLength(1)          // first character must match exactly
)._toQuery();
```

```
User types: "lpatop"
Fuzziness:  2 edits
Match:      "laptop" ✅
```

---

### G. Syncing Postgres → Elasticsearch

Elasticsearch is a **secondary store** — your source of truth stays in Postgres. You need a sync strategy:

**Option 1 — Sync on Write (simple, tightly coupled):**

```java
@Service
public class ProductService {

    private final ProductRepository postgresRepo;
    private final ProductSearchRepository elasticRepo;

    public Product createProduct(ProductRequest request) {
        Product saved = postgresRepo.save(request.toEntity());

        // Mirror to Elasticsearch immediately after DB write
        elasticRepo.save(ProductDocument.from(saved));

        return saved;
    }
}
```

**Option 2 — Event-Driven Sync (recommended for production):**

```
Postgres write
     │
     ▼
Publish event to message queue (Kafka / RabbitMQ)
     │
     ▼
Search indexing consumer reads event
     │
     ▼
Elasticsearch updated asynchronously
```

> **Why event-driven?** It decouples the write path from indexing. If Elasticsearch is temporarily down, events queue up and replay — no data loss, no tight coupling.

---

### Spring Boot Elasticsearch — Quick Reference Checklist

| Concern | Mechanism |
|---|---|
| Dependency | `spring-boot-starter-data-elasticsearch` |
| Map a class to an index | `@Document(indexName = "...")` |
| Full-text field | `@Field(type = FieldType.Text)` |
| Exact-match field | `@Field(type = FieldType.Keyword)` |
| Simple queries | `ElasticsearchRepository` method names |
| Advanced queries | `ElasticsearchOperations` + `NativeQuery` |
| Field boosting | `"title^10"` in `MultiMatchQuery` |
| Typo tolerance | `.fuzziness("AUTO")` in query |
| Autocomplete | Edge n-gram analyzer in index settings |
| Postgres sync (simple) | Write to both repos in the service layer |
| Postgres sync (production) | Event-driven via Kafka / RabbitMQ |
| Log search | ELK Stack — Logstash + Elasticsearch + Kibana |