# HANDOFF CANÔNICO — BRASEIRO D&D 5e / VTT Android
## Continuidade após RUN190 — 08/10/2026


**DESTINATÁRIO:** outro chat, Antigravity, Codex ou agente de engenharia Android/Java e sistemas de RPG.
**IDIOMA OBRIGATÓRIO:** português brasileiro (PT-BR), inclusive UI, fichas, mensagens, erros, prompts e narração.
**NÃO PARTIR DE PROJETO NOVO. NÃO DESCARTAR O QUE JÁ ESTÁ INTEGRADO.**
**STATUS DE CERTIFICAÇÃO MECÂNICA:** NÃO É 100% CERTIFICADO. Auditoria estática extensiva foi executada; auditoria funcional livro a livro e testes em dispositivo ainda faltam. Qualquer afirmação diferente é incorreta.


### 1. Estado canônico verificado — não confundir builds


**ÚLTIMA BUILD PUBLICADA E COMPILADA: BRASEIRO_DND_6.6.4_RUN190_DEBUG.apk**
- Versão nominal: 6.6.4-r190; versionCode: 606490; package/applicationId: com.braseiro.dnd5e.
- Release: https://github.com/bra83/braseirobuild/releases/tag/DND-6.6.4-RUN190
- APK direto: https://github.com/bra83/braseirobuild/releases/download/DND-6.6.4-RUN190/BRASEIRO_DND_6.6.4_RUN190_DEBUG.apk
- SHA-256 oficial publicado junto: https://github.com/bra83/braseirobuild/releases/download/DND-6.6.4-RUN190/BRASEIRO_DND_6.6.4_RUN190_DEBUG.apk.sha256
- Comprovante da compilação: https://github.com/bra83/braseirobuild/releases/download/DND-6.6.4-RUN190/RUN190_BUILD_PROOF.txt
- Workflow BRELGA bem-sucedido: https://github.com/bra83/braseirobuild/actions/runs/37742627681
- Execução anterior que falhou: https://github.com/bra83/braseirobuild/actions/runs/37727326830 (erro na validação do NOME DA VERSÃO: workflow verificava r189-debug em APK r190-debug; NÃO tratar como última execução).
- APK publicado com tamanho 377.231.438 bytes segundo a release; conferir novamente o hash do download antes de instalar.
- GitHub Actions da execução 37742627681: reconstrução RUN186–RUN189, testes de contrato, patch RUN190, compile Java, tarefa Gradle, assemble APK, verificação de versão/certificado/sha, upload e publicação = SUCCESS.
- NÃO significa testes de interface no aparelho. Esses testes NÃO foram comprovados.
- Nunca exigir desinstalação sem antes comparar applicationId, versionCode, assinatura, migrações SQLite e backups da campanha. A intenção é atualizar sobre RUN189/RUN188 sem perder dados.


**Último ZIP integral da fonte com backup documentado:** BRASEIRO_DND_6.6.4_RUN189_SOURCE_FULL.zip, na Biblioteca do ChatGPT em /DND_SANDBOX_WORKING; cópia em sete partes no Google Drive, mesmo diretório.
**RUN190:** APK publicado + patch de fonte/versionamento no GitHub e ZIP do patch/auditoria nos dois backups. NÃO confundir patch RUN190 com um ZIP integral de fonte RUN190. Criar imediatamente checkpoint de fonte RUN190 completa antes da próxima alteração.


### 2. Repositórios e backups — nunca voltar a perder arquivos


