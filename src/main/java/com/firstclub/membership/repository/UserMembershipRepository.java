package com.firstclub.membership.repository;

import com.firstclub.membership.domain.model.UserMembership;

import java.util.Optional;

public interface UserMembershipRepository {

	Optional<UserMembership> findActiveByUserId(String userId);

	Optional<UserMembership> findById(String id);

	UserMembership save(UserMembership membership);

	/**
	 * Persists changes only if the stored version matches the entity version.
	 *
	 * @return updated membership or empty if optimistic lock failed
	 */
	Optional<UserMembership> saveWithVersionCheck(UserMembership membership);
}
