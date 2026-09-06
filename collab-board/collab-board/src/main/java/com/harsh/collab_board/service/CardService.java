package com.harsh.collab_board.service;

import com.harsh.collab_board.entity.BoardList;
import com.harsh.collab_board.entity.Card;
import com.harsh.collab_board.entity.User;
import com.harsh.collab_board.repository.BoardListRepository;
import com.harsh.collab_board.repository.CardCharacterRepository;
import com.harsh.collab_board.repository.CardRepository;
import com.harsh.collab_board.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.harsh.collab_board.dto.BoardUpdateMessage;

import java.util.List;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private BoardListRepository boardListRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository ;

    @Autowired
    private BoardMemberService boardMemberService ;

    @Autowired
    private CardCharacterRepository cardCharacterRepository;

    private void broadcastBoardUpdate(Long boardId , BoardUpdateMessage message)  {
        messagingTemplate.convertAndSend("/topic/board/" + boardId,  message );
    }

    private void checkMembership(Long boardId , String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")) ;
        if ( !boardMemberService.isMember(boardId , user.getId())) {
            throw new RuntimeException("You are not a member of this board") ;
        }
    }

    public Card createCard(Long boardListId, Card card , String username) {
        BoardList boardList = boardListRepository.findById(boardListId)
                .orElseThrow(() -> new RuntimeException("List not found with id: " + boardListId));

        checkMembership(boardList.getBoard().getId() , username);

        card.setBoardList(boardList);
        Card saved = cardRepository.save(card);
        BoardUpdateMessage message = new BoardUpdateMessage() ;
        message.setType("CARD_CREATED");
        message.setCard(saved);
        message.setListId(boardListId);
        broadcastBoardUpdate(boardList.getBoard().getId() , message);

        return saved;
    }

    public List<Card> getCardsByList(Long boardListId , String username) {
        BoardList boardList = boardListRepository.findById(boardListId)
                .orElseThrow(() -> new RuntimeException("List not found with id :" + boardListId)) ;

        checkMembership(boardList.getBoard().getId(), username);

        return cardRepository.findByBoardListIdOrderByPositionAsc(boardListId);
    }

    public Card updateCard(Long id, Card updatedCard , String username) {
        Card existing = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + id));

        checkMembership(existing.getBoardList().getBoard().getId() , username);

        existing.setTitle(updatedCard.getTitle());
        existing.setDescription(updatedCard.getDescription());
        existing.setPosition(updatedCard.getPosition());
        Card saved = cardRepository.save(existing);

        BoardUpdateMessage message = new BoardUpdateMessage() ;
        message.setType("CARD_UPDATED");
        message.setCard(saved);
        broadcastBoardUpdate(existing.getBoardList().getBoard().getId() , message);

        return saved;
    }

    @Transactional
    public void deleteCard(Long id , String username) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + id));
        Long boardId = card.getBoardList().getBoard().getId();
        Long listId = card.getBoardList().getId() ;

        User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found")) ;

        if ( !boardMemberService.isMember(boardId , user.getId())) {
            throw new RuntimeException("You are not a member of this board") ;
        }

        if (!boardMemberService.isAdmin(boardId , user.getId())) {
            throw new RuntimeException("Only admins can delete cards") ;
        }

        cardCharacterRepository.deleteByCardId(id) ;

        cardRepository.deleteById(id) ;


      BoardUpdateMessage message = new BoardUpdateMessage() ;
      message.setType("CARD_DELETED") ;
      message.setCardId(listId) ;
      message.setListId(listId) ;
      message.setDeletedBy(username);
      broadcastBoardUpdate(boardId , message) ;

    }

    public Card moveCard(Long cardId, Long targetListId, Integer targetPosition , String username) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardId));

    checkMembership(card.getBoardList().getBoard().getId() , username);

        BoardList targetList = boardListRepository.findById(targetListId)
                .orElseThrow(() -> new RuntimeException("List not found with id: " + targetListId));

        Long sourceListId = card.getBoardList().getId();
        Integer sourcePosition = card.getPosition();

        if (sourceListId.equals(targetListId)) {
            List<Card> cardsInList = cardRepository.findByBoardListIdOrderByPositionAsc(targetListId);

            if (targetPosition > sourcePosition) {
                for (Card c : cardsInList) {
                    if (c.getId().equals(cardId)) continue;
                    if (c.getPosition() > sourcePosition && c.getPosition() <= targetPosition) {
                        c.setPosition(c.getPosition() - 1);
                        cardRepository.save(c);
                    }
                }
            } else {
                for (Card c : cardsInList) {
                    if (c.getId().equals(cardId)) continue;
                    if (c.getPosition() >= targetPosition && c.getPosition() < sourcePosition) {
                        c.setPosition(c.getPosition() + 1);
                        cardRepository.save(c);
                    }
                }
            }
        } else {
            List<Card> sourceCards = cardRepository.findByBoardListIdOrderByPositionAsc(sourceListId);
            for (Card c : sourceCards) {
                if (c.getPosition() > sourcePosition) {
                    c.setPosition(c.getPosition() - 1);
                    cardRepository.save(c);
                }
            }

            List<Card> targetCards = cardRepository.findByBoardListIdOrderByPositionAsc(targetListId);
            for (Card c : targetCards) {
                if (c.getPosition() >= targetPosition) {
                    c.setPosition(c.getPosition() + 1);
                    cardRepository.save(c);
                }
            }
        }

        card.setBoardList(targetList);
        card.setPosition(targetPosition);
        Card saved = cardRepository.save(card);

        BoardUpdateMessage message = new BoardUpdateMessage() ;
        message.setType("CARD_MOVED");
        message.setCard(saved);
        broadcastBoardUpdate(targetList.getBoard().getId() , message);
        return saved;
    }
}