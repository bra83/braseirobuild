# Reauditoria G05 com 5e.tools — progresso consolidado

Data: 2026-10-10. Base: RUN245 recuperada, sem HYBRID_FASTPLAY. O adendo `CODEX_ADENDO_G05_FONTE_5ETOOLS_RUN245_20261010.md` foi incorporado.

## Cobertura de fontes por linha

- As 280 issue rows foram cruzadas individualmente com registros estruturados do bestiário 5e.tools: 149 RHW (2026) e 131 XMM (*Monster Manual*, 2025). O conjunto corresponde a 266 pares únicos criatura/ação; repetições permanecem como linhas separadas para conservar a rastreabilidade dos issues RUN245.
- Em RHW, foram inspecionados os registros individuais e a seção da ação, página, ataque ou salvaguarda, dano, condição/efeito, duração, alvo e recarga quando presentes. Spellcasting foi mantido com a sua seção e `displayAs`; efeitos de feitiços referenciados não foram inventados nem copiados.
- Em XMM, as 131 criaturas/ações foram correspondidas aos registros individuais XMM e comparadas com a planilha e o código. Para a faixa 194–237 e 238–280, o endpoint JSON retornou HTTP 200 com cabeçalhos de navegador; páginas HTML dinâmicas isoladas não exibiam o texto mecânico no extrator. As linhas deixaram de ser classificadas como apenas localizador após a conferência individual do registro JSON.
- Esta é validação auxiliar 5e.tools. Não equivale a transcrição/validação pelo PDF licenciado.

## Identidade e limites dos PDFs

- *Ravenloft: The Horrors Within* é edição 2026, source RHW. `books.html` separa esse código de *Van Richten’s Guide to Ravenloft* (2021); nenhuma mecânica de Van Richten foi usada como substituta.
- Não foi encontrado PDF exato de RHW entre os 256 itens diretos da pasta de Drive. Os candidatos `Dominus & Dragons - Ravenloft - Capa Preta.pdf` e `fddh.pdf` identificam obras diferentes.
- O PDF do *Monster Manual* está identificado como `Manual dos Monstros 2024.pdf` (ID `1iqPMW_ljJPWZMs3jYVUagceeZ6xe7N9o`, 277.647.654 bytes); o texto interno aponta edição 2025. O download bruto retornou HTTP 413 (limite 268.435.456 bytes). A extração de texto do Drive devolveu 1.066.072 caracteres OCR, mas há erros de palavras de regras. A planilha `qa-ci/G05_XMM_PDF_OCR_CROSSCHECK_131.csv` registra busca local por linha: 87 rótulos criatura+ação, 20 apenas criatura e 24 sem localizador confiável. É sinal de localização, não validação de mecânica; DC, salvaguarda, duração, condição e efeito continuam sem homologação pelo PDF.

## Mecânicas e código

- O adendo pedia criatura, ação, edição, CD/atributo, resistência/teste, duração, condição e efeito. As planilhas registram `no save`, escape DC ou teste de habilidade quando isso é o que a fonte declara, em vez de normalizar todo DC como salvaguarda.
- Correções de leitura relevantes incluem os ataques de bugbear: Morningstar do Bugbear Stalker e Light Hammer do Bugbear Warrior recebem vantagem contra alvo já agarrado e não aplicam Grappled; Quick Grapple é a ação que aplica a condição. Os DCs de grapple são marcados como escape checks.
- Campos não expandidos ou ambíguos permanecem em evidência: spell effects nas linhas RHW 7, 11, 19, 30, 36–39, 77, 111, 114, 115, 124, 125 e 149; ausência de DC no registro de Siphon Spell (linha 12); múltiplos desfechos/saves em Wild Magic Surge (linha 50); valor numérico ambíguo de recarga de Mind Swap/Yithian (linha 148).
- Quatro entradas de handler determinístico fonte-bound existem no código: Brain-Rending Bite, Soothing Tentacle, Radiant Flame e Crushing Stone. A classe existente de on-hit RHW e o teste dos três riders passaram na suíte BRELGA 38061684317 (76/76). As outras 276 issue rows não têm executor específico aprovado. A suíte também confirmou a correção de replay G08/G09. Nenhuma ação recebeu implementação a partir de mecânica não verificada.

## Evidências

- `G05_AUXILIARY_REVIEW_5ETOOLS_280.csv`: matriz canônica individual de 280 issue rows.
- `G05_SOURCE_TO_EXECUTOR_MATRIX.csv`: fonte, edição, página, mecânica, divergência RUN245 e status do executor por linha.
- `G05_ACTIONS_NEEDING_SOURCE_REVIEW.csv`: inventário original com disposition e evidência atualizadas sem apagar os campos de identidade dos PDFs.
- `qa-ci/G05_REVIEW_RHW_002_050.csv`, `G05_REVIEW_RHW_051_100.csv`, `G05_REVIEW_RHW_101_149.csv`, `G05_REVIEW_XMM_150_193.csv`, `G05_REVIEW_XMM_194_237.csv`, `G05_REVIEW_XMM_238_280.csv`: relatórios detalhados por faixa.

G05 permanece aberto: a cobertura de revisão auxiliar chegou a 280/280, mas os executores, os campos não expandidos e as limitações de validação do PDF ainda impedem homologação.
