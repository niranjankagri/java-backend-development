# Topic 4: JPA relationships

The school model in this module has all four kinds:

```
Teacher 1 ──────< * Course * >────── * Student 1 ─────── 1 StudentProfile
        @OneToMany   @ManyToOne     @ManyToMany          @OneToOne
        (mappedBy)   teacher_id     join table           profile_id
                                    ENROLLMENT
```

| Relationship | In the code | Foreign key lives in | Default fetch |
|---|---|---|---|
| **Many-to-one** | `Course.teacher` | `course.teacher_id` | EAGER |
| **One-to-many** | `Teacher.courses` (`mappedBy = "teacher"`) | (same column, seen from the other side) | LAZY |
| **Many-to-many** | `Student.courses` with `@JoinTable(name = "enrollment")` | a third table `enrollment(student_id, course_id)` | LAZY |
| **One-to-one** | `Student.profile` with `cascade = ALL` | `student.profile_id` | EAGER |

Real join table Hibernate generated:

```sql
create table enrollment (
    course_id bigint not null,
    student_id bigint not null,
    primary key (course_id, student_id)
)
```

## Owning side and `mappedBy`

Every two-way relationship has one **owning side**: the field whose changes are written to the
database (the one with the foreign key or the `@JoinTable`). The other side says `mappedBy = "..."` and
is only a mirror. If you change only the mirror, **nothing is saved**. Hence the helper on
[`Student`](../../src/main/java/jbdl/session11/school/Student.java):

```java
public void enroll(Course course) {
    courses.add(course);               // owning side: this is what gets saved
    course.getStudents().add(this);    // mirror: keeps the Java objects consistent
}
```

## Cascade

`@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)` on `Student.profile`: saving a student
saves its profile, deleting the student deletes the profile (test `oneToOneCascade`). Use cascade only
for "part-of" relationships. Never cascade `REMOVE` from a student to its courses: deleting a
student must not delete the course for everyone.

## Fetching: LAZY, EAGER and the N+1 problem

- **LAZY**: the related data is loaded only when you first use it, and only while the transaction is
  open. Outside it: `LazyInitializationException` (test `lazyLoading`).
- **EAGER**: always loaded with the owner. Sounds convenient, but costs queries you didn't ask for.

Real SQL for one innocent line, `students.findByCoursesTitleOrderByName("Core Java")`:

```sql
select s1_0.id, s1_0.email, s1_0.name, s1_0.profile_id
from student s1_0 left join enrollment c1_0 on s1_0.id=c1_0.student_id
     left join course c1_1 on c1_1.id=c1_0.course_id
where c1_1.title=? order by s1_0.name

select sp1_0.id, sp1_0.city, sp1_0.phone from student_profile sp1_0 where sp1_0.id=?   -- Asha's profile
select sp1_0.id, sp1_0.city, sp1_0.phone from student_profile sp1_0 where sp1_0.id=?   -- Ravi's profile
```

1 query for the students + **1 more per student** for the EAGER profile: the **N+1 problem**. With
1,000 students that's 1,001 queries. Fixes:

1. Make it LAZY: `@OneToOne(fetch = FetchType.LAZY, ...)`. A good default for every relationship.
2. When you do need the related data, fetch it in the same query: JPQL `join fetch`, as
   `StudentRepository.findWithCourses` does for the courses, or `@EntityGraph(attributePaths = "profile")`.

(The code keeps the default EAGER one-to-one on purpose, with a comment, so you can see this SQL yourself.)

## Interview questions

- **What does `mappedBy` mean?** This side is the inverse; the other field owns the foreign key.
- **Default fetch types?** `@ManyToOne`/`@OneToOne`: EAGER; `@OneToMany`/`@ManyToMany`: LAZY.
- **What is the N+1 problem and how do you fix it?** 1 query + 1 per row for a relation; use LAZY + `join fetch` or an entity graph.
- **What causes `LazyInitializationException`?** Touching a lazy relation after the transaction/session closed.
- **When to use cascade?** For child objects that belong to the parent (profile, order lines), never for shared ones.
