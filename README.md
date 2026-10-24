# Lab 01: Multithreading in Java

## Task 1: Creating and Running Threads

Four thread creation techniques were implemented:

1. Extending the Thread class
2. Implementing Runnable
3. Using a lambda expression
4. Using a virtual thread

### Difference between start() and run()

start() launches a new thread, allowing concurrent execution.

Calling run() directly does not start a new thread.
For ordinary platform threads, the method executes
on the calling thread.

join() waits for a started thread to finish.

In concurrent mode, all four threads finished with
state TERMINATED.

In sequential mode, no threads were started, so their
states remained NEW.

## Task 2: Fixing a Race Condition

### Problem

Eight threads each incremented a shared counter
1,000,000 times.

Expected total: 8,000,000

The UnsafeCounter uses count++, which is not atomic.
Concurrent updates can overwrite each other and
cause lost increments.

### Benchmark Results

| Counter             |     Value | Time (ms) |
| ------------------- | --------: | --------: |
| UnsafeCounter 1     | 4,125,711 |        55 |
| UnsafeCounter 2     | 4,286,518 |         3 |
| UnsafeCounter 3     | 5,004,179 |         2 |
| SynchronizedCounter | 8,000,000 |       629 |
| LockCounter         | 8,000,000 |       282 |
| AtomicCounter       | 8,000,000 |       168 |

### Solutions

1. SynchronizedCounter uses synchronized methods
   to prevent concurrent updates.

2. LockCounter uses ReentrantLock with a finally
   block to guarantee the lock is released.

3. AtomicCounter uses AtomicLong for atomic
   increments without explicit locking.

### Why must get() also be synchronized?

Synchronizing only increment() protects the updates,
but an unsynchronized get() may read stale data when
other threads are updating the counter.

Synchronizing both methods ensures proper memory
visibility and consistent access to the shared field.

In this benchmark, join() establishes a happens-before
relationship, so the final read after joining the
workers is properly ordered. However, an unsynchronized
get() would still be unsafe for general concurrent use.

### Performance Discussion

All three synchronized implementations returned
the expected result of 8,000,000.

AtomicCounter was fastest among the correct
implementations in this particular run.

The results depend on the computer, thread scheduling,
JIT compilation, and system load. More repeated runs
would be needed for a reliable performance comparison.

## Task 3: Producer-Consumer Problem

### Objective

Implement a producer-consumer system using
synchronized, wait(), notifyAll(), and BlockingQueue.

### Implementation

The system uses:

- 2 producers, each generating 50 orders
- 3 consumers processing orders
- A shared buffer with capacity 5
- AtomicInteger for counting processed orders
- Poison pills to stop consumers

### Results

| Implementation     | Orders |   Time |
| ------------------ | -----: | -----: |
| BoundedBuffer      |    100 | 636 ms |
| ArrayBlockingQueue |    100 | 715 ms |

Both implementations processed 100 orders
successfully, and all consumers terminated.

### Why use while instead of if?

The while loop checks the buffer condition
again after a thread wakes up.

Using if can cause problems because the
buffer condition may change before the
awakened thread reacquires the lock.

### Why use notifyAll() instead of notify()?

notifyAll() wakes all threads waiting on
the monitor so they can recheck their
conditions.

notify() wakes only one waiting thread.
It may wake a producer when the buffer
is full or a consumer when it is empty,
which can cause the system to hang.

### Comparison

BoundedBuffer requires manual synchronization
using synchronized, wait(), and notifyAll().

ArrayBlockingQueue handles synchronization
internally and requires less code.

Both implementations are suitable for the
producer-consumer problem.

### Experiment 1: notify() vs notifyAll()

I replaced notifyAll() with notify() and ran
the program 20 times.

Results:

- Passed: 20
- Failed: 0
- Hung: 0

All 20 runs completed successfully.

However, notify() may cause deadlock because
it wakes only one waiting thread. That thread
might not be able to continue.

notifyAll() is safer for this producer-consumer
implementation because all waiting threads
can recheck their conditions.

### Experiment 2: if vs while

I replaced the while loops with if statements
while keeping notifyAll().

I executed the program 20 times with Java
assertions enabled.

