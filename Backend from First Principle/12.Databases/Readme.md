# Databases & SQL — Complete Guide

## I. Foundations: Persistence and Storage

A database is a system for **persisting information** — ensuring data survives even after a program stops running.

### The Core Problem: RAM is Temporary

```
Without a database:

  Program starts → data lives in RAM → program stops → data gone ❌

With a database:

  Program starts → reads from disk → works with data → writes back to disk → program stops
                                                                                     │
                                                              data still there next time ✅
```

### RAM vs. Disk

```
RAM (e.g., Redis cache):
  ├── Speed:    Extremely fast (nanoseconds)
  ├── Cost:     Expensive per GB
  ├── Capacity: Small (GBs)
  └── Volatile: Data lost on power off ❌

Disk (e.g., PostgreSQL on HDD/SSD):
  ├── Speed:    Slower (milliseconds)
  ├── Cost:     Cheap per GB
  ├── Capacity: Massive (TBs)
  └── Durable:  Data survives restarts ✅
```

| | RAM (Cache) | Disk (Database) |
|---|---|---|
| Speed | Nanoseconds | Milliseconds |
| Cost | High | Low |
| Capacity | GBs | TBs |
| Survives restart | No | Yes |
| Use case | Sessions, hot data, counters | All persistent application data |

> **Rule of thumb:** Cache what you read often and can afford to lose. Store in a database everything you cannot rebuild from scratch.

---

### What is a DBMS?

A **Database Management System** (e.g., PostgreSQL) is the software layer that sits between your application and raw disk storage. It handles:

```
Your Application
       │
       ▼
  ┌──────────────────────────────────────────┐
  │              DBMS (PostgreSQL)           │
  │                                          │
  │  ├── CRUD operations (Create/Read/       │
  │  │   Update/Delete)                      │
  │  ├── Query optimization                  │
  │  ├── Concurrency control (multi-user)    │
  │  ├── Security & access control           │
  │  └── Data integrity enforcement          │
  └──────────────────────────────────────────┘
       │
       ▼
   Raw Disk Storage (HDD / SSD)
```

You write SQL — the DBMS figures out the most efficient way to execute it.

---

## II. Why Use a DBMS Over Text Files?

Storing data in plain `.txt` or `.csv` files seems simple — until it isn't.

### Problem 1 — Parsing Slowness

```
Text file approach:

  Read entire file into memory
       │
       ▼
  Split every line by comma
       │
       ▼
  Scan line by line for the record you need
       │
       ▼
  Parse strings into usable types manually
       │
       ▼
  Found it (maybe) — after reading 100,000 lines ❌

DBMS approach:

  SELECT * FROM users WHERE id = 42;
       │
       ▼
  Index lookup → jump directly to row 42 ✅
```

### Problem 2 — No Structure or Type Enforcement

```
Text file:
  "laptop,not_a_price,2024-99-99"  ← nothing stops this ❌

DBMS:
  price NUMERIC NOT NULL CHECK (price > 0)
  created_at TIMESTAMPTZ NOT NULL
  → invalid data is rejected at write time ✅
```

### Problem 3 — Concurrency Corruption

```
Two users update the same text file simultaneously:

  User A reads file → [record1, record2]
  User B reads file → [record1, record2]

  User A writes  → [record1_updated, record2]
  User B writes  → [record1, record2_updated]   ← User A's change is gone ❌

DBMS with transactions:

  User A acquires lock → reads → writes → releases lock
  User B waits         →         reads → writes          ✅
  Both changes preserved, no corruption
```

| Problem | Text File | DBMS |
|---|---|---|
| Lookup speed | Full scan every time | Indexed, direct lookup |
| Type safety | None — anything goes | Enforced at schema level |
| Concurrent writes | Last writer wins, data lost | Transactions + locking |
| Relationships | Manual, no enforcement | Foreign keys, constraints |
| Backups & recovery | Manual | Built-in tooling |

---

## III. Relational (SQL) vs. Non-Relational (NoSQL)

### Relational Databases

Data is organized into **tables with a predefined schema**. Relationships between tables are explicit.

