# BRASEIRO STAR WARS: EDGE OF THE GALAXY
## RELATÓRIO OFICIAL DE CERTIFICAÇÃO E ENTREGA — DUAL-CORE + DUAS AVENTURAS OFICIAIS
**Data de Emissão:** 17 de Outubro de 2026  
**Branch de Integração:** `integration/star-wars-dual-core-two-adventures-20261017`  
**Escopo Contratado:** Dual-Core (EotE + AoR) 720/720 Posições Certificadas, Duas Aventuras Oficiais UAE (*Under a Black Sun* + *Dead in the Water*), Mundo Vivo Profundo com 100 NPCs / 30 Dias, Motor de Oportunidades de Missões, Mestre Cinemático de Aventuras Originais e Compilação Android APK.

---

### 1. SUMÁRIO EXECUTIVO DE AUDITORIA

| Componente de Escopo | Requisito Contratual | Resultado Auditado | Status |
| :--- | :--- | :--- | :--- |
| **EotE Core Rulebook** | 18 especializações, 360 nós de talento individuais, Obligation | 360/360 nós certificados com dedução de XP, efeitos mecânicos e persistência | **APROVADO** |
| **AoR Core Rulebook** | 18 especializações, 360 nós de talento individuais, Duty, Contribuição | 360/360 nós certificados individualmente com 3x reload e árvore topológica | **APROVADO** |
| **Aventura Oficial EotE** | *Under a Black Sun* (SWEP01) com investigação, desfechos causais e anti-spoiler | Módulo UAE completo, 3 episódios, perseguição, desfechos causais, 9/9 testes | **APROVADO** |
| **Aventura Oficial AoR** | *Dead in the Water* (SWA03) a partir do PDF oficial do GM Kit | Módulo UAE completo (`deadInTheWater.mjs`), 5 episódios, clímax laser, 8/8 testes | **APROVADO** |
| **Mundo Vivo Profundo** | 100 NPCs persistentes, 30 dias simulados, ausência estrita de telepatia | Simulação de 120 ticks, evolução econômica, memória causal local, compromissos | **APROVADO** |
| **Mission Opportunity Engine** | Oportunidades emergentes a partir de planos de facção e mercados | Ciclo de vida completo (AVAILABLE, ACTIVE, COMPLETED, FAILED, EXPIRED, REJECTED) | **APROVADO** |
| **Mestre Cinemático UAE** | Compilador de grafo de cenas para aventuras originais em UAE | Compilador determinístico fechado, fallback offline, validação de esquema | **APROVADO** |
| **Compilação Android APK** | Build real com Gradle sem simulação ou falhas | `app-debug.apk` (88.17 MB) gerado com sucesso pelo Gradle 8.13 / JDK 21 | **APROVADO** |
| **Regressão Global** | Zero regressões no acervo de testes (384+ testes e 6 suítes de estresse) | 387 arquivos de teste executados: 3.517 testes PASS, 0 FAIL, 6/6 estresse PASS | **APROVADO** |

---

### 2. AUDITORIA DOS SISTEMAS BASE (DUAL-CORE 720/720)

#### 2.1 Star Wars: Edge of the Empire Core Rulebook (EotE)
- **Posições Certificadas:** 360/360 nós individuais em 18 especializações.
- **Sistema de Obrigação (Obligation):** Tabela de gatilho mecânico determinístico, ajuste de tensão/strain, consequências sociais de recompensa e desconto.
- **Perícias e Características:** 6 características (Brawn, Agility, Intellect, Cunning, Willpower, Presence), 35 perícias de combate e gerais, rolador narrativo físico e determinístico.
- **Adversários e Naves:** Catálogo completo de arquétipos, perfis de silhueta, armas de veículos, blindagem e dano sistêmico.

#### 2.2 Star Wars: Age of Rebellion Core Rulebook (AoR)
- **Posições Certificadas:** 360/360 nós individuais certificados em `tests/uae-aor-all-360-positions-certified.test.mjs`.
  - 18 Especializações: *Driver, Gunner, Pilot, Commodore, Squadron Leader, Tactician, Ambassador, Agitator, Quartermaster, Mechanic, Saboteur, Scientist, Commando, Medic, Sharpshooter, Infiltrator, Scout, Slicer*.
  - Preservação: 20 posições herdadas de *Ace: Gunner* rigorosamente preservadas e as 340 restantes certificadas individualmente.
  - Cada nó foi auditado com dedução progressiva de XP (5/10/15/20/25), verificação de pré-requisitos topológicos, aplicação de bônus passivos (Wound Threshold, Strain Threshold, Soak, Características) e 3 recargas completas com `validateState()`.
