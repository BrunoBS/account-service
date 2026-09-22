package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.workspace;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.workspace.WorkspaceType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.workspace.WorkspaceTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.workspace.WorkspaceTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class WorkspaceTypeService extends EnumCatalogService<WorkspaceType, WorkspaceTypeEnum> {

    public WorkspaceTypeService(
            WorkspaceTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                WorkspaceType.class,
                WorkspaceTypeEnum.class,
                (dto, result) -> settingsValidator.validateJson(SchemaDefaults.DEFAULT_JSON_SCHEMA, dto.settings(), "settings", result)
        );
    }
}