```
users table:
  ┌────┬───────────────┬───────────────────────┐
  │ id │ name          │ email                 │
  ├────┼───────────────┼───────────────────────┤
  │  1 │ Alice         │ alice@example.com     │
  │  2 │ Bob           │ bob@example.com       │
  └────┴───────────────┴───────────────────────┘

orders table:
  ┌────┬─────────┬────────┬────────────┐
  │ id │ user_id │ total  │ status     │
  ├────┼─────────┼────────┼────────────┤
  │  1 │       1 │  99.00 │ shipped    │
  │  2 │       1 │ 149.00 │ pending    │
  └────┴─────────┴────────┴────────────┘
          │
          └── foreign key → users.id (enforced relationship)
```

**Best for:** Complex queries, strict data integrity, transactions — CRMs, ERP systems, financial applications.

---

### Non-Relational Databases (NoSQL)

Data is stored in **collections of documents** (like JSON). Schema is flexible — each document can have different fields.

```
users collection:
  {
    "_id": "abc123",
    "name": "Alice",
    "preferences": {
      "theme": "dark",
      "notifications": ["email", "push"]   ← nested, no fixed shape
    }
  }
  {
    "_id": "def456",
    "name": "Bob",
    "bio": "Writer"                         ← field doesn't exist on Alice's doc
  }
```

**Best for:** Unstructured or rapidly changing data — CMS platforms, product catalogs, event logs.

---

### Comparison

| | Relational (SQL) | Non-Relational (NoSQL) |
|---|---|---|
| Schema | Predefined, strict | Flexible, per-document |
| Query language | SQL (standardized) | Varies by database |
| Relationships | Foreign keys, JOINs | Embedded docs or manual |
| Data integrity | Strong (constraints, types) | Application-level |
| Scaling | Vertical (scale up) | Horizontal (scale out) |
| Best for | CRM, finance, ERP | CMS, catalogs, logs |
| Examples | PostgreSQL, MySQL | MongoDB, DynamoDB, Firestore |

### Why PostgreSQL?

PostgreSQL is open-source, battle-tested, and uniquely bridges both worlds:

```
Relational strengths:             NoSQL capabilities:
  ├── ACID transactions             ├── Native JSON / JSONB columns
  ├── Complex JOINs                 ├── Query inside JSON with operators
  ├── Foreign key enforcement       ├── Index JSON fields
  └── Rich type system              └── Store dynamic schemas in one column

→ One database that handles structured AND semi-structured data ✅
```

> **PostgreSQL's JSONB column** lets you store flexible document-like data inside a relational table — you get schema enforcement where you need it, and flexibility where you don't.

---

## IV. Database Migrations

In production, you **never** manually run `ALTER TABLE` or `DROP COLUMN` directly. You use migrations.

### What is a Migration?

A migration is a **versioned, sequential `.sql` file** that describes a schema change. A tool like `dbmate` tracks which migrations have been applied and runs only the new ones.

```
db/migrations/
  ├── 001_create_users.sql
  ├── 002_create_projects.sql
  ├── 003_add_status_to_projects.sql
  ├── 004_create_tasks.sql
  └── 005_add_index_on_tasks_project_id.sql
```

```
First deploy:
  Run 001 → Run 002 → Run 003 → Run 004 → Run 005 ✅
  dbmate records each in a schema_migrations table

Next deploy (new migration added):
  001–005 already applied → skip
  Run 006_... only ✅
```

### Up vs. Down Migrations

Every migration file has two sections:

```sql
-- migrate:up
CREATE TABLE projects (
  id         BIGSERIAL PRIMARY KEY,
  name       TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- migrate:down
DROP TABLE projects;
```

```
Forward (deploy):    dbmate up   → applies migrate:up   ✅
Rollback (revert):   dbmate down → applies migrate:down ✅
```

### The Migration Workflow

```
Developer makes a schema change:
         │
         ▼
Write a new migration file (006_add_avatar_to_users.sql)
         │
         ▼
Commit to version control (Git)
         │
         ▼
CI/CD pipeline runs: dbmate up
         │
         ├── Reads schema_migrations table
         ├── Finds 006 not yet applied
         └── Runs 006_add_avatar_to_users.sql ✅

If something goes wrong:
         │
         ▼
dbmate down → reverts to previous state
         │
         ▼
Fix the migration → re-deploy ✅
```

