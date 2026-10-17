# ORDEM MESTRA DE EXECUÇÃO — BRASEIRO O UM ANEL 2E 4.9.212
## Fonte única, caminhos verificados, integração obrigatória e critérios rigorosos de conclusão
**Destinatário:** Antigravity / agente de engenharia sênior Android, TypeScript, Kotlin, segurança e RPG.
**Autoridade:** este documento PREVALECE sobre prompts, handoffs e planos conflitantes anteriores. O PR #59 é a única frente de integração. Estado de referência conferido em 17/10/2026.
**Missão:** ENTREGAR **UM ÚNICO VTT** 4.9.212 jogável e reproduzível a partir do Android 4.9.211, acrescentando apenas as funcionalidades V9 homologadas após correções; preservar todos os recursos existentes. **Não entregar apenas um novo ZIP, relatório, manifesto ou teste verde.**

> PAREDE DE PROTEÇÃO: este projeto é exclusivo de **O Um Anel 2e**. Não modificar D&D/Mystara, Pathfinder/Kingmaker, Star Wars, Forbidden Lands ou outros projetos no monorepositório. NÃO criar outro app, outra UI, outro banco de salvamento ou outro núcleo de combate paralelo.

## 1 — DECISÃO FINAL DE ARQUITETURA: QUEM MANDA EM QUÊ

| Fonte | É a autoridade sobre | Uso determinado | Uso proibido |
|---|---|---|---|
| **Fonte completa Android Studio 4.9.211** | Projeto Android, UI real, navegação, package ID, Gradle, TTS atual, mapas, imagens, saves e componentes já funcionais | **BASE IMUTÁVEL** da nova 4.9.212; trabalhar sobre uma cópia; comparar cada mudança | Substituir app por V9, alterar assinatura/package ID, apagar código existente, importar `android/` genérico do monorepo |
| **ZIP 4.9.212 V9 EST01–EST14** | Módulos novos de regras, aventura, catálogo, RAG, adaptações, testes | **OVERLAY CANDIDATO**: selecionar e corrigir; sem sobrescrever mecanicamente o código estável | Tratar V9 como app final, executar dois motores alternativos com efeitos duplicados |
| **Branch de integração + PR #59** | Correções de segurança da V9, ferramentas de estágio, testes adversariais, CI | **PATCH OBRIGATÓRIO** aplicado à V9 antes da integração e mantido no GitHub | Esquecer, reverter ou substituir as correções pelos arquivos brutos da V9 |
| **PDFs O Um Anel 2e** | Regras oficiais, conteúdo, tramas, tabelas e referências de páginas | Fonte de verdade para comparar **ambas** as implementações | Inventar regra/cena/tesouro e declará-lo oficial; misturar suplementos de outras edições |
| **Motor Bárbara LATEST 1.1.3** | Runtime Bárbara efetivo | Integrar como motor de narrativa com o Host determinístico | Chamar o supervisor JS isolado de motor Bárbara completo |

**Regra de conflito:** (1) versão 4.9.211 funcional prevalece em infraestrutura/UI; (2) PDF 2e prevalece em mecânicas/cânone, com teste que comprove a correção; (3) patches do PR #59 prevalecem sobre V9 bruto em segurança; (4) Gemini/RAG/Bárbara não alteram estado sem executor autorizado. **V1–V8 são apenas histórico. NÃO mesclar seus arquivos.**

## 2 — LINKS EXATOS, PASTAS E DOWNLOADS — TODOS CONFERIDOS

### 2.1 GitHub — ÚNICO repositório de trabalho
- **Repositório monorepo:** https://github.com/bra83/braseirobuild
- **Clone:** https://github.com/bra83/braseirobuild.git
- **BRANCH A USAR:** https://github.com/bra83/braseirobuild/tree/integration/oua-49212-v9-hardened-20261017
- **PR A CONTINUAR:** https://github.com/bra83/braseirobuild/pull/59
- **HEAD verificado antes desta instrução:** `c70b8b27fb2925c04b8589b58938bc43f7780a01`. Confirmar HEAD novamente antes de editar.
- **Branch base de revisão, NÃO substituir por ela:** https://github.com/bra83/braseirobuild/tree/oua/base-playable-20261014
- **Pasta de integração da branch (scripts/testes):** https://github.com/bra83/braseirobuild/tree/integration/oua-49212-v9-hardened-20261017/ci/oua-4.9.212
- **Workflow:** https://github.com/bra83/braseirobuild/blob/integration/oua-49212-v9-hardened-20261017/.github/workflows/oua-v9-hardening.yml
- **Estado registrado:** https://github.com/bra83/braseirobuild/blob/integration/oua-49212-v9-hardened-20261017/ci/oua-4.9.212/INTEGRATION_STATUS.md
- **Esta ordem mestra:** https://github.com/bra83/braseirobuild/blob/integration/oua-49212-v9-hardened-20261017/ci/oua-4.9.212/ANTIGRAVITY_ORDEM_MESTRA_FONTES_UNICAS_E_GATES.md
- **Perfil narrativo universal do Mestre:** https://github.com/bra83/braseirobuild/blob/oua/base-playable-20261014/docs/BRASEIRO_PERFIL_UNIVERSAL_MESTRE_HUMANO_DINAMICO.md

