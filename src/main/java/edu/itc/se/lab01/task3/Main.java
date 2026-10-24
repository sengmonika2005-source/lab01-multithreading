
package edu.itc.se.lab01.task3;

import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args)
            throws InterruptedException {

        long start = System.nanoTime();

        // Buffer capacity = 5
        OrderQueue queue = new BoundedBuffer(5);

        // Shared counters
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger producersRemaining = new AtomicInteger(2);

        // Create two producers, each producing 50 orders
        Thread producer1 = new Thread(
            new Producer(queue, 1, 50, 3, producersRemaining),
            "producer-1"
        );

        Thread producer2 = new Thread(
            new Producer(queue, 51, 50, 3, producersRemaining),
            "producer-2"
        );

        // Create three consumers
        Thread consumer1 = new Thread(
            new Consumer(queue, processedCount),
            "consumer-1"
        );

        Thread consumer2 = new Thread(
            new Consumer(queue, processedCount),
            "consumer-2"
        );

        Thread consumer3 = new Thread(
            new Consumer(queue, processedCount),
            "consumer-3"
        );

        // Start consumers
        consumer1.start();
        consumer2.start();
        consumer3.start();

        // Start producers
        producer1.start();
        producer2.start();

        // Wait for producers to finish
        producer1.join();
        producer2.join();

        // Wait for consumers to finish
        consumer1.join();
        consumer2.join();
        consumer3.join();

        long elapsed =
            (System.nanoTime() - start) / 1_000_000;

        int total = processedCount.get();

        System.out.printf(
            "Total processed: %d (expected 100) in %d ms%n",
            total, elapsed
        );

        // Verify the total
        if (total != 100) {
            throw new IllegalStateException(
                "Expected 100 orders, but got " + total
            );
        }

        System.out.println("All orders processed successfully!");
    }
}
