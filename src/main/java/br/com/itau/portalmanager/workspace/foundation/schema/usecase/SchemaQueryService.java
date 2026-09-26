package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaVersionRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaOutput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaVersionOutput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SchemaQueryService {

    private final SchemaRepository schemaRepository;
    private final SchemaVersionRepository versionRepository;

    public SchemaQueryService(
            SchemaRepository schemaRepository,
            SchemaVersionRepository versionRepository
    ) {
        this.schemaRepository = schemaRepository;
        this.versionRepository = versionRepository;
    }

    @Transactional(readOnly = true)
    public SchemaOutput findByIdentifier(String identifier) {
        return SchemaOutput.from(required(identifier));
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
        Schema schema = required(schemaIdentifier);
        return versionRepository.findBySchema_IdOrderBySchemaVersionDesc(schema.getId())
                .stream()
                .map(SchemaVersionOutput::from)
                .toList();
    }

    private Schema required(String identifier) {
        return schemaRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found"));
    }
}
