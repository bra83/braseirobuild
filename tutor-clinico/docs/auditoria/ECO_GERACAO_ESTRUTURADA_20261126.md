# ECO — geração clínica estruturada e Qwen avançado — 26/11/2026

## Diagnóstico confirmado
O CI 45752320518 aprovou o APK, mas o modelo de 0,5B obteve 0/3 minutas neurais aceitas; os 3 roteiros editoriais passaram. Não confundir aprovação do aplicativo com competência do modelo.

## Implementação
- 7b320e1: gramática GBNF no runtime llama.cpp b5046, limitando a saída a três campos curtos gerados (hipótese, justificativa e exemplo de pergunta). A aplicação monta conceito de fonte recuperada e cautelas editoriais separadamente, sem afirmar que o modelo os escreveu. Citações indicam contexto, não sustentação comprovada. Rejeição de campos incompletos, certeza indevida, instrução reproduzida, pergunta não aberta e ausência de ancoragem lexical mínima.
- Modelo leve Qwen2.5 0,5B continua embutido. Qwen2.5 1,5B Q4_K_M oficial (aprox. 1,12 GB, Apache-2.0, revisão dd26da4) disponibilizado separadamente quando aprovado pelo CI. SHA-256 6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e. Motivo: APK anterior já ocupava 1,71 GB compactado; não inflar ainda mais. Importação via Ajustes, SAF e PIN, sem rede no ECO. Arquivo privado, cópia temporária limitada, hash, rename atômico; falhas preservam modelo instalado. Quando disponível, o avançado é selecionado automaticamente.
- 5ec50b8: inferência real dos dois modelos nas três mesmas situações fictícias; relatório de aceitação separado por modelo e duração. Publicação bloqueada se modelo avançado não passar ao menos 2/3 sem fallback; APK deve conter evidência. Modelo maior, atribuição e licença publicados como artefato distinto.
- a6f8137: nova verificação SHA com memo por fingerprint no uso do GGUF instalado, limpeza de importações interrompidas no início do processo, NLI da justificativa realmente gerada contra fala + fonte teórica, além do conceito editorial. NLI não prova uma interpretação nem certifica fatos clínicos.

## Evidência
CI 45753763896 (5ec50b8) confirmou lógica e Chromium; geração real ainda em execução na consulta. Nova revisão a6f8137 aguardando CI. Não declarar 2/3 ou 3/3 até logs verificáveis. Testes físicos e RAM/latência do Galaxy A57 continuam adiados, conforme decisão do titular. Validação de formato, relevância lexical e NLI são controles experimentais, não avaliação clínica independente. A auditoria de três situações não certifica o desempenho nos 21 autores.

Fonte oficial do modelo maior: https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/blob/main/qwen2.5-1.5b-instruct-q4_k_m.gguf
