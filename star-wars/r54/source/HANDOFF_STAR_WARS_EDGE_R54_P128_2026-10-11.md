# Braseiro Star Wars — R54 P128 — checkpoint integrado

**Base preservada:** `BRASEIRO_STAR_WARS_EDGE_R54_P127_SOURCE_CHECKPOINT.zip` (R54 P127). Nenhum retorno a P113/P126. Android `versionCode=10128`, `versionName=1.0.128`, shell offline `data-version=1.0.128`.

## Implementação efetiva deste bloco

1. **Cinco aventuras conectadas ao runtime UAE persistente:** novo `publishedAdventureUaeBridge.mjs`, acionado pelo registro real de adaptadores em `publishedAdventureRegistry.mjs`. Início, escolhas, testes, ativação e desfecho de encontros, aplicação de recompensas e arquivamento agora sincronizam `world.publishedAdventureRuntime` no mesmo snapshot de campanha. O estado legado continua responsável pelas regras e efeitos específicos das aventuras, preservando compatibilidade.
2. **Objetivos, gatilhos e transições:** objetivos de conclusão por episódio gerados exclusivamente a partir de `node.episode` já existente; conclusão do episódio ao entrar em episódio posterior; conclusão da aventura fecha os objetivos pendentes. Eventos `NODE_ENTER`, `NODE_TRANSITION`, `ADAPTER_*`, `OBJECTIVE_COMPLETED`, `ADVENTURE_COMPLETED` e `ADVENTURE_ARCHIVED` auditáveis. Condições legadas não interpretáveis pela UAE recebem evento explícito de reconciliação, sem inventar mecânica ou quebrar caminhos válidos.
3. **Persistência e migração:** snapshots restaurados sem repetir início/recompensas; migração de saves legados sem UAE; auditoria de divergências de cena externas/importadas; prevenção de colisões de IDs de evento quando journals antigos têm lacunas. Arquivamento e resolução de consequência tornados transacionais.
4. **UI Android:** painel da aventura mostra objetivos de episódio/final com status e progresso, por projeção pública `status.uae`. Episódios futuros, flags do Mestre, eventos internos, pistas ocultas e relógios não explicitamente públicos não são expostos.
5. **Equipamento/qualidades:** efeitos de attachments e Mods agora são calculados a partir do catálogo mecânico autorizado, ignorando efeitos forjados no save, mods desconhecidos e excedentes; qualidades graduadas são consolidadas sem duplicatas; correção de bug real em `weaponHasQuality`/`weaponQualityRank` que confundia **Imprecisa** com **Precisa** (substring). Mods legítimos continuam preservados na remoção/reinstalação.
6. **Bundle offline:** regenerado a partir das fontes locais canônicas, **71 módulos**, metadados `1.0.128-r54-p128`. Builders raiz e `tools/` byte-idênticos. Kokoro/TTS nativo não alterado.

## Testes executados

- **1.264/1.264 PASS**, 0 falhas, 0 pulados, 246 arquivos de teste, incluindo 6 suítes de stress de 33 sessões.
- **13 testes P128 novos** cobrindo cinco aventuras, fim a fim e arquivamento das cinco, reabertura, migração, journal, reconciliação, objetivos, UI anti-spoiler, mods e qualidades.
- `node tools/verify-final-source.mjs`: **PASS**; 121 JS/ESM, 8 Java (syntax gate; símbolos Android não resolvidos neste host), 77 JSON, 2 XML, zero segredos, Gradle wrapper íntegro.
- **Kokoro/TTS:** código nativo preservado; os testes de regressão de TTS estão incluídos na suíte completa. Não houve teste de voz em aparelho físico.
- **APK Android:** NÃO compilado/testado neste host (SDK, adb e sdkmanager ausentes). É necessário `assembleDebug` no GitHub self-hosted BRELGA ou Android Studio com JDK 17/SDK 36.

## Lacunas ainda não certificadas

- A integração UAE é um **journal canônico sincronizado**; os cinco adaptadores legados ainda executam as regras de cena e consequências específicas. Não alegar que a UAE seja a única fonte de estado ou que a conferência integral dos PDFs das cinco aventuras tenha sido concluída.
- `beyond-the-rim` mantém um método de encontro genérico não ativado (`encounterSpec` lança exceção). A capacidade anunciada por assinatura de função não prova cobertura de combate da aventura.
- **EQUIP.QUALITIES / EQUIP.ATTACHMENTS:** correções mecânicas concretas nesta rodada, mas a matriz de cobertura de livros permanece parcial. As **16 famílias de expansões EotE/AoR** marcadas como RAG/declaração no P126 não foram promovidas sem conferência das regras primárias. Não declarar cobertura 100%.
- Compilação APK, instrumentação Android, TTS real, longas sessões em aparelho e verificação de cada regra frente aos PDFs continuam pendentes.

## Retomada segura

1. Extrair o ZIP P128 completo e verificar SHA-256. Abrir a raiz com `settings.gradle.kts` no Android Studio.
2. Rodar `python3 tools/run-node-suite.py` e `node tools/verify-final-source.mjs`.
3. Compilar no self-hosted runner BRELGA (`./gradlew assembleDebug`, JDK 17, Android SDK 36); instalar no Galaxy S23 Ultra/A57 e validar save/reopen, áudio Kokoro e as cinco aventuras.
4. Confrontar PDFs de expansão EotE/AoR e aventuras com os contratos existentes; implementar famílias completas com testes e sem RAG em gameplay, sem regressão para esta baseline.

**Escopo:** este checkpoint é recuperável e testado no nível Node/fonte; **não é uma certificação de conclusão integral nem APK homologado**.
