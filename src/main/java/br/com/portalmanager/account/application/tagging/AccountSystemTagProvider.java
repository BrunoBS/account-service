package br.com.portalmanager.account.application.tagging;

import br.com.portalmanager.account.domain.Account;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class AccountSystemTagProvider {

    public List<String> resolve(Account account) {
        return Arrays.asList(
                account.getIdentifier(),
                account.getName(),
                account.getAuthorizerGroup(),
                account.getAcronym()
        );
    }
}
