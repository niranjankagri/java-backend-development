# Topic 1: Implement expense creation and settlement flows

(This covers "Continue developing the Splitwise backend" and "Implement expense creation and
settlement flows".)

## Adding an expense

`POST /api/groups/{groupId}/expenses`

```json
{"description": "Taxi", "amount": 500.00, "paidByUserId": 3, "splitType": "EXACT",
 "participants": [{"userId": 1, "value": 200.00}, {"userId": 2, "value": 300.00}]}
```

[`ExpenseService.addExpense`](../../../splitwise-backend/src/main/java/jbdl/splitwise/expenses/ExpenseService.java), step by step:

1. Load the group (404 if unknown).
2. The payer and every participant must be members; nobody listed twice (409 otherwise).
3. Pick the **split strategy** for the type and compute the shares.
4. Save the expense; its shares are saved with it (cascade).

## Three split types: the Strategy pattern

```
           SplitStrategy (interface)          SplitStrategies.forType(type)   ← a small factory
          /          |            \
   EqualSplit    ExactSplit    PercentSplit
```

| Type | Input | Rule | 100.00 among 3 |
|---|---|---|---|
| [`EQUAL`](../../../splitwise-backend/src/main/java/jbdl/splitwise/expenses/split/EqualSplit.java) | nothing | round down, give the leftover paise to the first people | 33.34 + 33.33 + 33.33 |
| [`EXACT`](../../../splitwise-backend/src/main/java/jbdl/splitwise/expenses/split/ExactSplit.java) | amount per person | must add up to the total, else 409 | – |
| [`PERCENT`](../../../splitwise-backend/src/main/java/jbdl/splitwise/expenses/split/PercentSplit.java) | % per person | must add up to 100; the last person takes the rest | 33.34 %, 33.33 %, 33.33 % → exact |

The service never says `if (type == EQUAL) ... else if ...`: adding a fourth type ("by shares": 2 parts
for a couple, 1 for a single) means one new class.

**The paisa problem:** 100.00 / 3 = 33.333…; three times 33.33 is 99.99. A real app can't lose a
paisa. Both EQUAL and PERCENT guarantee the shares add up to the exact total, and
[`SplitAndSimplifyTest`](../../../splitwise-backend/src/test/java/jbdl/splitwise/SplitAndSimplifyTest.java) checks it.

## Balances

```
balance = paid for the group − own shares + repaid to others − received from others
```

Positive: the person should get money back. Negative: they must pay. All balances in a group add up to 0.

## Settle-up plan: the fewest payments

[`DebtSimplifier`](../../../splitwise-backend/src/main/java/jbdl/splitwise/expenses/DebtSimplifier.java)
repeatedly lets the biggest debtor pay the biggest creditor. With n people that's at most n − 1
payments, however many expenses there were.

## Recording a settlement

`POST /api/groups/{groupId}/settlements {"fromUserId", "toUserId", "amount"}` adds a `Settlement` row (the
expenses stay untouched). Refused if it wouldn't reduce a real debt: paying yourself, paying more than
you owe, or paying someone who isn't owed that much.

## The Goa trip, real run

```
POST hotel 6000 (Asha, EQUAL)  → shares Asha 2000.00, Ravi 2000.00, Meena 2000.00
POST dinner 900 (Ravi, EQUAL)
POST taxi 500 (Meena, EXACT)   → shares Asha 200.00, Ravi 300.00

GET  /balances   [{"name":"Asha","balance":3500.00},{"name":"Ravi","balance":-1700.00},{"name":"Meena","balance":-1800.00}]
GET  /settle-up  [{"from":"Meena","to":"Asha","amount":1800.00},{"from":"Ravi","to":"Asha","amount":1700.00}]
POST /settlements Ravi → Asha 1700.00
                 [{"name":"Asha","balance":1800.00},{"name":"Ravi","balance":0.00},{"name":"Meena","balance":-1800.00}]
POST /settlements Meena → Asha 2000.00
                 {"detail":"Meena can pay Asha at most 1800.00","status":409,"title":"Conflict"}
```

## Interview questions

- **Why the Strategy pattern here?** Each split rule is its own class; new rules don't touch existing code (open/closed principle).
- **How do you split money without losing paise?** Integer arithmetic on minor units, distribute the remainder explicitly.
- **How many payments does settling n people need?** At most n − 1 with the greedy method (finding the true minimum is a hard problem; greedy is good enough in practice).
- **Why record settlements instead of editing expenses?** History stays explainable; balances can be recomputed any time.
