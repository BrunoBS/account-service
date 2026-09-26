package br.com.itau.portalmanager.workspace.foundation.schema.usecase.schema;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaOutput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaVersionOutput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.support.SchemaFinder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SchemaQueryService {

    private final SchemaRepository schemaRepository;
    private final SchemaVersionRepository versionRepository;
    private final SchemaFinder finder;

    public SchemaQueryService(
            SchemaRepository schemaRepository,
            SchemaVersionRepository versionRepository,
            SchemaFinder finder
    ) {
        this.schemaRepository = schemaRepository;
        this.versionRepository = versionRepository;
        this.finder = finder;
    }

    @Transactional(readOnly = true)
    public SchemaOutput findByIdentifier(String identifier) {
        return SchemaOutput.from(finder.find(identifier));
    }

    @Transactional(readOnly = true)
    public SchemaOutput findPlatformByIdentifier(String identifier) {
        return SchemaOutput.from(finder.findScoped(identifier, "PLATFORM", null));
    }

    @Transactional(readOnly = true)
    public SchemaOutput findWorkspaceByIdentifier(String workspaceIdentifier, String identifier) {
        return SchemaOutput.from(finder.findScoped(identifier, "WORKSPACE", workspaceIdentifier));
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findAll() {
        return schemaRepository.findAll().stream().map(SchemaOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findPlatform() {
        return schemaRepository.findAllByScope("PLATFORM", null)
                .stream()
                .map(SchemaOutput::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchemaOutput> findWorkspace(String workspaceIdentifier) {
        return schemaRepository.findAllByScope("WORKSPACE", workspaceIdentifier)
                .stream()
                .map(SchemaOutput::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchemaVersionOutput> findVersions(String schemaIdentifier) {
        Schema schema = finder.find(schemaIdentifier);
        return versionRepository.findBySchema_IdOrderBySchemaVersionDesc(schema.getId())
                .stream()
                .map(SchemaVersionOutput::from)
                .toList();
    }

}
