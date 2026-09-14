package com.harsh.collab_board.service;


import com.harsh.collab_board.dto.AuthResponse;
import com.harsh.collab_board.dto.LoginRequest;
import com.harsh.collab_board.dto.RegisterRequest;
import com.harsh.collab_board.entity.User;
import com.harsh.collab_board.repository.UserRepository;
import com.harsh.collab_board.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.harsh.collab_board.exception.ConflictException;
import com.harsh.collab_board.exception.BadRequestException;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository ;

    @Autowired
    private PasswordEncoder passwordEncoder ;

    @Autowired
    private JwtUtil jwtUtil ;

    public AuthResponse register(RegisterRequest request)  {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already Taken " ) ;
        }
        if ( userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered ") ;
        }

        User user = new User() ;
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user) ;

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token , user.getUsername()) ;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password")) ;

        if ( !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid username or password ") ;
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token , user.getUsername()) ;
    }
}
