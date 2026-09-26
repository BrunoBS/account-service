package br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceType;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.repository.WorkspaceTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
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
