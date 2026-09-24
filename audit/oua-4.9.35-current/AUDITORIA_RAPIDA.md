# O Um Anel 4.9.35 — auditoria funcional rápida

PASSOU 15 · CORRIGIDO E RETESTADO 0 · FALHOU 1 · SEM EVIDÊNCIA 3

- **PASSOU** — Integração / Rules Engine ligado à ação — Comandos passam por dispatcher com rejeição explícita. — `lib/master/commands.ts`
- **PASSOU** — Persistência / IndexedDB estável — Nome/schema preservados. — `lib/db.ts`
- **FALHOU** — Viagem / Movimento livre bloqueado — Mapa exige fluxo de rota/marcha. — `App.tsx`
- **PASSOU** — Descanso / Descanso altera relógio — Descanso está no Rules Engine e avança relógio. — `lib/master/commands.ts`
- **PASSOU** — Combate / Combate canônico conectado — Ataque/proteção existem no fluxo atual. — `Rules/UI`
- **PASSOU** — Conselho / Conselho conectado — Resultado do conselho chega ao estado. — `Rules/UI`
- **PASSOU** — Mundo / Mundo vivo persiste — Tempo/comandos propagam mundo vivo. — `lib/master/commands.ts`
- **PASSOU** — Android / TTS engines dinâmicas — Catálogo vem do Android. — `MainActivity.kt`
- **PASSOU** — Android / Braseiro Kokoro deduplicado — Debug/release Sherpa colapsam para um backend visível. — `MainActivity.kt`
- **PASSOU** — Android / Somente Voice real — Seleção usa Voice real. — `MainActivity.kt`
- **PASSOU** — Android / Kokoro multi-bloco — Primeiro bloco flush; sequência por onDone; sem watchdog neural. — `MainActivity.kt`
- **PASSOU** — Áudio / Música manual persiste entre cenas — Música manual usa slot independente. — `App.tsx/lib/AudioEngine.ts`
- **PASSOU** — Áudio / Faixa longa não usa SoundPool — Faixas longas usam player contínuo. — `lib/AudioEngine.ts`
- **SEM EVIDÊNCIA** — Varredura / TODO/FIXME — Ocorrências exigem inspeção; não são erro automaticamente. — `.offline-vendor\index-BgM8hFyV.js:625; scripts\test-cold-start-freeze-4.7.4-r2.mjs:47; scripts\test-structural-3.5.mjs:139; dist-offline\vendor\vendor-old.js:625; dist\vendor\vendor-old.js:625; android\app\src\main\java\com\braseiro\oumanel\MainActivity.kt:518; android\app\src\main\assets\vendor\vendor-old.js:625`
- **PASSOU** — Varredura / empty-click — Ocorrências exigem inspeção; não são erro automaticamente. — `nenhum`
- **SEM EVIDÊNCIA** — Varredura / not-implemented — Ocorrências exigem inspeção; não são erro automaticamente. — `.offline-vendor\index-BgM8hFyV.js:553; .offline-vendor\index-BgM8hFyV.js:582; .offline-vendor\index-BgM8hFyV.js:582; .offline-vendor\index-BgM8hFyV.js:582; .offline-vendor\index-BgM8hFyV.js:635; .offline-vendor\index-BgM8hFyV.js:635; .offline-vendor\index-BgM8hFyV.js:635; .offline-vendor\index-BgM8hFyV.js:635; .offline-vendor\index-BgM8hFyV.js:635; .offline-vendor\index-BgM8hFyV.js:1250; .offline-vendor\index-BgM8hFyV.js:1445; .offline-vendor\index-BgM8hFyV.js:1445; .offline-vendor\index-BgM8hFyV.js:1445; .offline-vendor\index-BgM8hFyV.js:1445; .offline-vendor\pdf.worker.min-DEtVeC4l.mjs:29; .offline-vendor\pdf.worker.min-DEtVeC4l.mjs:29; dist-offline\vendor\pdf.worker.min-DEtVeC4l.mjs:29; dist-offline\vendor\pdf.worker.min-DEtVeC4l.mjs:29; dist-offline\vendor\vendor-old.js:553; dist-offline\vendor\vendor-old.js:582`
- **PASSOU** — Android / Sem acesso/cópia de modelo Kokoro — nenhum acesso proibido — `MainActivity.kt/AndroidManifest.xml`
- **SEM EVIDÊNCIA** — Testes / Harness determinístico — file:///C:/Users/bcruz/AppData/Local/Temp/tmpihrn7m4n/audit_runtime.mjs:125
    throw new Error("Fa\xE7a a Jogada de Marcha do Guia antes de avan\xE7ar a Jornada.");
          ^

Error: FaÃ§a a Jogada de Marcha do Guia antes de avanÃ§ar a Jornada.
    at recordJourneyMove (file:///C:/Users/bcruz/AppData/Local/Temp/tmpihrn7m4n/audit_runtime.mjs:125:11)
    at file:///C:/Users/bcruz/AppData/Local/Temp/tmpihrn7m4n/audit_runtime.mjs:201:13
    at ModuleJob.run (node:internal/modules/esm/module_job:439:25)
    at async node:internal/modules/esm/loader:643:26
    at async asyncRunEntryPointWithESMLoader (node:internal/modules/run_main:101:5)

Node.js v24.18.0
 — `audit_runtime.ts`
- **PASSOU** — Build / Versão atual — Esperado 4.9.35 / 49035. — `android/app/build.gradle.kts`

- **PASSOU** — Build / Android completo — :app:assembleDebug concluído no BRELGA.
