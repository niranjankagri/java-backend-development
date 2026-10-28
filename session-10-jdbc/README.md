# Session 10: Java Database Connectivity (JDBC)

Student records in an in-memory H2 database: first with plain JDBC, then with Spring's JdbcTemplate.
Nothing to install; the same SQL works on PostgreSQL or MySQL.

| # | Topic | Code | README |
|---|---|---|---|
| 1 | Overview of JDBC | [`plainjdbc/PlainJdbcDemo`](src/main/java/jbdl/session10/plainjdbc/PlainJdbcDemo.java) | [01-jdbc-overview](topics/01-jdbc-overview/README.md) |
| 2 | DDL and DML commands | [`schema.sql`](src/main/resources/schema.sql), [`data.sql`](src/main/resources/data.sql) | [02-ddl-and-dml](topics/02-ddl-and-dml/README.md) |
| 3 | Introduction to Spring JdbcTemplate | [`students/StudentRepository`](src/main/java/jbdl/session10/students/StudentRepository.java) | [03-spring-jdbc-template](topics/03-spring-jdbc-template/README.md) |
| 4 | CRUD operations using JdbcTemplate | [`students/StudentRepository`](src/main/java/jbdl/session10/students/StudentRepository.java) | [04-crud-with-jdbc-template](topics/04-crud-with-jdbc-template/README.md) |
| 5 | Map rows to Java objects with RowMapper | [`students/StudentRowMapper`](src/main/java/jbdl/session10/students/StudentRowMapper.java) | [05-rowmapper](topics/05-rowmapper/README.md) |

## What you can do after this session

- Run a query with plain JDBC, protect it from SQL injection, and use a transaction.
- Write DDL with constraints and DML for the rows, and explain what each constraint guarantees.
- Use JdbcTemplate for all four CRUD operations, including reading back a generated id.
- Map rows to objects with a RowMapper.

## Run it

```bash
./mvnw -pl session-10-jdbc verify                                   # 10 tests
java -jar session-10-jdbc/target/session-10-jdbc-1.0.0-SNAPSHOT.jar
```

```
HikariPool-1 - Start completed.
o.s.jdbc.core.JdbcTemplate : Executing SQL query [SELECT COUNT(*) FROM student]
Students: 3
Asha - Core Java
Meena - Spring Boot
Ravi - Core Java
```
