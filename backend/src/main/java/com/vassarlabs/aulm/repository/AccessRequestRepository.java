package com.vassarlabs.aulm.repository;

import com.vassarlabs.aulm.model.AccessRequest;
import com.vassarlabs.aulm.model.AccessRequestStatus;
import com.vassarlabs.aulm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByStatusOrderByCreatedAtAsc(AccessRequestStatus status);
    List<AccessRequest> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndStatus(User user, AccessRequestStatus status);
}
