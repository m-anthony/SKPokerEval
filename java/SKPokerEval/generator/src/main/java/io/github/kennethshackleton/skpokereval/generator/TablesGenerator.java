package io.github.kennethshackleton.skpokereval.generator;

import static io.github.kennethshackleton.skpokereval.generator.ConstantPropertiesGenerator.propToIntArray;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import org.agrona.collections.IntArrayList;

public final class TablesGenerator {

    private static final int HIGHEST_FLUSH = 0x1FC0; // AKQJT98
    private static final int FACES = 13;
    private static final int SUITS = 4;
    private static final int HAND_CARDS_7 = 7;

    private static final Properties DECK_PROPERTIES = loadDeckProperties();

    private static final int[] SUIT_KEYS = propToIntArray(DECK_PROPERTIES, "SUIT_KEYS");
    private static final short[] FLUSH_RANKS = new short[1 + HIGHEST_FLUSH];
    private static final byte[] FLUSH_CHECK = generateFlushCheck();
    private static final byte[] FLUSH_CHECK_FIVE = generateFlushCheckFive();

    private static final int[] CARD_VALUE_KEYS = propToIntArray(DECK_PROPERTIES, "CARD_VALUE_KEYS");
    private static final int[] CARD_VALUE_KEYS_FIVE = propToIntArray(DECK_PROPERTIES, "CARD_VALUE_KEYS_FIVE");
    public static final HashTable FLAT_TABLE_FIVE = generateFlatTableFive();
    public static final HashTable FLAT_TABLE = generateFlatTable();
    public static final HashTable SHORT_TABLE_FIVE = HashTable.computeShorterHashTable(1352111, 9, FLAT_TABLE_FIVE);
    public static final HashTable SHORT_TABLE = HashTable.computeShorterHashTable2(2369371, 9, FLAT_TABLE);

    private TablesGenerator() {}

    public static void main(String[] args) throws IOException {
        HashTable.validate(FLAT_TABLE, SHORT_TABLE);
        System.out.println("7 cards table " + SHORT_TABLE);
        HashTable.validate(FLAT_TABLE_FIVE, SHORT_TABLE_FIVE);
        System.out.println("5 cards table " + SHORT_TABLE_FIVE);
        generateClassFile(new File(args[0]));
    }

    private static Properties loadDeckProperties() {
        final Properties tableProps = new Properties();
        try (var stream = TablesGenerator.class.getClassLoader().getResourceAsStream(Bootstrap.DECKCARDS_PROPERTIES)) {
            tableProps.load(stream);
        } catch (final Exception e) {
            // will return an empty property
        }
        return tableProps;
    }

    private static void generateClassFile(File directory) throws IOException {

        try (var sevenGen = new ConstantPropertiesGenerator(directory, "tables7.properties")) {
            sevenGen.addArray("SUIT_KEYS", TablesGenerator.SUIT_KEYS);
            sevenGen.addArray("CARD_VALUE_KEYS", TablesGenerator.CARD_VALUE_KEYS);
            sevenGen.addProperty("PRIME_HASH", Integer.toString(SHORT_TABLE.getPrimeSpread()));
            sevenGen.addArray("FLUSH_CHECK", TablesGenerator.FLUSH_CHECK);
            sevenGen.addArray("FLUSH_RANKS", TablesGenerator.FLUSH_RANKS);
            sevenGen.addArray("RANK_OFFSETS", SHORT_TABLE.getOffsets());
            sevenGen.addArray("RANK_HASHES", SHORT_TABLE.getRanks());
        }

        try (var fiveGen = new ConstantPropertiesGenerator(directory, "tables5.properties")) {
            fiveGen.addArray("SUIT_KEYS", TablesGenerator.SUIT_KEYS);
            fiveGen.addArray("CARD_VALUE_KEYS", TablesGenerator.CARD_VALUE_KEYS_FIVE);
            fiveGen.addProperty("PRIME_HASH", Integer.toString(SHORT_TABLE_FIVE.getPrimeSpread()));
            fiveGen.addArray("FLUSH_CHECK", TablesGenerator.FLUSH_CHECK_FIVE);
            fiveGen.addArray("FLUSH_RANKS", TablesGenerator.FLUSH_RANKS);
            fiveGen.addArray("RANK_OFFSETS", SHORT_TABLE_FIVE.getOffsets());
            fiveGen.addArray("RANK_HASHES", SHORT_TABLE_FIVE.getRanks());
        }
    }