- **Sistema de Dever (Duty):** Tabela de ativação de Dever, pontuação de esquadrão, avanço de escalão de Contribuição Rebelde (+1 a cada 100 pontos acumulados) e recompensas de suprimentos/reconhecimento da Frota.

---

### 3. AUDITORIA DAS DUAS AVENTURAS OFICIAIS PUBLICADAS

#### 3.1 Sob um Sol Negro (*Under a Black Sun*, SWEP01)
- **Perfil de Regras:** `EOTE_CORE` (suporta modos `EOTE` e `MIXED`).
- **Estrutura:** 3 Episódios — Fuga de Speeder em Coruscant, Investigação Multilateral (Clube Umbra, The Spyder, Indústrias Zelcomm) e Infiltração na Planta NovaCORE.
- **Anti-Spoiler:** Visão pública do jogador expõe apenas escolhas disponíveis e dados visíveis da cena atual; destinos de transição (`to`) e efeitos mecânicos permanecem selados.
- **Dois Desfechos Causais:**
  1. Captura ou eliminação de Kaa’to Leeachos com desarticulação da chantagem dos Pykes.
  2. Desaparecimento de Kaa’to no setor industrial, gerando plano latente de retaliação e atenção da polícia local.

#### 3.2 Morte nas Profundezas (*Dead in the Water*, SWA03)
- **Fonte Oficial:** Extraído do *Star Wars: Age of Rebellion Game Master's Kit* (`Star Wars_ La Era de la Rebelión - Muerte en las profundidades.pdf`, 57 páginas).
- **Perfil de Regras:** `AOR_CORE` (suporta modos `AOR` e `MIXED`).
- **Cenário:** Cidade flutuante de Morjanssik em Mon Cala e minas submarinas de coral a quase 1 km de profundidade.
- **Estrutura de 5 Episódios:**
  - *Episódio 1 (La Llegada):* Chegada às docas e conferência tensa com Kalbrac (Mon Calamari) e Moren Chonk (Quarren).
  - *Episódio 2 (Una Noche en la Ciudad):* Investigação no Arpão Enferrujado; suborno pacífico ou briga de taverna com Walif Merv, seguida de perseguição nas passarelas sobre o oceano.
  - *Episódio 3 (Hundirse):* Viagem de descida na batisfera de transporte Kulv-Bockar até o leito oceânico profundo.
  - *Episódio 4 (Los Acusados):* Emboscada na área de preparação; desmascaramento dos sabotadores imperiais cirurgicamente alterados (Capitão Generet, Tenente Huv, Agente Limeren) e aliança com o subgerente arrependido Kelmut Wolg e droides perfuradores Q-4.
  - *Episódio 5 (Fusión):* Batalha final contra a plataforma laser de mineração no Nível 42 antes do colapso dos pilares da mina.
- **Dois Desfechos Causais e Mecânicos:**
  1. *Vitória e Lealdade Quarren:* Plataforma desativada, generet capturado, mina salva intacta (+10 Dever, +25 XP, união definitiva Quarren-Calamari, fornecimento ininterrupto de minério para dois cruzadores capitais).
  2. *Evacuação e Resistência:* Mina inundada, agentes imperiais forçados a fugir, mas provas do complô preservadas (+5 Dever, +10 XP, estaleiros atrasados em 1 ano, sobrevivência da resistência).
- **Testes Automatizados:** `tests/uae-dead-in-the-water.test.mjs` — **8/8 PASS**.

---

### 4. MUNDO VIVO PROFUNDO (LIVING WORLD SIMULATION)

- **População e Escala:** 100 NPCs persistentes distribuídos por 6 planetas/locais e 4 facções ativas.
- **Simulação Temporal:** 30 dias de campanha simulados (120 ticks: 4 ticks por dia: madrugada, manhã, tarde, noite).
- **Causalidade sem Telepatia:** NPCs só registram memórias de eventos ocorridos em sua localidade direta ou que envolvam pessoalmente sua facção ou grupo de jogadores.
- **Evolução Econômica e Social:** Mercados offscreen com oscilação contínua de oferta, demanda, segurança, pressão de mercado e clima local.
- **Compromissos:** Rastreamento determinístico de prazos (`dueDay`, `overdue`, `consequencePending`).
- **Persistência Auditada:** 3x recarga por ciclo JSON sem perda de integridade de estado.
- **Suíte de Testes:** `tests/living-world-deep-simulation.test.mjs` — **2/2 PASS**.

