# Braseiro Star Wars VTT — R54 P159 | 12/10/2026

## Baseline e integridade

- Baseline exata: `BRASEIRO_STAR_WARS_EDGE_R54_P158_SOURCE_CHECKPOINT.zip` (92 MB); fontes canônicas extraídas sem alterar o ZIP original.
- Android Studio `app/build.gradle.kts`: `versionCode=10159`, `versionName=1.0.159`; `r46-shell.html` e bundle offline 79 módulos alinhados.
- Oito fontes Java idênticas à P158 conforme `evidence/P159/AUDIT.json`; Kokoro/TTS não foi modificado.

## Implementações reais

1. **AoR +2 posições:** `EMBAUCADOR NATO` → `NATCHARM` (Carisma/Engano) e `LÍDER NATO` → `NATLEAD` (Liderança/Frieza). Usa o motor de rerrolagem real: uma vez por sessão, dois resultados auditáveis, bloqueio de segundo uso, restauração após três reaberturas e rollback em erro de gravação. O catálogo permanece fail-closed para talentos não implementados.
2. **Android PT-BR:** novo `GameController.naturalRerollStatus`, ponte `BraseiroUiBridge.naturalRerollStatus` e seleção dinâmica em dois painéis de perícias. Exibe nome do talento realmente adquirido, bloqueia checkbox quando a perícia não corresponde, sessão inativa ou talento já consumido. O motor continua validando novamente no momento da ação.
3. **UAE:** gate reforçado para rejeitar objetivos marcados como concluídos com progresso parcial, objetivos ativos com meta alcançada, relógios concluídos com valor abaixo do máximo e relógios ativos já esgotados. A auditoria causal agora detecta contadores de gatilhos falsificados mesmo quando todos os eventos `TRIGGER_FIRED` foram apagados de um checkpoint inteiramente auditado. Checkpoints históricos permanecem sujeitos às regras de compatibilidade existentes.
4. **Cinco aventuras:** 15 cenários de integração P159 testam cascatas de gatilhos, relógios, objetivos, consequências, recibos de idempotência, rollback e recuperação tripla. Os grafos e a validação semântica de todas as cinco aventuras passaram; **isso não substitui a conferência integral com PDFs**.
5. **Inventário de pendências:** `evidence/P159/AOR_PENDING_90_POSITIONS.json` identifica as 90 posições restantes por carreira, especialização, nó e página de árvore; não declara efeitos mecânicos que ainda não foram certificados.

## Evidências verificadas

- `evidence/P159/NODE_REGULAR.txt`: **2097/2097** testes de integração/regressão em 274 arquivos.
- `evidence/P159/NODE_FOCAL.txt`: **27/27** testes novos P159 (incluídos na regressão).
- `evidence/P159/NODE_STRESS.txt`: **6/6** testes de stress.
- `evidence/P159/KOKORO_TTS_REGRESSION.txt`: **27/27** testes de código Kokoro/TTS.
- `evidence/P159/JAVA_SYNTAX.txt`: **8/8** arquivos Java com sintaxe válida (sem resolução de símbolos do SDK).
- `evidence/P159/AUDIT.json`: `allPassed=true`; cinco aventuras; oito fontes Java preservadas; Android 1.0.159 e bundle offline alinhados.
- `evidence/P159/PATCH_REPLAY.txt`: patch P158→P159 reproduz os 2167 arquivos, byte a byte.
- `evidence/P159/ANDROID_BUILD.txt`: Gradle local **não compilou o APK**: indisponibilidade de DNS para `downloads.gradle.org` antes da compilação. O runner self-hosted e o dispositivo físico não foram usados nesta execução.

## Cobertura e pendências

- AoR: **270/360 posições mecânicas (75,0%)**, faltam **90**.
- Certificação de livros/expansões EotE/AoR e comparação integral de todas as cinco aventuras com PDFs continuam pendentes.
- Perfis de *Além da Orla* marcados como provisórios não devem ser declarados oficiais sem comparação com fonte.
- APK Android e áudio Kokoro físico: pendentes; executar compilação no runner self-hosted BREGA/BRELGA, instalar, validar três reaberturas e som real.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p159-natural-rerolls-checkpoint-causality.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
node tools/audit-p159-release.mjs
```

O ZIP P159 é o projeto completo para Android Studio; não equivale a APK ou homologação física. A baseline P158 permanece intacta.
