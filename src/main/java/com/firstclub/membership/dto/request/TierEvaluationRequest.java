package com.firstclub.membership.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TierEvaluationRequest(
		@Min(0) int orderCount,
		@NotNull BigDecimal monthlyOrderValue,
		String cohort) {
}
