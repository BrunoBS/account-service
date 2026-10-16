package br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain.ParameterKeyStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain.ParameterKeyStatusTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.repository.ParameterKeyStatusTypeRepository;

import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

@Service
public class ParameterKeyStatusTypeService
        extends EnumCatalogService<ParameterKeyStatusType, ParameterKeyStatusTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "parameter-key-status-type";

    public ParameterKeyStatusTypeService(
            ParameterKeyStatusTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator schemaValidator) {
        super(
                repository,
                objectMapper,
                ParameterKeyStatusType.class,
                ParameterKeyStatusTypeEnum.class,
                SCHEMA_RESOURCE_CODE,
                schemaValidator);
    }
}
