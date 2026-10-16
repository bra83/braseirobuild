# Braseiro Star Wars VTT — R54 P158 | 12/10/2026

## Baseline preservada

- Base exata: `BRASEIRO_STAR_WARS_EDGE_R54_P157_SOURCE_CHECKPOINT.zip` (SHA-256 `8efc4d72a31f0b837189748f9bd3055844accc50cdb44aa493cbf1c6d04f0986`).
- Projeto Android Studio: `app/build.gradle.kts`, `versionCode=10158`, `versionName=1.0.158`. Shell offline 1.0.158; bundle de 79 módulos canônicos recompilado.
- Oito fontes Java preservadas com hashes idênticos a P157 (`evidence/P158/JAVA_P157_SHA256.json`). Nenhuma mudança nos arquivos Kokoro/TTS.

## Código realmente integrado

1. **AoR +6 posições:** Detonación Selectiva (2), Granadero Experto (1), Aclimatación (2), Resistencia a Venenos (1). Cobertura mecânica **268/360 (74,4%)**, faltam 92. Não foi concedida mecânica a outros talentos não certificados.
2. **Combate explosivo:** a ativação da qualidade Explosão considera o desconto de Granadeiro Especialista (mínimo uma Vantagem) já no recibo persistido do ataque. Detonação Seletiva permite excluir alvos declarados do raio, consumindo uma Vantagem por alvo ou um Triunfo para até as graduações compradas. Compartilha o mesmo orçamento com críticos e outras qualidades, impede replay, rejeita alvos forjados/duplicados e executa rollback integral em falha de persistência. Mantém bônus de Explosão Potente e tratamento distinto de acerto/erro.
3. **Sobrevivência:** Aclimatação remove até dois dados de Contratempo em testes contextualizados de busca de comida, água ou abrigo; Resistência a Venenos adiciona dado de Ampliação a Aguante para resistir/recuperar-se de toxinas. Contextos booleanos são validados, com perícias estritamente delimitadas; sem bônus em testes genéricos.
4. **Android PT-BR:** seleção de alvos afetados/excluídos, opção de pagamento por Triunfo e controles de testes contextualizados de sobrevivência/toxinas no painel existente. A ponte de combate/rolagem utiliza os métodos já existentes, sem criar uma segunda fonte de estado.
5. **Cinco aventuras:** grafos e semântica validados; start/choice idempotentes e três reaberturas por aventura com estado preservado. Não equivale à comparação integral com PDFs oficiais.

## Evidências desta execução

- **2.070/2.070** testes regulares de integração/regressão em **273** arquivos (`evidence/P158/NODE_REGULAR.txt`).
- **35/35** testes focais P158, incluídos na regressão (`evidence/P158/NODE_FOCAL.txt`).
- **6/6** testes de stress (`evidence/P158/NODE_STRESS.txt`).
- **27/27** testes de código Kokoro/TTS (`evidence/P158/KOKORO_TTS_REGRESSION.txt`).
- **8/8** arquivos Java aprovados no gate de sintaxe (`evidence/P158/JAVA_SYNTAX.txt`), sem resolver símbolos do SDK Android.
- Auditoria `node tools/audit-p158-release.mjs`: `allPassed=true`; cinco aventuras, 79 módulos, versão Android e oito fontes Java preservadas.
- **APK não compilado:** `bash gradlew :app:assembleDebug --offline --no-daemon` falhou antes de compilar, porque `downloads.gradle.org` não pôde ser resolvido (ver `evidence/P158/ANDROID_BUILD.txt`). Não foi testado em S23 Ultra/A57 e não houve reprodução física Kokoro.

## Pendências prioritárias

- **AoR:** 92/360 posições sem consumidor mecânico; completar as 92 com verificação fonte→regra→runtime→UI→teste.
- **EotE/AoR:** certificação integral dos livros/expansões ainda não realizada.
- **Cinco aventuras:** comparar cada cena, adversário, estatística, recompensa e consequência com os PDFs oficiais. Em *Além da Orla* ainda existem perfis provisórios.
- **Android:** compilar APK no runner self-hosted com Gradle/SDK, instalar, reabrir três vezes, validar Kokoro real, persistência e cenas.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p158-explosives-survival-five-adventures.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
node tools/audit-p158-release.mjs
```

O ZIP P158 é projeto completo Android Studio, **não APK** e **não homologação final**. A versão P157 original permanece intocada.
