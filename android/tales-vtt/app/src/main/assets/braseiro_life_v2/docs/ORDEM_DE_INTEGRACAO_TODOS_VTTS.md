# ORDEM TÉCNICA — IMPORTADOR UNIVERSAL DE AVENTURAS E LEGADO PERMANENTE (V1)

**Estado deste pacote (20/10/2026):** núcleo de referência isolado e testado em Node; **não instalado nos APKs**, **não integrado a GitHub** e **não homologado em dispositivos**. Não apresentar como implementação concluída de qualquer VTT.

## 1. Regra inegociável

`Aventura` é conteúdo e estado temporário. `Personagem`, `Mundo` e `Legado` são entidades persistentes independentes. Ao iniciar uma nova aventura, nunca zerar/reescrever personagens, relações, mundo, reputação, feitos, equipamentos e estados derivados somente porque a aventura mudou.

Escopo: persistência entre aventuras no **mesmo VTT/mundo/linha temporal**. Não misturar Faerûn, as Terras Proibidas, Terras Roubadas, Terra-média e galáxia de Star Wars; transferência entre universos, se desejada futuramente, exigirá consentimento e conversor explícito.

## 2. Arquitetura única, regras diferentes

- **AdventurePackage**: conteúdo imutável validado (`braseiro.adventure/v1`), com identificador estável, sistema, mundo, premissa, texto para jogadores, passado oculto, segredos GM-only, locais, encontros, inimigos com referência às regras, NPCs, objetivos e consequências possíveis. JSON importado é dado ficcional, nunca código executável ou ordem de sistema.
- **AdventureInstance**: instância jogada, personagens, locais, pistas, segredos efetivamente revelados, objetivos, eventos, status e decisões. Carregar/arquivar/encerrar não apaga `CharacterLegacy`.
- **WorldTimeline**: calendário canônico, mundo vivo, NPCs, facções, mortes, cidades, guerras, economia e notícias. Apenas alterações verificadas pelo jogo mudam o cânone.
- **CharacterLegacy**: registro imutável de feitos e relações, cruzado por `characterId` estável e `worldId`/`timelineId`. Nunca depender de `adventureId` para existir; ele é só proveniência.
- **RulesAdapter**: o motor mecânico de cada jogo verifica combate, testes, recompensas, progressão, morte, viagem e efeitos. Não aceitar que um campo de JSON ou um texto de IA transforme "dragão talvez morto" em "dragão morto".
- **KnowledgePropagation**: fatos objetivos versus conhecimento de cada NPC/região versus rumores/distorções. Uma testemunha, mensageiro, tempo de viagem ou mecanismo verificado espalha notícias; NPC desconhecido não ganha conhecimento mágico.
- **NarrativeAdapter**: separa contexto privado do Mestre de contexto público e conhecimento individual de NPCs. O Gemini e o Motor Bárbara usam o estado verificado para narrar consequências, sem reescrever o registro persistido por conta própria.

## 3. Tela em todos os VTTs

Menu **Aventuras**: `Nova aventura` / `Importar JSON` / `Aventuras importadas` / `Continuar` / `Pausar` / `Encerrar` / `Histórico`. Ao importar, mostrar título, autor, sistema compatível, mundo, níveis sugeridos, locais e recursos faltantes; validar antes de iniciar; escolher personagens existentes, nunca obrigar criar novos. Mostrar avisos para mapa/hex, inimigos/itens sem referência ao compêndio, conteúdo de outro sistema e arquivos grandes.

Na **ficha de personagem**, aba **Crônicas e Legado**: linha do tempo, feitos, títulos, mortes marcantes, recompensas, relações, rumores locais e consequências. O usuário pode adicionar nota narrativa manual com provenance `player_note`, **não** transformar nota em vitória confirmada sem ação do motor. Disponibilizar histórico exportável e backup/restauração com identidade estável.

A aventura gerada por IA precisa usar o **mesmo formato**; não criar um caminho de salvamento diferente para aventuras procedurais.

## 4. Fluxo transacional obrigatório

1. Abrir documento local com seletor Android/WebView SAF ou input de arquivo. Nunca executar `eval`, scripts, HTML, links externos ou comandos embutidos no JSON.
2. Aplicar limite de tamanho e validação (schema e referências cruzadas), compatibilidade do sistema e mundo, verificação de IDs estáveis e backup antes de importar.
3. Importar como pacote *sem mutar o mundo*. Resolver referências de mapa/hex, NPC, token e bestiário; solicitar correção onde não houver correspondência canônica.
4. Vincular personagens existentes, preservar fichas e legados, iniciar instância. Aventura deve aceitar caminhos alternativos e resultados imprevistos.
5. Receber do motor determinístico apenas **eventos validados**; registrar ID imutável, timestamp de jogo, testemunhas, local, atores e papéis distintos (autor, participante, testemunha). Confirmar a gravação em transação atômica ANTES de apresentar sucesso ao usuário.
6. Atualizar mundo, NPCs e reputação com regras; criar rumores progressivos sem inventar conhecimento prévio.
7. Ao pausar/encerrar, gravar status e resumo da aventura, sem apagar `CharacterLegacy`/mundo. Retomar e importar nova aventura recuperando o legado.
8. Ao criar/gerar uma nova aventura, carregar primeiro os fatos persistidos, contexto de conhecimento do local, relações e consequências abertas; limitar resumo ao contexto relevante, sem eliminar histórico bruto.

## 5. Persistência e migração