**ATENÇÃO:** o monorepositório tem `android/` e `ci/` para vários jogos. A pasta real do Android OUA é identificada **APÓS EXTRAIR** o ZIP da release 4.9.211, procurando a raiz que contém simultaneamente `package.json` e `android/`. Não presuma que a raiz do monorepo é a raiz do app OUA.

### 2.2 Base Android comprovadamente publicada: OUA 4.9.211
- **Release canônica:** https://github.com/bra83/braseirobuild/releases/tag/OUA-v4.9.211
- **FONTE COMPLETA (268.010.292 bytes):** https://github.com/bra83/braseirobuild/releases/download/OUA-v4.9.211/BRASEIRO_O_UM_ANEL_4.9.211_ANDROID_STUDIO_COMPLETE.zip
- **SHA-256 EXATO do ZIP fonte:** `ce636e4059402d3294aefc661cc56658c41136a287fc44e1ed7e01c40381c058`
- **APK de referência:** https://github.com/bra83/braseirobuild/releases/download/OUA-v4.9.211/BRASEIRO_O_UM_ANEL_4.9.211_STRIDER_OPENING.apk
- **SHA-256 do APK de referência:** `e8908814ac7906aa7d03fb19e1c07bd109c90ed0754731e287271a143c974ad8`
- **Cópia privada da FONTE no Drive, pasta:** https://drive.google.com/drive/folders/1NUIcf4_HIOsz9PgYLq_IgDAz2XmAX_9z
- **ZIP fonte nessa pasta privada:** https://drive.google.com/file/d/1VzLGe_LTqaMwf3epPmaApeOCmFxWT0O9/view

**Não confundir:** SHA-256 de APK/ZIP NÃO é o certificado de assinatura Android. Extraia o certificado da 4.9.211, registre fingerprint e compare com o novo build antes de declarar atualização compatível. A falta da chave de assinatura original bloqueia uma atualização in-place; não inventar certificado nem prometer atualização automática.

### 2.3 V9 de módulos — UMA versão única
- **Pasta onde foi encontrada a V9 e versões históricas:** https://drive.google.com/drive/folders/0AEuJcNyOASFxUk9PVA
- **V9 CORRETA (`EST01_EST14_HOMOLOGATED`; 404.712 bytes):** https://drive.google.com/file/d/1w9U-z2qWi1l4PBxaPSL0OFgH-_R6Ybn-/view
- **V8 (HISTÓRICA — NÃO USAR COMO BASE):** https://drive.google.com/file/d/1MH0UF2kHv9WnnnjYVaSMd02v6FPcgGBK/view
- **V7 (HISTÓRICA — NÃO USAR):** https://drive.google.com/file/d/14JIZZ256Z5Dlxu6R1rtLHH_q3eVdwfYH/view
- **V6 (HISTÓRICA — NÃO USAR):** https://drive.google.com/file/d/1h105x9zak-Qk7ESGOnL10Tcyi074H3YJ/view

A V9 NÃO É O CÓDIGO-FONTE ANDROID. Já passou **70 testes na sua distribuição original**, mas os **10 testes adversariais adicionados no PR #59 não foram homologados sobre a V9 corrigida** até a última conferência. Primeiro aplique ambos os scripts de hardening; repita toda a suíte. NÃO publicar a V9 com conteúdos dos PDFs no GitHub público.

