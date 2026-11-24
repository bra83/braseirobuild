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


# Ampliação após auditoria de requisitos e código — 24/11/2026

## P0 — Integridade, persistência e ciclo de vida

- [ ] ECO-078 — Substituir JSON monolítico por armazenamento transacional escalável e cifrado (SQLite/Room com criptografia avaliada ou equivalente); migração reversível com verificação de contagens e hashes.
- [ ] ECO-079 — Eliminar incompatibilidade de limites: cofre lê até 25 MB, journal aceita 1 milhão de caracteres e importação lê 12 MB; teste exportar/restaurar bases maiores e alertas de capacidade antes de gravar.
- [ ] ECO-080 — Persistir trecho, metadados, jobId, patientId, sessionId e checkpoint em transação nativa idempotente; ACK durável antes de avançar checkpoint ou excluir áudio.
- [ ] ECO-081 — Manter áudio temporário cifrado em blocos autenticados, inclusive intermediários Whisper; se biblioteca exige WAV claro, minimizar exposição, migrar para PCM em memória e auditar cache/crash.
- [ ] ECO-082 — Separar trabalho de ASR da Activity/WebView: serviço proprietário da fila e persistência; reconstrução de UI e retomada após morte do processo sem misturar pacientes.
- [ ] ECO-083 — Fila por sessão: permitir iniciar próxima consulta mesmo se anterior processa, com recursos limitados, prioridade explícita, isolamento e identidade imutável.
- [ ] ECO-084 — Gravação contínua com indicador inequívoco, interrupções por chamadas, microfone ocupado, falta de espaço, bateria baixa, permissões revogadas e recuperação.
- [ ] ECO-085 — Resolver atomicidade entre registro principal e rascunho, reprocessamento, revisão e limpeza; testes de falha injetada em cada fronteira e nenhum sucesso falso.
- [ ] ECO-086 — Rever autenticação Keystore, PIN/biometria, chaves distintas por finalidade, desbloqueio e processamento autorizado em background com ameaça documentada; bridge WebView não recebe autorização de escrita genérica enquanto bloqueada.
- [ ] ECO-087 — Limpar callbacks/cache/estado de UI ao bloquear, cancelar ou mudar de caso; verificar epoch, Activity e jobId na entrega; garantir que análise cancelada não fique pendente indefinidamente.
- [ ] ECO-088 — Manter backups incrementais versionados, manifesto autenticado, confirmação de integridade, restauração de teste, recuperação sem sobrescrever registros silenciosamente.
- [ ] ECO-089 — Preservar extensões e novos campos em importação/migração: normalizador atual permite extensões em sessões, mas projeta campos fixos no caso; validar IDs duplicados, referências órfãs e tipos.

## P0/P1 — Qualidade de escuta e revisão

- [ ] ECO-090 — Remover limites de navegação fixos: revisão 250 trechos, timeline 600, análise 1200; paginação, pesquisa global, cobertura informada e salto para qualquer trecho.
- [ ] ECO-091 — Editor por segmento com bruto imutável, revisão versionada, timestamps/falantes preservados quando alinhamento verificável, diff e desfazer/refazer.
- [ ] ECO-092 — Fila de revisão priorizada por confiança ASR calibrada quando disponível, baixa qualidade, negação, número, nome, troca de falante, lacuna, repetição ou conflito entre motores; não inventar índice de confiança.
- [ ] ECO-093 — Reescuta temporária de trecho antes da exclusão, explicitamente opcional e autorizada; padrão apagar após gravação durável, política de retenção mínima configurada, botão descarte imediato; informar quando fonte já não existe.
- [ ] ECO-094 — Diarização de 2+ vozes, terceira pessoa, fala sobreposta, silêncio, troca rápida e segmentos curtos; guardar apenas rótulos anônimos, não embeddings de voz entre sessões.
- [ ] ECO-095 — Melhorar alinhamento entre diarização e ASR, confirmação em lote com papéis exclusivos quando pertinente, possibilidade de desfazer e revisão dos casos incertos.
- [ ] ECO-096 — Mostrar progresso por etapas reais, minutos processados/restantes, RTF, tempo até próxima consulta, indicador de processamento offline; não prometer prazo estimado sem dados.
- [ ] ECO-097 — Transcrição incremental com reprocessamento seletivo de trechos problemáticos se fonte ainda disponível; comparar resultados sem substituir bruto silenciosamente.
- [ ] ECO-098 — VAD neural com comparação de omissões de fala baixa, pausas e sussurros; preservar silêncio como medida observável sem interpretar automaticamente.
- [ ] ECO-099 — Benchmark representativo com sotaques, idades adultas consentidas/fictícias, ruído, nomes, medicamentos, números, negações, ironia, diálogo espontâneo e fala simultânea; versões e dados de teste documentados.
- [ ] ECO-100 — Testar rotina de 6–8 sessões consecutivas, início de próxima consulta com anterior em fila, aparelho aquecido, sem rede e tela bloqueada; medir latência de captura, pós-processamento e revisão humana.

