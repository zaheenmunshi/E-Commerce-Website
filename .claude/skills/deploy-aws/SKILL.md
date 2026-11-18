---
name: deploy-aws
description: Prepare, check or perform an AWS deployment of the shop with the Java CDK in cdk/ (synth, diff, bootstrap, deploy, GitHub OIDC setup, teardown). Use when asked about deploying, AWS costs or infrastructure changes. Deploying costs money and REQUIRES explicit user approval.
---

# Deploy to AWS

**Stop rule:** never run `bootstrap`, `deploy` or `destroy` (or any AWS API call) unless the
user has explicitly approved that exact action in this conversation, after seeing the cost.
The stack costs about **US$63–71/month** in ap-south-1 (+US$9–10 with WAF). `synth` and
tests are free and safe.

## Safe checks (no AWS access)

```sh
cd cdk
./mvnw -q test
npx aws-cdk@2.1148.1 synth --quiet
```

Also make sure the app itself is green: `cd backend && ./mvnw verify`,
`cd frontend && npm run typecheck && npm run build`.

## Before the first deploy (with approval)

1. AWS login for the target account (`aws sts get-caller-identity`).
2. HTTPS: an ACM certificate ARN for the domain — without it the site is plain HTTP and the
   refresh cookie isn't `Secure`. Strongly recommended before real users.
3. Bootstrap once: `npx aws-cdk@2.1148.1 bootstrap aws://<account-id>/ap-south-1`
4. GitHub OIDC role once: `npx aws-cdk@2.1148.1 deploy EcommerceGithubOidc` → note `DeployRoleArn`;
   in GitHub add secret `AWS_DEPLOY_ROLE_ARN` and environment `production` (main only,
   required reviewer).

## Deploy (with approval)

```sh
cd cdk
npx aws-cdk@2.1148.1 diff Ecommerce
npx aws-cdk@2.1148.1 deploy Ecommerce -c certificateArn=arn:aws:acm:... -c domainName=shop.example.com [-c enableWaf=true]
```

or run the manual **Deploy** workflow in GitHub Actions. Afterwards check the `SiteUrl`
output and `/actuator/health`.

## Options & teardown

Context keys: `certificateArn`, `domainName`, `enableWaf` (false), `dbDeletionProtection`
(true), `githubRepository`. Teardown: `npx aws-cdk@2.1148.1 destroy Ecommerce` (bucket is
retained, DB leaves a final snapshot; disable deletion protection first). Details:
`cdk/README.md`.
