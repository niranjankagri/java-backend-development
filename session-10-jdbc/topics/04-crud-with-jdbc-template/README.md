# Topic 4: CRUD operations using JdbcTemplate

CRUD = **C**reate, **R**ead, **U**pdate, **D**elete: the four things almost every table needs.
All four in [`StudentRepository`](../../src/main/java/jbdl/session10/students/StudentRepository.java):

| Operation | SQL | JdbcTemplate call | Returns |
|---|---|---|---|
| Create | `INSERT INTO student (...) VALUES (?, ?, ?, ?)` | `update(creator, keyHolder)` | the `Student` with its new id |
| Read one | `SELECT * FROM student WHERE id = ?` | `query(sql, mapper, id)` | `Optional<Student>` |
| Read many | `SELECT * FROM student WHERE course_code = ? ORDER BY name` | `query(sql, mapper, code)` | `List<Student>` |
| Count | `SELECT COUNT(*) FROM student` | `queryForObject(sql, Integer.class)` | `int` |
| Update | `UPDATE student SET marks = ? WHERE id = ?` | `update(sql, marks, id)` | `true` if a row changed |
| Delete | `DELETE FROM student WHERE id = ?` | `update(sql, id)` | `true` if a row was deleted |

## Notes on each

- **Create:** a plain `update("INSERT ...", args)` returns only the row count. To get the id the
  database generated (`AUTO_INCREMENT`), we build the `PreparedStatement` ourselves with
  `RETURN_GENERATED_KEYS` and read it from a `KeyHolder`. The method has comments for each step.
- **Read one:** `query` returns a list, so "not found" is just an empty list → `Optional.empty()`.
  (`queryForObject` would throw an exception for a missing row; fine, but harder to read.)
- **Update / Delete:** `update` returns how many rows changed: `1` = done, `0` = no such id. The
  caller decides whether that's a 404 (Session 8).

## Tests: each one starts from the same data

[`StudentRepositoryTest`](../../src/test/java/jbdl/session10/StudentRepositoryTest.java) is annotated
`@Transactional`: every test runs inside a transaction that is **rolled back** at the end. So the
delete test removing Meena doesn't affect the next test; each sees the 3 students from `data.sql`.

| Test | Checks |
|---|---|
| `readAll` | 3 rows from `data.sql`, mapped to `Student`s |
| `create` | a new id, found again by id, count 4 |
| `read` | found / not found, by course, the JOIN |
| `update` / `delete` | 1 row changed, then 0 for an unknown id |
| `constraints` | UNIQUE, CHECK and foreign key violations → `DataIntegrityViolationException` |

## Interview questions

- **How do you get the generated id after an INSERT?** `KeyHolder` + `RETURN_GENERATED_KEYS` (or `SimpleJdbcInsert.executeAndReturnKey`).
- **How do you know whether an UPDATE found the row?** The returned row count.
- **Why `@Transactional` on a test?** Each test is rolled back, so tests don't depend on each other.
- **How would you insert 10,000 rows quickly?** `batchUpdate`: one round trip per batch instead of per row.
