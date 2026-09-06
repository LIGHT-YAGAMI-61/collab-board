package com.harsh.collab_board.controller;

import com.harsh.collab_board.entity.CardCharacter;
import com.harsh.collab_board.service.CardCharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.harsh.collab_board.dto.InsertCharRequest;


import java.util.List;

@RestController
@RequestMapping("/api/cards/{cardId}/characters")
public class CardCharacterController {

    @Autowired
    private CardCharacterService cardCharacterService;


    @PostMapping
    public CardCharacter insert(@PathVariable Long cardId,
                                @RequestBody InsertCharRequest request,
                                Authentication authentication) {
        return cardCharacterService.insertCharacter(
                cardId, request.charId, request.value,
                request.afterId, request.siteId, request.seq,
                authentication.getName()
        );
    }

    @DeleteMapping("/{charId}")
    public CardCharacter delete(@PathVariable Long cardId,
                                @PathVariable String charId,
                                Authentication authentication) {
        return cardCharacterService.deleteCharacter(cardId, charId, authentication.getName());
    }

    @PostMapping("/batch")
    public List<CardCharacter> insertBatch(@PathVariable Long cardId,
                                           @RequestBody List<InsertCharRequest> requests,
                                           Authentication authentication) {
        return cardCharacterService.insertCharacters(cardId, requests, authentication.getName());
    }

    @DeleteMapping("/batch")
    public List<CardCharacter> deleteBatch(@PathVariable Long cardId,
                                           @RequestBody List<String> charIds,
                                           Authentication authentication) {
        return cardCharacterService.deleteCharacters(cardId, charIds, authentication.getName());
    }

    @GetMapping
    public List<CardCharacter> getAll(@PathVariable Long cardId,
                                      Authentication authentication) {
        return cardCharacterService.getCharacters(cardId, authentication.getName());
    }

    @GetMapping("/text")
    public String getText(@PathVariable Long cardId,
                          Authentication authentication) {
        return cardCharacterService.getDescriptionText(cardId, authentication.getName());
    }

    @GetMapping("/ordered")
    public List<CardCharacter> getOrdered(@PathVariable Long cardId,
                                          Authentication authentication) {
        return cardCharacterService.getOrderedCharacters(cardId, authentication.getName());
    }
}