package br.com.portalmanager.account.api;

import br.com.portalmanager.account.api.request.CreateAccountRequest;
import br.com.portalmanager.account.api.request.UpdateAccountRequest;
import br.com.portalmanager.account.api.response.AccountResponse;
import br.com.portalmanager.account.application.AccountService;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.audit.annotation.AuditField;
import br.com.portalmanager.platform.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.audit.annotation.Auditable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.OPEN)
    @Auditable(
            resource = "ACCOUNT",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "id")
    )
    public ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) {
        AccountResponse response = AccountResponse.from(service.create(request.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public AccountResponse findById(@PathVariable Long accountId) {
        return AccountResponse.from(service.findById(accountId));
    }

    @GetMapping
    @AuthorizationRequired(level = AuthorizationLevel.OPEN)
    public List<AccountResponse> findAll(
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(required = false) String typeName,
            @RequestParam(required = false) String tagName
    ) {
        return service.findAll(active, typeName, tagName).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @PutMapping("/{accountId}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "ACCOUNT",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "accountId")
    )
    public AccountResponse update(
            @PathVariable Long accountId,
            @RequestBody UpdateAccountRequest request
    ) {
        return AccountResponse.from(service.update(accountId, request.toCommand()));
    }

    @DeleteMapping("/{accountId}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "ACCOUNT",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "accountId")
    )
    public ResponseEntity<Void> deactivate(@PathVariable Long accountId) {
        service.deactivate(accountId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{accountId}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "ACCOUNT",
            action = "RESTORE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "accountId")
    )
    public AccountResponse restore(@PathVariable Long accountId) {
        return AccountResponse.from(service.restore(accountId));
    }
}
