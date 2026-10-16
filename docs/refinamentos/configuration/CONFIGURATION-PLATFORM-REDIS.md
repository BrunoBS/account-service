# Configuration — migração para platform-redis

> Registro histórico da migração. O fluxo de publicação, os contratos HTTP e o scheduler foram posteriormente simplificados; consulte [o contrato atual](../dynamic-parameter/DYNAMIC-PARAMETER.md). Lease, SCAN, resultados síncronos e as contagens de testes abaixo pertencem à versão anterior.

A feature usa exclusivamente os contratos públicos RedisStore e RedisOperations para transporte Redis. Publication, sincronização após commit, envelope type/value, leitura do estado publicado, lease e reconciliação continuam no domínio de Configuration.

## Separação de responsabilidades

| Local | Responsabilidade |
| --- | --- |
| platform-redis / RedisStore | JSON, namespace, NX, expiração e conexão |
| platform-redis / RedisAtomic | setIfValue, deleteIfValue e compareAndDelete atômicos, genéricos e independentes de features |
| platform-redis / RedisKeys | SCAN com prefixo literal, limitado ao namespace e sem carregar todas as chaves |
| ConfigurationRuntimeAdapter | Mapeia o envelope de publicação e o guard usado pela feature para os contratos da lib |
| ConfigurationSynchronizationService | Adquire lease, relê estado atual no banco, publica/remove e sinaliza SYNCED/PENDING/FAILED |
| ConfigurationAfterCommitSynchronization | Executa sincronização somente após commit, sem transação distribuída |
| ConfigurationReconciliationService | Restaura snapshots publicados e remove órfãos do namespace gerenciado |

A biblioteca não importa tipos do Workspace nem conhece Configuration, prefixo config, estados DRAFT/PUBLISHED, envelopes ou regras de publicação. APIs públicas não expõem templates, conexão ou driver. A implementação genérica permanece em internal. O teste de arquitetura da feature impede dependência direta de Spring Data Redis/Lettuce/Jedis; o da biblioteca impede dependência de domínios consumidores.

## Remoções e dependências

- ConfigurationRedisAdapter foi substituído por ConfigurationRuntimeAdapter, que apenas delega à lib e mantém as regras de domínio.
- Removidos os recursos publish.lua, remove.lua e release.lua da feature. Não há Lua, DefaultRedisScript, StringRedisTemplate, ScanOptions ou driver Redis no código de Configuration.
- POM consome platform-redis em produção e platform-testing-redis em test. O starter Redis direto foi substituído pela capability, que fornece a infraestrutura gerenciada.
- Testes usam @WithRedis e RedisStore. Removidos container estático, DynamicPropertySource, FLUSHDB e LettuceConnectionFactory próprios. Falha real de conexão é testada por contexto Boot configurado para porta indisponível e contratos da lib.

## Contratos preservados

O serviço configura platform.redis.namespace=config e namespace-mode=SHARED. Assim, a chave pública completa continua sendo <microservice.code>.<key> e a chave física continua config:<microservice.code>.<key>, sem prefixo automático do produtor. Namespace/serialização são aplicados exclusivamente pela lib. Em testes, @WithRedis fornece namespace exclusivo/ISOLATED e limpeza restrita.

O lease técnico da feature agora também pertence ao namespace da lib e armazena o token como JSON. Ele é excluído da reconciliação de valores de negócio. Chaves técnicas com separadores de infraestrutura, como cache:region:item, também são preservadas; a regra de identificação pertence à feature, não à lib. Não é parte do contrato público de consumo.

Guard e mutação permanecem atômicos: um processo que perdeu o lease não consegue publicar, apagar valor ou liberar o lease de outro dono. Não foi feita substituição por GET seguido de SET/DEL. Publicações continuam persistentes; o envelope preserva JSON null, tipos, coleções e snapshots anteriores em DRAFT. Falha Redis não desfaz o commit do banco. Não há cache local, fallback para banco ou eventos implícitos.

SCAN tem memória limitada por cursor/batches e pode repetir chaves, conforme Redis; sincronização é idempotente. count é uma dica de batch e deve respeitar platform.redis.max-read-size. Limites de chave/payload e erros passam a ser os da biblioteca. Em Cluster, as operações guardadas exigem guard e destino no mesmo hash slot; a convenção física V1 de Configuration foi preservada e continua sem suporte a publicação cross-slot.

## Validação

- Workspace: clean verify completo aprovado, **542 testes**, zero falhas/erros/skips/flakes. Log: /private/tmp/configuration-platform-redis-full.log.
- Verificação final após preservar chaves técnicas: **138 testes aprovados** (126 unitários/arquiteturais + 12 integrações de lease/conexão/reconciliação). Log: /private/tmp/configuration-platform-redis-final.log.
- platform-libraries: clean verify completo aprovado nos **23 módulos / 568 testes**, zero falhas/erros/skips/flakes. Inclui 54 testes do módulo Redis e 4 da fixture. Log: /private/tmp/configuration-redis-library-reactor.log.
- Instalação para consumo local, com testes, aprovada: /private/tmp/configuration-redis-library-install.log. Sem deploy/publicação remota.
- Namespace Java, git diff --check, ausência de driver/Lua no produtor e preservação dos fontes fora do escopo conferidos. As migrations e o schema de Configuration não foram alterados.

Os testes mantêm cobertura de publicação explícita, lote transacional/rollback, lease expirado e concorrência entre instâncias, releitura após inativação, JSON null, persistência sem TTL, indisponibilidade real e recuperação, logs sem payload e reconciliação sem apagar outros namespaces. A biblioteca adicionou testes genéricos de guard, expiração/substituição e SCAN literal/isolado, sem tipos de negócio.

Uma substituição mecânica durante adaptação dos testes alterou indevidamente o literal de chave inválida config:key no teste de migration. O caso foi restaurado; a implementação e as migrations do banco não foram alteradas por esta migração.

Sem commit, push, merge, deploy ou publicação remota. A instalação Maven foi somente local para validar o consumo. Alterações anteriores e demais features foram preservadas. CI remoto depende de envio posterior da branch. O módulo consumidor ConfigurationProvider do refinamento original é atividade separada e não foi incluído nesta troca de infraestrutura do produtor.
