
package edu.itc.se.lab01.task3;

import java.util.concurrent.atomic.AtomicInteger;

public class Main2 {

    public static void main(String[] args)
            throws InterruptedException {

        long start = System.nanoTime();

        // Built-in blocking queue with capacity 5
        OrderQueue queue = new BlockingOrderQueue(5);

        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger producersRemaining = new AtomicInteger(2);

        // Reuse the same Producer and Consumer classes
        Thread[] threads = {

            new Thread(
                new Consumer(queue, processedCount),
                "consumer-1"
            ),

            new Thread(
                new Consumer(queue, processedCount),
                "consumer-2"
            ),

            new Thread(
                new Consumer(queue, processedCount),
                "consumer-3"
            ),

            new Thread(
                new Producer(queue, 1, 50, 3,
                             producersRemaining),
                "producer-1"
            ),

            new Thread(
                new Producer(queue, 51, 50, 3,
                             producersRemaining),
                "producer-2"
            )
        };

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }

        long elapsed =
            (System.nanoTime() - start) / 1_000_000;

        int total = processedCount.get();

        System.out.printf(
            "Total processed: %d (expected 100) in %d ms%n",
            total, elapsed
        );

        if (total != 100) {
            throw new IllegalStateException(
                "Expected 100 orders, got " + total
            );
        }

        System.out.println(
            "ArrayBlockingQueue completed successfully!"
        );
    }
}
