> Documento histórico anterior à refatoração. Consulte [Dynamic Bundle](../dynamic-bundle/DYNAMIC-BUNDLE.md) para nomes e contratos atuais.

# Integração com mensagens e traduções

Contrato final da revisão de 2026-10-13. As operações exigem autenticação e autorização ADM por ação. A revisão e as evidências estão em [MESSAGE-CODE-REVIEW.md](MESSAGE-CODE-REVIEW.md).

## Listagens paginadas

Ambos os endpoints retornam um objeto com `content` e `page`:

```http
GET /api/v1/messages?page=0&size=20&lifecycle=ACTIVE&sort=messageKey,asc
GET /api/v1/messages/{messageIdentifier}/translations?page=0&size=20&lifecycle=ACTIVE&sort=locale,asc
```

```json
{
  "content": [],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

| Uso no consumidor | Campo/parâmetro |
| --- | --- |
| Linhas da listagem | `response.content` |
| Total de registros | `response.page.totalElements` |
| Total de páginas | `response.page.totalPages` |
| Página atual, iniciando em zero | `response.page.number` |
| Tamanho da página | `response.page.size` |
| Próxima requisição | `page`, `size` e filtros mantidos |

Os padrões são page 0 e size 20, com limite de 100. Mensagens aceitam filtros `microserviceIdentifier`, `lifecycle`, `code` e `messageKey`; traduções aceitam `locale` e `lifecycle`, sempre no escopo da mensagem informada na rota.

Lifecycle ausente seleciona ACTIVE. ACTIVE e INACTIVE retornam os respectivos registros; QUARANTINED continua reconhecido pelo filtro comum, mas retorna página vazia nesses recursos. Valores vazios ou desconhecidos retornam 400. O detalhe de um recurso existente permanece uma representação individual, sem `content/page`.

## Criação e atualização

| Operação | Version | Traduções inline |
| --- | --- | --- |
| POST de mensagem | Omitir; null também é aceito; valor numérico retorna 400 | Opcionais, no máximo 100; criadas junto com a mensagem |
| PUT de mensagem | Obrigatória, com o valor atual retornado pela API | Não permitidas; usar endpoints próprios de tradução |
| POST de tradução | Omitir; null também é aceito; valor numérico retorna 400 | Body é a própria tradução |
| PUT de tradução | Obrigatória, com a versão atual da tradução | Body é a própria tradução |

Exemplo de POST de mensagem:

```json
{
  "microserviceIdentifier": "11111111-1111-1111-1111-111111111111",
  "messageKey": "workspace.not-found",
  "code": "WORKSPACE-0001",
  "httpStatus": 404,
  "translations": [
    {
      "locale": "pt-BR",
      "title": "Workspace não encontrado",
      "detail": "O workspace informado não foi encontrado.",
      "suggestion": "Confira o identificador e tente novamente."
    }
  ]
}
```

O identificador do exemplo deve ser substituído por um microsserviço existente e ACTIVE. O PUT da mensagem envia os atributos da mensagem e sua `version`, sem `translations`. A presença de version numérica aciona a condição do schema que rejeita a lista; a rota continua definindo a operação, por isso POST com versão numérica é rejeitado pelo validator de criação.

Versão desatualizada retorna 409. O consumidor deve reler o recurso e reconciliar a alteração; a versão da mensagem não substitui a versão da tradução. Campos desconhecidos e tipos incorretos retornam 400 antes do binding.

## Inativação e exclusão física

| Operação | Pré-condição | Efeito |
| --- | --- | --- |
| PATCH mensagem `/inactivate` | Mensagem ACTIVE | Mensagem INACTIVE; traduções e seus estados são preservados |
| DELETE mensagem | Mensagem INACTIVE | Remove mensagem e todas as traduções, inclusive ACTIVE |
| PATCH tradução `/inactivate` | Tradução ACTIVE | Tradução INACTIVE; mensagem permanece |
| DELETE tradução isolada | Tradução INACTIVE; mensagem existente | Remove somente essa tradução; mensagem e traduções irmãs permanecem |

DELETE retorna 204 sem body. Após sucesso, o consumidor remove o registro da tela ou recarrega a página; chamadas posteriores ao identificador removido retornam 404. Não existe etapa de quarentena. A remoção libera chave/código/locale para novos registros, respeitando o escopo de unicidade.

Para excluir uma mensagem, inative a mensagem e envie DELETE; não é necessário inativar cada tradução. Para excluir somente uma tradução, inative a tradução e envie seu DELETE, tanto sob mensagem ACTIVE quanto INACTIVE.

Os snapshots de mensagem e traduções removidas ficam na auditoria com eventos PURGE. Falha em gravação ou auditoria desfaz toda a operação, inclusive a cascata.

## Aplicação das migrations

`spring.flyway.enabled=true` está configurado no serviço e Hibernate usa `ddl-auto=validate`. As migrations são aplicadas automaticamente ao iniciar o serviço contra o banco configurado, antes da utilização dos contratos novos.

| Migration | Resultado |
| --- | --- |
| [V58](../../../src/main/resources/db/migration/V58__message_resource_schemas.sql) | Publica os schemas de mensagem e tradução e registra as configurações |
| [V59](../../../src/main/resources/db/migration/V59__message_physical_deletion_lifecycle.sql) | Preserva legados QUARANTINED como INACTIVE, incrementa versão/data e restringe as duas tabelas a ACTIVE/INACTIVE |

A V59 não exclui dados legados. Consumidores que mantêm versões em cache devem reler esses registros após a atualização. A FK ON DELETE CASCADE é mantida, além da remoção explícita em lote para auditar cada tradução.

Nesta revisão, aplicação em banco limpo e upgrades foram executados nos bancos MySQL de teste. A aplicação ao banco do ambiente ocorre na implantação/inicialização do serviço. Os consumidores encontrados neste repositório são testes de integração e já usam o formato paginado; clientes externos utilizam o contrato descrito acima.
