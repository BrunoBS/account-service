package br.com.itau.portalmanager.workspace.input.web.catalog.schematype;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.schematype.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.schematype.SchemaTypeService;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.schematype.SchemaTypeDTO;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerBaseCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schema-type")
public class SchemaTypeController extends OwnerBaseCatalogController<SchemaTypeDTO, SchemaType> {

    private final SchemaTypeService service;

    public SchemaTypeController(SchemaTypeService service) {
        this.service = service;
    }

    @Override
    protected SchemaTypeService getService() {
        return service;
    }
}
