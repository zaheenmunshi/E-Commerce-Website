package com.zaheenmunshi.ecommerce.cdk;

import java.util.List;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.LifecycleRule;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

/**
 * The site, sized for low cost (roughly US$63–71/month, see cdk/README.md): no NAT
 * gateways, one small Fargate task in public subnets behind an ALB, a single-AZ
 * t4g.micro PostgreSQL in isolated subnets, and a private S3 bucket for product images.
 */
public class EcommerceStack extends Stack {

	public EcommerceStack(Construct scope, String id, StackProps props, StackConfig config) {
		super(scope, id, props);

		Vpc vpc = Vpc.Builder.create(this, "Vpc")
			.maxAzs(2)
			.natGateways(0)
			.subnetConfiguration(List.of(
					SubnetConfiguration.builder().name("public").subnetType(SubnetType.PUBLIC).cidrMask(24).build(),
					SubnetConfiguration.builder()
						.name("data")
						.subnetType(SubnetType.PRIVATE_ISOLATED)
						.cidrMask(24)
						.build()))
			.build();

		Bucket images = Bucket.Builder.create(this, "ProductImages")
			.blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
			.enforceSsl(true)
			.encryption(BucketEncryption.S3_MANAGED)
			.versioned(true)
			.removalPolicy(RemovalPolicy.RETAIN)
			.lifecycleRules(List.of(LifecycleRule.builder()
				.abortIncompleteMultipartUploadAfter(Duration.days(7))
				.noncurrentVersionExpiration(Duration.days(90))
				.build()))
			.build();

		// 64 alphanumeric characters are valid base64: a 384-bit HMAC key for the JWTs
		Secret jwtSecret = Secret.Builder.create(this, "JwtSecret")
			.description("HMAC key that signs the API's JWT access tokens")
			.generateSecretString(SecretStringGenerator.builder()
				.passwordLength(64)
				.excludePunctuation(true)
				.includeSpace(false)
				.build())
			.build();

		Database database = new Database(this, "Database", vpc, config.dbDeletionProtection());
		WebApp web = new WebApp(this, "Web", new WebApp.Props(vpc, images, jwtSecret, database, config));
		database.allowConnectionsFrom(web.service().getService());

		if (config.enableWaf()) {
			new Waf(this, "Waf", web.service().getLoadBalancer().getLoadBalancerArn());
		}

		output("SiteUrl", web.siteUrl(), "Open this in a browser");
		output("ImagesBucket", images.getBucketName(), "Private S3 bucket with product images");
		output("DatabaseEndpoint", database.endpoint(), "PostgreSQL host (isolated subnets, no public access)");
		output("LogGroup", web.logGroupName(), "CloudWatch log group of the app");
		if (!config.https()) {
			output("HttpsWarning", "No certificateArn set: the site runs on plain HTTP and the refresh cookie is not "
					+ "marked Secure. Deploy with -c certificateArn=... before real customers use it.", "Security");
		}
	}

	private void output(String id, String value, String description) {
		new CfnOutput(this, id, CfnOutputProps.builder().value(value).description(description).build());
	}

}
