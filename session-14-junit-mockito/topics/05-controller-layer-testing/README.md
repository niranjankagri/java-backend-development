# Topic 5: Implement controller-layer testing

A controller test answers one question: **does this class translate between HTTP and the service
correctly?** Not "are the business rules right" (that's the service test).

## What a controller test checks

| Responsibility of the controller | Test in [`LoanControllerTest`](../../src/test/java/jbdl/session14/LoanControllerTest.java) |
|---|---|
| URL + method map to the right service call | `borrow`: `POST /api/loans` → `service.borrow(1, "Asha")` |
| Status code | 201 on create, 200 on return |
| JSON in → arguments; result → JSON | `$.id`, `$.dueOn` (a `LocalDate` as `"2026-10-15"`) |
| Invalid input is stopped **before** the service | `invalidBody`: 400 and `verifyNoInteractions(service)` |
| Service exceptions → error responses | `notFound` (404 + message), `conflict` (409) |

```java
@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private LoanService service;     // the service is a mock: no rules run here

    @Test
    void notFound() throws Exception {
        when(service.borrow(anyLong(), anyString())).thenThrow(new NoSuchElementException("book 99 not found"));

        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"bookId\": 99, \"member\": \"Asha\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("book 99 not found"));
    }
}
```

The service is told to throw; the test checks only that the controller turns that into a 404 with the
message.

## The test pyramid

```
            /\        few:   full-application / end-to-end tests   (LibraryApplicationTest: 1)
           /  \       some:  controller slices with MockMvc         (LoanControllerTest: 5)
          /____\      many:  unit tests of services and objects     (LoanServiceTest 7, JUnitBasicsTest 10)
```

Many fast unit tests for the rules, fewer slice tests for each layer's contract, a handful of
integration tests to prove the wiring. Session 17 applies this to the Splitwise backend.

## Interview questions

- **What belongs in a controller test?** Mapping, status codes, JSON, validation, error handling, not business rules.
- **Why mock the service in a controller test?** To test the controller alone and choose any outcome (including errors) easily.
- **What is the test pyramid?** Many unit tests, fewer integration tests, very few end-to-end tests.
