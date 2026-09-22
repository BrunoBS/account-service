package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.workspace;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.workspace.WorkspaceType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.workspace.WorkspaceTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.workspace.WorkspaceTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class WorkspaceTypeService extends EnumCatalogService<WorkspaceType, WorkspaceTypeEnum> {

    public WorkspaceTypeService(
            WorkspaceTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                WorkspaceType.class,
                WorkspaceTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