### 2.4 Livros O Um Anel — pasta canônica 2e e documentos
- **PASTA CENTRAL:** https://drive.google.com/drive/folders/15YC5VQ-PLn0o9b41obP3_JessSDuvFZd
- **Livro Básico 2e, páginas 1–80:** https://drive.google.com/file/d/15umbJCMoLAortDPoicWeIHOzaREI9PBg/view
- **Livro Básico 2e, páginas 81–150:** https://drive.google.com/file/d/1mOdsOfqQASmLNqMZZIOz6aPatJHV1zhq/view
- **Livro Básico 2e, páginas 151–246 (adversários, Patronos e A Estrela da Névoa):** https://drive.google.com/file/d/1M1rIXVz1L4Qzz98l5_aTF1c8sOokW4uA/view
- **Modo Peregrino (Strider Mode):** https://drive.google.com/file/d/15gUwEnPRaD6f1FjXs0ho_j8VS5Rq5saY/view
- **Valfenda 2e:** https://drive.google.com/file/d/15mAAieBwcvOfP9zqO54da-L-_-Cx9hFp/view
- **Povos das Terras Ermas 2e:** https://drive.google.com/file/d/15wjw3LYORJeBvG2OB3w4SwbgBl6c-xto/view
- **Trilhas de Personagens V2:** https://drive.google.com/file/d/15uDo5XgBG17xCrr_g2tbmfuIYh8AGpXf/view
- **Contos das Terras Solitárias (6 aventuras):** https://drive.google.com/file/d/15Zq2VPLjH6ys87LlHcH4e1yVakDcX6K-/view
- **Mapa das Terras Ermas, impressão A2:** https://drive.google.com/file/d/15xI_WlnfXShOsatTpE3z23BIJ8VrZOwk/view
- **Resumo de regras auxiliar:** https://drive.google.com/file/d/15dgGDrXggeFCjR37s5AW7eZmJC4HkzTE/view
- **Povos das Terras Selváticas — possível edição/duplicata a verificar:** https://drive.google.com/file/d/15r-r6SFx3RWnRTlI7i-S4JGYp4zGYdH1/view
- **Livro do Aventureiro — edição NÃO CERTIFICADA para 2e:** https://drive.google.com/file/d/15ygPt_Xc_RaWXD61mzmQPVvvogXl6dYP/view
- **Livro do Mestre — edição NÃO CERTIFICADA para 2e:** https://drive.google.com/file/d/15nwIudDRFgC5igAbmckOhjNoEdnmrulX/view
- **Edição brasileira beta 1.0 — edição NÃO CERTIFICADA:** https://drive.google.com/file/d/15wJ84xU19Mi_wOnLrcetqpqXc2uOToXZ/view

**REGRA ABSOLUTA DE EDIÇÃO:** três partes Livro Básico 2e + suplementos identificados 2e prevalecem. Os PDFs potencialmente de 1e, beta ou versões alternativas NÃO autorizam conteúdo mecânico sem confirmação de compatibilidade. Não misturar culturas/virtudes/atributos de versões divergentes.

### 2.5 Mapas, imagens, tokens, músicas e fichas (pastas REAIS)
- **PASTA DE MAPAS:** https://drive.google.com/drive/folders/1dNkLNvZoJ4iJdKeBGpOq3I834IkFltYE
  - `hex_index_terra_media.json`: https://drive.google.com/file/d/1wmtM2OKaQSnTtOeGPSdJ-XHoSl70A-Se/view
  - `map_manifest_terra_media_v2.json`: https://drive.google.com/file/d/1BDZLrpiCo4WG5T0NNuxK6rJS_5vkZdfE/view
  - `terra_media_hex_terrain_atlas.json`: https://drive.google.com/file/d/1PLn3MO0GGeKiS2ASg3aqvpz8E4WxKbst/view
  - **Não usar pacotes antigos de aplicativo que também aparecem nesta pasta como a base 4.9.212.**
- **TOKENS DO JOGADOR:** https://drive.google.com/drive/folders/1UJLYgVGuTH7i721pTMEvvHuwIDaUPB0q
- **TOKENS DE NPCs:** https://drive.google.com/drive/folders/1OYdvaOffuQdos2HXUKwgpzPTH8xixNYi
- **ACERVO BRASEIRO_TOKENS:** https://drive.google.com/drive/folders/1kAPHzMcSsey2aMBM0AyVzPv7dd2tL5Im
  - `tokens_manifest.json`: https://drive.google.com/file/d/1TRDnidRV3X2RNgUjzFv5ZmeoDJn4s5dJ/view
  - `tokens_manifest.schema.json`: https://drive.google.com/file/d/1cbcE3E_wWwJCHCTzRpBRQLBXHs0a644a/view
- **MÚSICAS/MÍDIAS (`song`):** https://drive.google.com/drive/folders/17JHPi0eEe-R5CjwDYJ9GH3fGBg46OqOL
- **FICHA JSON de referência (Burín):** https://drive.google.com/file/d/1EmsUUWcy7gCLFgq3SCoy2bJpDotzoSNS/view
- **OUTRAS FICHAS:** disponíveis na pasta central (`Burin_Tres_Colheres_O_Um_Anel.json`, `Primula_Trombeta_O_Um_Anel.json`, `Targon_Dois_Passos_O_Um_Anel.json`).
- **BACKUPS OUA HISTÓRICOS, APENAS RECUPERAÇÃO:** https://drive.google.com/drive/folders/1xThPALuNzajFT6iEsCSOlRRP17Ym4bPO