    private static HashTable generateFlatTable() {
        fillSevenFlushRanks();
        final IntArrayList hashes = new IntArrayList();
        final int highestKey = SUITS * CARD_VALUE_KEYS[FACES - 1] + (HAND_CARDS_7 - SUITS) * CARD_VALUE_KEYS[FACES - 2];
        final short[] ranks = new short[1 + highestKey];
        for (int c1 = 0; c1 < FACES; c1++) {
            for (int c2 = c1; c2 < FACES; c2++) {
                for (int c3 = c2; c3 < FACES; c3++) {
                    for (int c4 = c3; c4 < FACES; c4++) {
                        for (int c5 = Math.max(1 + c1, c4); c5 < FACES; c5++) {
                            for (int c6 = Math.max(1 + c2, c5); c6 < FACES; c6++) {
                                for (int c7 = Math.max(1 + c3, c6); c7 < FACES; c7++) {
                                    final int hash = CARD_VALUE_KEYS[c1]
                                            + CARD_VALUE_KEYS[c2]
                                            + CARD_VALUE_KEYS[c3]
                                            + CARD_VALUE_KEYS[c4]
                                            + CARD_VALUE_KEYS[c5]
                                            + CARD_VALUE_KEYS[c6]
                                            + CARD_VALUE_KEYS[c7];
                                    hashes.add(hash);
                                    ranks[hash] = best5CardsRank(c1, c2, c3, c4, c5, c6, c7);
                                }
                            }
                        }
                    }
                }
            }
        }
        return HashTable.buildFlatTable(hashes.toIntArray(), ranks);
    }

    private static short best5CardsRank(int... cards) {
        int fullHash = 0;
        for (final int card : cards) {
            fullHash += CARD_VALUE_KEYS_FIVE[card];
        }

        int maxRank = 0;
        for (int i = 1; i < cards.length; i++) {
            for (int j = 0; j < i; j++) {
                final int hashToRemove = CARD_VALUE_KEYS_FIVE[cards[i]] + CARD_VALUE_KEYS_FIVE[cards[j]];
                maxRank = Math.max(maxRank, FLAT_TABLE_FIVE.getRank(fullHash - hashToRemove));
            }
        }
        return (short) maxRank;
    }

