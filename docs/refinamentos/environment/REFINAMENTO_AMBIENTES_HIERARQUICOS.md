# Refinamento — Evolução da Feature Environment para Topologia Hierárquica

## 1. Objetivo

Evoluir a feature `core/environment` do `account-service` para suportar topologias hierárquicas por workspace, mantendo o conceito único de `Environment` e preservando o comportamento atual para workspaces sem segmentação.

O modelo deve suportar inicialmente ambientes `DEFAULT`, `SHARD` e `CELL`, sem criar features, controllers ou fluxos de publicação específicos para shard/cell.

---

## 2. Baseline analisada — main

A implementação atual já fornece uma base adequada para evolução incremental:

- domínio `core/environment/domain/Environment`;
- `EnvironmentRepository`;
- modelos `CreateEnvironmentInput`, `UpdateEnvironmentInput` e `EnvironmentOutput`;
- operações separadas em `EnvironmentCommandService`, `EnvironmentQueryService`, `EnvironmentFinder` e `EnvironmentNormalizer`;
- validação centralizada em `EnvironmentValidator`;
- APIs separadas entre `DefaultEnvironmentController` e `WorkspaceEnvironmentController`;
- requests/responses próprios da feature;
- catálogo `foundation/catalog/environmenttype`, com `EnvironmentType`, `EnvironmentTypeCode`, `EnvironmentTypeEnum`, repository e service;
- migration `V25__create_environments.sql`;
- teste integrado `EnvironmentApiIT`.

Portanto, a evolução não deve substituir a feature existente. Deve ampliar o modelo atual.

---

## 3. AS-IS

Hoje `Environment` representa o ambiente associado ao workspace e já existe distinção de API entre ambientes default e ambientes de workspace.

O catálogo `EnvironmentType` também já existe no Foundation e é consumido pela feature.

A estrutura atual, porém, é essencialmente plana. Não existe na baseline um conceito explícito de árvore de ambientes, resolução de descendentes/folhas ou regras de integridade hierárquica.

Modelo conceitual atual:

```text
Workspace
  ├── DEV
  ├── HML
  └── PRD
```

---

## 4. TO-BE

`Environment` continuará sendo a única entidade de ambiente.

```text
Workspace
  ├── DEV
  │    └── SHARD A
  ├── HML
  │    ├── SHARD A
  │    └── SHARD B
  └── PRD
       ├── SHARD A
       ├── SHARD B
       └── SHARD C
            └── CELL 01
```

`SHARD` e `CELL` não são novas features. São tipos de `Environment` organizados por relacionamento pai/filho.

---

## 5. Decisão de modelagem

A evolução deve adicionar ao `Environment` a capacidade de referenciar outro `Environment` como pai.

Direção conceitual:

```text
Environment
├── identifier
├── workspaceIdentifier
├── environmentType
├── parentIdentifier   // nullable
├── name
├── order
└── lifecycle/active
```

O `parentIdentifier` deve ser `null` para raízes e preenchido para ambientes subordinados.

Não criar `ShardEnvironment`, `CellEnvironment`, `ShardController` ou `CellController`.

---

## 6. EnvironmentType

O catálogo existente deve ser reaproveitado.

A melhoria necessária é permitir que o tipo expresse a natureza hierárquica do ambiente.

Tipos iniciais:

```text
DEFAULT
SHARD
CELL
```

Compatibilidade inicial:

```text
DEFAULT → SHARD
SHARD   → CELL
```

A modelagem deve continuar extensível para tipos futuros, como `REGION`, `ZONE` ou `CLUSTER`, sem alterar o motor de publicação.

### Decisão importante

Não duplicar no `EnvironmentType` informação que possa ser derivada ou governada de forma mais simples. A necessidade de persistir `parentType` no catálogo deve ser validada durante a implementação. A regra `DEFAULT → SHARD → CELL` pode ser inicialmente uma regra da feature se não houver necessidade real de parametrização dinâmica.

---

## 7. Ambientes default e ambientes customizados

Os ambientes default continuam sendo disponibilizados automaticamente pela plataforma conforme o comportamento atual.

Exemplo:

```text
DEV
HML
PRD
```

A criação de um workspace sem customização deve continuar funcionando sem qualquer conhecimento de shards/cells.

Ambientes customizados são adicionados sobre essa estrutura:

```text
PRD
├── SHARD A
└── SHARD B
```

Cada workspace pode possuir topologia diferente.

---

## 8. Ownership

Um ambiente customizado deve pertencer ao contexto do workspace.

O pai e o filho devem pertencer à mesma topologia acessível pelo workspace.

Não deve ser possível associar como pai um ambiente customizado pertencente a outro workspace.

Os ambientes default/globais devem continuar seguindo o modelo atual de disponibilização da plataforma; não migrar sua propriedade para workspace apenas para atender a hierarquia.

