# Limites operacionais de ambientes

Este documento acompanha a [revisão completa](ENVIRONMENT-FULL-CODE-REVIEW.md) e distingue o contrato implementado das evoluções de manutenção e volume.

## Quarentena e referências históricas

DELETE de ambiente exige INACTIVE e ausência de filhos de qualquer lifecycle. A operação grava QUARANTINED e preserva identifier, nome, tipo, workspace e pai. Não há purge HTTP de ambiente nem rotina de retenção/remoção física de ambientes implementada neste serviço.

| Situação | Comportamento implementado |
| --- | --- |
| Filho ACTIVE ou INACTIVE | Impede DELETE do pai |
| Filho QUARANTINED | Também impede DELETE do pai; a FK permanece |
| Tipo ou compatibilidade usada por ambiente QUARANTINED | Purge rejeitado; não há estado sem uso |
| Reutilizar nome de ambiente QUARANTINED no mesmo grupo de irmãos | Rejeitado pela regra e pela constraint |
| Restaurar ambiente QUARANTINED pela rota restore | Rejeitado; restore aceita somente INACTIVE |

Excluir a folha não permite excluir o próximo nível. Para retirar toda uma hierarquia da navegação ativa, a operação disponível é inativar a raiz: preserva a história e oculta os descendentes nas consultas ativas. Isso não remove fisicamente os registros nem libera seus nomes ou referências.

A remoção física posterior depende de um fluxo específico de manutenção. Não se deve resolver a retenção removendo a FK, ignorando filhos em quarentena ou apagando registros em cascata.

### Requisitos para uma futura rotina de manutenção

| Etapa | Condição exigida |
| --- | --- |
| Seleção | Somente QUARANTINED após o prazo de retenção definido pela plataforma; prazo não é inventado por este módulo |
| Referências | Ausência de filhos e de referências externas, inclusive mapeamentos Shared; constraints continuam sendo a proteção final |
| Concorrência | Leitura atual sob lock, na ordem de locks do módulo; revalidar estado e referências após a espera |
| Auditoria | Snapshot anterior à remoção, ação PURGE e outbox na mesma transação; falha reverte a remoção |
| Hierarquia | Remover fisicamente folhas elegíveis antes dos pais; pai ainda INACTIVE precisa passar pelo DELETE de quarentena |
| Execução | Lotes limitados, autorização específica e retomada sem repetir uma remoção já confirmada |

Esses são requisitos de uma evolução futura, não passos executáveis por um endpoint atual. Nenhuma remoção física de ambientes foi introduzida nesta revisão.

## Listagens e tamanho da hierarquia

As listas atuais devolvem arrays completos; roots, children e tree também não são paginadas. O filtro de lifecycle e a visibilidade são aplicados, mas não existe um limite de quantidade ou tamanho de settings na resposta. A árvore carrega os nós visíveis do workspace e os globais para montar a hierarquia.

| Proteção existente | Limite da proteção |
| --- | --- |
| Locks por tipo, workspace e hierarquia | Protegem a integridade; operações relacionadas podem esperar, sem garantia de throughput |
| Consulta em lote de pais e projeções | Reduzem consultas repetidas; não limitam o número de registros |
| Catálogo HTTP limitado a quatro códigos | Tipos e pares são pequenos; a quantidade de instâncias de ambiente permanece ilimitada |
| Isolamento do workspace e visibilidade | Evitam exposição de outros workspaces e ancestrais ocultos; não substituem paginação |

### Evolução de contrato para volumes grandes

As listas gerais de ambientes devem receber paginação no padrão de aplicações/publicadores, com tamanho máximo, campos de ordenação permitidos e desempate estável. A elegibilidade dos ancestrais deve ser aplicada antes da contagem e da seleção da página; paginar primeiro e filtrar depois produz páginas e totais incorretos.

Para a hierarquia, a evolução deve privilegiar navegação por raízes e filhos com páginas limitadas. Uma árvore truncada não pode ser devolvida como se fosse completa; qualquer limite de nós exige indicação explícita de continuação ou rejeição definida no contrato.

A adoção de PagedModel substitui o array público e precisa de revisão dos consumidores e da simulação dos endpoints. Essa alteração de contrato não foi misturada ao fechamento das regras atuais. Não houve teste de carga, e a revisão funcional não certifica capacidade ilimitada.
