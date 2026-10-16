# Braseiro Star Wars VTT — R54 P160 | 12/10/2026

## Base, integridade e estado

- **Base exata:** `BRASEIRO_STAR_WARS_EDGE_R54_P159_SOURCE_CHECKPOINT.zip` (SHA-256 `7fbb300ab17f556801bd2906912b12ced55061fb23e4226ad06ed6e30ddcf289`). O ZIP P159 permanece intocado.
- **Novo projeto completo:** `BRASEIRO_STAR_WARS_EDGE_R54_P160_SOURCE_CHECKPOINT.zip` (SHA-256 no arquivo adjacente). **Android Studio**, não APK.
- Android `versionCode=10160`, `versionName=1.0.160`; shell offline alinhado; bundle canônico recompilado com **79 módulos**.
- Oito fontes Java/TTS preservadas byte a byte em relação à baseline (ver `evidence/P160/AUDIT.json`). Nenhum modelo ONNX foi copiado ou embutido.

## Código efetivamente implementado

1. **AoR +5 posições oficiais:** `MUNICIÓN DE RESERVA` → `SPARECL` (1), `PILOTO EXPERTO` → `SKILLJOCK` (2), `MADERA DE HÉROE` → `HERFORT` (2). **275/360 (76,4%)**, restam **85**. Nenhum outro talento foi declarado implementado. Lista exata: `evidence/P160/AOR_PENDING_85_POSITIONS.json`.
2. **Munição de Reserva e Desespero:** uma falha crítica (Desespero) no último ataque pode ser adjudicada explicitamente pelo Mestre; o talento impede perda de munição por esse motivo, mas **não** impede o consumo normal de Munição Limitada. Sem o talento, a arma fica sem munição e não pode atacar até gastar uma Recarga adicional possuída; recarregar consome manobra e item real. A adjudicação usa ID do ataque, rodada, índice e identidade da arma, é registrada no combate e bloqueia dupla aplicação após reabertura.
3. **Piloto Especialista:** remove 1 Contratempo por graduação em testes de Pilotagem Planetária e Espacial, sem bônus para outras perícias. Usa o motor de dados real e persiste as compras da árvore AoR.
4. **Fibra de Herói:** durante encontro, gasta 1 Destino luminoso para ignorar penalidades de Ferimentos Críticos em testes de Fortaleza e Agilidade até o fim do encontro. **Não** remove os ferimentos nem suspende incapacidade, impedimentos de manobra ou efeitos fora de testes. Efeito, gasto e log são transacionais; ativação duplicada é bloqueada; encontro novo limpa o efeito anterior. Penalidade `next_check` ignorada não é consumida indevidamente.
5. **Segurança da progressão:** para estes talentos AoR, só nós realmente comprados e válidos da árvore autorizam efeitos; nomes traduzidos no display e nós duplicados não concedem graduações artificiais.
6. **Interface Android PT-BR:** painel P160 com Fibra de Herói, adjudicação explícita de Desespero e Recarga adicional, usando a mesma ponte e fonte de estado. Bridge e bundle recompilados; não foi criado estado paralelo.
7. **Cinco aventuras + UAE:** novos cenários verificam gatilho→objetivo→relógio→consequência→recibo, rollback após comando inválido e três restaurações por aventura. As regras e estruturas anteriores permanecem. Não equivale a cotejo editorial integral com os PDFs.

## Testes e evidências

- `evidence/P160/NODE_REGULAR.txt`: **2127/2127** testes de integração/regressão em **275** arquivos.
- `evidence/P160/NODE_FOCAL.txt`: **30/30** testes P160, incluídos nos 2127.
- `evidence/P160/NODE_STRESS.txt`: **6/6**.
- `evidence/P160/KOKORO_TTS_REGRESSION.txt`: **27/27** testes de código.
- `evidence/P160/JAVA_SYNTAX.txt`: **8/8** arquivos Java válidos sintaticamente; símbolos Android não resolvidos nesta máquina.
- `evidence/P160/AUDIT.json`: `allPassed=true`, cinco grafos/semânticas de aventuras aprovados, oito fontes Java preservadas, 79 módulos, cobertura e versão conferidas.
- ZIP/patch: ver evidência de replay e checksum entregue ao lado do projeto.
- `evidence/P160/ANDROID_BUILD.txt`: Gradle wrapper não compilou o APK porque `downloads.gradle.org` não resolveu DNS. Nenhuma instalação ou reprodução de áudio física foi executada.

## Pendências — não declarar 100%

- **AoR:** 85/360 posições sem consumidor mecânico; implementar por efeito oficial, runtime, interface e teste.
- **EotE/AoR:** auditoria integral dos livros/expansões contra fontes primárias ainda pendente.
- **Cinco aventuras:** comparação integral de cenas, adversários, estatísticas, recompensas e consequências com PDFs oficiais pendente; perfis provisórios de *Além da Orla* continuam sinalizados.
- **Android:** executar `:app:assembleDebug` no runner self-hosted BREGA/BRELGA com Gradle/SDK disponíveis; instalar e testar S23 Ultra/A57, três reaberturas e áudio Kokoro real.

## Reprodução

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p160-ammo-heroic-piloting-five-adventures.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
node tools/audit-p160-release.mjs
```

### Referências mecânicas secundárias consultadas (não substituem o cotejo dos PDFs do usuário)

- *Spare Clip*: https://star-wars-rpg-ffg.fandom.com/wiki/Spare_Clip_talent
- *Heroic Fortitude*: https://star-wars-rpg-ffg.fandom.com/wiki/Heroic_Fortitude_talent
- *Skilled Jockey*: verificar descrição do AoR Core no PDF do usuário; a equivalência foi exercitada no runtime de pilotagem.

**Estado recuperável:** P160. Não executar compilação de APK como prova de cobertura de regras; primeiro concluir pendências mecânicas/editoriais, depois homologar.
