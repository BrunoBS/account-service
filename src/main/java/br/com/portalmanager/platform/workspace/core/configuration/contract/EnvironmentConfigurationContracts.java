package br.com.portalmanager.platform.workspace.core.configuration.contract;

public final class EnvironmentConfigurationContracts {
    public static final String TYPE = "PLATFORM";
    public static final String WORKSPACE_CREATE = "workspace-environment-configuration-create";
    public static final String WORKSPACE_UPDATE = "workspace-environment-configuration-update";
    public static final String APPLICATION_CREATE = "application-environment-configuration-create";
    public static final String APPLICATION_UPDATE = "application-environment-configuration-update";
    public static final String WORKSPACE_SETTINGS = "workspace-environment-configuration-settings";
    public static final String APPLICATION_SETTINGS =
            "application-environment-configuration-settings";
    public static final String PUBLISHER_PARAMETERS =
            "environment-configuration-publisher-parameters";

    private EnvironmentConfigurationContracts() {}
}
