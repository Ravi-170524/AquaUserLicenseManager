package com.vassarlabs.aulm.repository;

import com.vassarlabs.aulm.model.AccessRequest;
import com.vassarlabs.aulm.model.AccessRequestStatus;
import com.vassarlabs.aulm.model.AccessRequestType;
import com.vassarlabs.aulm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByStatusOrderByCreatedAtAsc(AccessRequestStatus status);
    List<AccessRequest> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndStatus(User user, AccessRequestStatus status);

    /** Requests sent to "any admin", plus requests sent specifically to this admin. */
    @Query("select r from AccessRequest r where r.status = :status "
            + "and (r.assignedAdmin is null or r.assignedAdmin = :admin) order by r.createdAt asc")
    List<AccessRequest> findVisibleTo(@Param("status") AccessRequestStatus status, @Param("admin") User admin);

    /** Users whose signup hasn't been approved (or rejected) yet shouldn't show up as regular users. */
    @Query("select r.user.id from AccessRequest r where r.status = :status and r.requestType = :type")
    List<Long> findUserIdsByStatusAndRequestType(@Param("status") AccessRequestStatus status, @Param("type") AccessRequestType type);
}
