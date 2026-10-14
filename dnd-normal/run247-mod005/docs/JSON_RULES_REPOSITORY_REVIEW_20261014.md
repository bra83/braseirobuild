# Revisão de repositórios JSON de D&D — 14/10/2026

## Resultado útil para o fechamento

Os repositórios ajudam a acelerar comparação de esquemas e conferência do SRD 2024. Eles não substituem a fonte XMM já fixada nesta etapa: os dados de monstros XMM do aplicativo vêm do espelho local do 5e.tools no commit `8c026b807fac21862a309379a0c3228a0683198b`, conferidos contra os arquivos pinados.

## Candidatos conferidos

- [5etools-mirror-3/5etools-src](https://github.com/5etools-mirror-3/5etools-src): contém a estrutura de dados usada pelo projeto e permite comparar os registros XMM no mesmo esquema e no SHA já fixado. A licença MIT do código do site não é tratada aqui como licença separada para republicar o texto dos livros.
- [5e-bits/5e-database](https://github.com/5e-bits/5e-database): possui dados em `src/2024/en/` e teve registros 2024 de monstros adicionados. O repositório foi arquivado em 23/09/2026; sua página informa MIT para o projeto e OGL 1.0a para o material subjacente. Por isso, os registros não foram importados como se todos fossem SRD 5.2.1 sob CC-BY.
- [DangerBlack/dnd-5-2024-json](https://github.com/DangerBlack/dnd-5-2024-json): declara reunir todas as magias e monstros de 2024 em dois arquivos JSON, mas a página consultada não apresenta licença clara dos dados. Serve como pista de esquema, não como origem de conteúdo importado.
- [SRD 5.2.1 oficial](https://www.dndbeyond.com/srd): a Wizards of the Coast disponibiliza o SRD 5.2.1 sob CC-BY-4.0. É uma fonte aberta útil para comparar conteúdo que pertence ao SRD, não uma cópia dos três livros básicos completos.
- [mmerkes/dnd-5e-srd](https://github.com/mmerkes/dnd-5e-srd): oferece JSON estruturado gerado a partir do PDF oficial SRD 5.2.1 e documenta correções de artefatos de extração. Pode servir à conferência do subconjunto aberto; os dados devem continuar sendo verificados contra o PDF oficial.

## Decisão para esta etapa

Usar JSON público como estrutura de comparação quando a origem e a licença do dado estiverem claras. Para ações e efeitos do XMM, manter o fonte pinado, comparar cada comportamento com o registro correspondente e não trocar o conjunto por outro pacote que não cubra essa edição. Nenhum novo texto de livro foi copiado para o aplicativo durante esta pesquisa.


## Instrução do proprietário — uso pessoal

Em 14/10/2026, o proprietário confirmou que pagou pelo material e autorizou usar o material disponível para completar o VTT de uso estritamente pessoal, sem limitar a implementação ao SRD. A origem e a edição ainda devem ser registradas e conferidas: esse aceite orienta o escopo do projeto, não transforma a licença MIT do código de um repositório em licença de conteúdo. Manter o material dentro do projeto solicitado e não publicar dumps integrais em notas ou na resposta. A seleção automática de um JSON externo continua exigindo comparação com o livro/edição corretos para não misturar revisões ou descartar regras.
