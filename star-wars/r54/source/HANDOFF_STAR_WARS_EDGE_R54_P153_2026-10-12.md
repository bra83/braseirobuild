# Braseiro Star Wars VTT — R54 P153 | 12/10/2026

## Base e checkpoint

- Base canônica verificada por SHA-256: `BRASEIRO_STAR_WARS_EDGE_R54_P152_SOURCE_CHECKPOINT.zip` (`177dfb596f886007e35aef86507d283c741225fea8d797232f463507881397c7`).
- Novo projeto Android Studio: `BRASEIRO_STAR_WARS_EDGE_R54_P153_SOURCE_CHECKPOINT.zip` (fontes integrais, **não APK**).
- Android `versionCode=10153`, `versionName=1.0.153`, shell offline `1.0.153`; bundle WebView recompilado com **79 módulos**.
- Oito fontes Java do motor Android/TTS byte a byte iguais ao baseline P150 auditado. Nenhum modelo de voz foi copiado ou embutido.

## Implementação integrada P153

1. **Gatilhos com contexto correto:** cada efeito condicional de gatilho recebe o payload do evento causal, incluindo gatilhos que emitem eventos filhos. Um evento filho usa seu próprio payload e não herda os campos do pai.
2. **Condições compostas e valores qualificados:** comparação `payload:campo:valor` separa apenas no primeiro `:`; valores com `:` são preservados. Isso elimina comparações truncadas de rotas e identificadores compostos.
3. **Atomicidade UAE:** efeitos condicionais, alterações de contadores, relógios, objetivos, consequências e eventos encadeados são aplicados na mesma transação. Erro de comando ou de persistibilidade reverte todos os efeitos.
4. **Integridade causal do checkpoint:** snapshots criados sob auditoria integral (`causalAuditFromSequence===1`) não podem marcar objetivo como concluído/falho nem consequência como registrada/resolvida sem os eventos correspondentes no journal. Saves legados anteriores ao marco causal mantêm a política de migração, sem inventar eventos históricos.
5. **Pré-validação de conteúdo:** consequências declaradas por gatilhos são verificadas antes da aventura começar; IDs inválidos, objetos aninhados e campos internos de persistência são rejeitados.
6. **Cinco aventuras:** novos testes exercitam as cinco com efeitos condicionais, relógios, objetivos, consequências, rollback, três reaberturas e proteção contra duplicação.

## Gates e evidências

- Node regular: **1.896/1.896 PASS** em 267 arquivos, `evidence/P153/NODE_REGULAR.txt`.
- Stress: **6/6 PASS**, `evidence/P153/NODE_STRESS.txt`.
- Focais novos P153: **45/45 PASS** (incluídos no regular), `evidence/P153/P153_FOCAL_TESTS.txt`.
- Kokoro/TTS: **27/27 PASS** em testes de código, `evidence/P153/KOKORO_TTS_REGRESSION.txt`.
- Auditoria integrada: **PASS**, cinco aventuras iniciadas, transicionadas e reabertas três vezes, `evidence/P153/RELEASE_AUDIT.json`.
- Gate sintático Java: **PASS** para 8 fontes, sem resolução dos símbolos Android por ausência do SDK, `evidence/P153/JAVA_SYNTAX.txt`.
- Bundle offline: **79 módulos**, `evidence/P153/BUNDLE_BUILD.txt`.

## Pendências explícitas

- **AoR: 247/360 posições mecânicas, 113 pendentes**. P153 não reivindica novas posições implementadas.
- Certificação integral das expansões EotE/AoR contra os livros oficiais não executada.
- Cinco aventuras: auditoria estrutural/transacional concluída, mas cotejo cena a cena com os PDFs não executado. Combate oficial de *Além da Orla* permanece pendente.
- APK Gradle não compilado; sem homologação física no S23 Ultra/A57 e sem teste real de reprodução Kokoro.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node tools/audit-p153-release.mjs
node --test tests/uae-p153-trigger-context-recovery.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
```

Próximo bloco: regras AoR ainda não consumidas, homologação das aventuras com PDFs oficiais e compilação Gradle em runner com Android SDK, mantendo P153 como baseline imutável.
