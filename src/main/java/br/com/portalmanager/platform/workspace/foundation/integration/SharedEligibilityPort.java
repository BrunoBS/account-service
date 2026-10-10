package br.com.portalmanager.platform.workspace.foundation.integration;

/** Contract used by core capabilities to resolve Shared publication eligibility. */
public interface SharedEligibilityPort {
    SharedEligibilityResult resolve(String ownerWorkspaceIdentifier, String ownerApplicationIdentifier,
            String contractIdentifier, String participantWorkspaceIdentifier,
            String participantApplicationIdentifier, String sourceEnvironmentIdentifier);
}
