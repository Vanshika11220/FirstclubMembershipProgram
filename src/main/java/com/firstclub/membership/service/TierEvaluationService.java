package com.firstclub.membership.service;

import com.firstclub.membership.concurrency.UserLockManager;
import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserMembership;
import com.firstclub.membership.dto.request.TierEvaluationRequest;
import com.firstclub.membership.dto.response.MembershipResponse;
import com.firstclub.membership.dto.response.TierEvaluationResponse;
import com.firstclub.membership.exception.ConcurrentModificationException;
import com.firstclub.membership.exception.MembershipException;
import com.firstclub.membership.repository.MembershipTierRepository;
import com.firstclub.membership.repository.UserMembershipRepository;
import com.firstclub.membership.strategy.TierEvaluationContext;
import com.firstclub.membership.strategy.TierEvaluationStrategy;
import org.springframework.stereotype.Service;

@Service
public class TierEvaluationService {

	private static final int MAX_OPTIMISTIC_RETRIES = 3;

	private final UserMembershipRepository membershipRepository;
	private final MembershipTierRepository tierRepository;
	private final MembershipCatalogService catalogService;
	private final TierEvaluationStrategy evaluationStrategy;
	private final UserLockManager userLockManager;

	public TierEvaluationService(UserMembershipRepository membershipRepository,
			MembershipTierRepository tierRepository,
			MembershipCatalogService catalogService,
			TierEvaluationStrategy evaluationStrategy,
			UserLockManager userLockManager) {
		this.membershipRepository = membershipRepository;
		this.tierRepository = tierRepository;
		this.catalogService = catalogService;
		this.evaluationStrategy = evaluationStrategy;
		this.userLockManager = userLockManager;
	}

	public TierEvaluationResponse evaluateAndApply(String userId, TierEvaluationRequest request) {
		return userLockManager.executeWithUserLock(userId, () -> {
			TierEvaluationContext context = new TierEvaluationContext(
					userId,
					request.orderCount(),
					request.monthlyOrderValue(),
					request.cohort());

			TierLevel eligibleLevel = evaluationStrategy.evaluateEligibleTier(context);
			MembershipTier eligibleTier = tierRepository.findByLevel(eligibleLevel)
					.orElseThrow(() -> new MembershipException("TIER_NOT_CONFIGURED",
							"No tier configured for level: " + eligibleLevel));

			UserMembership membership = membershipRepository.findActiveByUserId(userId)
					.orElseThrow(() -> new com.firstclub.membership.exception.ResourceNotFoundException(
							"No active membership for user: " + userId));

			MembershipTier currentTier = catalogService.getTierOrThrow(membership.getTierId());
			boolean tierChanged = !eligibleTier.getId().equals(currentTier.getId())
					&& eligibleTier.getLevel().isHigherThan(currentTier.getLevel());

			if (tierChanged) {
				for (int attempt = 0; attempt < MAX_OPTIMISTIC_RETRIES; attempt++) {
					membership.setTierId(eligibleTier.getId());
					var saved = membershipRepository.saveWithVersionCheck(membership);
					if (saved.isPresent()) {
						membership = saved.get();
						break;
					}
					membership = membershipRepository.findActiveByUserId(userId)
							.orElseThrow(() -> new com.firstclub.membership.exception.ResourceNotFoundException(
									"No active membership for user: " + userId));
					if (attempt == MAX_OPTIMISTIC_RETRIES - 1) {
						throw new ConcurrentModificationException(
								"Could not apply tier upgrade due to concurrent modification");
					}
				}
			}

			var plan = catalogService.getPlanOrThrow(membership.getPlanId());
			var appliedTier = catalogService.getTierOrThrow(membership.getTierId());

			return new TierEvaluationResponse(
					eligibleLevel.name(),
					currentTier.getLevel().name(),
					appliedTier.getLevel().name(),
					tierChanged,
					MembershipResponse.from(membership, plan, appliedTier));
		});
	}
}
