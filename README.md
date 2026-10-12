# Balance Log — backend

Balance Log is a weight tracker. Log your weight whenever you step on the scale,
see the history and the curve, set a goal weight and follow your progress.

This repo is the Spring Boot REST API. The Vue frontend is in balance-log-frontend.

## Run

Needs Docker: bootRun starts Postgres from compose.yaml.

    ./gradlew bootRun

- your weight entries: GET http://localhost:8080/weight-entries with the token
- create an account: POST /auth/register with email and password
- log in: POST /auth/login returns a token

## Test

Needs Docker: the tests start their own Postgres with Testcontainers.

    ./gradlew test

Webtechnologien project, HTW Berlin, WiSe 2026/27.
