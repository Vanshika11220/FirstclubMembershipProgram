package com.firstclub.membership.factory;

import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.model.Benefit;

import java.util.Map;
import java.util.UUID;

/**
 * Factory for creating consistently configured membership benefits.
 */
public final class BenefitFactory {

	private BenefitFactory() {
	}

	public static Benefit freeDelivery(String minOrderValue) {
		return Benefit.of(
				UUID.randomUUID().toString(),
				BenefitType.FREE_DELIVERY,
				"Free delivery on eligible orders",
				Map.of("minOrderValue", minOrderValue));
	}

	public static Benefit discountPercentage(String percentage) {
		return Benefit.of(
				UUID.randomUUID().toString(),
				BenefitType.DISCOUNT_PERCENTAGE,
				percentage + "% extra discount on selected items",
				Map.of("percentage", percentage));
	}

	public static Benefit exclusiveDeals() {
		return Benefit.of(
				UUID.randomUUID().toString(),
				BenefitType.EXCLUSIVE_DEALS,
				"Access to exclusive member deals",
				Map.of());
	}

	public static Benefit earlyAccessSales() {
		return Benefit.of(
				UUID.randomUUID().toString(),
				BenefitType.EARLY_ACCESS_SALES,
				"Early access to sales events",
				Map.of("hoursEarly", "24"));
	}

	public static Benefit prioritySupport() {
		return Benefit.of(
				UUID.randomUUID().toString(),
				BenefitType.PRIORITY_SUPPORT,
				"Priority customer support",
				Map.of("responseTimeMinutes", "15"));
	}
}
