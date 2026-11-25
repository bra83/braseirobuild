# ECO 0.23.0 — candidato corretivo, não homologado

A matriz histórica de 165 requisitos e os relatórios Antigravity são evidências legadas, não uma certificação desta versão. Não considerar 148 itens integralmente validados.

Correções: migração v5 com FK ativas e staging isolado, preservação de metadados, chave AndroidKeyStore sem fallback volátil, identificação por job/caso/sessão, ACK nativo e contagem única, pré-registro de spool, recuperação explícita de prefixo autenticado sem declarar EOF completo, diário privado append/delete, consentimento na recuperação, cache TAGARELA com lease e proteção de concorrência.

Evidência local: 9 verificações SQL (não equivalem a instrumentação Android), 9 testes JVM do leitor de produção, sintaxe JS e 11 sondagens comportamentais da 0.22 reexecutadas na primeira onda. CI executa suites adicionais e build; status deve ser consultado pelo SHA exato.

Pendências: recuperação da captura deve ser homologada em Android real; ownership do serviço/Activity e fila durável, backup/restore completo e migrações físicas, prevenção de WAV intermediário do Whisper, diarização/RTF/benchmark A57, reranking/NLI/LLM/RAG/treinamento e requisitos originais restantes. Sem dados reais de pacientes até validação. Não reduzir escopo. Não instalar por cima sem backup e assinatura compatível.
