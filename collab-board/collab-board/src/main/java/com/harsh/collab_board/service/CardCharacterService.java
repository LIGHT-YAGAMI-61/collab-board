package com.harsh.collab_board.service;

import com.harsh.collab_board.controller.CardCharacterController;
import com.harsh.collab_board.dto.CharOperationMessage;
import com.harsh.collab_board.entity.Card;
import com.harsh.collab_board.entity.CardCharacter;
import com.harsh.collab_board.entity.User;
import com.harsh.collab_board.repository.CardCharacterRepository;
import com.harsh.collab_board.repository.CardRepository;
import com.harsh.collab_board.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.harsh.collab_board.dto.InsertCharRequest;

import java.util.List;

@Service
public class CardCharacterService {

        @Autowired
        private CardCharacterRepository cardCharacterRepository;

        @Autowired
        private CardRepository cardRepository;

        @Autowired
        private RgaService rgaService;

        @Autowired
        private SimpMessagingTemplate messagingTemplate;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private BoardMemberService boardMemberService;

        private void broadcastCharOp(Long boardId, CharOperationMessage message) {
                messagingTemplate.convertAndSend("/topic/board/" + boardId, message);
        }

        private void checkMembership(Long boardId, String username) {
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                if (!boardMemberService.isMember(boardId, user.getId())) {
                        throw new RuntimeException("You are not a member of this board");
                }
        }

        public CardCharacter insertCharacter(Long cardId, String charId, String value,
                                             String afterId, String siteId, Long seq,
                                             String username) {

                Card card = cardRepository.findById(cardId)
                        .orElseThrow(() -> new RuntimeException("Card not found"));

                Long boardId = card.getBoardList().getBoard().getId();
                checkMembership(boardId, username);

                var existing = cardCharacterRepository.findByCardIdAndCharId(cardId, charId);
                if (existing.isPresent()) {
                        return existing.get();
                }

                CardCharacter character = new CardCharacter();
                character.setCard(card);
                character.setCharId(charId);
                character.setValue(value);
                character.setAfterId(afterId);
                character.setSiteId(siteId);
                character.setSeq(seq);
                character.setDeleted(false);

                CardCharacter saved = cardCharacterRepository.save(character);

                CharOperationMessage message = new CharOperationMessage();
                message.setType("CHAR_INSERTED");
                message.setCardId(cardId);
                message.setCharId(charId);
                message.setValue(value);
                message.setAfterId(afterId);
                message.setSiteId(siteId);
                message.setSeq(seq);
                broadcastCharOp(boardId, message);

                return saved;
        }

        public CardCharacter deleteCharacter(Long cardId, String charId, String username) {
                CardCharacter character = cardCharacterRepository
                        .findByCardIdAndCharId(cardId, charId)
                        .orElseThrow(() -> new RuntimeException("Character not found"));

                Long boardId = character.getCard().getBoardList().getBoard().getId();
                checkMembership(boardId, username);

                character.setDeleted(true);
                CardCharacter saved = cardCharacterRepository.save(character);

                CharOperationMessage message = new CharOperationMessage();
                message.setType("CHAR_DELETED");
                message.setCardId(cardId);
                message.setCharId(charId);
                broadcastCharOp(boardId, message);

                return saved;
        }

        public List<CardCharacter> insertCharacters(Long cardId, List<InsertCharRequest> requests, String username) {
                Card card = cardRepository.findById(cardId)
                        .orElseThrow(() -> new RuntimeException("Card not found"));

                Long boardId = card.getBoardList().getBoard().getId();
                checkMembership(boardId, username);

                List<CardCharacter> saved = new java.util.ArrayList<>();
                for (InsertCharRequest req : requests) {
                        var existing = cardCharacterRepository.findByCardIdAndCharId(cardId, req.charId);
                        if (existing.isPresent()) {
                                saved.add(existing.get());
                                continue;
                        }

                        CardCharacter character = new CardCharacter();
                        character.setCard(card);
                        character.setCharId(req.charId);
                        character.setValue(req.value);
                        character.setAfterId(req.afterId);
                        character.setSiteId(req.siteId);
                        character.setSeq(req.seq);
                        character.setDeleted(false);
                        saved.add(cardCharacterRepository.save(character));

                        CharOperationMessage message = new CharOperationMessage();
                        message.setType("CHAR_INSERTED");
                        message.setCardId(cardId);
                        message.setCharId(req.charId);
                        message.setValue(req.value);
                        message.setAfterId(req.afterId);
                        message.setSiteId(req.siteId);
                        message.setSeq(req.seq);
                        broadcastCharOp(boardId, message);
                }
                return saved;
        }

        public List<CardCharacter> deleteCharacters(Long cardId, List<String> charIds, String username) {
                List<CardCharacter> saved = new java.util.ArrayList<>();
                for (String charId : charIds) {
                        CardCharacter character = cardCharacterRepository
                                .findByCardIdAndCharId(cardId, charId)
                                .orElseThrow(() -> new RuntimeException("Character not found"));

                        Long boardId = character.getCard().getBoardList().getBoard().getId();
                        checkMembership(boardId, username);

                        character.setDeleted(true);
                        saved.add(cardCharacterRepository.save(character));

                        CharOperationMessage message = new CharOperationMessage();
                        message.setType("CHAR_DELETED");
                        message.setCardId(cardId);
                        message.setCharId(charId);
                        broadcastCharOp(boardId, message);
                }
                return saved;
        }

        public String getDescriptionText(Long cardId, String username) {
                Card card = cardRepository.findById(cardId)
                        .orElseThrow(() -> new RuntimeException("Card not found"));
                checkMembership(card.getBoardList().getBoard().getId(), username);

                List<CardCharacter> characters = cardCharacterRepository.findByCardId(cardId);
                return rgaService.buildText(characters);
        }

        public List<CardCharacter> getCharacters(Long cardId, String username) {
                Card card = cardRepository.findById(cardId)
                        .orElseThrow(() -> new RuntimeException("Card not found"));
                checkMembership(card.getBoardList().getBoard().getId(), username);

                return cardCharacterRepository.findByCardId(cardId);
        }

        public List<CardCharacter> getOrderedCharacters(Long cardId, String username) {
                Card card = cardRepository.findById(cardId)
                        .orElseThrow(() -> new RuntimeException("Card not found"));
                checkMembership(card.getBoardList().getBoard().getId(), username);

                List<CardCharacter> characters = cardCharacterRepository.findByCardId(cardId);
                return rgaService.buildOrderedChars(characters);
        }
}