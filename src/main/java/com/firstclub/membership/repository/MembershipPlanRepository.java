package com.firstclub.membership.repository;

import com.firstclub.membership.domain.model.MembershipPlan;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanRepository {

	List<MembershipPlan> findAllActive();

	Optional<MembershipPlan> findById(String id);

	void save(MembershipPlan plan);
}
