package br.com.portalmanager.account.api;

import br.com.portalmanager.account.api.request.CreateAccountRequest;
import br.com.portalmanager.account.api.request.UpdateAccountRequest;
import br.com.portalmanager.account.api.response.AccountResponse;
import br.com.portalmanager.account.application.AccountService;
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
    public ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) {
        AccountResponse response = AccountResponse.from(service.create(request.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}")
    public AccountResponse findById(@PathVariable Long accountId) {
        return AccountResponse.from(service.findById(accountId));
    }

    @GetMapping
    public List<AccountResponse> findAll(
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(required = false) String typeName
    ) {
        return service.findAll(active, typeName).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @PutMapping("/{accountId}")
    public AccountResponse update(
            @PathVariable Long accountId,
            @RequestBody UpdateAccountRequest request
    ) {
        return AccountResponse.from(service.update(accountId, request.toCommand()));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deactivate(@PathVariable Long accountId) {
        service.deactivate(accountId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{accountId}/restore")
    public AccountResponse restore(@PathVariable Long accountId) {
        return AccountResponse.from(service.restore(accountId));
    }
}
