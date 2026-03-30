package com.mfra.website.config;

import com.mfra.website.module.auth.entity.AdminUserEntity;
import com.mfra.website.module.auth.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeedRunner implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-email}")
    private String defaultEmail;

    @Value("${app.admin.default-password}")
    private String defaultPassword;

    @Value("${app.admin.default-name}")
    private String defaultName;

    @Override
    public void run(ApplicationArguments args) {
        if (!adminUserRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(defaultEmail)) {
            AdminUserEntity admin = new AdminUserEntity();
            admin.setFullName(defaultName);
            admin.setEmail(defaultEmail);
            admin.setPassword(passwordEncoder.encode(defaultPassword));
            admin.setRole("ADMIN");
            admin.setIsActive(true);
            adminUserRepository.save(admin);
            log.info("Default admin user created: {}", defaultEmail);
        } else {
            log.info("Default admin user already exists: {}", defaultEmail);
        }
    }
}
