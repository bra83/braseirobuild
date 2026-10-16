# Braseiro Star Wars VTT — R54 P163 | 2026-10-12

## Checkpoint canônico

- Entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P162_SOURCE_CHECKPOINT.zip` SHA-256 `0af94322aa90844c5f989156f5fe73972cb0607dbe80814da4126b804ca964d1`, verificado antes de extrair.
- Saída: `BRASEIRO_STAR_WARS_EDGE_R54_P163_SOURCE_CHECKPOINT.zip`, **código-fonte completo para Android Studio**, não é APK.
- Android: `versionCode=10163`, `versionName=1.0.163`; shell e bundle offline alinhados, 79 módulos.
- As oito fontes Java (incluindo Kokoro/TTS) permanecem byte a byte idênticas ao P161/P162.

## Implementação integrada P163

1. **Oito posições AoR adicionais, com consumidores reais**: Especialista em Bacta (1), Aplicação de Estimulantes (1), Aprimorada (1), Suprema (1), Médico Especialista (1), Polifacético (1) e Parada em Seco (2). Cobertura de código: **295/360 (81,9%)**, pendentes **65** posições (`evidence/P163/AOR_PENDING_65_POSITIONS.json`). Não é certificação editorial dos livros.
2. **Medicina**: aplicação em combate com ação, teste real de Medicina, requisito de equipamento possuído, dificuldade 2/3, 4/1 de Tensão, +1 de Característica durante encontro, impedimento de repetição, gasto de Triunfo real para efeito Supremo, e reversão dos bônus ao encerrar combate.
3. **Médico Especialista**: reaproveita o consumidor de Master Doctor já existente em testes de perícia, agora com compra AoR autenticada; na ação Medicina, 2 Tensão voluntária reduzem dificuldade em 1, respeitando limites de rodada e críticos.
4. **Bacta**: recuperação em tanque com confirmação explícita de instalação, cálculo por horas, bônus de Especialista em Bacta quando há supervisão e cura efetiva; rotina pura de cuidado prolongado com supervisor explícito. A UI atual opera na ficha do jogador, sem gestão multi-personagem de pacientes; não confundir com homologação de companheiros.
5. **Polifacético**: compra de nó com escolha validada de duas perícias distintas; adição permanente às perícias de carreira, registro das escolhas, gasto de PE e persistência. UI PT-BR possui seletores para ambas.
6. **Parada em Seco**: o modo AoR agora exige o talento de fonte autêntica antes da manobra; velocidade vai a zero e a nave sofre Tensão de Sistemas igual à velocidade reduzida. Modo EotE preservado por compatibilidade.
7. **Transações**: ações novas de estimulantes, bacta e Medicina fazem commit atômico com rollback no estado e no repositório após falha de gravação.
8. **Cinco aventuras**: cinco cenários novos de integração cobrindo gatilho → objetivo → relógio → consequência, replay idempotente, rollback e três restaurações por aventura; grafos e semântica validados. Não é conferência integral de texto/fichas/recompensas contra os PDFs.
9. **Android PT-BR**: ações médicas no painel de recuperação, compra de Polifacético, método de ponte WebView e bundle offline reconstruído.

## Testes reproduzíveis

- `evidence/P163/NODE_FOCAL.txt`: **25/25** testes P163.
- `evidence/P163/NODE_REGULAR.txt`: **2219/2219** testes de integração/regressão, 278 arquivos.
- `evidence/P163/NODE_STRESS.txt`: **6/6** testes de stress.
- `evidence/P163/KOKORO_TTS_REGRESSION.txt`: **27/27** testes de código TTS.
- `evidence/P163/JAVA_SYNTAX.txt`: **8/8** arquivos Java sintaticamente válidos (não equivale a compilação Android).
- `evidence/P163/AUDIT.json`: `allPassed=true`, 5 aventuras válidas, bundle com 79 módulos, Java preservado, versão 1.0.163.
- `evidence/P163/ANDROID_BUILD.txt`: Gradle não baixou a distribuição neste ambiente. **APK não compilado nem homologado.**

## Pendências reais

1. 65 posições AoR ainda sem consumidor executável.
2. Certificação editorial integral das expansões EotE/AoR contra os PDFs do Drive; não atribuir 100% sem conferir fontes primárias.
3. Conferência integral das cinco aventuras (cenas, NPCs, fichas, recompensas, condições) contra os PDFs. Perfis provisórios de *Além da Orla* continuam provisórios.
4. Compilação APK no runner BREGA/BRELGA com Gradle e SDK disponíveis, instalação S23 Ultra/A57, três reaberturas e teste físico Kokoro.
5. A aplicação de estimulantes na UI é na ficha do jogador; estender a NPCs/companheiros quando o motor de party estiver conectado, antes de certificar o suporte multi-personagem.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p163-aor-medical-fullstop-five-adventures.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
node tools/audit-p163-release.mjs
bash gradlew :app:assembleDebug
```

**Não regressar para P162.** O ZIP P163 e o patch P162→P163 são checkpoints de fonte; nenhum APK foi produzido.
