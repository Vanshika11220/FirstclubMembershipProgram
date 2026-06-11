package com.firstclub.membership.domain.enums;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public enum PlanDuration {
	MONTHLY(1),
	QUARTERLY(3),
	YEARLY(12);

	private final int months;

	PlanDuration(int months) {
		this.months = months;
	}

	public int getAmount() {
		return months;
	}

	public Instant addTo(Instant start) {
		return ZonedDateTime.ofInstant(start, ZoneOffset.UTC)
				.plusMonths(months)
				.toInstant();
	}
}
