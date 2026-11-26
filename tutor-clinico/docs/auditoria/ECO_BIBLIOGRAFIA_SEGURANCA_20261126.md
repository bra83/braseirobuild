# ECO — revisão bibliográfica e segurança, 26/11/2026

Base: 6a5091f; CI 45782134457 completo aprovado. Escopo: revisão de localizadores e controles de software; sem acesso aos textos integrais licenciados, sem certificação clínica, jurídica ou pentest externo.

## Localizadores reconferidos
- The Ego and the Mechanisms of Defence (1936): Editora · edição de 1966; original de 1936. https://www.routledge.com/The-Ego-and-the-Mechanisms-of-Defence/Freud/p/book/9781855750388
- Normality and Pathology in Childhood (1965): Wellcome · edição londrina de 1966; obra de 1965. https://wellcomecollection.org/works/nutzuj9c
- Confusion of Tongues Between Adults and the Child (1933): Registro IJP 30:225–230 (tradução inglesa de 1949; original de 1933). https://stage.pep-web.org/content/IJP.030.0225A
- The Elasticity of Psycho-Analytic Technique (1928): Índice editorial da coletânea que contém o capítulo. https://www.routledge.com/Final-Contributions-to-the-Problems-and-Methods-of-Psycho-analysis/Ferenczi/p/book/9780429474750
- The Origins of Transference (1952): PubMed · artigo de Melanie Klein. https://pubmed.ncbi.nlm.nih.gov/12999365/
- The Theory of the Parent-Infant Relationship (1960): PubMed · IJP 41:585–595. https://pubmed.ncbi.nlm.nih.gov/13785877/
- The Analysis of the Self (1971): University of Chicago Press · registro específico da obra. https://press.uchicago.edu/ucp/books/book/chicago/A/bo8324792.html
- The Restoration of the Self (1977): University of Chicago Press · registro específico da obra. https://press.uchicago.edu/ucp/books/book/chicago/R/bo8324789

As 42 fichas mantêm localizador. O inventário distingue localização parcial, registro anterior e reconferência específica. Nenhum localizador equivale a leitura integral, confirmação de página ou licença. O RAG por autor fica restrito às duas fichas didáticas registradas de cada autor; biblioteca geral e referências pessoais permanecem consultáveis separadamente. As sínteses não são citações primárias.

## Segurança
- WebViews de interface e impressão: sem autofill de terceiros, sem captura de conteúdo pelo serviço Android quando disponível, sem múltiplas janelas, sem conteúdo misto, sem file URLs privilegiadas.
- Impressão passa por PIN nativo; autorização perde validade após bloqueio/recriação, antes da entrega ao spooler. Depois de entregue, o spooler/arquivo é externo ao cofre.
- CI verifica permissões da APK compilada contra allowlist e publica inventário Gradle do runtime. Isto não equivale a SBOM completa ou varredura de vulnerabilidades.
- Testes de regressão de bibliografia, isolamento do corpus e controles WebView. CI novo ainda pendente.

## Limites
Conferir todas as obras/páginas/licenças exige acesso autorizado. Não certificar chave vinculada a biometria, segurança de aparelho comprometido, metadados SQLite integralmente cifrados, retenção no spooler, ou conformidade externa. O benchmark neural mede aceitação automatizada, não validade clínica. O modelo leve obteve 2/21, avançado 16/21 em 45782134457.
