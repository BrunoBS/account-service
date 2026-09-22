package br.com.itau.portalmanager.workspace.input.web.catalog.support;

import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import br.com.portalmanager.platform.catalog.service.BaseCatalogService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

public abstract class OwnerCatalogController<E extends BaseCatalogEntity>
        extends CatalogController<E> {

    protected OwnerCatalogController(BaseCatalogService<E, CatalogDTO> service) {
        super(service);
    }

    @Override
    @GetMapping
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public List<CatalogDTO> findAll(@RequestParam Map<String, String> filters) {
        return super.findAll(filters);
    }

    @Override
    @GetMapping("/{id}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public CatalogDTO findById(@PathVariable Long id) {
        return super.findById(id);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public CatalogDTO create(@RequestBody CatalogDTO dto) {
        return super.create(dto);
    }

    @Override
    @PutMapping("/{id}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public CatalogDTO update(@PathVariable Long id, @RequestBody CatalogDTO dto) {
        return super.update(id, dto);
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public void delete(@PathVariable Long id) {
        super.delete(id);
    }

    @Override
    @PostMapping("/{id}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public CatalogDTO restore(@PathVariable Long id) {
        return super.restore(id);
    }
}
