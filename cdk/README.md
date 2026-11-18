# AWS infrastructure (CDK, Java)

`cdk/` describes the whole AWS setup in Java (AWS CDK v2). Nothing here runs by itself:
it only creates resources (and costs money) when someone runs `cdk deploy`.

## What it creates

| Stack | Resources |
|---|---|
| `Ecommerce` | VPC (2 AZs, public + isolated subnets, **no NAT gateway**), S3 bucket for product images (private, TLS only, versioned), RDS PostgreSQL 16 `db.t4g.micro` (encrypted, TLS only, isolated subnets, 7-day backups), ECS Fargate service (0.5 vCPU / 1 GB) behind an Application Load Balancer, Secrets Manager secrets for the DB login and the JWT signing key, CloudWatch logs (30 days), optional AWS WAF |
| `EcommerceGithubOidc` | GitHub OIDC identity provider + a deploy role only `zaheenmunshi/E-Commerce-Website` jobs in the `production` environment can assume (no AWS keys stored in GitHub) |

Security defaults: the container runs as a non-root user on a read-only filesystem, gets
its secrets from Secrets Manager at start (nothing baked into the image), can only read
and write its own S3 bucket, and the database accepts connections only from the app.

## Monthly cost estimate (ap-south-1, on-demand, approximate)

| Item | ~US$/month |
|---|---|
| Fargate task 0.5 vCPU + 1 GB, 24/7 | 18 |
| Application Load Balancer (hours + light traffic) | 18–22 |
| RDS `db.t4g.micro` single-AZ + 20 GB gp3 | 14–16 |
| Public IPv4 addresses (ALB ×2, task ×1) | 11 |
| Secrets Manager (2 secrets), CloudWatch logs, S3, ECR | 2–4 |
| **Total** | **≈ 63–71** |
| Optional WAF (`-c enableWaf=true`) | +9–10 |

A NAT gateway would add ~US$35/month, which is why the task runs in a public subnet with
a public IP (inbound traffic is still only allowed from the load balancer). Check the
AWS Free Tier / credits on your account; prices change, so confirm with the AWS Pricing
Calculator before deploying.

## Commands

Run everything from `cdk/` (Java 25 + Node needed; Maven comes via `./mvnw`):

```sh
./mvnw -q test                                 # unit tests (no AWS access)
npx aws-cdk@2.1148.1 synth                     # render CloudFormation into cdk.out/ (no AWS access)
npx aws-cdk@2.1148.1 diff Ecommerce            # compare with what is deployed (needs AWS login)
```

First deployment (needs an AWS account login; **this starts billing**):

```sh
npx aws-cdk@2.1148.1 bootstrap aws://<account-id>/ap-south-1
npx aws-cdk@2.1148.1 deploy EcommerceGithubOidc     # once: prints DeployRoleArn
npx aws-cdk@2.1148.1 deploy Ecommerce               # builds the Docker image, ~15 min
```

Then in GitHub → Settings: add the secret `AWS_DEPLOY_ROLE_ARN` (the role ARN printed
above) and an environment named `production` (limit it to the `main` branch and add a
required reviewer). After that, deploys run from the **Deploy** workflow.

## Options (CDK context)

| Key | Default | Meaning |
|---|---|---|
| `certificateArn` | empty | ACM certificate → HTTPS on 443, HTTP redirects, secure cookies. **Without it the site is plain HTTP — fine for a test, not for real customers.** |
| `domainName` | empty | Only used for the printed site URL; point the domain's DNS at the load balancer yourself. |
| `enableWaf` | `false` | AWS WAF managed rules (common exploits, bad inputs, SQL injection) + 1000 requests / 5 min per-IP limit. |
| `dbDeletionProtection` | `true` | Blocks deleting the database by accident. |
| `githubRepository` | `zaheenmunshi/E-Commerce-Website` | Repo allowed to use the deploy role. |

Example: `npx aws-cdk@2.1148.1 deploy Ecommerce -c certificateArn=arn:aws:acm:... -c domainName=shop.example.com -c enableWaf=true`

## Tearing down

`npx aws-cdk@2.1148.1 destroy Ecommerce` removes the app. The images bucket is kept
(RETAIN) and the database leaves a final snapshot; with `dbDeletionProtection=true`
switch it off first.
