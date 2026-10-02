# PATCH MANIFEST — ENGINE-002

BASE_BRANCH: dnd/master-run164-integration-candidate
BASE_COMMIT: b8e59f6c0a8d35bbc93865830359afecd0642d10
RUN164_CANDIDATE_SHA256: bb62fbbf5a818fd48197b086141b8ee6dfe8f395e697c297dc6300cee27470ce
QA_REFERENCE_COMMIT: f6f9864779414b406c4f4a6b3e1b7dba1c797032

CHECKPOINT: BRASEIRO_DND_6.6.2_ENGINE002_QA001_ATOMICITY_STRICT_DISPATCH_PATCH.zip
DRIVE_ID: 1giSedFL2dkjfXE51ngs84B95RRc96kbx
CHECKPOINT_SHA256: fb5975ecf25b33270b5c3601f2bec9a8d43fca8c9f4c14e88af7e509756671e3
PATCH_SHA256: 323892809682860d480cbc39f82786f53eb91d2c3ab51b1f840e329b9629f2e1

Reconstrução validada:
1. RUN109 completo — be1565cf347c37c46b4c2d33d0053bb25c71d382fdaa8f39752f0397c4d1cad7
2. RUN110 patch — e0e02a1711012e8b251caab3133570252e7fcec09f077aae3cd1e37a3a19803a
3. RUN163 — a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813
4. GAMEPLAY-001 — a4669e90434ccb0b1e769ed650084dcbbd8d46b0935de57c736d239d06678832
5. ANDROID-001 — 90a7e7a6bd9fa9a9c18a6122a87b8d8ee9b6e454fa4448c6adb333e4a461624d
6. MASTER_GAMEPLAY_HOOK.patch
7. INTEGRATED_FILE_HASHES.txt = 8/8 match

Delta: 4 Java + 3 testes. Nenhuma nova feature.
Contratos: atomicidade RUN149/RUN143; idempotência; strict action dispatch Heroes; rejeição de persistência de action inválida no AppDb.
Invariantes: PT-BR, auto_rolls=false, atomic_resource_commit=true, save compatibility, Runeterra/AU/Forgotten Secrets/Eberron/Heroes preservados, nenhum APK/AAB, BOOK_COMPLETE=false.
