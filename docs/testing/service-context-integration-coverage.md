# Service Context — cobertura dos testes de integração

## Situação

Foram criadas 22 classes `*IntegrationTest`, uma por operação HTTP do Service Context. A árvore atual contém 51 métodos `@Test` nesses arquivos. Os testes exercitam respostas e efeitos observáveis via HTTP usando clients, responses tipadas, factories/builders e scenarios.

| Área | Operações cobertas | Classes | Cenários contemplados |
|---|---|---:|---|
| Microservice | Create, FindByIdentifier, FindAll, Update, Activate, Inactivate, Purge | 7/7 | criação e validação, duplicidade/autorização, consulta e filtros, versão/conflito, lifecycle e purge |
| Feature | Create, FindByIdentifier, FindAll, Update, Activate, Inactivate | 6/6 | validação e duplicidade, dependências, consulta/filtros, versão e lifecycle |
| Feature Group | Create, FindByIdentifier, FindAll, Update, Activate, Inactivate, Purge | 7/7 | validação, duplicidade/autorização, associação/dependências, consulta, versão, lifecycle e purge |
| Feature Group Association | Associate, Remove | 2/2 | associação válida/idempotente, dados inválidos, autorização e remoção |
| **Total** | **22/22 operações** | **22** | **51 métodos `@Test` presentes no checkout** |

## Validação atual

O checkout atual foi validado pelo build completo, que compilou todas as fontes e executou testes unitários, de arquitetura e integração. Os relatórios registraram 864 testes no Surefire e 70 no Failsafe, totalizando **934 testes, 0 falhas, 0 erros e 0 ignorados**.

```bash
mvn --settings .github/maven-settings.xml --batch-mode --no-transfer-progress clean verify
```

Resultado: `BUILD SUCCESS`, duração total **31m13s**. A execução confirmou a descoberta dos testes legados e atuais. O JaCoCo produzido por esse comando combina dados de execução de unitários e integração; portanto, ele não é apresentado aqui como cobertura exclusiva de integração.

O valor JaCoCo de **87,20% (763/875 linhas)** registrado em uma versão anterior foi removido: ele veio de uma execução focada anterior às alterações atuais e não é uma medição válida deste checkout.

## Pendência de medição

- Reexecutar uma medição JaCoCo isolada para o Service Context no checkout atual, se for necessário reportar cobertura de linhas de produção exclusiva de integração. A cobertura HTTP por operação acima é uma contagem de operações, não cobertura de código.
