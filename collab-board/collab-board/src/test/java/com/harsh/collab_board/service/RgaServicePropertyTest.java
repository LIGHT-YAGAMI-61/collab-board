package com.harsh.collab_board.service;

import com.harsh.collab_board.entity.CardCharacter;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RgaServicePropertyTest {

    private final RgaService rgaService = new RgaService();
    private static final String[] SITES = {"siteA", "siteB", "siteC"};

    /**
     * Builds a random but STRUCTURALLY VALID sequence of CardCharacter ops
     * (inserts + deletes), deterministically from a seed. "Valid" means
     * every afterId either points to an already-created character, or is
     * null (root). This mimics what real concurrent clients would produce.
     */
    private List<CardCharacter> generateRandomCharSet(long seed, int opCount) {
        Random rnd = new Random(seed);
        List<CardCharacter> all = new ArrayList<>();
        List<String> liveIds = new ArrayList<>(); // charIds eligible as afterId or for deletion
        liveIds.add(null); // represents "start of text"

        Map<String, Long> siteSeq = new HashMap<>();

        for (int i = 0; i < opCount; i++) {
            boolean doInsert = all.isEmpty() || rnd.nextDouble() < 0.75;

            if (doInsert) {
                String site = SITES[rnd.nextInt(SITES.length)];
                long seq = siteSeq.merge(site, 1L, Long::sum);
                String charId = site + "-" + seq;
                String afterId = liveIds.get(rnd.nextInt(liveIds.size()));
                char value = (char) ('a' + rnd.nextInt(26));

                CardCharacter c = new CardCharacter();
                c.setCharId(charId);
                c.setValue(String.valueOf(value));
                c.setAfterId(afterId);
                c.setSiteId(site);
                c.setSeq(seq);
                c.setDeleted(false);

                all.add(c);
                liveIds.add(charId);
            } else {
                // delete a random already-inserted (non-null) character
                List<CardCharacter> deletable = all.stream()
                        .filter(c -> !c.isDeleted())
                        .toList();
                if (!deletable.isEmpty()) {
                    CardCharacter target = deletable.get(rnd.nextInt(deletable.size()));
                    target.setDeleted(true);
                }
            }
        }
        return all;
    }

    private List<CardCharacter> shuffledCopy(List<CardCharacter> original, long shuffleSeed) {
        List<CardCharacter> copy = new ArrayList<>(original);
        Collections.shuffle(copy, new Random(shuffleSeed));
        return copy;
    }

    /**
     * CORE CRDT INVARIANT: given the same set of operations, every replica
     * converges to the same visible text, no matter what order the
     * operations were delivered/applied in.
     */
    @Property(tries = 200)
    void convergesRegardlessOfDeliveryOrder(
            @ForAll long seed,
            @ForAll @IntRange(min = 1, max = 40) int opCount,
            @ForAll long shuffleSeedA,
            @ForAll long shuffleSeedB) {

        List<CardCharacter> baseline = generateRandomCharSet(seed, opCount);

        String expected = rgaService.buildText(baseline);
        String shuffledResultA = rgaService.buildText(shuffledCopy(baseline, shuffleSeedA));
        String shuffledResultB = rgaService.buildText(shuffledCopy(baseline, shuffleSeedB));

        assertEquals(expected, shuffledResultA,
                "Shuffle A diverged — seed=" + seed + " opCount=" + opCount + " shuffleSeed=" + shuffleSeedA);
        assertEquals(expected, shuffledResultB,
                "Shuffle B diverged — seed=" + seed + " opCount=" + opCount + " shuffleSeed=" + shuffleSeedB);
    }

    /**
     * SECOND INVARIANT: the number of visible (non-deleted) characters in
     * the rendered output must always equal the number of non-deleted rows
     * fed in — nothing should be silently dropped or duplicated by the
     * traversal, regardless of feed order.
     */
    @Property(tries = 200)
    void visibleCharCountMatchesNonDeletedCount(
            @ForAll long seed,
            @ForAll @IntRange(min = 1, max = 40) int opCount,
            @ForAll long shuffleSeed) {

        List<CardCharacter> baseline = generateRandomCharSet(seed, opCount);
        long expectedCount = baseline.stream().filter(c -> !c.isDeleted()).count();

        List<CardCharacter> ordered = rgaService.buildOrderedChars(shuffledCopy(baseline, shuffleSeed));

        assertEquals(expectedCount, ordered.size(),
                "Visible char count mismatch — seed=" + seed + " opCount=" + opCount);
    }

    /**
     * THIRD INVARIANT: buildText and buildOrderedChars must always agree —
     * concatenating the ordered chars' values must equal buildText's output,
     * for any random tree/shuffle.
     */
    @Property(tries = 150)
    void buildTextAndBuildOrderedCharsAgree(
            @ForAll long seed,
            @ForAll @IntRange(min = 1, max = 40) int opCount,
            @ForAll long shuffleSeed) {

        List<CardCharacter> baseline = generateRandomCharSet(seed, opCount);
        List<CardCharacter> shuffled = shuffledCopy(baseline, shuffleSeed);

        String text = rgaService.buildText(shuffled);
        List<CardCharacter> ordered = rgaService.buildOrderedChars(shuffled);

        StringBuilder rebuilt = new StringBuilder();
        for (CardCharacter c : ordered) rebuilt.append(c.getValue());

        assertEquals(text, rebuilt.toString(),
                "buildText/buildOrderedChars disagree — seed=" + seed + " opCount=" + opCount);
    }
}