package br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.repository.ApplicationScopeTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ApplicationScopeTypeService extends EnumCatalogService<ApplicationScopeType, ApplicationScopeTypeEnum> {

    public ApplicationScopeTypeService(
            ApplicationScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                ApplicationScopeType.class,
                ApplicationScopeTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
