# Integração do Motor de Vida V2

## Pontos obrigatórios do host

1. **Carregar campanha:** carregar um save V2 existente ou envolver o legado V1 atual com `createLifeStore`. Chave de armazenamento precisa incluir sistema + mundo + timeline. Nunca criar novo legado em cada abertura.
2. **Confirmar ação:** após o motor nativo confirmar um acontecimento, emitir `event` com ID estável, atores, visibilidade, local e tag. Bindings traduzem tags conhecidas em fatos. Não usar o texto gerado como confirmação.
3. **Avançar tempo:** converter o calendário nativo em um contador estável de dias e emitir `advance`. Jornadas, descansos e intervalos entre aventuras devem usar o mesmo relógio.
4. **Preparar cena:** emitir `evaluate`, persistir, obter `runtime.scene`. O GM recebe somente o contexto GM autorizado; a UI/TTS recebe somente a projeção pública apropriada.
5. **Usar lembrança:** após uma lembrança ser efetivamente usada, emitir `consume_context` com os IDs usados, em vez de marcar todos os candidatos de forma indiscriminada.
6. **Receber escolha:** enviar `choose` somente após decisão do jogador; atualizar o jogo conforme a intenção e confirmar o resultado em evento posterior. `rest-requested` não significa que o descanso ocorreu.
7. **Encerrar aventura:** manter o save V2 e o legado. Atualizar o legado via `synchronizeLegacy` com mesmo universo. Reusar identidades ao importar a próxima aventura.
8. **Fora de cena:** o avanço de dias processa recursos e regras materiais de locais diferentes. Ofertas pessoais localizadas aguardam uma avaliação no local correspondente.

## Contrato com o adaptador de regras

- Uma condição de doença, magia, XP, item, reputação mecânica ou cura exige o adaptador do jogo.
- `food-price-factor` é um fato de cenário: o código de comércio deve consultá-lo para aplicar preços. Este núcleo não altera automaticamente um catálogo externo que nunca o consulta.
- `rival-pursuing=false` deve ser consultado pelo planejador de adversários.
- `bell-absence-clue` deve ser consultado pela construção da cena e catálogo de pistas.
- `workshop-open` e `market-open` devem ser consultados pela disponibilidade de serviços.
- Aposentadoria confirmada pode ser aplicada por `retire`, a partir da decisão real do usuário. O personagem permanece no histórico; nenhum sucessor recebe automaticamente seus feitos ou ficha.
- Modos de controle de companheiros são responsabilidade do host: não enviar `choose` para personagem controlado pelo jogador sem sua escolha; no modo com curadoria, aguardar a curadoria.

## Autoria de novas instâncias

`examples/receitas-vida.json` contém dados reais gerados pelo mesmo código das receitas testadas. Para adicionar uma promessa ou NPC, crie IDs próprios, condições, efeitos e bindings. As regras não executam texto de cenário como instrução. A mesma linguagem genérica atende aos novos casos.

Cada regra precisa declarar versão, condições, agência e efeitos. `player_choice` permite apenas efeitos `offer`. Resultados de escolhas só são aplicados pelo comando `choose`. Regras de mundo possuem origem causal nos fatos; escolhas e mudanças mecânicas não devem ser disfarçadas de efeitos de mundo pelo autor.

## Verificação no aplicativo antes de chamar de integrado

- Executar aventura A, confirmar eventos, encerrar e iniciar B sem perder entidades ou histórico.
- Fechar e reabrir o aplicativo três vezes, verificando fichas, timeline, ofertas e eventos.
- Verificar persistência IndexedDB em WebView, conflitos de revisão e restauração de backup.
- Confirmar consulta dos efeitos pelo comércio, planejador de NPCs, pistas e UI.
- Confirmar que a IA usa o contexto filtrado e que a UI/TTS não recebe o contexto do GM.
- Validar controles de companheiros, orçamento de apresentações e tempo no calendário real do sistema.
- Testar desempenho com volume representativo; as receitas pequenas e os testes de arquivo não constituem homologação de uma campanha de vários anos no Galaxy A57.

O pacote não faz alterações em branches, pipelines, motores TTS ou livros homologados.

## Ampliação V2.1

As tabelas de cruzamento e seus filtros estão descritos em `MATRIZES_V21.md`. A integração deve salvar o perfil correto, classificar acontecimentos confirmados com `lifeCauseId`, fornecer capacidades reais e usar as propostas reservadas conforme o orçamento narrativo. Os nove JSON completos estão em `data/matrices/packs/`.

## Atualização V2.2

A configuração da automação, os perfis pessoais e o ciclo dos arcos estão em `CORRECOES_V22.md`. Conecte o provedor na abertura do runtime e use os comandos normais de jogo; não é necessário solicitar um sorteio manual a cada cena.
