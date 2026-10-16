> Documento histórico. Consulte [Service Context](../service-context/SERVICE-CONTEXT.md) para nomes e contratos atuais.

# Vínculos de features e contextos

`FeatureContextRelation` representa a associação N:N entre `Feature` e `Context`
na tabela `platform_feature_context_relations`, com chave composta de `feature_id`
e `feature_context_id`. As entidades não mantêm coleções bidirecionais.

## Responsabilidades e rotas

Caminhos relativos a `/api/v1/platform`:

| Controller → fachada → use case | Método | Rota |
| --- | --- | --- |
| FeatureController → FeatureFacade → FeatureQueryService | findAll (filtro contextIdentifier) | GET /features?contextIdentifier={contextIdentifier} |
| ContextController → ContextFacade → ContextQueryService | findAll (filtro featureIdentifier) | GET /contexts?featureIdentifier={featureIdentifier} |
| FeatureContextController → FeatureContextFacade → FeatureContextCommandService | associate | POST /contexts/{contextIdentifier}/features/{featureIdentifier} |
| FeatureContextController → FeatureContextFacade → FeatureContextCommandService | remove | DELETE /contexts/{contextIdentifier}/features/{featureIdentifier} |

`FeatureContextController` e sua fachada têm somente as duas escritas. Não há
`replace`, contrato de lista de UUIDs ou aliases de escrita a partir de features.
POST/PUT do cadastro de contexto recebem apenas seus atributos e rejeitam
`featureIdentifiers`, inclusive null ou lista vazia.

## Regras e integridade

Todos os endpoints exigem OWNER. Novas associações exigem contexto, feature e
microservice ativos. Inativação preserva os vínculos existentes; a remoção física
continua disponível após inativação e não exclui os cadastros globais.
Qualquer vínculo impede o purge do contexto, inclusive com feature inativa.

POST/DELETE não recebem body ou versão do cliente. O contexto é bloqueado antes
da feature; a inclusão também protege a elegibilidade do microservice. A associação
insere o par atomicamente após validar e bloquear os pais, evitando o gap lock de
uma consulta prévia a vínculo inexistente. A remoção lê a relação com lock atual.
Uma mudança real avança as versões de contexto e feature e gera auditoria.
Repetir uma associação elegível ou remover um vínculo ausente não altera versões
nem duplica eventos. Falha de auditoria reverte a alteração integralmente.
POST retorna HTTP 200 com a representação da feature. DELETE retorna HTTP 204
sem corpo, inclusive ao repetir a remoção de um vínculo já ausente entre recursos
existentes. O retorno interno da fachada/command e da operação auditada permanece
disponível para a auditoria; o controller não o serializa no DELETE.

Os controllers expõem somente `findAll` para cada coleção, recebendo o identificador
do vínculo como filtro opcional. Ausente ou em branco, esse filtro usa a consulta
geral. A busca de features usa um único `findAll` em controller, fachada, query
service e repositório, com `contextIdentifier`; `contextCode` foi removido do filtro.
A busca de contextos também usa um único `findAll` em todas as camadas, com
`featureIdentifier`. Ambos os filtros normalizam espaços no identificador.

Quando o filtro de vínculo está preenchido, as consultas exigem a existência do
recurso de origem, inclusive quando inativo,
e retornam páginas. `FeatureQueryService.findAll` filtra o lifecycle das features;
`ContextQueryService.findAll` filtra o dos contextos. Filtro e sort usam a validação central do recurso retornado.
A paginação e a contagem usam o mesmo filtro no banco. A página de features
carrega o microservice necessário à resposta na mesma consulta.

## Schema e evolução

A V65 separou os contratos de contexto sem alterar as relações existentes.
Migrations publicadas permanecem imutáveis; o schema histórico de body de
`PLATFORM/feature-context` não tem consumidor após a remoção de replace.

Microservices/features não têm exclusão; context tem purge físico sem vínculos.
O vínculo usa o lifecycle da feature global. Inativar a feature preserva as relações
e a exclui da listagem padrão de seus contextos (`lifecycle=ACTIVE`). Reativar a
feature a inclui novamente onde o vínculo foi mantido. Para retirar a feature de
um contexto específico, remover fisicamente o vínculo; os demais contextos são
preservados. A consulta administrativa OWNER com `lifecycle=INACTIVE` continua
permitindo localizar referências para limpeza. O vínculo não terá um estado
ativo/inativo independente. Detalhes e evidências:
[estrutura](PLATFORM-STRUCTURE-REFACTOR.md),
[política de exclusão e verificação](PLATFORM-LIFECYCLE-REVIEW.md).
