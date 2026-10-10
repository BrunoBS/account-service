package br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation;

import java.util.List;

public record SharedParticipationPageOutput(
    List<SharedParticipationOutput> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
