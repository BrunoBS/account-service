package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract;

import java.util.List;

public record ContractPageOutput(
        List<ContractOutput> content, int page, int size, long totalElements, int totalPages) {}
