/*
 * UnsafeCounter results:
 * UnsafeCounter 1        value=5,861,485 correct=false 45 ms
 * UnsafeCounter 2        value=6,845,888 correct=false 1 ms
 * UnsafeCounter 3        value=8,000,000 correct=true  2 ms
 *
 * Race condition: multiple threads update
 * the shared counter without synchronization.
 */

package edu.itc.se.lab01.task2;

import java.util.ArrayList;
import java.util.List;

/*
 * UnsafeCounter results:
 * Run 1: 7,332,240 - incorrect (57 ms)
 * Run 2: 6,015,073 - incorrect (1 ms)
 * Run 3: 8,000,000 - correct (1 ms)
 *
 * The expected result is 8,000,000.
 *
 * A race condition occurs because count++ is not atomic.
 * Multiple threads may read and update the same value,
 * causing some increments to be lost.
 *
 * Run 3 is correct by chance, but UnsafeCounter
 * is still not thread-safe.
 */

public class Bench {

    static final int THREADS = 8;
    static final int ITERATIONS = 1_000_000;

    static void run(String label, Counter counter)
            throws InterruptedException {

        long start = System.nanoTime();

        List<Thread> threads = new ArrayList<>();

        // Create and start 8 threads
        for (int t = 0; t < THREADS; t++) {
            Thread th = new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) {
                    counter.increment();
                }
            }, "inc-" + t);

            threads.add(th);
            th.start();
        }

        // Wait for all threads to finish
        for (Thread th : threads) {
            th.join();
        }

        long ms = (System.nanoTime() - start) / 1_000_000;

        long expected = (long) THREADS * ITERATIONS;
        long actual = counter.get();

        System.out.printf(
            "%-22s value=%,d correct=%-5b %d ms%n",
            label,
            actual,
            actual == expected,
            ms
        );
    }

    // public static void main(String[] args)
    //         throws InterruptedException {

    //     // Test UnsafeCounter 3 times
    //     run("UnsafeCounter 1", new UnsafeCounter());
    //     run("UnsafeCounter 2", new UnsafeCounter());
    //     run("UnsafeCounter 3", new UnsafeCounter());
    // }
    
    // public static void main(String[] args)
    //         throws InterruptedException {

    //     // Unsafe version
    //     run("UnsafeCounter 1", new UnsafeCounter());
    //     run("UnsafeCounter 2", new UnsafeCounter());
    //     run("UnsafeCounter 3", new UnsafeCounter());

    //     // Fixed version using synchronized
    //     run("SynchronizedCounter",
    //             new SynchronizedCounter());
    // }
    
    // public static void main(String[] args)
    //         throws InterruptedException {

    //     // Unsafe counter
    //     run("UnsafeCounter 1", new UnsafeCounter());
    //     run("UnsafeCounter 2", new UnsafeCounter());
    //     run("UnsafeCounter 3", new UnsafeCounter());

    //     // First solution: synchronized
    //     run("SynchronizedCounter", new SynchronizedCounter());

    //     // Second solution: ReentrantLock
    //     run("LockCounter", new LockCounter());
    // }

    
    public static void main(String[] args)
            throws InterruptedException {

        // Unsafe counter
        run("UnsafeCounter 1", new UnsafeCounter());
        run("UnsafeCounter 2", new UnsafeCounter());
        run("UnsafeCounter 3", new UnsafeCounter());

        // First solution: synchronized
        run("SynchronizedCounter",
                new SynchronizedCounter());

        // Second solution: ReentrantLock
        run("LockCounter",
                new LockCounter());

        // Third solution: AtomicLong
        run("AtomicCounter",
                new AtomicCounter());
                
        run("LongAdderCounter", new LongAdderCounter());
    }


}
