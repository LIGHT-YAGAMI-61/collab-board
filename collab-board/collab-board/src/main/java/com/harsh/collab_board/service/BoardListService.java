package com.harsh.collab_board.service;

import com.harsh.collab_board.entity.Board;
import com.harsh.collab_board.entity.BoardList;
import com.harsh.collab_board.repository.BoardListRepository;
import com.harsh.collab_board.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.harsh.collab_board.entity.User;
import com.harsh.collab_board.repository.UserRepository;
import com.harsh.collab_board.dto.BoardUpdateMessage;


import java.util.List;

@Service
public class BoardListService {

    @Autowired
    private BoardListRepository boardListRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository ;

    @Autowired
    private BoardMemberService boardMemberService ;

    private void broadcastBoardUpdate(Long boardId, BoardUpdateMessage message) {
        messagingTemplate.convertAndSend("/topic/board/" + boardId, message);
    }

    public BoardList createList(Long boardId, BoardList boardList , String username) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + boardId));

        User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found") ) ;

        if ( !boardMemberService.isMember(boardId , user.getId())) {
            throw new RuntimeException("You are not a member of this board") ;
        }

        boardList.setBoard(board);
        BoardList saved = boardListRepository.save(boardList);

        BoardUpdateMessage message = new BoardUpdateMessage();
        message.setType("LIST_CREATED");
        message.setList(saved);
        broadcastBoardUpdate(boardId, message);

        return saved;
    }

    public List<BoardList> getListsByBoard(Long boardId , String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")) ;

        if ( !boardMemberService.isMember(boardId , user.getId())) {
            throw new RuntimeException("You are not a member of this board") ;
        }
        return boardListRepository.findByBoardIdOrderByPositionAsc(boardId);
    }

    public BoardList updateList(Long id, BoardList updatedList , String username) {
        BoardList existing = boardListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("List not found with id: " + id));

        User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found") ) ;

        if ( !boardMemberService.isMember(existing.getBoard().getId() , user.getId())) {
            throw new RuntimeException("You are not a memeber of this board") ;
        }

        existing.setTitle(updatedList.getTitle());
        existing.setPosition(updatedList.getPosition());
        BoardList saved = boardListRepository.save(existing);

        BoardUpdateMessage message = new BoardUpdateMessage();
        message.setType("LIST_UPDATED");
        message.setList(saved);
        broadcastBoardUpdate(existing.getBoard().getId(), message);

        return saved;
    }

    public void deleteList(Long id , String username) {
        BoardList list = boardListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("List not found with id: " + id));

        Long boardId = list.getBoard().getId();

        User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found")) ;

        if ( !boardMemberService.isMember(boardId , user.getId())) {
            throw new RuntimeException("You are not a member of this board") ;
        }

        boardListRepository.deleteById(id);
        BoardUpdateMessage message = new BoardUpdateMessage() ;
        message.setType("LIST_DELETED");
        message.setDeletedListId(id);
        broadcastBoardUpdate(boardId , message) ;
    }
}