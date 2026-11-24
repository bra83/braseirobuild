# ECO — auditoria mestre de requisitos e código

Data: 24/11/2026. Base auditada: commit 3df8f17a250a1fcec6f8b440bfc9b92252bf339e, código 0.11.0 listening beta (commit d7e466b), branch feature/tutor-clinico-android-20261122. Escopo: revisão estática dos arquivos Java/Kotlin/HTML/JS, armazenamento, serviço, modelos e testes; comparação com diretrizes de segurança móvel, proteção de dados, documentação por IA e acompanhamento longitudinal. Não é pentest, auditoria jurídica, ensaio clínico ou teste em aparelho físico. Nenhum dado de paciente foi usado.

## Evidência de CI

Run https://github.com/bra83/braseirobuild/actions/runs/45341708551: 61 linhas PASS de lógica; 6 fixtures reais em português FP32 e U8U8; margens U8U8 entre 0,405 e 0,874; BUILD SUCCESSFUL, APK debug publicado. SHA256 d4f1273c488aeb12702f520c925b195a8d063ce646d605f281454ba0a59887f8. Isso não valida ASR, diarização, Android físico, qualidade clínica ou segurança operacional.

## Achados estáticos

| ID | Severidade | Fonte | Constatação | Aceitação |
|---|---|---|---|---|
| A01 | Crítica | SessionRecorder.java:16,69; WhisperFileEngine.kt | WAV temporário claro em cache privado; intermediários também WAV. | Criptografia por blocos/PCM em RAM, teste de inspeção pós-crash. |
| A02 | Crítica | SecureStore.java:49–99; MainActivity.java:379 | Cofre JSON inteiro, leitura 25 MB, journal 1M caracteres, importação 12MB; risco de backup não restaurável e base gravável que não abre. | DB transacional escalável; limites consistentes; round-trip grande. |
| A03 | Crítica | MlSessionEngine.java:201–236; MainActivity.java:192; index.html ecoAddSegment | Checkpoint e exclusão do WAV dependem do retorno de ASR, não de ACK de persistência do JS; saveDraft falha apenas com aviso. | ACK transacional nativo antes de avançar/excluir; falha injetada. |
| A04 | Alta | TranscriptionService.java; MainActivity.java:191; MlSessionEngine.java:325 | Serviço foreground mantém prioridade, mas a Activity possui motor e o destrói; recuperação após process death não garantida. | Serviço proprietário do job, fila e checkpoint durável. |
| A05 | Alta | MainActivity.java:135,192,296; SecureStore.java:40–47 | Bridge permite saveDraft genérico durante lock de transcrição, callbacks assíncronos sem guarda completa; Keystore não exige user auth. | Permissão restrita ao job, chaves adequadas, epoch e isolamento. |
| A06 | Alta | index.html ecoSegmentReview, ecoTimelineRows, ecoNeuralStart | Revisão 250, timeline 600, neural 1200/500k; salto pode não encontrar segmento. | Paginação/busca por ID e análise indexada. |
| A07 | Alta | SegmentDiarizer.java; SpeakerClusters.java | Classificação por segmento, duração 2.5–15s, grupos máximo 2; sem separação sobreposta nem DER medido. | Diarização real, sobreposição/indeterminado, benchmark. |
| A08 | Alta | index.html ecoSourceUnits, ecoWordRows | Revisão global perde timestamps e falante; nuvem prioriza bruto mesmo se texto revisado. | Editor segmentado e fonte explícita. |
| A09 | Alta | index.html normalizeImported, saveSession | Campos do caso projetados por whitelist; novos campos podem desaparecer em importação; comparador de retificação lista campos fixos. | Migração/schema e testes de extensão/retificação. |
| A10 | Alta | NeuralSemanticEngine.java:117–131; index.html ecoNeuralRows | Modelo apenas sessão atual, retorna vizinhos por ranking sem limiar clínico validado; não é RAG longitudinal/NLI. | Índice local por paciente, hard negatives, abstenção, avaliação. |
| A11 | Alta | MainActivity.java:238–254; index.html ecoNeuralStart | Cancelamento por lock suprime callback, estado pendente pode permanecer bloqueando nova análise. | Cancelamento/timeout explícito, limpeza de estado. |
| A12 | Alta | MainActivity.java:341–353; index.html pdfForCase | Impressão existente via WebView; seleção granular, prévia segura e controle do spool/arquivo não demonstrados. | Documentos por finalidade, reautenticação e teste de vazamento. |
| A13 | Média | index.html suggestions, techniques, caseScreen | Sugestões fixas até 10; abas atuais não expõem Investigar/Técnicas como módulos completos editáveis. | Modelo de perguntas/respostas/intervenções com versionamento. |
| A14 | Alta | qa/test.mjs; qa/provision_semantic.py | 61 PASS de lógica (VM/inspeção), seis fixtures reais Python FP32/U8U8, APK debug; sem prova de WER, DER, DOM real, desempenho A57 ou paridade nativa. | Testes instrumentados, corpus representativo e rotina consecutiva. |
| A15 | Alta | index.html ecoReviewFlags; WhisperFileEngine.kt | Heurística de palavras repetidas não detecta fidelidade/coerência, e áudio já pode ter sido apagado. | Revisão priorizada, confiança calibrada, política opcional de reescuta temporária. |

