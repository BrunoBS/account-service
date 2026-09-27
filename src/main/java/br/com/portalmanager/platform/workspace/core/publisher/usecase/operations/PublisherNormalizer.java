package br.com.portalmanager.platform.workspace.core.publisher.usecase.operations;

import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.CreatePublisherInput;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.UpdatePublisherInput;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class PublisherNormalizer {
    public CreatePublisherInput normalize(CreatePublisherInput input) {
        if (input == null) return null;
        return new CreatePublisherInput(code(input.code()), trim(input.name()), trim(input.description()),
                code(input.scope()), input.deprecated());
    }
    public UpdatePublisherInput normalize(UpdatePublisherInput input) {
        if (input == null) return null;
        return new UpdatePublisherInput(input.version(), trim(input.name()), trim(input.description()),
                code(input.scope()), input.deprecated());
    }
    public String normalizeScope(String scope) { return code(scope); }
    private String code(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
    private String trim(String value) { return value == null ? null : value.trim(); }
}
