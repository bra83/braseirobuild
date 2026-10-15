# Criação e edição D&D — fase 1, 15/10/2026

Estado inicial: worktree limpo. Lidos HANDOFF_NEXT_AGENT.md, CHECKPOINT_STATUS.md e docs/CORE_BASE_JOGAVEL_STATUS_20261014.md. Preservadas as mudanças anteriores de UI e o perfil padrão 2024 já existente.

Implementação nesta rodada, em MainActivity:

- Edição usa rules_source da ficha; a campanha 2024 não converte silenciosamente um personagem 2014. Ausência de proveniência usa fallback conservador 2014.
- Coleta preserva o JSON original e sobrescreve os campos editados, evitando descartar metadados estruturados não representados pelos controles. Gerar/importar uma ficha troca essa base.
- Nome e demais campos narrativos podem ser digitados no menu existente, com Aplicar/Cancelar; geração e Mythic continuam disponíveis.
- Salvamento rejeita entradas numéricas inválidas antes das normalizações e continua usando DndRulesEngine, CharacterRulesValidator e saveCharacterSheetAtomic. Erros explícitos das escolhas Run136 bloqueiam o save. Guarda contra toque repetido depois de uma gravação bem-sucedida.
- PV zero não vira cura ao salvar. Recálculo por identidade/nível preserva dano recebido e respeita a opção de recálculo automático.
- Mudança de espécie/antecedente/nível preserva atributos e escolhas personalizadas; campos ainda iguais ao padrão gerado acompanham a nova identidade. Troca de classe atualiza a lista de magias. A matriz sugerida continua explícita.
- +2/+1 de antecedente usa o motor Core2024EberronCharacterEngine para validar escolhas. Erros mantêm o diálogo aberto; aplicação repetida não acumula bônus; edições manuais posteriores são preservadas. Trocar antecedente remove o bônus anterior e sua configuração. Trocar espécie invalida as configurações dependentes.
- Inventário mantém quantidades, estado e itens fora do catálogo ao confirmar o seletor; Cancelar não aplica mudanças.
- Rótulos de subclasse indisponível não são gravados como nome de subclasse. Controles receberam descrições de acessibilidade para navegação/testes sem acessar CharacterFormRefs.

18 testes novos em CharacterCreationUiRobolectricTest usam telas e controles de produção: campanha, criação, nome, classe/espécie, atributos, antecedente, perícia por losango, inventário, magia, subclasse, confirmação/cancelamento, duplo save, PV zero, ficha 2014 e reabertura SQLite. Reflexão apenas para injetar dependências reais/abrir telas sem serviços de startup. Não substituem motores por mocks.

## Validação executada e infraestrutura

- Parser do JDK: os dois Java alterados/adicionados passaram, zero erros sintáticos. Isto NÃO comprova compilação Android nem execução de testes.
- git diff --check: passou.
- Tentativa real de :app:testDebugUnitTest, filtrando a suíte nova, PlayableUiRobolectricTest e CoreRulesProfileDefaultRobolectricTest: wrapper Gradle 8.11.1 falhou antes da configuração por SocketException: Operation not permitted no download. Nenhum teste Robolectric executou.
- Alternativa com Gradle instalado, offline: falha de inicialização FileLockContentionHandler / Could not determine a usable wildcard IP. Nenhum teste executou.
- Logs em docs/evidence/character-ui-phase1-20261015/. Sem XML novo e sem alegação de suíte verde.

Reexecução em ambiente Android habilitado:

```sh
./gradlew :app:testDebugUnitTest \
  --tests com.braseiro.dnd5e.CharacterCreationUiRobolectricTest \
  --tests com.braseiro.dnd5e.PlayableUiRobolectricTest \
  --tests com.braseiro.dnd5e.CoreRulesProfileDefaultRobolectricTest \
  -Pbraseiro.qaLiteAssets=true --no-daemon
```

## Dependências e limites do fechamento

AppDb.java permanece intocado por causa do PR #35. A UI depende da implementação corrente de saveCharacterSheetAtomic e da reconciliação de recursos; os testes de persistência precisam executar novamente depois da integração desse PR. Não foi constatado um novo defeito transacional por execução nesta rodada.

O fechamento integral NÃO está homologado: faltam execução Android/Robolectric e auditoria completa das restrições de escolha (quantidades de perícias, magias por classe/nível, todos os traços e talentos de origem). Os catálogos e motores existentes continuam presentes; não houve expansão de suplementos, aventura ou executor mecânico. Pendências de origem já expostas pelo motor continuam representadas como pendências; este bloco não transforma todo rascunho em ficha integralmente validada.

Não alterados Mystara, outros VTTs, executor de sopros/PR #33, DB61, Backup21, package, versões e assinatura. Gemini/RAG não receberam autoridade mecânica. Sem commit, push, merge ou APK.
