# BRASEIRO Tales VTT — D&D Gold Standard module transplant QA

Status: **SOURCE PASS / ANDROID RUNTIME NOT EXECUTED IN THIS ENVIRONMENT**

This revision keeps the approved Tales/Kids visual and mechanical baseline and adapts product infrastructure from the audited BRASEIRO D&D Gold Standard. No D&D rules, combat economy, classes, spells, Strixhaven data, maps or lore were transplanted.

## Product modules now present

- **Gemini text model**: editable model name in MENU.
- **Gemini voice model**: separate TTS model and voice selection.
- **API key**: local override encrypted with Android Keystore / AES-GCM; build-injected key remains supported.
- **Voice**: Gemini TTS for visible GM narration, with Android TTS fallback.
- **Portable campaign backup**: JSON export/import with format/profile/version validation, SHA-256 state checksum and read-after-write round-trip verification.
- **Automatic portable backup**: optional user-selected SAF directory, throttled after valid state changes, retention of recent backups.
- **Scene image portability**: current imported scene image is embedded in the portable campaign backup; manual/RAG PDFs are external libraries and are deliberately not included.
- **Kid JSON**: import/export with Tales/Kids profile, age, Luck, Attribute and Skill validation.
- **External Gemini illustration workflow**: CENA > ILUSTRAÇÃO shares a prompt; images shared back to BRASEIRO are copied into private storage and attached only if the same scene fingerprint is still pending.
- **Manual library**: import one or many PDFs into app-private storage.
- **Local indexing**: PDFBox text extraction; PdfRenderer + ML Kit OCR fallback for scanned/image pages.
- **RAG search**: local lexical retrieval by manual/page/snippet.
- **RAG → Motor Barbara**: top retrieved snippets are inserted into the GM request as `rag_context` with an explicit no-invention / Tales-only instruction.

## Regression evidence

- Core rules/firewall source contract: **112 PASS / 0 FAIL**.
- Static visual/mechanical source audit: **PASS**.
- Product-feature source audit: **PASS**.
- Main-screen contract count remains **13/13**; Biblioteca is an internal MENU utility route and does not alter the main navigation contract.
- Android OS backup remains disabled (`allowBackup=false`) intentionally; portable backup is explicit and user-controlled.

## Not claimed

Gradle build, APK, emulator and physical-device runtime were not executed in this environment. Those remain the external smoke-test gate.
