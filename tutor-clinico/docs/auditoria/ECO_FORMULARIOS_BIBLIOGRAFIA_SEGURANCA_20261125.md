# ECO — formulários residuais, bibliografia e segurança (25/11/2026)

## Escopo e critérios
Revisão interna de código, catálogo bibliográfico e testes automatizados; não é pentest externo, certificação clínica, auditoria jurídica ou validação em Galaxy A57. A certificação física foi adiada pelo titular. Não é possível afirmar risco zero ou perda zero.

## Formulários
- Cobertura anterior: atendimento manual, sessão de áudio, cadastro/edição do paciente, agenda, anamnese, enquadre, sonhos, familiares e cronologia.
- Novo diário de rascunhos residuais cifrado em SQLite: ficha evolutiva, rede de vínculos, preparação, fechamento por sessão, questões novas e atualizações por ID, 12 eixos de investigação por ID, hipóteses, intervenções, relações do genograma, plano de segurança, diário privado, respostas por modelo de questionário, financeiro, modelos de questionário, biblioteca pessoal, título de modelo documental, revisão de síntese IA e metadados de anexo.
- Rascunhos de cada paciente e de cada registro são separados; quatro categorias administrativas usam namespace reservado. Rascunhos não são registros aprovados. A confirmação de impressão/consentimento, senhas e escolhas transitórias de filtros não são persistidas. Revisão de IA exige a mesma proveniência de fontes; texto divergente permanece consultável, não é associado a outra consulta.
- Rascunhos de diário privado são omitidos de exportação padrão e incluídos apenas mediante a opção explícita. O sucesso de salvar no diário é conferido no cofre segregado. Anexo: somente metadados de formulário; bytes do arquivo entram após autorização e seleção. O rascunho é limpo após constatação de novo anexo correspondente.
- Limites: fechamento e questões exigem selecionar o registro original; modelos documentais guardam o nome do rascunho, não consentimentos de impressão. Debounce de 300 ms não garante sobreviver a encerramento forçado instantâneo. Backup de grandes volumes continua fora deste bloco.
- Testes Chromium: recuperação de segurança, isolamento entre pacientes, preparação, questionário administrativo, confirmação de salvamento e limpeza. Testes anteriores de anamnese/enquadre/sonhos/cronologia preservados. Os testes da interface usam bridge simulada; APK compilado não comprova criptografia nativa em dispositivo.

## Bibliografia
42 entradas de 21 autores: 42/42 com localizador bibliográfico, incluindo catálogos editoriais, bibliotecas, PEP-Web, PubMed e revisões acadêmicas quando o original não estava acessível. As fontes e o estado de conferência são expostos na biblioteca do aplicativo. São sínteses originais didáticas, não transcrições dos livros.
- PDM-3: edição de 5 de janeiro de 2026, Guilford, ISBN 9781462558711; artigo de apresentação de 2025, PMID 40948049. Antes o livro estava datado incorretamente como 2025.
- Benjamin: PMID 14750464; Luyten et al.: PMID 32023093 e 38262188; Shedler: DOI 10.1037/a0018378; Solms: DOI 10.1080/15294145.2013.10773711.
- Os metadados do catálogo de 42 itens não provam que cada síntese é sustentada por uma página específica. O inventário da biblioteca anterior identifica referências sem conferência e fontes compostas parcialmente localizadas. Não reproduz obras licenciadas nem inventa paginação. A conferência integral de todos os textos, páginas e licenças exige acesso autorizado às edições e revisão especializada. Obras de terceiros incluídas pelo usuário permanecem opt-in e declaradas por ele.
- Fontes: https://www.guilford.com/books/Psychodynamic-Diagnostic-Manual/Lingiardi-McWilliams/9781462558711 ; https://pubmed.ncbi.nlm.nih.gov/40948049/ ; https://pubmed.ncbi.nlm.nih.gov/14750464/ ; https://pubmed.ncbi.nlm.nih.gov/32023093/ ; https://pubmed.ncbi.nlm.nih.gov/38262188/ ; https://pubmed.ncbi.nlm.nih.gov/20141265/ . Localizadores de todos os itens no array ecoBibliographicChecks.

## Segurança: revisão interna e correções
| Controle | Estado / evidência |
|---|---|
| Rede | Manifest sem INTERNET, cleartext desativado, WebView sem cargas de rede, política CSP com connect-src none |
| WebView | FLAG_SECURE, depuração explicitamente desativada, acessos a arquivos e content URI desativados, prévia HTML em iframe sandbox sem scripts; ponte nativa ainda é superfície de ataque |
| Criptografia | AES-GCM 256 no AndroidKeyStore; IV gerado pelo provedor; backups PBKDF2-HMAC-SHA256 240000 e AES-GCM; leitores de áudio autenticam frames e trailer; alterações de conteúdo falham na verificação |
| Controle de acesso | PIN e bloqueio após tentativas, biometria forte opcional como gate de UI, reautenticação para importação/exportação; seletor expirado após 120 s, segredos pendentes descartados no bloqueio/destruição; ponte de rascunho por caso não aceita chamadas bloqueadas |
| Diário privado | Cofre separado, sem rastreamento automático pelo RAG, rascunho excluído por padrão do backup; mensagens de erro sem conteúdo clínico |
| Rascunhos | IDs divergentes recusados, segregação do namespace administrativo e campos recuperados somente para revisão |
| IA | Sem APIs, consulta por autor, fontes declaradas, geração não clínica e sinalização de referências ou formato incompletos; não elimina prompt injection semântica nem alucinações |
| Testes | Regressões estáticas em qa/test.mjs, fluxos Chromium com conteúdo sintético; validação de ASR e modelos na CI |

## Riscos residuais que não podem ser declarados resolvidos
- AndroidKeyStore não está vinculado criptograficamente à autenticação biométrica em cada uso: a chave é acessível ao processo do app. A mudança exige arquitetura de chaves e migração com recuperação; não trocar chave no lugar sob risco de perda dos registros.
- SQLite protege payloads cifrados, porém metadados relacionais como IDs, datas e estados não equivalem a criptografia integral do banco. Proteção contra aparelho comprometido/root não garantida.
- Trechos WAV temporários para Whisper podem existir em cache privado enquanto o motor trabalha. Exclusão e limpeza de resíduos não equivalem a apagamento físico verificável em flash.
- Exportações de anexos/PDF saem do cofre; o operador deve proteger o destino. Backups externos, autorização para gravação, minimização e política de retenção são responsabilidades separadas.
- Sem revisão externa, pentest, auditoria independente de dependências/SBOM, teste físico de autenticação, destruição de processo, backup e restauração, ou inspeção clínica por especialista, não declarar conformidade certificada, 100% de segurança ou aptidão para uso real.

## Evidência da rodada
- Última base completa aprovada antes deste bloco: 25d3d53, execução 45674344041.
- Testes de lógica e Chromium aprovados em 45568df (45698612475) e 90fbf5b (45698864123); compilações completas posteriores não observadas no momento deste registro.
- Não confundir teste de bridge simulada com teste nativo físico.
