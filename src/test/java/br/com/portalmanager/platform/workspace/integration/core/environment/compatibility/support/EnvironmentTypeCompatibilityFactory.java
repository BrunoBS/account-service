package br.com.portalmanager.platform.workspace.integration.core.environment.compatibility.support;

import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentTypeCompatibilityRequest;

public final class EnvironmentTypeCompatibilityFactory {

    public CreateEnvironmentTypeCompatibilityRequest validDefaultToShard() {
        return new CreateEnvironmentTypeCompatibilityRequest("DEFAULT", "SHARD");
    }

    public CreateEnvironmentTypeCompatibilityRequest validCustomToShard() {
        return new CreateEnvironmentTypeCompatibilityRequest("CUSTOM", "SHARD");
    }

    public CreateEnvironmentTypeCompatibilityRequest validShardToCell() {
        return new CreateEnvironmentTypeCompatibilityRequest("SHARD", "CELL");
    }

    public CreateEnvironmentTypeCompatibilityRequest incompatibleCellToShard() {
        return new CreateEnvironmentTypeCompatibilityRequest("CELL", "SHARD");
    }
}
