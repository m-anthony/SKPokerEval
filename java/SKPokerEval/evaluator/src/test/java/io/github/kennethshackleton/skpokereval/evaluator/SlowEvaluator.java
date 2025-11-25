package io.github.kennethshackleton.skpokereval.evaluator;

public class SlowEvaluator {
    // 1 to 1277 for non-straight and -1 to -10 for straights, used to rank high
    // cards, straights, flushes, straight flushes
    private static final short[] NO_PAIR_RANKS;
    private static final short[] TWO_VALUES_RANKS;
    private static final short[] THREE_VALUES_RANKS;
    private static final short MAX_HIGH_CARD_CLASS;
    private static final short MAX_PAIR_CLASS;
    private static final short MAX_DOUBLE_PAIR_CLASS;
    private static final short MAX_THREE_OF_A_KIND_CLASS;
    private static final short MAX_STRAIGHT_CLASS;
    private static final short MAX_FLUSH_CLASS;
    private static final short MAX_FULL_CLASS;
    private static final short MAX_QUADS_CLASS;
    private static final short MAX_STRAIGHT_FLUSH_CLASS;

    static {
        NO_PAIR_RANKS = initNoPairRanks();
        THREE_VALUES_RANKS = initTripleRanks();
        TWO_VALUES_RANKS = initDoubleRanks();
        // init MAX classes from lowest to highest
        MAX_HIGH_CARD_CLASS = getNoFlushNoPairClass(0x1E80); // AKQJ9
        MAX_PAIR_CLASS = getPairClass(12, 0xE00); // AAKQJ
        MAX_DOUBLE_PAIR_CLASS = getDoublePairClass(0x1800, 10); // AAKKQ
        MAX_THREE_OF_A_KIND_CLASS = getTripsClass(12, 0xC00); // AAAKQ
        MAX_STRAIGHT_CLASS = getNoFlushNoPairClass(0x1F00); // AKQJT
        MAX_FLUSH_CLASS = getFlushClass(0x1E80); // AKQJ9
        MAX_FULL_CLASS = getFullClass(12, 11); // AAAKK
        MAX_QUADS_CLASS = getQuadsClass(12, 11); // AAAAK
        MAX_STRAIGHT_FLUSH_CLASS = getFlushClass(0x1F00); // AKQJT
        if (MAX_STRAIGHT_FLUSH_CLASS != 7462) throw new AssertionError();
    }

