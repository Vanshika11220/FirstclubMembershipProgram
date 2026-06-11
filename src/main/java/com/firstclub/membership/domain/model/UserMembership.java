package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.MembershipStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class UserMembership {

	private final String id;
	private final String userId;
	private String planId;
	private String tierId;
	private MembershipStatus status;
	private final Instant startDate;
	private Instant expiryDate;
	private long version;

	public UserMembership(String userId, String planId, String tierId, Instant startDate, Instant expiryDate) {
		this.id = UUID.randomUUID().toString();
		this.userId = userId;
		this.planId = planId;
		this.tierId = tierId;
		this.status = MembershipStatus.ACTIVE;
		this.startDate = startDate;
		this.expiryDate = expiryDate;
		this.version = 0L;
	}

	public String getId() {
		return id;
	}

	public String getUserId() {
		return userId;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public String getTierId() {
		return tierId;
	}

	public void setTierId(String tierId) {
		this.tierId = tierId;
	}

	public MembershipStatus getStatus() {
		return status;
	}

	public void setStatus(MembershipStatus status) {
		this.status = status;
	}

	public Instant getStartDate() {
		return startDate;
	}

	public Instant getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Instant expiryDate) {
		this.expiryDate = expiryDate;
	}

	public long getVersion() {
		return version;
	}

	public void incrementVersion() {
		this.version++;
	}

	public boolean isActive() {
		return status == MembershipStatus.ACTIVE && expiryDate.isAfter(Instant.now());
	}

	public void cancel() {
		this.status = MembershipStatus.CANCELLED;
	}

	public void markExpiredIfNeeded() {
		if (status == MembershipStatus.ACTIVE && expiryDate.isBefore(Instant.now())) {
			this.status = MembershipStatus.EXPIRED;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UserMembership that)) {
			return false;
		}
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
