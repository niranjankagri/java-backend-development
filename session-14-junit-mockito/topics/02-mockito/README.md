# Topic 2: Explore the Mockito framework

`LoanService` needs a `LibraryRepository` (data) and a `Notifier` (sends messages). In a unit test we
don't want a database or real messages; we want **fake** collaborators we control. Mockito creates them.

```
 test ──▶ LoanService ──▶ repository  (mock: returns what the test says)
                     └──▶ notifier    (mock: records the calls, sends nothing)
```

## The four things you do with a mock

| Step | Code | Meaning |
|---|---|---|
| Create | `@Mock private Notifier notifier;` + `@ExtendWith(MockitoExtension.class)` | a fake object of that type |
| **Stub** (teach it) | `when(repository.findBook(1)).thenReturn(Optional.of(book));` | "when called with 1, return this" |
| Stub an error | `when(service.borrow(anyLong(), anyString())).thenThrow(new IllegalStateException(...))` | simulate a failure |
| **Verify** (ask it) | `verify(notifier).send("Asha", "You borrowed ...");` | "were you called like this?" |

A mock that wasn't taught returns defaults: `null`, `0`, `false`, empty `Optional`/lists.

## Verifying in more detail

| Code | Checks |
|---|---|
| `verify(repository, never()).findBook(anyLong())` | never called |
| `verifyNoInteractions(notifier)` | not called at all |
| `ArgumentCaptor<LocalDate> dueDate = ArgumentCaptor.forClass(LocalDate.class); verify(repository).saveLoan(eq(1L), eq("Ravi"), eq(TODAY), dueDate.capture());` | grab the value passed in, then assert on it |
| `InOrder order = inOrder(repository, notifier); order.verify(repository).deleteLoan(6); order.verify(notifier).send(...)` | the calls happened in this order |

**Argument matchers:** `any()`, `anyLong()`, `anyString()`, `eq(...)`. If one argument uses a matcher,
all must (hence `eq(1L)` next to `dueDate.capture()`).

## Strict stubs

`MockitoExtension` fails a test that stubs something it never uses (`UnnecessaryStubbingException`).
That keeps tests honest: every `when(...)` matters. If a test really needs a lenient stub, use
`lenient().when(...)`.

## Mock, stub, spy, fake

| Word | Is |
|---|---|
| **Stub** | returns canned answers |
| **Mock** | a stub that also records calls for verification (Mockito mocks are both) |
| **Spy** | a real object where you override some methods (`spy(realObject)`); use rarely |
| **Fake** | a working, simplified implementation (our `InMemoryLibraryRepository`) |

## What NOT to mock

- Simple value objects (`Book`, `Loan`, records): just create them.
- The class under test itself.
- Things you don't own and can't model well (prefer a thin wrapper you can mock).

## Interview questions

- **`when().thenReturn()` vs `doReturn().when()`?** The second form is needed for void methods and spies.
- **What does `verify` check?** That a method was called, how often, with which arguments.
- **Mock vs spy?** All methods fake vs a real object with selected methods overridden.
- **Why is overusing mocks bad?** Tests then check implementation details; refactoring breaks them even when behaviour is unchanged.
