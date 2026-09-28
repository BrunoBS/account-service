# Schema Configuration (V28)

Schema de plataforma é uma definição versionada independente do recurso. O administrador
vincula a definição ao par único `(resourceType, resourceCode)` pelo endpoint
`/api/v1/schema-configurations` (OWNER). Um vínculo ativo e um Schema ativo com
versão publicada fornecem diretamente `SchemaVersion.definition`.

Sem vínculo específico, com vínculo inativo, Schema inativo ou sem versão
publicada, o resolver tenta o vínculo do **mesmo resourceType** com
`resourceCode = DEFAULT`. Esse código reservado não representa um recurso
real: é a convenção para o fallback administrável. O resolver devolve a
definição da última versão publicada do Schema associado; se o default
também não estiver utilizável, devolve erro controlado
`schema.default.not-found`. Os casos de configuração administrativa
incompleta são registrados no log. Settings inválidos continuam gerando
erro de validação. Falhas de infraestrutura propagam como falhas.

| Consumidor atual | resourceType | resourceCode | JSON validado |
| --- | --- | --- | --- |
| Application | APPLICATION | application | application.settings |
| Environment de Workspace | ENVIRONMENT | workspace | environment.settings |
| Feature de plataforma | FEATURE | feature.code | feature.settings |
| Catálogos | CATALOG | código canônico do catálogo | catalog.settings |
| Publisher | PUBLISHER | código canônico do tipo de Publisher | futuro settings da configuração de ambiente |

O cadastro `core.publisher` não possui settings. Sua criação não depende de
SchemaConfiguration. O consumidor futuro deve usar o código do **tipo** do Publisher,
nunca seu identifier. Environment de Application e Microservice ainda não persistem
settings consumidos pelo mecanismo nesta versão. Schema de Workspace conserva
resolução explícita por workspace e código, sem fallback de plataforma.

V28 copia vínculos de schemas de plataforma existentes, vincula o Schema
DEFAULT histórico a cada um dos sete resourceTypes com código reservado
`DEFAULT` e remove as tabelas antigas de SchemaType. Administradores
podem substituir cada default por um Schema diferente via CRUD. Quando schemas
possuem tipo legado fora das categorias reconhecidas, as definições permanecem
no banco sem binding automático; o administrador deve vinculá-las após avaliar
o recurso correto. Tipos antigos cadastrados sem Schema não geram binding.
Quando schemas
legados de um mesmo escopo e owner compartilham código e se distinguem apenas
pelo antigo SchemaType, V28 acrescenta um sufixo determinístico aos códigos
colidentes para preservar os registros. Aplicações que consultavam diretamente
esses códigos precisam atualizar as referências. Migrations anteriores não
são alteradas para que bases já migradas possam receber V28.
