# BRASEIRO TALES VTT — Android Studio 1.3.21 candidato (build 28)

Projeto Android/Compose final para **Tales from the Loop — Tales/Kids**, com núcleo mecânico Flood/Teens isolado para expansão e biblioteca visual compartilhada.

## O que já está no source

- 13/13 telas/estados do VTT + Biblioteca/RAG como utilitário interno do MENU.
- abertura no MENU/Configurações, sem aventura demo forçada;
- NOVA AVENTURA ALEATÓRIA com histórico de seeds para evitar repetição exata e clima/horário variados;
- criação de Kid aleatório dentro dos limites canônicos de Tales/Kids;
- ícone de ajuda no topo com a ordem básica de jogo;
- hambúrguer e retorno da Biblioteca corrigidos para sempre alcançar o MENU;
- narrativa do Mestre com rolagem interna própria e textos longos do Motor Barbara;
- decisões e ação livre enviam intenção ao Mestre e exibem status/erro da chamada;
- prompt de ilustração separado da narrativa: somente dados visuais da cena;
- baseline visual 415×915 aprovada;
- footer aprovado travado no Android com 7 ícones lineares ciano;
- MAPA aprovado travado em base limpa + 9 POIs canônicos;
- KID com troca de foto e import/export JSON direto na ficha;
- biblioteca automática com 63 ilustrações contextuais individuais para Tales/Kids e Flood/Teens;
- assets reais TFTL incorporados;
- Motor Barbara com firewall Tales/Kids;
- Gemini: chave local protegida pelo Android Keystore, modelo de texto, modelo/voz TTS;
- Gemini TTS da narrativa visível como padrão; TTS Android só é usado quando o jogador o seleciona explicitamente;
- save/autosave local;
- backup portátil JSON export/import + backup automático opcional em pasta SAF;
- import/export de ficha Kid em JSON com validação;
- fluxo CENA → compartilhar prompt → Gemini → compartilhar imagem de volta → vincular somente à mesma cena;
- Biblioteca de PDFs local, extração PDFBox + OCR ML Kit e busca RAG;
- RAG conectado ao contexto do Motor Barbara;
- Problema Estendido, XP, Change e aniversário;
- 113/113 testes Tales PASS;
- 46/46 testes Flood PASS;
- 33/33 testes comparativos/regressões derivados da revisão de `DrOgres/tftloop` PASS;
- auditoria estática e auditoria de módulos de produto PASS.

## Abrir

1. Extraia o ZIP.
2. Abra a pasta `android/` no Android Studio.
3. Deixe o Android Studio sincronizar as dependências.
4. Configure a chave Gemini pelo **MENU** ou crie `.env` com `GEMINI_API_KEY=...`.
5. Para RAG: MENU → BIBLIOTECA & RAG → ADICIONAR PDFs.

## Auditoria comparativa 1.3.3

O repositório `DrOgres/tftloop` foi usado apenas como referência secundária de implementação e histórico de bugs. Nenhum código/assets foram copiados. A comparação reforçou regressões para Conditions, Push/reroll, XP, itens, relacionamentos e o firewall de pool zero entre Tales e Flood. Veja `docs/DROGRES_COMPARATIVE_AUDIT_1.3.3.md`.

## Importante

O source e os testes puros/estáticos foram validados aqui. O build Android completo não pôde ser executado neste ambiente porque a distribuição Gradle/dependências externas não estão disponíveis; o pacote permanece pronto para sincronização/build no Android Studio ou no workflow GitHub.

Documentação de QA em `docs/`.

## Fechamento final

- O app principal jogável está fechado no perfil **Tales/Kids**.
- O núcleo **Flood/Teens** permanece isolado e testado (46/46), sem contaminar as regras Tales; ele não é anunciado aqui como rota principal jogável até existir um profile router completo.
- A biblioteca visual de 63 cenas é suficiente para o fechamento atual; expansão futura para 100 imagens é opcional e não bloqueia o jogo.
- O único gate externo restante é abrir/sincronizar/compilar no Android Studio com acesso às dependências Gradle.

## R3 — stress validation

A source-level stress pass was added after R2. The pure Tales/Kids engine, Motor Barbara firewall and snapshot persistence were exercised across 170 simulated campaigns / 2,160 sessions / 171,900 turns, plus 250,000 reducer-fuzz events. One real snapshot defect was found and fixed: an intentionally empty scene-choice list could reload as the default choices. A permanent regression was added. See `docs/STRESS_CAMPAIGN_QA_R3.md`.

Android/Gradle/device/network-dependent features remain unexecuted in this environment and are not labeled DEVICE_PASS.
