package com.harsh.collab_board.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

        @Autowired
    private JwtUtil jwtUtil ;

        @Override
    protected void doFilterInternal(HttpServletRequest request , HttpServletResponse response , FilterChain filterChain )
            throws ServletException , IOException {
            String authHeader = request.getHeader("Authorization") ;

            if ( authHeader != null && authHeader.startsWith("Bearer")) {
                String token = authHeader.substring(7) ;

                try {
                    String username = jwtUtil.extractUsername(token) ;

                    if ( username != null && jwtUtil.isTokenValid(token , username) ) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(username , null , Collections.emptyList()) ;

                        SecurityContextHolder.getContext().setAuthentication(authToken) ;
                    }
                } catch (Exception e) {
                    // Invalid token leave request authenticated , let spring security handle it
                }
            }

            filterChain.doFilter(request , response) ;

        }
}
