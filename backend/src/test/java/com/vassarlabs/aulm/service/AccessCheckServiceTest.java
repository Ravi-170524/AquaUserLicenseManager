package com.vassarlabs.aulm.service;

import com.vassarlabs.aulm.model.License;
import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.PermissionType;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccessCheckServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final AccessCheckService accessCheckService = new AccessCheckService(userRepository);

    @Test
    void returnsTrueForAdminLicense() {
        User user = userWithLicense(LicenseType.ADMIN, LocalDate.now().minusYears(10), EnumSet.of(PermissionType.ACCESS));
        Mockito.when(userRepository.findByUsernameAndProjectName("alice", "proj")).thenReturn(Optional.of(user));

        assertThat(accessCheckService.checkAccess("alice", "proj", PermissionType.ACCESS)).isTrue();
    }

    @Test
    void returnsTrueWhenExpiryDateHasNotPassed() {
        User user = userWithLicense(LicenseType.STANDARD, LocalDate.now().plusDays(1), EnumSet.of(PermissionType.ACCESS));
        Mockito.when(userRepository.findByUsernameAndProjectName("alice", "proj")).thenReturn(Optional.of(user));

        assertThat(accessCheckService.checkAccess("alice", "proj", PermissionType.ACCESS)).isTrue();
    }

    @Test
    void returnsFalseWhenExpiryDateHasPassed() {
        User user = userWithLicense(LicenseType.STANDARD, LocalDate.now().minusDays(1), EnumSet.of(PermissionType.ACCESS));
        Mockito.when(userRepository.findByUsernameAndProjectName("alice", "proj")).thenReturn(Optional.of(user));

        assertThat(accessCheckService.checkAccess("alice", "proj", PermissionType.ACCESS)).isFalse();
    }

    @Test
    void returnsFalseWhenUserLacksPermission() {
        User user = userWithLicense(LicenseType.ADMIN, LocalDate.now().plusYears(1), EnumSet.of(PermissionType.MODIFY));
        Mockito.when(userRepository.findByUsernameAndProjectName("alice", "proj")).thenReturn(Optional.of(user));

        assertThat(accessCheckService.checkAccess("alice", "proj", PermissionType.ACCESS)).isFalse();
    }

    private User userWithLicense(LicenseType licenseType, LocalDate expiryDate, EnumSet<PermissionType> permissions) {
        User user = new User();
        user.setUsername("alice");
        user.setProjectName("proj");
        user.setEnabled(true);
        user.setPermissions(permissions);
        License license = new License();
        license.setLicenseType(licenseType);
        license.setExpiryDate(expiryDate);
        user.setLicense(license);
        return user;
    }
}
