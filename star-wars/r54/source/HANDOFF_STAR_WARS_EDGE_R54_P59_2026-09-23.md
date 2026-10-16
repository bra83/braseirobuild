# R54 P59 — build bootstrap para jogar Sob um Sol Negro

Baseline: R54 P58 do Google Drive.

## Alteração
- Gradle Wrapper deixa de depender de `services.gradle.org` e aponta diretamente para a release oficial Gradle 8.13 em `github.com/gradle/gradle-distributions`.
- SHA-256 oficial já pinado foi preservado: `20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`.
- Adicionado gate automatizado que valida endpoint de produção + checksum.
- Nenhuma regra, estado, EotE/AoR/MIXED, Duty/Obligation ou conteúdo de Sob um Sol Negro foi alterado.

## Evidência
- focal Sob um Sol Negro + wrapper: PASS.
- regressão completa: 907/907 PASS, 0 FAIL.
- assembleDebug local: BLOQUEADO pela infraestrutura deste ambiente em `UnknownHostException: github.com` antes de baixar o Gradle. Não é declarado PASS Android.

## Próximo passo
Executar `./gradlew :app:assembleDebug` no BRELGA/self-hosted, que possui acesso GitHub. Se completar, preservar o APK como artefato instalável da baseline P59.
