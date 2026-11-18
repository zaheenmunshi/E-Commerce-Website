package com.zaheenmunshi.ecommerce.cdk;

import java.util.Optional;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;

/**
 * Entry point for {@code cdk synth / diff / deploy} (see cdk.json). Two stacks:
 * <ul>
 * <li>{@code Ecommerce}: the site (VPC, RDS PostgreSQL, S3 images, ECS Fargate + ALB).</li>
 * <li>{@code EcommerceGithubOidc}: a one-time stack letting GitHub Actions deploy without
 * stored AWS keys.</li>
 * </ul>
 */
public final class EcommerceApp {

	private EcommerceApp() {
	}

	public static void main(String[] args) {
		App app = new App();
		// Region is pinned (default Mumbai); the account comes from the credentials used to
		// deploy, so synth needs no AWS lookups (availability zones resolve via Fn::GetAZs).
		Environment environment = Environment.builder()
			.region(Optional.ofNullable(System.getenv("CDK_DEFAULT_REGION")).orElse("ap-south-1"))
			.build();
		StackConfig config = StackConfig.from(app);

		new EcommerceStack(app, "Ecommerce",
				StackProps.builder()
					.env(environment)
					.description("Clothing e-commerce site: Spring Boot on ECS Fargate, RDS PostgreSQL, S3")
					.build(),
				config);
		new GithubOidcStack(app, "EcommerceGithubOidc",
				StackProps.builder()
					.env(environment)
					.description("GitHub Actions OIDC deploy role for the e-commerce site")
					.build(),
				config.githubRepository());

		Tags.of(app).add("Project", "ecommerce");
		app.synth();
	}

}
