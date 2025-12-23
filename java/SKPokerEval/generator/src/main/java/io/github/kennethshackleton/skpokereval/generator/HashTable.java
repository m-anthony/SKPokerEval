package io.github.kennethshackleton.skpokereval.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.agrona.collections.IntArrayList;

public class HashTable {

    private final int keyBits;
    private final int keyMask;
    private int primeSpread;
    private int offsetShift;
    private final int[] hashes;
    private short[] offsets;
    private short[] ranks;
    private int offsetMask;

    public static HashTable buildFlatTable(int[] hashes, short[] ranks) {
        final HashTable hashTable = new HashTable(hashes);
        hashTable.primeSpread = 1;
        hashTable.offsetShift = hashTable.keyBits;
        hashTable.offsetMask = Integer.MAX_VALUE;
        hashTable.offsets = new short[] {0};
        hashTable.ranks = ranks;
        return hashTable;
    }

    private HashTable(int[] hashes) {
        this.hashes = hashes;
        keyBits = Integer.SIZE - Integer.numberOfLeadingZeros(hashes[hashes.length - 1]);
        keyMask = (1 << keyBits) - 1;
    }

    public short getRank(int key) {
        final int spreaded = (int) (keyMask & (getPrimeSpread() * (long) key));
        final int offset = offsets[spreaded >>> getOffsetShift()] & 0xFFFF;
        return ranks[offset + (spreaded & offsetMask)];
    }

    public int size() {
        return offsets.length + ranks.length;
    }

    short[] getOffsets() {
        return offsets;
    }

    short[] getRanks() {
        return ranks;
    }

    @Override
    public String toString() {
        return "HashTable [primeSpread="
                + getPrimeSpread()
                + ", offsetShift="
                + getOffsetShift()
                + ", size="
                + size()
                + ", rankSize="
                + ranks.length
                + "]";
    }

    public static HashTable computeShorterHashTable(int prime, int offsetShift, HashTable reference) {

        final int offsetLength = 1 << (reference.keyBits - offsetShift);
        final int maxRankLength = reference.size() - offsetLength;
        if (maxRankLength < 0) return null;

        final HashTable result = new HashTable(reference.hashes);
        result.primeSpread = prime;
        result.offsetShift = offsetShift;
        result.offsetMask = (1 << offsetShift) - 1;
        result.offsets = new short[offsetLength];
        final short[][] buckets = new short[result.offsets.length][];
        final long[][] bucketBitSets = new long[result.offsets.length][];

        final int maxKey = fillBuckets(buckets, bucketBitSets, result, reference);
        if (maxKey == -1) return null;

        final Map<Integer, List<Integer>> orderMap =
                new TreeMap<>(Comparator.<Integer>naturalOrder().reversed());
        for (int i = 0; i < bucketBitSets.length; i++) {
            if (bucketBitSets[i] != null) {
                int count = 0;
                for (int j = 0; j < bucketBitSets[i].length; j++) {
                    count += Long.bitCount(bucketBitSets[i][j]);
                }
                orderMap.computeIfAbsent(count, n -> new ArrayList<>()).add(i);
            }
        }

        final int[] bucketOrder = orderMap.values().stream()
                .flatMap(List::stream)
                .mapToInt(Integer::intValue)
                .toArray();

        final short[] ranks = new short[maxRankLength + result.offsetMask];
        final long[] rankBitSet = new long[2 + (ranks.length / Long.SIZE)];
        int rankLength = 0;
        for (final int idx : bucketOrder) {
            final short[] bucket = buckets[idx];
            final long[] bucketBitSet = bucketBitSets[idx];
            final int offset = findOffset(bucket, bucketBitSet, ranks, rankBitSet, 0, ranks.length);
            if (offset >>> Short.SIZE > 0) return null;
            rankLength =
                    Math.max(rankLength, writeBucket(idx, offset, result.offsets, buckets[idx], ranks, rankBitSet));
            if (rankLength >= maxRankLength) return null;
        }
        result.ranks = Arrays.copyOf(ranks, rankLength);

        return result;
    }

    private static int findOffset(
            short[] bucket, long[] bucketBitSet, short[] ranks, long[] rankBitSet, int start, int stop) {
        for (int offset = start; offset < stop; offset++) {
            boolean collision = false;
            for (int bitSetIdx = 0; bitSetIdx < bucketBitSet.length && !collision; bitSetIdx++) {
                if (bucketBitSet[bitSetIdx] == 0) continue;
                final int rankBitOffset = bitSetIdx + (offset / Long.SIZE);
                final int rankBitRemaining = offset & 63;
                long collisionBits = (rankBitSet[rankBitOffset] >>> rankBitRemaining)
                        | ((rankBitSet[rankBitOffset + 1] << (63 - rankBitRemaining)) << 1);
                collisionBits &= bucketBitSet[bitSetIdx];
                while (collisionBits != 0 && !collision) {
                    int index = Long.numberOfTrailingZeros(collisionBits);
                    collisionBits &= ~(1L << index);
                    index += bitSetIdx * Long.SIZE;
                    collision = bucket[index] != ranks[index + offset];
                }
            }

            if (!collision) return offset;
        }
        return stop;
    }

