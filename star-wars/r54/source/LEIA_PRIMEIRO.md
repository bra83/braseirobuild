# ATUALIZAÇÃO CANÔNICA — STAR WARS EDGE R54 P141 (11/10/2026)

**Autoridade de retomada: `HANDOFF_STAR_WARS_EDGE_R54_P141_2026-10-11.md` + ZIP completo P141.** Versão 1.0.141, regressão 1554/1554 + stress 6/6 PASS, Kokoro/TTS 27/27 PASS (código), cinco grafos estruturalmente auditados, AoR 215/360 posições mecânicas. **Sem APK/homologação Android ou certificação integral dos PDFs.** O histórico abaixo permanece apenas para referência; não retomar P107.

---

# ATUALIZAÇÃO CANÔNICA P102 — 28/09/2026

> **Este cabeçalho substitui baselines antigas citadas abaixo.** A autoridade atual é `RETOMADA_ATUAL.md` + `HANDOFF_STAR_WARS_EDGE_R54_P102_2026-09-28.md` + `MATRIZ_COBERTURA_MECANICA_REAL_STAR_WARS_P102.md`.
>
> Baseline: **1.0.102 / P102** · regressão **1141/1141 PASS** · Android/Gradle **não executado** · não usar GitHub para corrigir · não recriar projeto.

# ATUALIZAÇÃO CANÔNICA P100 — 27/09/2026

> **Este cabeçalho substitui baselines antigas citadas abaixo.** A autoridade atual é `RETOMADA_ATUAL.md` + `HANDOFF_STAR_WARS_EDGE_R54_P100_2026-09-27.md` + `MATRIZ_COBERTURA_MECANICA_REAL_STAR_WARS_P100.md`.
>
> Baseline: **1.0.100 / P100** · regressão **1124/1124 PASS** · Android/Gradle **não executado** · não usar GitHub para corrigir · não recriar projeto.

# LEIA PRIMEIRO — STAR WARS EDGE OF THE GALAXY R54 P20 MASTER LONG NARRATION CLOSURE

Esta pasta é a entrega-fonte final do projeto Android Studio.

- Abra a raiz no Android Studio.
- JDK 17 + Android SDK 36.
- Execute o módulo `app`.
- Não substitua a shell R46.
- Não transforme combate em aba principal.
- Não aplique regras diretamente na UI: mutações passam pelo Motor Barbara.
- A chave Gemini é configurada dentro do app e protegida pelo Android Keystore; o fallback persistente usa AES-GCM e migra o campo plaintext legado.
- O projeto não contém APK/AAB.

Baseline ativa: modo padrão **Edge of the Empire (`EOTE_CORE`) + Obligation**, com campanhas **EOTE / AOR / MIXED** selecionáveis. Duty/Contribution ficam ativos apenas em AOR/MIXED e são separados por proveniência; somente o subconjunto AoR auditado é tratado como autoridade oficial.

Validação interna desta entrega: **783/783 regulares PASS** + **6/6 stress PASS**, total **789/789**; cada suíte de stress cobre os ciclos previstos pelo subsistema. Catálogo atual: **272 entradas pessoais ativas + 25 veículos/naves**.

Para retomar trabalho, leia primeiro `RETOMADA_ATUAL.md`; não reextraia o ZIP CORRIGIDO por cima do código atual sem reaplicar o delta registrado.

Neste host, a distribuição Gradle 8.13 não está em cache e `services.gradle.org` não resolve; por isso o build não inicia. A regressão regular e as 6 suítes stress foram executadas separadamente e passaram. Instalação e smoke Android continuam dependentes de um host com Gradle/SDK/cache ou rede funcional.

Escopo desta entrega: **sandbox aberto** com regras e complementos. As aventuras da pasta `Aventuras` do Drive ficam fora do corpus inicial e não são iniciadas nem pré-carregadas. Para implantação futura, use a Biblioteca/RAG e o contrato de extensão documentado em `SANDBOX_CONTENT_SCOPE.md`.
