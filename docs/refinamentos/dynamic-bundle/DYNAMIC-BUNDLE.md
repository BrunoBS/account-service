# Dynamic Bundle

Capacidade de mensagens dinâmicas. `Message` define a chave, código e status HTTP; `MessageTranslator` contém traduções por locale. Não existe agregado artificial Bundle. Cada camada possui `message` e `messagetranslator`; helpers que coordenam a composição pertencem a `message`.

API: `/api/v1/dynamic-bundles/messages`, com traduções em `/{messageIdentifier}/translators`. Criação composta recebe `translators`. A semântica de autorização, lifecycle, versionamento, composição e auditoria foi preservada. Identificadores de traduções usam `translatorIdentifier`.

Persistência: `messages` e `message_translators`, relação obrigatória N:1 e unicidade por mensagem/locale. V77 renomeia a tabela, constraints e índices preservando dados e cascata; publica versões novas dos JSON Schemas, preservando versões históricas. A view de consumo é `vw_dynamic_bundle_messages`, configurada em `application.yml`; suas colunas permanecem compatíveis com a biblioteca de messaging. Não há rota ou view legada.

Schemas atuais: `message.json` e `message-translator.json`; binding MESSAGE/message-translator. Testes de migrations anteriores usam cópias históricas em `src/test/resources/migration/message`.

Execução e testes: [checkpoint das ondas](../features-golden/EXECUCAO.md).
