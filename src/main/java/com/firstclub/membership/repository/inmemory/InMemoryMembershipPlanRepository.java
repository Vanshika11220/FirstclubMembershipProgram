package com.firstclub.membership.repository.inmemory;

import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.repository.MembershipPlanRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryMembershipPlanRepository implements MembershipPlanRepository {

	private final Map<String, MembershipPlan> plans = new ConcurrentHashMap<>();

	@Override
	public List<MembershipPlan> findAllActive() {
		return plans.values().stream()
				.filter(MembershipPlan::isActive)
				.collect(Collectors.toList());
	}

	@Override
	public Optional<MembershipPlan> findById(String id) {
		return Optional.ofNullable(plans.get(id));
	}

	@Override
	public void save(MembershipPlan plan) {
		plans.put(plan.getId(), plan);
	}
}
