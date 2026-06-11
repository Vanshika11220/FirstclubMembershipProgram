package com.firstclub.membership.dto.response;

import com.firstclub.membership.domain.model.Benefit;

import java.util.Map;

public record BenefitResponse(
		String id,
		String type,
		String description,
		Map<String, String> configuration) {

	public static BenefitResponse from(Benefit benefit) {
		return new BenefitResponse(
				benefit.getId(),
				benefit.getType().name(),
				benefit.getDescription(),
				benefit.getConfiguration());
	}
}
