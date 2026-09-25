# O Um Anel 4.9.36 — auditoria funcional rápida

PASSOU 42 · CORRIGIDO E RETESTADO 0 · FALHOU 0 · SEM EVIDÊNCIA 0

- **PASSOU** — Integração / Rules Engine ligado à ação — Comandos passam por dispatcher com rejeição explícita. — `lib/master/commands.ts`
- **PASSOU** — Persistência / IndexedDB estável — Nome/schema preservados. — `lib/db.ts`
- **PASSOU** — Viagem / Movimento livre bloqueado — UI não oferece salto livre e o Rules Engine rejeita avanço sem Marcha. — `components/EriadorMap.tsx + lib/rules/journey.ts`
- **PASSOU** — Descanso / Descanso altera relógio — Descanso está no Rules Engine e avança relógio. — `lib/master/commands.ts`
- **PASSOU** — Combate / Combate canônico conectado — Ataque/proteção existem no fluxo atual. — `Rules/UI`
- **PASSOU** — Combate / Coordenadas táticas coerentes — Rules e UI usam coordenadas normalizadas 0..1; saves legados 0..100 são migrados. — `lib/rules/combat.ts + lib/master/state.ts`
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
- **PASSOU** — Regras / dice-gandalf-auto-success — Runa de Gandalf vence TN alto sem dado de sucesso. — `audit_runtime.ts`
- **PASSOU** — Regras / dice-eye-miserable-auto-fail — Arrasado + Olho de Sauron falha mesmo com soma alta. — `audit_runtime.ts`
- **PASSOU** — Regras / dice-weary-zeroes-1-3 — Cansado ignora 1-3 nos dados de sucesso. — `audit_runtime.ts`
- **PASSOU** — Regras / dice-favoured-illfavoured-selection — Favorecido pega melhor; desfavorecido pega pior, com Olho ranqueado abaixo. — `audit_runtime.ts`
- **PASSOU** — Regras / condition-weary-recomputed — Endurance mudou e Cansado foi recalculado contra carga+fadiga. — `audit_runtime.ts`
- **PASSOU** — Regras / condition-miserable-recomputed — Esperanca/Sombra recalculam Arrasado. — `audit_runtime.ts`
- **PASSOU** — Regras / fatigue-never-negative — Fadiga nao fica negativa. — `audit_runtime.ts`
- **PASSOU** — Regras / fellowship-insufficient-no-mutation — Sociedade insuficiente nao altera estado. — `audit_runtime.ts`
- **PASSOU** — Regras / journey-four-roles-required — Jornada exige Guia, Batedor, Cacador e Vigia. — `audit_runtime.ts`
- **PASSOU** — Regras / journey-march-success-distance — Marcha bem-sucedida respeita rota restante e registra a jogada. — `audit_runtime.ts`
- **PASSOU** — Regras / journey-difficult-terrain-time — Terreno dificil custa dois dias; estrada normal custa um. — `audit_runtime.ts`
- **PASSOU** — Regras / progression-skill-cost-deducted — Elevar pericia para 2 custa 8 pontos e persiste o novo nivel. — `audit_runtime.ts`
- **PASSOU** — Regras / progression-combat-cost-deducted — Elevar proficiencia para 2 custa 8 Pontos de Aventura. — `audit_runtime.ts`
- **PASSOU** — Regras / equipment-derived-load-protection — Carga e Protecao sao recalculadas pelo equipamento canonico, ignorando valor manual incorreto. — `audit_runtime.ts`
- **PASSOU** — Regras / shadow-firm-will-scar — Firme Vontade converte Sombra em cicatriz permanente. — `audit_runtime.ts`
- **PASSOU** — Regras / shadow-bout-adds-flaw — Crise de Loucura avanca o Caminho da Sombra e reseta Sombra ate cicatrizes. — `audit_runtime.ts`
- **PASSOU** — Regras / journey-march-gate-blocks-free-move — Jornada ativa sem Marcha disponÃ­vel rejeita avanÃ§o de hex. — `audit_runtime.ts`
- **PASSOU** — Regras / journey-route-no-consecutive-duplicate — Movimento persiste rota sem duplicar hex consecutivo. — `audit_runtime.ts`
- **PASSOU** — Regras / combat-position-clamped — Posicao normalizada permanece dentro do tabuleiro. — `audit_runtime.ts`
- **PASSOU** — Regras / combat-legacy-percent-normalized — Coordenadas legadas em percentual sao convertidas para 0..1. — `audit_runtime.ts`
- **PASSOU** — Regras / combat-start-position-normalized — Inicio de combate cria tokens em coordenadas 0..1. — `audit_runtime.ts`
- **PASSOU** — Regras / combat-upsert-legacy-normalized — Upsert converte posicoes percentuais antigas. — `audit_runtime.ts`
- **PASSOU** — Regras / combat-save-migration — Save legado com posicao 0..100 migra para 0..1 ao reabrir. — `audit_runtime.ts`
- **PASSOU** — Build / Versão atual — Esperado 4.9.36 / 49036. — `android/app/build.gradle.kts`

- **PASSOU** — Build / Android completo — :app:assembleDebug concluído no BRELGA.
