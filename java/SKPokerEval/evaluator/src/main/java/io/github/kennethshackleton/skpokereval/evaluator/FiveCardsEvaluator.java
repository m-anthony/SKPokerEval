package io.github.kennethshackleton.skpokereval.evaluator;

import static java.lang.Integer.numberOfLeadingZeros;

import java.io.IOException;
import java.util.Properties;

public class FiveCardsEvaluator {

    private static final int SUITS = 4;
    private static final int FACES = 13;

    private static final int FLUSH_BIT_SHIFT;
    private static final short[] FLUSH_RANKS;
    private static final int[] CARDS = new int[SUITS * FACES];
    private static final short[] HASH_RANKS;
    private static final int HASH_PRIME;
    private static final short[] HASH_OFFSETS;
    private static final byte[] FLUSH_CHECK;

    static {
        final Properties tableProps = new Properties();
        try (var stream = FiveCardsEvaluator.class.getClassLoader().getResourceAsStream("tables5.properties")) {
            tableProps.load(stream);
        } catch (final IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        final int[] cardValuesKeys = PropertiesArrayReader.propToIntArray(tableProps, "CARD_VALUE_KEYS");
        final int[] suitKeys = PropertiesArrayReader.propToIntArray(tableProps, "SUIT_KEYS");
        final int maxValueKey = SUITS * cardValuesKeys[FACES - 1] + cardValuesKeys[FACES - 2];
        FLUSH_BIT_SHIFT = 32 - numberOfLeadingZeros(maxValueKey);
        for (int suit = 0; suit < SUITS; suit++) {
            for (int value = 0; value < FACES; value++) {
                CARDS[value + FACES * suit] = cardValuesKeys[value] + (suitKeys[suit] << FLUSH_BIT_SHIFT);
            }
        }

        FLUSH_CHECK = PropertiesArrayReader.propToByteArray(tableProps, "FLUSH_CHECK");
        FLUSH_RANKS = PropertiesArrayReader.propToShortArray(tableProps, "FLUSH_RANKS");
        HASH_PRIME = Integer.parseInt(tableProps.getProperty("PRIME_HASH"));
        HASH_OFFSETS = PropertiesArrayReader.propToShortArray(tableProps, "RANK_OFFSETS");
        HASH_RANKS = PropertiesArrayReader.propToShortArray(tableProps, "RANK_HASHES");
    }

    private static final int FACE_BIT_MASK = (1 << FLUSH_BIT_SHIFT) - 1;
    private static final int RANK_OFFSET_SHIFT = FLUSH_BIT_SHIFT - (32 - numberOfLeadingZeros(HASH_OFFSETS.length - 1));
    private static final int RANK_HASH_MOD = (1 << RANK_OFFSET_SHIFT) - 1;

    public static short getRank(long hand) {
        int key = 0;
        long hand2 = hand;
        while (hand2 != 0) {
            final int card = Long.numberOfTrailingZeros(hand2);
            key += CARDS[card];
            hand2 &= ~(1L << card);
        }

        final short suit = FLUSH_CHECK[key >>> FLUSH_BIT_SHIFT];
        if (suit != -1) {
            return FLUSH_RANKS[(int) (hand >> (FACES * suit)) & (0x1FFF)];
        } else {
            final int hash = (int) (FACE_BIT_MASK & (HASH_PRIME * (long) key));
            final int offset = HASH_OFFSETS[hash >>> RANK_OFFSET_SHIFT] & 0xFFFF;
            return HASH_RANKS[offset + (hash & RANK_HASH_MOD)];
        }
    }

    public static short getRank(int c1, int c2, int c3, int c4, int c5) {
        final int key = CARDS[c1] + CARDS[c2] + CARDS[c3] + CARDS[c4] + CARDS[c5];
        final short suit = FLUSH_CHECK[key >>> FLUSH_BIT_SHIFT];
        if (suit != -1) {
            final long hand = (1L << c1) | (1L << c2) | (1L << c3) | (1L << c4) | (1L << c5);
            return FLUSH_RANKS[(int) (hand >> (FACES * suit)) & (0x1FFF)];
        } else {
            final int hash = (int) (FACE_BIT_MASK & (HASH_PRIME * (long) key));
            final int offset = HASH_OFFSETS[hash >>> RANK_OFFSET_SHIFT] & 0xFFFF;
            return HASH_RANKS[offset + (hash & RANK_HASH_MOD)];
        }
    }
}