    public static short get7CardsHandClass(long hand) {
        int xor = 0;
        int or = 0;
        int and = 0x1FFF;
        long hand2 = hand;
        int flushRank = 0;
        for (int i = 0; i < 4; i++) {
            final int suitCards = (int) hand2 & 0x1FFF;
            if (Integer.bitCount(suitCards) >= 5) flushRank = getFlushClass(suitCards);
            hand2 >>>= 13;
            xor ^= suitCards;
            or |= suitCards;
            and &= suitCards;
        }

        if (and != 0) {
            // quads -> no possible straight flush
            return getQuadsClass(Integer.numberOfTrailingZeros(and), 31 - Integer.numberOfLeadingZeros(or & ~and));
        }
        final int distinctValues = Integer.bitCount(or);
        if (distinctValues == 3) {
            // 3 + 3 + 1 or 3 + 2 + 2 (since no quad) -> full
            int trips;
            int pair;
            if (Integer.bitCount(xor) == 1) {
                // 322
                trips = Integer.numberOfTrailingZeros(xor);
                pair = 31 - Integer.numberOfLeadingZeros(or & ~xor);
            } else {
                // 331
                trips = (int) (hand & (hand >>> 26));
                trips = (trips | (trips >>> 13)) & or;
                pair = Integer.numberOfTrailingZeros(trips);
                trips = 31 - Integer.numberOfLeadingZeros(trips);
            }
            return getFullClass(trips, pair);
        }
        if (distinctValues == 4) {
            // 3 + 2 + 1 + 1 (full) or 2 + 2 + 2 + 1 (DP)
            if (Integer.bitCount(xor) == 1) {
                // DP
                int pairs = or ^ xor;
                pairs -= 1 << Integer.numberOfTrailingZeros(pairs);
                final int kicker = 31 - Integer.numberOfLeadingZeros(or & ~pairs);
                return getDoublePairClass(pairs, kicker);
            } else {
                // full
                int trips = (int) (hand & (hand >>> 26));
                trips = (trips | (trips >>> 13)) & xor;
                trips = Integer.numberOfTrailingZeros(trips);
                final int pair = Integer.numberOfTrailingZeros(or & ~xor);
                return getFullClass(trips, pair);
            }
        }
        // distinct values >= 5 => no more possible full
        if (flushRank > 0) return (short) flushRank;
        final short highCardClass = getNoFlushNoPairClass(or);
        if (highCardClass > MAX_THREE_OF_A_KIND_CLASS) return highCardClass; // straight

        if (distinctValues == 5) {
            // 3 + 1 + 1 + 1 + 1 or 2 + 2 + 1 + 1 + 1
            if (xor != or) {
                // DP
                final int pairs = or & ~xor;
                final int kicker = 31 - Integer.numberOfLeadingZeros(or & ~pairs);
                return getDoublePairClass(pairs, kicker);
            } else {
                // trips
                int trips = (int) (hand & (hand >>> 26));
                trips = (trips | (trips >>> 13)) & xor;
                int kicker = or & ~trips; // 4 bits
                trips = Integer.numberOfTrailingZeros(trips);
                kicker &= ~(1 << Integer.numberOfTrailingZeros(kicker));
                kicker &= ~(1 << Integer.numberOfTrailingZeros(kicker));
                return getTripsClass(trips, kicker);
            }
        } else if (distinctValues == 6) {
            // 2 + 1 + 1 + 1 + 1 + 1
            final int pair = Integer.numberOfTrailingZeros(or & ~xor);
            int kicker = xor & ~(1 << Integer.numberOfTrailingZeros(xor));
            kicker = kicker & ~(1 << Integer.numberOfTrailingZeros(kicker));
            return getPairClass(pair, kicker);
        }
        return highCardClass;
    }

    public static short get5CardsHandClass(long hand) {
        int xor = 0;
        int or = 0;
        int and = 0x1FFF;
        long hand2 = hand;
        boolean flush = false;
        for (int i = 0; i < 4; i++) {
            final int suitCards = (int) hand2 & 0x1FFF;
            flush |= Integer.bitCount(suitCards) == 5;
            hand2 >>>= 13;
            xor ^= suitCards;
            or |= suitCards;
            and &= suitCards;
        }
        if (flush) return getFlushClass(or);
        final int distinctValues = Integer.bitCount(or);
        if (distinctValues == 5) {
            return getNoFlushNoPairClass(or);
        } else if (distinctValues == 4) {
            // 1 pair, xor = kickers
            final int pair = Integer.numberOfTrailingZeros(or ^ xor);
            return getPairClass(pair, xor);
        } else if (distinctValues == 3) { // 3 of a kind or 2 pairs
            if (Integer.bitCount(xor) == 1) {
                // DP, xor = kicker
                return getDoublePairClass(or & ~xor, Integer.numberOfTrailingZeros(xor));
            } else {
                // 3 of a kind
                int tripsBit = (int) (hand & (hand >>> 26));
                tripsBit = (tripsBit | (tripsBit >> 13)) & 0x1FFF;
                return getTripsClass(Integer.numberOfTrailingZeros(tripsBit), or ^ tripsBit);
            }
        } else if (distinctValues == 2) { // quads or full
            if (and == 0) {
                // full, xor = trips
                return getFullClass(Integer.numberOfTrailingZeros(xor), Integer.numberOfTrailingZeros(xor ^ or));
            } else {
                // quads
                return getQuadsClass(Integer.numberOfTrailingZeros(and), Integer.numberOfTrailingZeros(xor));
            }
        }

        return 0;
    }

    private static short getFlushClass(int cardValuesBits) {
        final short rank = NO_PAIR_RANKS[cardValuesBits];
        return (short) (rank >= 0 ? rank + MAX_STRAIGHT_CLASS : MAX_QUADS_CLASS - rank);
    }