## P1/P2 — Fluxo do atendimento e continuidade clínica

- [ ] ECO-101 — Painel Hoje: agenda, consulta seguinte, registro em aberto, transcrição pendente, revisão necessária e pendências de acompanhamento sem nome em notificações.
- [ ] ECO-102 — Preparação pré-sessão em uma tela: resumo aprovado, últimas falas relevantes, questões abertas, mudanças relatadas, objetivos do paciente e plano anterior.
- [ ] ECO-103 — Encerramento pós-sessão com fila de confirmação: o que foi relatado, intervenções realizadas, notas do profissional, itens a retomar, revisão de texto e status da documentação.
- [ ] ECO-104 — Estado documental rascunho/em revisão/confirmado/retificado; validação por profissional com data, versão e motivo de alteração; não confundir com assinatura certificada.
- [ ] ECO-105 — Registro inicial, contrato/enquadre, frequência, modalidade, objetivos do paciente, expectativas, limites de contato e alterações acordadas.
- [ ] ECO-106 — Estados do acompanhamento: ativo, pausado, encerrado, transferência, retorno; datas, motivo relatado, resumo de encerramento e plano de continuidade.
- [ ] ECO-107 — Registrar encaminhamentos, contatos profissionais, documentos recebidos, devolutivas e retorno pendente com responsabilidade e prazo, sem recomendar conduta automaticamente.
- [ ] ECO-108 — Registro manual de alergias/medicações e informações relevantes de segurança quando pertinentes; distinguir paciente, terceiro, relato e observação; não substituir avaliação profissional.
- [ ] ECO-109 — Área de segurança clínica manual: preocupações relatadas, avaliação humana, plano de segurança/contatos autorizados, encaminhamento e ações registradas; sem score automático de risco ou promessa de monitoramento.
- [ ] ECO-110 — Casais/famílias e menores como modalidade futura separada, com permissões, consentimento e representação adequados; impedir mistura automática de relatos; uso condicionado ao escopo profissional.
- [ ] ECO-111 — Campo de correções solicitadas pelo paciente, anotações contestadas, exportações e solicitações de acesso; revisão antes de alteração/exclusão.
- [ ] ECO-112 — Agenda com recorrência, faltas, reagendamentos, duração real, intervalo configurável e lembretes discretos, sem dados sensíveis no calendário externo.

## P2 — Escuta psicanalítica e instrumentos opcionais

