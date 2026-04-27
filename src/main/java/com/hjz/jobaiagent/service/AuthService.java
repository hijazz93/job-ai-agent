package com.hjz.jobaiagent.service;

import com.hjz.jobaiagent.dto.AuthResponse;
import com.hjz.jobaiagent.dto.LoginRequest;
import com.hjz.jobaiagent.dto.RegisterRequest;
import com.hjz.jobaiagent.entity.Role;
import com.hjz.jobaiagent.entity.User;
import com.hjz.jobaiagent.repository.UserRepository;
import com.hjz.jobaiagent.security.CustomUserDetails;
import com.hjz.jobaiagent.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已被注册");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.NORMAL_USER);
        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtTokenProvider.generateToken(userDetails);

        return new AuthResponse(token, "Bearer", 3600000L,
                new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name()));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtTokenProvider.generateToken(userDetails);

        return new AuthResponse(token, "Bearer", 3600000L,
                new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name()));
    }
}
