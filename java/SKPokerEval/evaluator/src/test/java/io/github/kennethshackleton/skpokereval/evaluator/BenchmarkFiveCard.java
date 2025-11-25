package io.github.kennethshackleton.skpokereval.evaluator;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BenchmarkFiveCard {

    private static final int HAND_COUNT = 50_000_000;
    private static final int RUNS = 25;

    public static void main(String[] args) {
        final long[] speeds = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            final int[] hands = buildHands();
            final long start = System.nanoTime();
            final short highestRank = run(hands);
            final long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            final long speed = HAND_COUNT * 1000L / durationMs;
            speeds[i] = speed;
            System.out.printf(
                    "Run %d done in %d ms (%d hands / second), highest rank = %d%n", i, durationMs, speed, highestRank);
        }
        Arrays.sort(speeds);
        System.out.printf("Median speed %d hands/s%n", speeds[RUNS / 2]);
    }

    private static short run(int[] hands) {
        int max = 0;
        for (int i = 0; i < hands.length; i += 5) {
            max = Math.max(
                    max, FiveCardsEvaluator.getRank(hands[i], hands[i + 1], hands[i + 2], hands[i + 3], hands[i + 4]));
        }
        return (short) max;
    }

    private static int[] buildHands() {
        final int[] hands = new int[BenchmarkFiveCard.HAND_COUNT * 5];
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BenchmarkFiveCard.HAND_COUNT; i++) {
            long hand = 0;
            for (int cards = 0; cards < 5; cards++) {
                int nextCard;
                do {
                    nextCard = random.nextInt(52);
                    hand |= 1L << nextCard;
                } while (Long.bitCount(hand) < cards);
                hands[i * 5 + cards] = nextCard;
            }
        }
        return hands;
    }
}
