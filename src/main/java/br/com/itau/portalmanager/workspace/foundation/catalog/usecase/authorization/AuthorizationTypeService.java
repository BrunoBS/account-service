package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.authorization;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.authorization.AuthorizationType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.authorization.AuthorizationTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.authorization.AuthorizationTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSettingsSchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuthorizationTypeService extends EnumCatalogService<AuthorizationType, AuthorizationTypeEnum> {

    public AuthorizationTypeService(
            AuthorizationTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsSchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                AuthorizationType.class,
                AuthorizationTypeEnum.class,
                (dto, result) -> settingsValidator.validate(dto.settings(), "settings", result)
        );
    }
}
