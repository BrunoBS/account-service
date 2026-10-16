> Documento histórico. Consulte [Service Context](../service-context/SERVICE-CONTEXT.md) para nomes e contratos atuais.

# Revisão geral e endpoints de Platform

> Registro da revisão anterior à separação dos quatro fluxos. A estrutura e os contratos atuais, incluindo a retirada de `featureIdentifiers` do cadastro de contexto, estão em [Platform — separação de responsabilidades](PLATFORM-STRUCTURE-REFACTOR.md). As evidências abaixo são históricas.


Data: 13/10/2026. Escopo: microservices, features e contexts. Referência: [padrão de revisão](../../REVIEW-STANDARD.md).

## Resultado da revisão geral

As três camadas controller → facade → use cases seguem o mesmo contrato de autorização OWNER, validação do JSON original, identidade pública UUID, código imutável, versão do cliente em PUT, lifecycle e auditoria transacional. Listagens são paginadas e usam o `LifecycleFilter` central. As rotas têm prefixos e combinações método/caminho distintos.

Foi encontrado e corrigido um gap entre consulta histórica e limpeza: a feature em INACTIVE/QUARANTINED mantinha relações com contexts e permitia removê-las individualmente, mas `GET /features/{featureIdentifier}/contexts` retornava 404 porque exigia feature ACTIVE. A consulta passou a exigir que a feature exista, mantendo OWNER e o filtro de lifecycle dos contexts. O detalhe da feature e seu update continuam restritos a ACTIVE. Nenhuma restauração ou nova associação a feature inativa foi liberada.

O teste de reprodução confirmou **200 esperado / 404 recebido** antes da correção. Depois da alteração, o fluxo permite descobrir as relações de uma feature inativa/em quarentena, remover o vínculo e concluir a exclusão do context sem reativar a feature.

Não foi necessário remover ou criar endpoints. Foram mantidas as regras de retenção; decisões de produto e melhorias opcionais estão separadas abaixo.

## Contratos comuns

| Tema | Contrato |
| --- | --- |
| Autorização | OWNER em todas as 24 operações; ações CREATE, READ, UPDATE, ACTIVATE, DEACTIVATE e DELETE na facade. |
| Create | Normalização → schema original → autorização → regras de negócio → ACTIVE → flush/snapshot; HTTP 201. Não aceita versão não nula. |
| Update | PUT exige `version` atual e `code` registrado. Ativo, unicidade e referências são verificados sob os locks necessários; versão desatualizada: 409. |
| Detalhe | GET por UUID retorna somente recurso ACTIVE; demais estados: 404. A consulta de relações históricas da feature é a exceção justificada para limpeza. |
| Coleções | Page; padrão ACTIVE, página 0, size 20, sort ID. ACTIVE/INACTIVE/QUARANTINED pelo filtro central. Resolver limita size a 100. |
| Sort | `id`, `identifier`, `code`, `name`, `createdAt`, `updatedAt`. Caminhos internos, campos desconhecidos e offsets inválidos são rejeitados. |
| Inativação | ACTIVE → INACTIVE; preserva vínculos e impede novas referências quando necessário. |
| Ativação | INACTIVE → ACTIVE. QUARANTINED não é restaurado por activate. |
| Exclusão do recurso | INACTIVE → QUARANTINED, com constraints específicas de dependência; HTTP 204. Nenhum dos três recursos é removido fisicamente. |
| Exclusão de vínculo | Remove somente a relação feature/context; retorna feature com HTTP 200. Não exclui nenhum dos dois recursos. |
| Auditoria | Snapshot após flush e evento na mesma transação; falha reverte mutação, versões, vínculos e eventos. Vínculo registra também `contextIdentifier`. |
| Settings | Contratos HTTP em `PLATFORM/microservice`, `PLATFORM/feature` e `PLATFORM/context`; schemas de settings em `MICROSERVICE/{code}` e `FEATURE/{code}`. Context não tem settings. |

Os caminhos nas tabelas seguintes são relativos a **`/api/v1/platform`**. Todos os identificadores entre chaves são UUIDs públicos. Os exemplos devem usar as URLs canônicas sem acrescentar uma barra final.

