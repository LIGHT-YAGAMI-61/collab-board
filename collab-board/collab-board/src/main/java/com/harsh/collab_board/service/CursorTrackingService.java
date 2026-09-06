package com.harsh.collab_board.service;

import com.harsh.collab_board.dto.CursorBroadcastMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CursorTrackingService {

    // sessionId -> the last field that session broadcast a cursor for
    private final Map<String, CursorLocation> sessionCursors = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void recordCursor(String sessionId, Long boardId, String username, String fieldType, Long entityId) {
        sessionCursors.put(sessionId, new CursorLocation(boardId, username, fieldType, entityId));
    }

    public void clearCursor(String sessionId) {
        sessionCursors.remove(sessionId);
    }

    // Called from WebSocketEventListener when a session disconnects.
    public void clearCursorOnDisconnect(String sessionId) {
        CursorLocation loc = sessionCursors.remove(sessionId);
        if (loc == null) return;

        CursorBroadcastMessage msg = new CursorBroadcastMessage();
        msg.setType("CURSOR_CLEARED");
        msg.setUsername(loc.username);
        msg.setFieldType(loc.fieldType);
        msg.setEntityId(loc.entityId);

        messagingTemplate.convertAndSend("/topic/board/" + loc.boardId + "/cursors", msg);
    }

    private static class CursorLocation {
        final Long boardId;
        final String username;
        final String fieldType;
        final Long entityId;

        CursorLocation(Long boardId, String username, String fieldType, Long entityId) {
            this.boardId = boardId;
            this.username = username;
            this.fieldType = fieldType;
            this.entityId = entityId;
        }
    }
}