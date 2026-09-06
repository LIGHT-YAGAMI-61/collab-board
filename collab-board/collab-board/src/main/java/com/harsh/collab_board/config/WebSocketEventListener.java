package com.harsh.collab_board.config;

import com.harsh.collab_board.service.PresenceService;
import com.harsh.collab_board.service.CursorTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;


@Component
public class WebSocketEventListener {

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private CursorTrackingService cursorTrackingService ;

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
                String destination = accessor.getDestination();

                        if (destination != null && destination.startsWith("/topic/board/")) {
                            String boardIdStr = destination.substring("/topic/board/".length());
                            int slashIndex = boardIdStr.indexOf('/');
                            if (slashIndex != -1) {
                                boardIdStr = boardIdStr.substring(0, slashIndex);
                            }
                            Long boardId = Long.parseLong(boardIdStr);
                        String username = (String) accessor.getSessionAttributes().get("username");
                        if (username != null) {
                                presenceService.addPresence(accessor.getSessionId(), boardId, username);
                            }
                    }
            }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        presenceService.removePresence(event.getSessionId());
    }
}