## MicroserviceController — 7 endpoints

| Método e caminho | O que faz | Pré-condição / resultado | Faz sentido manter? |
| --- | --- | --- | --- |
| `POST /microservices` | Cadastra a definição de um microservice e seus settings; não executa deploy. | Código/nome únicos, settings válidos; ACTIVE, 201. | Sim: criação do catálogo de microservices. |
| `GET /microservices` | Lista microservices por lifecycle, com paginação e sort. | ACTIVE por padrão; 200 com Page. | Sim: descoberta e administração por estado. |
| `GET /microservices/{microserviceIdentifier}` | Obtém o detalhe e a versão para edição. | Somente ACTIVE; 200 ou 404. | Sim: detalhe independente da coleção. |
| `PUT /microservices/{microserviceIdentifier}` | Atualiza nome, descrição e settings. | ACTIVE, versão atual e código imutável; 200. | Sim: edição dos dados, sem substituir ações de lifecycle. |
| `PATCH /microservices/{microserviceIdentifier}/inactivate` | Suspende a elegibilidade para novas referências. | ACTIVE → INACTIVE; 200. Features/mensagens existentes permanecem. | Sim: suspensão reversível. |
| `PATCH /microservices/{microserviceIdentifier}/activate` | Torna o microservice novamente ativo. | INACTIVE → ACTIVE; 200. | Sim: retorno da suspensão. |
| `DELETE /microservices/{microserviceIdentifier}` | Coloca o microservice em quarentena. | INACTIVE e sem nenhuma feature associada, inclusive em quarentena; 204. | Sim, dentro da política atual de retenção. |

## FeatureController — 10 endpoints

| Método e caminho | O que faz | Pré-condição / resultado | Faz sentido manter? |
| --- | --- | --- | --- |
| `POST /features` | Cadastra uma feature vinculada a um microservice, com settings e opção shareable. | Pai ACTIVE, código/nome únicos; feature ACTIVE, 201. | Sim: criação do recurso. |
| `GET /features` | Lista features por lifecycle e, opcionalmente, por `contextCode`. | Page; filtro no banco sem duplicação; 200. | Sim: coleção e busca das features de um context. |
| `GET /features/{featureIdentifier}` | Obtém dados, settings, pai e versão da feature. | Feature ACTIVE; pai inativo não impede a leitura; 200. | Sim: detalhe para administração. |
| `PUT /features/{featureIdentifier}` | Edita atributos/settings/shareable e pode trocar o microservice. | Feature ACTIVE, versão atual, código imutável e pai informado ACTIVE; 200. `shareable=false` bloqueado enquanto houver contratos Shared. | Sim: edição e mudança de referência validadas no mesmo agregado. |
| `PATCH /features/{featureIdentifier}/inactivate` | Inativa a feature, mantendo vínculos e contratos. | ACTIVE → INACTIVE; 200. | Sim: suspensão reversível. |
| `PATCH /features/{featureIdentifier}/activate` | Ativa novamente a feature. | INACTIVE e microservice ACTIVE; 200. | Sim: retomada com pai elegível. |
| `DELETE /features/{featureIdentifier}` | Coloca a feature em quarentena. | INACTIVE; vínculos com microservice, contexts e contratos permanecem; 204. | Sim: retenção sem exclusão física. |
| `GET /features/{featureIdentifier}/contexts` | Lista os contexts vinculados, inclusive para limpeza/histórico. | Feature existente em qualquer lifecycle; `lifecycle` filtra os contexts, ACTIVE por padrão; Page, 200. | Sim: paginação da associação e descoberta dos vínculos históricos. |
| `POST /features/{featureIdentifier}/contexts/{contextIdentifier}` | Adiciona somente esse vínculo. | Context, feature e microservice ACTIVE; 200. Vínculo válido já existente é idempotente. | Sim: mudança pontual sem reenviar a lista completa. |
| `DELETE /features/{featureIdentifier}/contexts/{contextIdentifier}` | Remove somente esse vínculo. | Recursos existentes, em qualquer lifecycle; 200. Vínculo ausente não muda versões nem gera evento. | Sim: remoção pontual e limpeza após inativação/quarentena. |

