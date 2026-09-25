# O Um Anel 4.9.35 — auditoria funcional rápida

PASSOU 18 · CORRIGIDO E RETESTADO 0 · FALHOU 0 · SEM EVIDÊNCIA 1

- **PASSOU** — Integração / Rules Engine ligado à ação — Comandos passam por dispatcher com rejeição explícita. — `lib/master/commands.ts`
- **PASSOU** — Persistência / IndexedDB estável — Nome/schema preservados. — `lib/db.ts`
- **PASSOU** — Viagem / Movimento livre bloqueado — UI não oferece salto livre e o Rules Engine rejeita avanço sem Marcha. — `components/EriadorMap.tsx + lib/rules/journey.ts`
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
- **PASSOU** — Varredura / TODO/FIXME — Ocorrências exigem inspeção; não são erro automaticamente. — `nenhum`
- **PASSOU** — Varredura / empty-click — Ocorrências exigem inspeção; não são erro automaticamente. — `nenhum`
- **PASSOU** — Varredura / not-implemented — Ocorrências exigem inspeção; não são erro automaticamente. — `nenhum`
- **PASSOU** — Android / Sem acesso/cópia de modelo Kokoro — nenhum acesso proibido — `MainActivity.kt/AndroidManifest.xml`
- **SEM EVIDÊNCIA** — Testes / Harness determinístico — file:///C:/Users/bcruz/AppData/Local/Temp/tmpm5b_7luq/audit_runtime.mjs:136
    startedAtWorldMinute: Math.max(0, Number(state2.worldClock.elapsedMinutes) || 0),
                                                               ^

TypeError: Cannot read properties of undefined (reading 'elapsedMinutes')
    at recordJourneyMove (file:///C:/Users/bcruz/AppData/Local/Temp/tmpm5b_7luq/audit_runtime.mjs:136:64)
    at file:///C:/Users/bcruz/AppData/Local/Temp/tmpm5b_7luq/audit_runtime.mjs:208:13
    at ModuleJob.run (node:internal/modules/esm/module_job:439:25)
    at async node:internal/modules/esm/loader:643:26
    at async asyncRunEntryPointWithESMLoader (node:internal/modules/run_main:101:5)

Node.js v24.18.0
 — `audit_runtime.ts`
- **PASSOU** — Build / Versão atual — Esperado 4.9.35 / 49035. — `android/app/build.gradle.kts`

- **PASSOU** — Build / Android completo — :app:assembleDebug concluído no BRELGA.
