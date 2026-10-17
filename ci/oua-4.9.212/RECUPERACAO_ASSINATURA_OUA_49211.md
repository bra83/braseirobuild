# OUA 4.9.212 — recuperar a assinatura EXATA da 4.9.211

> **PROVA DIRETA — NÃO PROCURAR EM COMPUTADOR ANTIGO.** A assinatura da 4.9.211 foi produzida pelo workflow [\`.github/workflows/oua-49211-final-release.yml\`](https://github.com/bra83/braseirobuild/blob/OUA-v4.9.211/.github/workflows/oua-49211-final-release.yml), [run 36987914805](https://github.com/bra83/braseirobuild/actions/runs/36987914805) (2026-10-02; sucesso).

## Caminhos confirmados no GitHub
- **Keystore histórica em base64:** \`build-signing/braseiro-stable-debug.keystore.b64\`, presente em \`main\`, \`OUA-v4.9.211\` e \`integration/oua-49212-v9-hardened-20261017\`.
- **Workflow gerador do APK:** \`.github/workflows/oua-49211-final-release.yml\`.
- **Comando usado:** \`base64 -d "$GITHUB_WORKSPACE/build-signing/braseiro-stable-debug.keystore.b64" > "$HOME/.android/debug.keystore"\` seguido de \`./gradlew :app:assembleDebug\`.
- **Fingerprint SHA-256 do CERTIFICADO da versão 4.9.211:** \`b1471859ec0a4838ec5dff4f18f77bacdc99d535de968d261a7fe4008213dd27\`.
- **Fingerprint SHA-1 do CERTIFICADO:** \`004514d56df790237afcfd9d6b95a27ee1a34d45\`.
- **Package ID:** \`com.braseiro.oumanel\`.
- **APK existente:** https://github.com/bra83/braseirobuild/releases/download/OUA-v4.9.211/BRASEIRO_O_UM_ANEL_4.9.211_STRIDER_OPENING.apk
- **SHA-256 do ARQUIVO APK (DIFERENTE DO SHA DO CERTIFICADO):** \`e8908814ac7906aa7d03fb19e1c07bd109c90ed0754731e287271a143c974ad8\`.

## Antigravity em Windows / PowerShell — comando seguro

Execute a partir da cópia da branch, em ambiente confiável, **sob a mesma conta que rodará o Gradle**.

\`\`\`powershell
$ErrorActionPreference = 'Stop'
$repo = 'D:\OUA_49212_WORKSPACE\braseirobuild'  # SUBSTITUIR pelo clone REAL
$source = Join-Path $repo 'build-signing\braseiro-stable-debug.keystore.b64'
$dest = Join-Path $env:USERPROFILE '.android\debug.keystore'
if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "Keystore histórica ausente: $source" }
New-Item -ItemType Directory -Force -Path (Split-Path $dest) | Out-Null
$b64 = ([System.IO.File]::ReadAllText($source) -replace '\s','')
[System.IO.File]::WriteAllBytes($dest, [System.Convert]::FromBase64String($b64))

# A build 4.9.211 usa uma assinatura Android Debug.
# Credenciais Android Debug convencionais: storepass android, alias androiddebugkey.
# Se forem diferentes, NÃO inventar credenciais nem imprimir o arquivo.
$report = & keytool -list -v -keystore $dest -storepass android -alias androiddebugkey 2>&1
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível abrir a keystore com credenciais debug convencionais; conferir configuração original, sem trocar a chave' }
$fingerprintLine = ($report | Select-String -Pattern 'SHA256:' | Select-Object -First 1).ToString()
$actual = ($fingerprintLine -replace '^.*SHA256:\s*','' -replace '[^0-9a-fA-F]','').ToLowerInvariant()
$expected = 'b1471859ec0a4838ec5dff4f18f77bacdc99d535de968d261a7fe4008213dd27'
if ($actual -ne $expected) { throw "CERTIFICADO INCORRETO: $actual; esperado: $expected" }
Write-Host 'OUA_SIGNING_CERT_MATCH=PASS'
\`\`\`

No Linux:
\`\`\`bash
set -euo pipefail
mkdir -p "$HOME/.android"
base64 -d "build-signing/braseiro-stable-debug.keystore.b64" > "$HOME/.android/debug.keystore"
chmod 600 "$HOME/.android/debug.keystore"
keytool -list -v -keystore "$HOME/.android/debug.keystore" \
  -storepass android -alias androiddebugkey | grep SHA256
\`\`\`
Conferir SHA-256 integral acima; se não corresponder, NÃO prosseguir para versão instalável.

## Compilação e confirmação
1. Manter o projeto Android **4.9.211 como base**, com V9 corrigida pelo PR #59; NÃO mexer em outros VTTs.
2. Colocar a keystore histórica no perfil do **usuário do processo Gradle** (Windows ou Linux). Em outro runner, copiar/configurar novamente no próprio runner, sem logs do Base64 ou senha.
3. Antes de compilar, não deixar Gradle gerar automaticamente outra \`debug.keystore\`. Rodar \`android/gradlew :app:assembleDebug\` na raiz Android REAL extraída do ZIP 4.9.211 e já integrada.
4. Na APK resultante, conferir \`apksigner verify --verbose --print-certs app-debug.apk\`; o **certificate SHA-256 digest** deve coincidir, além do \`applicationId\` e \`versionCode\` adequado. Validar instalação por cima da 4.9.211 mantendo saves, sem desinstalar.
5. Somente assinar **depois** dos gates de segurança/regras/UI/salvamento; a recuperação do certificado NÃO equivale a aprovação de release.

## Alerta sério de segurança
O repositório contém uma **chave privada de assinatura convertida em Base64**. Base64 NÃO é criptografia nem proteção. Caso o repositório esteja acessível publicamente, essa chave está exposta e terceiros podem produzir APKs com a identidade de assinatura antiga. Não mostrar o conteúdo no chat, CI logs, issues ou relatórios, nem copiar o arquivo decodificado para o Git público. Planejar rotação/migração de chave de assinatura com cuidado, testando compatibilidade de atualização; simplesmente apagar o arquivo do HEAD NÃO remove a exposição do histórico Git.

**Condição de fechamento:** \`OUA_SIGNING_CERT_MATCH=PASS\`, APK produzido por Gradle, \`apksigner\` com o mesmo certificado, atualização preservando dados, testes funcionais/segurança aprovados. Se a chave histórica estiver inacessível no ambiente de build, registrar o bloqueio e NÃO gerar uma assinatura divergente para suposta atualização in-place.
