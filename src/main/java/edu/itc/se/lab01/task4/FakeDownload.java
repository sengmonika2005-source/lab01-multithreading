
package edu.itc.se.lab01.task4;

import java.io.IOException;
import java.util.concurrent.Callable;

public class FakeDownload implements Callable<String> {

    private final String url;
    private final int taskNumber;

    public FakeDownload(String url, int taskNumber) {
        this.url = url;
        this.taskNumber = taskNumber;
    }

    @Override
    public String call()
            throws IOException, InterruptedException {

        // Simulate a slow download for timeout testing
        if (taskNumber == 499) {
            Thread.sleep(5000);
        } else {
            // Normal downloads take 200 milliseconds
            Thread.sleep(200);
        }

        // Every 50th download fails
        if (taskNumber % 50 == 0) {
            throw new IOException("boom");
        }

        // Simulate a successful download result
        return url + " -> " + url.length();
    }
}
