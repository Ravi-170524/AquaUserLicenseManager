package com.vassarlabs.aulm.config;

import com.vassarlabs.aulm.model.License;
import com.vassarlabs.aulm.model.LicenseStatus;
import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;
import com.vassarlabs.aulm.model.Project;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.repository.ProjectRepository;
import com.vassarlabs.aulm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Ensures a first admin account exists so the desktop UI is usable on a fresh database.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final String DEFAULT_ADMIN_PROJECT = "AULM";
    private static final String DEFAULT_ADMIN_GMAIL = "bipin.kumar@vassarlabs.com";

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, ProjectRepository projectRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!projectRepository.existsByName(DEFAULT_ADMIN_PROJECT)) {
            Project project = new Project();
            project.setName(DEFAULT_ADMIN_PROJECT);
            projectRepository.save(project);
        }

        if (userRepository.existsByUsernameAndProjectName(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PROJECT)) {
            return;
        }

        User admin = new User();
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setPasswordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setEmail(DEFAULT_ADMIN_GMAIL);
        admin.setFullName("Administrator");
        admin.setProjectName(DEFAULT_ADMIN_PROJECT);
        admin.setAdmin(true);
        admin.setSuperAdmin(true);
        admin.setPermissions(EnumSet.of(PermissionType.ACCESS, PermissionType.MODIFY, PermissionType.APPROVE));

        License license = new License();
        license.setLicenseKey(UUID.randomUUID().toString());
        license.setLicenseType(LicenseType.ADMIN);
        license.setStatus(LicenseStatus.ACTIVE);
        license.setIssuedDate(LocalDate.now());
        license.setExpiryDate(LicenseType.ADMIN.computeExpiryDate(LocalDate.now()));
        admin.setLicense(license);

        userRepository.save(admin);
        log.warn("Created default admin account (username='{}', project='{}', password='{}'). Log in and change this password immediately.",
                DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PROJECT, DEFAULT_ADMIN_PASSWORD);
    }
}
