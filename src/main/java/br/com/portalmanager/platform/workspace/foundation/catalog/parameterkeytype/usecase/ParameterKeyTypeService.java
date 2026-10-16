package br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyType;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.repository.ParameterKeyTypeRepository;

import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

@Service
public class ParameterKeyTypeService
        extends EnumCatalogService<ParameterKeyType, ParameterKeyTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "parameter-key-type";

    public ParameterKeyTypeService(
            ParameterKeyTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator schemaValidator) {
        super(
                repository,
                objectMapper,
                ParameterKeyType.class,
                ParameterKeyTypeEnum.class,
                SCHEMA_RESOURCE_CODE,
                schemaValidator);
    }
}
