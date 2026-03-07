package com.clinic.app.invitations.api.dto;

import java.util.List;

public record InvitationPageResponse(
    List<InvitationResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {}