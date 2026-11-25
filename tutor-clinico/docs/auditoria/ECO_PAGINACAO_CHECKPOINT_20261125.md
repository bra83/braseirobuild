# ECO — consulta paginada e checkpoints integrais locais (25/11/2026)

## Base comprovada
Run 45708149853 (426a0af) completo aprovado; backup integral superior a 50 MB, restauração sob outra chave e rollback em corrupção passaram no CI anterior.

## Entrega
- Consulta nativa somente leitura: páginas de 20 casos/sessões/segmentos, cursor por ID ou tempo+ID e texto de sessão em janelas de 16.000 caracteres. Isolamento por caso e sessão, sem reconstrução do JSON global. Interface disponível nos Ajustes e na recuperação de abertura.
- Preflight de volume (24.000.000 bytes cifrados estimados, limiar conservador não benchmarkado): evita entregar um snapshot excessivo à WebView e oferece consulta paginada. Não é edição paginada; a edição, busca global e assistente completos ainda dependem do estado global. Segmentos muito longos exibem prévia e permanecem íntegros no banco.
- Dois slots locais integrais rotativos (2/3), AtomicFile, senha aleatória individual protegida pela chave do cofre, envelope autenticado em blocos e reexportação portátil com senha do usuário. Anexos, revisões e rascunhos SQLite incluídos; diário privado e áudio temporário excluídos. Criação manual reautenticada, cancelamento, exportação inclusive no modo de recuperação. Checkpoints JSON antigos (0/1) continuam disponíveis.
- Testes de paginação/cross-case, janelas de texto, restauração de snapshot antigo com chave nova, exclusão de diário, anexos, cancelamento durante gravação, dois slots e bridge.

## Limites
O novo CI precisa passar. Não se trata de edição integral paginada, nem de checkpoint automático integral: a rotina automática antiga continua separada. A janela de texto limita a transferência à interface, mas cada registro cifrado é autenticado e decifrado por inteiro. Os checkpoints no mesmo aparelho não protegem contra perda do dispositivo; exportar cópia independente. Nenhum teste físico ou validação clínica nesta etapa.