---

## 9. Regras de integridade hierárquica

O `EnvironmentValidator`, que já é o ponto central de validação da feature, deve ser evoluído em vez de criar uma segunda camada paralela de validators.

Novas validações:

- pai informado deve existir;
- ambiente não pode ser pai de si mesmo;
- pai deve pertencer à topologia válida do workspace;
- tipo do filho deve ser compatível com o tipo do pai;
- impedir ciclos;
- impedir mover um nó para um de seus descendentes;
- nome deve ser único entre irmãos;
- validar alteração de tipo quando existirem filhos;
- validar exclusão quando existirem filhos;
- validar inativação considerando descendentes;
- preservar as regras atuais de default/workspace environment.

### Constraint lógica de nome

```text
UNIQUE(workspace, parent, normalized_name)
```

A constraint física deve considerar como a implementação atual representa ambientes default e ownership.

---

## 10. Ciclos

A estrutura é uma árvore e ciclos são inválidos.

Inválido:

```text
A
└── B
    └── C
        └── A
```

Regra:

```text
novoPai ∉ descendentes(environment)
```

A validação deve ocorrer antes da persistência.

---

## 11. Repository — gaps

O `EnvironmentRepository` existente deve ser ampliado, não substituído.

Novas capacidades esperadas:

```text
findChildren(parentIdentifier)
hasChildren(identifier)
findByWorkspaceAndParent(...)
existsSiblingByName(...)
```

A necessidade de consultas recursivas deve ser avaliada conforme o caso de uso. Para o CRUD hierárquico, consultas simples por pai podem ser suficientes inicialmente.

Não introduzir cache de árvore no Golden sem medição e necessidade comprovada.

---

## 12. Finder e Query Service

`EnvironmentFinder` continua responsável pela resolução de entidades obrigatórias/not-found.

`EnvironmentQueryService` deve ganhar operações específicas de leitura hierárquica sem misturar regra de comando.

Possíveis operações:

```text
findChildren(environmentIdentifier)
findTree(workspaceIdentifier)
findRoots(workspaceIdentifier)
```

A consulta de árvore é uma necessidade funcional. Otimizações específicas de read model, projeções ou SQL recursivo ficam para o refinamento de consultas do Golden.

---

## 13. Command Service

`EnvironmentCommandService` deve continuar orquestrando create/update/delete/activate/inactivate.

A hierarquia adiciona passos de orquestração, por exemplo na criação:

```text
normalizar entrada
      ↓
resolver workspace
      ↓
resolver EnvironmentType
      ↓
resolver parent, se informado
      ↓
validar hierarquia
      ↓
persistir Environment
```

Não colocar navegação de árvore ou consultas SQL dentro do domínio.

---

## 14. API

A API deve continuar orientada ao recurso `Environment`.

Não criar endpoints `/shards` ou `/cells`.

Os contratos atuais de workspace environment devem ser evoluídos para aceitar, quando aplicável:

```json
{
  "name": "SHARD A",
  "environmentTypeCode": "SHARD",
  "parentIdentifier": "<environment-parent>"
}
```

A resposta deve expor pelo menos a referência ao pai quando houver hierarquia.

A criação de um `CELL` segue o mesmo endpoint e contrato, mudando tipo e pai.

---

## 15. Controllers existentes

A separação atual entre `DefaultEnvironmentController` e `WorkspaceEnvironmentController` pode ser preservada.

A hierarquia pertence ao contexto de workspace e deve ser incorporada prioritariamente ao `WorkspaceEnvironmentController`.

Não é necessário criar um terceiro controller para ambientes hierárquicos.

---

## 16. Migration

Não alterar `V25__create_environments.sql`, pois ela já faz parte da baseline aplicada.

Criar uma nova migration incremental, por exemplo `V30__environment_hierarchy.sql`, contendo apenas a evolução necessária.

Direção:

```sql
ALTER TABLE environment
    ADD COLUMN parent_identifier ... NULL;
```

A migration definitiva deve usar os nomes reais da tabela/colunas definidos em V25 e adicionar FK/índices coerentes com o schema atual.

Índices esperados conceitualmente:

```text
(parent_identifier)
(workspace_identifier, parent_identifier)
```

Não criar closure table, nested set ou materialized path neste momento. A profundidade inicial é pequena e a adjacência por `parent_identifier` é suficiente para o comportamento funcional.

---

## 17. Regra de folha

A árvore introduz o conceito de nó folha.

```text
leaf(environment) = !hasChildren(environment)
```

Exemplo:

```text
PRD
├── SHARD A
└── SHARD B
```

`SHARD A` e `SHARD B` são folhas; `PRD` é agrupador.

Com células:

```text
PRD
├── SHARD A
│   ├── CELL 01
│   └── CELL 02
└── SHARD B
```

