package com.firstclub.membership.repository.inmemory;

import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.MembershipTier;
import com.firstclub.membership.repository.MembershipTierRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryMembershipTierRepository implements MembershipTierRepository {

	private final Map<String, MembershipTier> tiers = new ConcurrentHashMap<>();

	@Override
	public List<MembershipTier> findAll() {
		return List.copyOf(tiers.values());
	}

	@Override
	public Optional<MembershipTier> findById(String id) {
		return Optional.ofNullable(tiers.get(id));
	}

	@Override
	public Optional<MembershipTier> findByLevel(TierLevel level) {
		return tiers.values().stream()
				.filter(tier -> tier.getLevel() == level)
				.findFirst();
	}

	@Override
	public void save(MembershipTier tier) {
		tiers.put(tier.getId(), tier);
	}
}
