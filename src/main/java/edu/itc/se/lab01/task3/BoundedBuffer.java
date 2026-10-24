
package edu.itc.se.lab01.task3;

import java.util.ArrayDeque;

public class BoundedBuffer implements OrderQueue {

    private final ArrayDeque<Order> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                "Capacity must be positive"
            );
        }
        this.capacity = capacity;
    }

    @Override
    public synchronized void put(Order order)
            throws InterruptedException {

        // Wait while the buffer is full
        while (items.size() >= capacity) {
            wait();
        }

        // Add an order
        items.addLast(order);

        assert items.size() <= capacity;

        // Wake up waiting threads
        notifyAll();
    }

    @Override
    public synchronized Order take()
            throws InterruptedException {

        // Wait while the buffer is empty
        while (items.isEmpty()) {
            wait();
        }

        assert !items.isEmpty();

        // Remove the oldest order
        Order order = items.removeFirst();

        // Wake up waiting threads
        notifyAll();

        return order;
    }
}
