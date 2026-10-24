
package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;

public class Bank {

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

    // BUG: Lock order depends on the argument order.
    // This can cause a circular wait and deadlock.
    static void transfer(Account from, Account to, long amount) {

        synchronized (from) {
            synchronized (to) {

                if (from.balance() >= amount) {
                    from.withdraw(amount);
                    to.deposit(amount);
                }
            }
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        Account[] accounts = new Account[4];

        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = new Account(i, 1000);
        }

        // Detect deadlocks automatically
        Thread watchdog = Thread.ofPlatform()
                .daemon()
                .name("deadlock-watchdog")
                .start(() -> {

                    var bean = java.lang.management
                            .ManagementFactory.getThreadMXBean();

                    while (!Thread.currentThread().isInterrupted()) {

                        long[] deadlocked =
                                bean.findDeadlockedThreads();

                        if (deadlocked != null) {

                            System.out.println(
                                "[watchdog] DEADLOCK detected!"
                            );

                            var infos = bean.getThreadInfo(deadlocked);

                            for (var info : infos) {
                                System.out.println(
                                    "[watchdog] Thread: "
                                    + info.getThreadName()
                                );
                            }

                            break;
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });

        Thread[] workers = new Thread[8];

        for (int w = 0; w < workers.length; w++) {

            workers[w] = new Thread(() -> {

                var rnd = ThreadLocalRandom.current();

                for (int i = 0; i < 100_000; i++) {

                    Account a = accounts[
                        rnd.nextInt(accounts.length)
                    ];

                    Account b = accounts[
                        rnd.nextInt(accounts.length)
                    ];

                    if (a != b) {
                        transfer(a, b, rnd.nextInt(1, 50));
                    }
                }

            }, "transfer-" + w);

            workers[w].start();
        }

        for (Thread t : workers) {
            t.join();
        }

        long total = 0;

        for (Account a : accounts) {
            total += a.balance();
        }

        System.out.println(
            "total = " + total + " (expected 4000)"
        );
    }
}
