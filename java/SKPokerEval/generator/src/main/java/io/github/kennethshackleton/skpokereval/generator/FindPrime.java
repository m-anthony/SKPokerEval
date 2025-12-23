package io.github.kennethshackleton.skpokereval.generator;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.agrona.collections.IntArrayList;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "findprime")
public class FindPrime implements Callable<Integer> {

    @Option(
            names = {"-p", "--prime"},
            description = "the best prime factor currently known")
    private int initPrime;

    @Option(
            names = {"--start"},
            description = "lower bound of primes factor to test",
            defaultValue = "3")
    private int startPrime;

    @Option(
            names = {"--stop"},
            description = "upper bound of primes factor to test",
            defaultValue = "2147483647")
    private int stopPrime;

    @Option(
            names = {"-t", "--threads"},
            description = "number of concurrent threads to use")
    private int threads;

    @Option(
            names = {"--fast"},
            description = "use only one heuristic and one bucket size")
    private boolean fastMode;

    @ArgGroup(exclusive = true, multiplicity = "1")
    CardCount cardCount;

    private static class CardCount {
        @Option(names = "-5", description = "compute best table for 5 cards hands")
        boolean fiveCards;

        @Option(names = "-7", description = "compute best table for 7 cards hands")
        boolean sevenCards;
    }

    @Override
    public Integer call() {

        if (threads == 0) threads = Runtime.getRuntime().availableProcessors() / 2;
        HashTable initTable = null;
        if (initPrime != 0) {
            initTable = cardCount.sevenCards ? TablesGenerator.FLAT_TABLE : TablesGenerator.FLAT_TABLE_FIVE;
            initTable = HashTable.computeShorterHashTable(initPrime, 9, initTable);
            if (initTable != null && !fastMode) initTable = shorterHashTable(initPrime, initTable, false);
        }
        if (initTable == null) {
            initTable = cardCount.sevenCards ? TablesGenerator.SHORT_TABLE : TablesGenerator.SHORT_TABLE_FIVE;
        }

        final AtomicReference<HashTable> bestTable = new AtomicReference<>(initTable);
        System.out.printf(
                "Finding shorter hash tables on %d threads, for primes between %d and %d%n",
                threads, startPrime, stopPrime);
        System.out.println("initial table: " + bestTable);
        final long start = System.nanoTime();
        final IntArrayList primes = new IntArrayList();
        primes.addInt(2);
        final ExecutorService executor = Executors.newCachedThreadPool();
        final LinkedBlockingQueue<Integer> primeQueue = new LinkedBlockingQueue<>(4 * threads);
        for (int i = 0; i < threads; i++) {
            executor.execute(worker(primeQueue, bestTable));
        }

        final Thread mainThread = Thread.currentThread();
        final Thread hook = new Thread(() -> FindPrime.stop(executor, mainThread, primeQueue));
        Runtime.getRuntime().addShutdownHook(hook);

        int maxBits = Integer.SIZE;
        int ignoredPrimes = 0;
        for (int p = 3; p <= stopPrime && p > 0; p += 2) {
            for (int i = 0; i < primes.size(); i++) {
                final int q = primes.getInt(i);
                if (p % q == 0) {
                    break;
                } else if (q * q > p) {
                    primes.addInt(p);
                    if (p >= startPrime) {
                        try {
                            primeQueue.put(p);
                        } catch (final InterruptedException e) {
                            System.out.println("Next prime to test: " + p);
                            return p;
                        }
                        if (Integer.numberOfLeadingZeros(p - startPrime) < maxBits) {
                            maxBits = Integer.numberOfLeadingZeros(p - startPrime);
                            System.out.printf("%s - highest prime generated = %d%n", new Date(), p);
                        }
                    } else {
                        ignoredPrimes = primes.size();
                    }
                    break;
                }
            }
        }

        Runtime.getRuntime().removeShutdownHook(hook);
        awaitTermination(executor, primeQueue);
        System.out.printf(
                "processed %d primes in %s seconds%n",
                primes.size() - ignoredPrimes, TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start));
        return 0;
    }

    private static void stop(ExecutorService executor, Thread mainThread, LinkedBlockingQueue<Integer> primeQueue) {
        System.out.println("stop requested");
        mainThread.interrupt();
        try {
            mainThread.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("wait for primes that are currently processed");
        awaitTermination(executor, primeQueue);
    }

    private static void awaitTermination(ExecutorService executor, LinkedBlockingQueue<Integer> primeQueue) {
        while (!primeQueue.isEmpty()) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
        executor.shutdownNow();
        System.out.println("Done");
    }

    private Runnable worker(LinkedBlockingQueue<Integer> primeQueue, AtomicReference<HashTable> bestTable) {
        return () -> {
            while (!Thread.interrupted()) {
                int p;
                try {
                    p = primeQueue.take();
                } catch (final InterruptedException e) {
                    return;
                }

                HashTable currrentBestTable = bestTable.get();
                final HashTable newTable = shorterHashTable(p, currrentBestTable, fastMode);
                if (newTable != null) {
                    HashTable.validate(currrentBestTable, newTable);
                    do {
                        if (bestTable.compareAndSet(currrentBestTable, newTable)) {
                            System.out.println(new Date() + " - new best hashTable: " + newTable);
                            break;
                        } else {
                            currrentBestTable = bestTable.get();
                        }
                    } while (newTable.size() < currrentBestTable.size());
                }
            }
        };
    }

    private HashTable shorterHashTable(int prime, HashTable reference, boolean fast) {

        final int startOffsetShift = fast ? 9 : 0;
        final int stopOffsetShift = fast ? 10 : 23;
        HashTable best = reference;
        for (int i = startOffsetShift; i < stopOffsetShift; i++) {
            HashTable newTable = HashTable.computeShorterHashTable(prime, i, best);
            if (!fast) {
                newTable = HashTable.computeShorterHashTable2(prime, i, newTable == null ? best : newTable);
            }
            if (newTable != null) best = newTable;
        }
        return best == reference ? null : best;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new FindPrime()).execute(args));
    }
}
