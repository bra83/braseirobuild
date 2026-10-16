# BRASEIRO STAR WARS EDGE — R54 P175 · 2026-10-13

## Base, recuperação e escopo

Base imutável: P174 (`BRASEIRO_STAR_WARS_EDGE_R54_P174_SOURCE_CHECKPOINT.zip`).
Checkpoint canônico desta execução: `BRASEIRO_STAR_WARS_EDGE_R54_P175_SOURCE_CHECKPOINT.zip`.
Android Studio: `versionCode=10175`, `versionName=1.0.175`. Bundle offline reconstruído: 82 módulos.
Nenhum dos oito fontes Java (incluindo TTS/Kokoro) foi modificado; SHA-256 de cada um idêntico ao P174.

## Implementações reais P175

Dez posições AoR passam a ter consumidores mecânicos executáveis:

- `COORDINACIÓN TÁCTICA` (2): teste real de Liderança, ação, seleção de companheiros, 1 Tensão por aliado e manobra gratuita persistida; cobertura defensiva consumível, com setback real no ataque inimigo.
- `COORDINACIÓN TÁCTICA MEJORADA` (2): limite de aliados igual ao dobro de Presença; Triunfo permite ação gratuita; sem Triunfo, mantém manobra e consome o teste (sem reroll gratuito).
- `LÍDER EXPERTO` (1): 2 Tensão para reduzir dificuldade da próxima Liderança em 1 (mínimo Fácil), uma vez por rodada, consumido no fluxo real de perícia.
- `ATAQUE COORDINADO` (1): manobra com aliados engajados; +1 Vantagem automática na próxima rolagem de combate de cada aliado na rodada, consumível uma vez.
- `EVASIÓN IMPECABLE` (2): ação de Pilotagem oposta, uma tentativa por encontro, bloqueio real de ataques da nave-alvo por rodadas iguais à Agilidade; persiste e expira.
- `CONOCIMIENTO DEL ENTORNO` (1): +1/+2 Impulso em Percepção/Vigilância de companheiro em alcance Curto/Bocajarro do veículo aliado registrado, com confirmação do Mestre.
- `SOLUCIÓN INGENIOSA` (1): teste de perícia com Astúcia no lugar da característica normal, uma vez por sessão, com persistência e antifraude de aquisição.

Comandos expostos em ponte Android e interface PT-BR. Ações atômicas; rollback em falha de armazenamento.

## Testes executados

- Regressão regular: **2526/2526**.
- Stress: **6/6**.
- Kokoro/TTS: **27/27**.
- Novos testes P175: **20/20**, incluindo cinco aventuras e três reaberturas por aventura.
- Verificação estrutural das cinco aventuras preservada. **Não** equivale à certificação editorial contra PDFs.
- Verificador global `tools/verify-final-source.mjs` excedeu o tempo da execução: **não certificado**.

## Pendências sem maquiar cobertura

AoR: **335/360 posições (93,1%)** com consumidores mecânicos; **25 pendentes** listadas em `evidence/P175/AOR_PENDING_25_POSITIONS.json`. Essa métrica não comprova correção integral dos efeitos contra os livros.
Expansões EotE/AoR: certificação integral pendente. Cinco aventuras: confronto completo com PDFs e jogo ponta a ponta pendentes. Android: APK não compilado, sem homologação física (Gradle/SDK indisponíveis neste ambiente).

## Próximo bloco prioritário

Implementar as 25 posições AoR restantes em famílias funcionais; integrar ações às telas e aos fluxos reais; executar regressão e validação semântica contra PDFs; só então compilar via BRELGA/Android Studio e homologar instalação + três reaberturas. Não declarar 100% antes disso.

Fonte mecânica consultada: Age of Rebellion Core Rulebook, pp. 144, 146, 151, 156; árvore FFG Star Wars Talent Trees (referência de cruzamento). Sem alegação de auditoria completa de PDFs.
