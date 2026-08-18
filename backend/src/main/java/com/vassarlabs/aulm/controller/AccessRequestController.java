package com.vassarlabs.aulm.controller;

import com.vassarlabs.aulm.dto.AccessRequestResponse;
import com.vassarlabs.aulm.dto.ApproveAccessRequestRequest;
import com.vassarlabs.aulm.dto.CreateAccessRequestRequest;
import com.vassarlabs.aulm.dto.RejectAccessRequestRequest;
import com.vassarlabs.aulm.model.User;
import com.vassarlabs.aulm.service.AccessRequestService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access-requests")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    public AccessRequestController(AccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    @PostMapping("/mine")
    public AccessRequestResponse createMyRequest(@AuthenticationPrincipal User user,
                                                   @Valid @RequestBody CreateAccessRequestRequest request) {
        return AccessRequestResponse.from(accessRequestService.createRequest(user, request));
    }

    @GetMapping("/mine")
    public List<AccessRequestResponse> myRequests(@AuthenticationPrincipal User user) {
        return accessRequestService.myRequests(user).stream().map(AccessRequestResponse::from).toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccessRequestResponse> listPending(@AuthenticationPrincipal User admin) {
        return accessRequestService.listPending(admin).stream().map(AccessRequestResponse::from).toList();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public AccessRequestResponse approve(@PathVariable("id") Long id, @Valid @RequestBody ApproveAccessRequestRequest request,
                                          @AuthenticationPrincipal User admin) {
        return AccessRequestResponse.from(accessRequestService.approve(id, request, admin));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public AccessRequestResponse reject(@PathVariable("id") Long id, @RequestBody RejectAccessRequestRequest request,
                                         @AuthenticationPrincipal User admin) {
        return AccessRequestResponse.from(accessRequestService.reject(id, request, admin));
    }
}
