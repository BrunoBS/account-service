package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.messagetranslator.request.CreateMessageTranslatorRequest;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.messagetranslator.request.UpdateMessageTranslatorRequest;

public final class MessageTranslatorFactory extends AbstractTestDataFactory<CreateMessageTranslatorRequest, MessageTranslatorBuilder> {
    @Override protected MessageTranslatorBuilder builder() { return MessageTranslatorBuilder.builder(); }

    public CreateMessageTranslatorRequest valid(String locale) {
        return create(b -> b.withLocale(locale));
    }

    public CreateMessageTranslatorRequest withoutTitle() {
        return create(b -> b.withTitle(null));
    }

    public UpdateMessageTranslatorRequest update(Long version, String locale, String title) {
        return builder().withVersion(version).withLocale(locale).withTitle(title).buildUpdate();
    }
}
