package com.firstclub.membership.strategy;

import com.firstclub.membership.domain.enums.TierLevel;

/**
 * Strategy pattern: each implementation evaluates whether a user qualifies for a tier.
 */
public interface TierEvaluationStrategy {

	TierLevel evaluateEligibleTier(TierEvaluationContext context);
}
