# MATRIZ DE COBERTURA DE AVENTURAS PUBLICADAS

**Projeto:** Braseiro Star Wars: Edge of the Galaxy VTT  
**Motor:** Universal Adventure Engine (`ADVENTURE_SCHEMA_VERSION = 2`)  
**Atualização editorial pontual:** 2026-10-14 — P193 (módulos ainda sem certificação integral)  
**Testes atuais:** consultar `evidence/P193/TEST_SUMMARY.json` (regressão por arquivo, não suíte monolítica). **Certificação editorial integral dos PDFs e homologação Android: pendentes.**

---

## 1. Módulos publicados integrados e testados (não certificados editorialmente)

| ID do Módulo | Título (PT-BR) | Título Original | Código / Fonte | Modos Suportados | Perfil de Regras | Episódios | Clocks Persistentes | Status no Registro |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `under-black-sun` | Sob um Sol Negro | Under a Black Sun | SWEP01 | `EOTE`, `MIXED` | `EOTE_CORE` | 3 / 3 | `alertClock`, `momentum` | **ATIVO; REGRESSÃO AUTOMATIZADA APROVADA** |
| `beyond-the-rim` | Além da Orla | Beyond the Rim | SWE05 | `EOTE`, `MIXED` | `EOTE_CORE` | 3 / 3 | `imperialExposure`, `chase` | **ATIVO; REGRESSÃO AUTOMATIZADA APROVADA** |
| `long-arm-of-the-hutt` | O Longo Braço do Hutt | Long Arm of the Hutt | SWE02 | `EOTE`, `MIXED` | `EOTE_CORE` | 3 / 3 | `huttBounty` | **ATIVO; REGRESSÃO AUTOMATIZADA APROVADA** |
| `jewel-of-yavin` | A Joia de Yavin | The Jewel of Yavin | SWE09 | `EOTE`, `MIXED` | `EOTE_CORE` | 3 / 3 | `heistAlarm` | **ATIVO; REGRESSÃO AUTOMATIZADA APROVADA** |
| `mask-of-the-pirate-queen` | A Máscara da Rainha Pirata | Mask of the Pirate Queen | SWE13 | `EOTE`, `MIXED` | `EOTE_CORE` | 3 / 3 | `pirateAlert` | **ATIVO; REGRESSÃO AUTOMATIZADA APROVADA** |

---

## 2. Capacidades do Motor Universal em Cada Módulo

1. **Grafo de Cenas Anti-Spoiler:** Nenhum módulo vaza nós futuros, nós de desfecho ou segredos do Mestre para a visão do jogador (`antiSpoiler: true`).
2. **Execução Determinística:** Testes mecânicos, testes de perícias, pools de dados, consequências de Advantage/Threat e avanço de cenas são resolvidos sem dependência de LLM ou cálculo em tempo real de IA.
3. **Persistência Completa:** Suporte a salvar/reabrir estado, retomada de nós, acúmulo de histórico de testes e transições idempotentes.
4. **Living World Integration:** Fechamentos de episódios aplicam consequências diretas e memória persistente em facções e NPCs do mundo vivo (`ensureLivingWorldDepth`).
5. **Adapters Integrados no Bundle UI Bridge:** Todos os 5 módulos estão embarcados e exportados no `uiBridge.bundle.js` (91 módulos empacotados na versão P192), permitindo o empacotamento offline no WebView Android; o APK ainda não foi homologado.

---

## P174 — verificação incremental de cinco aventuras e mecânicas AoR

- Cinco aventuras reais: criação de sessão publicada, uso de Sistemas redundantes em campanha MISTA e três reaberturas por aventura; estados UAE permanecem idênticos. Este teste **não** comprova fidelidade narrativa integral aos PDFs.
- AoR: **325/360** posições com consumidores declarados, 35 sem consumidor. Sistemas redundantes e Peça defeituosa foram conectados a teste de Mecânica, inventário, aprovação do Mestre, recibos de uso por sessão e persistência; atuação sobre dispositivos não modelados e equivalência entre modelos distintos ainda não implementadas.
- Kokoro: 8 fontes Java byte-idênticas a P173; 27 testes host aprovados. Sem validação acústica física.
- Android: versão 1.0.174; bundle com 82 módulos. Gradle 8.13 indisponível localmente; nenhuma compilação Android aprovada.
- Fonte oficial: árvores AoR e descrições de talentos cruzadas com referências ao Core; o PDF espanhol disponível nesta sessão tem 50 páginas digitalizadas sem texto extraível e não permite certificação integral do livro.

## P192 — limites editoriais

Tabela 2-1 de SWE09 comparada pontualmente ao PDF p.70 e opções registradas; patrulha sem encontro automático, assinatura restrita a G-100, bônus sem liquidação. A certificação integral das cinco aventuras e das expansões EotE/AoR permanece pendente.

## P193 — escopo específico SWE09 (não certificação integral)

- Tabela 2-1: equipes Wing Guard derivadas dos recibos, com ETA e encontros reais de combate disponíveis quando a equipe chega; perfis oficiais de oficiais e sargento JoY pp.12–13, sendo **somente oficiais** usados na equipe bancária (quantidade igual aos PCs).
- Registro de resolução, proteção contra encontros inventados, múltiplas equipes, investigação do local se os PCs partiram, bloqueio de avanço em confronto pendente.
- Assinatura eletrônica aplicada a G-100 e testes de slicing de Cloud City identificados explicitamente; não a testes de Computadores sem slicing.
- Limites: combate exige acionamento da UI/Mestre, não auto-inicia ao atingir ETA; talentos de manobra dos guardas ainda sem efeito automático; substituição de ferramentas e liquidação não implementadas. PDFs de cinco aventuras e expansões ainda não certificados integralmente.

## P234 — escopo congelado de cinco aventuras e arquitetura híbrida

Os cinco módulos acima são o registro canônico exclusivo de aventuras, protegido em `adventure/hybridFiveAdventurePolicy.mjs` e conferido na inicialização do `publishedAdventureRegistry.mjs`. Fontes PDF importadas e classificadas como `AVENTURA` que não correspondam a um desses cinco módulos **não são retornadas pelo RAG ao Mestre**. Erratas, livros-base e suplementos de regras continuam consultáveis.

A auditoria automática `validation/P234_HYBRID_COVERAGE_LEDGER.json` registra **100 nós de grafo alcançáveis**, **141 verificações de perícia declaradas** e **três episódios declarados em cada uma das cinco aventuras**. São métricas de estrutura de código, **não** prova de cobertura integral dos PDFs. A comparação cena a cena, todas as fichas oficiais, recompensas, consequências e 360 consumidores AoR permanecem **NÃO CERTIFICADOS**.

Arquitetura: **código determinístico para efeitos, RAG somente para referência e Gemini apenas para narração/propostas validadas**. Falha de Gemini ou referência inventada recua para registro de intenção do jogador, sem aplicação de efeitos. Ver `P234_HYBRID_SCOPE_AND_CLOSURE.md`.

A compilação Gradle, o APK e a homologação física serão executados **somente após** a certificação editorial e semântica completa, conforme solicitação do usuário.
