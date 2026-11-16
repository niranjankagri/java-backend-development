#!/usr/bin/env bash
# Session 24: a smoke test of a RUNNING Airbnb backend, from the outside, through its public API.
# It answers one question after a deployment: does the main path work there?
#
#   airbnb-backend/scripts/smoke-test.sh http://localhost:8080
#   airbnb-backend/scripts/smoke-test.sh https://airbnb-backend.onrender.com
#
# Steps: wait until ready, register a host and a guest, create a listing, find it in search, book it,
# book the same nights again (must be 409), deactivate the listing. Every run uses new emails and a new
# city, so it never collides with real data or an earlier run; the listing is deactivated at the end.
# Needs only bash and curl. Exit code 0 = passed, 1 = failed (with the step and the answer).

set -euo pipefail

BASE_URL="${1:?usage: smoke-test.sh BASE_URL}"
WAIT_SECONDS="${SMOKE_WAIT_SECONDS:-180}"   # a free cloud service may need a minute or two to wake up

RUN="$(date +%s)-$RANDOM"
PASSWORD="smoke-password-$RUN"
HOST_EMAIL="smoke-host-$RUN@example.com"
GUEST_EMAIL="smoke-guest-$RUN@example.com"
CITY="Smoke-$RUN"
CHECK_IN="$(date -u -d '+60 days' +%F)"
CHECK_OUT="$(date -u -d '+62 days' +%F)"
BODY="$(mktemp)"
trap 'rm -f "$BODY"' EXIT

# call EXPECTED_STATUS METHOD PATH [JSON] [EMAIL]: one request; stops the test on another status
call() {
    local expected="$1" method="$2" path="$3" json="${4:-}" email="${5:-}"
    local args=(-sS -o "$BODY" -w '%{http_code}' -X "$method" "$BASE_URL$path")
    if [[ -n "$json" ]]; then args+=(-H 'Content-Type: application/json' -d "$json"); fi
    if [[ -n "$email" ]]; then args+=(-u "$email:$PASSWORD"); fi
    local status
    status="$(curl "${args[@]}")"
    if [[ "$status" != "$expected" ]]; then
        echo "FAILED: $method $path answered $status, expected $expected"
        cat "$BODY"; echo
        exit 1
    fi
    echo "ok   $status $method $path"
}

# the "id" of the last answer (no jq needed: the first "id":<number> in the JSON)
last_id() {
    grep -o '"id":[0-9]*' "$BODY" | head -1 | cut -d: -f2
}

echo "Smoke test of $BASE_URL"

# 1. ready? (retry: right after a deployment or a cold start the service may not answer yet)
deadline=$((SECONDS + WAIT_SECONDS))
until curl -sf "$BASE_URL/actuator/health/readiness" | grep -q '"status":"UP"'; do
    if (( SECONDS > deadline )); then
        echo "FAILED: not ready after $WAIT_SECONDS s"
        exit 1
    fi
    sleep 5
done
echo "ok   ready"

# 2. the main path
call 201 POST /api/users "{\"name\":\"Smoke Host\",\"email\":\"$HOST_EMAIL\",\"password\":\"$PASSWORD\"}"
call 201 POST /api/users "{\"name\":\"Smoke Guest\",\"email\":\"$GUEST_EMAIL\",\"password\":\"$PASSWORD\"}"
call 201 POST /api/listings \
    "{\"title\":\"Smoke test flat\",\"city\":\"$CITY\",\"maxGuests\":2,\"bedrooms\":1,\"nightlyPrice\":1000}" "$HOST_EMAIL"
LISTING_ID="$(last_id)"

call 200 GET "/api/listings/search?city=$CITY&checkIn=$CHECK_IN&checkOut=$CHECK_OUT"
grep -q '"totalListings":1' "$BODY" || { echo "FAILED: search did not find the new listing"; cat "$BODY"; exit 1; }
echo "ok   search found listing $LISTING_ID"

BOOKING="{\"listingId\":$LISTING_ID,\"checkIn\":\"$CHECK_IN\",\"checkOut\":\"$CHECK_OUT\",\"guests\":2}"
call 201 POST /api/bookings "$BOOKING" "$GUEST_EMAIL"
call 409 POST /api/bookings "$BOOKING" "$GUEST_EMAIL"      # the same nights again: no double booking

# 3. leave nothing visible behind
call 204 DELETE "/api/listings/$LISTING_ID" "" "$HOST_EMAIL"

echo "PASSED"
