# ordered-user-service

[![CI](https://github.com/ordered-system/ordered-user-service/actions/workflows/ci.yml/badge.svg)](https://github.com/ordered-system/ordered-user-service/actions/workflows/ci.yml)

Users, authentication, and address book for [ordered-system](https://github.com/ordered-system), extracted from the [`ordered-backend`](https://github.com/ordered-system/ordered-backend) monolith. Owns `user_db` (PostgreSQL) — the only service that can issue a JWT.

## What it does

- **Registration and login**, issuing stateless JWTs (JJWT, HS256) with custom claims — `userId` and `roles` — that [`ordered-gateway`](https://github.com/ordered-system/ordered-gateway) verifies at the edge and every downstream service reads via [`ordered-commons`](https://github.com/ordered-system/ordered-commons)' `JwtClaimsAuthenticationFilter`, without any service needing to call back here to check a session.
- **Roles**: `USER` / `SELLER` / `ADMIN`. Sellers self-promote through a dedicated endpoint (`AlreadySellerException` guards against re-promoting), which issues a **fresh** JWT carrying the new `ROLE_SELLER` claim — the old token stays valid with the old roles until it expires, by design of stateless JWTs.
- **Address book** (`AddressController`/`AddressService`) — the buyer's saved delivery addresses. Note this is distinct from the *snapshotted* delivery address stored on a placed `Order` over in `order-service`: editing an address here never rewrites history on past orders.

## API

Base path `/api/v1/users`, `/api/v1/auth`, `/api/v1/addresses`, reached through the gateway. `/api/v1/auth/**` is public (you can't authenticate with a token you don't have yet); everything else requires one. OpenAPI docs at `/v3/api-docs`.

## Stack

Java 21 · Spring Boot 4.1.0 · Spring Security · PostgreSQL + Flyway · JJWT (HS256) · Eureka Client · Spring Cloud Config Client · Micrometer / Prometheus / OpenTelemetry tracing · [`ordered-commons`](https://github.com/ordered-system/ordered-commons)

## Running it locally

```bash
git clone https://github.com/ordered-system/ordered-commons.git
(cd ordered-commons && make install)

git clone https://github.com/ordered-system/ordered-user-service.git
cd ordered-user-service
make up
make run
```

Runs on **port 9093**. Needs [`ordered-eureka`](https://github.com/ordered-system/ordered-eureka) and [`ordered-config-server`](https://github.com/ordered-system/ordered-config-server) reachable — the config server is what supplies the JWT signing secret, so this service and the gateway agree on it. Full stack: [`ordered-infra`](https://github.com/ordered-system/ordered-infra).

### Docker

The `Dockerfile` needs `ordered-commons` supplied as an additional build context — build via `ordered-infra`'s compose files rather than a bare `docker build .`.

## Testing

```bash
make test-unit
make test-integration
```

`RegisterLoginFlowIntegrationTest` covers register → login → token issuance end-to-end against a real Testcontainers Postgres.

## Where this fits

| Service | Database | Role |
|---|---|---|
| [ordered-order-service](https://github.com/ordered-system/ordered-order-service) | PostgreSQL | Orders, cart checkout, payments (Stripe) |
| [ordered-product-service](https://github.com/ordered-system/ordered-product-service) | PostgreSQL + Redis | Product catalog, stock reservation |
| **ordered-user-service** | PostgreSQL | Users, auth, JWT issuance |
| [ordered-engagement-service](https://github.com/ordered-system/ordered-engagement-service) | MongoDB | Reviews, browsing history |

Part of the [ordered-system](https://github.com/ordered-system) organization.

## License

MIT — see [LICENSE](LICENSE).
