package com.harsh.collab_board.service;

import com.harsh.collab_board.entity.Board;
import com.harsh.collab_board.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.harsh.collab_board.entity.User;
import com.harsh.collab_board.repository.UserRepository;
import com.harsh.collab_board.dto.BoardUpdateMessage;
import com.harsh.collab_board.exception.ResourceNotFoundException;
import com.harsh.collab_board.exception.ForbiddenException;

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
                .orElseThrow(() -> new ResourceNotFoundException("User not found")) ;

        Board savedBoard = boardRepository.save(board) ;
        boardMemberService.addMember(savedBoard , creator , "ADMIN");
        return savedBoard ;
    }

    public List<Board> getAllBoards(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")) ;
        return boardMemberService.getBoardsForUser(user.getId());
    }

    public Board getBoardById(Long id , String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")) ;

        Board board = boardRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + id)) ;
        
                    if ( !boardMemberService.isMember(id , user.getId())) {
                    throw new ForbiddenException("You are not a member of this board") ;
                }

                    return board ;
    }

    public void deleteBoard(Long id , String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")) ;

        if ( !boardMemberService.isAdmin(id , user.getId())) {
            throw new ForbiddenException("Only admin can delete this board") ;
        }

        boardRepository.deleteById(id);
    }

    public Board updateBoard(Long id, Board updatedBoard, String username) {
                Board existing = boardRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + id));

                        User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                        if ( !boardMemberService.isMember(id, user.getId())) {
                        throw new ForbiddenException("You are not a member of this board");
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
