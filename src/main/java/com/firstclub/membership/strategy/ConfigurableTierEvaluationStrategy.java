package com.firstclub.membership.strategy;

import com.firstclub.membership.domain.enums.TierLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class ConfigurableTierEvaluationStrategy implements TierEvaluationStrategy {

	private static final int PLATINUM_MIN_ORDERS = 15;
	private static final BigDecimal PLATINUM_MIN_MONTHLY_VALUE = new BigDecimal("15000");
	private static final Set<String> PLATINUM_COHORTS = Set.of("PREMIUM", "VIP");

	private static final int GOLD_MIN_ORDERS = 5;
	private static final BigDecimal GOLD_MIN_MONTHLY_VALUE = new BigDecimal("5000");

	@Override
	public TierLevel evaluateEligibleTier(TierEvaluationContext context) {
		if (qualifiesForPlatinum(context)) {
			return TierLevel.PLATINUM;
		}
		if (qualifiesForGold(context)) {
			return TierLevel.GOLD;
		}
		return TierLevel.SILVER;
	}

	private boolean qualifiesForPlatinum(TierEvaluationContext context) {
		return context.orderCount() >= PLATINUM_MIN_ORDERS
				|| context.monthlyOrderValue().compareTo(PLATINUM_MIN_MONTHLY_VALUE) >= 0
				|| (context.cohort() != null && PLATINUM_COHORTS.contains(context.cohort().toUpperCase()));
	}

	private boolean qualifiesForGold(TierEvaluationContext context) {
		return context.orderCount() >= GOLD_MIN_ORDERS
				|| context.monthlyOrderValue().compareTo(GOLD_MIN_MONTHLY_VALUE) >= 0;
	}
}
