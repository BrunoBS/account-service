package br.com.portalmanager.platform.workspace.integration.feature.sharing.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.request.ContractRequest;

public final class ContractBuilder implements TestDataBuilder<ContractRequest> {
    private String description = "Integration resource sharing contract";
    private String featureIdentifier;
    public static ContractBuilder builder() { return new ContractBuilder(); }
    public ContractBuilder withDescription(String value) { description = value; return this; }
    public ContractBuilder withFeatureIdentifier(String value) { featureIdentifier = value; return this; }
    @Override public ContractRequest build() { return new ContractRequest(description, featureIdentifier); }
}
