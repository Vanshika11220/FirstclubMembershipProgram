package com.firstclub.membership.exception;

public class ConcurrentModificationException extends MembershipException {

	public ConcurrentModificationException(String message) {
		super("CONCURRENT_MODIFICATION", message);
	}
}
