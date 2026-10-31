# Topic 5: Booking events and notifications

When a guest books, the host should hear about it. The booking code could call a notification service
directly, but then every new reaction (an e-mail, a calendar entry, analytics) would mean another change
in `BookingService`. Instead it **publishes an event** and does not care who listens (Session 20, topic 1).

## Publishing

[`BookingEvents`](../../../airbnb-backend/src/main/java/jbdl/airbnb/bookings/BookingEvents.java) holds two
records, `BookingConfirmed` and `BookingCancelled`. They carry plain values (ids, names, dates), not
entities. [`BookingService`](../../../airbnb-backend/src/main/java/jbdl/airbnb/bookings/BookingService.java)
publishes them with Spring's `ApplicationEventPublisher`:

```java
events.publishEvent(new BookingConfirmed(booking.getId(), listingId, listing.getTitle(),
        listing.getHost().getId(), guest.getName(), stay.checkIn(), stay.checkOut(), total));
```

These are **in-process** events: they never leave the application. Session 20 sent events between services
through Kafka; the same events could be forwarded there once other services need them.

## Listening, after the commit

[`HostNotifier`](../../../airbnb-backend/src/main/java/jbdl/airbnb/notifications/HostNotifier.java):

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(BookingConfirmed event) {
    inbox.add(event.hostId(), event.guestName() + " booked " + event.listingTitle() + " from " ...);
}
```

`publishEvent` is called inside the booking's transaction, but an `AFTER_COMMIT` listener runs only once
that transaction has **committed**. If it is rolled back (a database error, a constraint, anything after
the publish), the host is never told about a booking that does not exist. A plain `@EventListener` would
run immediately, inside the transaction, before anyone knows whether it will commit.

The notifications are kept in memory
([`NotificationInbox`](../../../airbnb-backend/src/main/java/jbdl/airbnb/notifications/NotificationInbox.java)),
and the logged-in user reads them at `GET /api/users/me/notifications`. A real app would store them or
send them right away.

## A real run

Asha's notifications after Ravi booked and cancelled, and Meera booked:

```
$ curl -u asha@example.com:asha-secret-1 localhost:8080/api/users/me/notifications
[{"message":"Ravi booked Sea view flat from 2026-11-30 to 2026-12-03 (10500.00 INR)","createdAt":"2026-10-31T06:42:48.328365Z"},
 {"message":"Ravi cancelled Sea view flat from 2026-11-30 to 2026-12-03","createdAt":"2026-10-31T06:42:49.122491600Z"},
 {"message":"Meera booked Sea view flat from 2026-12-02 to 2026-12-05 (10500.00 INR)","createdAt":"2026-10-31T06:42:49.296607Z"}]
```

Meera's refused first attempt (409, topic 1) left no notification.

## Tested

[`BookingEventsTest`](../../../airbnb-backend/src/test/java/jbdl/airbnb/BookingEventsTest.java):

- a booking tells the host; a refused booking tells nobody; a cancellation is told too;
- **AFTER_COMMIT**: the test publishes the same event in two transactions (`TransactionTemplate`). In the
  one that is rolled back (`setRollbackOnly()`), the host gets nothing; in the committed one, the message
  arrives.

## Interview questions

- **`@EventListener` vs. `@TransactionalEventListener`?** The first runs at once, inside the publisher's
  transaction; the second waits for a transaction phase (AFTER_COMMIT by default).
- **What if the listener fails after the commit?** The booking stays; the notification is lost. For must-
  not-lose side effects use a transactional outbox: the event is written to a table in the same
  transaction as the booking, and a separate job sends it afterwards and retries until it succeeds.
- **Why events instead of a direct call?** The booking code does not depend on every reaction; new
  reactions are added without touching it.
- **Why put plain values in events, not entities?** Listeners may run after the transaction, when lazy
  relations can no longer be loaded, and values cannot be changed by mistake.