    private static short getNoFlushNoPairClass(int cardValuesBits) {
        final short rank = NO_PAIR_RANKS[cardValuesBits];
        return (short) (rank >= 0 ? rank : MAX_THREE_OF_A_KIND_CLASS - rank);
    }

    private static short getPairClass(int pair, int kickers) {
        // for each pair, 12*11*10/6 = 220 kickers possible
        final int kickersLow = kickers & ((1 << pair) - 1);
        final int kickersHigh = kickers >>> (1 + pair);
        final int normalizedKickers = kickersLow | (kickersHigh << pair);
        return (short) (MAX_HIGH_CARD_CLASS + pair * 220 + THREE_VALUES_RANKS[normalizedKickers]);
    }

    private static short getDoublePairClass(int pairs, int kicker) {
        // 11 possible kickers for each double pair
        final int dpRank = TWO_VALUES_RANKS[pairs] - 1;
        kicker -= Integer.bitCount(pairs & ((1 << kicker) - 1));
        return (short) (MAX_PAIR_CLASS + dpRank * 11 + 1 + kicker);
    }

    private static short getTripsClass(int trips, int kickers) {
        // for each trips, 12*11/2 = 66 kickers
        // normalize kickers
        final int kickersLow = kickers & ((1 << trips) - 1);
        final int kickersHigh = kickers >>> (1 + trips);
        final int normalizedKickers = kickersLow | (kickersHigh << trips);
        return (short) (MAX_DOUBLE_PAIR_CLASS + trips * 66 + TWO_VALUES_RANKS[normalizedKickers]);
    }

    // trips != pair
    private static short getFullClass(int trips, int pair) {
        if (pair > trips) pair--;
        return (short) (MAX_FLUSH_CLASS + trips * 12 + pair + 1);
    }

    private static short getQuadsClass(int quads, int kicker) {
        if (kicker > quads) kicker--;
        return (short) (MAX_FULL_CLASS + quads * 12 + kicker + 1);
    }

    private static short[] initDoubleRanks() {
        final short[] ranks = new short[1 + 0x1800]; // 1 1000 0000 0000 = AK;
        short rank = 0;
        for (int i = 0; i < ranks.length; i++) {
            if (Integer.bitCount(i) == 2) ranks[i] = ++rank;
        }
        return ranks;
    }

    private static short[] initTripleRanks() {

        final short[] ranks = new short[1 + 0x1C00]; // 1 1100 0000 0000 = AKQ;
        short rank = 0;
        for (int i = 0; i < ranks.length; i++) {
            if (Integer.bitCount(i) == 3) ranks[i] = ++rank;
        }
        return ranks;
    }

    private static short[] initNoPairRanks() {
        final short[] ranks = new short[1 + 0x1FC0]; // 1 1111 1100 0000

        // put ranks for straight first, from -1 (A2345) to -10 (AKQJT)
        int straight = 0x1F00; // A-high straight 1 1111 0000 0000
        for (int i = 0; i < 9; i++) { // 10 kinds of straight, but A2345 is special
            ranks[straight] = (short) (i - 10);
            straight >>= 1;
        }
        if (Integer.bitCount(straight) != 4) throw new AssertionError();
        ranks[straight | (1 << 12)] = (short) -1; // 2345 | A

        short rank = 0;
        for (int i = 0; i < ranks.length; i++) {
            switch (Integer.bitCount(i)) {
                case 5 -> {
                    if (ranks[i] == 0) ranks[i] = ++rank;
                }
                case 6, 7 -> {
                    // 6 or 7 cards => rank of the best 5 cards subset
                    int bestReducedRank = 0;
                    int iterator = i;
                    while (iterator > 0) {
                        final int ignored = 1 << Integer.numberOfTrailingZeros(iterator);
                        iterator &= ~ignored;
                        final int reducedRank = ranks[i & ~ignored];
                        if (reducedRank < 0) {
                            bestReducedRank = Math.min(reducedRank, bestReducedRank);
                        } else if (bestReducedRank >= 0) {
                            bestReducedRank = Math.max(reducedRank, bestReducedRank);
                        } // else not a straight and we already have a straight => do nothing
                    }
                    ranks[i] = (short) bestReducedRank;
                }
                default -> {}
            }
        }
        return ranks;
    }
}
