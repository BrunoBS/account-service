package br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationType;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.repository.AuthorizationTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuthorizationTypeService extends EnumCatalogService<AuthorizationType, AuthorizationTypeEnum> {

    public AuthorizationTypeService(
            AuthorizationTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                AuthorizationType.class,
                AuthorizationTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
