package com.harsh.collab_board.service;

import com.harsh.collab_board.entity.*;
import com.harsh.collab_board.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.harsh.collab_board.exception.ResourceNotFoundException;
import com.harsh.collab_board.exception.ConflictException;

import java.util.List;

@Service
public class BoardMemberService {

    @Autowired
    private BoardMemberRepository boardMemberRepository;

    @Autowired
    private JoinRequestRepository joinRequestRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    public boolean isMember(Long boardId, Long userId) {
        return boardMemberRepository.existsByBoardIdAndUserId(boardId, userId);
    }

    public boolean isAdmin(Long boardId, Long userId) {
        return boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .map(m -> m.getRole().equals("ADMIN"))
                .orElse(false);
    }

    public void addMember(Board board, User user, String role) {
        BoardMember member = new BoardMember();
        member.setBoard(board);
        member.setUser(user);
        member.setRole(role);
        boardMemberRepository.save(member);
    }

    public JoinRequest requestToJoin(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found"));

        if (isMember(boardId, userId)) {
            throw new ConflictException("Already a member of this board");
        }

        joinRequestRepository.findByBoardIdAndUserIdAndStatus(boardId, userId, "PENDING")
                .ifPresent(r -> { throw new ConflictException("Join request already pending"); });

        JoinRequest request = new JoinRequest();
        request.setBoard(board);
        request.setUser(user);
        request.setStatus("PENDING");
        return joinRequestRepository.save(request);
    }

    public List<JoinRequest> getPendingRequests(Long boardId) {
        return joinRequestRepository.findByBoardIdAndStatus(boardId, "PENDING");
    }

    public void acceptRequest(Long requestId) {
        JoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        request.setStatus("ACCEPTED");
        joinRequestRepository.save(request);

        addMember(request.getBoard(), request.getUser(), "MEMBER");
    }

    public void rejectRequest(Long requestId) {
        JoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        request.setStatus("REJECTED");
        joinRequestRepository.save(request);
    }

    public List<BoardMember> getMembers(Long boardId) {
        return boardMemberRepository.findByBoardId(boardId);
    }

    public List<Board> getBoardsForUser(Long userId) {
        return boardMemberRepository.findByUserId(userId).stream()
                .map(BoardMember::getBoard)
                .toList() ;
    }
}