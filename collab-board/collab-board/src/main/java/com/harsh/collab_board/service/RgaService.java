package com.harsh.collab_board.service;

import com.harsh.collab_board.entity.CardCharacter;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RgaService {

    /**
     * Rebuilds the visible description text from a card's character rows.
     * This is the CRDT merge/traversal step — it works correctly no matter
     * what order the rows came from (server, offline queue, etc), because
     * ordering is fully determined by afterId + siteId + seq, not by
     * insertion order or row id.
     */
    public String buildText(List<CardCharacter> characters) {
        Map<String, List<CardCharacter>> childrenByParent = buildChildrenMap(characters);
        StringBuilder result = new StringBuilder();
        walk(null, childrenByParent, result);
        return result.toString();
    }

    /**
     * Same traversal as buildText, but returns the actual visible CardCharacter
     * objects in order (with charId) instead of a flattened string. This is
     * what the frontend needs to compute afterId for new inserts and to target
     * deletes correctly.
     */
    public List<CardCharacter> buildOrderedChars(List<CardCharacter> characters) {
        Map<String, List<CardCharacter>> childrenByParent = buildChildrenMap(characters);
        List<CardCharacter> result = new ArrayList<>();
        walkOrdered(null, childrenByParent, result);
        return result;
    }

    private Map<String, List<CardCharacter>> buildChildrenMap(List<CardCharacter> characters) {
        Map<String, List<CardCharacter>> childrenByParent = new HashMap<>();
        for (CardCharacter c : characters) {
            childrenByParent
                    .computeIfAbsent(c.getAfterId(), k -> new ArrayList<>())
                    .add(c);
        }

        Comparator<CardCharacter> tieBreaker = Comparator
                .comparing(CardCharacter::getSiteId)
                .thenComparing(CardCharacter::getSeq)
                .reversed();

        for (List<CardCharacter> siblings : childrenByParent.values()) {
            siblings.sort(tieBreaker);
        }
        return childrenByParent;
    }

    private void walk(String parentId,
                      Map<String, List<CardCharacter>> childrenByParent,
                      StringBuilder result) {
        List<CardCharacter> children = childrenByParent.get(parentId);
        if (children == null) return;

        for (CardCharacter c : children) {
            if (!c.isDeleted()) {
                result.append(c.getValue());
            }
            walk(c.getCharId(), childrenByParent, result);
        }
    }

    private void walkOrdered(String parentId,
                             Map<String, List<CardCharacter>> childrenByParent,
                             List<CardCharacter> result) {
        List<CardCharacter> children = childrenByParent.get(parentId);
        if (children == null) return;

        for (CardCharacter c : children) {
            if (!c.isDeleted()) {
                result.add(c);
            }
            walkOrdered(c.getCharId(), childrenByParent, result);
        }
    }
}