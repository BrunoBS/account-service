package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.domain.parameterkey.ParameterKey;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.parameterkey.DynamicParameterMicroserviceAdapter.Reference;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyMetadata;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Separate Spring entry point audits real transitions while retries remain no-ops. */
@Service
public class ParameterKeyLifecycleOperations {
    private final ParameterKeyRepository repository;

    public ParameterKeyLifecycleOperations(ParameterKeyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Auditable(
            action = AuditAction.ACTIVATE,
            event = "PARAMETER_KEY_ACTIVATED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata activate(ParameterKey resource, Reference reference) {
        resource.activate(LocalDateTime.now());
        return ParameterKeyMetadata.from(repository.saveAndFlush(resource), reference);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "PARAMETER_KEY_INACTIVATED",
            resourceType = "PARAMETER_KEY")
    public ParameterKeyMetadata inactivate(ParameterKey resource, Reference reference) {
        resource.inactivate(LocalDateTime.now());
        return ParameterKeyMetadata.from(repository.saveAndFlush(resource), reference);
    }
}
