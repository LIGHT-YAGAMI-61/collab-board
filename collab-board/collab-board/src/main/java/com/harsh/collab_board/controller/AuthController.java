package com.harsh.collab_board.controller;


import com.harsh.collab_board.dto.AuthResponse;
import com.harsh.collab_board.dto.LoginRequest;
import com.harsh.collab_board.dto.RegisterRequest;
import com.harsh.collab_board.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        @Autowired
    private AuthService authService ;

        @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
            return authService.register(request) ;
        }

        @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
            return authService.login(request) ;
        }
}
