# Topic 4: Explore MockMvc

**MockMvc** sends HTTP requests to your Spring MVC controllers **without** starting a server or opening
a port. The request goes through the real `DispatcherServlet` (Session 8): URL mapping, JSON
conversion, validation, exception handlers. Only the network is skipped, so it's fast and exact.

## The shape of a MockMvc test

```java
mvc.perform(post("/api/loans")                                   // build the request
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\": 1, \"member\": \"Asha\"}"))
        .andExpect(status().isCreated())                         // check the response
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.dueOn").value("2026-10-15"));
```

| Part | Examples |
|---|---|
| Request builders | `get(url)`, `post(url)`, `put`, `patch`, `delete`, `.param("author", "x")`, `.header("X-Request-Id", "a")`, `.content(json)` |
| Status | `status().isOk()`, `isCreated()`, `isBadRequest()`, `isNotFound()`, `isConflict()` |
| JSON body | `jsonPath("$.name").value("Asha")`, `jsonPath("$", hasSize(2))`, `jsonPath("$.errors.email").exists()` |
| Headers, views | `header().string("Location", ...)`, `view().name("books")`, `model().attribute(...)` |
| Debug | `.andDo(print())` prints the whole request and response |

`jsonPath` uses JsonPath expressions: `$` is the root, `$.id` a field, `$[0].name` the first element's field.

## Two ways to get a MockMvc

| | `@WebMvcTest(LoanController.class)` | `@SpringBootTest` + `@AutoConfigureMockMvc` |
|---|---|---|
| Starts | only the web layer for that controller | the whole application |
| Services | must be provided: `@MockitoBean` mocks | the real ones |
| Speed | fast | slower |
| Tests | the controller's HTTP contract | the parts working together |
| In this module | [`LoanControllerTest`](../../src/test/java/jbdl/session14/LoanControllerTest.java) | [`LibraryApplicationTest`](../../src/test/java/jbdl/session14/LibraryApplicationTest.java) |

Sessions 8-13 used the second form throughout; Session 14 adds the first.

## Interview questions

- **Does MockMvc start a server?** No: it calls the DispatcherServlet directly in the same JVM.
- **`@WebMvcTest` vs `@SpringBootTest`?** A web-layer slice with mocked dependencies vs the full context.
- **How do you check a JSON field?** `jsonPath("$.field").value(...)`.
- **`@MockitoBean` vs `@Mock`?** `@MockitoBean` puts the mock into the Spring context (replacing the bean); `@Mock` is plain Mockito with no Spring. (`@MockitoBean` replaced Spring Boot's older `@MockBean`.)
