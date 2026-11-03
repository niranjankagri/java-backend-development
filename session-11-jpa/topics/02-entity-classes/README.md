# Topic 2: Define entity classes

An **entity** is a Java class mapped to a table; each object is one row.

```java
@Entity                                   // → table STUDENT
public class Student {

    @Id                                   // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // the database assigns it (AUTO_INCREMENT)
    private Long id;

    @Column(nullable = false)             // → NOT NULL
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    protected Student() { }               // required by JPA

    public Student(String name, String email) { ... }
    // getters (and setters only where changes are allowed)
}
```

## Rules for an entity

| Rule | Why |
|---|---|
| `@Entity` + an `@Id` field | Hibernate must know the table and how to identify a row |
| A no-args constructor (`protected` is enough) | Hibernate creates the object first, then fills the fields |
| Not `final`, no `final` fields | Hibernate creates subclasses (proxies) for lazy loading |
| Not a `record` | Records are final and have no no-args constructor |
| `Long id` (wrapper), not `long` | `null` means "not saved yet" |

## Common annotations

| Annotation | Does | Example |
|---|---|---|
| `@Table(name = "...")` | Different table name | (defaults: class `StudentProfile` → table `student_profile`) |
| `@Column(nullable, unique, length)` | Column rules for the generated DDL | `Course.title`: `NOT NULL UNIQUE`, 100 chars |
| `@GeneratedValue(IDENTITY)` | Id from the database's auto-increment | every entity here |
| `@Enumerated(EnumType.STRING)` | Store an enum by name (never the default ORDINAL: reordering the enum would corrupt data) | (Session 12) |
| `@Transient` | A field that is not stored | |
| `@Version` | Optimistic locking: detects two users editing the same row | (Session 13) |

Names: Spring Boot converts `courseCode` to the column `course_code` and `StudentProfile` to the
table `student_profile`, which is why [`data.sql`](../../src/main/resources/data.sql) uses those names.

## equals and hashCode on entities

The entities here don't override `equals`/`hashCode`: inside one transaction Hibernate guarantees one
object per row, so identity (`==`) is enough for these examples. If you put entities in `HashSet`s
across transactions, base `equals` on a stable business key (like `email`), not on the generated id
(it changes from `null` to a number on save), and avoid Lombok's `@Data` on entities (Session 9).

## The code

| File | Table | Shows |
|---|---|---|
| [`Teacher.java`](../../src/main/java/jbdl/session11/school/Teacher.java) | `teacher` | Minimal entity; the inverse side of one-to-many |
| [`Course.java`](../../src/main/java/jbdl/session11/school/Course.java) | `course` | `@Column` rules, a `teacher_id` foreign key, a business method `isFull()` |
| [`Student.java`](../../src/main/java/jbdl/session11/school/Student.java) | `student` + `enrollment` | One-to-one with cascade, many-to-many with a join table, the `enroll` helper |
| [`StudentProfile.java`](../../src/main/java/jbdl/session11/school/StudentProfile.java) | `student_profile` | The other side of the one-to-one |
| [`StudentRepository.java`](../../src/main/java/jbdl/session11/school/StudentRepository.java) etc. | | Spring Data repositories: derived queries, `@Query` JPQL |

## Spring Data repositories

```java
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);                       // derived from the name
    List<Student> findByCoursesTitleOrderByName(String courseTitle);   // follows the relationship

    @Query("select s.name from Student s where s.profile.city = :city order by s.name")
    List<String> namesInCity(@Param("city") String city);              // JPQL: entities and fields, not tables
}
```

`JpaRepository` already has `save`, `saveAll`, `findById`, `findAll`, `count`, `existsById`,
`deleteById`, paging and sorting. No implementation class: Spring generates it at startup.

## Interview questions

- **Minimum for an entity?** `@Entity`, `@Id`, a no-args constructor.
- **Why can't an entity be a record or final?** Hibernate needs a no-args constructor and proxy subclasses.
- **`EnumType.ORDINAL` vs `STRING`?** Position vs name; `STRING` survives reordering.
- **JPQL vs SQL?** JPQL queries entities and their fields; Hibernate translates it to SQL for the database.
- **How does Spring Data implement `findByEmail`?** It parses the method name into a query at startup.
