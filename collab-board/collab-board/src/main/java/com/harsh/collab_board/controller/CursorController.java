package com.harsh.collab_board.controller;

import com.harsh.collab_board.dto.CursorBroadcastMessage;
import com.harsh.collab_board.dto.CursorUpdateRequest;
import com.harsh.collab_board.entity.User;
import com.harsh.collab_board.repository.UserRepository;
import com.harsh.collab_board.service.BoardMemberService;
import com.harsh.collab_board.service.CursorTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class CursorController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private BoardMemberService boardMemberService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CursorTrackingService cursorTrackingService;

    @MessageMapping("/cursor")
    public void handleCursorUpdate(@Payload CursorUpdateRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username == null) return;

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;
        if (!boardMemberService.isMember(request.getBoardId(), user.getId())) return;

        cursorTrackingService.recordCursor(
                headerAccessor.getSessionId(), request.getBoardId(), username,
                request.getFieldType(), request.getEntityId());

        CursorBroadcastMessage msg = new CursorBroadcastMessage();
        msg.setType("CURSOR_UPDATE");
        msg.setUsername(username);
        msg.setFieldType(request.getFieldType());
        msg.setEntityId(request.getEntityId());
        msg.setCharIndex(request.getCharIndex());
        msg.setAnchorCharId(request.getAnchorCharId());

        messagingTemplate.convertAndSend("/topic/board/" + request.getBoardId() + "/cursors", msg);
    }

    @MessageMapping("/cursor/clear")
    public void handleCursorClear(@Payload CursorUpdateRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        if (username == null) return;

        cursorTrackingService.clearCursor(headerAccessor.getSessionId());

        CursorBroadcastMessage msg = new CursorBroadcastMessage();
        msg.setType("CURSOR_CLEARED");
        msg.setUsername(username);
        msg.setFieldType(request.getFieldType());
        msg.setEntityId(request.getEntityId());

        messagingTemplate.convertAndSend("/topic/board/" + request.getBoardId() + "/cursors", msg);
    }
}