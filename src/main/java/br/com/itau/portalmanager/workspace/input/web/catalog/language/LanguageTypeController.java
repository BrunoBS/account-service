package br.com.itau.portalmanager.workspace.input.web.catalog.language;

import br.com.itau.portalmanager.workspace.foundation.catalog.languagetype.domain.LanguageType;
import br.com.itau.portalmanager.workspace.foundation.catalog.languagetype.usecase.LanguageTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/language-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class LanguageTypeController extends CatalogController<LanguageType> {

    public LanguageTypeController(LanguageTypeService service) {
        super(service);
    }
}
