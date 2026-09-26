# Microservice e remoção de catálogos — V19

## Modelo

`Microservice` é a entidade administrativa que representa um microserviço
implantável. `Feature` pertence a um `Microservice`; `FeatureContext` continua
associado a `Feature`. `Message` usa o identificador interno de `Microservice`
como referência, sem importar a entidade de domínio de Platform.

```text
Microservice 1 --- N Feature
Microservice 1 --- N Message
Feature N --- N FeatureContext
```

Os códigos, como `workspace-service` e `audit-service`, continuam a identificar
o microserviço. A mudança de nome do modelo não altera esses valores.

## Persistência e migração

A migração `V19__microservice_and_retire_unused_catalogs.sql` renomeia
`platform_services` para `platform_microservices` e as colunas de vínculo
`service_id` para `microservice_id` em `platform_features` e `messages`. Recria
as chaves estrangeiras, índices, verificações e as views afetadas. A view
`vw_platform_messages` mantém as colunas consumidas pela Library; a view
`vw_feature_runtime_config` passa a expor `microservice_code`,
`microservice_label` e `microservice_active`.

As tabelas `type_languages` e `type_infrastructures` são removidas. Não há
colunas com chaves estrangeiras para esses catálogos no esquema atual. Uma
futura implementação de Application poderá definir linguagem e infraestrutura
em seus settings, com regras próprias.

Mesmo em um banco novo, o Flyway executa V1 a V18 antes da V19; por isso V19
transforma a estrutura criada por essas versões. Não há dados produtivos a
migrar neste projeto. As migrações históricas permanecem intactas para que a
sequência de criação do banco continue verificável.

## Contratos HTTP

- Cadastro: `/api/v1/platform/microservices` substitui `/api/v1/platform/services`.
- Feature: campos `microserviceIdentifier` e `microserviceCode` substituem os
  campos `serviceIdentifier` e `serviceCode`.
- Message: campo e filtro `microserviceIdentifier` substituem
  `serviceIdentifier`.
- Endpoints `/api/v1/language-type` e `/api/v1/infrastructure-type` deixam
  de existir.

Os clientes desses endpoints e da view de configuração precisam seguir esses
novos nomes na mesma implantação. Os valores dos códigos e identificadores
persistidos continuam os mesmos.
