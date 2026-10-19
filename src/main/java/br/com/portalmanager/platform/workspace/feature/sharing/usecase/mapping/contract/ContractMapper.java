package br.com.portalmanager.platform.workspace.feature.sharing.usecase.mapping.contract;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.contract.Contract;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractInput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractPageOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractSummaryOutput;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public Contract toDomain(
            ContractInput input,
            Long workspaceId,
            Long applicationId,
            Long featureId,
            LocalDateTime now) {
        return new Contract(
                workspaceId, applicationId, featureId, normalizeDescription(input.description()), now);
    }

    public String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    public ContractOutput toOutput(
            Contract contract,
            String workspaceIdentifier,
            String applicationIdentifier,
            FeatureReferenceOutput feature) {
        return new ContractOutput(
                contract.getIdentifier(), workspaceIdentifier, applicationIdentifier,
                feature.identifier(), feature.name(), contract.getDescription(),
                contract.getLifecycle().value(), contract.getCreatedAt(), contract.getUpdatedAt());
    }

    public ContractSummaryOutput toSummary(
            Contract contract,
            ApplicationReferenceOutput ownerApplication,
            FeatureReferenceOutput feature) {
        return new ContractSummaryOutput(
                contract.getIdentifier(), feature.name(), contract.getLifecycle().value(),
                ownerApplication.workspaceIdentifier(), ownerApplication.identifier(),
                feature.identifier());
    }

    public ContractPageOutput toPageOutput(Page<?> page, List<ContractOutput> content) {
        return new ContractPageOutput(
                content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
