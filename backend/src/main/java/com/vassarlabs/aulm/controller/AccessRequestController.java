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

    @PostMapping("/mine")
    public AccessRequestResponse createMyRequest(@AuthenticationPrincipal User user,
                                                   @Valid @RequestBody CreateAccessRequestRequest request) {
        log.info("POST /api/access-requests/mine username={} project={} type={}", user.getUsername(), user.getProjectName(), request.requestType());
        return AccessRequestResponse.from(accessRequestService.createRequest(user, request));
    }

    @GetMapping("/mine")
    public List<AccessRequestResponse> myRequests(@AuthenticationPrincipal User user) {
        log.info("GET /api/access-requests/mine username={} project={}", user.getUsername(), user.getProjectName());
        return accessRequestService.myRequests(user).stream().map(AccessRequestResponse::from).toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccessRequestResponse> listPending(@AuthenticationPrincipal User admin) {
        log.info("GET /api/access-requests username={} project={}", admin.getUsername(), admin.getProjectName());
        return accessRequestService.listPending(admin).stream().map(AccessRequestResponse::from).toList();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public AccessRequestResponse approve(@PathVariable("id") Long id, @Valid @RequestBody ApproveAccessRequestRequest request,
                                          @AuthenticationPrincipal User admin) {
        log.info("POST /api/access-requests/{}/approve username={} project={}", id, admin.getUsername(), admin.getProjectName());
        return AccessRequestResponse.from(accessRequestService.approve(id, request, admin));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public AccessRequestResponse reject(@PathVariable("id") Long id, @RequestBody RejectAccessRequestRequest request,
                                         @AuthenticationPrincipal User admin) {
        log.info("POST /api/access-requests/{}/reject username={} project={}", id, admin.getUsername(), admin.getProjectName());
        return AccessRequestResponse.from(accessRequestService.reject(id, request, admin));
    }
}
