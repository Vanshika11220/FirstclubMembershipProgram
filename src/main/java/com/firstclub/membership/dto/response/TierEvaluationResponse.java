package com.firstclub.membership.dto.response;

public record TierEvaluationResponse(
		String eligibleTier,
		String previousTier,
		String appliedTier,
		boolean tierUpgraded,
		MembershipResponse membership) {
}
