# Braseiro Star Wars VTT — R54 P164 | 12/10/2026

## Base canônica e entrega

- Base: `BRASEIRO_STAR_WARS_EDGE_R54_P163_SOURCE_CHECKPOINT.zip`, SHA-256 validado.
- Saída: `BRASEIRO_STAR_WARS_EDGE_R54_P164_SOURCE_CHECKPOINT.zip` — projeto-fonte completo para Android Studio, **não APK**.
- Android: `versionCode=10164`, `versionName=1.0.164`; shell e diagnóstico alinhados.
- 79 módulos no bundle offline. As oito fontes Java, incluindo Kokoro/TTS, permanecem byte a byte idênticas ao P163.

## Implementação real P164

1. **Companheiros com fichas persistentes**: roster de até oito fichas personalizadas no grupo, nome, limiares de Ferimentos/Tensão, identificadores próprios, proveniência `user_custom` (não são NPCs oficiais), validação de importação e três reaberturas.
2. **Medicina em terceiros**: teste real do médico sobre o companheiro, sem penalidade de autotratamento, com custo de Tensão do Médico Especialista aplicado ao médico, gasto de ação em combate, limitação por encontro, checagem de suprimentos e rollback transacional.
3. **Aplicação de estimulantes em terceiros**: alvo selecionável, bônus temporário e Tensão no paciente, verificação de talento AoR e equipamento, commit atômico; encerramento de combate remove bônus de todos os companheiros no mesmo commit do combate.
4. **Bacta em terceiros**: instalação confirmada, duração, cura real, supervisão do personagem e Especialista em Bacta autenticado por compra AoR. Saúde do jogador não é confundida com a do paciente.
5. **Dano e remoção**: dano real e persistente em fichas personalizadas; remoção bloqueada durante combate. Proteção contra IDs falsos, duplicados e fichas inválidas.
6. **Android PT-BR**: painel de companheiros na tela de Recuperação, criação de ficha simplificada, seleção do paciente, dano, remoção e tratamento médico/estimulantes/bacta; métodos no WebView bridge e bundle reconstruído.
7. **Universal Adventure Engine**: predicados de condição com limites de profundidade e quantidade, e comparação `payload:` sem coerção de objetos/arrays; importações malformadas não liberam gatilhos por `String(object)`.
8. **Cinco aventuras**: testes adicionais de estado UAE com gatilhos, consequências, objetivos, relógios, rollback, idempotência e três reaberturas; tratamentos de companheiros durante sessões publicadas não corrompem o checkpoint da aventura.
9. **Regressão de versão**: teste Android antigo atualizado para comparar a versão atual real em vez de exigir número histórico; diagnóstico Android corrigido para P164.

## Testes executados

- **2.254/2.254** integração e regressão, 279 arquivos (`evidence/P164/NODE_REGULAR.txt`).
- **35/35** novos testes P164 (`evidence/P164/NODE_FOCAL.txt`).
- **6/6** stress (`evidence/P164/NODE_STRESS.txt`).
- **27/27** testes de código Kokoro/TTS (`evidence/P164/KOKORO_TTS_REGRESSION.txt`).
- **8/8** fontes Java sintaticamente válidas, sem modificação (`evidence/P164/JAVA_SYNTAX.txt`).
- Bundle offline **79 módulos**, `evidence/P164/BUNDLE_BUILD.txt`.

## Pendências reais

- Cobertura AoR **295/360 (81,9%)** permanece inalterada neste bloco; **65 posições pendentes**, enumeradas em `evidence/P164/AOR_PENDING_65_POSITIONS.json`.
- Certificação editorial integral das expansões EotE/AoR e confronto das cinco aventuras com os PDFs ainda não realizados.
- Companheiros agora têm suporte médico real, mas as fichas personalizadas simplificadas ainda não equivalem a integração de todas as fichas e missões oficiais de NPCs.
- APK Android não compilado: Gradle wrapper 8.13 falhou ao resolver `downloads.gradle.org` (`UnknownHostException`). Usar runner self-hosted BREGA/BRELGA com SDK e Gradle.
- Sem teste físico de instalação/reaberturas em S23 Ultra/A57 ou áudio Kokoro em aparelho; 27 testes são de código.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p164-companion-medical-integrated.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
bash gradlew :app:assembleDebug
```

**Não regredir para P163.** P164 é checkpoint completo de fonte. Continuar AoR + certificação PDF sem confundir testes automatizados com homologação editorial/física.
