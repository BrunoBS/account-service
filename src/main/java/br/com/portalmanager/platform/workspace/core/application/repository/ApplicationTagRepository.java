package br.com.portalmanager.platform.workspace.core.application.repository;

import br.com.portalmanager.platform.library.tagging.storage.TagRepository;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationTagRepository extends TagRepository<ApplicationTag, Application> {
}
