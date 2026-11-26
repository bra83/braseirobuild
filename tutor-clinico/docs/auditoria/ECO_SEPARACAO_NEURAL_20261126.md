# ECO — separação neural real de vozes simultâneas — 26/11/2026

## Implementação publicada
Commits da08b655bb6dc0ddcb5ee72db5779f1fcea36fba, d7d1686e5f73db5ab09ab2ae49b45a3a8a2d48a5, 89b1ce4df21edd3fd9c008a4507bc1a1a32a0632.

- Conv-TasNet Libri2Mix sepnoisy 16 kHz FP32, ONNX, aproximadamente 20,2 MB. Modelo fixado por SHA-256 3fa85c7bb2537ced4bb7acb2ea91255c7d6b222fd3d0fae5088e8a3999a963fd; entrada mono PCM 16 kHz, saída duas ondas independentes. O modelo distribuído permanece inalterado, com atribuição e licença CC BY-SA 4.0.
- Pyannote detecta trechos simultâneos; janelas locais com margem de 350 ms, agrupadas sem sobreposição e divididas em até cinco segundos. Não confundir classes do detector com identidade individual.
- Separação em memória, sem gravar arquivos intermediários das trilhas; saída inválida/silenciosa não é amplificada indiscriminadamente. Vosk PT-BR transcreve cada trilha independentemente, inclusive quando o motor primário é TAGARELA, Whisper Base/Small ou Vosk. Se não houver duas transcrições distintas não apresenta o par como hipótese. O motor é usado quando a análise de falantes está habilitada.
- Persistência dos dois textos em IDs distintos no mesmo intervalo; cada candidato integra a contagem de persistência e recuperação, sem substituir mistura original ou texto clínico. Rótulos A/B são locais a cada janela, não identificam paciente/terapeuta; atribuição automática proibida. Revisão mostra pares separados, excluídos do texto original e das análises clínicas automáticas.
- Gate de CI baixa exatamente o modelo, confere SHA e licença, executa inferência ONNX em duas misturas sintéticas PT-BR com fontes limpas conhecidas e exige melhoria SI-SDR positiva nas duas fontes, média de pelo menos 2 dB por mistura. Vosk precisa produzir ao menos um par de transcrições não vazias e distintas. Verificação do APK exige modelo, hash, licença e métricas. Teste Robolectric cobre colisão de timestamps, repetição idempotente, rejeição de reatribuição, isolamento, ACK, recuperação e backup com nova chave.

## Evidência e limites
CI 45748098104 em execução na consulta; lógica e Chromium aprovados. Inferência, teste nativo novo e APK ainda não certificados no momento desta anotação. Separação e transcrição implementadas; não declarar qualidade validada sem logs. Modelo treinado em inglês, teste sintético não equivale a conversa espontânea brasileira. Não há identificação persistente entre janelas, nem separação de três ou mais fontes. A/B pode permutar, vazamento residual e erros ASR são possíveis. A revisão humana deve confrontar os trechos. Nenhuma gravação clínica real usada; teste físico A57 adiado pelo titular.

Fontes: https://huggingface.co/iamdgarcia/ConvTasNet_Libri2Mix_sepnoisy_16k-onnx ; https://huggingface.co/JorisCos/ConvTasNet_Libri2Mix_sepnoisy_16k ; https://arxiv.org/abs/1809.07454 ; https://creativecommons.org/licenses/by-sa/4.0/.
