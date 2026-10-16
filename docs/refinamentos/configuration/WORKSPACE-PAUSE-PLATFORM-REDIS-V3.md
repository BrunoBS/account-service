# Checkpoint — pausa de Configuration para platform-redis V3

> Registro histórico anterior à renomeação. O contrato atual é [DynamicParameter / ParameterKey](../dynamic-parameter/DYNAMIC-PARAMETER.md).

Data: 2026-10-14T23:41:01.195445-03:00

Repositório: Workspace/Account Service. Branch: `feature/segments-categories-v1`. HEAD: `e138e092d0f2ab0fb699e4154d1784af905d2933`.

Status: **Configuration pausada por solicitação explícita do usuário**. Todas as implementações e alterações locais foram preservadas. Este checkpoint é documental: não houve stash, reset, descarte, commit, push ou merge.

## Concluído

- Segmentos/categorias V1 e revisão de persistência saveAndFlush, preservadas na mesma branch.
- Configuration checkpoints 1 e 2: contratos, JSON Schema único, CRUD, catálogos, autorização, auditoria, JSON MySQL, chave minúscula e DECIMAL multipleOf 0.01.
- Configuration checkpoint 3: publicação individual/em lote, transação e auditoria atômicas no banco, sincronização Redis após commit, lease e reconciliação; consultas com interfaces de projeção e mapeamento nos QueryServices.
- Última regressão: BUILD SUCCESS, 541 testes (125 unitários/arquiteturais e 416 integrações), zero falhas/erros/skips. Log: /private/tmp/configuration-full.log. Evidências detalhadas em [checkpoint 3](CONFIGURATION-CHECKPOINT-3.md).

## Pendências e próximos passos

- Configuration checkpoint 4 (biblioteca de consumo) e checkpoint 5 (verificação entre repositórios) permanecem pendentes. Não alterar o produtor nem migrar o serviço consumidor nesta tarefa.
- Próximo trabalho autorizado: baixar e ler integralmente platform-redis-refinamento_V3.md, executar Onda 0 e implementar todas as ondas exclusivamente em BrunoBS/platform-libraries, com builds/testes por onda e Redis real via Testcontainers.
- Não publicar artefatos nem realizar merge automaticamente. Decisões futuras de integração de Configuration deverão ser retomadas separadamente, após a entrega do módulo.

## Inventário local preservado no início da pausa

Inventário dos arquivos modificados/não rastreados, incluindo artefatos locais já existentes; não representa inclusão em commit:

```text
docs/refinamentos/configuration/CONFIGURATION-CHECKPOINT-1.md
docs/refinamentos/configuration/CONFIGURATION-CHECKPOINT-2.md
docs/refinamentos/configuration/CONFIGURATION-CHECKPOINT-3.md
docs/refinamentos/persistence/SAVE-AND-FLUSH-REVIEW.md
docs/refinamentos/segment/SEGMENT-CATEGORIES-V1.md
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/catalog/configurationstatustype/ConfigurationStatusTypeController.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/catalog/configurationtype/ConfigurationTypeController.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/ConfigurationController.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/request/CreateConfigurationRequest.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/request/PublishConfigurationsRequest.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/request/UpdateConfigurationRequest.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/response/ConfigurationMetadataResponse.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/response/ConfigurationPublishBatchResponse.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/response/ConfigurationPublishItemResponse.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/response/ConfigurationPublishResponse.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/response/ConfigurationResponse.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/segment/category/SegmentCategoryController.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/segment/category/request/CreateSegmentCategoryRequest.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/segment/category/request/UpdateSegmentCategoryRequest.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/segment/category/response/SegmentCategoryResponse.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/segment/segment/SegmentController.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/segment/segment/request/CreateSegmentRequest.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/segment/segment/request/UpdateSegmentRequest.java
src/main/java/br/com/portalmanager/platform/workspace/entrypoint/web/segment/segment/response/SegmentResponse.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/domain/Configuration.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/domain/ConfigurationMessageKeys.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/domain/Operator.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/domain/ValueType.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/facade/ConfigurationFacade.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/integration/ConfigurationMicroserviceAdapter.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/integration/ConfigurationMicroservicePort.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/integration/ConfigurationReconciliationScheduler.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/integration/ConfigurationRedisAdapter.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/integration/ConfigurationRuntimePort.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/integration/ConfigurationRuntimeProperties.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/integration/ConfigurationRuntimeSettings.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/repository/ConfigurationJsonConverter.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/repository/ConfigurationRepository.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/repository/projection/ConfigurationPublishedKeyProjection.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/repository/projection/ConfigurationReadProjection.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/repository/projection/ConfigurationRuntimeProjection.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/ConfigurationMetadata.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/ConfigurationOutput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/ConfigurationPublicationItem.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/ConfigurationPublicationOutput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/ConfigurationPublishedKeysPage.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/ConfigurationReconciliationOutput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/ConfigurationRuntimeSnapshot.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/ConfigurationSynchronizationStatus.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/CreateConfigurationInput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/PublishConfigurationsInput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/model/UpdateConfigurationInput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationAfterCommitSynchronization.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationFinder.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationLifecycleOperations.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationNormalizer.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationPublicationOperations.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationPublicationService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationQueryService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationReconciliationService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationRuntimeQueryService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/operations/ConfigurationSynchronizationService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/validation/ConfigurationQueryValidation.java
src/main/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/validation/ConfigurationValidator.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/repository/microservice/projection/MicroserviceReferenceProjection.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/usecase/model/microservice/MicroserviceReference.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/domain/SegmentMessageKeys.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/domain/category/SegmentCategory.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/domain/segment/Segment.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/facade/category/SegmentCategoryFacade.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/facade/segment/SegmentFacade.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/repository/category/SegmentCategoryRepository.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/repository/segment/SegmentRepository.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/model/category/CreateSegmentCategoryInput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/model/category/SegmentCategoryOutput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/model/category/UpdateSegmentCategoryInput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/model/segment/CreateSegmentInput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/model/segment/SegmentOutput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/model/segment/UpdateSegmentInput.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/operations/SegmentNormalizer.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/operations/category/SegmentCategoryCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/operations/category/SegmentCategoryFinder.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/operations/category/SegmentCategoryQueryService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/operations/segment/SegmentCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/operations/segment/SegmentFinder.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/operations/segment/SegmentQueryService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/validation/SegmentQueryValidation.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/validation/category/SegmentCategoryValidator.java
src/main/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/validation/segment/SegmentValidator.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationstatustype/domain/ConfigurationStatusType.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationstatustype/domain/ConfigurationStatusTypeCode.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationstatustype/domain/ConfigurationStatusTypeEnum.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationstatustype/facade/ConfigurationStatusTypeFacade.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationstatustype/repository/ConfigurationStatusTypeRepository.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationstatustype/usecase/ConfigurationStatusTypeService.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationtype/domain/ConfigurationType.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationtype/domain/ConfigurationTypeCode.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationtype/domain/ConfigurationTypeEnum.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationtype/facade/ConfigurationTypeFacade.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationtype/repository/ConfigurationTypeRepository.java
src/main/java/br/com/portalmanager/platform/workspace/foundation/catalog/configurationtype/usecase/ConfigurationTypeService.java
src/main/resources/configuration/redis/publish.lua
src/main/resources/configuration/redis/release.lua
src/main/resources/configuration/redis/remove.lua
src/main/resources/db/migration/V66__create_segments_and_categories.sql
src/main/resources/db/migration/V67__segment_resource_schemas.sql
src/main/resources/db/migration/V68__configuration_catalogs_and_platform_microservice.sql
src/main/resources/db/migration/V69__create_configurations.sql
src/main/resources/db/migration/V70__configuration_resource_schemas.sql
src/main/resources/db/migration/V71__unify_configuration_resource_schema.sql
src/main/resources/db/migration/V72__configuration_decimal_multiple_of_schema.sql
src/main/resources/db/migration/V73__configuration_lowercase_key_schema.sql
src/main/resources/db/migration/V74__configuration_publication_schema.sql
src/main/resources/schemas/configuration-publication.json
src/main/resources/schemas/configuration-status-type-settings.json
src/main/resources/schemas/configuration-type-settings.json
src/main/resources/schemas/configuration.json
src/main/resources/schemas/segment-category.json
src/main/resources/schemas/segment.json
src/test/java/br/com/portalmanager/platform/workspace/ConfigurationArchitectureTest.java
src/test/java/br/com/portalmanager/platform/workspace/SegmentArchitectureTest.java
src/test/java/br/com/portalmanager/platform/workspace/entrypoint/web/configuration/ConfigurationApiIT.java
src/test/java/br/com/portalmanager/platform/workspace/entrypoint/web/segment/SegmentApiIT.java
src/test/java/br/com/portalmanager/platform/workspace/feature/configuration/domain/ConfigurationDomainTest.java
src/test/java/br/com/portalmanager/platform/workspace/feature/configuration/integration/ConfigurationRedisIT.java
src/test/java/br/com/portalmanager/platform/workspace/feature/configuration/integration/ConfigurationRuntimeSettingsTest.java
src/test/java/br/com/portalmanager/platform/workspace/feature/configuration/repository/ConfigurationMigrationIT.java
src/test/java/br/com/portalmanager/platform/workspace/feature/configuration/support/ConfigurationRedisTestSupport.java
src/test/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/ConfigurationConcurrencyIT.java
src/test/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/ConfigurationPublicationIT.java
src/test/java/br/com/portalmanager/platform/workspace/feature/configuration/usecase/ConfigurationReconciliationIT.java
src/test/java/br/com/portalmanager/platform/workspace/feature/segment/domain/SegmentDomainTest.java
src/test/java/br/com/portalmanager/platform/workspace/feature/segment/repository/SegmentMigrationIT.java
src/test/java/br/com/portalmanager/platform/workspace/feature/segment/usecase/SegmentConcurrencyIT.java
target 2/classes/META-INF/build-info.properties
target 2/site/jacoco/jacoco-resources/branchfc.gif
target 2/site/jacoco/jacoco-resources/branchnc.gif
target 2/site/jacoco/jacoco-resources/branchpc.gif
target 2/site/jacoco/jacoco-resources/down.gif
target 2/site/jacoco/jacoco-resources/greenbar.gif
target 2/site/jacoco/jacoco-resources/method.gif
target 2/site/jacoco/jacoco-resources/package.gif
target 2/site/jacoco/jacoco-resources/prettify.css
target 2/site/jacoco/jacoco-resources/redbar.gif
target 2/site/jacoco/jacoco-resources/session.gif
target 2/site/jacoco/jacoco-resources/sort.gif
target 2/site/jacoco/jacoco-resources/up.gif
README.md
docs/REVIEW-STANDARD.md
pom.xml
src/main/java/br/com/portalmanager/platform/workspace/feature/message/usecase/operations/message/MessageCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/message/usecase/operations/translation/MessageTranslationCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/domain/PlatformMessageKeys.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/domain/microservice/Microservice.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/repository/microservice/MicroserviceRepository.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/usecase/operations/context/ContextCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/usecase/operations/feature/FeatureCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/usecase/operations/featurecontext/FeatureContextAssociationOperations.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/usecase/operations/featurecontext/FeatureContextCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/usecase/operations/microservice/MicroserviceCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/platform/usecase/operations/microservice/MicroserviceQueryService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/shared/usecase/operations/contract/SharedContractCommandService.java
src/main/java/br/com/portalmanager/platform/workspace/feature/shared/usecase/operations/participation/SharedParticipationCommandService.java
src/main/resources/META-INF/platform-messages/messages_en.properties
src/main/resources/META-INF/platform-messages/messages_pt_BR.properties
src/test/java/br/com/portalmanager/platform/workspace/feature/platform/usecase/PlatformCommandServiceTest.java
src/test/java/br/com/portalmanager/platform/workspace/support/SchemaDefaultFixture.java
```
