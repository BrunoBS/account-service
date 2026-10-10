# Vínculos de features e contextos

`FeatureContextRelation` representa explicitamente a tabela existente
`platform_feature_context_relations`, com chave composta de `feature_id` e
`feature_context_id`. `Feature` e `FeatureContext` não mantêm coleções bidirecionais.
Não é necessária uma nova migração para esse relacionamento.

O cadastro (`POST /api/v1/platform/contexts`) e a edição
(`PUT /api/v1/platform/contexts/{identifier}`) aceitam `featureIdentifiers`:

```json
{
  "name": "Gestão de contas",
  "description": "Funcionalidades do contexto",
  "featureIdentifiers": ["UUID-da-feature"]
}
```

O cadastro também exige `code`. O serviço resolve os identificadores públicos
para as features e grava os vínculos na mesma transação do contexto.

Na edição, a lista substitui os vínculos atuais: lista vazia remove todos;
campo omitido ou nulo preserva os vínculos. Identificadores repetidos são
normalizados para um único vínculo. Uma feature inexistente faz toda a operação
falhar sem persistir alterações parciais. Novos vínculos exigem contexto ativo.

Os endpoints existentes de associação individual continuam disponíveis.
As consultas por contexto e os contextos de uma feature utilizam o repositório
da entidade de relação. Um contexto vinculado não pode ser excluído.
