# Segmentation

Cadastro global de Segment e Category, fora da hierarquia de Workspace/Account. Cada camada se organiza por `segment` e `category`. Category pertence obrigatoriamente a um Segment; códigos podem se repetir entre segmentos distintos. As regras de lifecycle, autorização, normalização e versionamento permanecem iguais.

API: `/api/v1/segmentation/segments`, com categorias em `/{segmentIdentifier}/categories`. O pai vem do path. Não há aliases legados.

V79 renomeia `segment_category` para `category`, índices e constraints, preservando FK obrigatória `segment_id` e unique `(segment_id, code)`. Essas restrições já existiam em V66; não há remoção silenciosa de registros. O teste de upgrade verifica órfãos/duplicidades antes da renomeação e compara todas as colunas depois. `segment` permanece com seu nome de domínio correto.

JSON Schemas: `segment.json`, `category.json`; bindings `SEGMENTATION/segment` e `SEGMENTATION/category`. Versões históricas publicadas são preservadas; Category recebe uma versão nova. O escopo global de schemas permanece `PLATFORM`.

[Resultados das ondas](../features-golden/EXECUCAO.md).
