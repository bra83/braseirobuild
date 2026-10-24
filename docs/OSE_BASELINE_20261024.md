# BASELINE E AUDITORIA DE PROVENIÊNCIA — BRASEIRO OSE
**Data:** 24 de outubro de 2026  
**Líder Técnico:** Antigravity  
**Ambiente Host / Runner:** BRELGA (`C:\actions-runner`) Windows X64  
**Branch Base:** `main` (`e351c7f50dd0dfe8f8d34ddd577cfb1bf280ba26`)  
**Branch de Trabalho:** `ose/final-integration-20261024`  

---

## 1. IDENTIFICAÇÃO DE COMMITS E VERSÕES

| Componente | Referência / SHA / Tag | Observação |
|---|---|---|
| Repositório Canônico | `https://github.com/bra83/braseirobuild.git` | Clonado em `C:\Users\bcruz\Downloads\braseirobuild_repo` |
| `origin/main` HEAD | `e351c7f50dd0dfe8f8d34ddd577cfb1bf280ba26` | Integrado com PR #23 |
| Último Commit OSE em `main` | `f605c4857` (PR #23) | `fix(OSE): Galaxy A57 / One UI responsive layout and Android viewport (#23)` |
| Release OSE 1.0.8 Histórica | Tag `ose-1.0.8-final` (`0a451e5b301fdf0a723c2259abb0826161768a5c`) | `versionCode=9`, `versionName=1.0.8` |
| PR #24 (Retainers Authority) | `origin/ose/reconcile-retainers-authority-20261008` (`9e46fce5f`) | Reconciliação autoridade + auxiliares |
| PR #25 (Wage Ledger) | `origin/ose/retainer-wage-ledger-integrated-20261008` (`d074d6d1b`) | Salários diários persistentes e recibos |
| Certificado de Assinatura | `build-signing/braseiro-stable-debug.keystore.b64` | Alias `androiddebugkey` |
| SHA-1 da Assinatura | `6F:47:92:78:1F:11:ED:FF:C8:92:AF:EB:95:69:4E:3C:03:F3:5C:DF` | Validado via `keytool` |
| SHA-256 da Assinatura | `6A:A8:7A:24:A2:EB:47:62:C3:F4:93:C9:D5:FD:DB:F1:0B:60:CC:86:59:A9:04:23:AE:D7:CD:E8:AE:58:2B:D3` | Validado via `keytool` |

---

## 2. TOOLCHAIN LOCAL VERIFICADA NO BRELGA

- **Java JDK:** Eclipse Temurin Hotspot JDK 17 (`C:\actions-runner\_work\_tool\Java_Temurin-Hotspot_jdk\17.0.20-101\x64`) e JDK 21 (`21.0.12-101.0.LTS`)
- **Android SDK:** `C:\actions-runner\android-sdk` (Platforms 34, 35; Build-Tools 36.0.0, 35.0.0)
- **Node.js:** v24.18.0 (`node.exe` funcional)
- **Gradle:** Wrapper Gradle 8.10.2 (testes `:rules-classic` e `:rules-advanced` concluídos com sucesso)
- **GitHub Runner Service:** `Runner.Listener` (PID ativo no host BRELGA)

---

## 3. TESTES DE BASELINE EXECUTADOS (ANTES DE ALTERAÇÕES)

| Teste | Comando | Resultado |
|---|---|---|
| JS Syntax `p1.js` | `node --check ose/1.0.0/source/web-ui-p1/p1.js` | **PASS** |
| Multi-Character Smoke | `node ose/1.0.0/source/web-ui-p1/multiple_character_smoke.js` | **PASS** |
| Character Control Smoke | `node ose/1.0.0/source/web-ui-p1/character_control_smoke.js` | **PASS** |
| Galaxy A57 Responsive Contract | `node ose/1.0.0/source/web-ui-p1/galaxy_a57_layout_smoke.cjs` | **PASS** (12 contratos validados) |
| Motor Vida V2.2 Isolado | `node --test tests/*.test.mjs` | **PASS** (134/134 testes passaram) |
| JVM Rules Classic & Advanced | `./gradlew :rules-classic:test :rules-advanced:test` | **BUILD SUCCESSFUL** |

---

## 4. INVENTÁRIO DO ACERVO OSE — GOOGLE DRIVE (`G:\Meu Drive\Livros Jogos\Old School`)

| Arquivo / Livro | Tamanho | Status no Drive Local | Escopo OSE |
|---|---|---|---|
| `OSE - Tomo de regras.pdf` | 25.4 MB | Presente e legível | Core Classic |
| `OSE - Tomo do jogador.pdf` | 22.8 MB | Presente e legível | Core Advanced Player |
| `Ose referee 2026.pdf` | 39.8 MB | Presente e legível | Core Referee 2026 |
| `advanced-fantasy-referee-s-tome-v0-3-ose_compress.pdf` | 19.5 MB | Presente e legível | Gap-fill Referee v0.3 |
| `ilide.info-ose-fa-bestiario-10-hd-pr_9f5ad6c613ceb92188b6769dc0e34c99.pdf` | 48.7 MB | Presente e legível | Bestiário Advanced |
| `ilide.info-old-school-essentials-classic-game-set-magic-v1-0-pr_5d4eb4817db8b4bac30d7606054bfc5f.pdf` | 68.2 MB | Presente e legível | Magia Classic |
| `ilide.info-old-school-essentials-classic-game-set-treasures-v1-0-pr_f1abe83292b8b19ec67a4b708b7bb20e.pdf` | 98.4 MB | Presente e legível | Tesouros Classic |
| `ilide.info-ose-regrasbasicas-ptbr-10-pr_84c92c2ed1f892921bcb1385fe713bd6.pdf` | 11.2 MB | Presente e legível | Regras Básicas PT-BR |
| `THE INCANDESCENT GROTTOES.pdf` | 86.1 MB | Presente e legível | Aventura Suplementar |
| `ilide.info-o-buraco-no-carvalho-pr_ef084c1b3a1253dbbd02da9c9cd0644e.pdf` | 71.3 MB | Presente e legível | Aventura Suplementar |
| `in-the-shadow-of-tower-silveraxe.pdf` | 62.5 MB | Presente e legível | Aventura Suplementar |
| `The-Isle-of-the-Plangent-Mage-v1-2.pdf` | 47.9 MB | Presente e legível | Aventura Suplementar |
| `Adventure-Holy-Mountain-Shaker.pdf` | 13.4 MB | Presente e legível | Aventura Suplementar |
| `pdfcoffee.com_hall-of-the-blood-king-pdf-free.pdf` | 10.8 MB | Presente e legível | Aventura Suplementar |
| `AdvAnthology-2.pdf` | 10.5 MB | Presente e legível | Aventura Suplementar |
| `ilide.info-ose-advanthology-i-pr_afba605aa537401ffeb2f9842f077d4f.pdf` | 96.2 MB | Presente e legível | Aventura Suplementar |
| `ilide.info-ose-the-scourge-of-the-northland-pr_766f3a17737d9933a5b47b53ec6636f9.pdf` | 15.1 MB | Presente e legível | Aventura Suplementar |

---

## 5. PLANO DE CONSOLIDAÇÃO INCREMENTAL

1. **Lote B:** Integrar PR #24 e PR #25 na branch `ose/final-integration-20261024`, mantendo os contratos do PR #23 (A57). Validar todos os testes de auxiliares, salários e autoridade.
2. **Lote C:** Integrar Motor de Vida V2.2 com packs temáticos OSE Classic e Advanced (`ose-classic` e `ose-advanced`), adaptadores Room e ponte causal de eventos verificados.
3. **Lote D:** Importador de aventuras JSON `braseiro.adventure/v1` com schema corrigido, isolamento de segredos `gm_only`, duas aventuras canônicas (Classic e Advanced) e persistência de mundo.
4. **Lote E:** TTS Universal V3 integrado ao `AndroidTtsController`, suporte a Kokoro instalado, Gemini TTS / Charon, retenção de seleção e retratos/tokens de personagem e NPC por ID.
5. **Lote F:** Cobertura de regras por livro/seção com evidências determinísticas.
6. **Lote G:** Validação de suite completa, pipeline CI e compilação do APK assinado no BRELGA.
