package com.firstclub.membership.strategy;

import java.math.BigDecimal;

public record TierEvaluationContext(
		String userId,
		int orderCount,
		BigDecimal monthlyOrderValue,
		String cohort) {
}
