package br.com.portalmanager.platform.workspace.core.workspace.repository;

import br.com.portalmanager.platform.library.tagging.storage.TagRepository;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceTagRepository extends TagRepository<WorkspaceTag, Workspace> {
}
