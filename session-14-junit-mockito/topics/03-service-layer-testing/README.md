# Topic 3: Implement service-layer testing

[`LoanServiceTest`](../../src/test/java/jbdl/session14/LoanServiceTest.java) tests every rule of
[`LoanService`](../../src/main/java/jbdl/session14/loans/LoanService.java) with mocked collaborators.

## Design the code so it can be tested

| Testable | Hard to test |
|---|---|
| `public LoanService(LibraryRepository repository, Notifier notifier, Clock clock)` (everything passed in) | `new EmailNotifier()` inside the service |
| `LocalDate.now(clock)` | `LocalDate.now()`: "today" changes every day, so the expected due date would too |
| `LibraryRepository` is an interface | a concrete database class with no seam |

The test passes a **fixed clock**, so "today" is always 1 Oct 2026 and the due date always 15 Oct:

```java
private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
service = new LoanService(repository, notifier, FIXED_CLOCK);
```

## Given / when / then

```java
@Test
void borrowHappyPath() {
    // given: what the mocks will answer
    when(repository.countLoansOf("Asha")).thenReturn(0);
    when(repository.findBook(1)).thenReturn(Optional.of(cleanCode));
    when(repository.saveLoan(1, "Asha", TODAY, TODAY.plusDays(14))).thenReturn(new Loan(42, ...));

    // when: call the code under test
    Loan loan = service.borrow(1, "Asha");

    // then: the result AND the side effects
    assertThat(loan.id()).isEqualTo(42);
    assertThat(cleanCode.getCopiesAvailable()).isEqualTo(1);
    verify(notifier).send("Asha", "You borrowed \"Clean Code\". Please return it by 2026-10-15.");
}
```

## What the 7 tests cover

| Rule | Test | Checks |
|---|---|---|
| Borrowing works | `borrowHappyPath` | loan returned, copy taken, message text |
| 14-day loan | `borrowCapturesDueDate` | the due date passed to the repository (captor) |
| Max 3 books | `borrowLimit` | exception message; book never looked up; no message |
| Copy must be on the shelf | `borrowNoCopy` | nothing saved, nothing sent |
| Unknown book | `borrowUnknownBook` | `NoSuchElementException` + message |
| No fine on time | `giveBackOnTime` | 0, copy back, loan deleted, no message |
| 10 rupees per late day | `giveBackLate` | 30 rupees; delete happens before the message (`InOrder`) |

Every **unhappy path** gets its own test: they're where bugs hide. And they check what did **not**
happen (`never()`, `verifyNoInteractions`), not only what did.

## Speed

From the real test run: the 7 service tests took about **1 second** in total; the single full-application
test (`LibraryApplicationTest`) took about **14 seconds**, almost all of it starting Spring.

## Interview questions

- **How do you test code that uses the current time?** Inject a `Clock`; use `Clock.fixed` in tests.
- **Why constructor injection helps testing?** You can create the class with fakes using plain `new`.
- **What should a service test assert?** The return value, state changes, and the calls to collaborators (including the ones that must not happen).