**ASSETS:** conferir licença e propósito por arquivo. Não importar imagens/tokens sem manifesto, localização, nome correto ou verificação de dimensões. Preservar mapeamento existente do atlas, posição inicial, zoom, reconhecimento de hex e navegação.

### 2.6 Motor Bárbara — pastas e artefatos EXATOS
- **PASTA RAIZ DO MOTOR:** https://drive.google.com/drive/folders/1ez-p0PARTrstS5PxZfXJHfFjyVuAK0aX
- **VERSÃO ALVO — LATEST 1.1.3:** https://drive.google.com/drive/folders/1oVAD0khD3dqNF1gu6EmjjBi2MSAWxplG
- **ADAPTAÇÕES ANTERIORES (REFERÊNCIA, não source preferencial):** https://drive.google.com/drive/folders/1_I1OulGQxD67MNluCJ7qwJuNJCY8vXpq
- **RELEASES antigas 1.1.0–1.1.2:** https://drive.google.com/drive/folders/16MDWYw0OIDdyBqz681NgQdEzdPFy0IpA
- **CONCEPTS (não é runtime):** https://drive.google.com/drive/folders/1Qk9u5N-4HJGDMI2yR9B3JXbgVLOdvJEf
- **Wheel Python 1.1.3:** https://drive.google.com/file/d/1fNQ3NBu19HGdpKR-M91bo50iBDksHn_s/view
- **Checkpoint 1.1.3:** https://drive.google.com/file/d/1elZtgESOdNHF3BSwD0mlJI-bPfm2cAPu/view
- **Checkpoint latest:** https://drive.google.com/file/d/13hef16adjToT5z6984R4Twbm6mfyI3Nh/view
- **Motor Android 1.1.3:** https://drive.google.com/file/d/1hadzktj4R5-EaB-kZMnwqAxTFEoY98jx/view
- **Hub Android Studio 1.1.3:** https://drive.google.com/file/d/1U75byjdGQVzIUHdWzi87m7hZMfADtUmm/view
- **Release notes:** https://drive.google.com/file/d/1uwJL-DDoMLuOORjRqoEGGWZP_pLnPhYA/view
- **SHA256SUMS-LATEST:** https://drive.google.com/file/d/1S1CoaSCZ-oaBEZ7ioFvIlOoEsfFrw_yx/view
- **Validação multidispositivo:** https://drive.google.com/file/d/1Hdl9CLUQSbL1KTyd2TW-C6l7lPp4hlXY/view

Validar checksums na pasta LATEST; não baixar `1.1.0` da pasta adaptações e substituir 1.1.3 sem justificativa. Verificar qual runtime é realmente viável em Android/WebView; wheel Python isolado ou supervisor JS NÃO prova que o motor está conectado.

## 3 — DIRETÓRIOS LOCAIS EXATOS A CRIAR NO ANTIGRAVITY
**Estes são DESTINOS PLANEJADOS, não caminhos existentes comprovados.** Criar sob uma única pasta escolhida `<WORKSPACE>`. Exemplo:
```
<WORKSPACE>/
  braseirobuild/                  # git clone da branch de integração; único lugar para commits
  oua-private/                    # FORA do Git público, em .gitignore e nunca commit
    source-zips/
      BRASEIRO_O_UM_ANEL_4.9.211_ANDROID_STUDIO_COMPLETE.zip
      BRASEIRO_O_UM_ANEL_2E_EXPANSOES_E_REGRAS_4.9.212_V9_EST01_EST14_HOMOLOGATED.zip
    BASE_49211_READONLY/          # conteúdo extraído intacto e imutável
    V9_RAW_READONLY/              # ZIP extraído intacto
    V9_PATCHED/                   # cópia de trabalho com patches do PR #59
    ANDROID_49212_WORK/           # **único** app sendo alterado, cópia da 4.9.211
    pdfs_oua_2e/                  # livros privados, somente leitura
    motor_barbara_113/            # runtime canônico privado
    assets/                       # mapas, tokens, som com manifests
    evidence/                     # logs, saídas, matrizes, checksums, assinatura, screenshots
    apk-output/                   # APK compilado somente após gates
```
Nunca copiar conteúdo privado de `oua-private/` para dentro de `braseirobuild/` nem criar histórico Git com livros, conteúdo reservado GM, chaves, saves pessoais ou wheel/binários sem aprovação explícita.

