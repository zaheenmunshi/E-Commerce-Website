package com.zaheenmunshi.ecommerce.cdk;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

class EcommerceStackTest {

	private static final StackProps PROPS = StackProps.builder()
		.env(Environment.builder().region("ap-south-1").build())
		.build();

	private static Template template;

	@BeforeAll
	static void synthesizeDefaultStack() {
		template = synthesize(new StackConfig("", "", false, true, "zaheenmunshi/E-Commerce-Website"));
	}

	@Test
	void imageBucketIsPrivateAndHttpsOnly() {
		template.hasResourceProperties("AWS::S3::Bucket",
				Map.of("PublicAccessBlockConfiguration", Map.of("BlockPublicAcls", true, "BlockPublicPolicy", true,
						"IgnorePublicAcls", true, "RestrictPublicBuckets", true)));
		template.hasResourceProperties("AWS::S3::BucketPolicy",
				Map.of("PolicyDocument", Match.objectLike(Map.of("Statement", Match.arrayWith(List.of(Match.objectLike(
						Map.of("Effect", "Deny", "Condition", Map.of("Bool", Map.of("aws:SecureTransport", "false"))))))))));
	}

	@Test
	void databaseIsEncryptedPrivateAndTlsOnly() {
		template.hasResourceProperties("AWS::RDS::DBInstance",
				Map.of("StorageEncrypted", true, "PubliclyAccessible", false, "DeletionProtection", true));
		template.hasResourceProperties("AWS::RDS::DBParameterGroup",
				Map.of("Parameters", Map.of("rds.force_ssl", "1")));
	}

	@Test
	void noNatGatewaysToKeepCostsDown() {
		template.resourceCountIs("AWS::EC2::NatGateway", 0);
	}

	@Test
	void containerIsHardenedAndGetsSecretsFromSecretsManager() {
		template.hasResourceProperties("AWS::ECS::TaskDefinition",
				Map.of("ContainerDefinitions", Match.arrayWith(List.of(Match.objectLike(Map.of(
						"ReadonlyRootFilesystem", true,
						"User", "10001",
						"Privileged", false,
						"Secrets", Match.arrayWith(List.of(Match.objectLike(Map.of("Name", "DB_PASSWORD")),
								Match.objectLike(Map.of("Name", "JWT_SECRET"))))))))));
	}

	@Test
	void plainHttpDeploymentDoesNotMarkCookiesSecureAndWarns() {
		template.hasResourceProperties("AWS::ECS::TaskDefinition",
				Map.of("ContainerDefinitions", Match.arrayWith(List.of(Match.objectLike(Map.of("Environment",
						Match.arrayWith(List.of(Map.of("Name", "COOKIE_SECURE", "Value", "false")))))))));
		template.hasOutput("HttpsWarning", Match.anyValue());
	}

	@Test
	void httpsDeploymentRedirectsHttpAndMarksCookiesSecure() {
		Template https = synthesize(new StackConfig("shop.example.com",
				"arn:aws:acm:ap-south-1:123456789012:certificate/abc", false, true, "zaheenmunshi/E-Commerce-Website"));
		https.hasResourceProperties("AWS::ElasticLoadBalancingV2::Listener",
				Map.of("Protocol", "HTTPS", "Port", 443));
		https.hasResourceProperties("AWS::ECS::TaskDefinition",
				Map.of("ContainerDefinitions", Match.arrayWith(List.of(Match.objectLike(Map.of("Environment",
						Match.arrayWith(List.of(Map.of("Name", "COOKIE_SECURE", "Value", "true")))))))));
	}

	@Test
	void wafOnlyWhenEnabled() {
		template.resourceCountIs("AWS::WAFv2::WebACL", 0);
		Template withWaf = synthesize(new StackConfig("", "", true, true, "zaheenmunshi/E-Commerce-Website"));
		withWaf.resourceCountIs("AWS::WAFv2::WebACL", 1);
		withWaf.resourceCountIs("AWS::WAFv2::WebACLAssociation", 1);
	}

	@Test
	void githubDeployRoleTrustsOnlyTheRepositoryProductionEnvironment() {
		App app = new App();
		Template oidc = Template
			.fromStack(new GithubOidcStack(app, "TestOidc", PROPS, "zaheenmunshi/E-Commerce-Website"));
		oidc.hasResourceProperties("AWS::IAM::Role",
				Map.of("AssumeRolePolicyDocument", Match.objectLike(Map.of("Statement", List.of(Match.objectLike(
						Map.of("Action", "sts:AssumeRoleWithWebIdentity", "Condition",
								Map.of("StringEquals", Map.of("token.actions.githubusercontent.com:aud",
										"sts.amazonaws.com", "token.actions.githubusercontent.com:sub",
										"repo:zaheenmunshi/E-Commerce-Website:environment:production")))))))));
	}

	private static Template synthesize(StackConfig config) {
		App app = new App();
		return Template.fromStack(new EcommerceStack(app, "TestEcommerce", PROPS, config));
	}

}
