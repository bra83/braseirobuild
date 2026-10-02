# MASTER — RUN164 INTEGRATION CANDIDATE

## Base mecânica
RUN163: `BRASEIRO_DND_6.6.2_RUN163_HEROES_FAERUN_MEGA_PTBR_PATCH.zip`
SHA-256: `a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813`

## Base Android
RUN109 Android Studio recomposto a partir das quatro partes canônicas.
SHA-256 recomposto: `be1565cf347c37c46b4c2d33d0053bb25c71d382fdaa8f39752f0397c4d1cad7`

RUN110 TTS fallback delta:
SHA-256: `e0e02a1711012e8b251caab3133570252e7fcec09f077aae3cd1e37a3a19803a`

## ENGINE-001
Drive ID: `1Bz9nXDJ4ZOOYBY38cO0r-1y8NuI_DjRI`
SHA-256: `ac5b68c4d33a230d86cb2109f6231d079d4541a7d757dc4fef9d3e97b5aca0c7`
Status aceito: `PARTIAL`.
Nenhum delta Java. Gaps source-fidelity continuam explícitos; `BOOK_COMPLETE=false`.

## GAMEPLAY-001
Drive ID: `1xBAl8F926owmHHnDVokIrR-vWpNyGDfJ`
SHA-256: `a4669e90434ccb0b1e769ed650084dcbbd8d46b0935de57c736d239d06678832`
Integrado: `WorldStateEngine.java`, novo `GameplaySandboxRuntimeEngine.java` e 3 testes.

## ANDROID-001
Drive ID: `1qURkJK1DljGqoWTQl5-FskHP63S4ZwGn`
SHA-256: `90a7e7a6bd9fa9a9c18a6122a87b8d8ee9b6e454fa4448c6adb333e4a461624d`
Cinco arquivos Android aplicados.

## Hook MASTER
`MasterOrchestrator.java` recebeu somente nove delegadores GAMEPLAY-001:
- gameplaySandboxContext
- previewGameplayInput
- commitGameplayInput
- resolveGameplayEberronFramework
- resolveGameplayFaerunFaction
- resolveGameplayFaerunDeityRenown
- beginGameplayTravel
- resolveGameplayFaerunTravelService
- gateGeminiGameplayProposal

Nenhuma regra mecânica foi duplicada no orquestrador.

## Overlay de integração
Nome: `BRASEIRO_DND_6.6.2_RUN164_INTEGRATION_CANDIDATE_OVERLAY.zip`
SHA-256: `bb62fbbf5a818fd48197b086141b8ee6dfe8f395e697c297dc6300cee27470ce`

Ordem de reconstrução:
1. RUN109 completo;
2. RUN110 patch;
3. RUN163 mecânico;
4. GAMEPLAY-001;
5. ANDROID-001;
6. hook MASTER;
7. metadados ENGINE-001.

## Invariantes
- PT-BR preservado;
- `auto_rolls=false`;
- atomicidade preservada pelos gates herdados;
- RUN147 preservado;
- Forgotten Secrets preservado;
- Runeterra preservado;
- Mystara fora do fluxo atual;
- `BOOK_COMPLETE=false`;
- nenhum APK/AAB gerado.
