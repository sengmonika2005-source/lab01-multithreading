
package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class GracefulBank {

    public static void main(String[] args)
            throws InterruptedException {

        final int THREADS = 8;

        TryLockBank.Account[] accounts =
                new TryLockBank.Account[4];

        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = new TryLockBank.Account(i, 1000);
        }

        AtomicLong completed = new AtomicLong();
        AtomicLong retries = new AtomicLong();

        Thread[] workers = new Thread[THREADS];

        // Create and start 8 workers
        for (int w = 0; w < THREADS; w++) {

            workers[w] = new Thread(() -> {

                var rnd = ThreadLocalRandom.current();
                long localCompleted = 0;

                try {
                    while (!Thread.currentThread().isInterrupted()) {

                        int a = rnd.nextInt(accounts.length);
                        int b = rnd.nextInt(accounts.length - 1);

                        if (b >= a) {
                            b++;
                        }

                        long amount = rnd.nextLong(1, 50);

                        if (TryLockBank.transfer(
                                accounts[a],
                                accounts[b],
                                amount,
                                retries)) {

                            completed.incrementAndGet();
                            localCompleted++;
                        }
                    }

                } catch (InterruptedException e) {
                    // Restore the interrupt flag
                    Thread.currentThread().interrupt();

                } finally {
                    System.out.printf(
                        "%s stopped after %d transfers%n",
                        Thread.currentThread().getName(),
                        localCompleted
                    );
                }

            }, "transfer-" + w);

            workers[w].start();
        }

        // Let workers run for 2 seconds
        Thread.sleep(2000);

        System.out.println(
            "[main] interrupting 8 workers after 2000 ms"
        );

        // Request all workers to stop
        for (Thread worker : workers) {
            worker.interrupt();
        }

        // Wait for workers with a shared 1-second deadline
        long deadline = System.nanoTime() + 1_000_000_000L;

        for (Thread worker : workers) {
            long remaining = deadline - System.nanoTime();

            if (remaining <= 0) {
                break;
            }

            worker.join(
                Math.max(1, remaining / 1_000_000)
            );
        }

        boolean allStopped = true;

        for (Thread worker : workers) {
            if (worker.isAlive()) {
                allStopped = false;
            }
        }

        if (!allStopped) {
            throw new IllegalStateException(
                "Some workers did not stop in time!"
            );
        }

        long total = 0;

        for (TryLockBank.Account account : accounts) {
            total += account.balance();
        }

        System.out.printf(
            "transfers=%d retries=%d%n",
            completed.get(),
            retries.get()
        );

        System.out.printf(
            "all workers stopped=%b total=%d (expected 4000)%n",
            allStopped,
            total
        );

        if (total != 4000) {
            throw new IllegalStateException(
                "Total balance changed!"
            );
        }

        System.out.println(
            "Graceful shutdown completed successfully!"
        );
    }
}
