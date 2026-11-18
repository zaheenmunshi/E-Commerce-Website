package com.zaheenmunshi.ecommerce.cdk;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ecr.assets.Platform;
import software.amazon.awscdk.services.ecs.AssetImageProps;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinition;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.ContainerInsights;
import software.amazon.awscdk.services.ecs.CpuArchitecture;
import software.amazon.awscdk.services.ecs.DeploymentCircuitBreaker;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LinuxParameters;
import software.amazon.awscdk.services.ecs.LogDrivers;
import software.amazon.awscdk.services.ecs.MountPoint;
import software.amazon.awscdk.services.ecs.OperatingSystemFamily;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.RuntimePlatform;
import software.amazon.awscdk.services.ecs.Secret;
import software.amazon.awscdk.services.ecs.Volume;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.SslPolicy;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.IBucket;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.constructs.Construct;

/**
 * The Spring Boot container (which also serves the frontend) on ECS Fargate behind an
 * Application Load Balancer. The container runs as a non-root user on a read-only root
 * filesystem; secrets are injected from Secrets Manager, never baked into the image.
 */
public class WebApp extends Construct {

	private static final int APP_PORT = 8080;

	private static final String APP_USER = "10001";

	public record Props(IVpc vpc, IBucket images, ISecret jwtSecret, Database database, StackConfig config) {
	}

	private final ApplicationLoadBalancedFargateService service;

	private final LogGroup logs;

	private final StackConfig config;

	public WebApp(Construct scope, String id, Props props) {
		super(scope, id);
		this.config = props.config();

		this.logs = LogGroup.Builder.create(this, "Logs")
			.retention(RetentionDays.ONE_MONTH)
			.removalPolicy(RemovalPolicy.DESTROY)
			.build();

		FargateTaskDefinition task = FargateTaskDefinition.Builder.create(this, "Task")
			.cpu(512)
			.memoryLimitMiB(1024)
			.runtimePlatform(RuntimePlatform.builder()
				.cpuArchitecture(CpuArchitecture.X86_64)
				.operatingSystemFamily(OperatingSystemFamily.LINUX)
				.build())
			.build();
		task.addVolume(Volume.builder().name("tmp").build());
		props.images().grantReadWrite(task.getTaskRole());

		ContainerDefinition app = task.addContainer("App", ContainerDefinitionOptions.builder()
			.image(ContainerImage.fromAsset(repositoryRoot().toString(),
					AssetImageProps.builder().file("Dockerfile").platform(Platform.LINUX_AMD64).build()))
			.portMappings(List.of(PortMapping.builder().containerPort(APP_PORT).build()))
			.environment(environment(props))
			.secrets(secrets(props))
			.logging(LogDrivers.awsLogs(AwsLogDriverProps.builder().logGroup(logs).streamPrefix("app").build()))
			.readonlyRootFilesystem(true)
			.user(APP_USER)
			.privileged(false)
			.linuxParameters(LinuxParameters.Builder.create(this, "Linux").initProcessEnabled(true).build())
			.build());
		// the only writable path: Tomcat and the JVM need a temp directory
		app.addMountPoints(MountPoint.builder().sourceVolume("tmp").containerPath("/tmp").readOnly(false).build());

		ApplicationLoadBalancedFargateService.Builder builder = ApplicationLoadBalancedFargateService.Builder
			.create(this, "Service")
			.cluster(Cluster.Builder.create(this, "Cluster").vpc(props.vpc()).containerInsightsV2(ContainerInsights.DISABLED).build())
			.taskDefinition(task)
			.desiredCount(1)
			.publicLoadBalancer(true)
			// public subnets + public IP instead of a NAT gateway (~US$35/month saved)
			.taskSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
			.assignPublicIp(true)
			.minHealthyPercent(100)
			.maxHealthyPercent(200)
			.healthCheckGracePeriod(Duration.seconds(120))
			.circuitBreaker(DeploymentCircuitBreaker.builder().rollback(true).build());
		if (config.https()) {
			builder.protocol(ApplicationProtocol.HTTPS)
				.certificate(Certificate.fromCertificateArn(this, "Certificate", config.certificateArn()))
				.redirectHttp(true)
				.sslPolicy(SslPolicy.RECOMMENDED_TLS);
		}
		this.service = builder.build();

		service.getTargetGroup()
			.configureHealthCheck(HealthCheck.builder()
				.path("/actuator/health")
				.healthyHttpCodes("200")
				.interval(Duration.seconds(30))
				.build());
		service.getTargetGroup().setAttribute("deregistration_delay.timeout_seconds", "30");
		service.getLoadBalancer().setAttribute("routing.http.drop_invalid_header_fields.enabled", "true");
	}

	public ApplicationLoadBalancedFargateService service() {
		return service;
	}

	public String logGroupName() {
		return logs.getLogGroupName();
	}

	public String siteUrl() {
		String host = config.domainName().isBlank() ? service.getLoadBalancer().getLoadBalancerDnsName()
				: config.domainName();
		return (config.https() ? "https://" : "http://") + host;
	}

	/** Must match the keys read by backend/src/main/resources/application.yml. Sorted for stable diffs. */
	private Map<String, String> environment(Props props) {
		return new TreeMap<>(Map.of("DB_URL", props.database().jdbcUrl(), "STORAGE_TYPE", "s3", "S3_BUCKET",
				props.images().getBucketName(), "AWS_REGION", Stack.of(this).getRegion(), "COOKIE_SECURE",
				String.valueOf(config.https()), "DEMO_DATA", "false", "MAIL_ENABLED", "false",
				"CHECK_BREACHED_PASSWORDS", "true"));
	}

	private static Map<String, Secret> secrets(Props props) {
		ISecret db = props.database().secret();
		return new TreeMap<>(Map.of("DB_USERNAME", Secret.fromSecretsManager(db, "username"), "DB_PASSWORD",
				Secret.fromSecretsManager(db, "password"), "JWT_SECRET", Secret.fromSecretsManager(props.jwtSecret())));
	}

	/** cdk commands run inside cdk/, so the Docker build context (the repo root) is one level up. */
	private static Path repositoryRoot() {
		return Path.of("..").toAbsolutePath().normalize();
	}

}
