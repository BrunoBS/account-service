# Índice da documentação

## Documentação de uso atual

- [README do serviço](../README.md): visão geral, arquitetura vigente, APIs e regras
  importantes por feature.
- [Foundation Usage](golden/FOUNDATION-USAGE.md): dependências e integração atual com
  as libraries da plataforma.
- [Convenções de API](golden/API-CONVENTIONS.md): formato e práticas dos contratos HTTP.
- [Padrão de revisão](REVIEW-STANDARD.md): critérios usados nas revisões de código.
- Guias de feature vigentes: [Dynamic Bundle](refinamentos/dynamic-bundle/DYNAMIC-BUNDLE.md),
  [Parameter](refinamentos/dynamic-parameter/DYNAMIC-PARAMETER.md),
  [Segmentation](refinamentos/segmentation/SEGMENTATION.md),
  [Service Context](refinamentos/service-context/SERVICE-CONTEXT.md),
  [Sharing](refinamentos/resource-sharing/RESOURCE-SHARING.md) e
  [Onboarding](refinamentos/onboarding/ONBOARDING.md).
- [Relatório consolidado da refatoração Golden](refinamentos/features-golden/EXECUCAO.md):
  ondas, migrations, checkpoints e validação final.

## Decisões arquiteturais

Os documentos `ADR-*` em [golden](golden/) registram decisões e mudanças ao longo do
tempo. Quando um ADR tiver uma nota de substituição ou atualização, siga a seção de
estado atual e o README do serviço para a estrutura vigente.

## Registros históricos

Os diretórios em `refinamentos/` também contêm análises, checkpoints de pausa,
refinamentos de implementação e revisões de versões anteriores. Esses arquivos são
evidência histórica: nomes de packages, rotas, contagens de testes e propostas neles
podem descrever o estado da data em que foram produzidos. Para saber se um contrato
continua vigente, use primeiro o README e o guia atual da feature correspondente.

Não altere documentos de refinamento históricos apenas para substituir nomes antigos;
preserve o contexto e corrija-os somente quando forem referenciados como instrução
vigente ou quando contiverem links quebrados que impeçam a navegação.
