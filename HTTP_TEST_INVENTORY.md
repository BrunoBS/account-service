# HTTP e integração — inventário de refatoração

Baseline do checkout local em `main` antes desta refatoração. A varredura independente confirmou **36 classes** e **316 métodos `@Test`**: 32 classes HTTP em `entrypoint` e quatro testes de integração do parâmetro que chamam endpoints HTTP de dynamic parameters. O inventário não estava no checkout no início; o conteúdo fornecido na conversa contém 36 linhas de classe apesar da referência a 35.

Critério: inclui classes de teste que chamam controllers via RestAssured ou clients HTTP. Exclui persistência, Redis/concorrência puramente interna, testes de domínio e `WorkspaceServiceApplicationIT` (apenas valida inicialização do contexto).

| Classe | Casos antes | Baseline observada | Trabalho/status |
| --- | ---: | --- | --- |
| `br/com/portalmanager/platform/workspace/entrypoint/application/ApplicationApiIT.java` | 16 | RestAssured/helper local; payloads e/ou rotas no teste | Reescrito em 8 classes `entrypoint/application/integration`, com ApplicationClient/Response/Factory/Builder/Scenario; 31 testes HTTP validados |
| `br/com/portalmanager/platform/workspace/entrypoint/catalog/CatalogApiIT.java` | 6 | RestAssured/helper local; payloads e/ou rotas no teste | Migrado: CatalogClient, CatalogResponse, CatalogFactory, CatalogRequestBuilder, CatalogResource e CatalogScenario; 6/6 ITs validados |
| `br/com/portalmanager/platform/workspace/entrypoint/catalog/PublisherTypeApiIT.java` | 3 | RestAssured/helper local; payloads e/ou rotas no teste | Migrado: PublisherCatalogClient, CatalogResponse, CatalogFactory e CatalogScenario; 3/3 ITs validados |
| `br/com/portalmanager/platform/workspace/entrypoint/configuration/EnvironmentConfigurationApiIT.java` | 13 | Fixture com chamadas HTTP indiretas e helpers compartilhados | Revisar operações, responses e assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/configuration/EnvironmentConfigurationAuditIT.java` | 3 | Fixture com chamadas HTTP indiretas e helpers compartilhados | Revisar operações, responses e assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/configuration/EnvironmentConfigurationConcurrencyIT.java` | 3 | Fixture com chamadas HTTP indiretas e helpers compartilhados | Revisar operações, responses e assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/configuration/EnvironmentConfigurationCopyIT.java` | 5 | Fixture com chamadas HTTP indiretas e helpers compartilhados | Revisar operações, responses e assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/configuration/EnvironmentConfigurationPersistenceIT.java` | 4 | Fixture com chamadas HTTP indiretas e helpers compartilhados | Revisar operações, responses e assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/configuration/OnboardingConfigurationIT.java` | 2 | Fixture com chamadas HTTP indiretas e helpers compartilhados | Revisar operações, responses e assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/dynamicbundle/MessageApiIT.java` | 21 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/dynamicbundle/MessageAuditIT.java` | 5 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/environment/EnvironmentApiIT.java` | 32 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/environment/EnvironmentAuditIT.java` | 2 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/environment/EnvironmentTypeCompatibilityAuditIT.java` | 3 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/environment/EnvironmentTypePurgeIT.java` | 5 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/lifecycle/LifecycleListingIT.java` | 3 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/onboarding/OnboardingApiIT.java` | 10 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/parameter/ParameterKeyApiIT.java` | 19 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/schema/SchemaApiIT.java` | 7 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/schema/SchemaAuditIT.java` | 3 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/segmentation/SegmentApiIT.java` | 15 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/servicecontext/ServiceContextApiIT.java` | 2 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/servicecontext/ServiceContextEndpointContractIT.java` | 3 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/servicecontext/feature/FeatureApiIT.java` | 20 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/servicecontext/featuregroup/FeatureGroupApiIT.java` | 14 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/servicecontext/microservice/MicroserviceApiIT.java` | 15 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/sharing/ResourceSharingApiIT.java` | 22 | RestAssured/helper local; payloads e/ou rotas no teste | Criar client/response/factory por recurso e migrar assertions |
| `br/com/portalmanager/platform/workspace/entrypoint/workspace/WorkspaceApiIT.java` | 28 | Client parcial; IT ainda usa rotas e ValidatableResponse diretamente | Completar client, factory/scenario e migrar IT |
| `br/com/portalmanager/platform/workspace/entrypoint/workspace/WorkspaceAuditIT.java` | 1 | Client parcial; IT ainda usa rotas e ValidatableResponse diretamente | Completar client, factory/scenario e migrar IT |
| `br/com/portalmanager/platform/workspace/entrypoint/workspace/WorkspaceAuthorizationIT.java` | 10 | Client parcial; IT ainda usa rotas e ValidatableResponse diretamente | Completar client, factory/scenario e migrar IT |
| `br/com/portalmanager/platform/workspace/entrypoint/workspace/WorkspaceNameConcurrencyIT.java` | 4 | Client parcial; IT ainda usa rotas e ValidatableResponse diretamente | Completar client, factory/scenario e migrar IT |
| `br/com/portalmanager/platform/workspace/entrypoint/workspace/WorkspaceTaggingIT.java` | 4 | Client parcial; IT ainda usa rotas e ValidatableResponse diretamente | Completar client, factory/scenario e migrar IT |
| `br/com/portalmanager/platform/workspace/feature/parameter/integration/ParameterKeyRedisIT.java` | 3 | DynamicParameterClient expõe protocolRequest; testes enviam HTTP diretamente | Migrado: operações tipadas do DynamicParameterClient/Response; manter protocolo de publicação no client; aguardando IT final |
| `br/com/portalmanager/platform/workspace/feature/parameter/integration/ParameterPublicationInteropIT.java` | 2 | DynamicParameterClient expõe protocolRequest; testes enviam HTTP diretamente | Migrado: operações tipadas do DynamicParameterClient/Response; Scenario compartilhado; aguardando IT final |
| `br/com/portalmanager/platform/workspace/feature/parameter/usecase/ParameterKeyPublicationIT.java` | 5 | DynamicParameterClient expõe protocolRequest; testes enviam HTTP diretamente | Migrado: operações tipadas do DynamicParameterClient/Response; endpoints removidos encapsulados; aguardando IT final |
| `br/com/portalmanager/platform/workspace/feature/parameter/usecase/ParameterKeyReconciliationIT.java` | 3 | DynamicParameterClient expõe protocolRequest; testes enviam HTTP diretamente | Migrado: operações tipadas do DynamicParameterClient/Response; Scenario compartilhado; aguardando IT final |

## Contagem inicial

- Classes HTTP: 36.
- Casos `@Test`: 316.
- Chamadas de baixo nível permanecerão somente quando headers ausentes, JSON malformado ou contrato do envelope forem o comportamento validado; essas exceções serão listadas após a migração.
- Estado inicial: Workspace, Environment Configuration e parâmetros já tinham auxiliares; `BaseClient` isoladamente não conta como migração concluída.

## Atualização: Application

A classe antiga `ApplicationApiIT` foi substituída por `ApplicationCreateIntegrationTest`, `ApplicationFindByIdentifierIntegrationTest`, `ApplicationFindAllIntegrationTest`, `ApplicationSummaryIntegrationTest`, `ApplicationUpdateIntegrationTest`, `ApplicationInactivateIntegrationTest`, `ApplicationRestoreIntegrationTest` e `ApplicationDeleteIntegrationTest`. Os 31 testes HTTP cobrem operações, validações, autorização/visibilidade, lifecycle, tags, onboarding e concorrência. O suporte reutilizável fica em `entrypoint/application` e o client de onboarding em `entrypoint/onboarding/support`.

Validação direcionada executada com `mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress -Dtest=ApplicationDomainTest,ApplicationCreateIntegrationTest,ApplicationFindByIdentifierIntegrationTest,ApplicationFindAllIntegrationTest,ApplicationSummaryIntegrationTest,ApplicationUpdateIntegrationTest,ApplicationInactivateIntegrationTest,ApplicationRestoreIntegrationTest,ApplicationDeleteIntegrationTest -Dit.test=ApplicationPersistenceIT -Djacoco.destFile=target/jacoco-core-application.exec -Djacoco.dataFile=target/jacoco-core-application.exec -Djacoco.append=true org.jacoco:jacoco-maven-plugin:0.8.15:prepare-agent verify`: 33 testes Surefire e 2 testes Failsafe passaram; `BUILD SUCCESS`. JaCoCo em `core/application`: 81,9% linhas, 80,7% instruções, 64,6% branches, 79,6% métodos e 90,5% classes. As linhas não cobertas concentram-se em métodos de resolução de referências de Resource Sharing/integração com Application Configuration, fora dos endpoints de Application exercitados aqui; `ApplicationQueryService` tem 33/78 linhas cobertas nessa seleção.
