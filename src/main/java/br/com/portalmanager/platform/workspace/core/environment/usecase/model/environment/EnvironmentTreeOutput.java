package br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment;

import java.util.List;

public record EnvironmentTreeOutput(EnvironmentOutput environment, List<EnvironmentTreeOutput> children) {}
