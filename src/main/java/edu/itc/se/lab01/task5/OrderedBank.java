
package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class OrderedBank {

    static final class Account {
        final int id;
        private long balance;

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

    // Fix: Always acquire locks in ascending ID order
    static boolean transfer(Account from, Account to,
                            long amount) {

        if (from == to) {
            return false;
        }

        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;

        synchronized (first) {
            synchronized (second) {

                if (from.balance() >= amount) {
                    from.withdraw(amount);
                    to.deposit(amount);
                    return true;
                }

                return false;
            }
        }
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

        Thread[] workers = new Thread[THREADS];

        long start = System.nanoTime();

        for (int w = 0; w < THREADS; w++) {

            workers[w] = new Thread(() -> {

                var rnd = ThreadLocalRandom.current();

                for (int i = 0; i < TRANSFERS; i++) {

                    int a = rnd.nextInt(accounts.length);

                    int b = rnd.nextInt(accounts.length - 1);

                    if (b >= a) {
                        b++;
                    }

                    long amount = rnd.nextLong(1, 50);

                    if (transfer(accounts[a],
                                 accounts[b], amount)) {
                        completed.incrementAndGet();
                    }
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
            "attempts=%d completed=%d total=%d " +
            "(expected 4000) in %d ms%n",
            THREADS * TRANSFERS,
            completed.get(),
            total,
            elapsed
        );

        if (total != 4000) {
            throw new IllegalStateException(
                "Total balance changed!"
            );
        }

        System.out.println(
            "OrderedBank completed without deadlock!"
        );
    }
}
