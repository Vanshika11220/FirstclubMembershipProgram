package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.PlanDuration;

import java.math.BigDecimal;
import java.util.Objects;

public final class MembershipPlan {

	private final String id;
	private final String name;
	private final PlanDuration duration;
	private final BigDecimal price;
	private final boolean active;

	private MembershipPlan(String id, String name, PlanDuration duration, BigDecimal price, boolean active) {
		this.id = id;
		this.name = name;
		this.duration = duration;
		this.price = price;
		this.active = active;
	}

	public static MembershipPlan of(String id, String name, PlanDuration duration, BigDecimal price, boolean active) {
		return new MembershipPlan(id, name, duration, price, active);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public PlanDuration getDuration() {
		return duration;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public boolean isActive() {
		return active;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MembershipPlan that)) {
			return false;
		}
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
