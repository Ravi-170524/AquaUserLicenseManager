package com.vassarlabs.aulm.controller;

import com.vassarlabs.aulm.dto.AccessRequestResponse;
import com.vassarlabs.aulm.dto.ApproveAccessRequestRequest;
import com.vassarlabs.aulm.dto.CreateAccessRequestRequest;
import com.vassarlabs.aulm.dto.RejectAccessRequestRequest;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.service.AccessRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access-requests")
public class AccessRequestController {

    private static final Logger log = LoggerFactory.getLogger(AccessRequestController.class);

    private final AccessRequestService accessRequestService;

    public AccessRequestController(AccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    /** Submits a renewal or permission-change request for the caller (registration goes through /api/auth/register instead). */
    @PostMapping("/createMyRequest")
    public AccessRequestResponse createMyRequest(@AuthenticationPrincipal User user,
                                                   @Valid @RequestBody CreateAccessRequestRequest request) {
        log.info("POST /api/access-requests/createMyRequest username={} project={} type={}", user.getUsername(), user.getProjectName(), request.requestType());
        return AccessRequestResponse.from(accessRequestService.createMyRequest(user, request));
    }

    /** Lists the caller's own past and pending requests. */
    @GetMapping("/getMyRequests")
    public List<AccessRequestResponse> getMyRequests(@AuthenticationPrincipal User user) {
        log.info("GET /api/access-requests/getMyRequests username={} project={}", user.getUsername(), user.getProjectName());
        return accessRequestService.getMyRequests(user).stream().map(AccessRequestResponse::from).toList();
    }

    /** Lists pending requests this admin can act on (sent to them specifically, or to "any admin" in their project). */
    @GetMapping("/getPendingRequests")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccessRequestResponse> getPendingRequests(@AuthenticationPrincipal User admin) {
        log.info("GET /api/access-requests/getPendingRequests username={} project={}", admin.getUsername(), admin.getProjectName());
        return accessRequestService.getPendingRequests(admin).stream().map(AccessRequestResponse::from).toList();
    }

    /** Approves a request: grants the requested (or admin-chosen) license and permissions. */
    @PostMapping("/{id}/approveRequest")
    @PreAuthorize("hasRole('ADMIN')")
    public AccessRequestResponse approveRequest(@PathVariable("id") Long id, @Valid @RequestBody ApproveAccessRequestRequest request,
                                          @AuthenticationPrincipal User admin) {
        log.info("POST /api/access-requests/{}/approveRequest username={} project={}", id, admin.getUsername(), admin.getProjectName());
        return AccessRequestResponse.from(accessRequestService.approveRequest(id, request, admin));
    }

    /** Rejects a request with a reason; nothing is granted. */
    @PostMapping("/{id}/rejectRequest")
    @PreAuthorize("hasRole('ADMIN')")
    public AccessRequestResponse rejectRequest(@PathVariable("id") Long id, @RequestBody RejectAccessRequestRequest request,
                                         @AuthenticationPrincipal User admin) {
        log.info("POST /api/access-requests/{}/rejectRequest username={} project={}", id, admin.getUsername(), admin.getProjectName());
        return AccessRequestResponse.from(accessRequestService.rejectRequest(id, request, admin));
    }
}
