package ru.andreevcode.logicore.corelogistics.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import ru.andreevcode.logicore.corelogistics.data.ResponseHubDto;
import ru.andreevcode.logicore.corelogistics.BaseIT;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TransportHubServiceMultiThreadIT extends BaseIT {

    @Autowired
    TransportHubService transportHubService;

    @Test
    void testUpdateCapacityOptimisticLocking() throws InterruptedException {
        jdbcTemplate.update("""
                    INSERT INTO logistics.transport_hub(name, capacity, code) VALUES ('test-hub-1', 100, 'hub-1');
                """);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await(); // Ждем старта
                    transportHubService.updateCapacity(1L, -10);
                    successCount.incrementAndGet();
                } catch (OptimisticLockingFailureException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.countDown(); // Пли!
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(10, successCount.get());
        assertEquals(0, failureCount.get());

        assertThat(transportHubService.findAll())
                .containsExactlyInAnyOrder(new ResponseHubDto(1L, "test-hub-1", 0, "hub-1"));
    }

}
