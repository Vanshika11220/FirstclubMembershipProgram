package com.firstclub.membership.concurrency;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Per-user fine-grained locking to serialize membership mutations for the same user
 * while allowing concurrent operations across different users.
 */
@Component
public class UserLockManager {

	private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>(); //userId, Lock Map

	public <T> T executeWithUserLock(String userId, Supplier<T> action) {
		ReentrantLock lock = locks.computeIfAbsent(userId, id -> new ReentrantLock());
		lock.lock();
		try {
			return action.get();
		} finally {
			lock.unlock();
			if (!lock.hasQueuedThreads()) {
				locks.remove(userId, lock);
			}
		}
	}
}
