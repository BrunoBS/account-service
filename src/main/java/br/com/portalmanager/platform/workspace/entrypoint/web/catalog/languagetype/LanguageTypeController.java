package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.languagetype;

import br.com.portalmanager.platform.workspace.foundation.catalog.languagetype.domain.LanguageType;
import br.com.portalmanager.platform.workspace.foundation.catalog.languagetype.usecase.LanguageTypeService;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
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
