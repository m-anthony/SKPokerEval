package io.github.kennethshackleton.skpokereval.evaluator;

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
public class EvaluatorBenchmarks {

    @Benchmark
    public int evalSevenFast() {
        return SevenCardsEvaluator.getRank(HandGeneratorBenchmark.generateHand());
    }

    @Benchmark
    public int evalSevenSlow() {
        return SlowEvaluator.get7CardsHandClass(HandGeneratorBenchmark.generateHand());
    }

    @Benchmark
    public long baselineSeven() {
        return HandGeneratorBenchmark.generateHand();
    }

    @Benchmark
    public int getKey7() {
        return SevenCardsEvaluator.getKey(HandGeneratorBenchmark.generateHand());
    }

    @Benchmark
    public int evalFiveFast() {
        return FiveCardsEvaluator.getRank(HandGeneratorBenchmark.generateHandFive());
    }

    @Benchmark
    public int evalFiveSlow() {
        return SlowEvaluator.get5CardsHandClass(HandGeneratorBenchmark.generateHandFive());
    }

    @Benchmark
    public long baselineFive() {
        return HandGeneratorBenchmark.generateHandFive();
    }
}