> **Migrations are to your database what Git commits are to your code** — a full, auditable history of every structural change, who made it, and when.

---

## V. Data Modeling

Good schema design prevents entire categories of bugs. The core building blocks are enums, primary keys, relationships, and referential integrity constraints.

### Enums

Custom types for **fixed, known values**. Enforces integrity and acts as inline documentation.

```sql
-- Define the enum type
CREATE TYPE project_status AS ENUM ('active', 'on_hold', 'archived', 'completed');
CREATE TYPE task_priority   AS ENUM ('low', 'medium', 'high', 'critical');

-- Use it in a table
CREATE TABLE projects (
  id     BIGSERIAL      PRIMARY KEY,
  name   TEXT           NOT NULL,
  status project_status NOT NULL DEFAULT 'active'
);
```

```
Without enum:  status TEXT   → "actve", "Active", "ACTIVE", null — all accepted ❌
With enum:     status project_status → only 'active','on_hold','archived','completed' ✅
```

---

### Relationship Types

#### One-to-One

Each row in table A corresponds to exactly one row in table B. Used to split a large table into a lean core and optional extended data.

```
users                          user_profiles
┌────┬──────────┬───────┐      ┌────┬─────────┬──────────────┬───────────┐
│ id │ email    │ role  │      │ id │ user_id │ display_name │ avatar_url│
├────┼──────────┼───────┤      ├────┼─────────┼──────────────┼───────────┤
│  1 │ a@e.com  │ admin │      │  1 │       1 │ Alice        │ /img/a.png│
│  2 │ b@e.com  │ user  │      │  2 │       2 │ Bob          │ null      │
└────┴──────────┴───────┘      └────┴─────────┴──────────────┴───────────┘
                                              │
                                UNIQUE CONSTRAINT on user_id
                                → one profile per user, enforced ✅
```

```sql
CREATE TABLE user_profiles (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT    NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  display_name TEXT,
  avatar_url   TEXT
);
```

---

#### One-to-Many

One row in table A corresponds to many rows in table B. The most common relationship type.

```
projects                    tasks
┌────┬──────────────┐       ┌────┬────────────┬────────────────┐
│ id │ name         │       │ id │ project_id │ title          │
├────┼──────────────┤       ├────┼────────────┼────────────────┤
│  1 │ Backend API  │──┐    │  1 │          1 │ Setup DB       │
│  2 │ Mobile App   │  │    │  2 │          1 │ Build auth     │
└────┴──────────────┘  └──► │  3 │          1 │ Write tests    │
                            │  4 │          2 │ Design screens │
                            └────┴────────────┴────────────────┘
                                      │
                              foreign key → projects.id
```

```sql
CREATE TABLE tasks (
  id         BIGSERIAL    PRIMARY KEY,
  project_id BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  title      TEXT         NOT NULL,
  priority   task_priority NOT NULL DEFAULT 'medium'
);
```

---

#### Many-to-Many

One row in table A can relate to many rows in table B, and vice versa. Requires a **linking (junction) table**.

```
users ←──────── project_members ────────► projects
┌────┬───────┐  ┌────┬──────────┬────────┐  ┌────┬──────────────┐
│ id │ name  │  │ id │ user_id  │proj_id │  │ id │ name         │
├────┼───────┤  ├────┼──────────┼────────┤  ├────┼──────────────┤
│  1 │ Alice │  │  1 │        1 │      1 │  │  1 │ Backend API  │
│  2 │ Bob   │  │  2 │        2 │      1 │  │  2 │ Mobile App   │
│  3 │ Carol │  │  3 │        1 │      2 │  └────┴──────────────┘
└────┴───────┘  │  4 │        3 │      2 │
                └────┴──────────┴────────┘

Alice is on Backend API AND Mobile App ✅
Backend API has Alice AND Bob ✅
```

```sql
CREATE TABLE project_members (
  id         BIGSERIAL PRIMARY KEY,
  user_id    BIGINT    NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
  project_id BIGINT    NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  role       TEXT      NOT NULL DEFAULT 'member',
  UNIQUE (user_id, project_id)  -- no duplicate memberships
);
```

