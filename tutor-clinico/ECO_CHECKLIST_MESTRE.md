# ECO — checklist mestre de desenvolvimento

2026-11-24. Fonte de requisitos permanente. Todos os itens abaixo permanecem abertos até comprovação por evidência; alguns já existem parcialmente. Base: 0.11.0 listening beta, build 45341708551 aprovado no CI; validação física não comprovada. Prioridades P0 (bloqueadores) a P5 (certificação); execução incremental e sem remover requisitos anteriores. Registre estado `pendente`, `implementado`, `CI aprovado`, `validado no aparelho`, `aceito`; não marque checkbox sem aceitação. Não é autorização para uso clínico real.

## P0 — Escuta, transcrição e desempenho

- [ ] ECO-001 — Comparar Whisper Base, Small Q5, Parakeet v3 e TAGARELA PT-BR em fala espontânea fictícia; validar licenças, exportação e execução ARM64.
- [ ] ECO-002 — Implementar transcrição incremental durante gravação autorizada; buffers limitados, fila com backpressure e recuperação de erros.
- [ ] ECO-003 — Meta A57: sessão fictícia de 50–60 min; processamento final completo em até 5 min como alvo, 10 min como limite de aceitação; medir P50/P95 incluindo diarização e análise.
- [ ] ECO-004 — Medir WER, CER, erros de negação, nomes, omissões, inserções, alucinações, RTF, RAM, bateria e temperatura; comparar condições de ruído, sotaques e fala sobreposta.
- [ ] ECO-005 — Escolha explícita de motor rápido/preciso, fallback informado, dicionário de vocabulário local quando suportado.
- [ ] ECO-006 — Gravação, importação de áudio, pausa, retomar, cancelar, progresso real, tempo restante baseado em medição.
- [ ] ECO-007 — Transcrição em segundo plano com notificação, tela bloqueada, troca de app, recuperação segura; nunca gravar escondido.
- [ ] ECO-008 — Diarização real com grupos anônimos, timestamps, sobreposição/indeterminado, atribuição paciente/terapeuta por grupo e correção individual; DER/JER com referência.
- [ ] ECO-009 — Preservar texto bruto, revisão editável com trilha de alterações e alinhamento quando verificável; flag de possíveis repetições sem inventar texto.
- [ ] ECO-010 — Excluir áudio temporário após sucesso ou descarte; definir janela de recuperação e limpeza de crash; sem upload de áudio; preservar arquivo importado original.
- [ ] ECO-011 — Modo sem gravação: ficha, marcação rápida, notas e cronologia manual, mesmos recursos de acompanhamento.
- [ ] ECO-012 — Marcação durante escuta: tempo, categoria, nota, vinculação ao trecho correspondente quando disponível.

## P1 — Cadastro, sessão e anamnese

- [ ] ECO-013 — Cadastro rápido por código; identidade opcional em cofre separado; pseudônimo não é anonimização.
- [ ] ECO-014 — Anamnese editável: identificação, queixa, expectativas, infância, adolescência, família, vínculos, sexualidade pertinente, trabalho, saúde, medicações relatadas, sono, hábitos, perdas, recursos e rede de apoio.
- [ ] ECO-015 — Perguntas personalizadas por profissional, categorias, ordenar, ocultar, criar, editar, excluir; respostas e notas editáveis, não obrigatórias.
- [ ] ECO-016 — Histórico de resposta, data, autor, fonte, versão e retificação; confirmação antes de incorporar sugestão extraída de sessão.
- [ ] ECO-017 — Ficha individual com sessões, resumo profissional, status, objetivos, plano e retorno; sem gerar síntese falsa quando campo vazio.
- [ ] ECO-018 — Diário do terapeuta privado separado do registro assistencial; controle de acesso e exportação independente.
- [ ] ECO-019 — Documentos de autorização, consentimento/recusa e respectivas versões; alternativas sem gravação.
- [ ] ECO-020 — Medicações e saúde como informações relatadas; sem prescrição ou recomendação automática.
- [ ] ECO-021 — Busca, filtros, ordenação, edição de dados, arquivamento, exportação e exclusão com retenção aplicável.

## P1 — Interface e acessibilidade

- [ ] ECO-022 — Identidade ECO, ícone, tema claro/escuro (cinza quase preto, branco, verde neon), fonte legível e controles acessíveis.
- [ ] ECO-023 — Dashboard de sessão: transcrição, nuvem de palavras aberta, falantes, recorrências neurais, frequência, negações, acústica, linha do tempo e investigação, blocos independentes recolhíveis.
- [ ] ECO-024 — Nuvem clicável com contagem e trechos de origem; filtros por falante e sessão, texto revisado versus bruto explicitado.
- [ ] ECO-025 — Não esconder funcionalidades atrás de menus genéricos; acesso direto à anamnese, técnicas, investigar, imprimir, sonhos, genograma e evolução.
- [ ] ECO-026 — Preservar estado de recolhimento, scroll, teclado, foco, rotação, navegação e rascunhos; paginar listas extensas sem truncamento silencioso.
- [ ] ECO-027 — Testar fluxo em tela física Galaxy A57, com sessão longa, acessibilidade e ambos os temas.

