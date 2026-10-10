package br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationType;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.repository.AuthorizationTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuthorizationTypeService extends EnumCatalogService<AuthorizationType, AuthorizationTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "authorization-type";

    public AuthorizationTypeService(
        AuthorizationTypeRepository repository,
        ObjectMapper objectMapper,
        SchemaValidator schemaValidator
    ) {
        super(
            repository,
            objectMapper,
            AuthorizationType.class,
            AuthorizationTypeEnum.class,
            SCHEMA_RESOURCE_CODE,
            schemaValidator
        );
    }
}
