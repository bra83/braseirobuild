# ECO — Evidências de Interface do Usuário (UI)

Este diretório contém o registro detalhado e as evidências estruturadas de funcionamento das telas e módulos clínicos implementados no ECO `0.15.0-arm64-beta` (build 20).

---

## 1. Módulos e Telas Auditadas

### 1.1 Painel "Hoje" / Dashboard
- **Evidência**: Header com badge ativo `[Motor Neural Ativo: TAGARELA PT-BR INT8]`.
- **Conteúdo**: Próxima consulta com tempo e horário, fila de rascunhos em aberto aguardando confirmação, atalhos rápidos para Sessão, Novo Caso, Atlas e Agenda.
- **Teste associado**: `qa/test.mjs` ("ECO 0.15 Painel Hoje exibe badge ativo do TAGARELA e agenda").

### 1.2 Atlas de Sonhos & Dicionário de Símbolos Oníricos
- **Evidência**: Subaba `sonhos` no Atlas Clínico (`ecoAtlasTab === "sonhos"`).
- **Conteúdo**: Lista de sonhos documentados do caso com narrativa manifesta, personagens, afetos sentidos, associações do paciente e hipóteses profissionais; Dicionário teórico com 12 verbetes completos (animais, casas, parentes, morte, viagem, queda, perseguição, espelhos, dentes, números, cores, natureza) contendo citações acadêmicas de Freud e Jung e barra de busca em tempo real (`data-dream-symbol-search`).
- **Teste associado**: `qa/test.mjs` ("ECO 0.15 Atlas de Sonhos e Dicionário com Freud e Jung verificáveis").

### 1.3 Eixos de Investigação Psicanalítica & Diário de Hipóteses
- **Evidência**: Subaba `investigar` no Atlas Clínico (`ecoAtlasTab === "investigar"`).
- **Conteúdo**: 12 eixos psicanalíticos com perguntas orientadoras, campos editáveis para anotações do terapeuta, status (`a investigar`, `em curso`, `explorado`, `descartado`) e prioridade; Diário de Hipóteses com balanceamento explícito de evidências favoráveis (+) e contrárias (-).
- **Teste associado**: `qa/test.mjs` ("ECO 0.15 12 Eixos de Investigação Psicanalítica e Diário de Hipóteses").

### 1.4 Biblioteca de Técnicas Clínicas por Escola
- **Evidência**: Subaba `tecnicas` no Atlas Clínico (`ecoAtlasTab === "tecnicas"`).
- **Conteúdo**: Técnicas divididas com rigor por escola: Freud (Associação Livre, Atenção Flutuante, Clarificação, Interpretação, Elaboração), Lacan (Pontuação/Escansão, Corte, Retificação Subjetiva, Significante-Mestre), Jung (Imaginação Ativa, Amplificação, Sombra), Winnicott (Holding, Espaço Transicional, Uso do Objeto), Klein & Bion (Transferência no Aqui-e-Agora, Função Alfa, Continência); Registro de intervenções clínicas com contexto, resposta verbal/não verbal e efeito observado.
- **Teste associado**: `qa/test.mjs` ("ECO 0.15 Biblioteca de Técnicas por Escola com Freud, Lacan, Jung, Winnicott, Klein e Bion").

### 1.5 Genograma Familiar & Linha do Tempo Biográfica
- **Evidência**: Subaba `genograma` no Atlas Clínico (`ecoAtlasTab === "genograma"`).
- **Conteúdo**: Mapeamento visual transgeracional em 4 gerações com tipologia de vínculos (próximo, simbiótico, conflituoso, distante, rompido, dependente, cuidador); Linha do tempo com marcos biográficos em ordem cronológica com datas aproximadas e fontes.
- **Teste associado**: `qa/test.mjs` ("ECO 0.15 Genograma familiar interativo e Linha do Tempo Biográfica").

### 1.6 Diário Privado do Terapeuta
- **Evidência**: Subaba `diario` no Atlas Clínico (`ecoAtlasTab === "diario"`).
- **Conteúdo**: Espaço para anotações confidenciais sobre contratransferência, impasses na escuta e supervisão clínica, estritamente segregado do prontuário e omitido de relatórios externos.
- **Teste associado**: `qa/test.mjs` ("ECO 0.15 Diário Privado do Terapeuta segregado de relatórios externos").

### 1.7 Área de Segurança Clínica Manual
- **Evidência**: Subaba `seguranca` no Atlas Clínico (`ecoAtlasTab === "seguranca"`).
- **Conteúdo**: Protocolo de segurança clínica conduzido exclusivamente por julgamento humano, com registro de preocupações, avaliação clínica, plano de segurança acordado e contatos de emergência autorizados, sem escores automáticos de risco.
- **Teste associado**: `qa/test.mjs` ("ECO 0.15 Área de Segurança Clínica Manual sem escore automático de risco").

### 1.8 Emissão e Impressão de Documentos A4
- **Evidência**: Subaba `documentos` no Atlas Clínico (`ecoAtlasTab === "documentos"`).
- **Conteúdo**: Seleção de modelo (Ficha Completa, Resumo Multidisciplinar, Encaminhamento Clínico, Síntese para Supervisão), seleção granular de seções, prévia A4 formatada com cabeçalho profissional e rodapé de sigilo confidencial (LGPD), e botão de impressão via Android Print Manager.
- **Teste associado**: `qa/test.mjs` ("ECO 0.15 Emissão de Documentos Clínicos Formatados A4 com cabeçalho e salvaguardas").
