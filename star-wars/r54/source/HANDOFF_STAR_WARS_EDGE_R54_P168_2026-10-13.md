# BRASEIRO STAR WARS VTT — R54 P168 — 13/10/2026

## Base canônica e artefatos

- **Base de entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P167_SOURCE_CHECKPOINT.zip` (SHA-256 `c9e46c259ab3c7902504ae0f4cd32d1994b37912b18781508ccdb67ac9352562`, verificado antes de editar).
- **Checkpoint de saída:** `BRASEIRO_STAR_WARS_EDGE_R54_P168_SOURCE_CHECKPOINT.zip`, **projeto completo Android Studio**, NÃO é APK.
- **Android:** `versionCode=10168`, `versionName=1.0.168`, `r46-shell.html` e diagnóstico sincronizados. Bundle offline recompilado com **80 módulos** (P167: 79).
- **TTS/Kokoro:** oito fontes Java preservadas byte a byte em relação a P167; testes de código 27/27, mas **áudio real em aparelho não homologado**.

## Código integrado P168

1. **Universal Adventure Engine:** `validateAdventureCheckpoint` agora rejeita diretamente objetivos concluídos sem dependências concluídas (inclusive saves pré-auditoria). `resolveConsequence` rejeita efeitos externos para consequências autoradas; somente scripts da fonte podem conceder recompensas. `validateAdventureSemantics` rejeita duas definições conflitantes do mesmo ID de consequência. Tudo transacional com rollback.
2. **Cinco aventuras:** 20 cenários novos, cinco adaptadores reais (`Under a Black Sun`, `Beyond the Rim`, `Long Arm of the Hutt`, `Jewel of Yavin`, `Mask of the Pirate Queen`) × quatro gates (checkpoint adulterado, injeção, fluxo integral com três reaberturas, colisão de scripts). Não equivale à comparação integral com PDFs.
3. **AoR — 9 posições novas:** Retórica Inspiradora (base, aprimorada, suprema); Língua Afiada (base, aprimorada, suprema); Presença Imponente; De uma Peça Só (2). Ações sociais agora consomem rolagem narrativa, ação/manobra, Tensão, alvos reais e Vantagens, com efeitos persistentes e durações de rodada/serial do encontro. Inimigos recebem Tensão e Contratempo efetivos; companheiros recuperam Tensão e têm Impulso consumido nos próprios testes. Presença Imponente consome Destino luminoso e cura Tensão. De uma Peça Só aplica bônus real a reparos de casco bem-sucedidos, sem efeito em falhas.
4. **Android PT-BR:** painel de talentos sociais, seleção explícita de companheiros/inimigos, confirmação de alcance, custo de manobra, botão de Presença Imponente e testes de companheiros; ponte `uiBridge` conectada ao controlador e ao bundle offline.
5. **Cobertura AoR executável:** **313/360 (86,94%)**. Restam **47 posições** em `evidence/P168/AOR_PENDING_47_POSITIONS.json`. A cobertura é do catálogo de posições, **não certificação editorial das expansões**.

## Evidência executada

- `evidence/P168/NODE_REGULAR.txt`: **2355/2355** testes, **286 arquivos**, sem falhas.
- `evidence/P168/NODE_FOCAL.txt`: **35/35** cenários P168, incluindo 20 gates de cinco aventuras.
- `evidence/P168/NODE_STRESS.txt`: **6/6**.
- `evidence/P168/KOKORO_TTS_REGRESSION.txt`: **27/27**.
- `evidence/P168/JAVA_SYNTAX.txt`: **8/8 fontes**, análise sintática, não compilação Android.
- `evidence/P168/JAVA_HASH_COMPARE.txt`: **8/8** hashes Java idênticos aos de P167.
- `evidence/P168/ANDROID_GRADLE_ATTEMPT.txt`: tentativa de `bash gradlew :app:assembleDebug --offline` bloqueada porque não existe distribuição Gradle local e o wrapper não consegue acessar `downloads.gradle.org`. **Sem APK homologado.**

## Pendências

1. Implementar as **47 posições AoR** restantes com consumidores reais, sem elevar cobertura por aliases vazios.
2. Certificar expansões EotE/AoR e cotejar as cinco aventuras com os PDFs oficiais, inclusive estatísticas, cenas, escolhas, encontros, recompensas e progressão. Não declarar 100% sem essas provas.
3. Compilar em GitHub runner self-hosted BREGA/BRELGA com Gradle+SDK, testar instalação e três reaberturas no Galaxy S23 Ultra/A57, áudio Kokoro/TTS físico, offline, navegação e UI.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p168-*.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
bash gradlew :app:assembleDebug --offline
```

**Continuar de P168. Não apagar o P167, não reescrever o projeto a partir de um template, não apresentar ZIP de fonte como APK.**
