# Escopo de conteúdo — sandbox R54 P19

## Incluído nesta entrega

- Regras EotE/AoR e complementos catalogados como base de consulta.
- Criação manual, aleatória e importação de personagens.
- Equipamentos, talentos, veículos, adversários genéricos, facções, NPCs e mundo vivo para uma campanha aberta.
- Mapas, localizadores e assets visuais incorporados ao catálogo local quando houver vínculo exato.
- Barbara/Gemini/TTS como infraestrutura de consulta, narração e resolução validada.

Auditoria P6: 243 armas e 29 itens/equipamentos possuem binding visual exato; 82/82 localidades possuem asset de mapa valido. Desses locais, 1 usa mapa dedicado confirmado e 81 usam esquemas locais jogaveis gerados, rotulados explicitamente como esquemas, nao como cartografia oficial.

## Explicitamente fora desta entrega

- Nenhuma aventura publicada do Drive é carregada como campanha.
- Nenhum roteiro, missão obrigatória, capítulo, trilha de cenas ou mapa específico de aventura é iniciado automaticamente.
- A pasta Drive `Aventuras` é somente fonte futura; ela não é corpus padrão nem conteúdo inicial do sandbox.

## Ponto de extensão futuro

O sandbox expõe `startSandboxSession`, `sandboxOpening`, oportunidades, hooks, facções, clocks e a Biblioteca/RAG. Um pacote de aventura futuro pode ser importado e ativado explicitamente, sem alterar o núcleo de regras. Fontes desativadas permanecem visíveis na Biblioteca, mas são filtradas antes do RAG e não influenciam Barbara.

O fluxo visual usa **INICIAR SANDBOX**. O alias legado `startLivingAdventure` existe apenas para compatibilidade com bridges antigas e não carrega aventura publicada.

## Limite de incorporação do Drive

O Drive é o acervo-fonte, não um diretório que o APK copia integralmente. A entrega embarca o catálogo e os assets visuais validados; livros e complementos em PDF continuam disponíveis para importação/indexação privada pela Biblioteca. Isso evita afirmar que cada PDF/imagem do Drive já virou regra executável. A pasta `Aventuras` permanece deliberadamente fora do corpus inicial.
