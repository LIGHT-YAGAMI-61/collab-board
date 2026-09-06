package com.harsh.collab_board.controller;

import com.harsh.collab_board.entity.BoardMember;
import com.harsh.collab_board.entity.JoinRequest;
import com.harsh.collab_board.entity.User;
import com.harsh.collab_board.repository.UserRepository;
import com.harsh.collab_board.service.BoardMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}")
public class BoardMemberController {

    @Autowired
    private BoardMemberService boardMemberService;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/join-request")
    public JoinRequest requestToJoin(@PathVariable Long boardId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        return boardMemberService.requestToJoin(boardId, user.getId());
    }

    @GetMapping("/join-requests")
    public List<JoinRequest> getPendingRequests(@PathVariable Long boardId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (!boardMemberService.isAdmin(boardId, user.getId())) {
            throw new RuntimeException("Only admin can view join requests");
        }
        return boardMemberService.getPendingRequests(boardId);
    }

    @PutMapping("/join-requests/{requestId}/accept")
    public void acceptRequest(@PathVariable Long boardId, @PathVariable Long requestId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (!boardMemberService.isAdmin(boardId, user.getId())) {
            throw new RuntimeException("Only admin can accept requests");
        }
        boardMemberService.acceptRequest(requestId);
    }

    @PutMapping("/join-requests/{requestId}/reject")
    public void rejectRequest(@PathVariable Long boardId, @PathVariable Long requestId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (!boardMemberService.isAdmin(boardId, user.getId())) {
            throw new RuntimeException("Only admin can reject requests");
        }
        boardMemberService.rejectRequest(requestId);
    }

    @GetMapping("/members")
    public List<BoardMember> getMembers(@PathVariable Long boardId) {
        return boardMemberService.getMembers(boardId);
    }
}