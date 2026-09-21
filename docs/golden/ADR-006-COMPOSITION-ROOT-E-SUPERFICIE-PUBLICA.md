# ADR-006 — Composition Root e superfície pública dos módulos

- **Status:** ACCEPTED
- **Data:** 2026-09-21
- **Escopo:** Golden Reference / adequação arquitetural pós-G4
- **Relacionados:** `ADR-ADEQUACAO-ARQUITETURAL-POS-G4.md`, `GOLDEN_REFERENCE_PADRAO_ARQUITETURAL.md`

## Contexto

A adequação pós-G4 fechou a topologia de primeiro nível em:

```text
br.com.itau.portalmanager.workspace
├── foundation
├── core
├── feature
└── input
```

Durante a execução, a antiga `configuration.MessagingConfiguration` foi movida para
`foundation.messaging` com a justificativa de eliminar uma quinta zona.

O review posterior identificou que essa justificativa não demonstra ownership.
`MessagingConfiguration` não implementa Catalog/Schema nem regra reutilizável da
macrozona Foundation da aplicação. Ela adapta a aplicação consumidora ao contrato
`platform-messaging`, declarando um `NoOpApiMessageRepository` porque o banco do
Workspace não é o catálogo corporativo de mensagens.

Também foi identificado que classes auxiliares abaixo de `usecase.support` são
implementação interna do módulo e não devem se tornar, por acidente, API de colaboração
entre módulos.

## Problema

A arquitetura precisava responder explicitamente:

1. onde residem classes Spring de bootstrap/wiring técnico específicas da aplicação;
2. como impedir que uma quinta macrozona seja criada apenas para esse wiring;
3. qual é a superfície pública permitida para colaboração entre módulos.

Sem essa decisão, configurações técnicas poderiam ser distribuídas arbitrariamente em
`foundation`, `core`, `input` ou em uma nova zona, e helpers públicos de um módulo
poderiam ser consumidos por outro módulo sem passar por Use Cases.

## Decisão

### 1. Composition root

O package raiz exato:

```text
br.com.itau.portalmanager.workspace
```

é também o **composition root** da aplicação.

Ele pode conter somente:

- a classe `@SpringBootApplication`;
- classes `@Configuration` de wiring/bootstrap técnico específico do consumidor.

Isso não cria uma quinta macrozona. Subpackages novos diretamente abaixo do root
continuam restritos a `foundation`, `core`, `feature` e `input`.

Classes do composition root não são domínio, Core, Feature, Input nem Application
Foundation. Elas existem apenas para compor o runtime.

As macrozonas internas não devem depender dessas classes de wiring.

### 2. Messaging

`MessagingConfiguration` deixa de pertencer a `foundation.messaging`.

A configuração será renomeada para:

```text
br.com.itau.portalmanager.workspace.WorkspaceMessagingConfiguration
```

Ela continuará declarando somente o adapter necessário ao consumidor:

```text
ApiMessageRepository -> NoOpApiMessageRepository
```

Essa decisão não altera `platform-messaging` nem reabre a Foundation da plataforma.

### 3. Application Foundation x Golden Platform Foundation

A documentação deve distinguir:

- **Golden Platform Foundation**: `platform-build + platform-libraries`;
- **application foundation macrozone**: `br.com.itau.portalmanager.workspace.foundation`.

A macrozona `foundation` da aplicação não deve ser usada como depósito de wiring
Spring apenas por uma responsabilidade ser transversal.

### 4. Superfície pública entre módulos

A colaboração entre módulos de `core`/`feature` permanece **Use Case -> Use Case**.

Quando uma chamada cross-module exigir tipos de contrato, são permitidos tipos
explicitamente associados ao contrato de Use Case, como classes terminadas em:

- `UseCase`;
- `Input`;
- `Output`.

Pacotes/helpers como:

```text
usecase.support
domain
repository
integration
```

são internos ao módulo proprietário e não constituem API de colaboração cross-module.

A identificação de módulo em fitness functions deve considerar o caminho até o primeiro
segmento estrutural `domain/usecase/repository/integration`. Assim, módulos aninhados
como:

```text
core.configuration.workspace
core.configuration.application
```

são tratados como módulos distintos.

## Consequências

### Positivas

- wiring técnico deixa de ser confundido com Foundation;
- não é criada uma quinta macrozona;
- a topologia continua fechada;
- o composition root fica explícito e verificável;
- helpers de Use Case não viram API interna acidental;
- futuros módulos aninhados podem ser governados pelas mesmas fitness functions.

### Trade-offs

- o package raiz passa a aceitar mais de uma classe, porém apenas bootstrap/`@Configuration`;
- fitness functions precisam distinguir composition root de macrozonas;
- configurações técnicas permanecem explícitas na aplicação em vez de ganhar uma abstração genérica.

## Alternativas consideradas

### Manter `foundation.messaging`

Rejeitada. A transversalidade de messaging não prova ownership da macrozona Foundation
da aplicação, e a capability real já é fornecida pela Golden Platform Foundation.

### Criar `configuration` ou `bootstrap` como quinta zona

Rejeitada para o baseline atual. Seria mudança da topologia macro e não é necessária para
o caso concreto.

### Colocar wiring em `input`

Rejeitada. Wiring de runtime não é porta de entrada.

### Colocar wiring em `core.workspace.integration`

Rejeitada para esta configuração. O bean configura a composição global do consumidor e
não uma integração pertencente ao domínio Workspace.

## Pendência relacionada — Maven groupId

O review identificou que o Java package root usa:

```text
br.com.itau.portalmanager.workspace
```

enquanto a coordenada Maven da aplicação permanece:

```text
br.com.portalmanager:workspace-service
```

Não há decisão vigente suficiente no projeto para concluir que o `groupId` Maven deve
ser alterado para acompanhar o namespace Java. Portanto, esta atividade **não altera**
o `groupId` e registra o tema como pendência para decisão de identidade/publicação
antes do checkpoint `GOLDEN-REFERENCE-V1`.

## Dívida relacionada — busca reversa de tags

`WorkspaceTagSearchIntegration` continua sendo uma fronteira explícita para leitura do
dado administrado pela capability de tagging. Entretanto, seu SQL conhece o contrato
físico da tabela `tags`.

Esse acoplamento é aceito provisoriamente porque a API pública atual de `TagManager`
não expõe busca reversa por tag. A dívida deve permanecer documentada e coberta por
teste de integração; ela não autoriza alteração de `platform-libraries` nesta fase.
