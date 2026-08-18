package com.vassarlabs.aulm.repository;

import com.vassarlabs.aulm.model.LicenseType;
import com.vassarlabs.aulm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndProjectName(String username, String projectName);
    Optional<User> findByUuid(UUID uuid);
    boolean existsByUsernameAndProjectName(String username, String projectName);
    long countByAdminTrue();
    List<User> findByAdminTrueAndProjectNameOrderByUsername(String projectName);
    List<User> findByProjectNameOrderByUsername(String projectName);
    List<User> findByLicense_LicenseTypeAndProjectNameInOrderByUsername(LicenseType licenseType, Collection<String> projectNames);
}
