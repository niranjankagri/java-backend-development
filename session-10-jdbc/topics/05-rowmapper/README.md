# Topic 5: Map database rows to Java objects with RowMapper

A query returns **rows and columns**; Java code wants **objects**. A `RowMapper<T>` is the
translation for one row:

```java
public class StudentRowMapper implements RowMapper<Student> {
    @Override
    public Student mapRow(ResultSet row, int rowNumber) throws SQLException {
        return new Student(
                row.getLong("id"),
                row.getString("name"),
                row.getString("email"),
                row.getInt("marks"),
                row.getString("course_code"));   // database column  →  Java field courseCode
    }
}
```

`JdbcTemplate.query(sql, mapper, args)` runs the query and calls `mapRow` **once per row**, already
positioned on that row. You never call `rows.next()` yourself.

```
 id | name  | email             | marks | course_code          Student[id=1, name=Asha, ..., courseCode=JAVA]
----+-------+-------------------+-------+------------   ──▶   Student[id=2, name=Ravi, ..., courseCode=JAVA]
  1 | Asha  | asha@example.com  |    91 | JAVA                Student[id=3, name=Meena, ..., courseCode=SPRING]
  ...
```

## Three ways to write a mapper

| Way | Code | When |
|---|---|---|
| A named class (used here) | `new StudentRowMapper()` | Easiest to read; reused by every query of the repository |
| A lambda | `(row, rowNumber) -> row.getString("name") + " - " + row.getString("title")` | Small one-off mappings: `namesWithCourseTitle()` uses this |
| `DataClassRowMapper.newInstance(Student.class)` | matches columns to record components by name (`course_code` → `courseCode`) | Less code, but the mapping becomes invisible: learn the manual way first |

## Reading column values

| Java type | Method | Note |
|---|---|---|
| `String` | `getString("name")` | |
| `int`, `long` | `getInt`, `getLong` | SQL `NULL` comes back as **0**: use `getObject("marks", Integer.class)` if NULL is possible |
| `BigDecimal` | `getBigDecimal` | money (Session 1) |
| `LocalDate`, `LocalDateTime` | `getObject("created", LocalDate.class)` | |

Use **column names**, not positions (`getString(2)`): the code survives a column being added.

## Interview questions

- **What does a RowMapper do?** Converts the current `ResultSet` row into one object.
- **RowMapper vs ResultSetExtractor?** One row → one object vs the whole `ResultSet` → anything (e.g. a parent with its children from a JOIN).
- **How do you handle a nullable int column?** `getObject(column, Integer.class)` or `rs.wasNull()` after `getInt`.
- **Who calls `rs.next()`?** JdbcTemplate, before each `mapRow` call.
