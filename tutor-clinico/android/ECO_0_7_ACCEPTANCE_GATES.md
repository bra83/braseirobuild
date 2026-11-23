# ECO 0.9.0-beta — gates de aceite e limitações

Estado: beta de desenvolvimento com microfone Whisper experimental e comparação WER local. A compilação e testes de lógica não certificam uso clínico.
Não use dados reais de pacientes antes de validações independentes de segurança e base legal.

| Gate | O que foi implementado neste ciclo | Evidência que ainda falta |
|---|---|---|
| G1 Integridade | Backups preservam campos ML/revisões; rascunho cifrado independente; falhas de escrita não descartam sessão; teste de falha injetada | Restauração real em instalação limpa e teste de atualização assinada |
| G2 Qualidade ASR | Whisper Small q5_1 selecionável e Base disponível; cortes adaptativos por pausa, filtragem acústica de silêncio; timestamps por segmento; teste WER/CER local em qa/benchmark_asr.mjs | Medição WER/CER em áudio humano pt-BR e em Galaxy A57; ausência de alucinações não demonstrada |
| G3 Robustez Android | Áudio importado em pedaços; resultado progressivo; cancelamento; rascunho incremental | Testes físicos com 1/10/30/60 min, queda de app, perda de contexto e bateria |
| G4 NLP | Extrator factual auditável: pessoas, referências temporais, contextos, sonhos e possíveis negações; exige confirmação humana | Não há NLP neural clínico, embeddings ou NER treinado. Validar precisão/recall e falsos positivos |
| G5 Evidência | Trechos Whisper trazem startMs/endMs; etiquetas de falante editáveis pelo usuário; citações textuais no Atlas | Diarização automática confiável e calibração de confiança ainda ausentes |
| G6 Segurança | Keystore AES-GCM para cofre e autosave separado; exportação cifrada; sem permissão INTERNET; limpeza de trechos temporários | Pentest, exclusão segura verificada e auditoria Android física; WAV temporário fica em cache privado sem criptografia própria |
| G7 UX | Importação e captação independentes; acesso sem sintomas; temas e revisão; seleção do motor; status de processamento | Testes manuais completos em Galaxy A57 e acessibilidade |
| G8 Distribuição | GitHub Actions compila APK de depuração | Assinatura estável de release, migração e update sem desinstalação; não há secrets de release validados |
| G9 LGPD | Declaração estruturada de teste ou autorização com referência, data e finalidade operacional | Revisão jurídica, termo adequado, bases legais, retenção, acesso e procedimentos de incidentes |

## Arquitetura atual

- Android Java WebView: interface e bridge.
- WhisperFileEngine Kotlin: transcrição offline por arquivo, trechos de 25 s com pausa conservadora, modelos Base e Small q5_1.
- MlSessionEngine Java + WhisperLiveEngine Kotlin: mic agora oferece Whisper Base experimental, Whisper Small Q5 experimental e fallback Vosk. A fila tem capacidade limitada; excesso deve interromper captura com aviso. Ainda não há validação física da velocidade em tempo real.
- NLP factual: implementação determinística no JS, jamais diagnóstico ou interpretação psicodinâmica automática.
- TF-IDF: comparação lexical entre textos de sessões da mesma pessoa.
- Persistência: vault.aes e draft.aes cifrados AES-GCM; formato de dados ainda JSON, **não** banco SQL transacional por entidade.
- Backup .psicbackup cifrado; sem sincronização automática no Google Drive.
- Sem atribuição automática confiável terapeuta/paciente, somente rótulos revisados manualmente.
- Sem semântica neural/embeddings, sem detecção robusta de contradições e sem comprovação de precisão clínica.

## Ciclo de validação proposto

1. Fazer backup cifrado dos testes anteriores, atualizar o app sem reinstalar e verificar dados em dois aparelhos diferentes.
2. Importar mesmo MP3 via Small e Base; salvar transcrições e avaliar com `node qa/benchmark_asr.mjs referencia.txt gerado.txt`.
3. Medir erros críticos, inclusive negações ("não tenho"), papéis de falantes e nomes de pessoas.
4. Testar trechos de silêncio, transições e longos períodos; contabilizar trechos não reconhecidos.
5. Registrar áudio fictício ao vivo; avaliar o novo microfone Whisper em tempo real e conferir explicitamente a fila limitada, o atraso, e o risco de deixar áudio não transcrito sob carga no Galaxy A57.
6. Validar todo fluxo de importação/correção/marcação/fonte/Atlas/PDF/backup/restauro com falhas injetadas e sem perda.
7. Revisar LGPD, segurança e assinatura release antes de dados reais.

**Regra de release:** nunca chamar de "final", "100%" ou "adequado a pacientes" com gates pendentes. Registro técnico deve distinguir saída automática, revisão e nota profissional.

## Gates novos 0.8

- Microfone Whisper Base/Small experimental captura WAVs curtos em cache privado, reconhecimento por thread e fila limitada com erro de sobrecarga. Foi integrado e compilado; funcionamento em Galaxy não validado.
- A transcrição ao vivo pode atrasar; se não acompanhar a velocidade da fala, a gravação deve parar explicitamente em vez de consumir memória sem limite.
- O Atlas só considera citações confirmadas ainda presentes na transcrição vigente; marcações desatualizadas aparecem em separado.
- A revisão oferece cálculo WER local usando transcrição de referência humana de gravação fictícia, sem persistir o texto de referência.
- Ainda não existe diarização automática nem banco SQLite criptografado; nenhuma destas funcionalidades deve ser presumida pronta.

## 0.9.0-beta: mudança de arquitetura e critérios de aceite

- Captura agora grava primeiro e transcreve somente após finalizar e fechar o WAV. O AudioRecord pertence apenas à thread de captura: o botão Parar não chama stop/release diretamente.
- WAV temporário do microfone fica no cache privado, sem criptografia própria, enquanto a transcrição é processada ou está pausada. Após sucesso, descarte explícito ou inicialização posterior por limpeza, é removido. Em caso de erro, pode ser retido até decisão de retomar ou descartar. Por privacidade, ainda apenas simulações.
- Importação e gravação podem ser pausadas após o trecho Whisper corrente, retomando por deslocamento PCM com progresso confirmado; as palavras reconhecidas anteriormente ficam no rascunho criptografado.
- Encerramento parcial preserva texto e descarta somente gravação temporária criada pelo ECO, nunca arquivos importados.
- Linha do tempo textual mostra minuto:segundo por segmento, pesquisa e navegação por trecho; sem áudio guardado, não há reprodução posterior.
- NLP factual v2 sugere ações, referências temporais, possível negação, pessoas e contexto. Não há modelo neural clínico nem inferência de diagnóstico.
- GATES AINDA ABERTOS: teste físico Android de parar/pausar/retomar, qualidade PT-BR WER, retenção segura, acessibilidade e proteção para uso real.
