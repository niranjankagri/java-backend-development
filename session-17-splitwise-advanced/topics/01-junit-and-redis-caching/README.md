# Topic 1: Integrate JUnit testing and Redis caching

## Tests at every level (Session 14 applied)

| Level | Class | What it proves |
|---|---|---|
| Unit (Mockito, no Spring) | [`ExpenseServiceTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/ExpenseServiceTest.java) | each rule alone: shares saved, payer must be a member, no duplicates, settlement saved / overpay refused |
| Unit (plain JUnit) | [`SplitAndSimplifyTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/SplitAndSimplifyTest.java) | the money maths, paisa-exact |
| Database | [`SchemaDesignTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/SchemaDesignTest.java) | the schema and mappings |
| API (MockMvc, full app) | [`UserAndGroupApiTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/UserAndGroupApiTest.java), [`ExpenseFlowApiTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/ExpenseFlowApiTest.java), [`SecurityTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/SecurityTest.java) | the flows, with login |
| Production-like | [`ProdProfileSmokeTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/ProdProfileSmokeTest.java) | the prod profile on a real PostgreSQL and Redis (Testcontainers) |

33 tests in the module. In the Mockito test, entities get their ids with `ReflectionTestUtils.setField(user, "id", 1L)`:
the database normally assigns them, and the first version of the test failed with a
`NullPointerException` because the mocked `save` returned an expense without one.

Shared helpers live in [`ApiTestSupport`](../../../splitwise-backend/src/test/java/jbdl/splitwise/ApiTestSupport.java):
`register(mvc, "Asha")` returns a `TestUser`, and `.with(as(asha))` logs in as her.

## Cached balances (Session 15 applied)

Balances are read far more often than they change (Session 13's HLSD), so they're cached per group.

```java
@Component
public class BalanceCalculator {
    @Cacheable(value = "balances", key = "#groupId")
    public LinkedHashMap<Long, BigDecimal> balances(long groupId) { ... four aggregate queries ... }
}
```

Three design points:

1. **A separate bean.** If `ExpenseService` cached its own method, its internal calls would skip the
   proxy and the cache (Session 9's self-invocation trap).
2. **Evict on every change** to a group's money or members:
   `@CacheEvict(value = "balances", key = "#groupId")` on `addExpense`, `recordSettlement`,
   `GroupService.addMember` and `removeMember`. The test `cacheIsEvictedOnChange` reads balances
   (cached), adds an expense, reads again (must be new), settles, reads again.
3. **Evict happens after the method returns.** `recordSettlement` used to return balances computed
   inside itself, which would have been the old cached ones. It now returns nothing and the controller
   asks for fresh balances afterwards.

The value is a `LinkedHashMap` (Serializable) so Redis can store it. Default cache: in memory;
`prod` profile: Redis with a 10-minute TTL. On the running Docker stack Redis held the key `balances::1`.
