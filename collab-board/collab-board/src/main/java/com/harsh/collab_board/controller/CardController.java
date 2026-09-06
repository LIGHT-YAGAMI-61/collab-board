package com.harsh.collab_board.controller;


import com.harsh.collab_board.dto.MoveCardRequest;
import com.harsh.collab_board.entity.Card;
import com.harsh.collab_board.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists/{listId}/cards")
public class CardController {


        @Autowired
    private CardService cardService ;

        @PostMapping
    public Card createCard(@PathVariable Long listId , @RequestBody Card card , org.springframework.security.core.Authentication authentication ) {
            return cardService.createCard(listId , card , authentication.getName()) ;
        }

        @GetMapping
    public List<Card> getCardsByList(@PathVariable Long listId  , org.springframework.security.core.Authentication authentication) {
            return cardService.getCardsByList(listId , authentication.getName()) ;
        }

        @PutMapping("/{id}")
    public Card updateCard(@PathVariable Long listId , @PathVariable Long id , @RequestBody Card card , org.springframework.security.core.Authentication authentication) {
            return cardService.updateCard(id , card , authentication.getName()) ;
        }

        @DeleteMapping("/{id}")
    public void deleteCard(@PathVariable Long listId , @PathVariable Long id , org.springframework.security.core.Authentication authentication ) {
             cardService.deleteCard(id , authentication.getName()) ;
        }

        @PutMapping("/{id}/move")
    public Card moveCard(@PathVariable Long listId, @PathVariable Long id , @RequestBody MoveCardRequest request , org.springframework.security.core.Authentication authentication) {
            return cardService.moveCard(id , request.getTargetListId() , request.getTargetPosition() , authentication.getName()) ;
        }

}