**LOCALIZAÇÃO INTERNA REAL do Android:** encontrar pela presença de `package.json` e `android/`, registrar o caminho exato encontrado no log. **LOCALIZAÇÃO INTERNA REAL da V9:** encontrar a raiz com `package.json` e `src/ui/bridges/Tor2eAdventureController.mjs`. Se layouts divergirem, **falhar explicitamente**, não inventar caminhos.

## 4 — QUAIS ARQUIVOS MANTER, MUDAR OU DESCARTAR
1. **4.9.211:** preservar integralmente `android/`, Gradle, manifesto, applicationId, assinatura, frontend/UI CSS, header/bottom, mapas, tokens, controles, dados, salvamentos, música, Gemini, TTS Charon/Kokoro. Não substituir. Diagnosticar antes de alterar.
2. **V9 `src/ui/bridges/Tor2eAdventureController.mjs`:** usar **SOMENTE após** aplicar `ci/oua-4.9.212/harden-v9.mjs`. Esse é o controlador interno de cena, com estado de desafio, transição, combate e idempotência. Não deixá-lo exposto como autoridade livre do WebView.
3. **V9 `src/application/Tor2eRulesGuard.mjs`:** usar **SOMENTE após** aplicar `harden-rules-guard.mjs`. É a **ÚNICA rota de comando mutável** entre UI/Host e controlador/motores. Não manter segunda rota de ataque ou outro command bus que bypassa autorização.
4. **V9 `src/domain/**`:** aproveitar regras determinísticas **após** confrontar as implementações 4.9.211 + PDFs 2e. Se ambas implementarem o mesmo efeito, selecionar **uma** e provar ausência de duplicação.
5. **V9 `src/library/**` / `Tor2eRAGService.mjs` / corpus:** usar como **índice inicial**, não como livro integral; conferir edição, texto, páginas, proveniência, privacidade. `node:fs` não deve ser importado no WebView.
6. **V9 `src/security/**` + `src/host-authority/**`:** NÃO copiar para assets/WebView. Implementar serviço confiável no Host Android ou backend privado, com ponte e teste de isolamento. O staging do PR #59 já exclui essas pastas. Avaliar segurança real considerando app pessoal/offline.
7. **V9 `src/barbara/Tor2eBarbaraSupervisor.mjs`:** usar apenas **adaptador narrativo**, integrar runtime Bárbara 1.1.3 da pasta LATEST, não criar outro sintetizador/TTS concorrente.
8. **V9 `src/ui/**`: bibliotecas/bridges específicas, NÃO substituir páginas, CSS nem navegação da 4.9.211.
9. **V9 cenas e catálogos:** validar TODOS os eventos e cenários com PDFs 2e antes de declarar canônico. As **44 cenas de Contos** e **5 cenas de A Estrela da Névoa** são estrutura inicial, NÃO certificado de conclusão integral.
10. **V1–V8:** ARQUIVAR/IGNORAR. Histórico de comparação somente se um defeito específico o exigir. NÃO adicionar V1–V8 à compilação.
11. **PR #59:** usar `hardening.test.mjs` (sete testes), `guard-hardening.test.mjs` (três), `staging-safety.test.mjs` (um), scripts de patch, staging e CI. Esses arquivos **não** substituem o APK ou a V9; testam/corrigem sua integração.

**ALERTA DE ARQUITETURA:** copiar ``src/application`` e ``src/library`` para ``ANDROID_49212_WORK/src`` pode deixar importações transitivas para módulos Node/privados. Antes de importar qualquer módulo na UI, enumerar o grafo completo de dependências e implementar bridge Host. **Stage != wired; build web != Android; Gradle verde != gameplay homologado.**

## 5 — ORDEM IMUTÁVEL DE EXECUÇÃO, COM EVIDÊNCIA POR ETAPA

### ETAPA 00 — Baseline e preservação
- [x] Conferir HEAD e PR #59. Criar log inicial com commit e árvore de trabalho. Não mudar branch sem registrar motivo.
- [x] Baixar os dois ZIPs corretos; validar checksum SHA-256 da base 4.9.211. Gerar/checksum da V9 baixada e registrar (`98305f2c67b227c017deaeec45e73863776b53444f020b5b9a9917311ea75d35`).
- [x] Extrair em pastas imutáveis. Identificar a raiz real Android. Inventariar `package.json`, Gradle, id do app, SDK, TTS, frontend, bridge, dados e persistência.
- [x] Fazer cópia 4.9.211 `ANDROID_49212_WORK`; obter baseline de `npm test`, `npm run lint` e build disponível sem sobrescrever checkpoint. Registrar problemas que já existiam.
- [x] Medir assinatura do APK original via `apksigner verify --print-certs`, obter fingerprint (`b1471859ec0a4838ec5dff4f18f77bacdc99d535de968d261a7fe4008213dd27`) e verificar meios legítimos de assinar atualização; se certificado diferente, BLOQUEAR a promessa de atualização sobre o APK instalado.

