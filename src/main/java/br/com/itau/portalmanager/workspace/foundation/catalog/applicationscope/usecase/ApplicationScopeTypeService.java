package br.com.itau.portalmanager.workspace.foundation.catalog.applicationscope.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.applicationscope.domain.ApplicationScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.applicationscope.domain.ApplicationScopeTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.applicationscope.repository.ApplicationScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
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
