package br.com.portalmanager.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "account_approvers")
public class AccountApprover {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "functional", nullable = false, length = 255)
    private String functional;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    protected AccountApprover() {
    }

    AccountApprover(String functional, String email, Account account) {
        this.id = UUID.randomUUID().toString();
        this.functional = functional;
        this.email = email;
        this.account = account;
    }

    public String getId() {
        return id;
    }

    public String getFunctional() {
        return functional;
    }

    public String getEmail() {
        return email;
    }

    public Account getAccount() {
        return account;
    }
}
