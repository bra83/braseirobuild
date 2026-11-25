# ECO — fechamento do gargalo de backup integral — 25/11/2026

## Base homologada em CI
Execução 45705845669, commit c926ab9: workflow completo success, teste integrado de anexos acima de 24 MB aprovado, APK ARM64 e modelos offline verificados. Pyannote: 0 quadros acima de 0,65 em silêncio, 0 em voz única e 386 na mistura de duas vozes sintéticas. Isso é evidência de inferência real, não de precisão clínica em fala espontânea. APK de teste publicado; assinatura permanente ausente.

## Nova implementação
- Arquivo integral .ecobkp v2, um snapshot SQLite consistente, percorrido por tabela e registro; envelope AES-256-GCM em blocos autenticados, PBKDF2-HMAC-SHA256, final autenticado e orçamento simétrico de 8 GiB. Não usa JSON global nem envia o backup pela WebView.
- Abrange casos, sessões, jobs sem áudio pendente, segmentos originais e marcações, agenda, rascunhos SQLite, configurações portáveis, revisões, anexos e blocos. Diário privado excluído por padrão e incluído apenas por autorização explícita. Não transporta gravações temporárias, chaves, PIN ou checkpoints de decodificador. Exige finalizar gravações/rascunhos de áudio ativos.
- Restauração somente em cofre vazio, dentro de transação única. Recriptografia sob chave de destino; validação de referências, IDs, checksum e anexos; falha antes da confirmação final causa rollback. Arquivo legado e mesclagem não destrutiva continuam disponíveis separadamente.
- Exportação integral acessível na tela de recuperação se a interface não conseguir carregar o estado.
- Novo teste nativo cria arquivo superior a 50 MB com anexos e sessões sintéticas; verifica recusa de cofre ocupado, restauração sob outra chave, rollback em corrupção do último byte, diário excluído, revisão, rascunho e segmento original preservados. Testes de bridge e opção de diário acrescentados.
- Gate Pyannote agora exige contraste funcional real (mistura >=100 quadros, voz única <=5, silêncio =0) e evidência no APK.

## Limites restantes
Código publicado; novo CI ainda não concluído. O teto de 8 GiB é orçamento de implementação, não benchmark de telefone. A abertura e edição normais da interface ainda usam o snapshot global; portanto recuperação de um arquivo grande no SQLite não certifica a navegação ou desempenho nessa escala. O checkpoint local antigo ainda tem limite próprio. O backup integral não realiza mesclagem em cofre povoado e não carrega áudio temporário. Sem promessa de perda zero. Pyannote não separa sinais nem transcreve duas vozes simultâneas individualmente. Validação clínica e física, revisão bibliográfica primária, assinatura permanente e segurança independente continuam gates distintos.
