# Topic 1: Overview of JDBC

**JDBC** (Java Database Connectivity) is the standard Java API for talking to relational databases.
Your code uses the interfaces in `java.sql`; a **driver** (one jar per database) implements them.

```
your code ──▶ java.sql interfaces (Connection, PreparedStatement, ResultSet)
                    │
                    ▼
              JDBC driver (h2.jar, postgresql.jar, mysql-connector-j.jar ...)
                    │
                    ▼
              the database
```

Switching from H2 to PostgreSQL means a different driver and URL, not different Java code (as long as
the SQL itself is standard).

## The steps of every database call

| Step | Code ([`PlainJdbcDemo`](../../src/main/java/jbdl/session10/plainjdbc/PlainJdbcDemo.java)) |
|---|---|
| 1. Connect | `Connection c = DriverManager.getConnection("jdbc:h2:mem:demo", "sa", "")` |
| 2. Prepare the SQL | `PreparedStatement s = c.prepareStatement("SELECT title FROM book WHERE pages > ?")` |
| 3. Set parameters | `s.setInt(1, 400)` (numbered from **1**) |
| 4. Execute | `executeQuery()` for SELECT → `ResultSet`; `executeUpdate()` for INSERT/UPDATE/DELETE/DDL → row count |
| 5. Read results | `while (rows.next()) { rows.getString("title"); }` |
| 6. Close | try-with-resources closes ResultSet, statement, connection |

The URL picks the driver: `jdbc:h2:...`, `jdbc:postgresql://host:5432/db`, `jdbc:mysql://host:3306/db`.

## Statement vs PreparedStatement: SQL injection

```java
// UNSAFE: user input glued into the SQL text
"SELECT COUNT(*) FROM book WHERE title = '" + title + "'"
// with title = x' OR '1'='1   the query becomes   ... WHERE title = 'x' OR '1'='1'   → every row

// SAFE: the ? is filled by the driver, the whole input is one value
"SELECT COUNT(*) FROM book WHERE title = ?"
```

The test `sqlInjection` runs both with the attack string: the unsafe query counts **3** books, the
safe one **0**. Always use `PreparedStatement` (or JdbcTemplate, which does) for any value that comes
from outside. Bonus: the database can reuse the prepared plan.

## Transactions

By default each statement is committed on its own (**auto-commit**). To make several statements
all-or-nothing:

```java
connection.setAutoCommit(false);
try {
    ... insert 1 ...
    ... insert 2 ...
    connection.commit();
} catch (SQLException e) {
    connection.rollback();   // undo insert 1 as well
    throw e;
}
```

`insertBothOrNone` with a `null` second title (breaks `NOT NULL`): the test shows the first insert was
undone. Spring does the same with `@Transactional` (Session 11).

## Connection pools

Opening a database connection takes milliseconds (network, login). A **pool** keeps a few open
connections and lends them out. Spring Boot sets up **HikariCP** automatically:

```
HikariPool-1 - Starting...
HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:students user=SA
HikariPool-1 - Start completed.
```

## Interview questions

- **What is JDBC?** A Java API with interfaces for connecting to databases; drivers implement it.
- **`Statement` vs `PreparedStatement`?** Text SQL vs precompiled SQL with `?` parameters (safe from injection, reusable).
- **`executeQuery` vs `executeUpdate` vs `execute`?** ResultSet / row count / either (returns a boolean).
- **What is a connection pool and why?** Reused open connections; opening one per request is slow.
- **How do you run statements in one transaction?** `setAutoCommit(false)`, then `commit()` or `rollback()`.
