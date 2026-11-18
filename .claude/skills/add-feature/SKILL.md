---
name: add-feature
description: Recipe for adding a new end-to-end feature (entity, API endpoint, security rule, tests, TypeScript client and page) following this repo's package-by-feature layout. Use when asked to add or extend a backend endpoint, entity, page or user-facing feature.
---

# Add a feature slice

Read `CLAUDE.md` first. Base package: `backend/src/main/java/com/zaheenmunshi/ecommerce/`.

## Backend

1. **Domain** — `<feature>/domain/`: entity extending `common.domain.BaseEntity` (UUID id,
   timestamps), explicit `@Table`/`@Column` names (avoid SQL reserved words like `order`,
   `user`), `protected` no-arg constructor, intention-revealing methods instead of setters
   where possible. Repository = Spring Data interface in the same package; ownership-scoped
   finders such as `findByIdAndSellerId`.
2. **DTOs** — `<feature>/dto/`: request/response **records**; Bean Validation with messages
   the shopper can read; a `static from(Entity)` factory on responses.
3. **Service** — `<feature>/service/`: constructor injection, `@Transactional`
   (`readOnly = true` for reads), map entities → DTOs inside the transaction (open-in-view is
   off). User-facing failures: `throw ApiException.badRequest(...)`, `.notFound(...)`,
   `.conflict(...)`. Compute money/stock on the server.
4. **Controller** — `<feature>/web/`: thin `@RestController` under `/api/...`, `@Valid` bodies,
   `@AuthenticationPrincipal AppUserPrincipal me` for the caller; return `ResponseEntity`
   with the right status (`201` create, `204` delete).
5. **Security** — add an explicit rule in `security/SecurityConfig.java`
   (`permitAll` only for public reads; `hasRole("SELLER")` / `authenticated()` otherwise),
   placed before the `/api/**` catch-all.
6. **Tests** — `backend/src/test/java/.../<feature>/<Name>IT.java` extending
   `support.IntegrationTest` (helpers: `signup()`, `signupSeller()`, `authPost(...)`,
   `json(...)`, `clock.advance(...)`). Cover: happy path, validation error message,
   anonymous → 401, wrong role → 403, other user's resource → 404. Pure logic → `*Test`.
7. `cd backend && ./mvnw verify` — must be green.

## Frontend

1. Types in `frontend/src/lib/types.ts`.
2. Typed call in `frontend/src/lib/api.ts` — use `authFetch` from `lib/auth.ts` for protected
   endpoints, `requireJson`/`toApiError` from `lib/http.ts`.
3. Page logic in `frontend/src/pages/<page>.ts`; markup in `frontend/public/<page>.html` with
   one `<script type="module" src="/js/pages/<page>.js">`. Escape output with `escapeHtml`,
   images via `safeImageUrl`, events via `addEventListener` (CSP: no inline handlers/styles).
4. New pretty URL? Add a view controller in `config/WebConfig.java` (+ a test in
   `web/PagesAndHeadersIT.java`).
5. `cd frontend && npm run typecheck && npm run build`.

## Finish

Run the app (`run-local` skill) and exercise the feature through http://localhost:5173.
Update the API table in `README.md` if endpoints changed.
