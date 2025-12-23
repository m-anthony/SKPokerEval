package io.github.kennethshackleton.skpokereval.generator;

import java.io.File;
import java.io.IOException;
import org.agrona.collections.IntHashSet;

public class Bootstrap {

    public static final String DECKCARDS_PROPERTIES = "Deckcards.properties";
    private static final int FACES = 13;
    private static final int SUITS = 4;

    public static void main(String[] args) throws IOException {
        final var generator = new ConstantPropertiesGenerator(new File(args[0]), DECKCARDS_PROPERTIES);
        generator.addArray("CARD_VALUE_KEYS", generateCardValueKeys());
        generator.addArray("CARD_VALUE_KEYS_FIVE", generateCardValueKeysFive());
        generator.addArray("SUIT_KEYS", generateCardSuitKeys());
        generator.close();
    }

    private static int[] generateCardValueKeys() {

        final int[] keys = new int[FACES];
        int nextKey = 0;
        // keep all hashes with previous validated keys, so that we can always have k7 =
        // k to compute only new hashes
        IntHashSet validatedHashes = new IntHashSet();
        for (int k = 0; k < FACES; k++) { // first key will always be 0
            final IntHashSet currentHashes = new IntHashSet(validatedHashes.capacity(), validatedHashes.loadFactor());
            boolean accept;
            do {
                accept = true;
                keys[k] = nextKey++;
                if (currentHashes.capacity() > validatedHashes.capacity()) {
                    // rehash validatedHashes because of capacity increase
                    final IntHashSet validatedHashesTmp =
                            new IntHashSet(currentHashes.capacity(), currentHashes.loadFactor());
                    validatedHashesTmp.addAll(validatedHashes);
                    validatedHashes = validatedHashesTmp;
                }
                currentHashes.copy(validatedHashes); // way faster than using clean+addAll
                for (int k1 = 0; k1 < k && accept; k1++) {
                    for (int k2 = k1; k2 < k && accept; k2++) {
                        for (int k3 = k2; k3 < k && accept; k3++) {
                            for (int k4 = k3; k4 <= k && accept; k4++) {
                                for (int k5 = Math.max(k4, 1 + k1); k5 <= k && accept; k5++) {
                                    for (int k6 = Math.max(k5, 1 + k2); k6 <= k && accept; k6++) {
                                        // k7 = k => k >= 1 + k3 <=> k3 < k
                                        accept = currentHashes.add(keys[k1] + keys[k2] + keys[k3] + keys[k4] + keys[k5]
                                                + keys[k6] + keys[k]);
                                    }
                                }
                            }
                        }
                    }
                }

            } while (!accept);
            validatedHashes = currentHashes;
        }
        return keys;
    }

    private static int[] generateCardValueKeysFive() {

        final int[] keys = new int[FACES];
        int nextKey = 0;
        // keep all hashes with previous validated keys, so that we can always have k7 =
        // k to compute only new hashes
        IntHashSet validatedHashes = new IntHashSet();
        for (int k = 0; k < FACES; k++) { // first key will always be 0
            final IntHashSet currentHashes = new IntHashSet(validatedHashes.capacity(), validatedHashes.loadFactor());
            boolean accept;
            do {
                accept = true;
                keys[k] = nextKey++;
                if (currentHashes.capacity() > validatedHashes.capacity()) {
                    // rehash validatedHashes because of capacity increase
                    final IntHashSet validatedHashesTmp =
                            new IntHashSet(currentHashes.capacity(), currentHashes.loadFactor());
                    validatedHashesTmp.addAll(validatedHashes);
                    validatedHashes = validatedHashesTmp;
                }
                currentHashes.copy(validatedHashes); // way faster than using clean+addAll
                for (int k1 = 0; k1 < k && accept; k1++) {
                    for (int k2 = k1; k2 <= k && accept; k2++) {
                        for (int k3 = k2; k3 <= k && accept; k3++) {
                            for (int k4 = k3; k4 <= k && accept; k4++) {
                                // k5 = k => k >= 1 + k1 <=> k1 < k
                                accept = currentHashes.add(keys[k1] + keys[k2] + keys[k3] + keys[k4] + keys[k]);
                            }
                        }
                    }
                }

            } while (!accept);
            validatedHashes = currentHashes;
        }
        return keys;
    }

    private static int[] generateCardSuitKeys() {
        final int[] keys = new int[SUITS];
        int nextKey = 0;
        // keep all hashes with previous validated keys, so that we can always have k7 =
        // k to compute only new hashes
        IntHashSet validatedHashes = new IntHashSet();

        for (int k = 0; k < SUITS; k++) { // first key will always be 0
            final IntHashSet currentHashes = new IntHashSet(validatedHashes.capacity(), validatedHashes.loadFactor());
            boolean accept;
            do {
                accept = true;
                keys[k] = nextKey++;
                if (currentHashes.capacity() > validatedHashes.capacity()) {
                    // rehash validatedHashes because of capacity increase
                    final IntHashSet validatedHashesTmp =
                            new IntHashSet(currentHashes.capacity(), currentHashes.loadFactor());
                    validatedHashesTmp.addAll(validatedHashes);
                    validatedHashes = validatedHashesTmp;
                }
                currentHashes.copy(validatedHashes); // way faster than using addAll
                for (int k1 = 0; k1 <= k && accept; k1++) {
                    for (int k2 = k1; k2 <= k && accept; k2++) {
                        for (int k3 = k2; k3 <= k && accept; k3++) {
                            for (int k4 = k3; k4 <= k && accept; k4++) {
                                for (int k5 = k4; k5 <= k && accept; k5++) {
                                    for (int k6 = k5; k6 <= k && accept; k6++) {
                                        accept = currentHashes.add(keys[k1] + keys[k2] + keys[k3] + keys[k4] + keys[k5]
                                                + keys[k6] + keys[k]);
                                    }
                                }
                            }
                        }
                    }
                }

            } while (!accept);
            validatedHashes = currentHashes;
        }
        return keys;
    }
}
