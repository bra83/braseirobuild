# QA-001 — RELEASE DO MASTER

Base de auditoria: `dnd/master-run164-integration-candidate`.

O QA deve reconstruir a árvore exatamente nesta ordem:

1. RUN109 Android Studio completo, SHA-256 `be1565cf347c37c46b4c2d33d0053bb25c71d382fdaa8f39752f0397c4d1cad7`;
2. RUN110 patch, SHA-256 `e0e02a1711012e8b251caab3133570252e7fcec09f077aae3cd1e37a3a19803a`;
3. RUN163, SHA-256 `a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813`;
4. GAMEPLAY-001, SHA-256 `a4669e90434ccb0b1e769ed650084dcbbd8d46b0935de57c736d239d06678832`;
5. ANDROID-001 overlay, SHA-256 `90a7e7a6bd9fa9a9c18a6122a87b8d8ee9b6e454fa4448c6adb333e4a461624d`;
6. aplicar `MASTER_GAMEPLAY_HOOK.patch`;
7. conferir `INTEGRATED_FILE_HASHES.txt`.

ENGINE-001 não possui delta Java. Seu checkpoint de auditoria tem SHA-256 `ac5b68c4d33a230d86cb2109f6231d079d4541a7d757dc4fef9d3e97b5aca0c7` e status `PARTIAL`.

Antes da liberação ao QA, o MASTER executou 16 scripts integrados: 13/13 regressões RUN141→RUN163 + 3/3 GAMEPLAY-001, todos PASS.

O gate Gradle compile-only foi tentado e bloqueado antes da compilação por `UnknownHostException: services.gradle.org`. O QA deve repetir o gate em ambiente com rede/cache/Android SDK.

Nenhum APK/AAB deve ser gerado.

`BOOK_COMPLETE=false` continua obrigatório.