- Banco preferencial no host: **SQLite com WAL e transações** onde já exista SQLite; **IndexedDB** nos VTTs web. Tratar SharedPreferences como preferências, não como única base de eventos.
- Criar tabela/coleção `adventure_packages`, `adventure_instances`, `legacy_events`, `character_event_links`, `npc_knowledge`, `world_consequences` com chaves e índices por `system`, `worldId`, `timelineId`, `characterId`, `adventureId`, `regionId`.
- ID de evento único e revisão do registro impedem crédito duplicado ou sobrescrita silenciosa em sincronização. Backups do mundo + personagens + aventuras **na mesma versão consistente**; restauração testa 3 reaberturas.
- Migrar saves legados sem eliminar `CampaignState`/`adventureRuntime` atuais; converter/adicionar campos novos sem substituir dados anteriores. Em O Um Anel, `adventureRuntime` já está dentro de `CampaignState`, mas o legado precisa de armazenamento independente.
- Se personagem morre, o seu arco termina, mas o registro histórico do mundo sobre ele e as consequências de suas ações não devem desaparecer. O personagem só reaparece jogável quando regras/continuidade autorizarem.
- Mudanças em eventos verificados: não editar o fato silenciosamente; gerar evento de correção com trilha de auditoria.

## 6. Especificidades por VTT

| VTT | Adaptador a implantar |
| --- | --- |
| D&D 5e Normal | Activity/file picker, `AppDb` SQLite, referências aos compêndios 2024/2025/2026, histórico da ficha e eventos de combate válidos. Preservar Mystara congelado e não importar conteúdo de aventuras oficiais automaticamente. |
| Forbidden Lands | Importação JS/WebView, estado permanente IndexedDB/host, hexgrid validado, encontro/combate/viagem com regras Free League; nunca substituir mapas ou cenas e áreas já auditadas. |
| Pathfinder 2e Kingmaker | Registro por herói e reino, hex do Atlas oficial, leis/regras PF2e, mudanças de facção/reino e consequências de guerras; não quebrar o fluxo Restov/New Stetven. |
| Star Wars EotE + AoR | Um importador nas duas modalidades, diferenças Obligation/Duty, reputação por setor, facções, naves, dívidas e campanhas; eventos de personagens seguem regras EotE/AoR. |
| O Um Anel 2e | Estender `adventureTypes.ts`, `adventureState.ts`, `CampaignState` e fluxo de seleção; preservar jornadas, Patronos, reputação local, Anos e progressão sem entregar segredos do Mestre. |
| GURPS, Cyberpunk RED e demais | Mesmo contrato JSON/legado; validar regras, sistemas de perícias, combate, economia e mundo via adaptadores próprios; não presumir que compartilham tabelas de D&D. |

## 7. Integração com Mestre/IA

- Injetar **fatos canônicos** ao Mestre, não afirmações textuais soltas; fatos pessoais desconhecidos da cena ficam no canal privado, com instrução explícita de que NPCs não podem agir como se os conhecessem.
- Manter segredos GM-only até condições de revelação verificadas; a abertura não conta toda a trama ao jogador.
- Motor Bárbara narra de maneira sensorial e com agência. Não inserir resumos mecânicos no texto da cena e não iniciar automaticamente com “vocês”.
- Limitar contexto ao relevante sem truncar a base do legado. Gerar índices/RAG por NPC, região, personagem e período, preservando fonte/IDs.
- Um NPC que sobrevive à aventura continua existindo, com agenda, crenças, dívidas e relações, salvo quando houver transição de mundo confirmada.

## 8. Gates de aceite antes de APK final

A. Importação: JSON válido abre; arquivo inválido ou outro sistema rejeitado com mensagem clara; segredo não vaza; referências cruzadas e mapa/hex checadas; arquivos abusivos rejeitados.

B. Persistência: iniciar missão A, salvar, fechar/reabrir, vencer dragão **validado pelo sistema**, fechar/reabrir, terminar A, importar B, fechar/reabrir, verificar o mesmo evento e personagem por ID. Fazer 3 reaberturas e backup/restore.

C. Reconhecimento: aldeão testemunha reconhece; desconhecido em região distante não; rumor chega após tempo plausível; fato persiste por anos; NPC morto não volta à vida por erro de memória.

D. Combate: IA não confere vitória, XP, item ou fama sozinha; eventos repetidos não duplicam; derrota ou negociação também produzem histórico; mortes e efeitos seguem regras oficiais.

E. Regressão: iniciar aventuras originais, sandbox, retomar campanha existente, trocar personagens e TTS; nada perde dados, travamentos, imagens, mapas, menus, regras ou chaves de configuração.

F. Android: teste físico no Galaxy A57 e testes no S23 Ultra quando aplicável; validar seletor de arquivos, permissões, app em segundo plano, TTS durante mudança de cenas e reabertura.

**Somente após todos os gates** iniciar compilação BRELGA para cada projeto, verificar SHA/assinatura e fornecer APKs verdadeiramente publicados.

## 9. O que já está implementado neste pacote e o que não está

Núcleo de referência `src/legacy-engine.mjs` com validação estrutural, importação isolada, cadastro de personagens por identidade estável, eventos verificados, prevenção de duplicidade, continuidade entre aventuras, contexto separado Mestre/jogador, reconhecimento regional básico, encerramento sem apagar legado. Exemplo de aventura original e 20 testes unitários.

**Ainda falta**: integração de UI, armazenamento real SQLite/IndexedDB, transações do host, associação a compêndios oficiais, conhecimento NPC com propagação temporal real, migração de todos os saves e instalação em cada branch. Este pacote é referência funcional, não atualização dos aplicativos.
