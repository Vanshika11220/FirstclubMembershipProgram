package com.firstclub.membership.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SubscribeRequest(
		@NotBlank String userId,
		@NotBlank String planId,
		@NotBlank String tierId) {
}
