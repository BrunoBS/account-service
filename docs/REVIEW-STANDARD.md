# Padrão de revisão de features

Este documento define como revisar e evoluir módulos de domínio neste projeto. Use-o como roteiro para novas features e para revisões de módulos existentes. Ele registra critérios arquiteturais e de qualidade; não substitui as regras específicas de cada domínio nem exige abstrações sem necessidade concreta.

## Objetivo da revisão

Para cada alteração, confirmar que o comportamento de negócio está explícito, que as fronteiras entre módulos e camadas são respeitadas, que dados e operações estão protegidos contra estados inválidos e concorrência, e que a implementação é simples de entender e validar.

## Sequência de trabalho

1. Delimitar o escopo: listar endpoints, métodos públicos, entidades, tabelas e módulos que participam do fluxo.
2. Descrever o fluxo atual e o esperado, incluindo atores, pré-condições, estados possíveis, efeitos persistidos e erros.
3. Revisar os pontos abaixo por classe e método, registrando achados com evidência e impacto.
4. Resolver primeiro gaps de regra, integridade, autorização e concorrência; depois legibilidade, duplicação e otimizações justificadas.
5. Implementar ajustes pequenos e coerentes com as fronteiras existentes; não criar camadas, builders, interfaces ou catálogos sem benefício demonstrável.
6. Validar a arquitetura, os cenários afetados e as regressões relevantes. Registrar comandos e resultados.
7. Encerrar com achados resolvidos, riscos remanescentes, decisões pendentes e itens deliberadamente adiados.

## Critérios por área

### 1. Domínio e regras de negócio

- Cada regra tem um único lugar claro de decisão; regras não ficam escondidas em controller, mapper ou repository.
- Entidades encapsulam estado e transições próprias. Use cases coordenam carregamento, autorização, validação, persistência e efeitos entre módulos.
- Identidade técnica, identificador público e referências entre agregados são distinguidos e usados consistentemente.
- Relações refletem necessidades reais de domínio e leitura. Evitar coleções bidirecionais ou carregamento automático de grafos quando não são necessários.
- Regras de unicidade, elegibilidade, lifecycle e compatibilidade valem tanto para criação quanto para edição, quando aplicável.
- Para cada transição, verificar estados de origem permitidos, repetição/idempotência e efeitos sobre dependências.
- Não permitir mudança de um atributo que invalide relações existentes. Proteger a regra no caso de uso e, quando possível, também no banco ou por serialização adequada.

### 2. Use cases, legibilidade e responsabilidades

- Command e query têm responsabilidades distintas quando a separação torna o fluxo mais claro; ambos ficam organizados junto ao módulo e à responsabilidade que atendem.
- Métodos têm nomes objetivos que descrevem a intenção e evitam termos ambíguos. Variáveis e parâmetros usam nomes legíveis, sem abreviações desnecessárias.
- A sequência do método é fácil de seguir: carregar referências necessárias, validar, executar mudança e persistir.
- Compartilhar lógica apenas quando existe comportamento realmente comum. Evitar duplicação, mas não extrair helpers genéricos que escondam a regra ou aumentem saltos de leitura.
- Construtores de produção não devem substituir dependências obrigatórias por implementações silenciosas ou `noop` para facilitar testes. Fornecer doubles no teste.
- Mapper converte modelos entre fronteiras; não consulta banco nem aplica regras. Objetos de detalhes/entrada devem existir quando reduzem duplicação ou tornam o contrato mais claro.
- Evitar retornar dados que o chamador não usa, consultas repetidas e montagem de respostas que provocam carregamentos desnecessários.
- Usar tipos explícitos nas variáveis locais, em vez de `var`, mantendo o tipo do objeto legível no código.

### 3. Arquitetura e integração entre módulos

