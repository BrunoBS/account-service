package br.com.portalmanager.account.application.tagging;

import br.com.portalmanager.platform.tagging.model.TagOwnerType;

public enum AccountTagOwnerType implements TagOwnerType {
    ACCOUNT;

    @Override
    public String value() {
        return name();
    }
}
