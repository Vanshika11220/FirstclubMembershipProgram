package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.TierLevel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MembershipTier {

	private final String id;
	private final String name;
	private final TierLevel level;
	private final List<Benefit> benefits;

	private MembershipTier(String id, String name, TierLevel level, List<Benefit> benefits) {
		this.id = id;
		this.name = name;
		this.level = level;
		this.benefits = Collections.unmodifiableList(benefits);
	}

	public static MembershipTier of(String id, String name, TierLevel level, List<Benefit> benefits) {
		return new MembershipTier(id, name, level, benefits);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public TierLevel getLevel() {
		return level;
	}

	public List<Benefit> getBenefits() {
		return benefits;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MembershipTier that)) {
			return false;
		}
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
