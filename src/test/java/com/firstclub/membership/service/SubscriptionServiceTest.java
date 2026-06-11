package com.firstclub.membership.service;

import com.firstclub.membership.dto.request.SubscribeRequest;
import com.firstclub.membership.dto.request.TierEvaluationRequest;
import com.firstclub.membership.dto.response.CatalogResponse;
import com.firstclub.membership.dto.response.MembershipResponse;
import com.firstclub.membership.exception.MembershipException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SubscriptionServiceTest {

	@Autowired
	private SubscriptionService subscriptionService;

	@Autowired
	private MembershipCatalogService catalogService;

	@Autowired
	private TierEvaluationService tierEvaluationService;

	private String planId;
	private String silverTierId;
	private String goldTierId;

	@BeforeEach
	void setUp() {
		CatalogResponse catalog = catalogService.getCatalog();
		planId = catalog.plans().get(0).id();
		silverTierId = catalog.tiers().stream()
				.filter(t -> "SILVER".equals(t.level()))
				.findFirst()
				.orElseThrow()
				.id();
		goldTierId = catalog.tiers().stream()
				.filter(t -> "GOLD".equals(t.level()))
				.findFirst()
				.orElseThrow()
				.id();
	}

	@Test
	void subscribeAndTrackMembership() {
		String userId = "user-1";

		MembershipResponse response = subscriptionService.subscribe(
				new SubscribeRequest(userId, planId, silverTierId));

		assertThat(response.userId()).isEqualTo(userId);
		assertThat(response.active()).isTrue();
		assertThat(response.tier().level()).isEqualTo("SILVER");

		MembershipResponse fetched = subscriptionService.getMembership(userId);
		assertThat(fetched.membershipId()).isEqualTo(response.membershipId());
	}

	@Test
	void upgradeAndDowngradeTier() {
		String userId = "user-2";
		subscriptionService.subscribe(new SubscribeRequest(userId, planId, silverTierId));

		MembershipResponse upgraded = subscriptionService.upgradeTier(userId);
		assertThat(upgraded.tier().level()).isEqualTo("GOLD");

		MembershipResponse downgraded = subscriptionService.downgradeTier(userId);
		assertThat(downgraded.tier().level()).isEqualTo("SILVER");
	}

	@Test
	void cancelMembership() {
		String userId = "user-3";
		subscriptionService.subscribe(new SubscribeRequest(userId, planId, goldTierId));

		MembershipResponse cancelled = subscriptionService.cancel(userId);
		assertThat(cancelled.status()).isEqualTo("CANCELLED");

		assertThatThrownBy(() -> subscriptionService.getMembership(userId))
				.isInstanceOf(com.firstclub.membership.exception.ResourceNotFoundException.class);
	}

	@Test
	void preventsDuplicateSubscription() {
		String userId = "user-4";
		subscriptionService.subscribe(new SubscribeRequest(userId, planId, silverTierId));

		assertThatThrownBy(() -> subscriptionService.subscribe(new SubscribeRequest(userId, planId, silverTierId)))
				.isInstanceOf(MembershipException.class)
				.hasMessageContaining("already has an active membership");
	}

	@Test
	void evaluateTierUpgradesBasedOnOrderMetrics() {
		String userId = "user-5";
		subscriptionService.subscribe(new SubscribeRequest(userId, planId, silverTierId));

		var result = tierEvaluationService.evaluateAndApply(userId,
				new TierEvaluationRequest(10, new BigDecimal("6000"), null));

		assertThat(result.tierUpgraded()).isTrue();
		assertThat(result.appliedTier()).isEqualTo("GOLD");
	}
}
