package br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract;

import java.util.List;

public record SharedContractPageOutput(
    List<SharedContractOutput> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