Results:

- Passed: 0
- Failed: 0
- Timed out: 20

All 20 runs exceeded the 5-second timeout.

The problem occurs because if does not recheck
the buffer condition after a thread wakes up.

A thread may continue when the buffer is still
empty or already full, potentially causing
invalid operations and preventing termination.

The while loop is necessary because it rechecks
the condition before proceeding.

Conclusion:
The correct implementation uses while loops
together with notifyAll().

## Task 4: Thread Pools, Future and Virtual Threads

### 1. CPU-Bound Experiment

The program counted prime numbers below 4,000,000.

The work was divided into 16 chunks and submitted
to an ExecutorService with 20 platform threads.

Results:

- Single-threaded: 1519 ms
- Thread pool: 222 ms
- Prime count: 283146
- Speedup: 6.84x

Both implementations produced identical results.

The parallel implementation was faster because
multiple CPU cores could execute tasks concurrently.

### 2. I/O-Bound Experiment

The program simulated 500 downloads.

Each normal download takes 200 milliseconds.

| Executor                | Time (ms) |
| ----------------------- | --------: |
| Fixed pool (20 threads) |      5223 |
| Virtual threads         |      1020 |

Both executors produced:

- Successful downloads: 489
- Failed downloads: 10
- Timed-out downloads: 1

Virtual threads were approximately 5.12 times
faster in this experiment.

### 3. Why Are Virtual Threads Faster?

Platform threads are limited by the fixed pool size.
When threads sleep or wait for I/O, they occupy
worker slots.

Virtual threads are lightweight and allow many
blocking tasks to make progress concurrently.

They are particularly useful for I/O-bound workloads.

### 4. Failure Handling

Every 50th simulated download throws IOException.

Future.get() reports the failure through
ExecutionException.

The program catches these exceptions and counts
the failures without crashing.

### 5. Timeout Handling

One download sleeps for 5 seconds.

The program calls Future.get() with a 1-second
timeout.

If TimeoutException occurs, the task is
cancelled using future.cancel(true).

### 6. Executor Shutdown

Both executors use try-with-resources.

This ensures that the executors are closed
and the program exits without System.exit().

## Task 5: Deadlock Detection and Graceful Shutdown

### 1. Deadlock Demonstration

The Bank program intentionally creates a deadlock
because threads acquire account locks in different
orders.

The program became unresponsive as expected.

### 2. Deadlock Detection Using jstack

I used jcmd -l to identify the Java process and
jstack to inspect its threads.

The deadlock involved:

- transfer-0
- transfer-5

The jstack report confirmed one Java-level deadlock.

Evidence was saved in deadlock.txt.

### 3. Deadlock Watchdog

A daemon watchdog thread checks for deadlocks
every second using findDeadlockedThreads().

The watchdog successfully detected:

- transfer-0
- transfer-2
- transfer-3

### 4. Fix A: OrderedBank

OrderedBank prevents deadlocks by acquiring
account locks in ascending ID order.

Results:

- Transfer attempts: 80000
- Successful transfers: 78452
- Total balance: 4000
- Execution time: 27 ms

The program completed without deadlock.

### 5. Fix B: TryLockBank

TryLockBank uses ReentrantLock and timed tryLock()
to avoid waiting indefinitely for locks.

If lock acquisition fails, the thread releases
any acquired lock and retries after a random delay.

Results:

- Transfer attempts: 80000
- Successful transfers: 78729
- Lock retries: 2012
- Total balance: 4000
- Execution time: 5677 ms

The program completed without deadlock.

### 6. Graceful Shutdown

Eight workers performed concurrent transfers
for approximately 2 seconds.

The main thread then interrupted all workers.

Results:

- Completed transfers: 14477
- Lock retries: 720
- All workers stopped: true
- Final total balance: 4000

### 7. Conclusion

The original Bank implementation suffered from
a deadlock caused by inconsistent lock ordering.

OrderedBank eliminates circular wait through
consistent lock acquisition order.

TryLockBank avoids indefinite waiting using
timed lock attempts and retries.

Graceful shutdown uses interruption to stop
workers cooperatively while maintaining data
consistency and releasing locks safely.
