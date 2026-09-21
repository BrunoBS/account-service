package br.com.portalmanager.account.application.model;

public record ApproverCommand(
        String functional,
        String email
) {
}
