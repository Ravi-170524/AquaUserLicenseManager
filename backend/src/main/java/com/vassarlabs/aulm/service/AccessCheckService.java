package com.vassarlabs.aulm.service;

import com.vassarlabs.aulm.model.License;
import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.repository.UserRepository;
import com.vassarlabs.aulm.util.InputNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AccessCheckService {

    private static final Logger log = LoggerFactory.getLogger(AccessCheckService.class);

    private final UserRepository userRepository;

    public AccessCheckService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean hasAccess(String userName, String projectName, PermissionType permissionType) {
        String normalizedUser = InputNormalizer.lowerTrim(userName);
        String normalizedProject = InputNormalizer.lowerTrim(projectName);
        log.info("hasAccess username={} project={} permission={}", normalizedUser, normalizedProject, permissionType);
        return userRepository.findByUsernameAndProjectName(normalizedUser, normalizedProject)
                .filter(User::isEnabled)
                .filter(user -> user.getPermissions() != null && user.getPermissions().contains(permissionType))
                .map(this::isLicenseAllowed)
                .orElse(false);
    }

    private boolean isLicenseAllowed(User user) {
        License license = user.getLicense();
        if (license == null) {
            return false;
        }

        if (license.getLicenseType() == LicenseType.ADMIN) {
            return true;
        }

        LocalDate expiryDate = license.getExpiryDate();
        return expiryDate == null || !LocalDate.now().isAfter(expiryDate);
    }
}