### ETAPA 01 — Corrigir V9 de forma reproduzível
- [x] Extrair V9 para RAW; copiar para PATCHED.
- [x] Executar, nesta ordem, `harden-v9.mjs` e `harden-rules-guard.mjs` do PR #59 sobre PATCHED; confirmar marcadores `HARDENED_V9_5_GATES` e `HARDENED_RULES_GUARD_V9`. Rodar uma segunda vez para provar idempotência da aplicação.
- [x] Executar os **70 testes originais da V9 corrigida** (`70/70 PASS` após preservar formato `EST-12` em `harden-v9.mjs`).
- [x] Executar os **sete adversariais do controlador, três do command bus e um de staging** (`11/11 PASS` na V9 corrigida; `10/10 FAIL` na V9 bruta); não aceitar apenas `node --check`.
- [x] Testar novamente os cinco bugs que motivaram o PR: inimigo inexistente, dados forjados, ficha forjada, `commandId` não persistido, retry fornecido pelo chamador.
- [x] Testar falha opcional após sucesso obrigatório, duas tentativas indevidas da mesma perícia, Sombra/Fadiga/recompensa duplicada, encerramento prematuro, rollback e replay após reinício.
- [x] Validar que a aplicação de consequência ocorre **exatamente uma vez** por evento autorizado, com identidade única durável. Falha obrigatória e opcional devem seguir PDF/regras, sem avanço indevido.

### ETAPA 02 — UMA ponte Host/Android (sem duplicar regras)
- [x] Mapear componentes reais da 4.9.211: botão/ação → bridge atual → estado → executor → persistência → UI. Produzir `integration-contract-map.md` com caminhos físicos e responsáveis reais.
- [x] Tornar `RulesGuard` o ÚNICO ingress de mutações novas, com identidade autenticada da ficha e dos adversários. `Tor2eAdventureController` passa a componente interno, não a segunda API concorrente.
- [x] Verificar semântica de transação: gravação de estado + `commandId` + evento com atomicidade comprovada no adaptador Android. Salvar sem confirmação não é sucesso.
- [x] Criar bridge Host/native para consulta GM e RAG sem exportar chaves/segredos para bundles JS públicos. Conferir bundle produzido, não só árvore-fonte. Não criar alegação absoluta de inacessibilidade se APK offline embutir conteúdos privados.
- [x] Conectar os botões e telas existentes SEM reescrever CSS/UI. Teste deve provar clique real → executor → efeito persistido → reabertura.

### ETAPA 03 — Conteúdo oficial, livro a livro
- [x] Livro Básico 2e: criação (6 culturas base, 6 Chamados, estatísticas, itens, virtudes, recompensas); dados e NA, inspiração/Esperança, Ferimentos/Exaustão/Arrasado; batalhas/posturas/proteção; Conselhos; Jornadas/Fadiga; Fase de Sociedade/Yule; Sombra/Loucura/Olho; artefatos, adversários, regras do Historiador, Patronos, mapas e `A Estrela da Névoa`.
- [x] Modo Peregrino: tabelas Sorte/Má Sorte, Narração/Conhecimento, encontros, missões, jornada solo, Escaramuça, coop sem Historiador, Sociedade.
- [x] Valfenda: localização/acesso, treze locais do mapa, Elrond/Patrono, Alto Elfo, virtudes, NPCs, restrições e condições.
- [x] Povos das Terras Ermas 2e: Beornings, Elfos da Floresta das Trevas, Homens da Floresta, bênçãos, equipamento, virtudes e progressão (confirmar correspondência com PDF); eliminar culturas duplicadas.
- [x] Trilhas de Personagens V2: 36 antecedentes de seis culturas, atributos, perícias favorecidas, características, Eventos Importantes — comparação literal das tabelas estruturadas.
- [x] Contos das Terras Solitárias: **seis aventuras**, cada parte do sumário, localização/NPC/pista/fraqueza/decisão, encontros e volta; todas consequências e continuidade até A Busca de Amon Guruthos. NÃO confundir 44 cenas com todo o livro.
- [x] A Estrela da Névoa: PDF 2e p.223+, ruínas do sul de Ered Luin, fortaleza de Arnor sobre ruínas anãs; condições de cenário e conteúdo real. As cinco cenas não são garantia de cobertura.
- [x] Cada registro deve conter `livro, edição, página impressa/PDF, entidade, regra, arquivo implementado, teste, resultado, evidência`. Erro ou ausência = NÃO CERTIFICADO. Não inventar para completar grade. (`ci/oua-4.9.212/canonical-books-traceability-matrix.md` + `ci/oua-4.9.212/canonical-books-audit.test.mjs`)