- Repositório: https://github.com/bra83/braseirobuild
- Pasta principal do projeto D&D no Drive: https://drive.google.com/drive/folders/1v_KCxY1KNhazW1pqJTHgUSfs1yyuH_wp (DND_SANDBOX_WORKING).
- Pasta adicional de materiais D&D 5e: https://drive.google.com/drive/folders/13wB-KfYCe2k5xGmb-z2D09KRDJtSyfHu
- Fonte RUN180 em sete partes com SHA: https://drive.google.com/drive/folders/1Lc4qFMjb36z8UWebHAqolCi00iSbtseH (RUN180_SOURCE_PARTS).
- RUN189 fonte completa: Biblioteca ChatGPT /DND_SANDBOX_WORKING/BRASEIRO_DND_6.6.4_RUN189_SOURCE_FULL.zip; Drive /DND_SANDBOX_WORKING/BRASEIRO_DND_6.6.4_RUN189_SOURCE_FULL.zip.part00 a part06 + RUN189_SOURCE_PARTS_MANIFEST.json.
- RUN189 APK: https://github.com/bra83/braseirobuild/releases/tag/DND-6.6.4-RUN189; preservado também na Biblioteca e em oito partes no Drive.
- Patch + relatório RUN190: https://drive.google.com/file/d/15PU4p81FLlp7URDc7abiSAZ52BhWOljU/view
- Auditoria extensiva, documento principal: https://drive.google.com/file/d/18yI6j1AQ_BYntHHJ--mSzT99KpVlgtVk/view
- Inventário de 387 fontes Java/Python e hashes: https://drive.google.com/file/d/1D7pocdWXCg7ngi434cuifrZXRGYPGqGt/view
- Resultado dos 22 gates de auditoria estática: https://drive.google.com/file/d/1qWjrNZFeTKi60Zlv9eurymvyGTfMZk_s/view
- Auditoria preliminar: https://drive.google.com/file/d/11IUXgm0plV51gbhOEs8ME00sY2mx8Gb7/view
- Especificação universal de companheiros autônomos e curadoria: https://drive.google.com/file/d/1Ilu85WP9ka3ZDmNysHPJAb8h0zK1tEJC/view
- Perfil humano universal do Mestre: https://github.com/bra83/braseirobuild/blob/main/docs/BRASEIRO_PERFIL_UNIVERSAL_MESTRE_HUMANO_DINAMICO.md


**Política permanente de backup:** a cada RUN significativa, salvar (1) fonte Android Studio integral, (2) APK assinado, (3) manifestos SHA-256 da fonte e APK, (4) relatórios de testes, (5) patch reproduzível e ordem de aplicação, em **DOIS locais persistentes**: Biblioteca ChatGPT e Google Drive/DND_SANDBOX_WORKING. Confirmar que a cópia aparece de fato nos dois e que hashes batem. Não apagar ZIP, fontes ou workspaces sem essa checagem. Nunca depender somente dos artefatos GitHub Actions sujeitos à expiração.


### 3. Recuperação/reconstrução exata no self-hosted BRELGA


Runner de GitHub Actions: BRELGA, Windows X64, self-hosted.
Fonte canônica antiga no próprio runner: C:\DND181\extract\dnd_work
**NUNCA apagar ou sobrescrever C:\DND181**. Clonar para workspace separado (ex.: C:\DND191\work).
Workflows anteriores usam esta ordem de patches sobre fonte RUN181:
1. ci/dnd-run185/patch.py
2. ci/dnd-run186/patch.py
3. ci/dnd-run187/patch.py
4. ci/dnd-run188/patch.py
5. ci/dnd-run189/patch.py
6. ci/dnd-run190/patch.py
Documentar os hashes/commits exatos de todos os patches ao fazer a próxima build. A origem RUN181 já contempla o avanço RUN180→RUN181; NÃO reaplicar RUN181 sobre ela sem verificação de idempotência.
A referência executável mais recente é:
https://github.com/bra83/braseirobuild/blob/main/.github/workflows/build-dnd-run190-rule-curiosity-brelga.yml
Arquivos RUN190:
https://github.com/bra83/braseirobuild/tree/main/ci/dnd-run190
Em caso de falha do runner, ler logs do job e CORRIGIR A CAUSA. Falhas históricas: variável ANDROID_HOME não propagada para outra etapa; uso errado de javac no PATH; insuficiência de disco em workspaces temporários. Não remover arquivos sem backup. SHA e assinatura devem ser conferidos.


**PRIMEIRA TAREFA DO NOVO CHAT:** recuperar a fonte integrada RUN190 da reconstrução no runner, gerar ZIP completo com pasta android/, Gradle wrapper, fontes, assets, version.properties, configurações e dependências, verificar SHA-256 e salvar nos DOIS locais persistentes. Opcionalmente preservá-la como release/checkpoint identificado por RUN. Manter o APK RUN190 publicado intacto.


### 4. O que a RUN190 acrescentou — Curiosidade de Regra


