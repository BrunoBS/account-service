package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.authorization;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.authorization.AuthorizationType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.authorization.AuthorizationTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.authorization.AuthorizationTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuthorizationTypeService extends EnumCatalogService<AuthorizationType, AuthorizationTypeEnum> {

    public AuthorizationTypeService(
            AuthorizationTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                AuthorizationType.class,
                AuthorizationTypeEnum.class,
                (dto, result) -> settingsValidator.validateJson(SchemaDefaults.DEFAULT_JSON_SCHEMA, dto.settings(), "settings", result)
        );
    }
}
