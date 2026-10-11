# BRASEIRO UNIVERSAL — ESTADO CANÔNICO DE CONTINUIDADE

## Decisão expressa do proprietário
- Braseiro D&D 5e NORMAL (RUN245) é a base. NÃO confundir com HYBRID FASTPLAY.
- Objetivo: VTT universal Android, mantendo interfaces, Mestre de IA Gemini, mundo vivo, mapas, fichas, TTS, inventários e saves existentes.
- Adjudicação manual é requisito central: Mestre descreve situação, regra pertinente, sucesso e fracasso; jogador informa resultado; app registra e Mestre continua.
- A IA não precisa de código para cada situação narrativa. Alterações numéricas (HP, slots, itens, tempo, posição) devem usar operações validadas; resultado manual não autoriza edições arbitrárias.
- Não desviar para APK, G05 ou cobertura de livros quando o trabalho em curso é universalização.

## Linha de base
- Fonte escolhida: BRASEIRO_DND_NORMAL_RUN245_RESTAURACAO_4_CLASSES_FONTE_COMPLETA.zip, com as 4 classes Java faltantes recuperadas da RUN243.
- Raiz Java: app/src/main/java/com/braseiro/dnd5e/; pacote Android com.braseiro.dnd5e. DB_SCHEMA=61; BACKUP_FORMAT=21.
- Pasta Drive: https://drive.google.com/drive/folders/1FmHU3byadycibofjR40bxvd-5P3_tl7K
- Materiais oficiais: https://drive.google.com/drive/folders/13wB-KfYCe2k5xGmb-z2D09KRDJtSyfHu
- GitHub: https://github.com/bra83/braseirobuild

## Integrado nesta revisão (porta inicial)
- adventure/ManualAdjudicationEngine.java: proposta/resolução manual, seleção success/failure/mixed/custom, validação, recibo e idempotência.
- adventure/AdventureState.java: proposta pendente e histórico incluídos no mesmo JSON persistido do estado da aventura.
- adventure/AdventureEngine.java: params.manual_request e params.manual_result, com commit SQLite antes de atualizar memória; criação de contexto de narração após resultado.
- MainActivity.java: diálogo de resultado ao receber a proposta via executeAdventureAction(); sucesso/fracasso e resultados especiais via texto. Na retomada do módulo, reabre pedido manual pendente.
- Esta entrega NÃO conecta automaticamente o Gemini à criação de propostas de testes. NÃO reprocessa alterações físicas de HP/itens/tempo nem apresenta o fluxo em todos os modos da UI. Ainda requer conexão ao loop narrativo e testes Android reais.

## Testar primeiro
1. Proposta em módulo ativo: retorna requires_manual_result e exibe diálogo.
2. Reiniciar/resumir e confirmar que proposta continua no AdventureState.
3. Confirmar sucesso/falha: grava receipt e aparece no narration_context; não deduz dano/fadiga.
4. Repetir mesma resposta: already_resolved. Resposta diferente ao mesmo ID: blocked.
5. Modificar nó antes da confirmação: blocked.
6. Falhar persistência: estado ativo deve permanecer inalterado.
7. Verificar que o seletor de sistema e o mestre narrativo permanecem preservados.

## Dívidas e honestidade de escopo
- UI existe como método chamado quando o fluxo executeAdventureAction recebe manual_request. Não comprova acesso direto a partir da narração Gemini.
- Os efeitos numéricos e backup portável Backup21 não incluem contratos novos de adjudicação fora dos estados já exportados; confirmar cobertura de adventure_state_journal no Backup21 antes de alegar portabilidade total.
- Nenhum novo APK, build BRELGA ou teste físico foi executado nesta mudança.
- Rastrear sempre arquivos reais, testes efetivamente executados e regressões.

## Repositórios externos candidatos
- Loreweaver: contrato motor IA/regras em dados; MIT.
- Tome: schemas de entidades e fichas configuráveis; MIT.
- AetherTable: provedores IA e rolagens honestas; MIT, persistência limitada.
- PlanarAlly: mapa e iluminação; MIT.
- Gravewright: arquitetura de plugins; GPL-3.0-only.
- SWRPG Dice: dados Star Wars; MIT.
- Year Zero Universal Roller: dados Year Zero; MIT.
- Steward: apenas referência (licença não comercial).
- Não substituir o Braseiro por esses projetos; comparar e incorporar funcionalidades isoladas, sempre com testes e revisão de licença.