## P2 — Investigar e técnicas

- [ ] ECO-028 — Catálogo de investigação: repetição, ambivalência, divergência contextual/temporal, lacunas, associação livre, família, transferência, resistência, defesas, discurso, sonhos, desejos e angústias.
- [ ] ECO-029 — Perguntas contextualizadas com evidência, fonte, minuto e sessão; separar relato, hipótese profissional e sugestão algorítmica.
- [ ] ECO-030 — Cada pergunta permite resposta editável, nota, status a investigar/em curso/explorado/descartado, prioridade, datas, vínculos e histórico.
- [ ] ECO-031 — Criar, editar, excluir e reorganizar perguntas próprias; retomar pendências no próximo atendimento.
- [ ] ECO-032 — Diário de hipóteses: evidências favoráveis/contrárias, explicações alternativas e revisão humana.
- [ ] ECO-033 — Técnicas: associação livre, atenção flutuante, clarificação, pontuação, confronto cuidadoso, interpretação, elaboração, silêncio, sonhos, transferência, resistência, construção narrativa e outras por abordagem.
- [ ] ECO-034 — Biblioteca de Freud, Lacan, Jung (psicologia analítica), Winnicott, Klein e Bion; distinguir escolas, fonte, finalidade, limites, exemplos e contexto.
- [ ] ECO-035 — Registro editável de intervenção, resposta do paciente, efeito observado e acompanhamento; técnicas personalizáveis.
- [ ] ECO-036 — Sem diagnóstico automático, detector de mentira, prescrição, atribuição de emoção ou escolha autônoma de intervenção; protocolo humano de segurança e encaminhamento.

## P2 — Fichas e impressão

- [ ] ECO-037 — Ficha A4 completa e resumida por paciente, exportar PDF e imprimir via Android.
- [ ] ECO-038 — Selecionar identificação, anamnese, sessões, texto bruto/revisado, falantes, pontos de investigação/respostas, hipóteses, técnicas, genograma, sonhos, gráficos e plano.
- [ ] ECO-039 — Separar registro interno, resumo de acompanhamento, documento para terceiros e supervisão pseudonimizada; não incluir texto integral por padrão.
- [ ] ECO-040 — Prévia, filtros de período, paginação, cabeçalho profissional, datas, assinatura, opção de ocultar identificação, sem alegar assinatura digital quando não implementada.
- [ ] ECO-041 — Testar relatório com transcrição longa, caracteres PT-BR, tabelas, quebras de página, fontes, vazamento em spool/cache e cancelamento.

## P3 — Sonhos, genograma e cronologia

- [ ] ECO-042 — Atlas de sonhos: conteúdo manifesto, data, personagens, lugares, afetos relatados, associações, observações, recorrência e vínculo com sessão.
- [ ] ECO-043 — Dicionário de símbolos pesquisável: animais, casas, cômodos, parentes, morte, nascimento, viagem, queda, perseguição, espelhos, dentes, números, cores, objetos e natureza.
- [ ] ECO-044 — Cada verbete: corrente, autor, obra, edição, capítulo/página verificáveis, paráfrase autorizada, sentidos possíveis, perguntas abertas, limites e associações pessoais.
- [ ] ECO-045 — Fontes iniciais Freud A interpretação dos sonhos e Conferências introdutórias X; Jung textos sobre sonhos, Obras completas vols. 8 e 16; referências conferidas e teorias diferenciadas.
- [ ] ECO-046 — Não afirmar equivalências universais; identificar divergências entre autores, não inferir diagnóstico, revisar bibliografia e direitos de uso; importação de referências pelo usuário.
- [ ] ECO-047 — Genograma visual editável, parentes, vínculos relatados, eventos, notas, histórico, fontes; mapa de rede social.
- [ ] ECO-048 — Linha do tempo biográfica com datas aproximadas/incertas, eventos, pessoas, fontes e correções; não inventar datas.
- [ ] ECO-049 — Gráficos longitudinais de contagens, temas, sonhos e hipóteses; frequência não equivale a gravidade ou melhora clínica.

## P3 — Inteligência neural local