---

### Referential Integrity: ON DELETE Behavior

When a parent row is deleted, what happens to its children?

```
ON DELETE RESTRICT   → block the delete if children exist
                       "You can't delete this project — it still has tasks" ❌ (intentional)

ON DELETE CASCADE    → automatically delete all children
                       "Delete the project → delete all its tasks too" ✅

ON DELETE SET NULL   → set the foreign key to NULL
                       "Delete the user → keep the tasks, set assigned_to = NULL"

ON DELETE SET DEFAULT → set the foreign key to a default value
```

| Scenario | Recommended Behavior |
|---|---|
| Delete project → tasks | `CASCADE` — tasks are meaningless without a project |
| Delete user → their tasks | `SET NULL` — tasks can be reassigned |
| Delete user → their account | `CASCADE` — full cleanup |
| Delete category → its products | `RESTRICT` — force explicit reassignment first |

---

## VI. Advanced Performance & Security

### Parameterized Queries (SQL Injection Prevention)

**Never** interpolate user input directly into a SQL string.

```
❌ Dangerous — string concatenation:
   String query = "SELECT * FROM users WHERE email = '" + userInput + "'";

   User enters: ' OR '1'='1
   Final query: SELECT * FROM users WHERE email = '' OR '1'='1'
   → Returns ALL rows ❌ — attacker bypasses authentication

✅ Safe — parameterized query:
   String query = "SELECT * FROM users WHERE email = $1";
   db.query(query, [userInput]);

   User enters: ' OR '1'='1
   Treated as a literal string — matches no email ✅
   Database never interprets it as SQL
```

```java
// Spring Boot — safe with JdbcTemplate
String sql = "SELECT * FROM users WHERE email = ?";
jdbcTemplate.queryForObject(sql, userRowMapper, userInput);

// Spring Data JPA — parameterized by default
Optional<User> findByEmail(String email);  // never vulnerable ✅
```

> **Rule:** User input is always data, never code. Parameterized queries enforce this at the driver level — no exceptions.

---

### Indexes

An index is a **separate data structure** (typically a B-Tree) that lets the database jump directly to rows instead of scanning the entire table.

```
Without index — Full Table Scan:

  SELECT * FROM tasks WHERE project_id = 42;

  Scan row 1   → project_id = 7?  No
  Scan row 2   → project_id = 13? No
  ...
  Scan row 50,000 → project_id = 42? Yes ✅ (but checked every row) ❌

With index on project_id — B-Tree Lookup:

  Index: project_id → [row pointers]
    42 → [row 312, row 1089, row 4420]

  Jump directly to those 3 rows ✅
```

```sql
-- Create an index
CREATE INDEX idx_tasks_project_id ON tasks(project_id);

-- Composite index (useful for multi-column WHERE clauses)
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);

-- Unique index (also enforces uniqueness constraint)
CREATE UNIQUE INDEX idx_project_members_unique ON project_members(user_id, project_id);
```

**When to add an index:**

| Add Index When | Avoid Index When |
|---|---|
| Column appears in `WHERE`, `JOIN ON`, `ORDER BY` | Table is tiny (< a few thousand rows) |
| Query returns a small % of rows | Column has very low cardinality (e.g., boolean) |
| Column is a foreign key | Table has extremely high write volume |
| Sort / range queries on this column | You haven't profiled yet — don't guess |

> **Indexes speed up reads but slow down writes** — every `INSERT`, `UPDATE`, and `DELETE` must also update the index. Add indexes based on query profiling (`EXPLAIN ANALYZE`), not speculation.

---

### Triggers

A trigger is **database-side logic** that fires automatically in response to a row event (`INSERT`, `UPDATE`, `DELETE`).

```
Most common use case — auto-updating timestamps:

  Row updated in any table
         │
         ▼
  BEFORE UPDATE trigger fires
         │
         ▼
  Sets updated_at = NOW() automatically
         │
         ▼
  Row written to disk with correct timestamp ✅

  → Application code never has to remember to set updated_at
  → Cannot be accidentally forgotten or bypassed
```

