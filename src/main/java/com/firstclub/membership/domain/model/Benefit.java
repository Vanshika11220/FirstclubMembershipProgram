package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.BenefitType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Benefit {

	private final String id;
	private final BenefitType type;
	private final String description;
	private final Map<String, String> configuration;

	private Benefit(String id, BenefitType type, String description, Map<String, String> configuration) {
		this.id = id;
		this.type = type;
		this.description = description;
		this.configuration = Collections.unmodifiableMap(new HashMap<>(configuration));
	}

	public static Benefit of(String id, BenefitType type, String description, Map<String, String> configuration) {
		return new Benefit(id, type, description, configuration);
	}

	public String getId() {
		return id;
	}

	public BenefitType getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, String> getConfiguration() {
		return configuration;
	}

	public String getConfigValue(String key) {
		return configuration.get(key);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Benefit benefit)) {
			return false;
		}
		return Objects.equals(id, benefit.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
