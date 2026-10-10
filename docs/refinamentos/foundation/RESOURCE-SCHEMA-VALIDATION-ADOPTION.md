# Resource Schema Validation — Adoption Guide

## Objetivo

Checklist para aplicar o padrão Golden de validação estrutural por JSON Schema em novos recursos sem duplicar regras entre controller, use case e domínio.

## Passo a passo

### 1. Definir o binding do recurso

Escolher explicitamente:

```text
resourceType
resourceCode
```

Exemplos:

```text
APPLICATION / application
WORKSPACE   / workspace
ENVIRONMENT / environment
PUBLISHER   / publisher
```

Os valores devem seguir a convenção do domínio e são resolvidos exatamente como informados.

### 2. Criar ou reutilizar a definição de Schema

Cadastrar o Schema independente do recurso, versionar sua definição e publicar uma versão.

O Schema deve representar o contrato HTTP estrutural completo do recurso.

### 3. Criar SchemaConfiguration

Associar:

```text
(resourceType, resourceCode) → schema_id
```

O vínculo deve estar ativo e o Schema deve possuir versão PUBLISHED.

### 4. Declarar a validação no endpoint

O serviço consumidor não manipula `JsonNode` nem chama o validator diretamente no controller. O binding é declarado pela annotation fornecida por `platform-schema-validation`:

```java
@ValidateResourceSchema(
    type = "APPLICATION",
    code = "application"
)
@PostMapping
public ResponseEntity<ApplicationResponse> create(
        @RequestBody CreateApplicationRequest request
) {
    ...
}
```

O `ResourceSchemaRequestBodyAdvice` intercepta o body bruto antes da desserialização, valida o JSON original e repõe os mesmos bytes para o `HttpMessageConverter`.

### 5. Manter o Request DTO como contrato Java

Depois da validação estrutural, o fluxo permanece:

```text
HTTP JSON bruto
  ↓
platform-schema-validation
  ↓
Request
  ↓
Input
```

O serviço não deve criar adapter HTTP local, validator estrutural local ou reconstruir o payload a partir do DTO.

### 6. Normalizar no ponto já definido pelo domínio

Trim, canonicalização, prefixos e transformações internas continuam fora do Schema quando forem regras internas.

Atenção: a normalização não deve ser usada para tornar válido um payload que viola o contrato externo declarado no JSON Schema.

### 7. Manter no Validator Java apenas regras contextuais

Exemplos de regras que permanecem no Java:

```text
workspace existe?
workspace permite aplicações?
applicationScope existe e está ativo?
nome já existe no workspace?
version recebida corresponde à atual?
usuário possui acesso ao recurso?
```

Não duplicar em Java regras já cobertas pelo JSON Schema.

### 8. Testar o contrato

Cobertura mínima recomendada:

| Cenário                                      | Resultado esperado      |
| -------------------------------------------- | ----------------------- |
| required ausente                             | 400                     |
| opcional ausente                             | válido                  |
| opcional = null sem null no schema           | 400                     |
| tipo inválido                                | 400                     |
| propriedade extra não permitida              | 400                     |
| enum estrutural inválido                     | 400                     |
| pattern inválido                             | 400                     |
| array duplicado com uniqueItems              | 400                     |
| estrutura válida + regra contextual inválida | 400 pelo Validator Java |
| estrutura + contexto válidos                 | sucesso                 |

## Separação de responsabilidade

```text
Schema                  Java
----------------------  --------------------------------
required                relacionamento
type                    lifecycle/estado
nullability             existência no banco
min/max                 duplicidade
pattern                 autorização contextual
array/object structure  versionamento
additionalProperties    integridade entre recursos
```

## Convenção de erro

Erros estruturais devem continuar usando o contrato de validação global:

```text
HTTP 400
GLOBAL-0001
details[].field
```

O caminho do campo deve representar o payload HTTP:

```text
applicationScope
tags
tags[0]
settings
```

e não caminhos artificiais como:

```text
.applicationScope
.tags
```

## Annotation declarativa implementada

O padrão HTTP usa annotation declarativa no controller:

```java
@ValidateResourceSchema(
    type = "APPLICATION",
    code = "application"
)
@PostMapping
public ResponseEntity<ApplicationResponse> create(
        @RequestBody CreateApplicationRequest request
) {
    ...
}
```

A infraestrutura web:

1. detecta a annotation;
2. obtém o body bruto em `RequestBodyAdvice.beforeBodyRead`;
3. resolve `resourceType/resourceCode`;
4. valida pelo `ResourceSchemaValidator`;
5. repõe os mesmos bytes para a desserialização normal do Request DTO;
6. somente então permite a execução do controller.

A annotation declara apenas metadados. A resolução, o adapter HTTP e a validação estrutural ficam no módulo compartilhado `platform-schema-validation`. O serviço consumidor fornece a configuração de datasource/view e a view `vw_platform_resource_schemas`; o domínio continua responsável pelas regras contextuais.

## Critérios para considerar um recurso migrado

Um recurso está aderente quando:

- possui binding documentado;
- possui Schema ativo e versão PUBLISHED;
- possui SchemaConfiguration;
- declara `@ValidateResourceSchema` no endpoint e valida o JSON bruto antes de Request/Input;
- não reconstrói payload estrutural via reflection;
- distingue ausente de `null`;
- não duplica regra estrutural no Validator Java;
- possui cobertura dos cenários mínimos;
- pipeline completa está green.
