# BRASEIRO STAR WARS — R54 / P171 — CHECKPOINT INTEGRADO

**Origem:** P170 completo, SHA-256 validado. **Destino:** P171 / Android 1.0.171 / versionCode 10171.

## Implementação real

1. **IA de NPC / companheiros:** o Mestre pode definir o alvo dos próximos turnos de iniciativa dos adversários. Cada ataque passa pelo mesmo turno real do NPC, com perícia, dificuldade, Guarda-costas, defesa do companheiro, dano, críticos, log e persistência de fichas. A escolha é explícita; não há ataque gratuito nem decisão autônoma oculta.
2. **Guarda-costas:** efeito adquirido em fonte, graduações, Tensão voluntária, proteção em turnos reais de NPC, expiração na próxima oportunidade do jogador. Alvos incapacitados são removidos automaticamente.
3. **Tenacidade aprimorada:** incapacitação por Tensão em encontro com iniciativa abre uma única oportunidade no próximo turno do jogador. Os NPCs restantes aguardam a resolução. Sucesso recupera Tensão e continua o encontro; falha encerra com derrota. Morte e incapacitação por ferimentos/críticos não habilitam a janela. Salvamento atômico e restauração de janela.
4. **Android PT-BR:** botão para o Mestre escolher alvo de iniciativa, estado de recuperação exibido, bundle offline reconstruído, versão 1.0.171.
5. **Universal Adventure Engine / cinco aventuras:** 5 novos testes end-to-end em aventuras publicadas: início, encontro, alvo de NPC, consequências mecânicas e três restaurações. Nenhuma fonte editorial foi alterada.
6. **Kokoro/TTS:** os 8 arquivos Java permanecem byte a byte idênticos ao P170.

## Evidências reproduzíveis

- `python tools/run-node-suite.py --regular`: **2431/2431 PASS** (289 arquivos, 18 testes novos).
- `python tools/run-node-suite.py --stress`: **6/6 PASS**.
- `node --test tests/*tts*.test.mjs tests/r54-p91-android-share-kokoro-audit.test.mjs`: **27/27 PASS**.
- `node tools/java-syntax-gate.mjs`: **PASS** (8 Java).
- `node tools/audit-published-adventures-p148.mjs`: **EXIT 0** (somente gate estrutural e semântico).
- `node tools/build-ui-bridge-bundle.mjs`: **PASS**, 80 módulos.
- `./gradlew :app:assembleDebug --offline`: **BLOQUEADO** por download da distribuição Gradle indisponível no ambiente; APK não homologado.

## Pendências

- **38 posições AoR sem consumidor mecânico**, cobertura permanece **322/360 (89,4%)**. Não houve ativação fictícia de talentos.
- Complementos semânticos de Melhorias caseiras; certificação EotE/AoR contra livros e comparação das cinco aventuras com PDFs oficiais não realizadas.
- Homologação de APK, áudio Kokoro real e três reaberturas físicas não realizadas.

## Continuidade

Usar o ZIP P171 completo; não aplicar o delta a outra base. Compilar no runner GitHub self-hosted BRELGA com SDK/Gradle disponíveis, executar regressão e homologar aparelho. Preservar o TTS sem mudanças não testadas.
