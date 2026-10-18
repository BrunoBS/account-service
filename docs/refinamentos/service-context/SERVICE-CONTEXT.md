# Service Context

A feature Service Context administra Microservice, Feature e FeatureGroup. A relação entre Feature e FeatureGroup é N:N e persiste em `feature_group_feature`; FeatureGroup persiste em `feature_group`.

A migration V81 renomeia `service_context_groups` para `feature_group`, `service_context_feature_groups` para `feature_group_feature` e `group_id` para `feature_group_id`, preservando registros, chaves e relacionamentos. As migrations anteriores permanecem imutáveis.

API: `/api/v1/service-contexts/microservices`, `/features` e `/feature-groups`; associação em `/feature-groups/{featureGroupIdentifier}/features/{featureIdentifier}`. A listagem de features usa `featureGroupIdentifier` como filtro.

Schema HTTP: `feature-group.json`, binding `SERVICE_CONTEXT/feature-group`, definição `feature-group-resource`. Versões publicadas anteriores são mantidas; V81 publica uma nova versão com o título Feature Group request. O escopo `PLATFORM` dos schemas permanece como ownership global.
