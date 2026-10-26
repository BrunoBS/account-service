package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.messagetranslator.request.CreateMessageTranslatorRequest;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.messagetranslator.request.UpdateMessageTranslatorRequest;

public final class MessageTranslatorBuilder implements TestDataBuilder<CreateMessageTranslatorRequest> {
    private Long version;
    private String locale = "pt-BR";
    private String title = "Workspace created";
    private String detail = "The workspace was created successfully";
    private String suggestion = "Continue setup";

    public static MessageTranslatorBuilder builder() { return new MessageTranslatorBuilder(); }
    public MessageTranslatorBuilder withVersion(Long value) { version = value; return this; }
    public MessageTranslatorBuilder withLocale(String value) { locale = value; return this; }
    public MessageTranslatorBuilder withTitle(String value) { title = value; return this; }
    public MessageTranslatorBuilder withDetail(String value) { detail = value; return this; }
    public MessageTranslatorBuilder withSuggestion(String value) { suggestion = value; return this; }

    @Override public CreateMessageTranslatorRequest build() {
        return new CreateMessageTranslatorRequest(version, locale, title, detail, suggestion);
    }

    public UpdateMessageTranslatorRequest buildUpdate() {
        return new UpdateMessageTranslatorRequest(version, locale, title, detail, suggestion);
    }
}
