---
name: run-local
description: Start, check and stop the whole shop locally (PostgreSQL, Spring Boot backend on :8080, TypeScript frontend dev server on :5173). Use when asked to run, start, restart or test the app locally, or when the local app "doesn't work".
---

# Run the app locally

## 1. Database

```sh
nc -z localhost 5432 && echo "postgres up" || \
  /Users/postgres/bin/pg_ctl -D ~/Library/PostgreSQL/16/data -l ~/Library/Logs/postgresql-16.log start
```

(On other machines start PostgreSQL 16 with the platform's service manager.)
`backend/.env` must exist (copy `backend/.env.example`); don't print its contents.

## 2. Start frontend + backend

From the repo root, in the background:

```sh
./dev.sh                    # macOS / Linux
java scripts/Dev.java       # any OS (Java 25)
```

Wait for `[BE] ... Started EcommerceBackendApplication` and
`[FE] frontend dev server: http://localhost:5173`.

## 3. Verify

```sh
curl -s localhost:8080/actuator/health                                  # {"status":"UP"}
curl -s "localhost:5173/api/products?tag=men" | head -c 300             # JSON list (demo data if DEMO_DATA=true)
curl -s -o /dev/null -w '%{http_code}\n' localhost:5173/                 # 200
```

Log in as the demo seller `seller@demo.local` with `DEMO_SELLER_PASSWORD` from `backend/.env`.

## 4. Stop

Ctrl-C in the terminal running the script (it stops both processes). If a stray process
keeps a port: `lsof -nP -iTCP:8080 -sTCP:LISTEN` / `-iTCP:5173`, then stop that PID —
only if it is this project's process.

## Troubleshooting

| Symptom | Fix |
|---|---|
| "Port 8080/5173 is already in use" | another instance is running — find it with `lsof` (above) |
| Backend fails with a connection error | PostgreSQL isn't running (step 1) or `DB_PASSWORD` in `backend/.env` is wrong |
| Logged out after every backend restart | `JWT_SECRET` missing in `backend/.env` (`openssl rand -base64 48`) |
| Login works but the next page is logged out | `COOKIE_SECURE=true` on plain http — set `COOKIE_SECURE=false` locally |
| Page shows old JS | the frontend watcher isn't running; use `./dev.sh` or `cd frontend && npm run dev` |
| `java -version` isn't 25 | `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` |
