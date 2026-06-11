package com.firstclub.membership.dto.response;

import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.MembershipTier;

import java.math.BigDecimal;

public record PlanResponse(
		String id,
		String name,
		String duration,
		BigDecimal price) {

	public static PlanResponse from(MembershipPlan membershipPlan) {
		return new PlanResponse(
				membershipPlan.getId(),
				membershipPlan.getName(),
				membershipPlan.getDuration().name(),
				membershipPlan.getPrice()
		);
	}
}
