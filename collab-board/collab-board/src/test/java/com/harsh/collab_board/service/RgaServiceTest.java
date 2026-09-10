package com.harsh.collab_board.service;

import com.harsh.collab_board.entity.CardCharacter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RgaServiceTest {

    private final RgaService rgaService = new RgaService();

    private CardCharacter ch(String charId, String value, String afterId, String siteId, long seq, boolean deleted) {
        CardCharacter c = new CardCharacter();
        c.setCharId(charId);
        c.setValue(value);
        c.setAfterId(afterId);
        c.setSiteId(siteId);
        c.setSeq(seq);
        c.setDeleted(deleted);
        return c;
    }

    private CardCharacter ch(String charId, String value, String afterId, String siteId, long seq) {
        return ch(charId, value, afterId, siteId, seq, false);
    }

    // Scenario 1: basic concurrent insert — two sites both insert at the
    // very start (afterId = null) at "the same time". Order they arrive
    // in the list shouldn't matter — result must converge.
    @Test
    void concurrentInsert_sameParent_convergesRegardlessOfFeedOrder() {
        CardCharacter a = ch("A-1", "A", null, "siteA", 1);
        CardCharacter b = ch("B-1", "B", null, "siteB", 1);

        String result1 = rgaService.buildText(List.of(a, b));
        String result2 = rgaService.buildText(List.of(b, a));

        assertEquals(result1, result2, "Order of delivery must not affect final text");
        // siteId "B" > "A", and comparator is reversed (descending), so B sorts first
        assertEquals("BA", result1);
    }

    // Scenario 2: concurrent insert at the exact same cursor position —
    // two sites both insert a child of the SAME existing character
    // (same afterId). This stresses the afterId collision/tiebreak logic.
    @Test
    void concurrentInsert_samePosition_convergesRegardlessOfFeedOrder() {
        CardCharacter root = ch("R-1", "H", null, "siteA", 1);
        CardCharacter x = ch("A-2", "X", "R-1", "siteA", 2);
        CardCharacter y = ch("B-1", "Y", "R-1", "siteB", 1);

        List<CardCharacter> order1 = List.of(root, x, y);
        List<CardCharacter> order2 = List.of(root, y, x);
        List<CardCharacter> order3 = List.of(y, x, root);

        String r1 = rgaService.buildText(order1);
        String r2 = rgaService.buildText(order2);
        String r3 = rgaService.buildText(order3);

        assertEquals(r1, r2);
        assertEquals(r2, r3);
        // siteB > siteA, so Y wins the tiebreak and sits before X
        assertEquals("HYX", r1);
    }

    // Scenario 3: delete + concurrent insert race — a character is
    // tombstoned (deleted) but a new character was inserted "after" it
    // concurrently. The insert must survive: the deleted char disappears
    // from visible text, but traversal still walks through it to reach
    // its children.
    @Test
    void deleteWithConcurrentInsert_insertSurvivesTombstone() {
        CardCharacter r1 = ch("A-1", "H", null, "siteA", 1);
        CardCharacter r2 = ch("A-2", "I", "A-1", "siteA", 2, true); // deleted
        CardCharacter r3 = ch("B-1", "!", "A-2", "siteB", 1);        // child of the deleted char

        String result = rgaService.buildText(List.of(r1, r2, r3));

        assertEquals("H!", result, "Deleted char must be hidden but its subtree must still render");
    }

    // Scenario 4: offline reconnect — Tab A queued edits offline while
    // Tab B edited concurrently; when Tab A reconnects, both sets of
    // operations get merged in one batch, in no particular order.
    // The invariant: however the combined operation list is shuffled,
    // every replica must converge to the identical visible text.
    @Test
    void offlineReconnect_mergedOpsConvergeUnderAnyShuffle() {
        CardCharacter r1 = ch("A-1", "H", null, "siteA", 1);
        CardCharacter r2 = ch("A-2", "e", "A-1", "siteA", 2);
        CardCharacter r3 = ch("A-3", "l", "A-2", "siteA", 3);
        // Tab B, offline concurrently, inserts after the same anchor A-3
        CardCharacter b1 = ch("B-1", "l", "A-3", "siteB", 1);
        CardCharacter b2 = ch("B-2", "o", "B-1", "siteB", 2);

        List<CardCharacter> base = new ArrayList<>(List.of(r1, r2, r3, b1, b2));
        String expected = rgaService.buildText(base);

        for (int i = 0; i < 20; i++) {
            List<CardCharacter> shuffled = new ArrayList<>(base);
            Collections.shuffle(shuffled);
            assertEquals(expected, rgaService.buildText(shuffled),
                    "Shuffled delivery order must still converge to same text");
        }
    }

    // Sanity check: buildOrderedChars should produce the same visible
    // sequence as buildText, just as CardCharacter objects instead of a string.
    @Test
    void buildOrderedChars_matchesBuildText() {
        CardCharacter r1 = ch("A-1", "H", null, "siteA", 1);
        CardCharacter r2 = ch("A-2", "I", "A-1", "siteA", 2, true);
        CardCharacter r3 = ch("B-1", "!", "A-2", "siteB", 1);

        List<CardCharacter> chars = List.of(r1, r2, r3);
        String text = rgaService.buildText(chars);
        List<CardCharacter> ordered = rgaService.buildOrderedChars(chars);

        StringBuilder rebuilt = new StringBuilder();
        for (CardCharacter c : ordered) rebuilt.append(c.getValue());

        assertEquals(text, rebuilt.toString());
        assertEquals(2, ordered.size(), "Deleted char should be excluded from ordered list");
    }
}