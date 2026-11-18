package com.zaheenmunshi.ecommerce.cdk;

import software.constructs.Construct;

/**
 * Deployment options, read from CDK context (cdk.json defaults, override with
 * {@code -c key=value}).
 *
 * @param domainName optional public host name, only used for the site URL output
 * @param certificateArn optional ACM certificate; when set the load balancer serves HTTPS
 * @param enableWaf attach AWS WAF managed rules + rate limiting (extra cost, ~US$8+/month)
 * @param dbDeletionProtection block accidental deletion of the database
 * @param githubRepository "owner/repo" allowed to deploy through GitHub OIDC
 */
public record StackConfig(String domainName, String certificateArn, boolean enableWaf, boolean dbDeletionProtection,
		String githubRepository) {

	public static StackConfig from(Construct scope) {
		return new StackConfig(text(scope, "domainName", ""), text(scope, "certificateArn", ""),
				flag(scope, "enableWaf", false), flag(scope, "dbDeletionProtection", true),
				text(scope, "githubRepository", "zaheenmunshi/E-Commerce-Website"));
	}

	public boolean https() {
		return !certificateArn.isBlank();
	}

	private static String text(Construct scope, String key, String fallback) {
		Object value = scope.getNode().tryGetContext(key);
		return value == null ? fallback : value.toString().trim();
	}

	/** Context from cdk.json is a JSON boolean, from the command line it is a string. */
	private static boolean flag(Construct scope, String key, boolean fallback) {
		Object value = scope.getNode().tryGetContext(key);
		return value == null || value.toString().isBlank() ? fallback : Boolean.parseBoolean(value.toString());
	}

}
