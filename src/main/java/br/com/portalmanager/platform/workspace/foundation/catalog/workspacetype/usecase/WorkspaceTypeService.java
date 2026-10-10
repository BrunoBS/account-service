package br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceType;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.repository.WorkspaceTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class WorkspaceTypeService extends EnumCatalogService<WorkspaceType, WorkspaceTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "workspace-type";

    public WorkspaceTypeService(
        WorkspaceTypeRepository repository,
        ObjectMapper objectMapper,
        SchemaValidator schemaValidator
    ) {
        super(
            repository,
            objectMapper,
            WorkspaceType.class,
            WorkspaceTypeEnum.class,
            SCHEMA_RESOURCE_CODE,
            schemaValidator
        );
    }
}
