package com.firstclub.membership.config;

import com.firstclub.membership.domain.enums.PlanDuration;
import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.factory.BenefitFactory;
import com.firstclub.membership.repository.MembershipPlanRepository;
import com.firstclub.membership.repository.MembershipTierRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class MembershipDataInitializer {

	@Bean
	CommandLineRunner seedMembershipData(MembershipPlanRepository planRepository,
			MembershipTierRepository tierRepository) {
		return args -> {
			seedPlans(planRepository);
			seedTiers(tierRepository);
		};
	}

	private void seedPlans(MembershipPlanRepository planRepository) {
		if (!planRepository.findAllActive().isEmpty()) {
			return;
		}

		planRepository.save(MembershipPlan.of(
				"plan-monthly", "FirstClub Monthly", PlanDuration.MONTHLY, new BigDecimal("299.00"), true));
		planRepository.save(MembershipPlan.of(
				"plan-quarterly", "FirstClub Quarterly", PlanDuration.QUARTERLY, new BigDecimal("799.00"), true));
		planRepository.save(MembershipPlan.of(
				"plan-yearly", "FirstClub Yearly", PlanDuration.YEARLY, new BigDecimal("2499.00"), true));
	}

	private void seedTiers(MembershipTierRepository tierRepository) {
		if (!tierRepository.findAll().isEmpty()) {
			return;
		}

		tierRepository.save(MembershipTier.of(
				"tier-silver",
				"Silver",
				TierLevel.SILVER,
				List.of(
						BenefitFactory.freeDelivery("500"),
						BenefitFactory.discountPercentage("5"))));

		tierRepository.save(MembershipTier.of(
				"tier-gold",
				"Gold",
				TierLevel.GOLD,
				List.of(
						BenefitFactory.freeDelivery("0"),
						BenefitFactory.discountPercentage("10"),
						BenefitFactory.exclusiveDeals(),
						BenefitFactory.earlyAccessSales())));

		tierRepository.save(MembershipTier.of(
				"tier-platinum",
				"Platinum",
				TierLevel.PLATINUM,
				List.of(
						BenefitFactory.freeDelivery("0"),
						BenefitFactory.discountPercentage("15"),
						BenefitFactory.exclusiveDeals(),
						BenefitFactory.earlyAccessSales(),
						BenefitFactory.prioritySupport())));
	}
}
