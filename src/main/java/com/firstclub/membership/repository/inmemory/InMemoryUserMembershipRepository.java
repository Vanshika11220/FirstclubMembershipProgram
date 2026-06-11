package com.firstclub.membership.repository.inmemory;

import com.firstclub.membership.domain.enums.MembershipStatus;
import com.firstclub.membership.domain.model.UserMembership;
import com.firstclub.membership.repository.UserMembershipRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserMembershipRepository implements UserMembershipRepository {

	private final Map<String, UserMembership> byId = new ConcurrentHashMap<>(); //id(UUID), userMembership
	private final Map<String, String> activeUserIndex = new ConcurrentHashMap<>(); //userId, id(UUID)

	@Override
	public Optional<UserMembership> findActiveByUserId(String userId) {
		return Optional.ofNullable(activeUserIndex.get(userId))
				.map(byId::get)
				.filter(UserMembership::isActive);
	}

	@Override
	public Optional<UserMembership> findById(String id) {
		return Optional.ofNullable(byId.get(id));
	}

	@Override
	public synchronized UserMembership save(UserMembership membership) {
		byId.put(membership.getId(), membership);
		if (membership.getStatus() == MembershipStatus.ACTIVE && membership.isActive()) {
			activeUserIndex.put(membership.getUserId(), membership.getId());
		} else {
			activeUserIndex.remove(membership.getUserId(), membership.getId());
		}
		return membership;
	}

	@Override
	public synchronized Optional<UserMembership> saveWithVersionCheck(UserMembership membership) {
		UserMembership existing = byId.get(membership.getId());
		if (existing == null) {
			return Optional.of(save(membership));
		}
		if (existing.getVersion() != membership.getVersion()) {
			return Optional.empty();
		}
		membership.incrementVersion();
		return Optional.of(save(membership));
	}
}