    private static int writeBucket(
            int idx, int offset, short[] offsets, short[] bucket, short[] ranks, long[] rankBitSet) {
        int rankLength = 0;
        offsets[idx] = (short) offset;
        for (int j = 0; j < bucket.length; j++) {
            if (bucket[j] != 0) {
                final int rankIdx = offset + j;
                if (bucket[j] != ranks[rankIdx] && ranks[rankIdx] != 0) {
                    throw new AssertionError();
                }
                ranks[rankIdx] = bucket[j];
                rankBitSet[rankIdx / Long.SIZE] |= 1L << (rankIdx & 63);
                rankLength = Math.max(rankLength, 1 + rankIdx);
            }
        }
        return rankLength;
    }

    private static int fillBuckets(short[][] buckets, long[][] bucketBitSets, HashTable config, HashTable reference) {
        int maxKey = 0;
        for (final int key : config.hashes) {
            final int h = (int) (reference.keyMask & (config.getPrimeSpread() * (long) key));
            short[] bucket = buckets[h >>> config.getOffsetShift()];
            if (bucket == null) {
                bucket = new short[1 << config.getOffsetShift()];
                buckets[h >>> config.getOffsetShift()] = bucket;
                bucketBitSets[h >>> config.getOffsetShift()] = new long[1 + (bucket.length / Long.SIZE)];
            }
            final int i = h & config.offsetMask;
            if (bucket[i] != 0) return -1;
            bucketBitSets[h >>> config.getOffsetShift()][i / Long.SIZE] |= 1L << (i & 63);
            bucket[i] = reference.getRank(key);
            maxKey = Math.max(maxKey, key);
        }
        return maxKey;
    }

    public static HashTable computeShorterHashTable2(int prime, int offsetShift, HashTable reference) {

        final int offsetLength = 1 << (reference.keyBits - offsetShift);
        final int maxRankLength = reference.size() - offsetLength;
        if (maxRankLength < 0) return null;

        final HashTable result = new HashTable(reference.hashes);
        result.primeSpread = prime;
        result.offsetShift = offsetShift;
        result.offsetMask = (1 << offsetShift) - 1;
        result.offsets = new short[offsetLength];
        final short[][] buckets = new short[result.offsets.length][];
        final long[][] bucketBitSets = new long[result.offsets.length][];

        final int maxKey = fillBuckets(buckets, bucketBitSets, result, reference);
        if (maxKey == -1) return null;

        final Map<Integer, IntArrayList> orderMap =
                new TreeMap<>(Comparator.<Integer>naturalOrder().reversed());
        for (int i = 0; i < bucketBitSets.length; i++) {
            if (bucketBitSets[i] != null) {
                int count = 0;
                for (int j = 0; j < bucketBitSets[i].length; j++) {
                    count += Long.bitCount(bucketBitSets[i][j]);
                }
                orderMap.computeIfAbsent(count, n -> new IntArrayList()).add(i);
            }
        }

        final short[] ranks = new short[maxRankLength + result.offsetMask];
        final long[] rankBitSet = new long[2 + (ranks.length / Long.SIZE)];
        int rankLength = 0;
        for (final var e : orderMap.entrySet()) {
            final IntArrayList bucketIndexes = e.getValue();
            final TreeMap<Integer, IntArrayList> offsetMap = new TreeMap<>();
            offsetMap.put(0, bucketIndexes);
            while (!offsetMap.isEmpty()) {
                final Entry<Integer, IntArrayList> entry = offsetMap.pollFirstEntry();
                final Integer nextKey = offsetMap.ceilingKey(1 + entry.getKey());
                final int min = entry.getKey();
                int max = nextKey == null ? ranks.length : nextKey;
                for (int i = 0; i < entry.getValue().size(); i++) {
                    final int idx = entry.getValue().getInt(i);
                    final int offset = findOffset(buckets[idx], bucketBitSets[idx], ranks, rankBitSet, min, max);
                    if (offset == min) {
                        max = min + 1;
                        if (offset >>> Short.SIZE > 0) return null;
                        rankLength = Math.max(
                                rankLength, writeBucket(idx, offset, result.offsets, buckets[idx], ranks, rankBitSet));
                        if (rankLength >= maxRankLength) return null;
                    } else {
                        offsetMap
                                .computeIfAbsent(offset, n -> new IntArrayList())
                                .addInt(idx);
                    }
                }
            }
        }
        result.ranks = Arrays.copyOf(ranks, rankLength);
        return result;
    }

    public static void validate(HashTable reference, HashTable result) {
        for (final int hash : reference.hashes) {
            if (result.getRank(hash) != reference.getRank(hash)) throw new AssertionError();
        }
    }

    public int getPrimeSpread() {
        return primeSpread;
    }

    public int getOffsetShift() {
        return offsetShift;
    }
}
