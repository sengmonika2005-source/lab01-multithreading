
package edu.itc.se.lab01.task3;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable {

    private final OrderQueue queue;
    private final AtomicInteger processedCount;

    public Consumer(OrderQueue queue,
                    AtomicInteger processedCount) {

        this.queue = queue;
        this.processedCount = processedCount;
    }

    @Override
    public void run() {
        try {
            while (true) {

                // Take an order from the buffer
                Order order = queue.take();

                // Stop when receiving a poison pill
                if (order.id() == -1) {
                    System.out.println(
                        Thread.currentThread().getName()
                        + " received poison pill, stopping"
                    );
                    break;
                }

                // Simulate processing (5-30 ms)
                Thread.sleep(
                    ThreadLocalRandom.current()
                        .nextInt(5, 31)
                );

                // Count the processed order
                processedCount.incrementAndGet();

                // Print the processed order
                System.out.println(
                    Thread.currentThread().getName()
                    + " processed " + order
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
