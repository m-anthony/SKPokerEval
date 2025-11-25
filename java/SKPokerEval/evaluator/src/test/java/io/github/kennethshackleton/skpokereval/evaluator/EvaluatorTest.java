package io.github.kennethshackleton.skpokereval.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EvaluatorTest {

    @Test
    public void testSevenEvaluator() {

        // for each 7-cards hand, the evaluation should be the max of the evaluation of
        // each 5-cards hand possible
        for (int c1 = 0; c1 < 52; c1++) {
            for (int c2 = c1 + 1; c2 < 52; c2++) {
                for (int c3 = c2 + 1; c3 < 52; c3++) {
                    for (int c4 = c3 + 1; c4 < 52; c4++) {
                        for (int c5 = c4 + 1; c5 < 52; c5++) {
                            for (int c6 = c5 + 1; c6 < 52; c6++) {
                                for (int c7 = c6 + 1; c7 < 52; c7++) {
                                    final short eval5 = evalSevenWithFive(c1, c2, c3, c4, c5, c6, c7);
                                    final long hand = (1L << c1)
                                            | (1L << c2)
                                            | (1L << c3)
                                            | (1L << c4)
                                            | (1L << c5)
                                            | (1L << c6)
                                            | (1L << c7);
                                    assertEquals(eval5, SevenCardsEvaluator.getRank(hand));
                                    assertEquals(
                                            SlowEvaluator.get7CardsHandClass(hand), SevenCardsEvaluator.getRank(hand));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private short evalSevenWithFive(int c1, int c2, int c3, int c4, int c5, int c6, int c7) {
        int eval5 = FiveCardsEvaluator.getRank(hand5(c1, c2, c3, c4, c5));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c2, c3, c4, c6)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c2, c3, c5, c6)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c2, c4, c5, c6)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c3, c4, c5, c6)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c2, c3, c4, c5, c6)));

        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c2, c3, c4, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c2, c3, c5, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c2, c4, c5, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c3, c4, c5, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c2, c3, c4, c5, c7)));

        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c2, c3, c6, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c2, c4, c6, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c3, c4, c6, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c2, c3, c4, c6, c7)));

        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c2, c5, c6, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c3, c5, c6, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c2, c3, c5, c6, c7)));

        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c1, c4, c5, c6, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c2, c4, c5, c6, c7)));
        eval5 = Math.max(eval5, FiveCardsEvaluator.getRank(hand5(c3, c4, c5, c6, c7)));
        return (short) eval5;
    }

    private long hand5(int c1, int c2, int c3, int c4, int c5) {
        return (1L << c1) | (1L << c2) | (1L << c3) | (1L << c4) | (1L << c5);
    }

    @Test
    public void testFiveCardEvaluator() {
        for (int c1 = 0; c1 < 52; c1++) {
            for (int c2 = c1 + 1; c2 < 52; c2++) {
                for (int c3 = c2 + 1; c3 < 52; c3++) {
                    for (int c4 = c3 + 1; c4 < 52; c4++) {
                        for (int c5 = c4 + 1; c5 < 52; c5++) {
                            final long hand = (1L << c1) | (1L << c2) | (1L << c3) | (1L << c4) | (1L << c5);
                            assertEquals(SlowEvaluator.get5CardsHandClass(hand), FiveCardsEvaluator.getRank(hand));
                            assertEquals(
                                    FiveCardsEvaluator.getRank(hand), FiveCardsEvaluator.getRank(c1, c2, c3, c4, c5));
                        }
                    }
                }
            }
        }
    }
}
