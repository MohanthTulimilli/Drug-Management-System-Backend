package com.dms.service;

import com.dms.dto.*;
import com.dms.entity.Role;
import com.dms.entity.User;
import com.dms.repository.UserRepository;
import com.dms.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest req) {
        String email = (req.email() == null) ? "" : req.email().trim();
        if (email.isEmpty())
            throw new RuntimeException("Invalid credentials");
        // Try exact match first, then case-insensitive (works with any DB collation)
        User user = userRepository.findByEmail(email)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        String rawPassword = (req.password() == null) ? "" : req.password().trim();
        if (rawPassword.isEmpty()) throw new RuntimeException("Invalid credentials");
        String storedHash = user.getPassword();
        if (storedHash == null || !passwordEncoder.matches(rawPassword, storedHash))
            throw new RuntimeException("Invalid credentials");
        if (!user.isEnabled()) throw new RuntimeException("Account disabled");
        return toAuthResponse(jwtService.generateToken(user), user);
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username()))
            throw new RuntimeException("Username already exists");
        if (userRepository.existsByEmail(req.email()))
            throw new RuntimeException("Email already exists");
        Role role = Role.RETAILER;
        if (req.role() != null) {
            try { role = Role.valueOf(req.role()); } catch (Exception ignored) {}
        }
        User user = User.builder()
                .username(req.username()).email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .firstName(req.firstName()).lastName(req.lastName())
                .phone(req.phone()).role(role).enabled(true).build();
        user = userRepository.save(user);
        return toAuthResponse(jwtService.generateToken(user), user);
    }

    /** ADMIN only: create RETAILER account. No self-registration. */
    public UserDto registerRetailer(RegisterRetailerRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new RuntimeException("Email already exists");
        String username = req.email(); // use email as username for uniqueness
        if (userRepository.existsByUsername(username))
            throw new RuntimeException("User with this email already exists");
        User user = User.builder()
                .username(username)
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .firstName(req.name())
                .lastName("")
                .phone(null)
                .role(Role.RETAILER)
                .enabled(true)
                .build();
        user = userRepository.save(user);
        return toUserDto(user);
    }

    /** ADMIN only: create DELIVERY account. No self-registration. */
    public UserDto registerDelivery(RegisterDeliveryRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new RuntimeException("Email already exists");
        String username = req.email();
        if (userRepository.existsByUsername(username))
            throw new RuntimeException("User with this email already exists");
        User user = User.builder()
                .username(username)
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .firstName(req.name())
                .lastName("")
                .phone(null)
                .role(Role.DELIVERY)
                .enabled(true)
                .build();
        user = userRepository.save(user);
        return toUserDto(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUserDto).toList();
    }

    public List<UserDto> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream().map(this::toUserDto).toList();
    }

    public UserDto getUserById(Long id) {
        return toUserDto(userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found")));
    }

    public UserDto updateUser(Long id, RegisterRequest req) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (req.firstName() != null) user.setFirstName(req.firstName());
        if (req.lastName() != null) user.setLastName(req.lastName());
        if (req.email() != null) user.setEmail(req.email());
        if (req.phone() != null) user.setPhone(req.phone());
        if (req.role() != null) { try { user.setRole(Role.valueOf(req.role())); } catch (Exception ignored) {} }
        if (req.password() != null && !req.password().isEmpty())
            user.setPassword(passwordEncoder.encode(req.password()));
        return toUserDto(userRepository.save(user));
    }

    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    public long countUsers() { return userRepository.count(); }
    public long countByRole(Role role) { return userRepository.countByRole(role); }

    private AuthResponse toAuthResponse(String token, User u) {
        String roleName = (u.getRole() != null) ? u.getRole().name() : "USER";
        return new AuthResponse(token, u.getId(), u.getUsername(), u.getEmail(),
                u.getFirstName(), u.getLastName(), roleName);
    }

    private UserDto toUserDto(User u) {
        return new UserDto(u.getId(), u.getUsername(), u.getEmail(), u.getFirstName(),
                u.getLastName(), u.getPhone(), u.getRole().name(), u.isEnabled(), u.getCreatedAt());
    }
}