O usuário solicitou **ícone na interface que apresente uma curiosidade sobre regra/mecânica diferente a cada cena, com explicação, livro e página EXATA**, para aprender materiais desconhecidos.
Implementado no RUN190:
- Ícone 📖 nos fluxos de narração principal, Strixhaven e modo seguro.
- Modal em PT-BR com título, explicação, exemplo na mesa, identificação da obra, página impressa e página no arquivo PDF; botão ABRIR LIVRO no Drive.
- Catálogo local de 14 curiosidades **verificadas** no livro Dungeon Master's Guide (2024), página impressa com deslocamento de +4 na posição do PDF.
- Referência do PDF verificado: https://drive.google.com/file/d/1FDZwdunnLxO3FG85N-hMKEG9UTwmWBEl/view
- Java: ci/dnd-run190/java/RuleCuriosityCatalog.java; integração: ci/dnd-run190/patch.py.
- Seleção salva nas preferências por perfil/mundo; ao detectar nova identidade de cena (local + narração) troca a entrada; reabrir o modal não deve rodar outra dica; tocar no ícone não avança relógio/mundo/ficha, não consome recursos.
- A curiosidade NÃO é prova de que a regra escolhida já opera automaticamente no motor.
- O catálogo ainda NÃO percorre todos os materiais, somente 14 dicas de UM livro. Próximas ampliações exigem verificar obra, edição, texto e página impressa/PDF antes de adicionar; NÃO inventar páginas pelo Gemini. O livro fica no Drive, não é embutido no APK.
- Testes estáticos/Java do catálogo foram aprovados no BRELGA; testes de toque e troca de cena em aparelhos Galaxy continuam pendentes.


### 5. Recursos prévios que devem ser preservados


RUN186 — Eberron/Faerûn + Strixhaven acadêmico, catálogo e interfaces de regras.
RUN187 — Perfil Mestre Humano Dinâmico: narrativa sensorial, NPCs, motivações, memória e consequências somente após COMMITTED no banco.
RUN188 — **Gemini-first**: Gemini padrão; se falhar, botão TENTAR NOVAMENTE (GEMINI), sem transição automática para offline; MESTRE OFFLINE separado e somente por escolha explícita do jogador. Falha não deve avançar cena ou gastar recurso; ação pendente é preservada. Uso pontual offline não altera o padrão da próxima ação.
RUN189 — **Companheiros autônomos/curadoria**: três modos individuais em cada ficha:
- EU CONTROLO — nenhuma decisão automática do Mestre pelo jogador;
- MESTRE CONTROLA — companheiro pode agir com motivos, personalidade e conhecimento limitados;
- MESTRE COM CURADORIA — proposta, motivo, risco, APROVAR/ALTERAR/RECUSAR, sem aplicar consequências antes da resolução validada.
- Botão O QUE OS COMPANHEIROS ACHAM? para destravar impasses sem avançar a cena.
- SQLite DB_VERSION 61 e tabela companion_proposals; salvamentos e migrações precisam sobreviver reinícios/atualizações.
- Testes de contrato SQL com fila, duplicações, aprovação/recusa, propostas obsoletas e isolamento entre campanhas passaram; NÃO há teste prolongado no aparelho real comprovado.
- Autonomia não significa ação aleatória: deve nascer de percepção → hipótese → motivação → proposta/decisão. Companheiros podem errar, discordar, recusar ordens coerentemente, investigar e ajudar a destravar história sem revelar segredos e sem tirar o protagonismo do usuário.
Outros subsistemas a preservar: personagem e evolução, mapa/hex, importação de tokens/retratos pelo Gemini, clima, tempo, mundo vivo, NPCs, inventário, combate e TTS Android (incluindo seleção de motores Charon/Kokoro quando instalados; VTT não embute/copia o modelo neural). Não permitir retrocessos silenciosos.


### 6. Auditoria: resultados verificáveis e VEREDITO


A extensa auditoria de 08/10/2026 inspecionou programaticamente 387 arquivos de fonte Java/Python (325 Java/62 Python), criou inventário por caminho/tamanho/SHA, rodou 22 verificações estáticas direcionadas, comparou a RUN190 com a RUN189 e verificou 14 citações bibliográficas de um PDF.
**ISSO NÃO É UMA AUDITORIA FUNCIONAL DE TODOS OS LIVROS**. Não analisou exaustivamente todos os PDFs de escopo; não reproduziu todas as mecânicas fim a fim em Android.


