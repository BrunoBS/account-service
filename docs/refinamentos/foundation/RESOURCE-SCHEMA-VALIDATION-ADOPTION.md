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

### 4. Validar o payload bruto no entrypoint

Padrão atual:

```java
@PostMapping
public ResponseEntity<?> create(
        @RequestBody JsonNode payload
) {
    resourceSchemaValidator.validate(
        "APPLICATION",
        "application",
        payload
    );

    CreateApplicationRequest request =
            objectMapper.treeToValue(
                    payload,
                    CreateApplicationRequest.class
            );

    ...
}
```

A validação precisa ocorrer antes da conversão para Request/Input.

### 5. Converter para Request somente após o schema

Depois da validação estrutural:

```text
JsonNode
  ↓
Request
  ↓
Input
```

O Request continua sendo o contrato Java do entrypoint e o Input continua sendo o contrato do Use Case.

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

| Cenário | Resultado esperado |
| --- | --- |
| required ausente | 400 |
| opcional ausente | válido |
| opcional = null sem null no schema | 400 |
| tipo inválido | 400 |
| propriedade extra não permitida | 400 |
| enum estrutural inválido | 400 |
| pattern inválido | 400 |
| array duplicado com uniqueItems | 400 |
| estrutura válida + regra contextual inválida | 400 pelo Validator Java |
| estrutura + contexto válidos | sucesso |

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

## Próxima evolução: annotation declarativa

O padrão atual ainda chama o validator explicitamente no controller.

Objetivo da próxima evolução:

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

A infraestrutura web deverá:

1. detectar a annotation;
2. obter o body bruto antes da desserialização final;
3. resolver `resourceType/resourceCode`;
4. validar pelo `ResourceSchemaValidator`;
5. somente então permitir a execução do controller.

A annotation deve apenas declarar metadados. A regra de resolução e validação continua concentrada no Foundation/Schema.

## Critérios para considerar um recurso migrado

Um recurso está aderente quando:

- possui binding documentado;
- possui Schema ativo e versão PUBLISHED;
- possui SchemaConfiguration;
- valida o JSON bruto antes de Request/Input;
- não reconstrói payload estrutural via reflection;
- distingue ausente de `null`;
- não duplica regra estrutural no Validator Java;
- possui cobertura dos cenários mínimos;
- pipeline completa está green.
