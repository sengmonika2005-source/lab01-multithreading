package edu.itc.se.lab01.task1;

public class CountdownTask implements Runnable {

    @Override
    public void run() {
        try {
            for (int i = 3; i >= 1; i--) {
                System.out.println(
                    Thread.currentThread().getName() + ": " + i
                );
                Thread.sleep(200);
            }

            System.out.println(
                Thread.currentThread().getName() + ": liftoff!"
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
