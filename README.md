# Balance Log — backend

**A weight tracker.** Log your weight whenever you step on the scale, see the history and the curve, set a goal weight and follow your progress.

**Open the app: https://balance-log-frontend.onrender.com** — how to use it, with pictures, is in [balance-log-frontend](https://github.com/Reconnact/balance-log-frontend#how-to-use).

This repo is the Spring Boot REST API behind it, on https://balance-log-backend.onrender.com. On Render's free plan it sleeps after a quiet spell; the first request wakes it in up to 90 seconds.

## API

JSON in and out. Every route but register and login needs the token from login as `Authorization: Bearer <token>`; a token is valid for 7 days. Each account sees only its own weights and goal.

| method | path | body | answer |
|---|---|---|---|
| POST | `/auth/register` | `email`, `password` | 201, 400, 409 email taken |
| POST | `/auth/login` | `email`, `password` | 200 `{token, expiresAt}`, 401 |
| GET | `/profile` | — | 200 `{id, email}` |
| GET | `/weight-entries?from=&to=` | — | 200 list, oldest first |
| GET | `/weight-entries/{id}` | — | 200, 404 |
| POST | `/weight-entries` | `weightKg`, optional `measuredAt` | 201 with `Location`, 400 |
| PUT | `/weight-entries/{id}` | `weightKg`, `measuredAt` | 200, 400, 404 |
| DELETE | `/weight-entries/{id}` | — | 204, 404 |
| GET | `/goal` | — | 200 `{targetKg}`, 404 none set |
| PUT | `/goal` | `targetKg` | 200, 400 |
| GET | `/progress` | — | 200 `{latestKg, targetKg, kgToGo, kgPerWeek}`, 404 no goal or no entry |

- `from` and `to` are ISO instants, both optional.
- Without `measuredAt`, a POST takes the current time.
- Weights and the goal lie between 20 and 400 kg, with one decimal; `measuredAt` can't be in the future; a password has 8 to 72 characters.
- A 400 lists the message per field under `errors`.

The Postman collection in `postman/` has every route.

## Run

Needs Docker: bootRun starts Postgres from compose.yaml.

    ./gradlew bootRun

The API then answers on http://localhost:8080.

## Test

Needs Docker: the tests start their own Postgres with Testcontainers.

    ./gradlew test

GitHub Actions runs the tests on every push to main and on every pull request.

Webtechnologien project, HTW Berlin, WiSe 2026/27.