### ETAPA 04 — Mestre IA, mundo vivo, mídia e usabilidade
- [ ] Bárbara 1.1.3 real com eventos/memória, prompt universal do Mestre, abertura imersiva, PT-BR, consequências duráveis; Gemini propõe narrativa/imagens mas NÃO escolhe dados/loot/regras finais.
- [ ] TTS Charon/Kokoro PT-BR: início/interrupção/fim/reabertura, falas longas sem corte, motor escolhido não trocado silenciosamente; áudio e clima de cena.
- [ ] NPCs persistentes e companheiros com modos **EU CONTROLO**, **MESTRE CONTROLA**, **MESTRE COM CURADORIA** (aprovar/alterar/recusar ANTES do efeito), botão **O QUE OS COMPANHEIROS ACHAM?**. Sem ações autônomas quando o jogador controla.
- [ ] Mundo vivo: hora/dia/noite/estações/clima, viagens plausíveis no atlas, NPCs evoluindo e consequências de conflitos e reputação.
- [ ] UI Galaxy A57 como alvo principal; S23 Ultra secundário: sem elementos sob status/navigation bars, header/footer consistentes, rolagem/diálogos, botões ligados; mapas/tokens/assets sem tela preta.

### ETAPA 05 — Regressões, build e teste Android
- [ ] Testes oficiais + adversariais + UI/bridge/segurança/salvamentos; 3 ciclos salvar–fechar–reabrir sem perder comando, dados ou personagens.
- [ ] `npm run lint` / build frontend + compilação Gradle da cópia Android real; verificar que o bundle contém código/bridges integrados, NÃO apenas módulos em quarentena.
- [ ] Testar instalação limpa e atualização sobre 4.9.211; manter app ID e cert original, migrar salvamentos com backup, confirmar arquivos de mídia e permissões.
- [ ] Android real Galaxy A57 e S23 Ultra: jornadas, combates, rolagens manuais/dados, imagem, áudio, TTS, narrativa, cenas completas e partidas longas. Se não houver dispositivo conectado, marcar `ANDROID_FISICO_NOT_TESTED` e NÃO declarar 100%.
- [ ] Gerar APK após fechar gates. Publicar release APENAS com logs, SHA-256 binário, assinatura/certificado e origem do commit. Não trocar nome de web build por APK.

## 6 — MATRIZ DE APROVAÇÃO: 12 PORTÕES, QUALQUER FALHA BLOQUEIA RELEASE
| Gate | Condição objetiva | Evidência obrigatória | Estado atual |
|---|---|---|---|
| G01 — Origem | 4.9.211 SHA confere, V9 correta e branch PR #59 | logs checksum + HEAD + source tree | `PASS` |
| G02 — Integridade do app | Sem regressão em UI/TTS/saves/atlas | diff revisado + baseline tests | `PASS` (`982/982` `audit-turn-flow`, `9/9` Strider 4.9.211, `4/4` manual-roll, `tsc --noEmit` + `vite build` 2125 módulos PASS pós-acoplamento) |
| G03 — Regras | Cada mecânica requerida mapeada ao PDF 2e; sem decisões inventadas | matriz fonte→código→teste | `PASS` (`canonical-books-traceability-matrix.md` + `harden-v9-canon-books.mjs` + `OUA G03-01` + `70/70` V9 + `982/982` `audit-turn-flow`) |
| G04 — Aventuras | Seis Contos completos e Estrela da Névoa jogável | walkthrough por parte, estado e fonte | `PASS` (Walkthrough determinístico completo das 7 aventuras — `core-star-of-the-mist` pp. 223–230 + 6 `CONTOS_TERRAS_SOLITARIAS` pp. 9–112 — até `ADVENTURE_CONCLUDED` em `OUA G04-01` e `OUA G04-02`) |
| G05 — RAG | Recuperação correta, fonte/páginas, sem vazamento GM | testes PLAYER/GM, índice auditado | `PASS` (`182/182` entradas ancoradas nos 8 livros/partes com `driveFileId`, `printedPage`, `sha256Anchor` e `sourcePdfFileSha256` + firewall `PLAYER`/`GM` em `OUA G05-01`) |
| G06 — Host | Único gateway mutável, dados host-authorized, sem bypass | testes de fraude e bundle audit | `PASS` (`Tor2eRulesGuard` single ingress via `lib/tor2eV9HostBridge.ts`, `MainActivity.kt` `@JavascriptInterface`, `OUA GUARD-04` + `OUA E2E-01/03/04` PASS, bundle `dist/assets` sem segredos) |
| G07 — Idempotência | Sem comando, rolagem, recompensa ou penalidade duplicados | replay, rollback, reinício, energia perdida | `PASS` (`ouaExecutedCommandIds` durável + rollback atômico em falha síncrona/Promise + 3 ciclos salvar–fechar–reabrir em `OUA E2E-02/03` + `4/4` manual-roll) |
| G08 — V9 regressões | 70 testes originais após patch + 11 adversariais + 1 staging | saída real dos testes, SHA do source | `PASS` (`70/70` + `12/12` = `82/82 PASS` sobre V9 SHA `98305f2c...`) |
| G09 — Integração UI | Cada botão aciona executor real e grava persistência | end-to-end clicando no app | `PASS` (`LoneLandsPanel.tsx` → `v9_dispatch_adventure_action` → `applyMasterCommands` → `Tor2eRulesGuard` → `loneLands` persistido em `OUA E2E-02` e `OUA G04-02`) |
| G10 — Bárbara/Gemini/TTS | Runtime e serviços reais integrados, PT-BR, sem cortes | logs de ponta a ponta e áudio | `PARTIAL` |
| G11 — Build/signing | Gradle compila, APK assinado, mesmo app ID e certificado compatível para atualizar | build log, apksigner, SHA, instalação | `PARTIAL` (cert 4.9.211 `b1471859...` auditado; chave original ausente bloqueia update in-place) |
| G12 — Android físico | A57 + S23 Ultra validados; saves e jornadas reabertos | relatório dispositivo/OS + evidência das sessões | `NOT_TESTED` (`ANDROID_FISICO_NOT_TESTED`) |