## FeatureContextController — 7 endpoints

| Método e caminho | O que faz | Pré-condição / resultado | Faz sentido manter? |
| --- | --- | --- | --- |
| `POST /contexts` | Cadastra context e associa opcionalmente a lista inicial de features. | Código/nome únicos; novas referências ativas; 201. | Sim: cadastro atômico do context e de suas relações iniciais. |
| `GET /contexts` | Lista contexts por lifecycle, com paginação e sort. | ACTIVE por padrão; Page, 200. | Sim: catálogo e seleção por estado. |
| `GET /contexts/{contextIdentifier}` | Obtém atributos e versão do context. | ACTIVE; 200 ou 404. Não carrega todas as features na resposta. | Sim: detalhe para edição. |
| `PUT /contexts/{contextIdentifier}` | Edita atributos e substitui opcionalmente a seleção de features. | ACTIVE, versão atual, código imutável; até 100 UUIDs por requisição; 200. | Sim: edição e substituição atômica em lote. |
| `PATCH /contexts/{contextIdentifier}/inactivate` | Suspende novas associações, preservando as existentes. | ACTIVE → INACTIVE; 200. | Sim: suspensão reversível. |
| `PATCH /contexts/{contextIdentifier}/activate` | Ativa novamente o context sem recriar seus vínculos. | INACTIVE → ACTIVE; 200. | Sim: retomada do recurso. |
| `DELETE /contexts/{contextIdentifier}` | Coloca o context em quarentena. | INACTIVE e sem qualquer vínculo com feature, inclusive inativa/em quarentena; 204. | Sim: exclusão lógica após limpeza das dependências. |

## Redundâncias e conflitos

| Combinação | Avaliação |
| --- | --- |
| GET coleção × GET detalhe | Complementares: coleção filtra/pagina; detalhe entrega o recurso ativo por identidade. |
| PUT × PATCH activate/inactivate | Complementares: PUT altera dados e exige versão; PATCH expressa a transição sobre o estado atual e não aceita alteração de atributos no body. |
| PATCH inactivate × DELETE | Não são equivalentes: inativação é reversível; DELETE entra em quarentena e não tem restore. |
| DELETE feature × DELETE feature/context | Recursos diferentes: o primeiro põe a feature em quarentena e mantém relações; o segundo remove somente a associação. |
| POST context com lista × POST associação individual | Complementares: inicialização atômica versus inclusão incremental posterior. |
| PUT context com lista × POST/DELETE associação individual | Sobreposição intencional sobre a mesma relação: substituição do conjunto versus mudança de um par. Compartilham regras, locks, versões e auditoria. A mudança individual avança a versão do context; PUT baseado em versão anterior retorna 409. |
| Lista omitida/null × lista vazia no PUT context | Contratos distintos: omitida/null preserva vínculos; vazia remove todos. Uma lista informada substitui o conjunto. O consumidor deve enviar a lista apenas quando pretende mudar as relações. |
| GET features com contextCode × GET feature/contexts | Relação navegada em sentidos opostos: features de um context versus contexts de uma feature. Não duplicam o mesmo resultado. |
| GET feature/contexts × feature inativa/em quarentena | Havia conflito funcional com a limpeza; corrigido nesta revisão. A autorização OWNER e o lifecycle dos contexts foram preservados. |
| Métodos/caminhos dos controllers | 24 combinações distintas; templates normalizados e URLs representativas têm um único handler compatível. A aplicação e os testes HTTP confirmam os dispatches. |

Não recomendo remover as rotas individuais de associação. Elas permitem mudar um vínculo ou limpar recursos inativos sem enviar o conjunto inteiro. Também não recomendo remover a lista do create/update de context: ela serve ao cadastro e à edição atômicos. A proteção de versão é o que impede uma forma de escrita de sobrescrever silenciosamente a outra.

## Pontos que merecem decisão ou documentação

