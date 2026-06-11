package com.firstclub.membership.dto.response;

import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserMembership;

import java.time.Instant;

public record MembershipResponse(
		String membershipId,
		String userId,
		String status,
		PlanResponse plan,
		TierResponse tier,
		Instant startDate,
		Instant expiryDate,
		boolean active) {

	public static MembershipResponse from(UserMembership membership, MembershipPlan plan, MembershipTier tier) {
		return new MembershipResponse(
				membership.getId(),
				membership.getUserId(),
				membership.getStatus().name(),
				PlanResponse.from(plan),
				TierResponse.from(tier),
				membership.getStartDate(),
				membership.getExpiryDate(),
				membership.isActive());
	}
}
