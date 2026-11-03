# Topic 3: Implement transactional behaviour in Spring Boot

A **transaction** groups database changes into one unit: all of them are saved (**commit**) or none
(**rollback**). Session 10 did it by hand (`setAutoCommit(false)`, `commit()`, `rollback()`); in Spring
you put `@Transactional` on a method.

```java
@Transactional
public Student register(String name, String email, String city, List<String> courseTitles) {
    students.save(student);                      // 1. INSERT student
    for (String title : courseTitles) {
        if (course.isFull()) {
            throw new CourseFullException(title);    // → everything above is rolled back
        }
        student.enroll(course);                  // 2. INSERT enrollment rows (at commit)
    }
    return student;
}
```

Spring wraps the bean in a **proxy** (like the aspects of Session 9): begin before the method, commit
after it, roll back if it throws. Because it's a proxy, the **self-invocation trap** applies here too:
calling a `@Transactional` method from another method of the same class skips the transaction.

## The rules, each one tested ([`TransactionTest`](../../src/test/java/jbdl/session11/TransactionTest.java))

| Rule | Method in [`EnrollmentService`](../../src/main/java/jbdl/session11/enrollment/EnrollmentService.java) | Test |
|---|---|---|
| Changes to loaded entities are saved at commit, no `save()` needed (dirty checking) | `enroll` | `commitByDirtyChecking` |
| A `RuntimeException` rolls back | `enroll` on a full course | `rollbackOnRuntimeException` |
| All steps or none: the new student row is undone too | `register` | `rollbackWholeRegistration` |
| **A checked exception does NOT roll back** (it commits!) | `renameThenFailChecked` | `checkedExceptionCommits` |
| `rollbackFor = Exception.class` fixes that | `renameThenFailCheckedWithRollback` | `rollbackForChecked` |
| Lazy relations work only while the transaction is open | `courseTitlesOf` vs `loadWithoutTransaction` | `lazyLoading` |

The checked-exception rule surprises many developers: Spring follows the old EJB convention that
checked exceptions are "expected business outcomes". If your checked exception means failure, add
`rollbackFor`, or use unchecked exceptions (most Spring code does).

## Options of `@Transactional`

| Option | Meaning |
|---|---|
| `readOnly = true` | Nothing will be written: Hibernate skips dirty checking; use for queries (`courseTitlesOf`) |
| `rollbackFor` / `noRollbackFor` | Change which exceptions roll back |
| `propagation = REQUIRED` (default) | Join the caller's transaction, or start one |
| `propagation = REQUIRES_NEW` | Always a separate transaction (e.g. an audit log row that must stay even if the main work rolls back) |
| `isolation` | How much concurrent transactions see of each other (`READ_COMMITTED` is the usual default) |
| `timeout` | Roll back if it takes too long |

Put `@Transactional` on **service** methods (one business operation = one transaction), not on
controllers or repositories. Spring Data repository methods are already transactional on their own.

## Transactions in tests

- [`EntityAndRepositoryTest`](../../src/test/java/jbdl/session11/EntityAndRepositoryTest.java) is
  `@Transactional`: each test is **rolled back**, so the next starts from `data.sql` again.
- `TransactionTest` is **not**, on purpose: it needs real commits and rollbacks to check what reached
  the database. Its tests create their own uniquely named courses and students instead.

## Interview questions

- **How does `@Transactional` work?** A proxy begins/commits/rolls back around the method.
- **Which exceptions roll back by default?** `RuntimeException` and `Error`, not checked exceptions.
- **Why doesn't `@Transactional` work on a private method or a self-call?** The proxy only intercepts calls from outside on public methods.
- **`REQUIRED` vs `REQUIRES_NEW`?** Join the current transaction vs suspend it and start a new one.
- **What is dirty checking?** Hibernate compares managed entities with their loaded state at flush time and writes UPDATEs.