## Decisão de arquitetura

1. P0: banco transacional cifrado, armazenamento temporário seguro, fila nativa independente da UI, confirmação de persistência e isolamento de pacientes antes de ampliar IA.
2. P0: transcrição incremental com benchmark no A57, meta 5 min pós-sessão (limite 10), rotina 6–8 sessões, DER/WER e tempo de revisão. Não escolher Parakeet/TAGARELA sem teste de licença, exportação e desempenho.
3. P1: fluxo de atendimento, ficha, editor por segmento, revisão e anamnese editável. Modo sem gravação igualmente funcional.
4. P2: investigação, técnicas, referências de sonhos, documentos e acompanhamento clínico opcional. Separar relato, interpretação e sugestão.
5. P3: semântica longitudinal, NLI e geração local fundamentada, com verificação de fontes, abstenção e avaliação.
6. P4/P5: administração, portabilidade, privacidade operacional, testes instrumentados, segurança, entrega e revisão de finalidade/regulação. Sem API Gemini clínica; apenas laboratório fictício isolado.

## Referências externas para os gates

- LGPD: https://planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm
- ANPD guia segurança pequeno porte: https://www.gov.br/anpd/pt-br/centrais-de-conteudo/materiais-educativos-e-publicacoes/guia-orientativo-sobre-seguranca-da-informacao-para-agentes-de-tratamento-de-pequeno-porte
- ANPD comunicação incidente: https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/comunicado-de-incidente-de-seguranca-cis
- OWASP MASVS: https://mas.owasp.org/MASVS/
- Android foreground service timeouts: https://developer.android.com/develop/background-work/services/fgs/timeout
- SAFER Guides (referência técnica internacional, não norma brasileira): https://healthit.gov/clinical-quality-and-safety/safer-guides
- Qualidade de notas ambient AI (2026): https://medinform.jmir.org/2026/1/e86474
- Ensaio randomizado ambient AI: https://pmc.ncbi.nlm.nih.gov/articles/PMC12768499/
- Routine Outcome Monitoring review: https://pubmed.ncbi.nlm.nih.gov/36931228/
- Experiências de pacientes e terapeutas com autorrelato: https://pubmed.ncbi.nlm.nih.gov/37322037/
- Anvisa RDC 657/2022, finalidade de software: https://www.gov.br/anvisa/pt-br/centraisdeconteudo/publicacoes/produtos-para-a-saude/manuais/software-como-dispositivo-medico-perguntas-e-respostas

## Resultado

77 requisitos originais preservados, 77 novos, total 154. Nenhum novo requisito marcado concluído. Não é autorização de prática clínica. Próxima entrega deve atacar A01–A05 antes de considerar o app apto para dados reais. A legislação e escopo profissional devem ser avaliados por profissional competente; classificação regulatória não concluída nesta revisão.