- Entrypoint Web traduz HTTP em contrato de entrada e saída; não contém regra de negócio nem expõe entidade JPA.
- Use cases consomem repositórios do próprio módulo e interfaces de integração explícitas para capacidades de outros módulos.
- Um módulo não acessa diretamente o repository interno de outro módulo. Use caso de uso público ou contrato `Port` na fronteira apropriada, com `Adapter` na implementação.
- Evitar dependência de `core` para `feature` quando a arquitetura proíbe esse sentido; validar com testes arquiteturais.
- `domain`, `repository`, `integration`, `usecase`, `facade` e `entrypoint` devem conter responsabilidades coerentes. Criar subpacotes de domínio quando houver mais de um domínio interno real, sem fragmentar agregados relacionados.
- Configuração Spring fica em configuração própria quando necessária; não misturar wiring com lógica de negócio.
- Reutilizar capabilities da plataforma quando houver caso concreto, sem criar implementações paralelas ou abstrações equivalentes.

### 4. Persistência, integridade e concorrência

- Em `core` e `feature`, atualizações de entidades, incluindo lifecycle, status e alterações do agregado por associações, usam `repository.saveAndFlush(entity)`. Montar o output/snapshot com a entidade retornada, depois da persistência e do flush, para refletir a versão atual. Criações individuais seguem o mesmo padrão; operações em lote podem usar `saveAllAndFlush`.
- Não substituir a persistência explícita de uma atualização por dirty checking ou `flush()` isolado. O flush intermediário pode ser necessário para ordenar remoções/inclusões e liberar valores únicos; exclusões físicas usam `delete`/`deleteAll` e flush. Operações SQL atômicas de associação/idempotência preservam sua semântica e não são convertidas para save de entidade.
- Atributos equivalentes entre módulos seguem limites e nulabilidade consistentes. Diferenças exigem regra de domínio explícita, incluindo o limite de tags manuais versus tags derivadas de sistema.
- Entidades e migrations concordam em nomes, tipos, nulabilidade, tamanhos, defaults, chaves, índices e relacionamentos.
- Migrations aplicadas são imutáveis; mudanças de schema entram em nova migration. Confirmar que a migration funciona sobre banco limpo e sobre upgrade, quando aplicável.
- Restrições únicas e foreign keys protegem invariantes persistentes. Mensagens de erro não expõem SQL, nomes de constraints ou detalhes internos.
- Uma consulta `exists` antes de gravar não resolve concorrência por si só. Validar a condição de corrida e usar constraint, lock, operação atômica ou tratamento transacional apropriado.
- Após esperar por um lock, usar dados atuais do banco. Verificar entidades já presentes no contexto de persistência e o comportamento do isolamento real; não assumir que `refresh` recupera a versão atual sob qualquer isolamento.
- Para idempotência, definir qual chamada repetida é segura e quais dados originais devem ser preservados.
- Verificar `@Version`, locks e ordem de aquisição quando há alteração concorrente ou mais de um agregado envolvido. Não assumir que um lock protege uma sequência se a evidência de banco/teste não confirmar.
- Escolher relações JPA e cascades com cuidado. Evitar `EAGER`, fetch de coleções em páginas e cascades que removam dados fora do ciclo de vida pretendido.
- Consultas de leitura retornam projeções/`Optional` quando apropriado, fazem paginação no Spring Data e evitam N+1, `limit 1` com retorno de lista e consultas redundantes.
- Índices e otimizações devem responder a filtros/joins reais e ser avaliados com plano de execução ou evidência de volume; não adicionar por especulação.

### 5. Validação, normalização e erros