- [ ] ECO-050 — Preservar embeddings DistilUSE U8U8 real; testes de paridade, qualidade em português e execução no A57.
- [ ] ECO-051 — Indexação semântica longitudinal por paciente, busca por significado, filtros, referências e exclusão de índices quando registro removido.
- [ ] ECO-052 — Mapa semântico interativo e comparação de sessões, agrupamentos explicáveis por trechos.
- [ ] ECO-053 — NLI multilíngue validado em PT-BR para compatibilidade/possível divergência; tratar sujeito, tempo, negação, fala hipotética, citações e correções; nunca rotular mentira.
- [ ] ECO-054 — Modelo generativo local com licença compatível, benchmark ARM64/RAM, quantização e saída estruturada, recuperação RAG com fontes verificadas.
- [ ] ECO-055 — Resumo pós-sessão, perguntas de investigação, preenchimento assistido e preparação pré-sessão; toda sugestão vinculada a citações verificáveis, abstenção sem evidência e aprovação humana.
- [ ] ECO-056 — Modelo offline com falhas explícitas; nada de fallback lexical disfarçado de neural; versão/modelo/métricas auditáveis.
- [ ] ECO-057 — Laboratório de IA com amostras exclusivamente fictícias; comparar motores, exportar métricas sem dados clínicos.

## P4 — Consultório e biblioteca

- [ ] ECO-058 — Agenda, retornos, alertas discretos, pendências, controle financeiro e honorários com acesso separado.
- [ ] ECO-059 — Biblioteca científica offline com catálogo de obras, referências, pesquisa e anotações, importação legítima e atualizações.
- [ ] ECO-060 — Modo supervisão com seleção mínima, revisão de risco de reidentificação e exportação protegida.
- [ ] ECO-061 — Modelos personalizados de questionários, fichas, relatórios e técnicas.
- [ ] ECO-062 — Android/desktop: estudar sincronização e recuperação sem comprometer segregação de dados; backup cifrado opcional em Drive, sem áudio.

## P0/P4 — Segurança, privacidade e integrações

- [ ] ECO-063 — Auditar finalidade e base legal LGPD, sigilo profissional, regras aplicáveis ao exercício profissional, retenção, direitos e transferência de dados; revisão especializada antes de uso real.
- [ ] ECO-064 — Criptografia local autenticada, Android Keystore, PIN/biometria, bloqueio, captura de tela, logs sem conteúdo, backups protegidos, testes de integridade.
- [ ] ECO-065 — Separar cofre de identidade e histórico; códigos aleatórios; nunca considerar código binário anonimização.
- [ ] ECO-066 — Sem rede no ambiente clínico por padrão; isolamento por paciente em armazenamento, índices, prompts, relatórios e cache.
- [ ] ECO-067 — Backup exportável criptografado, restauração testada, migrações, corrupção, perda de dispositivo, exclusão e revogação quando aplicável.
- [ ] ECO-068 — Gemini Developer API: somente laboratório didático independente e dados integralmente fictícios; termos de 23/03/2026 vedam prática clínica; serviços gratuitos não aceitam dados sensíveis/confidenciais/pessoais.
- [ ] ECO-069 — Bloquear acesso do laboratório a cofre, arquivos clínicos, áudio, transcrição, prontuário, índices e backups; não permitir envio automático.
- [ ] ECO-070 — Se avaliar provedor externo diferente no futuro: autorização contratual de finalidade, proteção/retensão, subprocessadores, transferência internacional, avaliação de risco e aprovação antes de integração.
- [ ] ECO-071 — Não distribuir chave API no APK; limites de custo, cotas e revogação em laboratório isolado.

## P5 — Certificação e entrega

- [ ] ECO-072 — Cada requisito tem ID estável, estado, versão, commit, teste automatizado, evidência e validação física quando pertinente; não marcar concluído só por código ou build.
- [ ] ECO-073 — CI: testes de regressão, inferência real, compilação, APK, análise de dependências/licenças, testes de migração e backup.
- [ ] ECO-074 — Teste de fluxo completo fictício: cadastro, consentimento, 50–60 min de áudio, diarização, edição, pesquisa, investigação, técnicas, sonho, relatório, impressão, backup/restore.
- [ ] ECO-075 — Testar troca de app, bloqueio de tela, baixa RAM, bateria, armazenamento cheio, cancelamento, reinício do processo, duas sessões seguidas e isolamento entre pacientes.
- [ ] ECO-076 — Benchmark clínico simulado: nenhum texto fabricado em síntese, fontes válidas, ausência de vazamento; limites documentados e falhas visíveis.
- [ ] ECO-077 — Auditoria final profissional e de privacidade; uso real somente após requisitos críticos aprovados e avaliação aplicável.

## Fontes iniciais verificáveis

- Gemini API Additional Terms of Service (vigência 23/03/2026): https://ai.google.dev/gemini-api/terms
- LGPD: https://planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm
- Freud, The Dream-Work: https://www.freudedition.net/en/werke/vi-dream-work/druckschrift-0
- Freud, Introductory lectures: https://www.gutenberg.org/cache/epub/38219/pg38219-images.html
- Jung, revisão acadêmica histórica: https://pmc.ncbi.nlm.nih.gov/articles/PMC4217604/

## Regra de manutenção

Atualizar este arquivo a cada entrega, preservando IDs. Acrescentar requisitos com novos IDs; não apagar os anteriores. Evidências: link de commit, execução CI, teste físico e relatório de aceitação. Nenhuma funcionalidade crítica será declarada concluída por mera existência de interface.
