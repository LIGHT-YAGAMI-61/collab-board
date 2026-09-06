package com.harsh.collab_board.service;

import com.harsh.collab_board.entity.Board;
import com.harsh.collab_board.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.harsh.collab_board.entity.User;
import com.harsh.collab_board.repository.UserRepository;
import com.harsh.collab_board.dto.BoardUpdateMessage;

import java.security.BasicPermission;
import java.util.List;

@Service
public class BoardService {

    @Autowired
    private BoardRepository boardRepository ;

    @Autowired
    private UserRepository userRepository ;

    @Autowired
    private BoardMemberService boardMemberService ;

    @Autowired
    private SimpMessagingTemplate messagingTemplate ;

    private void broadcastBoardUpdate(Long boardId, BoardUpdateMessage message) {
                messagingTemplate.convertAndSend("/topic/board/" + boardId, message);
            }

    public Board createBoard(Board board , String username ) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")) ;

        Board savedBoard = boardRepository.save(board) ;
        boardMemberService.addMember(savedBoard , creator , "ADMIN");
        return savedBoard ;
    }

    public List<Board> getAllBoards(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")) ;
        return boardMemberService.getBoardsForUser(user.getId());
    }

    public Board getBoardById(Long id , String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")) ;

        if ( !boardMemberService.isMember(id , user.getId())) {
            throw new RuntimeException("You are not a member of this board") ;
        }

        return boardRepository.findById(id).orElseThrow(() -> new RuntimeException("Board not found with id :"  + id )) ;
    }

    public void deleteBoard(Long id , String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found")) ;

        if ( !boardMemberService.isAdmin(id , user.getId())) {
            throw new RuntimeException("Only admin can delete this board") ;
        }

        boardRepository.deleteById(id);
    }

    public Board updateBoard(Long id, Board updatedBoard, String username) {
                Board existing = boardRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Board not found with id: " + id));

                        User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                        if ( !boardMemberService.isMember(id, user.getId())) {
                        throw new RuntimeException("You are not a member of this board");
                    }

                        existing.setTitle(updatedBoard.getTitle());
                Board saved = boardRepository.save(existing);

                BoardUpdateMessage message = new BoardUpdateMessage();
                message.setType("BOARD_UPDATED");
                message.setBoard(saved);
                broadcastBoardUpdate(id, message);

                return saved;
            }
}
