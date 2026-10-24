
package edu.itc.se.lab01.task3;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable {

    public static final Order POISON =
            new Order(-1, "STOP", 0);

    private final OrderQueue queue;
    private final int startId;
    private final int numberOfOrders;
    private final int consumerCount;
    private final AtomicInteger producersRemaining;

    public Producer(OrderQueue queue, int startId,
                    int numberOfOrders, int consumerCount,
                    AtomicInteger producersRemaining) {

        this.queue = queue;
        this.startId = startId;
        this.numberOfOrders = numberOfOrders;
        this.consumerCount = consumerCount;
        this.producersRemaining = producersRemaining;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < numberOfOrders; i++) {

                int id = startId + i;

                int qty = ThreadLocalRandom.current()
                        .nextInt(1, 6);

                Order order = new Order(
                        id, "item-" + id, qty
                );

                queue.put(order);

                Thread.sleep(
                    ThreadLocalRandom.current().nextInt(21)
                );
            }

            // Only the last producer sends poison pills.
            // This ensures all orders have been produced.
            if (producersRemaining.decrementAndGet() == 0) {
                for (int i = 0; i < consumerCount; i++) {
                    queue.put(POISON);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
