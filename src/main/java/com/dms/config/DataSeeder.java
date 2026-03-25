package com.dms.config;

import com.dms.entity.*;
import com.dms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MedicineRepository medicineRepo;
    private final BatchRepository batchRepo;

    @Override
    public void run(String... args) {
        seedUsers();
    }

    private void seedUsers() {
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPER_ADMIN)
                .findFirst()
                .ifPresentOrElse(
                        u -> log.info("SUPER_ADMIN already exists with email={}", u.getEmail()),
                        () -> {
                            log.info("Seeding initial SUPER_ADMIN user...");
                            seedUser(
                                    "superadmin",
                                    "superadmin@system.com",
                                    "ChangeMeNow!123",
                                    "Super",
                                    "Admin",
                                    Role.SUPER_ADMIN
                            );
                            log.info("Initial SUPER_ADMIN user seeded. Please change the password after first login.");
                        }
                );
    }

    private void seedUser(String username, String email, String pw, String fn, String ln, Role role) {
        userRepository.save(User.builder().username(username).email(email)
                .password(passwordEncoder.encode(pw)).firstName(fn).lastName(ln)
                .role(role).enabled(true).build());
    }

}
