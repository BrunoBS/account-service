package br.com.portalmanager.platform.workspace.core.publisher.domain;

public final class PublisherSchemaType {
    private PublisherSchemaType() {}
    public static String forCode(String publisherCode) { return "PUBLISHER_" + publisherCode; }
}
