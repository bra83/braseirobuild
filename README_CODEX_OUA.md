# Braseiro O Um Anel 2E — pacote enxuto para Codex

Fonte congelada no commit:
6025e7b4ee43070468674f3cb100b746f001895e

Este branch contém somente:
- ci/oua-4.9.30 até ci/oua-4.9.47
- workflows OUA relacionados a 4.9.30–4.9.46

Build-base Android Studio:
https://github.com/bra83/braseirobuild/releases/download/OUA-v4.9.29/OUA_4.9.29_UPGRADE_SAFE_ANDROID_STUDIO.zip

Ordem:
1. Baixar/extrair a build-base 4.9.29.
2. Aplicar patches cumulativamente 4.9.30 -> 4.9.47 conforme validate-oua-4.9.46.yml.
3. 4.9.47 está parcial: patch_model.py + patch_rules.py + templates/lib/rules/core47.ts.
4. Completar patch_commands.py, patch_ui.py, patch_finalize.py e validate-oua-4.9.47.yml.
5. Não gerar APK antes do fechamento mecânico.

Não tratar este pacote como projeto Android completo; ele é o delta cumulativo a aplicar sobre a build-base.
