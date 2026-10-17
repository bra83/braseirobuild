# Mapa de Contrato de Integração — Braseiro O Um Anel 2e `4.9.212` (ETAPA 02)

Documento de rastreabilidade obrigatório da **ETAPA 02** conforme [`ANTIGRAVITY_ORDEM_MESTRA_FONTES_UNICAS_E_GATES.md`](./ANTIGRAVITY_ORDEM_MESTRA_FONTES_UNICAS_E_GATES.md), mapeando como os módulos V9 endurecidos (`src/tor2e-v49212`) se conectam ao aplicativo Android canônico `4.9.211 -> 4.9.212` **sem duplicar estado, sem substituir UI/CSS/TTS/saves e com ingresso único obrigatório**.

---

## 1. Fluxo Único de Mutação e Persistência (`G02` + `G09`)

```
[UI React: components/LoneLandsPanel.tsx]
       │
       ▼  onApplyCommands([{ type: 'v9_dispatch_adventure_action', commandId, ... }], reason)
[App.tsx -> applyMasterCommands (lib/master/commands.ts)]
       │
       ▼  case 'v9_dispatch_adventure_action'
[lib/tor2eV9HostBridge.ts -> dispatchV9AdventureCommand()]
       │  • Constrói ficha autorizada pelo Host a partir de Character.sheet (lib/db.ts)
       │  • Constrói autorizador de combate a partir de Character.sheet.combat
       │  • Verifica PIN do Historiador via window.BraseiroAndroid.tor2eHostAuthority (MainActivity.kt)
       ▼  ÚNICO INGRESSO V9
[src/tor2e-v49212/application/Tor2eRulesGuard.mjs -> guard.dispatch(command, envelope)]
       │  • Bloqueia replay durável por commandId (ouaExecutedCommandIds / v9ExecutedCommandIds)
       │  • Isola GM_HELP (somente-leitura, zero mutação de estado) de PLAYER_ACTION (mutante)
       │  • Encapsula internamente Tor2eAdventureController + Tor2eDiceEngine + Tor2eCombatEngine + Tor2eRAGService
       │  • Executa commit transacional síncrono (rejeita Promise assíncrona) com rollback atômico integral
       ▼
[MasterState.company.loneLands + Character.sheet]
       │
       ▼  saveMasterTransaction (App.tsx)
[IndexedDB (saveCanonicalTransaction em lib/db.ts) + EmergencyMirror + BraseiroNative.saveRecoveryBackup (MainActivity.kt)]
```

---

## 2. Mapa de Arquivos do Host Android (`4.9.211 -> 4.9.212`)

| Arquivo no Host Android | Papel Canônico na `4.9.211` | Contrato na `4.9.212` (ETAPA 02) |
|---|---|---|
| `App.tsx` | Raiz da aplicação React, estado da mesa (`campaign`, `characters`, `npcs`), transação `saveMasterTransaction` e despacho `onApplyCommands` | **Preservado 100% intacto.** Todas as ações V9 entram pelo fluxo existente `onApplyCommands -> applyMasterCommands -> saveMasterTransaction`. |
| `lib/master/types.ts` | Tipos canônicos do `MasterState`, `LoneLandsState` e união discriminada `MasterCommand` | Estendido de forma retrocompatível em `LoneLandsState` (`v9ExecutedCommandIds`, `v9AdventureSnapshot`, `v9AdventureView`, `v9ClaimedAdventureRewards`, `v9LastResolution`, `v9LastHelpResponse`) e em `MasterCommand` (`v9_dispatch_adventure_action`). Saves `4.9.211` continuam carregando sem migração destrutiva. |
| `lib/master/commands.ts` | Executor transacional único `applyMasterCommands` / `applyOne` | Adicionado `case 'v9_dispatch_adventure_action':`, que delega exclusivamente a `dispatchV9AdventureCommand` (`lib/tor2eV9HostBridge.ts`), sincroniza `nextState` e `nextCharacters` e registra auditoria em `campaign.events`. |
| `lib/tor2eV9HostBridge.ts` | **Novo adaptador Host/WebView -> V9 (`ETAPA 02`)** | **Único ponto do app que importa `Tor2eRulesGuard.mjs`.** Constrói `authorizedCharacterSheet` e `authorizeCombatAction` a partir das fichas reais (`Character[]` de `lib/db.ts`), valida `GM_HELP` na ponte nativa e garante rollback atômico síncrono. |
| `lib/db.ts` | Persistência canônica IndexedDB (`saveCanonicalTransaction`), `EmergencyCampaignMirror` e tipos `CampaignState` / `CharacterSheet` | **Preservado sem banco paralelo.** O snapshot V9 (`v9AdventureSnapshot`) e a lista durável de comandos (`v9ExecutedCommandIds`) são gravados dentro de `campaign.masterState.company.loneLands` na mesma transação IndexedDB + backup nativo. |
| `lib/barbaraBridge.ts` | Ponte de supervisão narrativa, áudio, POIs e interpretação de dados com o Motor Bárbara `1.1.3` | **Preservado intacto.** Não há segundo supervisor nem motor paralelo. |
| `components/LoneLandsPanel.tsx` | Painel de mecânicas de *Contos das Terras Solitárias* (Herdeiro, Estelang, Forodwaith, Conselhos, Amon Guruthos) | Adicionada seção **6 Aventuras Canônicas · Máquina de Cenas Transacional** no topo do painel, mantendo todo o CSS/layout original e despachando `START_ADVENTURE`, `PERFORM_SCENE_CHECK`, `APPLY_FAIL_FORWARD`, `SCENE_COMBAT_ATTACK`, `TRANSITION_SCENE` e `CONCLUDE_ADVENTURE` via `v9_dispatch_adventure_action`. |
| `components/JourneyResolutionPanel.tsx` | Painel de resolução de Viagem (marcha, eventos, papéis, Forodwaith) | **Preservado intacto** (regras de viagem continuam na máquina principal `lib/rules/journey.ts`). |
| `components/CombatRulesPanel.tsx` | Painel de resolução de Combate (posturas, ataques, proteção, tarefas) | **Preservado intacto** (encontros de cena das 6 aventuras V9 também podem ser resolvidos na seção dedicada de `LoneLandsPanel.tsx` com arma e proficiência autorizadas pela ficha real). |
| `components/SupplementRulesPanel.tsx` | Painel de regras de suplementos (Modo Peregrino, Valfenda, Povos das Terras Ermas, Trilhas) | **Preservado intacto.** |
| `android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt` | Host nativo Android (`WebView`, `BraseiroNative`, TTS Kokoro/Charon, `BarbaraSupervisor`, `AndroidKeyStore`, `saveRecoveryBackup`) | Adicionados `@JavascriptInterface verifyTor2eGmAccess`, `setTor2eGmPin` (usando `AndroidKeyStore` AES/GCM em `privatePreferences`) e `authorizeTor2eCombatAndSheet`, expostos em `window.BraseiroAndroid.tor2eHostAuthority` e `window.__TOR2E_HOST_AUTHORITY_BRIDGE__`. |
| `src/tor2e-v49212/ui/bridges/Tor2eHostGmBridge.mjs` | Ponte WebView-safe gerada por `stage-v9-in-android.mjs` | Substitui `Tor2eGmSecretsVault.mjs` no bundle WebView. Zero segredos, zero hashes hardcoded e zero `src/security` ou `src/host-authority` no cliente (`G10`). |