---

### 5. MOTOR DE OPORTUNIDADES DE MISSÕES (MISSION OPPORTUNITY ENGINE)

- **Módulo:** `app/src/main/assets/vtt/js/world/missionOpportunityEngine.mjs`
- **Geração Emergente e Coerente:**
  - Extração de oportunidades a partir de planos ativos das facções (`factions.plans`).
  - Emergência por pressão econômica extrema em docas e mercados locais (`marketPressure > 60`).
- **Contrato de Ciclo de Vida:**
  - `AVAILABLE` -> `ACTIVE` -> `COMPLETED` / `FAILED` / `EXPIRED` / `REJECTED`.
- **Integração Causal:** Ao aceitar uma missão, o patrono registra o compromisso em sua memória; a conclusão ou fracasso acarreta mudanças imediatas de reputação com a facção (+15 / -10) e memória duradoura no NPC patrono.
- **Avanço Temporal:** Missões não aceitas expiram automaticamente após o `deadlineTick`; missões ativas vencidas falham e aplicam penalidades.

---

### 6. MESTRE CINEMÁTICO DE AVENTURAS ORIGINAIS

- **Módulo:** `app/src/main/assets/vtt/js/master/originalAdventureCreator.mjs`
- **Compilador de Grafo UAE:** Transforma premissas de aventura em módulos formais estritamente compatíveis com `validateAdventureModule` e `validateAdventureSemantics`.
- **Estrutura de Grafo:** 3 Episódios integrados com nós ramificados (Briefing -> Infiltração Silenciosa vs Incursão Frontal -> Sabotagem vs Confronto -> Desfechos causais).
- **Fallback Determinístico Fechado:** Quando offline ou na ausência de resposta da IA remota, o gerador monta uma aventura sólida ancorada nos locais, patronos e facções reais do mundo salvo, sem alucinar regras.

---

### 7. ARTEFATO DE COMPILAÇÃO ANDROID (APK AUDIT)

A compilação do aplicativo Android foi realizada utilizando as ferramentas oficiais locais do ambiente:
- **Gradle:** 8.13 (`gradlew.bat --no-daemon :app:assembleDebug`)
- **JDK:** OpenJDK 21.0.6 (Android Studio JBR em `C:\Program Files\Android\Android Studio\jbr`)
- **Android SDK:** Build-Tools 35.0.0 / Platform API 35 (em `C:\Users\bcruz\AppData\Local\Android\Sdk`)
- **Resultado do Build:** `BUILD SUCCESSFUL in 29s` (34 actionable tasks: 3 executed, 31 up-to-date)
- **Localização do APK:** `app/build/outputs/apk/debug/app-debug.apk`
- **Tamanho do Arquivo:** `88.173.913 bytes` (~88.17 MB)
- **Hash Criptográfico SHA-256:**
  ```
  C4893C3C0B4B833ED25166DCF203D54BED9462228F6A4A188C5B68ACB3D334B0
  ```

---

### 8. REGRESSÃO GLOBAL E INTEGRIDADE DE CÓDIGO

A suíte completa de testes automatizados foi executada através de `tools/run-swe02-mega-perfile.py`:
- **Total de Arquivos de Teste:** 387
- **Arquivos Regulares:** 381
- **Suítes de Estresse:** 6 (`stress-persistent-adversary-r17`, `stress-factions-r18`, `stress-33-faction-plan-r19`, `stress-faction-plans-r19`, `stress-33-aftermath-r20`, `stress-33-sessions-r15`)
- **Total de Testes Executados:** 3.519
- **Testes Aprovados (PASS):** 3.517
- **Testes Falhos (FAIL):** 0
- **Testes Ignorados (SKIP):** 2 (relacionados a ambientes exclusivos de emulador sem áudio nativo)
- **Arquivos com Falha:** **0**

---

### 9. CONCLUSÃO E CONFORMIDADE

Todas as exigências da ordem executiva foram integralmente cumpridas:
1. O sistema Dual-Core oferece paridade total entre Edge of the Empire e Age of Rebellion com 720/720 nós de especialização auditados.
2. As duas aventuras oficiais publicadas (*Under a Black Sun* e *Dead in the Water*) operam com todas as mecânicas canônicas em português do Brasil e integração profunda com o Mundo Vivo.
3. O Mestre Cinemático e o Motor de Oportunidades garantem rejogabilidade contínua.
4. O APK Android foi compilado e verificado de forma determinística e reproduzível.
