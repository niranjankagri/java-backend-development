# Session 11: Hibernate, Spring Data JPA

A small school model (teachers, courses, students, profiles) stored with JPA/Hibernate on an
in-memory H2 database, with Spring Data repositories and transactional enrollment rules.

| # | Topic | Code | README |
|---|---|---|---|
| 1 | Introduction to JPA and Hibernate | [`SchoolApplication`](src/main/java/jbdl/session11/SchoolApplication.java), [`application.properties`](src/main/resources/application.properties) | [01-jpa-and-hibernate](topics/01-jpa-and-hibernate/README.md) |
| 2 | Define entity classes | [`school`](src/main/java/jbdl/session11/school) | [02-entity-classes](topics/02-entity-classes/README.md) |
| 3 | Implement transactional behaviour | [`enrollment/EnrollmentService`](src/main/java/jbdl/session11/enrollment/EnrollmentService.java) | [03-transactions](topics/03-transactions/README.md) |
| 4 | JPA relationships | [`Student`](src/main/java/jbdl/session11/school/Student.java), [`Course`](src/main/java/jbdl/session11/school/Course.java), [`Teacher`](src/main/java/jbdl/session11/school/Teacher.java) | [04-jpa-relationships](topics/04-jpa-relationships/README.md) |

## What you can do after this session

- Explain JPA, Hibernate and Spring Data, and read the SQL Hibernate generates.
- Write entities and Spring Data repositories with derived queries and JPQL.
- Use `@Transactional` correctly: rollback rules, `readOnly`, the checked-exception surprise.
- Map one-to-one, one-to-many, many-to-one and many-to-many, and spot and fix the N+1 problem.

## Run it

```bash
./mvnw -pl session-11-jpa verify                                    # 13 tests
java -jar session-11-jpa/target/session-11-jpa-1.0.0-SNAPSHOT.jar   # prints the generated SQL and:
```

```
Students: 2
Taking Core Java: [Asha, Ravi]
Asha's courses: [Core Java, Spring Boot]
```
