package com.zaheenmunshi.ecommerce.cdk;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.OidcProviderNative;
import software.amazon.awscdk.services.iam.OpenIdConnectPrincipal;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.constructs.Construct;

/**
 * Lets the GitHub "deploy" workflow get short-lived AWS credentials through OpenID
 * Connect, so no AWS access keys are stored in GitHub. Only jobs of the given repository
 * running in its "production" environment can assume the role, and the role can only
 * hand over to the CDK bootstrap roles (which do the actual deployment).
 * Deploy once by hand: {@code npx aws-cdk deploy EcommerceGithubOidc}.
 */
public class GithubOidcStack extends Stack {

	static final String GITHUB_ISSUER = "token.actions.githubusercontent.com";

	public GithubOidcStack(Construct scope, String id, StackProps props, String githubRepository) {
		super(scope, id, props);

		OidcProviderNative github = OidcProviderNative.Builder.create(this, "GithubProvider")
			.url("https://" + GITHUB_ISSUER)
			.clientIds(List.of("sts.amazonaws.com"))
			.build();

		Role deployRole = Role.Builder.create(this, "DeployRole")
			.description("Assumed by GitHub Actions (" + githubRepository + ", environment production) to run cdk deploy")
			.maxSessionDuration(Duration.hours(1))
			.assumedBy(new OpenIdConnectPrincipal(github,
					Map.of("StringEquals", Map.of(GITHUB_ISSUER + ":aud", "sts.amazonaws.com", GITHUB_ISSUER + ":sub",
							"repo:" + githubRepository + ":environment:production"))))
			.build();

		deployRole.addToPolicy(PolicyStatement.Builder.create()
			.effect(Effect.ALLOW)
			.actions(List.of("sts:AssumeRole"))
			.resources(List.of("arn:" + getPartition() + ":iam::" + getAccount() + ":role/cdk-*"))
			.build());

		new CfnOutput(this, "DeployRoleArn", CfnOutputProps.builder()
			.value(deployRole.getRoleArn())
			.description("Save as the GitHub secret AWS_DEPLOY_ROLE_ARN")
			.build());
	}

}
