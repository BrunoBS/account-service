package br.com.portalmanager.platform.workspace.entrypoint.web.environment.request;

public record CreateEnvironmentTypeCompatibilityRequest(String parentTypeCode, String childTypeCode) {}
