# Topic 1: Project overview and requirement analysis

We build the backend of an **Airbnb-like** rental app: hosts offer places to stay, guests search
them for their dates and book them. It is the second project of the course (after Splitwise,
Sessions 12, 13 and 17) and runs over four sessions:

| Session | Part |
|---|---|
| 21 (this one) | requirements, architecture, schema, users, listings, search with availability |
| 22 | bookings (no double bookings), login, reviews, events |
| 23, 24 | deployment |

## The problem in one example

Asha has a flat in Goa and rents it out. Ravi wants a place in Goa for 2 people from 20 to 23
December, under 5,000 a night.

1. Asha registers and creates a listing: "Sea view flat", Goa, 4 guests, 3,500 a night.
2. Ravi searches: Goa, 2 guests, 20 to 23 December, up to 5,000. The flat is found **only if nobody
   else has booked it for any of those nights**.
3. Ravi books it (Session 22): 3 nights × 3,500 = 10,500. Nobody else can book those nights now.

Step 2 is the heart of this session: search must know about bookings, so the booking table is
designed now, even though booking through the API comes in Session 22.

## Requirements

**Functional** requirements say *what* the system does; **non-functional** ones say *how well*.

### Functional (with the session that delivers them)

| # | As a user I can... | Session |
|---|---|---|
| F1 | register with name and email (one account per email) | 21 |
| F2 | as a host: create a listing (title, city, guests, bedrooms, nightly price, amenities) | 21 |
| F3 | as a host: change my listing; take it off the market (deactivate); see my listings | 21 |
| F4 | search listings by city, guests and budget, sorted, page by page | 21 |
| F5 | search only listings free on my dates | 21 |
| F6 | book a listing for dates; the price is fixed at booking time | 22 |
| F7 | cancel a booking, which frees the dates | 22 |
| F8 | log in; only the host can change a listing, only the guest sees their bookings | 22 |
| F9 | review a stay after it ended | 22 |

### Non-functional

| # | Requirement | How |
|---|---|---|
| N1 | **No double bookings**, even when two guests book the same nights at the same moment | overlap rule (this session), database lock or constraint (Session 22) |
| N2 | Money is exact | `BigDecimal` with 2 decimals, as in Splitwise |
| N3 | Search stays fast with many listings | index on the city, paging (at most 50 per page), indexed booking dates |
| N4 | Invalid input gets a clear 4xx answer with the reason, never a 500 | validation + Problem Details |
| N5 | History is kept | listings are deactivated, never deleted; bookings keep the price agreed |
| N6 | Every rule is covered by automated tests | from day one |
| N7 | Runs in containers with a real database | Sessions 23 and 24 |

### Out of scope (for now)

Payments, photos, maps and distance search, prices per season, messages between host and guest,
currencies other than INR. Writing down what is **not** built is part of the analysis: it stops the
project from growing without anyone deciding it.

## Interview questions

- **Functional vs. non-functional requirements?** What the system does (search by city) vs. how well
  (search answers in under 200 ms, no double bookings ever).
- **What is the hardest requirement here, and why?** N1: two requests at the same moment both see the
  dates as free unless the database prevents it. It shapes the design of bookings (Session 22).
- **Why write down what is out of scope?** So everyone agrees what "done" means, and new wishes become
  explicit decisions.
