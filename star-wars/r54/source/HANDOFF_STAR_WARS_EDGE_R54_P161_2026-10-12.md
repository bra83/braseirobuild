# Braseiro Star Wars VTT — R54 P161 | 12/10/2026

## Checkpoint canônico

- **Entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P160_SOURCE_CHECKPOINT.zip`, SHA-256 `696b8a581a5e532c4cba9268d11da3f01ef847371d13922b315b41d38e283e52`.
- **Saída:** `BRASEIRO_STAR_WARS_EDGE_R54_P161_SOURCE_CHECKPOINT.zip`, ZIP integral para Android Studio. Não é APK.
- Android `versionCode=10161`, `versionName=1.0.161`; shell offline atualizado e bundle de **79 módulos** recompilado.
- Oito arquivos Java, incluindo TTS, **idênticos byte a byte** ao checkpoint P160; nenhum modelo Kokoro incorporado.

## Implementação de código P161

1. **Age of Rebellion +8 posições oficiais** (mecânicas): NERVIOS DE ACERO/Steely Nerves (1), SUBIR DE UN SALTO/Let's Ride (3), CONOCIMIENTO ESPECIALIZADO/Knowledge Specialization (2), PROVEEDOR HABITUAL/Know Somebody (2). **283/360 (78,6%)**, restam **77**. Lista exata: `evidence/P161/AOR_PENDING_77_POSITIONS.json`.
2. **Nervos de Aço:** 1 Destino luminoso, exige talento AoR realmente comprado e encontro ativo; ignora apenas penalidades de críticos em testes de Presença/Vontade até o fim do encontro. Não remove ferimentos, incapacidade ou restrições de manobra. Ativação idempotente, custo e log atômicos; limpeza no início/fim do combate.
3. **Embarque Rápido:** embarque/desembarque em veículo ativo real e posto selecionado. Com talento comprado, incidental 1/rodada; sem talento, usa manobra e regras de Tensão; impede duplicação no mesmo turno, estação inválida e embarque repetido. Estado e log persistidos com rollback.
4. **Conhecimento Especializado:** escolha validada de perícia de Conhecimento na compra; migração para talentos já comprados sem escolha via vinculação posterior sem custo em PE. Ao optar por gastar um Triunfo numa rolagem da perícia escolhida, acrescenta sucessos por graduação comprada. Sem Triunfo, a rolagem continua normalmente sem bônus; não cria sucessos artificiais. Registra consumo do Triunfo, permite graduação independente por perícia, não confia em nomes exibidos.
5. **Provedor Habitual:** na solicitação de compra de item **não restrito** do catálogo, reduz a raridade em 1 por graduação, uma vez por sessão; não altera catálogo/preço nem aprova automaticamente a compra. Recibo de uso e pedido são gravados na mesma transação. Alterar o modificador de raridade do mercado preserva a redução e revalida a compra real do talento.
6. **Interface Android PT-BR:** ações de Nervos de Aço, embarque/desembarque, rolagem de Conhecimento, vinculação de compra antiga e solicitação de compra legal. Ponte JavaScript e bundle recompilados; sem novo armazenamento paralelo.
7. **Cinco aventuras/UAE:** executadas novamente as transições de gatilho → objetivo → relógio → consequência e recuperação em três reaberturas, com rollback de comando inválido. Isso não substitui a conferência editorial de todas as cenas e fichas com os PDFs oficiais.
8. **Regressão histórica:** testes de versões anteriores adaptados para aceitar a versão posterior e talentos antes inativos que agora têm consumidor, mantendo as demais verificações e casos negativos.

## Evidências reproduzíveis

- `evidence/P161/NODE_REGULAR.txt`: **2170/2170**, 276 arquivos de testes.
- `evidence/P161/NODE_FOCAL.txt`: **43/43** novos testes P161 (incluídos nos 2170).
- `evidence/P161/NODE_STRESS.txt`: **6/6** suítes independentes.
- `evidence/P161/KOKORO_TTS_REGRESSION.txt`: **27/27** testes de código.
- `evidence/P161/JAVA_SYNTAX.txt`: **8/8** Java sintaticamente válidos (sem resolução do Android SDK).
- `evidence/P161/AUDIT.json`: `allPassed=true`, cinco grafos/semânticas validados, 79 módulos, 8 fontes Java preservadas, 283/360 AoR e versão conferidas.
- `evidence/P161/ANDROID_BUILD.txt`: `bash gradlew --offline :app:assembleDebug` falhou ao tentar acessar `downloads.gradle.org` (DNS indisponível). **Nenhum APK foi compilado, instalado ou homologado** neste ambiente.

## Pendências para o próximo bloco

1. **77 posições AoR** sem consumidor mecânico, listadas nominalmente no JSON; não marcar cobertura total sem implementação/testes por efeito.
2. Certificação integral das expansões **EotE/AoR** contra os PDFs primários do Drive.
3. Conferência de **cinco aventuras** contra os PDFs: cenas, NPCs, fichas, recompensas e consequências. Dados provisórios de *Além da Orla* permanecem provisórios.
4. Compilação e homologação Android no runner **BREGA/BRELGA** com Gradle/SDK, instalação em S23 Ultra/A57, três reaberturas e reprodução física Kokoro.

## Reprodução

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p161-aor-knowledge-market-boarding-steely-five-adventures.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
node tools/audit-p161-release.mjs
```

**Regra de continuidade:** preservar P161 integralmente; não confundir cobertura mecânica, testes de código, cotejo editorial e homologação física. A versão P161 é um checkpoint de fonte testado, não um APK final.
