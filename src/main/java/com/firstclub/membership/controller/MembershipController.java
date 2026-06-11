package com.firstclub.membership.controller;

import com.firstclub.membership.dto.request.SubscribeRequest;
import com.firstclub.membership.dto.request.TierEvaluationRequest;
import com.firstclub.membership.dto.response.CatalogResponse;
import com.firstclub.membership.dto.response.MembershipResponse;
import com.firstclub.membership.dto.response.TierEvaluationResponse;
import com.firstclub.membership.service.MembershipCatalogService;
import com.firstclub.membership.service.SubscriptionService;
import com.firstclub.membership.service.TierEvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/membership")
public class MembershipController {

	private final MembershipCatalogService catalogService;
	private final SubscriptionService subscriptionService;
	private final TierEvaluationService tierEvaluationService;

//	costructor injection is preferred for better testability and immutability
	public MembershipController(MembershipCatalogService catalogService,
			SubscriptionService subscriptionService,
			TierEvaluationService tierEvaluationService) {
		this.catalogService = catalogService;
		this.subscriptionService = subscriptionService;
		this.tierEvaluationService = tierEvaluationService;
	}

	@GetMapping("/catalog")
	public CatalogResponse getCatalog() {
		return catalogService.getCatalog();
	}

	@PostMapping("/subscribe")
	@ResponseStatus(HttpStatus.CREATED)
	public MembershipResponse subscribe(@Valid @RequestBody SubscribeRequest request) {
		return subscriptionService.subscribe(request);
	}

	@GetMapping("/users/{userId}")
	public MembershipResponse getMembership(@PathVariable String userId) {
		return subscriptionService.getMembership(userId);
	}

	@PostMapping("/users/{userId}/upgrade")
	public MembershipResponse upgradeTier(@PathVariable String userId) {
		return subscriptionService.upgradeTier(userId);
	}

	@PostMapping("/users/{userId}/downgrade")
	public MembershipResponse downgradeTier(@PathVariable String userId) {
		return subscriptionService.downgradeTier(userId);
	}

	@PostMapping("/users/{userId}/cancel")
	public MembershipResponse cancel(@PathVariable String userId) {
		return subscriptionService.cancel(userId);
	}

	@PostMapping("/users/{userId}/evaluate-tier")
	public TierEvaluationResponse evaluateTier(@PathVariable String userId,
			@Valid @RequestBody TierEvaluationRequest request) {
		return tierEvaluationService.evaluateAndApply(userId, request);
	}
}
