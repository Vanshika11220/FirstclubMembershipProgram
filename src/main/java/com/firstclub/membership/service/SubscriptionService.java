package com.firstclub.membership.service;

import com.firstclub.membership.concurrency.UserLockManager;
import com.firstclub.membership.domain.enums.MembershipStatus;
import com.firstclub.membership.domain.enums.PlanDuration;
import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.domain.model.UserMembership;
import com.firstclub.membership.dto.request.SubscribeRequest;
import com.firstclub.membership.dto.response.MembershipResponse;
import com.firstclub.membership.exception.ConcurrentModificationException;
import com.firstclub.membership.exception.MembershipException;
import com.firstclub.membership.repository.UserMembershipRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SubscriptionService {

	private static final int MAX_OPTIMISTIC_RETRIES = 3;

	private final UserMembershipRepository membershipRepository;
	private final MembershipCatalogService catalogService;
	private final UserLockManager userLockManager;

	public SubscriptionService(UserMembershipRepository membershipRepository,
			MembershipCatalogService catalogService,
			UserLockManager userLockManager) {
		this.membershipRepository = membershipRepository;
		this.catalogService = catalogService;
		this.userLockManager = userLockManager;
	}

	public MembershipResponse subscribe(SubscribeRequest request) {
		return userLockManager.executeWithUserLock(request.userId(), () -> {
			membershipRepository.findActiveByUserId(request.userId()).ifPresent(existing -> {
				throw new MembershipException("ALREADY_SUBSCRIBED",
						"User already has an active membership. Cancel or wait for expiry before re-subscribing.");
			});

			MembershipPlan plan = catalogService.getPlanOrThrow(request.planId());
			MembershipTier tier = catalogService.getTierOrThrow(request.tierId());

			Instant start = Instant.now();
			Instant expiry = calculateExpiry(start, plan.getDuration());

			UserMembership membership = new UserMembership(
					request.userId(), plan.getId(), tier.getId(), start, expiry);

			return MembershipResponse.from(membershipRepository.save(membership), plan, tier);
		});
	}

	public MembershipResponse getMembership(String userId) {
		UserMembership membership = membershipRepository.findActiveByUserId(userId)
				.orElseThrow(() -> new com.firstclub.membership.exception.ResourceNotFoundException(
						"No active membership for user: " + userId));

		membership.markExpiredIfNeeded();
		if (membership.getStatus() == MembershipStatus.EXPIRED) {
			membershipRepository.save(membership);
			throw new com.firstclub.membership.exception.ResourceNotFoundException(
					"Membership expired for user: " + userId);
		}

		MembershipPlan plan = catalogService.getPlanOrThrow(membership.getPlanId());
		MembershipTier tier = catalogService.getTierOrThrow(membership.getTierId());
		return MembershipResponse.from(membership, plan, tier);
	}

	public MembershipResponse upgradeTier(String userId) {
		return changeTier(userId, Direction.UPGRADE);
	}

	public MembershipResponse downgradeTier(String userId) {
		return changeTier(userId, Direction.DOWNGRADE);
	}

	public MembershipResponse cancel(String userId) {
		return userLockManager.executeWithUserLock(userId, () ->
				updateMembershipWithRetry(userId, membership -> {
					if (!membership.isActive()) {
						throw new MembershipException("NOT_ACTIVE", "No active membership to cancel");
					}
					membership.cancel();
				}));
	}

	private MembershipResponse changeTier(String userId, Direction direction) {
		return userLockManager.executeWithUserLock(userId, () ->
				updateMembershipWithRetry(userId, membership -> {
					if (!membership.isActive()) {
						throw new MembershipException("NOT_ACTIVE", "No active membership to modify");
					}

					MembershipTier currentTier = catalogService.getTierOrThrow(membership.getTierId());
					TierLevel newLevel = direction == Direction.UPGRADE
							? currentTier.getLevel().upgrade()
							: currentTier.getLevel().downgrade();

					if (newLevel == currentTier.getLevel()) {
						throw new MembershipException("TIER_LIMIT",
								"Cannot " + direction.name().toLowerCase() + " beyond " + currentTier.getName());
					}

					MembershipTier newTier = catalogService.getTierByLevelOrThrow(newLevel);
					membership.setTierId(newTier.getId());
				}));
	}

	private MembershipResponse updateMembershipWithRetry(String userId,
			java.util.function.Consumer<UserMembership> mutator) {
		for (int attempt = 0; attempt < MAX_OPTIMISTIC_RETRIES; attempt++) {
			UserMembership membership = membershipRepository.findActiveByUserId(userId)
					.orElseThrow(() -> new com.firstclub.membership.exception.ResourceNotFoundException(
							"No active membership for user: " + userId));

			mutator.accept(membership);

			var saved = membershipRepository.saveWithVersionCheck(membership);
			if (saved.isPresent()) {
				MembershipPlan plan = catalogService.getPlanOrThrow(saved.get().getPlanId());
				MembershipTier tier = catalogService.getTierOrThrow(saved.get().getTierId());
				return MembershipResponse.from(saved.get(), plan, tier);
			}
		}
		throw new ConcurrentModificationException(
				"Membership was modified by another request. Please retry.");
	}

	private Instant calculateExpiry(Instant start, PlanDuration duration) {
		return duration.addTo(start);
	}

	private enum Direction {
		UPGRADE, DOWNGRADE
	}
}
