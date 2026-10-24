
package edu.itc.se.lab01.task4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {

    // Part 1: CPU-bound prime counting
    static void testPrimes() throws Exception {

        final long LIMIT = 4_000_000;
        final int CHUNKS = 16;

        long start = System.nanoTime();

        long single = new PrimeCounter(0, LIMIT).call();

        long singleTime =
                (System.nanoTime() - start) / 1_000_000;

        System.out.printf(
            "Single-threaded: %d primes in %d ms%n",
            single, singleTime
        );

        int cores = Runtime.getRuntime()
                .availableProcessors();

        start = System.nanoTime();

        long total = 0;

        try (ExecutorService pool =
                Executors.newFixedThreadPool(cores)) {

            List<Future<Long>> futures = new ArrayList<>();

            long chunkSize = LIMIT / CHUNKS;

            for (int i = 0; i < CHUNKS; i++) {

                long from = i * chunkSize;

                long to = (i == CHUNKS - 1)
                        ? LIMIT
                        : from + chunkSize;

                futures.add(
                    pool.submit(new PrimeCounter(from, to))
                );
            }

            for (Future<Long> future : futures) {
                total += future.get();
            }
        }

        long parallelTime =
                (System.nanoTime() - start) / 1_000_000;

        System.out.printf(
            "Pool (%d threads): %d primes in %d ms%n",
            cores, total, parallelTime
        );

        if (single != total) {
            throw new IllegalStateException(
                "Prime counts do not match!"
            );
        }

        System.out.println(
            "Both methods produced the same result!"
        );

        if (parallelTime > 0) {
            System.out.printf(
                "Speedup: %.2fx%n",
                (double) singleTime / parallelTime
            );
        }
    }

    // Part 2: I/O-bound download simulation
    static void testDownloads(boolean virtual)
            throws InterruptedException {

        int ok = 0;
        int failed = 0;
        int timedOut = 0;

        long start = System.nanoTime();

        ExecutorService executor = virtual
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(20);

        try (executor) {

            List<Future<String>> futures = new ArrayList<>();

            // Start the slow download first
            Future<String> slowFuture = executor.submit(
                new FakeDownload(
                    "https://example.com/file499", 499
                )
            );

            // Submit the remaining downloads
            for (int i = 1; i <= 500; i++) {

                if (i == 499) {
                    continue;
                }

                String url =
                    "https://example.com/file" + i;

                futures.add(
                    executor.submit(new FakeDownload(url, i))
                );
            }

            // Give the slow download a 1-second timeout
            try {
                slowFuture.get(1, TimeUnit.SECONDS);
                ok++;

            } catch (TimeoutException e) {
                timedOut++;
                slowFuture.cancel(true);

            } catch (ExecutionException e) {
                failed++;
            }

            // Collect results from other downloads
            for (Future<String> future : futures) {

                try {
                    future.get();
                    ok++;

                } catch (ExecutionException e) {
                    failed++;
                }
            }
        }

        long elapsed =
                (System.nanoTime() - start) / 1_000_000;

        String mode = virtual
                ? "Virtual threads"
                : "Platform threads (20)";

        System.out.printf(
            "%s: ok=%d failed=%d timedOut=%d in %d ms%n",
            mode, ok, failed, timedOut, elapsed
        );

        if (ok != 489 || failed != 10 || timedOut != 1) {
            throw new IllegalStateException(
                "Unexpected download results"
            );
        }
    }

    public static void main(String[] args)
            throws Exception {

        System.out.println("=== CPU-BOUND TEST ===");

        testPrimes();

        System.out.println();
        System.out.println("=== I/O-BOUND TEST ===");

        testDownloads(false);
        testDownloads(true);

        System.out.println();
        System.out.println("Task 4 experiments completed!");
    }
}