Achados impeditivos (IDs consistentes com o relatório):
A-001 CRÍTICO: BookMechanicsScopeEngine.coverage() informa complete=false, target_known=false, book_mechanics_scope_review_pending. Inventário HISTÓRICO de 27/09 com 177 PDFs D&D em escopo, não atualizado nem exaustivamente auditado. NÃO certificar 100%.
A-002 CRÍTICO: RuleRetrievalPolicy.decide() mantém caminhos de consulta a índice/manual para regras específicas; RAG-off integral de gameplay NÃO comprovado. Consulta pedagógica é diferente de dependência para resolver mecânica.
A-003 ALTO: Eberron/Faerûn mostram formulário técnico showRun163ActionDialog com ID canônico, número manual e JSON manual. Usuário fotografou esses campos. Corrigir integração UI→regra→rolagem→efeito→commit; não exigir JSON para jogar.
A-004 ALTO: combate atual inclui iniciativa registrada externamente e edição manual de Durações/Recursos em JSON. É uma rota técnica parcial; examinar, não alegar que todos os cálculos sejam automáticos.
A-005 ALTO: ContentExecutionAuditEngine distingue catálogo conhecido de efeito realmente completo (countEffectPipelineComplete, state_rule_gated, pending, auto_rolls=false). Quantidades de statblocks/rulings em catálogos não são testes de efeitos.
A-006 ALTO: relatórios históricos RUN175/176/178 mostram rolagens/reações/durações parcialmente manuais mesmo com cobertura textual PASS.
A-007 ALTO: RUN189_BUILD_PROOF registra DEVICE_END_TO_END_TEST=NOT_RUN e UI_INSTRUMENTATION_TEST=NOT_RUN; compilar APK não é testar em aparelho.
A-008 ALTO: RUN189 autonomia e curadoria têm testes SQL/estáticos, mas sem comprovação de campanhas longas em aparelho/rede/offline.
A-009 MÉDIO: narrativa de UI sobre consultar obrigatoriamente TODOS os manuais não corresponde exatamente à RuleRetrievalPolicy; esclarecer política de consulta.
A-010 MÉDIO: PDF DMG 2024 não está embarcado no APK; botão ABRIR LIVRO usa arquivo do Drive. Não afirmar offline completo do PDF.


Matriz de lacunas por área no relatório principal: núcleo d20, combate/iniciativa/condições, magias, classes/subclasses/2024/homebrew, espécies/origens/talentos, criação e progressão, inventário, monstros/encontros, Eberron/Faerûn, Strixhaven, Xanathar/Tasha, viagem/tempo/clima, mundo vivo/NPCs, Gemini-first, companhia, TTS e imagens/mapa.
Classificar cada funcionalidade em: PASS REAL, PARCIAL, MANUAL, FALHA, NÃO TESTADO, PENDENTE DE ESCOPO. Não converter presença de método em PASS.


### 7. O que precisa ser FEITO em seguida — ordem prioritária


**PRIORIDADE P0 — concluir auditoria pedida repetidamente.**
1. Inventariar os materiais atualizados do Drive e da fonte (edição/versão, PDF id/hash, conjunto incluído e excluído, mecanismo esperado). O número histórico de 177 PDFs NÃO é um número atualizado confirmado.
2. Para cada livro no escopo, extrair índice mecânico seção a seção: regra e exceções, requisitos, escolhas, rolagens, efeitos, custo, duração, consequências, páginas e fontes.
3. Cruzar com métodos/runtime Java/Python, interface real e persistência SQLite. Produzir matriz rastreável LIVRO → PÁGINA → MECÂNICA → CLASSE/MÉTODO → TELA/CONTROLE → TESTE → STATUS.
4. Executar testes por mecânica com ao menos: validade de pré-requisitos, rolagens/saves, resultado, gasto de slots/recursos, reações e turno, estado de combate, persistência após reinício, deduplicação de transações, save/restore e migração.
5. Incluir dispositivo real ou Android instrumentado: criação e evolução de personagem, combate longo, IA online/erro/retry/offline escolhido, curadoria em grupo, mundo vivo e NPCs 100+ turnos, TTS em múltiplas cenas, tokens Gemini/export/import, UI Galaxy A57 e S23 Ultra. Reportar evidências por cenário; se não houver aparelho/acesso, marcar NÃO TESTADO, NUNCA PASS.
6. Publicar relatório com totais por classe (pass/parcial/manual/falha/não testado/pendente). Somente falar em 100% se nenhuma linha estiver pendente e todas as fontes elegíveis tiverem testabilidade comprovada.


**PRIORIDADE P1 — corrigir lacunas sem recomeçar projeto.**
A. Substituir formulários Eberron/Faerûn e combate com JSON/IDs por UI guiada com seletores, descrições e confirmações cabíveis; delegar resolução numérica aos motores determinísticos.
B. Eliminar dependência de RAG/manual na execução das regras já licenciadas/incorporadas e implementar resolutores ausentes. RAG, se existir, apenas como consulta opcional/educativa; falha sem PDF não pode impedir mecânica compilada.
C. Integrar automaticamente ficha, combate, consumos, efeitos, turnos, persistência, mundo vivo e curadoria; Mestre Gemini narra e decide intenções, não inventa transações válidas nem concede recursos sem validação.
D. Melhorar testes Android instrumentados e criar gates de release que falham quando uma implementação é alegada sem evidência.
E. Ampliar catálogo de curiosidades para outros livros SOMENTE após validar referências exatas/edição. Deixar o ícone compacto e acessível e a entrada estável por cena.


