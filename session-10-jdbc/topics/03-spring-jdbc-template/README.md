# Topic 3: Introduction to Spring JdbcTemplate

Plain JDBC (topic 1) repeats the same code around every query: open a connection, prepare, set
parameters, loop over the `ResultSet`, catch `SQLException`, close everything. `JdbcTemplate` does
all of that; you write only the SQL and how to turn a row into an object.

## The same query both ways

```java
// plain JDBC: ~12 lines
try (Connection c = DriverManager.getConnection(url, "sa", "");
     PreparedStatement s = c.prepareStatement("SELECT * FROM student WHERE course_code = ?")) {
    s.setString(1, courseCode);
    try (ResultSet rows = s.executeQuery()) {
        while (rows.next()) {
            list.add(new Student(rows.getLong("id"), rows.getString("name"), ...));
        }
    }
}

// JdbcTemplate: 1 line
jdbc.query("SELECT * FROM student WHERE course_code = ? ORDER BY name", new StudentRowMapper(), courseCode);
```

## What JdbcTemplate does for you

| You'd write by hand | JdbcTemplate |
|---|---|
| Get and close a connection | borrows one from the pool (HikariCP) and gives it back |
| `prepareStatement`, `setString(1, ...)` | from the SQL and the extra arguments |
| `while (rows.next())` | calls your `RowMapper` once per row |
| `catch (SQLException e)` (checked) | translates it to Spring's **unchecked** `DataAccessException` (e.g. `DataIntegrityViolationException`, `DuplicateKeyException`) |
| Join a running transaction | uses the connection of the current `@Transactional` transaction |

## Setup in Spring Boot

1. `spring-boot-starter-jdbc` + a driver in [`pom.xml`](../../pom.xml)
2. `spring.datasource.url=jdbc:h2:mem:students` in [`application.properties`](../../src/main/resources/application.properties)
3. Spring Boot creates the `DataSource` (pool) and a `JdbcTemplate` bean; inject it:

```java
@Repository
public class StudentRepository {
    private final JdbcTemplate jdbc;
    public StudentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
}
```

Set `logging.level.org.springframework.jdbc.core.JdbcTemplate=DEBUG` to see every query:

```
o.s.jdbc.core.JdbcTemplate : Executing SQL query [SELECT COUNT(*) FROM student]
Students: 3
```

## The main methods

| Method | For | Returns |
|---|---|---|
| `query(sql, rowMapper, args...)` | SELECT, many rows | `List<T>` (empty if none) |
| `queryForObject(sql, Integer.class, args...)` | SELECT of one value (`COUNT(*)`) | the value |
| `queryForObject(sql, rowMapper, args...)` | SELECT of exactly one row | the object; **throws** if 0 rows |
| `update(sql, args...)` | INSERT / UPDATE / DELETE | number of changed rows |
| `update(creator, keyHolder)` | INSERT that must return the generated id | row count; id in the `KeyHolder` |
| `execute(sql)` | DDL | nothing |

`NamedParameterJdbcTemplate` is the same with named parameters (`:course` instead of `?`), handy for
long queries. Spring 6.1+ also has `JdbcClient`, a fluent version:
`jdbcClient.sql("SELECT ...").param(code).query(Student.class).list()`.

## Interview questions

- **Why JdbcTemplate over plain JDBC?** No boilerplate, no resource leaks, unchecked exception translation, transaction support.
- **What is `DataAccessException`?** Spring's unchecked, database-independent exception hierarchy.
- **`query` vs `queryForObject`?** List (0..n rows) vs exactly one result (throws `EmptyResultDataAccessException` on 0).
- **JdbcTemplate vs JPA?** You write the SQL vs the framework generates it from entity mappings (Session 11).
