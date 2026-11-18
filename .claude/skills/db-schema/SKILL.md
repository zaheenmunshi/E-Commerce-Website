---
name: db-schema
description: Inspect or change the shop's database schema — tables, entities, local PostgreSQL access (psql/DBeaver), ddl-auto, H2 test mode, and the plan for versioned migrations. Use when asked about tables, data, schema changes, connecting to the DB, or choosing/moving to another database.
---

# Database schema

## Where the schema comes from

JPA entities (UUID ids, `BaseEntity` timestamps) in the `domain` packages:
`user/domain` (`app_users`, `user_identities`), `seller/domain` (`seller_profiles`),
`product/domain` (`products`, `product_images`, `product_sizes`, `product_tags`),
`order/domain` (`orders`, `order_items`), `security/refresh` (`refresh_tokens`).
Hibernate creates/updates tables at startup (`DB_DDL_AUTO=update`, see
`backend/src/main/resources/application.yml`).

## Connect locally

- Server: user-owned PostgreSQL 16 cluster `~/Library/PostgreSQL/16/data`, `localhost:5432`
  (start: `/Users/postgres/bin/pg_ctl -D ~/Library/PostgreSQL/16/data -l ~/Library/Logs/postgresql-16.log start`).
- App database `ecommerce`, login `ecom_app` (password = `DB_PASSWORD` in `backend/.env`;
  don't print it). DBeaver has a connection for it.
- psql without echoing the password:

```sh
cd backend && PGPASSWORD="$(grep '^DB_PASSWORD=' .env | cut -d= -f2-)" \
  /Users/postgres/bin/psql -h localhost -U ecom_app -d ecommerce -c '\dt'
```

Useful: `\d products`, `select count(*) from products;`, `\di` (indexes).

## Changing the schema

1. Change the entity (keep column names explicit, avoid reserved words, keep it DB-agnostic).
2. Update DTOs/services/tests; `cd backend && ./mvnw verify` (tests run on H2 in PostgreSQL
   mode with `create-drop`, see `backend/src/test/resources/application-test.yml`).
3. Restart the backend; `update` adds new tables/columns but never drops or renames — for
   renames/drops, migrate data manually or reset the dev DB (ask the user before dropping data).

## When the production database is chosen

Switch to versioned migrations: add Flyway (managed by the Spring Boot BOM, no version),
generate a baseline `V1__init.sql` from the current schema (`pg_dump --schema-only`), put
migrations in `backend/src/main/resources/db/migration`, and set `DB_DDL_AUTO=validate`.
For another database (e.g. MySQL), change `DB_URL` + the JDBC driver dependency; entities
need no changes.
