# ENGINE-002 — APPLY INSTRUCTIONS

Aplicar somente sobre a árvore RUN164 candidate reconstruída conforme QA001_RELEASE.md do commit b8e59f6c0a8d35bbc93865830359afecd0642d10.

Ordem:
1. RUN109 completo.
2. RUN110 patch.
3. RUN163.
4. GAMEPLAY-001.
5. ANDROID-001.
6. MASTER_GAMEPLAY_HOOK.patch.
7. Confirmar INTEGRATED_FILE_HASHES.txt = 8/8.
8. Obter ENGINE-002.patch do checkpoint Drive.
9. Confirmar SHA-256 do patch = 323892809682860d480cbc39f82786f53eb91d2c3ab51b1f840e329b9629f2e1.
10. Executar `git apply --check ENGINE-002.patch`.
11. Executar `git apply ENGINE-002.patch`.
12. Rodar os 19 scripts registrados em ENGINE002_VALIDATION.txt.

Não aplicar em RUN143/RUN149/RUN163 isoladamente. Não aplicar diretamente em main. Não gerar APK/AAB.