- O JSON Schema valida estrutura HTTP: campos permitidos, tipos, obrigatoriedade, formatos e limites de tamanho/coleção, conforme o contrato publicado.
- Controllers devem expor requests tipados. Para contratos validados por JSON Schema, validar a árvore do JSON recebido antes da desserialização, em um mecanismo comum de entrada HTTP. A normalização preserva propriedades desconhecidas e tipos originais; somente valores textuais conhecidos são normalizados. Não validar somente a serialização do DTO, pois coerções e descarte de campos podem esconder erros do payload.
- Usar `@ValidateResourceSchemaRequest` da biblioteca no request e, quando necessário, um bean `SchemaPayloadNormalizer`. O advice genérico pertence à biblioteca e é registrado automaticamente; serviços não devem recriar advice, anotação ou enum de recursos para essa capacidade.
- Após a validação estrutural, o request traduz para o input do use case. Facade/command recebem inputs tipados, sem repetir parsing ou validação estrutural. Testar criação e edição com tipos incorretos e propriedades desconhecidas, incluindo objetos aninhados.
- O validator de use case valida regras de negócio: existência e estado de referências, unicidade, autorização contextual, transições e invariantes cruzadas.
- Manter um único arquivo de JSON Schema por recurso, compartilhado por criação e edição. Reutilizar a validação da biblioteca e expressar regras condicionais no próprio schema, sem criar mecanismos paralelos de seleção ou extração de schemas. Obrigatoriedade específica de uma operação fica explícita no use case quando não pode ser expressa pelo contrato compartilhado.
- O arquivo de schema de referência, a versão publicada pela migration e as fixtures de teste devem representar o mesmo contrato. Mudança de contrato requer nova versão publicada, sem editar a migration aplicada.
- Tags manuais são normalizadas e reconciliadas pela capability de tagging. O schema controla estrutura e limites; não criar uma segunda implementação de normalização ou reconciliação.
- Não repetir no validator verificações estruturais que pertencem ao schema. Pré-condições específicas da operação podem continuar no use case.
- Normalização acontece de forma previsível antes da validação pertinente, sem converter silenciosamente tipos inválidos ou apagar propriedades desconhecidas que devem ser rejeitadas.
- Códigos de mensagem ficam em `*MessageKeys` no domínio dono do erro; textos localizados ficam no mecanismo de mensagens. Não inserir nomes de banco ou dados sensíveis na resposta.
- Falhas de configuração do servidor usam erro de configuração da plataforma, não exceção genérica ou erro de validação do usuário.
- Traduzir violações de integridade em erro de domínio amigável sem amarrar o contrato HTTP a uma constraint específica.

### 6. Autorização e visibilidade

- Toda operação define explicitamente a policy/capability requerida e exige o contexto de autenticação esperado.
- Leituras por recurso aplicam visibilidade no ponto de entrada do use case/Finder, inclusive em coleções e integrações que possam revelar existência ou dados.
- Verificar OWNER, usuário autorizado por grupo e usuário sem vínculo, tanto para leitura quanto para escrita; confirmar o comportamento de recurso inexistente/invisível.
- Campos sensíveis podem exigir condição adicional (por exemplo, owner), mesmo quando a operação geral aceita uma policy mais ampla.
- A autorização usa a identidade e os grupos da sessão confiável; nunca aceita ator, usuário ou permissões definidos pelo request.
- Acesso entre recursos autorizado por contrato deve ter ponto de entrada explícito, pré-condição documentada e testes de escopo. Não aplicar indiscriminadamente o filtro do usuário sobre referências históricas ou de compartilhamento autorizadas por outra regra.
- Em listagens paginadas, confirmar que conteúdo e contagem recebem o mesmo filtro de visibilidade antes da paginação.
- `@ResourceVisibility` deve ser aplicado onde o mecanismo precisa filtrar ou autorizar a referência; não é necessário replicá-lo em métodos auxiliares que não são pontos de acesso ao recurso.

### 7. API e fluxo

- Rotas e nomes refletem a relação de domínio e deixam claro se a operação lê uma entidade, lista uma associação ou executa uma ação.
- Para o fluxo completo, verificar ordem das chamadas, dados necessários em cada etapa, possibilidade de repetição e estados retornados.
- Requests e responses são específicos ao contrato quando isso evita campos ambíguos ou alterações acidentais; não devolver entidades ou modelos internos.
- Requisitos condicionais do body são explícitos: por exemplo, campos obrigatórios em uma ação e ausentes em outra devem ser validados de modo inequívoco.
- Paginação segue os tipos e defaults do Spring Data; evitar paginação manual paralela sem necessidade comprovada.
- Status HTTP e corpo de resposta correspondem à operação: ações sem representação podem retornar `204`; leituras e criações retornam a representação e status adequados.
- Evitar endpoints de consulta que repetem informação já disponível no detalhe, salvo quando há necessidade de paginação, seleção independente ou custo de leitura distinto.

