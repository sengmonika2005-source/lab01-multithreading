/*
 * Difference between start() and run():
 *
 * start() creates a new thread, allowing multiple threads
 * to execute concurrently. Their outputs may appear
 * in different orders.
 *
 * run() executes directly on the main thread, so each
 * countdown finishes before the next one begins.
 * It does not create a new thread.
 *
 * Calling run() directly on a virtual thread does nothing.
 * Therefore, artemis does not print a countdown in sequential mode.
 * 
 * join() waits for a started thread to finish.
 *
 * In concurrent mode, all four threads finish with
 * state TERMINATED.
 *
 * In sequential mode, the Thread objects remain in
 * state NEW because start() was never called.
 */

package edu.itc.se.lab01.task1;

public class Main {

    public static void main(String[] args)
            throws InterruptedException {

        boolean sequential = args.length > 0
                && args[0].equals("--sequential");

        // Create four threads in different ways
        Thread apollo = new Countdown("apollo");

        Thread gemini = new Thread(
                new CountdownTask(), "gemini"
        );

        Thread mercury = new Thread(
                () -> new CountdownTask().run(), "mercury"
        );

        Thread artemis = Thread.ofVirtual()
                .name("artemis")
                .unstarted(new CountdownTask());

        Thread[] threads = {
                apollo, gemini, mercury, artemis
        };

        // Start threads concurrently or sequentially
        if (sequential) {
            for (Thread thread : threads) {
                thread.run();
            }
        } else {
            for (Thread thread : threads) {
                thread.start();
            }
        }

        System.out.println("Mission control: all launched");

        // Wait for every thread to finish
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("Mission control: all landed");

        // Display thread information
        for (Thread thread : threads) {
            System.out.printf(
                "%s virtual=%b state=%s%n",
                thread.getName(),
                thread.isVirtual(),
                thread.getState()
            );
        }
    }
}
