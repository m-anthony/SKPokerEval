package io.github.kennethshackleton.skpokereval.evaluator;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BenchmarkSevenCard {

    private static final int HAND_COUNT = 50_000_000;
    private static final int RUNS = 25;

    public static void main(String[] args) {
        final long[] speeds = new long[RUNS];
        final long[] speeds2 = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            final int[] hands = buildHands();
            final long[] handsBits = new long[HAND_COUNT];
            long hand = 0;
            for (int j = 0; j < hands.length; j++) {
                hand |= 1L << hands[j];
                if (j % 7 == 6) {
                    handsBits[j / 7] = hand;
                    hand = 0;
                }
            }
            long start = System.nanoTime();
            short highestRank = run(hands);
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            long speed = HAND_COUNT * 1000L / durationMs;
            speeds[i] = speed;
            System.out.printf(
                    "Run %d done in %d ms (%d hands / second), highest rank = %d%n", i, durationMs, speed, highestRank);

            start = System.nanoTime();
            highestRank = run(handsBits);
            durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            speed = HAND_COUNT * 1000L / durationMs;
            speeds2[i] = speed;
            System.out.printf(
                    "Run %d with longs done in %d ms (%d hands / second), highest rank = %d%n",
                    i, durationMs, speed, highestRank);
        }
        Arrays.sort(speeds);
        System.out.printf("Median speed %d: hands/s%n", speeds[RUNS / 2]);
        Arrays.sort(speeds2);
        System.out.printf("Median speed with long hands: %d hands/s%n", speeds2[RUNS / 2]);
    }

    private static short run(long[] hands) {
        int max = 0;
        for (final long h : hands) {
            max = Math.max(max, SevenCardsEvaluator.getRank(h));
        }
        return (short) max;
    }

    private static short run(int[] hands) {
        int max = 0;
        for (int i = 0; i < hands.length; i += 7) {
            max = Math.max(
                    max,
                    SevenCardsEvaluator.getRank(
                            hands[i],
                            hands[i + 1],
                            hands[i + 2],
                            hands[i + 3],
                            hands[i + 4],
                            hands[i + 5],
                            hands[i + 6]));
        }
        return (short) max;
    }

    private static int[] buildHands() {
        final int[] hands = new int[BenchmarkSevenCard.HAND_COUNT * 7];
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < BenchmarkSevenCard.HAND_COUNT; i++) {
            long hand = 0;
            for (int cards = 0; cards < 7; cards++) {
                int nextCard;
                do {
                    nextCard = random.nextInt(52);
                    hand |= 1L << nextCard;
                } while (Long.bitCount(hand) < cards);
                hands[i * 7 + cards] = nextCard;
            }
        }
        return hands;
    }
}