- [ ] ECO-113 — Mapa de temas emergentes, recorrentes, interrompidos e retomados; distinguir contagens de observação clínica.
- [ ] ECO-114 — Concordância KWIC com palavras exatas e contexto, n-gramas, expressões recorrentes, metáforas e linguagem pessoal; origem e filtros por falante.
- [ ] ECO-115 — Marcadores manuais para lapsos, sonhos, associações, mudanças de assunto, silêncio, afetos relatados, repetição e significantes; IA sugere candidatos, não confirma sentido.
- [ ] ECO-116 — Atlas de pessoas e relações com nomes/apelidos e resolução humana de referências/pronomes; registrar quem disse sobre quem.
- [ ] ECO-117 — Mapa de acontecimentos e versões do relato com temporalidade explícita, hipóteses alternativas e contexto; nunca equiparar divergência a mentira.
- [ ] ECO-118 — Diário de transferência/contratransferência e impasses da escuta, separado de transcrição; anotações privadas com finalidade e acesso definidos.
- [ ] ECO-119 — Registro de enquadre e aliança percebida: feedback voluntário do paciente, mudanças de objetivos, rupturas e reparações relatadas; não inferir por voz.
- [ ] ECO-120 — Monitoramento opcional de resultados autorrelatados e metas definidas pelo paciente, com instrumentos adequados/licenciados e validados em PT-BR, escopo profissional verificado; sem diagnóstico automático ou reprodução indevida de testes restritos.
- [ ] ECO-121 — Perguntas do paciente, frases que deseja retomar, tarefas acordadas apenas quando pertinentes à abordagem; não impor técnicas incompatíveis.
- [ ] ECO-122 — Biblioteca de formulações clínicas por escola, glossário, casos fictícios, indicações/limites, controvérsias e fontes primárias verificáveis; não fundir Freud/Lacan/Jung/Klein/Winnicott/Bion.
- [ ] ECO-123 — Comparar a mesma imagem onírica em diferentes sonhos do mesmo paciente; mostrar referências teóricas lado a lado, associações pessoais e interpretações registradas exclusivamente como hipóteses.

## P3 — IA verificável e governança de modelos

- [ ] ECO-124 — Modelo de dados de afirmação: relato literal, paráfrase, observação, hipótese, sugestão automática, confirmação/contestação; cada uma com autor e origem.
- [ ] ECO-125 — Separar citações de resumo, exigir IDs de fontes resolvíveis, testar cobertura e fidelidade (omissões, inclusões, atribuição, negação, temporalidade, terceiro e citações).
- [ ] ECO-126 — Busca semântica paginada/indexada por paciente com retrieval híbrido explícito e busca literal; resultados calibrados, opção nenhum resultado relevante, hard negatives e avaliação Precision@k/Recall@k.
- [ ] ECO-127 — Testar embeddings nativos Android vs referência Python, tokenizer/acentos/truncamento/dense; testar NLI com casos de negação, mudança de opinião, hipótese e falante diferente.
- [ ] ECO-128 — Não tratar texto da sessão, anexos ou fontes recuperadas como instruções à IA; teste de prompt injection, saída estruturada, verificação de citações e recusa a acessar outro caso.
- [ ] ECO-129 — Atualizar ou invalidar resultados, mapas, resumos, índices e referências quando transcrição é corrigida, apagada ou retificada; rastrear versão de modelo e parâmetros.
- [ ] ECO-130 — Gerar sugestões alternativas e perguntas abertas, mostrar evidências contrárias e incerteza; impedir confirmação automática ou geração de hipótese clínica definitiva.
- [ ] ECO-131 — Separar RAG teórico de memória clínica; fontes com edição/capítulo/página verificável, referências indisponíveis marcadas, respeitar direitos autorais.
- [ ] ECO-132 — Gerenciador de modelos offline: licença, checksum, tamanho, versão, compatibilidade, instalação/remoção/rollback, espaço livre e consumo; download opcional sem dados de paciente e pacote verificável.
- [ ] ECO-133 — Laboratório de avaliação com corpus fictício, critérios definidos antes dos testes, revisão independente, regressão por versão, modelo/fonte de cada resultado; medir também tempo de edição e qualidade do relatório.
- [ ] ECO-134 — Modo local PC opcional para modelos pesados somente após avaliar criptografia, pareamento, isolamento, transporte autenticado e falha offline; nenhuma API clínica gratuita por padrão.

## P0/P4 — Privacidade operacional e segurança ampliada

