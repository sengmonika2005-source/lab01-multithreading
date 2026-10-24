
package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TryLockBank {

    static final class Account {

        final int id;
        private long balance;

        final ReentrantLock lock = new ReentrantLock();

        Account(int id, long balance) {
            this.id = id;
            this.balance = balance;
        }

        long balance() {
            return balance;
        }

        void withdraw(long amount) {
            balance -= amount;
        }

        void deposit(long amount) {
            balance += amount;
        }
    }

    // Fix B: Timed lock attempts with retries
    static boolean transfer(
            Account from,
            Account to,
            long amount,
            AtomicLong retries) throws InterruptedException {

        if (from == to) {
            return false;
        }

        while (!Thread.currentThread().isInterrupted()) {

            boolean firstLocked = false;
            boolean secondLocked = false;

            try {
                // Try to lock the source account
                firstLocked = from.lock.tryLock(
                    10, TimeUnit.MILLISECONDS
                );

                if (firstLocked) {

                    // Try to lock the destination account
                    secondLocked = to.lock.tryLock(
                        10, TimeUnit.MILLISECONDS
                    );
                }

                // Transfer only when both locks are acquired
                if (firstLocked && secondLocked) {

                    if (from.balance() >= amount) {
                        from.withdraw(amount);
                        to.deposit(amount);
                        return true;
                    }

                    return false;
                }

            } finally {

                // Always release acquired locks
                if (secondLocked) {
                    to.lock.unlock();
                }

                if (firstLocked) {
                    from.lock.unlock();
                }
            }

            // Record a failed lock acquisition
            retries.incrementAndGet();

            // Random back-off before retrying
            Thread.sleep(
                ThreadLocalRandom.current().nextInt(1, 6)
            );
        }

        return false;
    }

    public static void main(String[] args)
            throws InterruptedException {

        Account[] accounts = new Account[4];

        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = new Account(i, 1000);
        }

        final int THREADS = 8;
        final int TRANSFERS = 10_000;

        AtomicLong completed = new AtomicLong();
        AtomicLong retries = new AtomicLong();

        Thread[] workers = new Thread[THREADS];

        long start = System.nanoTime();

        for (int w = 0; w < THREADS; w++) {

            workers[w] = new Thread(() -> {

                var rnd = ThreadLocalRandom.current();

                try {
                    for (int i = 0; i < TRANSFERS; i++) {

                        int a = rnd.nextInt(accounts.length);

                        int b = rnd.nextInt(
                            accounts.length - 1
                        );

                        if (b >= a) {
                            b++;
                        }

                        long amount = rnd.nextLong(1, 50);

                        if (transfer(
                                accounts[a],
                                accounts[b],
                                amount,
                                retries)) {

                            completed.incrementAndGet();
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }, "transfer-" + w);

            workers[w].start();
        }

        // Wait for all workers
        for (Thread worker : workers) {
            worker.join();
        }

        long total = 0;

        for (Account account : accounts) {
            total += account.balance();
        }

        long elapsed =
            (System.nanoTime() - start) / 1_000_000;

        System.out.printf(
            "attempts=%d completed=%d retries=%d " +
            "total=%d (expected 4000) in %d ms%n",
            THREADS * TRANSFERS,
            completed.get(),
            retries.get(),
            total,
            elapsed
        );

        if (total != 4000) {
            throw new IllegalStateException(
                "Total balance changed!"
            );
        }

        System.out.println(
            "TryLockBank completed without deadlock!"
        );
    }
}
