package br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceType;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.repository.WorkspaceTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class WorkspaceTypeService extends EnumCatalogService<WorkspaceType, WorkspaceTypeEnum> {

    public WorkspaceTypeService(
            WorkspaceTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                WorkspaceType.class,
                WorkspaceTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
