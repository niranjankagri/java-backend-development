# Topic 3: Understand the project HLSD (High-Level System Design)

A high-level design shows the **parts** of the system, how they **talk**, and where the **data**
lives, without the code. It's the picture you draw before (and explain after) building.

## Today: one service

```
 ┌────────────┐   HTTPS + JSON   ┌───────────────────────── splitwise-backend ─────────────────────────┐
 │ mobile app │ ───────────────▶ │  controllers ─▶ services ─▶ repositories ─────────┐                │
 │ web app    │                  │  (users, groups, expenses, settle-up)              │                │
 └────────────┘                  └────────────────────────────────────────────────────│────────────────┘
                                                                                       ▼
                                                                        ┌──────────────────────────┐
                                                                        │ relational database      │
                                                                        │ H2 now, PostgreSQL later │
                                                                        └──────────────────────────┘
```

## Where it's heading (Sessions 15-24)

```
 clients ──▶ API gateway ──▶ splitwise-backend (several instances behind a load balancer)
              (Session 22)        │            │                │
                                  ▼            ▼                ▼
                            PostgreSQL     Redis cache     Kafka: "expense added" events
                                          (Session 15)     (Session 20) ──▶ notification service
```

## Key decisions and their reasons

| Decision | Why | Trade-off |
|---|---|---|
| A monolith first | One team, one codebase, simple deployment and debugging | Splitting later (Session 19) needs clear package boundaries: we have them (package by feature) |
| Relational database | Money needs transactions and constraints | Harder to scale writes than some NoSQL stores; fine for this load |
| Store shares, compute balances | One formula for every split type; history is the truth | Balance queries do work on each request → cache them (Session 15) |
| Stateless API | Any instance can serve any request; easy to scale out | Login state must live in tokens (Sessions 16, 18) |
| Append-only money records | Auditable | Corrections are new rows, not edits |

## Rough capacity check

Say 100,000 users, 5 groups each, 20 expenses per group per month: 10 million expense rows a month,
~4 per second on average, maybe 50 per second at peak. A single PostgreSQL handles that easily with
the indexes from topic 4; balances (read far more often than written) are the hot spot, hence caching.

## Interview questions

- **What goes into a high-level design?** Components, their interactions, data stores, external systems, and the key trade-offs.
- **Monolith or microservices first?** Usually a well-structured monolith, split when team or scaling needs prove it.
- **Why keep the API stateless?** Horizontal scaling and resilience: no sticky sessions.
- **Where would you cache, and what's the risk?** Balances per group; stale data → evict on every new expense or settlement.
