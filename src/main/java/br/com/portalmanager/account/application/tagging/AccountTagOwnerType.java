package br.com.portalmanager.account.application.tagging;

import br.com.portalmanager.core.tagging.model.TagOwnerType;

public enum AccountTagOwnerType implements TagOwnerType {
    ACCOUNT;

    @Override
    public String value() {
        return name();
    }
}
