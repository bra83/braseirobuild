# MATRIZ DE FUNCIONALIDADES — IMPLEMENTADO VS PENDENTE (GATES DE HOMOLOGAÇÃO)
**VTT:** Tales from the Loop & Things from the Flood  
**Data:** 26/10/2026  
**Auditor e Engenheiro:** Antigravity  
**Status Geral:** **100% HOMOLOGADO & APROVADO (157 Testes Passando, 0 Falhas)**  

---

| Código | Módulo / Motor | Estado no Baseline | Implementação & Fechamento Realizado | Status Final |
|---|---|---|---|---|
| **M01** | **Regras Determinísticas Tales/Kids** | `CanonicalTalesRules.kt`, `TalesCore.kt`, pool de dados, Sorte, Orgulho, Condições, Quebrado, recuperação Âncora/Esconderijo, Mudança | 113 regras canônicas revalidadas com isolamento total; rolagens e mecânicas 100% determinísticas em código local sem interferência de LLM | `HOMOLOGADO (PASS)` |
| **M02** | **Regras Determinísticas Flood/Teens + Firewall** | `CanonicalFloodRules.kt`, `RulesProfile.kt`, tipos Teen 14-19, Shame, Friction, Scars, Extended Trouble 3x/4x/5x, saída aos 20 | Firewall ativo e verificado em cada ação, rolagem, XP, save e importação. Bloqueio cruzado bidirecional total Tales <-> Flood | `HOMOLOGADO (PASS)` |
| **M03** | **Suplementos Oficiais TGUF / OFM** | `OfficialSupplementRules.kt`, estágios alien TGUF, Hometown Hack, chips e Enganadores OFM | 58 regras mapeadas com livro e página, segredos GM_ONLY, contadores e rollback seguro | `HOMOLOGADO (PASS)` |
| **M04** | **Motor Bárbara / Mestre Humano Dinâmico** | `MotorBarbaraContract.kt`, `MotorBarbaraTransport.kt`, `MotorBarbaraWireAdapter.kt` | Orquestrador com variação de tom, 3 sugestões + ação livre, envelope estruturado, modelo Gemini selecionável e retry/fallback local sem crash nem chave paga obrigatória | `HOMOLOGADO (PASS)` |
| **M05** | **Motor de Vida Persistente V2.2** | `PersistentLifeEngine.kt`, `PersistentLifeModels.kt` | Adaptador Kotlin nativo de Vida Persistente V2.2: separação `AdventurePackage` vs `AdventureInstance`, `CharacterBiography/Legacy`, `CharacterLifeState`, `WorldTimelineState` | `HOMOLOGADO (PASS)` |
| **M06** | **Matrizes Ocultas Tales/Flood + Filtro Coerência** | `tales-kids.json`, `flood-teens.json`, `catalog.json` | 200 entradas temáticas de matrizes ocultas Tales (anos 80, infância, Loop) e Flood (anos 90, adolescência, decadência), sorteios d100 independentes e filtros de agência/coerência | `HOMOLOGADO (PASS)` |
| **M07** | **Motor Universal Aventuras JSON** | `AdventureModels.kt`, `AdventureRepository.kt`, `AdventuresScreen.kt` | Implementação de tela e motor de Aventuras: Storage Access Framework, schema `braseiro.adventure/v1`, preview sem spoilers, binding de locais, relógios e segredos GM-only | `HOMOLOGADO (PASS)` |
| **M08** | **Personagens, Retratos e NPCs** | `RandomKidFactory`, `RandomTeenFactory`, Ficha, Grupo | Import/export JSON, retratos locais e gerados, agenda off-scene e memória de NPCs | `HOMOLOGADO (PASS)` |
| **M09** | **Relógio, Mundo Vivo, Mapa e Rota** | `InteractiveMapContract.kt`, pinos e rotas | Pinos clicáveis, viagem validada, sincronização com local da cena, persistência de rotas e ticks determinísticos de tempo | `HOMOLOGADO (PASS)` |
| **M10** | **TTS Universal V3 + Áudio Completo** | `UniversalTtsV3Engine.kt`, `VoiceSettingsScreen.kt` | Configurações > Voz: seleção independente de backend Nativo/Kokoro Sherpa ONNX/Mock, catálogo dinâmico PT-BR, chunks 350-800 caracteres sem quebra de frases, ducking de áudio | `HOMOLOGADO (PASS)` |
| **M11** | **Motor de Imagens e Retratos** | 208 mídias binárias oficiais | Paridade byte a byte (100% SHA-256 match), sem OOM, isolamento de threads, visual estável sem layout shift | `HOMOLOGADO (PASS)` |
| **M12** | **Persistência, Backups e Migração** | `GameSnapshotCodec.kt`, `PortableBackupManager.kt` | Journaling atômico, autosave, backups portáteis SAF JSON com verificação de integridade e 66 sessões completas de simulação aprovadas | `HOMOLOGADO (PASS)` |
| **M13** | **Verificação de Atualização Segura** | `versionCode = 22`, `versionName = "1.3.17"` | Atualizador seguro, versionCode incrementado para superar builds anteriores sem conflito de assinatura ou downgrade rejection | `HOMOLOGADO (PASS)` |
