package com.firstclub.membership.exception;

public class ResourceNotFoundException extends MembershipException {

	public ResourceNotFoundException(String message) {
		super("NOT_FOUND", message);
	}
}
