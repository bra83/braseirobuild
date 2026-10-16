# BRASEIRO STAR WARS — META CANÔNICA DE ENCERRAMENTO (15/10/2026)

**ORDEM DO PROPRIETÁRIO: CONCLUIR TODO O VTT, SEM DEIXAR REGRAS, MECÂNICAS, CENAS, ENCONTROS, CONSEQUÊNCIAS OU SISTEMAS PARA TRÁS. NÃO COLOCAR PARA JOGAR INCOMPLETO.**

## Escopo absolutamente fixo

Exatamente cinco aventuras: `under-black-sun` (SWEP01), `beyond-the-rim` (SWE05), `long-arm-of-the-hutt` (SWE02), `jewel-of-yavin` (SWE09) e `mask-of-the-pirate-queen` (SWE13). Regras integrais EotE/AoR e expansões selecionadas fazem parte do jogo; outras aventuras não. Preservar fichas, UI PT-BR, Android, assinatura/package, saves, mundo vivo, mapas, veículos, companheiros, imagens e TTS externo Kokoro.

## Método híbrido, sem entregar autoridade às IAs

* **CÓDIGO**: executa deterministicamente dados, efeitos, iniciativa, talentos, perícias, testes, atributos, combate, saúde, equipamentos, dinheiro, Duty/Obligation, relógio, veículos, viagens, recompensas, NPCs, inventário, saves, migrations e rollback; decisões do jogador são explícitas e recibos têm causalidade verificável.
* **RAG**: consulta referência oficial da edição certa, livro e página; não inventa estatísticas nem altera estado. Ausência de fonte identificável torna a regra não certificada e bloqueia efeito automático.
* **GEMINI + MOTOR BÁRBARA**: narração humana/sensorial, cenas, falas e sugestões. A resposta não pode fabricar rolagem, fatos canônicos, dano, XP, equipamento ou resultado. Propostas só viram efeitos após executor e validação (e curadoria quando pertinente). Falha da IA deve ser contida, jamais sucesso improvisado.

## Gate de encerramento por lote

1. INVENTÁRIO: relacionar PDF oficial exato, seção, página, versão, cada cena e cada mecânica para não omitir nada.
2. IMPLEMENTAÇÃO: grafo/decisões, NPC e ficha oficial, execuções determinísticas, controller/bridge/UI PT-BR, save/load/migração; RAG apenas para fonte/narração.
3. PROVA: percorrer todos os ramos e consequências com rolagens/faces reais, evento físico e Mestre quando exigidos, ausência de progresso/PE falsos; três reaberturas de save, replay/idempotência e rollback.
4. REGRESSÃO: testes focais + execução **individual** de todos testes regulares + seis arquivos stress, sem pular arquivos para declarar sucesso.
5. MATRIZ: marcar `CERTIFICADO` **somente** com prova individual do PDF, lógica e teste; `IMPLEMENTADO_NAO_CERTIFICADO` e `PENDENTE` não contam como conclusão.
6. ENTREGA TÉCNICA: fonte Android Studio integral + delta + SHA256 + handoff + evidências e backup Google Drive com download/readback de todos os fragmentos e reconstrução SHA.

## Ordens de prioridade, megalotes (não microtarefas)

1. SWE02 *Long Arm of the Hutt*: New Meen, Ryloth, Geonosis, Teemo, todas cenas, antagonistas, efeitos, evidências, destinos, recompensas. P235 implementa somente um bloco integrado de New Meen e não deve ser declarado SWE02 integral.
2. Auditoria/correção **integral** das quatro aventuras restantes, cada uma com todos os encontros e finais.
3. Certificar **todas** as famílias de regras EotE/AoR/expansões, incluindo as 360 posições AoR declaradas e ainda não certificadas individualmente.
4. Testar os cinco caminhos completos, variações de escolha, NPC/mundo vivo, companion em três modos, offline/RAG/Gemini e integridade de save após três reaberturas.
5. **SÓ NO FINAL** Gradle, APK assinado, CI BRELGA, instalação/upgrade, testes no Galaxy A57/S23 Ultra, áudio Kokoro/Google/Samsung e homologação física.

## Regra de progresso

Não avançar o número de versão apenas para preencher hora; usar cada sessão para fechar um conjunto substantivo comprovado. Se não houver fonte ou testes, declarar pendência. **O objetivo é a entrega total e certificada, não uma quantidade de checkpoints ou um protótipo jogável.**

## Backup e retenção

Pasta Google Drive `BRASEIRO_STAR_WARS_CHECKPOINTS` (ID `1qTEzIaJi1Ot3jqjVpKFpIvP4oRwJg1an`). Depois de cada checkpoint aprovado, salvar ZIP inteiro (dividir em partes <=32MiB se necessário), manifesto com SHA de todas partes + ZIP, delta e handoff. Rebaixar e comparar SHA antes de considerar protegido. Preservar checkpoint principal, anterior validado e marcos históricos distintos P126 e P113; remover versões intermediárias antigas apenas após prova de redundância, nunca livros/PDF, saves, fontes de outros VTTs ou marcos de conteúdo exclusivo.
