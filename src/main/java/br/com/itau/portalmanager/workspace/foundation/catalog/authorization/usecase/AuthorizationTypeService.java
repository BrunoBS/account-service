package br.com.itau.portalmanager.workspace.foundation.catalog.authorization.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.authorization.domain.AuthorizationType;
import br.com.itau.portalmanager.workspace.foundation.catalog.authorization.domain.AuthorizationTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.authorization.repository.AuthorizationTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
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
