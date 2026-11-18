# CLAUDE.md — working on this repository

Clothing store: HTML/CSS/TypeScript frontend (`frontend/`), Spring Boot 4.1 REST API on
Java 25 / Amazon Corretto (`backend/`), PostgreSQL 16, AWS CDK in Java (`cdk/`). Read
`README.md` for the full picture; this file is the rulebook.

## Commands

```sh
./dev.sh                                   # FE :5173 (live reload, proxies API) + BE :8080 together
java scripts/Dev.java                      # same, cross-platform (Java 25 compact source file)
cd backend && ./mvnw verify                # unit (*Test) + integration (*IT, H2) tests — run before every commit
cd backend && ./mvnw spring-boot:run       # backend only
cd frontend && npm run typecheck && npm run build
cd frontend && npm run dev                 # frontend only
cd cdk && ./mvnw test && npx aws-cdk@2.1148.1 synth --quiet
```

Local DB: user-owned cluster `~/Library/PostgreSQL/16/data` on `localhost:5432`, database
`ecommerce`, role `ecom_app`; start with
`/Users/postgres/bin/pg_ctl -D ~/Library/PostgreSQL/16/data -l ~/Library/Logs/postgresql-16.log start`.
Settings and secrets come from `backend/.env` (template: `backend/.env.example`).

## Backend rules

- **Package by feature**: `auth`, `user`, `seller`, `product`, `order`, `storage`, each with
  `domain` (entities + repositories), `dto` (records), `service` (logic, `@Transactional`),
  `web` (controllers). Cross-cutting: `security`, `common`, `config`.
- Dependencies point inward: `web → service → domain`. Controllers stay thin — no business
  rules, no repositories in controllers.
- Request/response bodies are **records** in `dto` with Bean Validation messages written for
  the shopper (the frontend shows `detail` verbatim).
- User-facing errors: throw `common.error.ApiException` (`badRequest`, `notFound`, `conflict`, …);
  `ApiExceptionHandler` turns them into RFC 9457 problem responses. Never expose exception text.
- Constructor injection only; no field injection, no Lombok. Tabs for indentation in Java.
- Entities extend `common.domain.BaseEntity` (UUID id, timestamps); keep them DB-agnostic.
- Prices, totals and stock are always computed on the server.
- Use modern Java where it is clearer (records, sealed types + pattern `switch`, text blocks,
  `var`, `_`), not for show. KISS and SOLID: small classes, one responsibility, interfaces only
  where there is a real second implementation (e.g. `ImageStorage`).
- Versions come from BOMs (Spring Boot parent, AWS SDK BOM, JUnit BOM in `cdk`) — don't pin
  versions of managed dependencies.
- Every change needs tests: unit `*Test` or full-stack `*IT` extending `support.IntegrationTest`.

## Security rules (non-negotiable)

- Don't weaken `security/SecurityConfig.java`: no wildcard `permitAll` for `/api/**`, no CSP
  relaxation (`unsafe-inline`, external origins), no CORS opening, keep it stateless.
- **Every new endpoint** gets an explicit authorization rule in `SecurityConfig` **and** an
  integration test proving anonymous / wrong-role / other-owner access is refused.
- Ownership checks live in services (`findByIdAndSellerId`-style queries); someone else's id
  must look like "not found".
- Tokens: access token in JS memory only; refresh token only in the HttpOnly cookie. Never put
  tokens in localStorage/sessionStorage, URLs or logs. POSTs to `/api/auth/*` need
  `X-Requested-With`.
- Never log passwords, tokens or full request bodies.
- Never commit `backend/.env`, keys, certificates or credentials; never read them into output.

## Frontend rules

- Strict TypeScript (`tsconfig.json`), native ES modules with `.js` import suffixes, no
  frameworks or runtime dependencies.
- CSP forbids inline scripts, inline `on*` handlers and `style=""` attributes in markup — use
  modules, `addEventListener` and CSS classes (setting `element.style` from TS is fine).
- Escape every rendered value with `escapeHtml` (`src/lib/dom.ts`); images only via
  `safeImageUrl`. Use absolute asset paths (`/css/…`, `/img/…`, `/js/…`).
- All API access goes through `src/lib/api.ts` (typed calls) and `src/lib/auth.ts`
  (`authFetch`, `session`, refresh handling). Shared types live in `src/lib/types.ts`.
- One module per page in `src/pages/`; HTML pages in `public/`.

## Infrastructure & deployment

- `cdk/` is Java 25 CDK v2; `.github/workflows` has CI, CodeQL and a manual Deploy workflow.
- **Never run `cdk deploy`, `cdk bootstrap`, `cdk destroy` or any AWS API call without the
  user's explicit approval** — the stack costs ~US$63–71/month. `cdk synth` and tests are fine.

## Git

- Branches `feature/*`, `fix/*`, `chore/*`; Conventional Commit messages (`feat:`, `fix:`, …).
- **No AI attribution** in commit messages or PR descriptions (no `Co-Authored-By` AI lines,
  no "Generated with" footers).
- Never backdate commits, never force-push `main`, push only when the user asks.
