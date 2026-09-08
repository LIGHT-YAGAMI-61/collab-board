package com.harsh.collab_board.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import com.harsh.collab_board.service.BoardMemberService;
import com.harsh.collab_board.repository.UserRepository;
import com.harsh.collab_board.entity.User;

import com.harsh.collab_board.security.JwtUtil;
import com.harsh.collab_board.service.PresenceService;
import org.springframework.context.annotation.Lazy;



@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

        @Autowired
        private JwtUtil jwtUtil ;

        @Autowired
        private BoardMemberService boardMemberService ;

        @Autowired
        private UserRepository userRepository ;

        @Autowired
        @Lazy
        private PresenceService presenceService ;

        @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
            registry.enableSimpleBroker("/topic") ;
            registry.setApplicationDestinationPrefixes("/app") ;
        }

        @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
            registry.addEndpoint("/ws")
                    .setAllowedOriginPatterns("http://localhost:5173", "http://localhost:8082", "https://collab-board-frontend.wittyhill-2d8ef2d7.centralindia.azurecontainerapps.io")
                    .withSockJS() ;
        }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if ( accessor != null &&  StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new org.springframework.messaging.MessagingException("Missing token");
                    }

                    String token = authHeader.substring(7);
                    String username = jwtUtil.extractUsername(token);

                    if (username == null || !jwtUtil.isTokenValid(token, username)) {
                        throw new org.springframework.messaging.MessagingException("Invalid token");
                    }

                    accessor.getSessionAttributes().put("username" , username) ;
                }
                else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination() ;

                    if ( destination != null && destination.startsWith("/topic/board")) {
                        String boardIdStr = destination.substring("/topic/board/".length()) ;
                        int slashIndex = boardIdStr.indexOf('/');
                        if (slashIndex != -1) {
                            boardIdStr = boardIdStr.substring(0, slashIndex);
                        }
                        Long boardId = Long.parseLong(boardIdStr) ;

                        String username = (String) accessor.getSessionAttributes().get("username") ;
                        if ( username == null ) {
                            throw new org.springframework.messaging.MessagingException("Not authenticated") ;
                        }

                        User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new org.springframework.messaging.MessagingException("User not found") ) ;

                        if ( !boardMemberService.isMember(boardId , user.getId())) {
                            throw new org.springframework.messaging.MessagingException("Not a member of this board") ;
                        }

                    }
                }
                return message;
            }
        });
    }
}
