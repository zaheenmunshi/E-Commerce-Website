package com.zaheenmunshi.ecommerce.cdk;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.services.wafv2.CfnWebACL;
import software.amazon.awscdk.services.wafv2.CfnWebACLAssociation;
import software.constructs.Construct;

/**
 * Optional AWS WAF in front of the load balancer: AWS managed rule groups for common
 * exploits, known bad inputs and SQL injection, plus a per-IP rate limit.
 */
public class Waf extends Construct {

	private static final int REQUESTS_PER_5_MINUTES_PER_IP = 1000;

	public Waf(Construct scope, String id, String loadBalancerArn) {
		super(scope, id);
		CfnWebACL acl = CfnWebACL.Builder.create(this, "WebAcl")
			.scope("REGIONAL")
			.defaultAction(CfnWebACL.DefaultActionProperty.builder()
				.allow(CfnWebACL.AllowActionProperty.builder().build())
				.build())
			.visibilityConfig(visibility("ecommerce-waf"))
			.rules(List.of(
					// product image uploads (up to 5 MB) exceed the rule set's 8 KB body limit, so count instead of block
					managedRuleGroup("AWSManagedRulesCommonRuleSet", 1, List.of("SizeRestrictions_BODY")),
					managedRuleGroup("AWSManagedRulesKnownBadInputsRuleSet", 2, List.of()),
					managedRuleGroup("AWSManagedRulesSQLiRuleSet", 3, List.of()), rateLimit(4)))
			.build();

		CfnWebACLAssociation.Builder.create(this, "Association")
			.resourceArn(loadBalancerArn)
			.webAclArn(acl.getAttrArn())
			.build();
	}

	private static CfnWebACL.RuleProperty managedRuleGroup(String name, int priority, List<String> countOnlyRules) {
		return CfnWebACL.RuleProperty.builder()
			.name(name)
			.priority(priority)
			.overrideAction(CfnWebACL.OverrideActionProperty.builder().none(Map.of()).build())
			.statement(CfnWebACL.StatementProperty.builder()
				.managedRuleGroupStatement(CfnWebACL.ManagedRuleGroupStatementProperty.builder()
					.vendorName("AWS")
					.name(name)
					.ruleActionOverrides(countOnlyRules.stream()
						.map(rule -> CfnWebACL.RuleActionOverrideProperty.builder()
							.name(rule)
							.actionToUse(CfnWebACL.RuleActionProperty.builder()
								.count(CfnWebACL.CountActionProperty.builder().build())
								.build())
							.build())
						.toList())
					.build())
				.build())
			.visibilityConfig(visibility(name))
			.build();
	}

	private static CfnWebACL.RuleProperty rateLimit(int priority) {
		return CfnWebACL.RuleProperty.builder()
			.name("RateLimitPerIp")
			.priority(priority)
			.action(CfnWebACL.RuleActionProperty.builder().block(CfnWebACL.BlockActionProperty.builder().build()).build())
			.statement(CfnWebACL.StatementProperty.builder()
				.rateBasedStatement(CfnWebACL.RateBasedStatementProperty.builder()
					.limit(REQUESTS_PER_5_MINUTES_PER_IP)
					.evaluationWindowSec(300)
					.aggregateKeyType("IP")
					.build())
				.build())
			.visibilityConfig(visibility("RateLimitPerIp"))
			.build();
	}

	private static CfnWebACL.VisibilityConfigProperty visibility(String metricName) {
		return CfnWebACL.VisibilityConfigProperty.builder()
			.metricName(metricName)
			.cloudWatchMetricsEnabled(true)
			.sampledRequestsEnabled(true)
			.build();
	}

}
