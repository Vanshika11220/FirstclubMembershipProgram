package com.firstclub.membership.dto.response;

import com.firstclub.membership.domain.model.MembershipTier;

import java.util.List;

public record TierResponse(
		String id,
		String name,
		String level,
		List<BenefitResponse> benefits) {

	public static TierResponse from(MembershipTier tier) {
		return new TierResponse(
				tier.getId(),
				tier.getName(),
				tier.getLevel().name(),
				tier.getBenefits().stream().map(BenefitResponse::from).toList());
	}
}
