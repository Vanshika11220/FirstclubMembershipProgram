package com.firstclub.membership.exception;

public class MembershipException extends RuntimeException {

	private final String errorCode;

	public MembershipException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