Folhas:

```text
CELL 01
CELL 02
SHARD B
```

---

## 18. Configuração em nós com filhos — decisão pendente

As anotações levantam a possibilidade de que, quando um ambiente ganha filhos, ele deixe de receber configuração diretamente.

Exemplo:

```text
DEV
├── SHARD A
└── SHARD B
```

Possível regra futura:

```text
DEV      → agrupador
SHARD A  → configurável
SHARD B  → configurável
```

Essa regra **não deve ser implementada implicitamente na primeira evolução estrutural**. Ela impacta configuração existente, promoção, rollback e transição de topologia.

Primeiro implementar a árvore. Depois fechar a semântica de configuração por folha.

---

## 19. Destination Resolver

A feature deve prever um componente de resolução de destinos, mas ele não precisa fazer parte da primeira onda estrutural.

Responsabilidade:

```text
destino solicitado
       +
topologia do workspace
       ↓
Destination Resolver
       ↓
folhas efetivas
```

Exemplo:

```text
resolve(PRD)

PRD
├── SHARD A
└── SHARD B

=> [SHARD A, SHARD B]
```

Com células:

```text
PRD
├── SHARD A
│   ├── CELL 01
│   └── CELL 02
└── SHARD B

=> [CELL 01, CELL 02, SHARD B]
```

O resolver **resolve topologia**. Ele não publica configuração.

---

## 20. Publicação

O motor de publicação/promoção não deve conhecer `SHARD`, `CELL` ou tipo de cliente.

Fluxo desejado:

```text
solicitar publicação em PRD
          ↓
resolver destinos
          ↓
[destino 1, destino 2, ...]
          ↓
publicar
```

Não criar:

```java
if (workspace.isShard()) { ... }
```

A transparência da topologia é um requisito arquitetural.

---

## 21. Publicação seletiva — decisão posterior

O desenho permite futuramente selecionar apenas parte das folhas:

```text
HML
├── [x] SHARD A
└── [ ] SHARD B
```

Mas a permissão de publicação parcial é uma regra de produto e deve ser refinada junto ao motor de promoção.

A feature `Environment` deve apenas ser capaz de fornecer a topologia e os destinos válidos.

---

## 22. Application — impacto identificado

A baseline possui `core/application` independente de `core/environment`.

A evolução da árvore exige um refinamento posterior sobre a relação Application × Environment.

Precisamos decidir se uma Application:

- referencia a topologia do workspace;
- recebe uma cópia da topologia;
- pode usar subconjunto da topologia;
- acompanha automaticamente alterações futuras.

Não acoplar essa decisão à primeira migration de Environment.

---

## 23. Publisher — impacto identificado

A baseline também possui `core/publisher` como domínio separado.

Antes de alterar Publisher, deve-se definir se publisher está associado ao ambiente raiz, a uma folha ou se sua resolução ocorre por herança/topologia.

Não adicionar relacionamento shard/cell diretamente em Publisher. A referência deve continuar sendo para `Environment`.

---

## 24. Exclusão e lifecycle

Para a primeira implementação hierárquica, a regra mais segura é bloquear exclusão de um ambiente que possua filhos.

```text
hasChildren(environment) == true
=> delete rejeitado
```

A exclusão em cascata não deve ser introduzida automaticamente.

Para inativação, ainda precisa ser decidido se:

- bloquear pai enquanto houver descendentes ativos; ou
- inativar explicitamente a subárvore.

Não implementar cascata silenciosa.

---

## 25. Mensagens

A feature já possui `EnvironmentMessageKeys` e bundles de mensagens centralizados no serviço.

As novas validações devem seguir esse padrão, adicionando chaves como:

```text
environment.parent.not-found
environment.parent.invalid
environment.parent.self
environment.hierarchy.cycle
environment.hierarchy.invalid-type
environment.children.exists
environment.sibling.name-already-exists
```

Os nomes definitivos devem seguir o padrão já adotado no projeto.

---

## 26. Testes

A baseline possui `EnvironmentApiIT`, mas não há na árvore atual um conjunto específico de testes de domínio/usecase para Environment equivalente ao nível de cobertura de outras áreas.

A evolução deve acrescentar testes para:

- criar SHARD sob DEFAULT;
- criar CELL sob SHARD;
- rejeitar CELL sob DEFAULT, conforme regra inicial;
- rejeitar SHARD sem pai;
- rejeitar pai inexistente;
- rejeitar pai de outro workspace;
- rejeitar self-parent;
- rejeitar ciclo indireto;
- rejeitar nome duplicado entre irmãos;
- permitir mesmo nome sob pais diferentes;
- bloquear delete com filhos;
- consultar filhos;
- consultar árvore;
- manter CRUD atual de ambientes sem hierarquia;
- garantir que workspaces existentes continuem válidos após migration.

