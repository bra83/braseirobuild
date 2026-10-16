# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P188

**Origem:** P187 ZIP integral, SHA256 `08a9875cb7fe0ea0f5ac3b7ded0c9e16a189387ec6fb3214dfbd5900816aa1ab`, 97.712.031 bytes, 5.002 arquivos. **Destino:** P188 ZIP integral e SHA256 no arquivo `.zip.sha256` que acompanha o checkpoint. Não substituir código por este relatório.

## Implementação real P188

- SWE09 *The Jewel of Yavin*: novo módulo `app/src/main/assets/vtt/js/adventure/jewelBankPreparations.mjs` implementa preparações source-backed de *Setting Up the Slicing Shop* e *Banking on a Break*, cotejadas com o PDF oficial do usuário nas páginas impressas 39–41 e 68–72.
- **Amostras de criptografia:** sem amostra = 3 dados de revés; amostra básica = 1; completa = 0. Origem explícita e confirmação GM (`public_office_distraction`, `arend_access_card`, `government_infiltration`). Amostra básica pode ser melhorada para completa com novo recibo causal; não se permite regredir.
- **Lobot:** modos `active`, `distracted`, `neutralized` e alerta da secretária Bea, sempre confirmados pelo Mestre; afetam realmente os dados do slicing: downgrade, dificuldade Desafiadora ou revés extra. Alarme e falhas anteriores continuam aplicando upgrades.
- **Computer spike:** fabricação com teste narrativo real de Computadores Difícil, material de 300 créditos debitado atomicamente da ficha, 10 horas registradas no recibo, bônus de reforços derivado das vantagens do teste e aplicado somente à primeira tentativa de slicing. Falha consome componentes e não gera reforços. Não há fabricação gratuita.
- Recibos `BANK_PREP` com replay determinístico de pool, faces, símbolos, custos, opções e estado; anti-duplicação, proteção contra adulteração, rollback, reaberturas, migração sem invalidar saves P187. Integração com recibos `BANK_STEP` e validação global de operações da aventura.
- Integração real `GameController.prepareJewelBank`, `adjudicateJewelOperation('bank_prepare')`, `BraseiroUiBridge.prepareJewelBank`, `publishedAdventureRegistry`, status público e controles de preparação/slicing no painel Android PT-BR. A UI exige checkbox explícito do Mestre para fatos narrativos; mostra crédito disponível e progresso G-100.
- Android `versionCode=10188`, `versionName=1.0.188`; shell e diagnóstico sincronizados; bundle offline reconstruído com **89 módulos**. As **8 fontes Java** de Kokoro/TTS são byte a byte idênticas ao P187.

## Evidências verificadas

- `tests/uae-p188-bank-preparation-spike-encryption-lobot.test.mjs`: **12/12** testes focais.
- `evidence/P188/TEST_SUMMARY.json`: **2.776/2.776** regulares em **307** arquivos, **6/6** stress, total **2.782/2.782** em **313** arquivos. Testes executados por arquivo; **não** declarar suíte monolítica aprovada.
- `evidence/P188/PDF_TARGETED_SOURCE_CROSSCHECK.json`: comparação **direcionada** das páginas 39–41 e 68–72 do SWE09, não certificação editorial integral.
- Java/Kokoro 8/8 idênticos; bundle 89 módulos; ZIP CRC e SHA registrados nos artefatos externos.

## Limites explícitos

1. **Restraining bolt** continua uma confirmação do Mestre, sem consulta/consumo do inventário; não declarar item físico certificado.
2. As **10 horas** de fabricação do spike são registradas no recibo, mas **não** foram integradas ao relógio universal do mundo.
3. O contrato de preparação é ativado ao executar a primeira ação `BANK_PREP`; saves P187 sem preparação mantêm o pool legado para não invalidar recibos já emitidos. Integrar no futuro o contrato obrigatório de preparação nas novas sessões, com migração segura.
4. Ainda faltam NPC slicers R3-8C/Naboka com contratação e pagamento, efeitos completos da Tabela 2-1, liquidação financeira, limpeza de evidências e fuga/traição.
5. **Cinco aventuras não certificadas editorialmente de ponta a ponta**; AoR 360/360 consumidores declarados não é certificação semântica das regras/expansões EotE/AoR.
6. **Sem Gradle/SDK/adb/APK/CI BRELGA/homologação física** comprovados nesta execução. O GitHub connector disponível não expôs criação de release com ZIP anexo nem disparo do workflow exigido.

## Próximo bloco P189+

- Implementar verificação real do restraining bolt no inventário e vincular tempo de fabricação ao relógio mundial com rollback e compatibilidade.
- Integrar slicers NPC oficiais, computer spike e amostras ao fluxo completo de preparação; terminar Tabela 2-1, liquidação e consequências de segurança.
- Completar fuga/traição/rivais SWE09 e New Meen/Geonosis/Teemo SWE02, depois cotejar os cinco PDFs cena a cena e talentos EotE/AoR/expansões.
- Rodar testes focais + regressão integral por arquivo + stress; preservar Kokoro/TTS; gerar ZIP Android Studio completo, SHA256, delta e handoff; salvar Biblioteca com readback.
- Tentar BRELGA/Gradle apenas quando o checkpoint estiver efetivamente no repositório/release e houver CI real. Não inventar APK, testes, certificação ou conclusão.

**Regra:** P188 só é canônico após ZIP CRC + SHA + reconstrução de delta e, se possível, Biblioteca com readback. Não regressar ao P187.
