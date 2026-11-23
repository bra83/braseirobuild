# ECO — Android 0.2 ML offline
Reconhecimento acústico real: Vosk 0.3, modelo pt-BR small, Apache-2.0 (baixado no GitHub Actions).
- O aplicativo captura somente áudio PCM volátil em memória via SpeechService; não salva .wav/.m4a, nem envia áudios ao Drive.
- Transcrição de baixa a moderada precisão; sem diarização, sem análise semântica por LLM.
- Marcação de elementos clínicos (sonhos, familiares, afetos) é manual e visível.
- Recuperação entre sessões usa TF-IDF + similaridade cosseno (Machine Learning estatístico não neural); sempre mostra trechos de origem.
- Marcações e transcrições persistem no cofre local AES-GCM. Backup exportável e criptografado via seletor de documentos Android; pode escolher Drive manualmente.
- A captura exige autorização confirmada; apenas testes com dados fictícios nesta fase.
- Testar na tela ativa; ao sair do app a captura é encerrada.
- Este APK ainda exige auditoria de segurança, validação de precisão e jurídica antes de uso clínico real.
