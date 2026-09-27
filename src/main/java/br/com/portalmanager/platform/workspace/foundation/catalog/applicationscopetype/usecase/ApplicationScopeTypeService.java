package br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.repository.ApplicationScopeTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ApplicationScopeTypeService extends EnumCatalogService<ApplicationScopeType, ApplicationScopeTypeEnum> {

    private static final String SCHEMA_TYPE_CODE = "APPLICATION_SCOPE_TYPE";

    public ApplicationScopeTypeService(
            ApplicationScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                ApplicationScopeType.class,
                ApplicationScopeTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(SCHEMA_TYPE_CODE, dto.settings(), result)
        );
    }
}
