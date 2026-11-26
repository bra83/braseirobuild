# ECO — entrega de software e evidências em 26/11/2026

**Base testada:** `5ecad1768027ac5fafc157ac9f5fede9b07c4176` — execução [45786218946](https://github.com/bra83/braseirobuild/actions/runs/45786218946) concluída com sucesso. Esta entrega não certifica todos os 165 requisitos. A matriz histórica permanece intacta; ECO_EVIDENCIAS_165_20261126.csv contém os 165 IDs com evidência relacionada, estado histórico e limites explícitos. Não se reduz critério para obter aprovação.

## Entregáveis verificados
- [APK debug ARM64 0.24.0](https://github.com/bra83/braseirobuild/actions/runs/45786218946/artifacts/15101507651)
- [Qwen 1.5B offline opcional](https://github.com/bra83/braseirobuild/actions/runs/45786218946/artifacts/15101152019)
- [Relatório neural 21 autores](https://github.com/bra83/braseirobuild/actions/runs/45786218946/artifacts/15101456478)
- [Evidências visuais](https://github.com/bra83/braseirobuild/actions/runs/45786218946/artifacts/15100930689)
- [Permissões do APK e inventário runtime](https://github.com/bra83/braseirobuild/actions/runs/45786218946/artifacts/15102245633)

## Evidências CI
- Testes Node, Chromium (16 destinos, 3 larguras, 2 temas), integração SQLite em Robolectric, migrações de versões históricas, oito sessões sintéticas consecutivas, backup integral >50 MiB e restauração atômica, sete checkpoints com reserva de espaço.
- Escala 100 casos/10.000 sessões: 364 ms gravação, 127 ms reabertura/leitura, fixture serializada 1.151.221 bytes (host, não A57).
- ONNX real, paridade de tokenização NLI, reranking, TAGARELA PT-BR e separação neural Conv-TasNet: ganhos 8,55–11,25 dB SI-SDR em duas misturas sintéticas. Não generalizar para áudio clínico.
- Qwen leve 2/21 (9,52%), avançado 16/21 (76,19%); 3/3 casos adversariais recusados por modelo; zero erros de execução. P50/P95: leve 8154/9281 ms; avançado 13512/14452 ms. Aceitação automatizada, não exatidão clínica.
- APK íntegro, arquitetura ARM64, permissões do pacote compilado aprovadas. Publicação debug concluída. Release permanente omitido por credencial indisponível.

## Classificação conservadora do complemento
- parcial_com_limite_explicito: 19
- evidencia_relacionada_subcriterios_nao_certificados: 132
- bloqueio_externo_ou_fisico: 7
- politica_ou_escopo_futuro: 7

A maioria dos requisitos tem evidência relacionada, não aprovação de cada subcritério. Não calcular porcentagem de conclusão a partir deste agrupamento.

## Bloqueios e trabalho restante
- Teste físico A57, impressão/spooler, credencial release, acesso autorizado a obras e revisão clínica/jurídica e de segurança independentes.
- Software ainda parcial: modo de grande volume não contém toda edição/fluxo de IA; fila de próxima sessão durante processamento não comprovada; benchmark Parakeet e ASR representativo incompleto; WAV intermediário Whisper, criptografia integral de metadados e arquitetura de chaves exigem avaliação; backup histórico integral não é backup incremental verdadeiro. Modelo 0.5B insuficiente no teste autoral. Não declarar risco zero.
- PC/Drive e publicação comercial fora da homologação atual.

## Instalação para homologação
Utilizar dados fictícios. O modelo avançado é opcional e deve ser importado no aplicativo; não é outro APK. Antes de qualquer reinstalação, exportar cofre integral cifrado, verificar acesso ao backup e guardar a senha fora do aparelho. Não remover o aplicativo antigo com dados sem backup verificado. Não utilizar dados reais até avaliação profissional e de privacidade aplicável.
