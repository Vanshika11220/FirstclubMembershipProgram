package com.firstclub.membership.concurrency;

import com.firstclub.membership.dto.request.SubscribeRequest;
import com.firstclub.membership.repository.UserMembershipRepository;
import com.firstclub.membership.service.MembershipCatalogService;
import com.firstclub.membership.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrencyTest {

	@Autowired
	private SubscriptionService subscriptionService;

	@Autowired
	private MembershipCatalogService catalogService;

	@Autowired
	private UserMembershipRepository membershipRepository;

	private String planId;
	private String tierId;

	@BeforeEach
	void setUp() {
		var catalog = catalogService.getCatalog();
		planId = catalog.plans().get(0).id();
		tierId = catalog.tiers().get(0).id();
	}

	@Test
	void concurrentSubscribeOnlyOneSucceeds() throws InterruptedException {
		String userId = "concurrent-user";
		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failureCount = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					startLatch.await();
					subscriptionService.subscribe(new SubscribeRequest(userId, planId, tierId));
					successCount.incrementAndGet();
				} catch (Exception e) {
					failureCount.incrementAndGet();
				} finally {
					doneLatch.countDown();
				}
			});
		}

		startLatch.countDown();
		doneLatch.await();
		executor.shutdown();

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(failureCount.get()).isEqualTo(threadCount - 1);
		assertThat(membershipRepository.findActiveByUserId(userId)).isPresent();
	}
}