Também criar teste de migration/upgrade para banco existente.

---

## 27. GAP analysis

| Área | AS-IS | TO-BE | Ação |
|---|---|---|---|
| Environment | plano | hierárquico | adicionar parent |
| EnvironmentType | catálogo existente | DEFAULT/SHARD/CELL | evoluir sem duplicar catálogo |
| Repository | CRUD/queries atuais | consultas pai/filho | ampliar interface |
| Validator | centralizado | regras de árvore | ampliar `EnvironmentValidator` |
| Command | CRUD atual | orquestra parent/hierarquia | evoluir serviço existente |
| Query | consultas atuais | filhos/raízes/árvore | ampliar serviço existente |
| API | default/workspace | mesmo recurso + parent | evoluir contratos |
| Migration | V25 | incremental | criar V30+ |
| Testes | EnvironmentApiIT | hierarquia + upgrade | ampliar cobertura |
| Publicação | fora da feature | resolver folhas | onda posterior |
| Application | independente | integração a definir | refinamento posterior |
| Publisher | independente | referência continua Environment | refinamento posterior |

---

## 28. Implementação proposta em ondas

### E0 — Baseline e proteção

- manter `main` como referência;
- registrar comportamento atual de Environment;
- ampliar testes de regressão antes da alteração estrutural;
- garantir upgrade Flyway de banco existente.

### E1 — Persistência hierárquica

- nova migration;
- `parent_identifier` nullable;
- FK e índices;
- evolução de `Environment`;
- evolução de input/output/request/response;
- nenhuma mudança ainda em publicação.

### E2 — Validação e CRUD da árvore

- resolver parent no Finder/Command;
- regras de tipo;
- self-parent;
- ciclos;
- unicidade entre irmãos;
- delete com filhos;
- testes unitários/integrados.

### E3 — Consultas hierárquicas

- children;
- roots;
- tree;
- manter consulta simples existente;
- sem cache antecipado.

### E4 — Semântica de folha

Após decisão funcional:

- definir se nó com filhos deixa de ser configurável;
- definir transição quando uma folha ganha primeiro filho;
- definir transição quando um pai perde último filho;
- tratar configurações existentes.

### E5 — Destination Resolver

- resolver folhas a partir de um destino;
- não publicar;
- não conhecer tipo de cliente;
- testes determinísticos da resolução.

### E6 — Integração com promoção/publicação

- publicação em múltiplos destinos;
- total vs seletiva;
- estado por destino;
- falha parcial;
- retry/idempotência;
- auditoria;
- rollback.

### E7 — Application e Publisher

- fechar herança/cópia/referência de topologia para Application;
- fechar associação/herança de Publisher;
- evitar duplicação da árvore.

---

## 29. Critérios de aceite da evolução estrutural

A fase estrutural estará concluída quando:

1. o CRUD atual continuar funcionando;
2. workspaces sem customização não sofrerem alteração comportamental;
3. for possível criar um SHARD sob ambiente DEFAULT;
4. for possível criar uma CELL sob SHARD;
5. ciclos forem impossíveis;
6. pai inválido/outro workspace for rejeitado;
7. nomes de irmãos forem únicos;
8. exclusão de pai com filhos for bloqueada;
9. a árvore puder ser consultada;
10. nenhuma regra específica de cliente for introduzida;
11. nenhuma feature paralela de shard/cell for criada;
12. migrations existentes permanecerem imutáveis;
13. upgrade de uma base existente for testado.

---

## 30. Decisões ainda pendentes

Antes das ondas E4+ devem ser decididos:

- nó com filhos deixa obrigatoriamente de ser configurável?
- nó com filhos deixa obrigatoriamente de ser publicável?
- profundidade fica limitada a DEFAULT → SHARD → CELL ou será genérica?
- mover nós será permitido?
- como tratar configuração existente quando uma folha ganha filhos?
- publicação parcial será permitida?
- como Application consome a árvore?
- como Publisher é resolvido na árvore?
- como lifecycle propaga na subárvore?
- como promoção consolida sucesso parcial?

Esses pontos não devem ser transformados em comportamento por suposição.

---

## 31. Resultado arquitetural esperado

A intenção externa permanece simples:

```text
Publicar em PRD
```

Workspace simples:

```text
PRD
=> PRD
```

Workspace segmentado:

```text
PRD
├── SHARD A
└── SHARD B

=> SHARD A, SHARD B
```

Workspace com células:

```text
PRD
├── SHARD A
│   ├── CELL 01
│   └── CELL 02
└── SHARD B

=> CELL 01, CELL 02, SHARD B
```

A complexidade permanece encapsulada em `Environment` e, posteriormente, no `Destination Resolver`. O restante da plataforma trabalha com ambientes/destinos e não com regras específicas de shard, cell ou tipo de cliente.