    private static HashTable generateFlatTableFive() {
        final IntArrayList hashes = new IntArrayList();
        final short[] ranks = new short[1 + SUITS * CARD_VALUE_KEYS_FIVE[FACES - 1] + CARD_VALUE_KEYS_FIVE[FACES - 2]];
        short currentRank = 1;

        // high cards
        for (int c1 = 0; c1 < FACES; c1++) {
            for (int c2 = 0; c2 < c1; c2++) {
                for (int c3 = 0; c3 < c2; c3++) {
                    for (int c4 = 0; c4 < c3; c4++) {
                        for (int c5 = 0; c5 < c4; c5++) {
                            final boolean straight = (c1 - c5 == 4) || (c1 == 12 && c2 == 3);
                            if (!straight) {
                                final int hash = CARD_VALUE_KEYS_FIVE[c1]
                                        + CARD_VALUE_KEYS_FIVE[c2]
                                        + CARD_VALUE_KEYS_FIVE[c3]
                                        + CARD_VALUE_KEYS_FIVE[c4]
                                        + CARD_VALUE_KEYS_FIVE[c5];
                                hashes.add(hash);
                                ranks[hash] = currentRank++;
                            }
                        }
                    }
                }
            }
        }

        // pairs
        for (int pair = 0; pair < FACES; pair++) {
            for (int c1 = 0; c1 < FACES; c1++) {
                if (c1 == pair) continue;
                for (int c2 = 0; c2 < c1; c2++) {
                    if (c2 == pair) continue;
                    for (int c3 = 0; c3 < c2; c3++) {
                        if (c3 != pair) {
                            final int hash = 2 * CARD_VALUE_KEYS_FIVE[pair]
                                    + CARD_VALUE_KEYS_FIVE[c1]
                                    + CARD_VALUE_KEYS_FIVE[c2]
                                    + CARD_VALUE_KEYS_FIVE[c3];
                            hashes.add(hash);
                            ranks[hash] = currentRank++;
                        }
                    }
                }
            }
        }

        // two pairs
        for (int p1 = 0; p1 < FACES; p1++) {
            for (int p2 = 0; p2 < p1; p2++) {
                for (int c = 0; c < FACES; c++) {
                    if (c != p1 && c != p2) {
                        final int hash =
                                2 * (CARD_VALUE_KEYS_FIVE[p1] + CARD_VALUE_KEYS_FIVE[p2]) + CARD_VALUE_KEYS_FIVE[c];
                        hashes.add(hash);
                        ranks[hash] = currentRank++;
                    }
                }
            }
        }

        // trips
        for (int trips = 0; trips < FACES; trips++) {
            for (int c1 = 0; c1 < FACES; c1++) {
                if (c1 == trips) continue;
                for (int c2 = 0; c2 < c1; c2++) {
                    if (c2 != trips) {
                        final int hash =
                                3 * CARD_VALUE_KEYS_FIVE[trips] + CARD_VALUE_KEYS_FIVE[c1] + CARD_VALUE_KEYS_FIVE[c2];
                        hashes.add(hash);
                        ranks[hash] = currentRank++;
                    }
                }
            }
        }

        // straight A2345
        final int lowStraightHash = CARD_VALUE_KEYS_FIVE[0]
                + CARD_VALUE_KEYS_FIVE[1]
                + CARD_VALUE_KEYS_FIVE[2]
                + CARD_VALUE_KEYS_FIVE[3]
                + CARD_VALUE_KEYS_FIVE[12];
        hashes.add(lowStraightHash);
        ranks[lowStraightHash] = currentRank++;

        // other straights
        for (int low = 0; low + 4 < FACES; low++) {
            final int hash = CARD_VALUE_KEYS_FIVE[low]
                    + CARD_VALUE_KEYS_FIVE[low + 1]
                    + CARD_VALUE_KEYS_FIVE[low + 2]
                    + CARD_VALUE_KEYS_FIVE[low + 3]
                    + CARD_VALUE_KEYS_FIVE[low + 4];
            hashes.add(hash);
            ranks[hash] = currentRank++;
        }

        // flush
        for (int c1 = 0; c1 < FACES; c1++) {
            for (int c2 = 0; c2 < c1; c2++) {
                for (int c3 = 0; c3 < c2; c3++) {
                    for (int c4 = 0; c4 < c3; c4++) {
                        for (int c5 = 0; c5 < c4; c5++) {
                            final boolean straight = (c1 - c5 == 4) || (c1 == 12 && c2 == 3);
                            if (!straight) {
                                FLUSH_RANKS[(1 << c1) | (1 << c2) | (1 << c3) | (1 << c4) | (1 << c5)] = currentRank++;
                            }
                        }
                    }
                }
            }
        }

        // Full house
        for (int trips = 0; trips < FACES; trips++) {
            for (int pair = 0; pair < FACES; pair++) {
                if (trips != pair) {
                    final int hash = 3 * CARD_VALUE_KEYS_FIVE[trips] + 2 * CARD_VALUE_KEYS_FIVE[pair];
                    hashes.add(hash);
                    ranks[hash] = currentRank++;
                }
            }
        }

        // Quad
        for (int quad = 0; quad < FACES; quad++) {
            for (int c = 0; c < FACES; c++) {
                if (quad != c) {
                    final int hash = 4 * CARD_VALUE_KEYS_FIVE[quad] + CARD_VALUE_KEYS_FIVE[c];
                    hashes.add(hash);
                    ranks[hash] = currentRank++;
                }
            }
        }

        // straight flush A2345
        FLUSH_RANKS[0x100F] = currentRank++;

        // other straight flush
        for (int low = 0; low < 9; low++) {
            FLUSH_RANKS[(0x1F << low)] = currentRank++;
        }
        return HashTable.buildFlatTable(hashes.toIntArray(), ranks);
    }

    private static void fillSevenFlushRanks() {
        // FLUSH_RANKS has been already filled for 5 cards, complete for 6 or 7 cards
        for (int hand = 0; hand < FLUSH_RANKS.length; hand++) {
            final int cards = Integer.bitCount(hand);
            if (cards != 6 && cards != 7) continue;
            short rank = 0;
            int iterator = hand;
            while (iterator != 0) {
                final int ignoredCard = 1 << Integer.numberOfTrailingZeros(iterator);
                iterator &= ~ignoredCard;
                rank = (short) Math.max(rank, FLUSH_RANKS[hand & ~ignoredCard]);
            }
            FLUSH_RANKS[hand] = rank;
        }
    }

    private static byte[] generateFlushCheck() {
        final byte[] flushCheck = new byte[1 + HAND_CARDS_7 * SUIT_KEYS[SUITS - 1]];
        Arrays.fill(flushCheck, (byte) -1);
        for (int suit1 = 0; suit1 < SUITS; suit1++) {
            for (int suit2 = 0; suit2 < SUITS; suit2++) {
                for (int suit3 = suit2; suit3 < SUITS; suit3++) {
                    final int hash = 5 * SUIT_KEYS[suit1] + SUIT_KEYS[suit2] + SUIT_KEYS[suit3];
                    flushCheck[hash] = (byte) suit1;
                }
            }
        }
        return flushCheck;
    }

    private static byte[] generateFlushCheckFive() {
        final byte[] flushCheck = new byte[1 + 5 * SUIT_KEYS[SUITS - 1]];
        Arrays.fill(flushCheck, (byte) -1);
        for (int suit1 = 0; suit1 < SUITS; suit1++) {
            final int hash = 5 * SUIT_KEYS[suit1];
            flushCheck[hash] = (byte) suit1;
        }
        return flushCheck;
    }
}
