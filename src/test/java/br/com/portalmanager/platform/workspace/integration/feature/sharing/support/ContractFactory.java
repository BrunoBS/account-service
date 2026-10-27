package br.com.portalmanager.platform.workspace.integration.feature.sharing.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.request.ContractRequest;

public final class ContractFactory extends AbstractTestDataFactory<ContractRequest, ContractBuilder> {
    @Override protected ContractBuilder builder() { return ContractBuilder.builder(); }
    public ContractRequest valid(String featureIdentifier) { return builder().withFeatureIdentifier(featureIdentifier).build(); }
    public ContractRequest withDescription(String featureIdentifier, String description) {
        return builder().withFeatureIdentifier(featureIdentifier).withDescription(description).build();
    }
    public ContractRequest withoutFeature() { return builder().withFeatureIdentifier(null).build(); }
    public ContractRequest withFeature(String featureIdentifier) { return builder().withFeatureIdentifier(featureIdentifier).build(); }
    public ContractRequest withDescriptionAboveMaximum(String featureIdentifier) {
        return builder().withFeatureIdentifier(featureIdentifier).withDescription("d".repeat(501)).build();
    }
}
