# BASELINE SOURCE AND SHA-256 AUDIT REPORT
**Projeto:** Tales from the Loop (Kids) & Things from the Flood (Teens) VTT  
**Data da Auditoria:** 26/10/2026  
**Responsável:** Antigravity  

---

## 1. Origem e Reconciliação do Baseline

Foi realizada a inspeção física e binária exata das fontes disponíveis:

1. **Repositório GitHub:** `bra83/braseirobuild`
   - Branch `build/tales-1.3.16-apk-final` (SHA `23e2d2a46b3fe3af63951090dd81f69dd6096535`)
   - Branch `build/tales-1.3.17-apk-final` (confirmados 3 workflows com FAILURE em 17/09/2026; descartado como base de acordo com as regras de integridade).

2. **Base Física Android Studio:**
   - Pasta de partida: `c:\Users\bcruz\Downloads\BRASEIRO_TALES_VTT_ANDROID_STUDIO_FINAL_1.3.5\BRASEIRO_TALES_VTT_ANDROID_STUDIO_FINAL_1.3.5\android`
   - Validação de integridade de assets: conferência contra o manifesto `_binary_sha256.json`.
   - **Resultado:** 208 arquivos binários conferidos, 208 matched, 0 missing, 0 mismatched (100% integridade).

3. **Cadeia Canônica de Transformação Executada e Validada:**
   - `tales-1.3.6.patch` (fluxo NOVA AVENTURA determinístico e retry resiliente).
   - `tales-1.3.7-overlay` (TTS Gemini com fallback Android, escolhas de ação padrão).
   - `tales-1.3.8-transform.py` (NarrativePanel, IconicItemVisual, localOpeningNarrative).
   - `tales-1.3.14-transform.py` (Blocos 1.3.9 a 1.3.14: retrato do Kid em Grupo, imagem de cena orientada ao local atual, launcher icon aprovado, rotas e mapa interativo, autoridade mecânica de inventário, recuperação canônica e Mudança).
   - `tales-1.3.16-final-transform.py` (encadeamento 1.3.15 até 1.3.16i: Itens e efeitos compráveis, fundação Things from the Flood, Cicatrizes, Friction, seletor de perfil, ciclo de Teen e saída aos 20).
   - Suíte de 146 testes unitários auditados em `audit/tales-1.3.16-dev-current/test`.
   - Patch dos suplementos oficiais `They Grow Up So Fast` (TGUF) e `Nossas Amigas as Máquinas` (OFM) com `OfficialSupplementRules.kt`.

---

## 2. Compilação e Verificação do Baseline Inicial

- **JDK:** OpenJDK 21.0.10 (JBR do Android Studio em `C:\Program Files\Android\Android Studio\jbr`)
- **Android SDK:** API 35 / API 36 / Build-tools 36.0.0 em `C:\Users\bcruz\AppData\Local\Android\Sdk`
- **Gradle:** 9.3.1 / AGP 8.8.2 / Kotlin Compose plugin
- **Comando de Teste:** `.\gradlew.bat testDebugUnitTest`
  - **Resultado:** `BUILD SUCCESSFUL`
  - **Testes Executados:** 146
  - **Falhas:** 0
  - **Erros:** 0
- **Comando de Build:** `.\gradlew.bat assembleDebug`
  - **Resultado:** `BUILD SUCCESSFUL`
  - **APK Produzido:** `app-debug.apk`
  - **Tamanho:** 152.247.459 bytes
  - **Status do Gate P0:** PASS
