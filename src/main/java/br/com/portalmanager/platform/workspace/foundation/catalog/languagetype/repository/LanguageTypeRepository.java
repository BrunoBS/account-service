package br.com.portalmanager.platform.workspace.foundation.catalog.languagetype.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.languagetype.domain.LanguageType;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LanguageTypeRepository extends CatalogRepository<LanguageType> {
}
