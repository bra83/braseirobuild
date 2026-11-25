# ECO — backup de grandes volumes e detecção de fala sobreposta — 25/11/2026

## Entregas
- Correção de alinhamento dos quatro grupos acústicos e abstenção em temporização conflitante (358da44, dfcf771).
- Transporte portátil binário AES-256-GCM com PBKDF2-HMAC-SHA256 240000, salt e nonce por arquivo, blocos de 256 KiB, índice/tamanho/cabeçalho autenticados e marcador final obrigatório (c6eef01, fac2bf9, 4a64a7f).
- Arquivo complementar de anexos até 8 GiB, mantendo 8 MB por anexo; SAF com reautenticação, cancelamento, integridade de anexo, transação de restauração e mesclagem sem substituir registros existentes (5e1dce7, daa6124, e55e22f, 069f961, f59b42c, 201a239). Exportar backup clínico sem anexos primeiro, depois arquivo de anexos. Na recuperação, restaurar casos antes de anexos. Diário privado continua excluído por padrão.
- Pyannote Segmentation 3.0 FP32 (MIT, CNRS; exportação sherpa-onnx), ONNX SHA256 220ad67ca923bef2fa91f2390c786097bf305bceb5e261d4af67b38e938e1079, disponível no pipeline por blocos com diarização ativa. Inferência em janelas de 10s, classes de pares de vozes e marcação experimental de possível sobreposição; não há identificação clínica ou separação de sinais (8e00305, be6bde9, e817250, 5a8db32, 271cd1f, 6e4a5ae).
- CI: regressão do transporte em heap limitado, teste nativo de restauração de anexos e rollback após corrupção final, filtro de revisão de fala sobreposta, provisionamento do modelo e licença com inferência host e hash no APK. Chromium compatível com a versão Playwright, cancelamento de compilações obsoletas por branch.

## Evidência e limites
- Execuções 45704329153 e 45704324152: transporte de 50+ MiB com heap de 32 MiB e regressão de quatro falantes aprovados. As mesmas execuções falharam posteriormente por crash fatal interno do Chromium. Fix de browser em 073fa90, ainda sem confirmação final.
- Teste de restauração SQLite >24 MB, testes UI novos, execução neural de fala sobreposta, compilação e APK do último conjunto ainda não confirmados no momento da publicação.
- Backup de grandes coleções de anexos não equivale a backup integral de prontuários arbitrariamente grandes: o snapshot clínico JSON continua limitado a 50 MB e trabalha em memória. Os dois arquivos são exportados em momentos distintos, não constituem snapshot atômico único. Exportar com atividade clínica suspensa e validar recuperação em dispositivo de teste. O orçamento de 8 GiB é limite de código, não benchmark comprovado. Os arquivos devem ser guardados em local autorizado.
- OSD é experimental: limiar não calibrado clinicamente; não separa falas, não produz transcrição individual de duas vozes simultâneas e não comprova DER/JER ou precisão em fala espontânea PT-BR. Legado Vosk em tempo real e condições reais ainda exigem validação.
- Não declarar 100% ou liberar para dados reais; manter certificação física adiada, revisão bibliográfica integral, pentest, assinatura permanente e matriz de 165 requisitos como gates independentes.
