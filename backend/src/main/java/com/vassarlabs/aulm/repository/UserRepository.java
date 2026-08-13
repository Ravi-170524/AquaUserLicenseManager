package com.vassarlabs.aulm.repository;

import com.vassarlabs.aulm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndProjectName(String username, String projectName);
    Optional<User> findByUuid(UUID uuid);
    boolean existsByUsernameAndProjectName(String username, String projectName);
    long countByAdminTrue();
}
