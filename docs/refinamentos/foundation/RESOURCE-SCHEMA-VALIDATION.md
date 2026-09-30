# Resource Schema Validation

## Objetivo

Padronizar a validação estrutural dos recursos da Golden Reference por JSON Schema persistido e publicado, preservando o payload exatamente como foi recebido na borda HTTP.

A decisão principal desta evolução é validar o **JSON bruto da requisição antes da conversão para Request/Input**. Isso preserva a diferença entre:

- campo ausente;
- campo informado com valor válido;
- campo informado explicitamente como `null`.

Essa distinção se perde quando o payload é convertido primeiro para um DTO Java simples, porque campo ausente e campo `null` podem resultar no mesmo valor Java.

## Fluxo Golden

```text
HTTP JSON bruto
    ↓
ResourceSchemaValidator
    ↓
SchemaResolutionPort
    ↓
schema_configuration
    ↓
Schema ativo + última versão PUBLISHED
    ↓
validação estrutural
    ↓
Request DTO
    ↓
Input normalizado
    ↓
Validator contextual / de negócio
    ↓
Use Case
    ↓
Persistência
```

## Responsabilidades

### JSON Schema

O JSON Schema é responsável por regras puramente estruturais do contrato:

- campos obrigatórios (`required`);
- tipo (`string`, `object`, `array`, `integer`, etc.);
- rejeição de `null` quando o schema não declara `null` como tipo aceito;
- `minLength` / `maxLength`;
- `minimum` / `maximum`;
- regex / `pattern`;
- `enum` estrutural;
- `uniqueItems`;
- estrutura de arrays e objetos;
- `additionalProperties`.

### Java / Use Case

Java permanece responsável por regras contextuais e de integridade:

- existência de recurso relacionado;
- estado/lifecycle do recurso relacionado;
- catálogo existente e ativo;
- duplicidade dependente de persistência;
- autorização contextual;
- versionamento otimista;
- regras que dependem de mais de um agregado ou consulta.

## Ausente x null

Para um campo opcional declarado assim:

```json
{
  "authorizerGroup": {
    "type": "string",
    "minLength": 1
  }
}
```

o comportamento esperado é:

```json
{}
```

Válido: o campo é opcional e não foi informado.

```json
{
  "authorizerGroup": "A-TEAM"
}
```

Válido: o valor atende ao contrato.

```json
{
  "authorizerGroup": null
}
```

Inválido: `null` foi informado explicitamente, mas o tipo aceito é somente `string`.

## Application como Golden Reference inicial

Binding atual:

```text
resourceType = APPLICATION
resourceCode = application
schema       = application-resource
version      = 1
status       = PUBLISHED
```

A migration `V34__application_resource_schema.sql` publica o contrato estrutural da Application e cria seu vínculo em `schema_configuration`.

O controller valida o `JsonNode` recebido antes de convertê-lo para `CreateApplicationRequest` ou `UpdateApplicationRequest`.

Depois da validação estrutural, o fluxo segue normalmente para Request → Input → normalização → regras contextuais.

## Implementação compartilhada

O ponto comum é:

```java
resourceSchemaValidator.validate(
    resourceType,
    resourceCode,
    payload
);
```

O `ResourceSchemaValidator`:

1. resolve o schema publicado para o par `resourceType/resourceCode`;
2. valida o `JsonNode` original;
3. acumula os erros em `ValidationResult`;
4. lança `ValidationException` quando houver falha.

Ele não conhece Application, Workspace, Environment ou qualquer outro domínio.

## Decisões removidas pela evolução

A implementação anterior reconstruía um payload estrutural a partir do Input por meio de:

- `ResourceValidationData`;
- `ApplicationValidationData`;
- reflection sobre `record`.

Essas abstrações foram removidas porque o JSON original já é a fonte mais fiel do contrato HTTP e preserva a presença dos campos.

## Normalização

A validação estrutural acontece **antes da normalização**.

Consequência: valores aceitos pelo schema precisam estar no formato efetivamente contratado pela API. Por exemplo, se o schema declara:

```json
"applicationScope": {
  "type": "string",
  "enum": ["BACKEND", "FRONTEND", "SHARED"]
}
```

então `"BACKEND"` é válido e `" backend "` é inválido, mesmo que uma camada Java fosse capaz de trimar/normalizar depois.

Isso é intencional: o schema representa o contrato externo.

## Testes mínimos obrigatórios

Cada recurso que adotar esse padrão deve possuir cobertura para:

- required ausente;
- opcional ausente;
- opcional informado com `null`;
- tipo inválido;
- propriedade não permitida quando `additionalProperties=false`;
- limites de string/número;
- regex;
- arrays e `uniqueItems`;
- regra contextual que continua em Java;
- resolução do schema específico e fallback quando aplicável.

## Adapter HTTP declarativo

A validação HTTP é declarada no método do controller com `@ValidateResourceSchema`.

O `ResourceSchemaRequestBodyAdvice`, localizado no entrypoint web, lê o body bruto antes da desserialização, valida o `JsonNode` pelo `ResourceSchemaValidator` e devolve os mesmos bytes ao `HttpMessageConverter`. Assim o controller continua recebendo seu Request DTO normal e a distinção entre campo ausente e `null` explícito é preservada.

O conceito não fica preso ao REST. Um consumer de evento, batch ou mensageria pode usar o mesmo `ResourceSchemaValidator` desde que forneça um `JsonNode` representando o payload recebido.
