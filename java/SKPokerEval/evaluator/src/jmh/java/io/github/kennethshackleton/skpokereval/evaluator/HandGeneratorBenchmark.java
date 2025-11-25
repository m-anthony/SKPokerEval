package io.github.kennethshackleton.skpokereval.evaluator;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(5)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 11, time = 5, timeUnit = TimeUnit.SECONDS)
public class HandGeneratorBenchmark {

    private static final long MAX = 52L * 52 * 52 * 52 * 52 * 52 * 52;
    private static final int MAX_FIVE = 52 * 52 * 52 * 52 * 52;
    private static final long MASK = (1L << 52) - 1;

    @Benchmark
    public static long generateHand() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        long bits = random.nextLong(MAX);
        long h = 0;
        for (int i = 0; i < 7; i++) {
            h |= Long.lowestOneBit(~h);
            final int shift = (int) (bits % 52);
            h = ((h << shift) | (h >>> (52 - shift))) & MASK;
            bits /= 52;
        }
        return h;
    }

    @Benchmark
    public static long generateHandNaive() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        long h = 0;
        do {
            h |= 1L << random.nextInt(52);
        } while (Long.bitCount(h) < 7);
        return h;
    }

    @Benchmark
    public static long generateHandFive() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        int bits = random.nextInt(MAX_FIVE);
        long h = 0;
        for (int i = 0; i < 5; i++) {
            h |= Long.lowestOneBit(~h);
            final int shift = bits % 52;
            h = ((h << shift) | (h >>> (52 - shift))) & MASK;
            bits /= 52;
        }
        return h;
    }

    @Benchmark
    public static long generateHandNaiveFive() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        long h = 0;
        do {
            h |= 1L << random.nextInt(52);
        } while (Long.bitCount(h) < 5);
        return h;
    }
}
