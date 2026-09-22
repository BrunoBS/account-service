package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.applicationscope;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.applicationscope.ApplicationScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.applicationscope.ApplicationScopeTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.applicationscope.ApplicationScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSettingsSchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ApplicationScopeTypeService extends EnumCatalogService<ApplicationScopeType, ApplicationScopeTypeEnum> {

    public ApplicationScopeTypeService(
            ApplicationScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsSchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                ApplicationScopeType.class,
                ApplicationScopeTypeEnum.class,
                (dto, result) -> settingsValidator.validate(dto.settings(), "settings", result)
        );
    }
}
