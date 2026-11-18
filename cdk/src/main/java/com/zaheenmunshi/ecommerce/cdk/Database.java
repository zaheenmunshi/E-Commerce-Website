package com.zaheenmunshi.ecommerce.cdk;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.IConnectable;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.IInstanceEngine;
import software.amazon.awscdk.services.rds.ParameterGroup;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.rds.StorageType;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.constructs.Construct;

/**
 * PostgreSQL 16 on RDS: encrypted, TLS-only, reachable only from the app's security group
 * inside isolated subnets. Credentials are generated into Secrets Manager.
 */
public class Database extends Construct {

	public static final String NAME = "ecommerce";

	private final DatabaseInstance instance;

	public Database(Construct scope, String id, IVpc vpc, boolean deletionProtection) {
		super(scope, id);
		IInstanceEngine engine = DatabaseInstanceEngine
			.postgres(PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_16).build());

		ParameterGroup parameters = ParameterGroup.Builder.create(this, "Parameters")
			.engine(engine)
			.description("Require TLS for every connection")
			.parameters(Map.of("rds.force_ssl", "1"))
			.build();

		this.instance = DatabaseInstance.Builder.create(this, "Instance")
			.engine(engine)
			.instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.MICRO))
			.vpc(vpc)
			.vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_ISOLATED).build())
			.credentials(Credentials.fromGeneratedSecret("ecom_app"))
			.databaseName(NAME)
			.parameterGroup(parameters)
			.storageType(StorageType.GP3)
			.allocatedStorage(20)
			.maxAllocatedStorage(50)
			.storageEncrypted(true)
			.publiclyAccessible(false)
			.multiAz(false)
			.autoMinorVersionUpgrade(true)
			.backupRetention(Duration.days(7))
			.deletionProtection(deletionProtection)
			.removalPolicy(RemovalPolicy.SNAPSHOT)
			.cloudwatchLogsExports(List.of("postgresql"))
			.cloudwatchLogsRetention(RetentionDays.ONE_MONTH)
			.build();
	}

	public void allowConnectionsFrom(IConnectable peer) {
		instance.getConnections().allowDefaultPortFrom(peer, "app containers to PostgreSQL");
	}

	public String jdbcUrl() {
		return "jdbc:postgresql://" + endpoint() + ":" + instance.getDbInstanceEndpointPort() + "/" + NAME
				+ "?sslmode=require";
	}

	public String endpoint() {
		return instance.getDbInstanceEndpointAddress();
	}

	public ISecret secret() {
		return instance.getSecret();
	}

}
