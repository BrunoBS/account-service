package br.com.portalmanager.platform.workspace.core.application.usecase.model;

import java.util.List;

public record CreateApplicationInput(String name, String alias, String acronym, String applicationScope,
                                     String authorizerGroup, String settings, List<String> tags) {}
