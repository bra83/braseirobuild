# ECO — Manual do Usuário e Guia de Prática Clínica

**Versão:** `0.19.0-arm64-beta` (build 24)  
**Projeto:** ECO — Atlas da Escuta e Estação Clínica Autônoma  
**Data:** 24/11/2026  
**Identificador Android:** `br.com.tutorclinico.offline`  

---

## 1. Visão Geral do ECO

O ECO é uma estação clínica autônoma concebida para apoiar a escuta terapêutica, a investigação psicanalítica e a documentação longitudinal com rigor conceitual e total privacidade.

O aplicativo opera **100% offline**, sem qualquer conexão à internet. Toda a inteligência de reconhecimento de voz (ASR), diarização acústica e indexação semântica opera diretamente no processador do aparelho (arquitetura ARM64).

---

## 2. Painel "Hoje" (Dashboard Inicial)

Ao abrir o aplicativo, o painel inicial apresenta:
- **Badge do Motor Neural**: Exibe a prontidão do modelo `TAGARELA PT-BR INT8` (0.6B Parakeet TDT).
- **Agenda do Dia**: Mostra as consultas futuras do dia atual (horários ultrapassados são automaticamente filtrados para despoluir a visualização).
- **Rascunhos em Aberto**: Notifica caso exista uma sessão gravada aguardando confirmação ou retificação, permitindo retomada com um clique.
- **Atalhos Rápidos**: Botões diretos para `[● Iniciar Sessão]`, `[＋ Novo Caso]`, `[◎ Atlas Clínico]`, `[▦ Agenda]` e `[⚙ Ajustes]`.

---

## 3. Escuta e Reconhecimento de Fala (ASR)

O ECO oferece três modalidades de registro clínico:

1. **Gravação de Áudio com Reconhecimento Local**:
   - Captação de áudio em PCM cifrado com cabeçalho binário `ECO1` e chave efêmera de 256 bits em fluxo contínuo.
   - Reconhecimento com `TAGARELA PT-BR INT8` (Parakeet TDT) ou `Whisper Small/Base`.
   - Processamento em segundo plano desvinculado da tela (`TranscriptionService`), com notificação fixa e checkpoints duráveis.
   - A exclusão do áudio temporário é proibida até que todos os segmentos sejam formalmente persistidos no banco de dados SQLite nativo.
2. **Importação de Arquivo de Áudio**:
   - Suporte a WAV, MP3, M4A, FLAC e OGG.
   - Decodificação eficiente em trechos de 25 segundos via MediaCodec nativo.
3. **Atendimento Manual (Sem Gravação / Sem Microfone)**:
   - Permite conduzir e documentar a sessão exclusivamente por texto e anotações clínicas em tempo real, sem solicitar nem acionar permissão de microfone.
   - Preserva acesso a todos os recursos de marcação cronológica, hipóteses e técnicas.

---

## 4. Revisão de Falantes e Diarização sem Perda de Contexto

- **Atribuição em Lote**: A diarização acústica agrupa as falas em grupos neutros (`Falante 1` e `Falante 2`). É possível definir com um toque: `Falante 1 = Paciente / Falante 2 = Terapeuta`.
- **Correção Individual Permanente**: Falantes podem ser corrigidos individualmente para `paciente`, `terapeuta` ou `desconhecido`. As correções manuais são registradas no banco nativo (`speaker_confirmed = 1`) e protegidas contra sobrescrita por reprocessamentos automáticos.
- **Preservação de Posição de Tela**: Ao retificar um falante na lista, a tela mantém a ancoragem visual (`ecoRenderKeepPosition`), sem saltar para o topo.

---

## 5. Módulos do Prontuário e Atlas Clínico

No prontuário do paciente, a grade modular de 12 atalhos (`.eco-module-grid`) permite navegação ágil para:
1. **Resumo & Ficha Geral**: Dados cadastrais e histórico resumido.
2. **Anamnese Estruturada**: 10 categorias clínicas com preenchimento gradual e salvamento transacional.
3. **Enquadre & Contrato**: Acordo formal com frequência, honorários, recesso e limites terapêuticos.
4. **Eixos de Investigação**: 12 eixos psicanalíticos com apontamentos, prioridades e status de exploração.
5. **Diário de Hipóteses**: Registro de evidências clínicas a favor (+) e divergentes (-).
6. **Catálogo de Técnicas**: 6 escolas teóricas (Freud, Lacan, Jung, Winnicott, Klein, Bion) com campos de intervenção e efeito observado.
7. **Genograma Gráfico**: Grafo vetorial SVG interativo cobrindo 4 gerações familiares e tipos de vínculos relacionais.
8. **Atlas de Sonhos**: Narrativa manifesta, associações livres e elementos oníricos.
9. **Dicionário de Símbolos**: Busca lexical instantânea sem perda de foco do teclado virtual.
10. **Linha do Tempo / Cronologia**: Marcos biográficos do analisando.
11. **Diário Privado do Terapeuta**: Área restrita para anotações de contratransferência, estritamente segregada do prontuário público.
12. **Emissão de Documentos**: Geração de relatórios com checklist seletivo de seções.

---

## 6. Segurança na Emissão de Documentos e Minutas

- **Bloqueio de Impressão Não Confirmada**: O aplicativo proíbe expressamente a impressão ou exportação de documentos sem a confirmação de revisão humana prévia (`MINUTA NÃO CONFIRMADA`). O profissional deve marcar o termo de responsabilidade clínica antes de imprimir.
- **Pseudonimização Rigorosa para Supervisão**:
  - Nome do paciente substituído por `[PACIENTE]`.
  - Nomes de familiares e parentes substituídos por `[FAMILIAR]`.
  - Mascaramento automático de CPF (`[CPF REMOVIDO]`), telefone (`[TELEFONE REMOVIDO]`) e logradouros (`[ENDEREÇO REMOVIDO]`).
  - Faixa etária inferior a 18 anos identificada categoricamente como `Faixa infantojuvenil (Pseudonimizada)`.
- **Proteção Contra Erro de Paciente**: O sistema rejeita pedidos de impressão quando o identificador do caso é inválido ou nulo, sem nunca transferir a impressão para outro paciente cadastrado.
