package com.firstclub.membership.service;

import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.dto.response.CatalogResponse;
import com.firstclub.membership.dto.response.PlanResponse;
import com.firstclub.membership.dto.response.TierResponse;
import com.firstclub.membership.repository.MembershipPlanRepository;
import com.firstclub.membership.repository.MembershipTierRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MembershipCatalogService {

	private final MembershipPlanRepository planRepository;
	private final MembershipTierRepository tierRepository;

	public MembershipCatalogService(MembershipPlanRepository planRepository,
			MembershipTierRepository tierRepository) {
		this.planRepository = planRepository;
		this.tierRepository = tierRepository;
	}

	public CatalogResponse getCatalog() {
		List<PlanResponse> plans = planRepository.findAllActive().stream()
				.sorted(Comparator.comparing(plan -> plan.getDuration().getAmount()))
				.map(this::toPlanResponse)
				.toList();

		List<TierResponse> tiers = tierRepository.findAll().stream()
				.sorted(Comparator.comparing(tier -> tier.getLevel().getRank()))
				.map(this::toTierResponse)
				.toList();

		return new CatalogResponse(plans, tiers);
	}

	public MembershipPlan getPlanOrThrow(String planId) {
		return planRepository.findById(planId)
				.filter(MembershipPlan::isActive)
				.orElseThrow(() -> new com.firstclub.membership.exception.ResourceNotFoundException(
						"Membership plan not found: " + planId));
	}

	public MembershipTier getTierOrThrow(String tierId) {
		return tierRepository.findById(tierId)
				.orElseThrow(() -> new com.firstclub.membership.exception.ResourceNotFoundException(
						"Membership tier not found: " + tierId));
	}

	public MembershipTier getTierByLevelOrThrow(TierLevel level) {
		return tierRepository.findByLevel(level)
				.orElseThrow(() -> new com.firstclub.membership.exception.ResourceNotFoundException(
						"Membership tier not configured for level: " + level));
	}

	private PlanResponse toPlanResponse(MembershipPlan plan) {
		return PlanResponse.from(plan);
	}

	private TierResponse toTierResponse(MembershipTier tier) {
		return TierResponse.from(tier);
	}
}
