# ECO 0.7.0-beta — gates de aceite e limitações

Estado: beta de desenvolvimento. A compilação e testes de lógica não certificam uso clínico.
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