1. **Retenção do microservice:** excluir a feature não libera o pai. Uma feature QUARANTINED continua referenciando seu microservice e impede seu DELETE. Essa é a regra vigente, preservada e testada; não há purge, cascade ou desvinculação de pai para uma feature em quarentena. Se o produto espera excluir o pai depois de excluir todos os filhos logicamente, será necessário mudar essa política explicitamente. Não foi aplicada uma exclusão histórica automática.
2. **Edição sob pai inativo:** PUT de feature exige que o microservice informado esteja ACTIVE, mesmo quando é o pai já registrado. A leitura continua permitida e os vínculos permanecem. Essa é a política atual de escrita; preservação de vínculo não equivale a liberar qualquer edição da feature com pai inativo.
3. **Busca por microservice:** GET de features oferece lifecycle e contextCode, mas não filtro por microserviceIdentifier. Não é falha de integridade; é uma melhoria útil caso a tela precise listar features de um microservice. Preferir acrescentar um filtro à coleção existente a criar uma rota que devolva a mesma coleção.
4. **Inspeção histórica:** o `lifecycle` de GET feature/contexts filtra o context, não a feature. Para listar contexts inativos de uma feature em quarentena, usar `?lifecycle=INACTIVE`. O detalhe individual da feature continua retornando 404 nesses estados.
5. **Limite de lote:** o limite de 100 itens é por request de context; não existe limite de 100 relações totais imposto às operações individuais. Listagens são paginadas e o consumidor deve distinguir seleção completa de edição apenas dos atributos.
6. **Idempotência condicionada:** repetir POST de vínculo exige referências ainda elegíveis. Após inativação, ele pode retornar erro de estado mesmo quando a relação existe. DELETE de vínculo permanece disponível para limpeza em qualquer lifecycle e é idempotente enquanto os recursos existem.

## Validação e evidência

- Inventário dos três controllers: 7 microservices, 10 features e 7 contexts; 24 combinações método/caminho únicas. Artefato: `/private/tmp/platform-controller-inventory.json`.
- `PlatformEndpointContractIT`: fluxo HTTP que percorre as 24 rotas, negação de autorização para todas elas sem mutação, substituição em lote versus mudança individual, retenção do pai e limpeza de relações históricas.
- `FeatureApiIT`: novo cenário que reproduziu o 404 e verifica a consulta histórica para INACTIVE/QUARANTINED, filtro do lifecycle dos contexts, recurso ausente, limpeza e OWNER negado.
- Rodada final aprovada: **106 testes unitários/arquitetura e 134 testes de integração em 10 classes**, sem falhas, erros ou skips. Inclui as 24 rotas, a consulta histórica corrigida, autorização, auditoria, concorrência, lifecycle e Shared.
- Comando: `mvn --offline --batch-mode --no-transfer-progress -Dit.test=PlatformEndpointContractIT,MicroserviceApiIT,FeatureApiIT,ContextApiIT,PlatformApiIT,LifecycleListingIT,MicroserviceConcurrencyIT,FeatureConcurrencyIT,ContextConcurrencyIT,SharedApiIT verify`. Log: `/private/tmp/platform-final-endpoints-review.log`.
- Reprodução anterior à correção: `/private/tmp/platform-historical-relations-reproduce.log`, confirmando 404 na consulta histórica. O novo cenário passou na rodada final, junto com os outros 19 testes de FeatureApiIT.
- `git diff --check` aprovado. Nenhum achado bloqueador ou de alta severidade permanece aberto; a decisão de retenção do microservice e o filtro opcional de coleção foram documentados sem mudar o contrato por suposição.
- A bateria completa anterior à revisão geral aprovou 106 unitários/arquitetura e 333 testes de integração em 55 classes. Esse resultado é histórico; os testes novos e a mudança de consulta são verificados pela rodada atual, separadamente.

A revisão geral encontrou um gap funcional de consulta/limpeza, corrigido acima. Os demais pontos listados representam contratos ou melhorias opcionais e não colisões de rotas. As alterações desta etapa se limitam à consulta histórica, testes e documentação; nenhuma regra de exclusão ou autorização foi relaxada.
