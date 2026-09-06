package com.harsh.collab_board.service;

import com.harsh.collab_board.dto.BoardUpdateMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // sessionId -> which board + which user that session belongs to (so we can clean up on disconnect)
    private final Map<String, SessionInfo> sessionInfoBySessionId = new ConcurrentHashMap<>();

    // boardId -> (username -> set of sessionIds currently open for that user on that board)
    private final Map<Long, Map<String, Set<String>>> sessionsByBoardAndUser = new ConcurrentHashMap<>();

    private static class SessionInfo {
        Long boardId;
        String username;
        SessionInfo(Long boardId, String username) {
            this.boardId = boardId;
            this.username = username;
        }
    }

    public void addPresence(String sessionId, Long boardId, String username) {
        sessionInfoBySessionId.put(sessionId, new SessionInfo(boardId, username));

        sessionsByBoardAndUser
                .computeIfAbsent(boardId, id -> new ConcurrentHashMap<>())
                .computeIfAbsent(username, u -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

        broadcastPresence(boardId);
    }

    public void removePresence(String sessionId) {
        SessionInfo info = sessionInfoBySessionId.remove(sessionId);
        if (info == null) return;

        Map<String, Set<String>> usersOnBoard = sessionsByBoardAndUser.get(info.boardId);
        if (usersOnBoard != null) {
            Set<String> sessions = usersOnBoard.get(info.username);
            if (sessions != null) {
                sessions.remove(sessionId);
                // Only drop the user once ALL their sessions for this board are gone —
                // a user can briefly have 2 sessions during a reconnect, and the old
                // one disconnecting should NOT remove them if the new one is still open.
                if (sessions.isEmpty()) {
                    usersOnBoard.remove(info.username);
                }
            }
            if (usersOnBoard.isEmpty()) {
                sessionsByBoardAndUser.remove(info.boardId);
            }
        }

        broadcastPresence(info.boardId);
    }

    private void broadcastPresence(Long boardId) {
        Map<String, Set<String>> usersOnBoard = sessionsByBoardAndUser.getOrDefault(boardId, Map.of());
        BoardUpdateMessage message = new BoardUpdateMessage();
        message.setType("PRESENCE_UPDATE");
        message.setActiveUsers(List.copyOf(usersOnBoard.keySet()));
        messagingTemplate.convertAndSend("/topic/board/" + boardId, message);
    }
}