```sql
-- Step 1: Create the trigger function (reusable across tables)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 2: Attach to a table
CREATE TRIGGER trigger_tasks_updated_at
  BEFORE UPDATE ON tasks
  FOR EACH ROW
  EXECUTE FUNCTION set_updated_at();

-- Now every UPDATE on tasks automatically sets updated_at ✅
-- Repeat for any other table that needs it
```

Other trigger use cases:

| Use Case | Trigger Type |
|---|---|
| Auto-set `updated_at` | `BEFORE UPDATE` |
| Write to an audit log on any change | `AFTER INSERT OR UPDATE OR DELETE` |
| Prevent deletion of certain rows | `BEFORE DELETE` (raise exception) |
| Sync a denormalized cache column | `AFTER UPDATE` |

> **Use triggers sparingly.** They execute invisibly — logic hidden inside the database is hard to test, debug, and discover. Auto-timestamps are the canonical good use case. Business logic belongs in application code.

---

## VII. Spring Boot Implementation

### A. Entity and Repository

```java
// Enum mapping
public enum ProjectStatus { ACTIVE, ON_HOLD, ARCHIVED, COMPLETED }

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
}

// Spring Data JPA — zero boilerplate for standard queries
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByStatus(ProjectStatus status);
    List<Project> findByNameContainingIgnoreCase(String keyword);
}
```

---

### B. Safe Parameterized Queries

```java
// Spring Data JPA — always parameterized, never vulnerable
@Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.priority = :priority")
List<Task> findByProjectAndPriority(
    @Param("projectId") Long projectId,
    @Param("priority")  TaskPriority priority
);

// JdbcTemplate — explicit parameterization
@Repository
public class TaskRepository {
    public List<Task> findOverdue(Long projectId, Instant cutoff) {
        String sql = "SELECT * FROM tasks WHERE project_id = ? AND due_date < ? AND status != 'done'";
        return jdbcTemplate.query(sql, taskRowMapper, projectId, cutoff);
        //                                             ^^^^^^^^^^^^^^^^^^^^
        //                               passed as parameters, never interpolated ✅
    }
}
```

---

### C. Database Migrations with Flyway

```java
// application.properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

```
db/migration/
  ├── V1__create_users.sql
  ├── V2__create_projects.sql
  ├── V3__create_tasks.sql
  └── V4__add_index_tasks_project_id.sql
```

```sql
-- V3__create_tasks.sql
CREATE TYPE task_priority AS ENUM ('low', 'medium', 'high', 'critical');

CREATE TABLE tasks (
  id         BIGSERIAL     PRIMARY KEY,
  project_id BIGINT        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  title      TEXT          NOT NULL,
  priority   task_priority NOT NULL DEFAULT 'medium',
  created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trigger_tasks_updated_at
  BEFORE UPDATE ON tasks
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

---

### D. Many-to-Many with Linking Table

```java
@Entity
@Table(name = "project_members")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String role = "member";
}
```

---

## VIII. Quick Reference Checklist

| Concern | Mechanism |
|---|---|
| Persistent storage | Disk-based DBMS (PostgreSQL) |
| Fast ephemeral data | RAM-based cache (Redis) |
| CRUD + integrity + concurrency | Use a DBMS, never raw text files |
| Flexible + relational data | PostgreSQL (JSONB + relational) |
| Schema changes in production | Migrations (Flyway / dbmate) |
| Rollback a migration | `migrate:down` / Flyway undo |
| Fixed allowed values | `ENUM` type |
| One profile per user | One-to-One + `UNIQUE` foreign key |
| One project, many tasks | One-to-Many + foreign key |
| Users ↔ Projects membership | Many-to-Many + junction table |
| Prevent orphaned records | `ON DELETE CASCADE` or `RESTRICT` |
| SQL Injection prevention | Parameterized queries — always |
| Speed up `WHERE` / `JOIN` columns | `CREATE INDEX` |
| Auto-update `updated_at` | `BEFORE UPDATE` trigger |
| Audit trail | `AFTER INSERT OR UPDATE OR DELETE` trigger |
| Spring entity mapping | `@Entity`, `@Column`, `@Enumerated` |
| Spring safe queries | `@Query` with `@Param` / JdbcTemplate `?` |
| Spring migrations | Flyway + `spring.flyway.enabled=true` |