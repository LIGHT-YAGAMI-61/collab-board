package com.harsh.collab_board.dto;


import com.harsh.collab_board.entity.BoardList;
import com.harsh.collab_board.entity.Card;
import com.harsh.collab_board.entity.Board;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BoardUpdateMessage {

        private String type ;  // CARD_CREATED, CARD_UPDATED, CARD_DELETED, CARD_MOVED, LIST_CREATED, LIST_UPDATED, LIST_DELETED , BOARD_UPDATED
        private Card card ;  // present for CARD_CREATED, CARD_UPDATED, CARD_MOVED
        private Long listId ;   // present for CARD_CREATED (which list the card belongs to) and CARD_DELETED (which list the card was in)
        private BoardList list ;  // present for LIST_CREATED, LIST_UPDATED
        private Board board ; // present for BOARD_UPDATED
        private Long cardId ;  // present for CARD_DELETED
        private Long deletedListId ;  // present for LIST_DELETED
        private String deletedBy ; // present for CARD_DELETED - username of the user who deleted it
        private java.util.List<String> activeUsers ; // present for PRESENCE_UPDATE - usernames currently viewing this board
}