**P2 — distribuição e backups confiáveis.**
- Criar fonte integral RUN190 com hashes nos dois repositórios de backup.
- Após correções, utilizar RUN191 ou nova versão incremental (jamais rebatizar APK antigo), compilar no self-hosted BRELGA, verificar assinatura compatível, package/versionCode, integridade do download, relatar testes com honestidade.
- Gerar checkpoints ANTES de esgotar cotas. Nunca entregar apenas fragmentos; links permanentes para arquivos, manifestos, APK e fonte completa.


### 8. Testes mínimos por cadeia funcional


**Personagem:** criar manual e aleatório, espécies/classes/subclasses/talentos, proficiências, magias, itens, ouro; salvar, fechar app, abrir, subir nível, reler ficha; validar inconsistências e itens faltantes.
**Combate:** iniciativa e ordem real, ações/ações bônus/movimento/reações, vantagem, condições, testes resistidos, concentrações, descanso, derrota/morte, iniciativa de companions nos três modos; turno não avança antes da confirmação.
**Magias:** conjurar com requisito e componentes corretos, gastar slot, rolar ataque/save, efeito real, concentração e término; falha não gasta recurso duas vezes.
**Mundo ativo:** hora, clima, deslocamento, viagens, encontros, consequências de NPCs ausentes, agências e quests persistem através de 100+ turnos e reinício.
**Gemini:** sucesso, rede indisponível, resposta parcial, timeout, cancelar/retry, offline apenas por decisão e sem duplicar narrativa/comitê.
**Curadoria:** proposta baseada em informação do personagem, aprovar/editar/recusar, repetição de clique, proposta obsoleta, economia de slots, curador ausente; nunca manipular personagem sob controle do usuário.
**Curiosidade:** somente uma dica por cena; fonte e página exatas; não alterar a aventura; reinício preserva dica; abrir Drive com mensagem de erro útil quando sem internet.
**TTS:** engines instaladas e vozes PT-BR, continuidade multi-blocos, cena a cena, não travar UI; não embutir modelos do TTS Android externo.


### 9. Instrução de execução para o novo agente


Você está assumindo um projeto Android grande após muitas falsas declarações de conclusão. Comece verificando a release RUN190 e baixando/reconstituindo a fonte integral. Leia a auditoria extensiva e seu CSV; não reescreva o VTT e não suponha que o relatório é auditoria livro a livro. Primeiro crie checkpoint RUN190 redundante, depois expanda a auditoria funcional usando subagentes/linhas de trabalho paralelas conforme disponibilidade (auditoria editorial/implementação de regras; interface e integração; persistência/combate/mundo; CI e testes Android). Trabalhe em blocos grandes, coordenados e verificáveis. Não pause entre subsistemas pedindo aprovação se a tarefa estiver definida.


Faça correções verdadeiras, teste automaticamente, documente falhas e só então gere APK incremental compatível no BRELGA. Regras já compiladas devem funcionar sem importar PDF durante o jogo. NÃO mascarar dependência de manual com Gemini; não exigir dados técnicos ao jogador para operações comuns. Narração humana, autonomia e curiosidade são incrementos importantes, MAS não substituem correção mecânica.


**É EXPRESSAMENTE PROIBIDO:** (a) declarar 100% por compilação; (b) inventar percentuais ou páginas; (c) afirmar teste em aparelho sem execução; (d) descartar builds/regressões; (e) apagar arquivo antes de backup em dois locais; (f) entregar versão anterior como se fosse a nova; (g) trocar automaticamente Gemini por offline; (h) tirar decisão do personagem inteiramente controlado pelo usuário.


### 10. Critérios de término


Entregar: (1) matriz por livro/página/regra/execução, (2) evidências de todos os testes e status real, (3) fonte Android Studio integral e SHA-256, (4) APK incremental assinado e hash, (5) changelog e logs, (6) backups confirmados no Drive e na Biblioteca, (7) relato de limitações não resolvidas, sem alegações indevidas. Se a revisão completa não estiver concluída, continuar indicando explicitamente NÃO CERTIFICADO e priorizar correções das funções que bloqueiam o jogo.


**ESTE HANDOFF SUBSTITUI AS ALEGAÇÕES ANTERIORES DE 100% E APONTA COMO BASE A RUN190 PUBLICADA EM 08/10/2026.**