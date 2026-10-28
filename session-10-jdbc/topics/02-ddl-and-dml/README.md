# Topic 2: DDL and DML commands

SQL commands come in groups:

| Group | Stands for | Commands | Changes |
|---|---|---|---|
| **DDL** | Data Definition Language | `CREATE`, `ALTER`, `DROP`, `TRUNCATE` | the **structure**: tables, columns, constraints |
| **DML** | Data Manipulation Language | `INSERT`, `UPDATE`, `DELETE` (and `SELECT`, sometimes called DQL) | the **rows** |
| DCL | Data Control Language | `GRANT`, `REVOKE` | permissions |
| TCL | Transaction Control Language | `COMMIT`, `ROLLBACK`, `SAVEPOINT` | transactions (topic 1) |

## DDL in this module: [`schema.sql`](../../src/main/resources/schema.sql)

```sql
CREATE TABLE IF NOT EXISTS course (
    code        VARCHAR(10)  PRIMARY KEY,
    title       VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS student (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,                -- the database assigns 1, 2, 3...
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,                     -- no two students share an email
    marks       INT          NOT NULL CHECK (marks BETWEEN 0 AND 100),
    course_code VARCHAR(10)  NOT NULL REFERENCES course (code)    -- foreign key: must exist in course
);
```

| Constraint | Guarantees | Broken in the test `constraints` by |
|---|---|---|
| `PRIMARY KEY` | unique, not null; identifies the row | (the database assigns ids) |
| `NOT NULL` | a value is always there | a `null` title in `PlainJdbcDemoTest.rollback` |
| `UNIQUE` | no duplicates in that column | a second `asha@example.com` |
| `CHECK` | a condition on the value | marks = 150 |
| `REFERENCES` (foreign key) | the value exists in the other table | course `PYTHON` |

The database enforces these **even if the Java code has a bug**: the last line of defence for your data.
Spring turns each violation into a `DataIntegrityViolationException`.

## DML in this module: [`data.sql`](../../src/main/resources/data.sql) and the repository

```sql
INSERT INTO student (name, email, marks, course_code) VALUES ('Asha', 'asha@example.com', 91, 'JAVA');
UPDATE student SET marks = ? WHERE id = ?;
DELETE FROM student WHERE id = ?;
SELECT s.name, c.title FROM student s JOIN course c ON c.code = s.course_code ORDER BY s.name;
```

**Always write a `WHERE` on `UPDATE` and `DELETE`:** without it, every row changes.

`DELETE` vs `TRUNCATE` vs `DROP`: delete chosen rows (DML, can roll back) / delete all rows fast (DDL)
/ remove the whole table.

## How Spring Boot runs these files

For an embedded database (H2), Spring Boot runs `schema.sql` then `data.sql` at startup
(`spring.sql.init.mode=embedded`, the default). For a real database you'd use a migration tool
(Flyway or Liquibase) that versions every change: `V1__create_student.sql`, `V2__add_phone.sql`...

## Interview questions

- **DDL vs DML?** Structure vs rows.
- **`DELETE` vs `TRUNCATE`?** Row by row with `WHERE`, logged and rollback-able vs all rows at once.
- **Primary key vs unique key?** Both unique; a primary key is also `NOT NULL` and there's one per table.
- **What is a foreign key?** A column whose values must exist in another table's key; keeps references valid.
- **Why constraints in the database if the code validates?** Several programs, bugs and manual fixes all write to the same data.