- [ ] ECO-135 — Inventário de dados e finalidades, minimização, registro de operações de tratamento, matriz de base legal e retenção por tipo, responsáveis e canal de direitos; avaliação especializada.
- [ ] ECO-136 — Definir finalidade pretendida e escopo profissional; avaliar enquadramento regulatório inclusive RDC 657/2022 da Anvisa se funções avançarem para apoio diagnóstico/terapêutico; não presumir habilitação profissional.
- [ ] ECO-137 — Consentimento/recusa granular por áudio, transcrição, finalidade e compartilhamento, registro de revogação quando aplicável; permitir atendimento sem gravação sem penalidade.
- [ ] ECO-138 — Notificações, recentes, screenshots, clipboard, teclado/IME, autofill, impressão, compartilhamento, arquivos temporários e logs: testes de vazamento e política de exportação mínima.
- [ ] ECO-139 — Reautenticar para exportação sensível/backup/alteração de identidade; prévia, finalidade, destinatário, registro de exportação e confirmação; arquivos PDF fora do cofre não têm proteção automática.
- [ ] ECO-140 — Auditoria de acesso, alteração, exportação, exclusão, restore e segurança, sem registrar conteúdo clínico em logs; integridade e limites de retenção.
- [ ] ECO-141 — Plano de incidente: detectar, conter, registrar, recuperar, avaliar risco e comunicar ANPD/titular quando aplicável; simulação de perda/roubo e vazamento.
- [ ] ECO-142 — Modo demonstração separado do armazenamento real; aviso persistente de protótipo até aprovação; reset de dados fictícios sem afetar cofre.
- [ ] ECO-143 — Verificação OWASP MASVS/MASTG, WebView bridge, entradas e anexos, intents, dependências, assinatura release, SBOM, build reprodutível e atualizações sem perda de dados.
- [ ] ECO-144 — Exportação portável estruturada por paciente e período, formato aberto documentado, opção de pseudonimização, proteção e rastreabilidade; respeitar direitos e obrigações de retenção.

## P4/P5 — Documentos, operação e certificação

- [ ] ECO-145 — Modelos de documento distintos: ficha interna, registro de sessão, evolução, resumo de encerramento, encaminhamento, supervisão, relatório autorizado e solicitação de acesso; campos editáveis e propósito explícito.
- [ ] ECO-146 — Separar registros clínicos, anotações de reflexão privada, administração e financeiro em escopos definidos; não incluir notas privadas por padrão em PDF.
- [ ] ECO-147 — Impressão segura: prévia real, seleção mínima, contagem de páginas, índice, quebra de página longa, rodapé confidencial, versão, data, exportação protegida; testar spooler e compartilhamento.
- [ ] ECO-148 — Anexos autorizados: PDFs/imagens e documentos relevantes com hash, origem, classificação, criptografia e tamanho; impedir anexos executáveis e links remotos automáticos.
- [ ] ECO-149 — Interoperabilidade futura: exportação JSON/CSV documentada, estudar FHIR apenas onde útil e sem rede automática; importação com mapeamento e relatório de conflitos.
- [ ] ECO-150 — Central de saúde do sistema: versão, modelos, estado de fila, espaço, integridade do cofre, último backup/restauração, diagnóstico técnico sem conteúdo clínico.
- [ ] ECO-151 — Testes instrumentados Android e UI automatizada em emulador/aparelho; DOM real para painel recolhível e navegação; testes atuais VM não comprovam renderização física.
- [ ] ECO-152 — Teste de escala com meta de 100 pacientes fictícios e 10.000 sessões, pesquisa, abertura, migração, backup/restore, exportação longa; definir orçamento de latência e RAM antes de testar.
- [ ] ECO-153 — Matriz de rastreabilidade ECO-001 em diante: requisito, risco, prioridade, status, commit, teste, resultado, evidência física, dependência e decisão; critérios de aceitação não podem ser reduzidos para passar.
- [ ] ECO-154 — Entrega release assinada, notas de versão, migração, rollback, manual do profissional e procedimento de contingência em papel; auditoria independente quando possível.

Relatório técnico: [ECO_AUDITORIA_MESTRE_2026-11-24.md](ECO_AUDITORIA_MESTRE_2026-11-24.md). Todos os requisitos novos estão abertos; não são funcionalidades implementadas.
