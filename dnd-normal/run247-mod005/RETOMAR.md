# GUIA DE RETOMADA - BRASEIRO D&D 5E NORMAL

## Ponto de Partida Confirmado
- **Branch:** `implementation/dnd-v1-core-bastions-event-megablock-20261016`
- **Módulo:** `dnd-normal/run247-mod005`
- **Diretório de Trabalho:** `C:\Users\bcruz\Downloads\braseirobuild_v1_worktree\dnd-normal\run247-mod005`
- **Ambiente Java:** `C:\actions-runner\_work\_tool\Java_Temurin-Hotspot_jdk\17.0.20-101\x64`
- **Android SDK:** `C:\Users\bcruz\AppData\Local\Android\Sdk`

---

## Comandos Rápidos de Validação

### Executar Suítes de UI e Backup21
```cmd
set JAVA_HOME=C:\actions-runner\_work\_tool\Java_Temurin-Hotspot_jdk\17.0.20-101\x64
set PATH=C:\actions-runner\_work\_tool\Java_Temurin-Hotspot_jdk\17.0.20-101\x64\bin;%PATH%
gradlew.bat testDebugUnitTest --tests com.braseiro.dnd5e.CharacterCreationUiRobolectricTest --tests com.braseiro.dnd5e.SessionContinuityUiRobolectricTest
```

### Executar Suítes de Combate e Multiataque XMM
```cmd
gradlew.bat testDebugUnitTest --tests com.braseiro.dnd5e.ExplorationCombatUiRobolectricTest --tests com.braseiro.dnd5e.PlayableUiRobolectricTest --tests com.braseiro.dnd5e.XmmMultiattackHostRobolectricTest
```

### Executar Suítes de Bastiões e Classes PHB 2024
```cmd
gradlew.bat testDebugUnitTest --tests com.braseiro.dnd5e.Bastion2024SpecialFacilitiesRobolectricTest --tests com.braseiro.dnd5e.Core2024ClassRuntimeRobolectricTest --tests com.braseiro.dnd5e.Dmg2024RulesRobolectricTest --tests com.braseiro.dnd5e.XmmManualArbitrationBridgeRobolectricTest
```

---

## Próximo Bloco Prioritário
1. **Multiataques Híbridos XMM:** Estender `XmmMultiattackHostPlan` e `AppDb.applyCompiledStatblockMultiattack` para admitir variantes com componentes de magia ou uso especial (ex: Baforadas e Magias de Dragão) mantendo a transação SQLite atômica.
2. **Subclasses PHB 2024:** Finalizar a expansão das 48 subclasses com recursos ativáveis no nível de seleção de subclasse (nível 3).
3. **GAP Ledger:** Atualizar `docs/dnd-v1-3core/GAP_LEDGER.csv` com novos IDs `VERIFIED_E2E`.
