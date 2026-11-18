---
name: security-check
description: Checklist-driven security review of a change (diff, branch or files) against this shop's security controls — authorization rules, ownership checks, validation, token handling, output escaping, secrets, headers. Use before committing security-relevant changes or when asked for a security review.
---

# Security check

## 1. Scope

```sh
git status --short
git diff --stat main...HEAD      # or: git diff for uncommitted work
```

Read every changed file fully, plus `backend/src/main/java/com/zaheenmunshi/ecommerce/security/SecurityConfig.java`.

## 2. Checklist

**Backend**
- [ ] Every new/changed endpoint has an explicit rule in `SecurityConfig` (nothing new falls
      through to `anyRequest().permitAll()` under `/api`).
- [ ] Reads/writes of user-owned data are scoped to the caller (`findByIdAndSellerId`,
      `findByCustomerId...`); foreign ids return 404, not 403 or data.
- [ ] Request DTOs validate size/format; no entity is bound directly from JSON.
- [ ] Prices, totals, roles, stock and ownership are never taken from the client.
- [ ] Errors use `ApiException`; no exception messages/stack traces reach the client.
- [ ] No passwords/tokens/PII in logs.
- [ ] Uploads still go through `ImageUploadService` (magic bytes, size, random names).
- [ ] CSP / headers / stateless session / CSRF guard (`AuthEndpointGuardFilter`) unchanged
      or strengthened.

**Frontend**
- [ ] Every interpolated value in HTML strings goes through `escapeHtml`; image URLs through
      `safeImageUrl`.
- [ ] No `eval`, `new Function`, inline scripts, `on*` attributes or `style=""` in markup.
- [ ] Tokens only in memory (`lib/auth.ts`); nothing in localStorage except the cart/wishlist.
- [ ] Redirect targets validated (`nextPath()` style: starts with `/`, not `//`).

**Repo / infra**
- [ ] No secrets, `.env`, keys or real credentials added (`git diff | grep -iE "secret|password|BEGIN .*KEY"`).
- [ ] CDK: no public S3, no public DB, least-privilege IAM, no wildcard actions.

## 3. Commands

```sh
cd backend && ./mvnw verify
cd frontend && npm run typecheck && npm audit --audit-level=high
grep -rn "innerHTML\|insertAdjacentHTML" frontend/src | grep -v escapeHtml     # review each hit
grep -rn "permitAll" backend/src/main/java
```

## 4. Report

List findings as `file:line — severity (critical/high/medium/low) — problem — fix`, most
severe first. Say explicitly when a check could not be verified.