### 8. Desempenho e uso de recursos

- Para cada método, contar consultas e chamadas a módulos externos; remover leituras redundantes e buscar dados em lote quando o fluxo naturalmente os exige.
- Carregar apenas o necessário para executar a regra e construir a resposta. Preferir projeção em listagens e associação lazy em detalhes não solicitados.
- Evitar processamento N+1 e materialização integral de listas quando há paginação ou contagem disponível.
- Avaliar custo de locks e transações; manter transações com escopo suficiente para atomicidade, sem abranger trabalho externo lento sem necessidade.
- Cache só é adotado quando há semântica de invalidação clara e evidência de benefício.

### 9. Auditoria e histórico

- Operações auditáveis incluem ator, recurso, ação e contexto de correlação a partir da sessão/request confiável.
- Alterações relevantes preservam versão e histórico exigidos pelo produto; retries idempotentes não criam eventos duplicados indevidos.
- Um output de comando também pode servir de snapshot de auditoria mesmo quando o HTTP retorna `204`. Em `PURGE`, buscar pelo identificador, validar, preservar o output antes da remoção, remover/flush e retornar o objeto removido. O aspect captura esse retorno; métodos `PURGE` não podem retornar `void` ou snapshot nulo.
- Eventos ou marcos persistidos devem ser gravados na mesma transação da mudança que representam, quando a consistência exige atomicidade.
- Confirmar rollback integral se uma etapa obrigatória falhar. Não capturar exceção e continuar com uma transação potencialmente inválida.

### 10. Testes e verificação

- Testes unitários cobrem regras puras, transições e validações com casos válidos, inválidos e limites significativos.
- Testes de integração verificam API, autorização, persistência real, migrations, constraints e interações entre módulos.
- Cenários concorrentes/idempotentes são testados quando o fluxo depende deles; não inferir segurança concorrente apenas da implementação.
- Testes cobrem owner/autorizado/não autorizado, entidades ausentes/inativas, payload estrutural inválido e conflito de versão, conforme aplicável.
- Rodar primeiro compilação/arquitetura e testes focados; depois os testes de regressão afetados e a verificação completa definida pelo projeto.
- Registrar qualquer teste não executado e o motivo (ambiente, dependência externa ou escopo); distinguir claramente validação local de pipeline.
- Não adicionar testes que apenas espelham uma linha de implementação; cada teste deve proteger comportamento ou regressão relevante.

## Registro dos achados

Agrupar observações por classe e método. Para cada achado, registrar:

| Campo | Conteúdo |
| --- | --- |
| Local | Classe e método, ou migration/rota afetada |
| Severidade | Bloqueador, alta, média ou baixa |
| Evidência | Comportamento observado ou cenário que reproduz |
| Impacto | Regra, segurança, integridade, desempenho ou manutenção afetada |
| Ajuste | Correção proposta ou aplicada |
| Verificação | Teste/comando executado e resultado |

Não registrar preferência estilística como bug. Se houver mais de uma opção válida, documentar a decisão e o critério usado. Diferenciar explicitamente risco real, melhoria opcional e item dependente de decisão de produto.

## Critério de conclusão

Uma revisão pode ser concluída quando não restarem achados bloqueadores ou de alta severidade sem encaminhamento, as regras críticas estiverem cobertas por validação e integridade, os testes relevantes estiverem verdes, e as limitações ou etapas futuras estiverem registradas com clareza. Uma implementação deliberadamente incompleta deve declarar quais operações ainda não podem ser concluídas e o que as habilitará.
