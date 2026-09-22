package br.com.itau.portalmanager.workspace.input.web.catalog.language;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.language.LanguageType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.language.LanguageTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/language-type")
public class LanguageTypeController extends OwnerCatalogController<LanguageType> {

    public LanguageTypeController(LanguageTypeService service) {
        super(service);
    }
}