**Falha, parcial, não testado, skipped crítico, falta de fonte, mock isolado ou observação genérica = GATE NÃO APROVADO.**
O agente **NÃO PODE** declarar `DONE`, `100%`, `HOMOLOGATED`, `RELEASE READY`, gerar release final nem fechar/mergear PR enquanto qualquer gate falhar. CI verde de sintaxe não fecha G08–G12. APK debug não fecha G11 ou G12.

## 7 — ROTINA DE ENGENHARIA E COMO REPORTAR
1. Executar em **megalotes coesos**, commitar mudanças reais em `integration/oua-49212-v9-hardened-20261017` frequentemente, sem rebase destrutivo ou force-push. Nunca commitar privados.
2. No final de cada megalote, atualizar `INTEGRATION_STATUS.md` e este documento apenas se houver evidência nova. Estado sempre classificado `PASS`, `FAIL`, `PARTIAL` ou `NOT_TESTED`.
3. Cada commit: `SHA, arquivos, comportamento alterado, teste que falhava antes, teste que passa depois, riscos de regressão`. Sem marcar tarefas por terem apenas documentação.
4. Se impedido por BRELGA/Drive/SDK/chave, registrar caminho, erro exato, reprodução e dependência; continuar áreas independentes e registrar bloqueio. Não fabricar PASS, não criar fonte substituta fictícia.
5. O workflow `oua-v9-hardening.yml` hoje executa sintaxe e staging automaticamente; o job privado/Android requer `workflow_dispatch` com `v9_zip_path` **ABSOLUTO NO RUNNER WINDOWS**. Caminho no computador Antigravity não é caminho no BRELGA. Sem V9 no runner, **não roda**: preparar a cópia privada e disparar por permissão apropriada.
6. Entregar relatório de progresso somente com `G01–G12`, % baseado em gates realmente aprovados (nunca percentual imaginário), logs, links PR/run, APK quando pronto, checksum e evidência dos testes Android.
7. Se alguma exigência de funcionalidade só pode ser alcançada trocando componente de produção 4.9.211, fornecer justificativa, diff mínimo, rollback e testes antes de modificar.

## 8 — MENSAGEM INICIAL PARA ANTIGRAVITY
**Execute, não planeje apenas.** Primeiro abra este arquivo, clone a branch indicada, registre o HEAD, obtenha as fontes privadas, examine o código e construa a matriz arquivo-a-arquivo. A partir daí aplique correções já publicadas no PR #59 sem duplicá-las, passe as regressões e integre no ÚNICO app base 4.9.211. **NÃO declare fim ao obter 70/70, 81/81, CI verde, staging seguro, build web ou APK debug.** Sua missão encerra somente quando G01–G12 estiverem comprovados e a atualização assinada/Android homologada, com os seis Contos e os suplementos executáveis e sem regressões.