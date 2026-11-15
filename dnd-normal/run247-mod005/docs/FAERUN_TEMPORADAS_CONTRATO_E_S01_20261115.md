# Braseiro D&D — Temporadas de Aventura em Faerûn
## Contrato editorial e técnico v1 • 2026-11-15

**Estado:** temporada 01 cadastrada e projetada no contexto de Mestre, sem homologação de compilação, de campanha longa ou de Android físico.
**Base:** branch de correção 6.6.59/PR #96, sem merge nem alterações em Mystara.
**Fonte da metodologia:** estudo estrutural, sem reprodução, dos materiais enviados pelo usuário do Clube dos Taberneiros: cenário, gerador, encontros e locais.
**Referências oficiais:** [Mapa de Faerûn (D&D Beyond)](https://www.dndbeyond.com/resources/1782-map-of-faerun), [Forgotten Realms](https://www.dndbeyond.com/resources/2037-the-forgotten-realms), [SRD 5.2.1](https://www.dndbeyond.com/srd), [Adventures in Faerûn](https://www.dndbeyond.com/sources/dnd/fraif).

### 1. Objetivo

Transformar o VTT de D&D em um ambiente capaz de receber **temporadas narrativas modulares**, sem roteiro obrigatório. Uma temporada oferece mundo, locais, tensões, grupos, aventuras opcionais, geradores e encontros. A IA é intérprete e narradora; a autoridade mecânica (D&D 2024, ações, recursos, rolamentos, descanso, progressão, movimento) permanece no host. Não substituir `Core2024RulesEngine`, `WorldStateEngine`, `TravelRulesEngine`, Atlas, ledger, Gemini, TTS, banco ou backups.

**Nunca** confundir:
- gancho ≠ aventura aceita;
- proposta ≠ acontecimento realizado;
- rumor ≠ fato verdadeiro;
- possibilidade secreta ≠ conhecimento de NPC;
- encontro ≠ combate automático;
- narrar resultado ≠ resolver teste pelo motor;
- lugar original inserido no mapa ≠ lugar canônico;
- volume editorial ≠ dia/estação do calendário do mundo.

### 2. Primeira temporada — As Contas da Estrada Longa

**Proposta:** uma região comercial com múltiplas pressões simultâneas — desaparecimento de fretes, cobrança de estradas, rotas fluviais problemáticas, queimadas e falsificações — sem antagonista único obrigatório. A vida diária, trabalho, caça, comércio e exploração continuam mesmo quando os aventureiros ignoram essas tensões.

O primeiro contato pós-Strixhaven permanece **Yartar, DM28**, somente depois de confirmado pelo catálogo do mapa instalado. O Marco do Percurso / Newarre permanece conteúdo de campanha existente e não é marcado como canônico de Forgotten Realms.

**Ancorações canônicas previstas (8):** Yartar, Triboar, Westbridge, Red Larch, Beliard, Womford, Waterdeep e Neverwinter. **Só Yartar/DM28 tem o hex explicitamente informado/confirmado nesta entrega**; nenhuma outra coordenada foi criada. O motor confere o nome exato e o hex corrente do banco do Atlas antes de oferecer um gancho. Localizações secundárias criadas para esta temporada são invenções originais situadas *dentro* dessas áreas, nunca substituições dos catálogos oficiais.

**Conteúdo do pacote:** 5 facções originais, 6 arcos abertos, 10 aventuras opcionais com consequências previstas para a inação, 12 encontros de viagem/cotidiano, 10 locais preparados e 4 modelos de relógio não instanciados.

**Aventura importável já escrita:** `app/src/main/assets/faerun_seasons/adventures/as_mulas_voltaram_v1.json`, usando o contrato `braseiro.adventure/v1` que já existe no aplicativo: 5 locais, 4 NPCs originais, 5 objetivos, 6 encontros, 6 pistas e 4 segredos privados. Ainda requer importação explícita e validação integrada; não é iniciada automaticamente. Gerador de 6 categorias × 6 subcategorias × 6 resultados = **216 entradas básicas**, combináveis por **12 lançamentos de D6**; saída é só uma premissa, não evento factual.

### 3. Divisão editorial em três volumes

| Volume | Conteúdo | Situação |
| --- | --- | --- |
| I — O Norte em movimento | Panorama regional, ganchos, seis conflitos, gerador | Dados iniciais presentes no JSON; material de leitura diagramado ainda não criado |
| II — Gente e Caminhos | Facções, encontros, dez locais e suas pistas | Dados iniciais presentes; mapas táticos/ilustrações autorais ainda não criados |
| III — Convergências | Aventuras, relógios, consequências e conexões | Dez premissas, uma aventura completa em JSON, quatro relógios-modelo e possibilidades de inação; outras nove aventuras completas e testes de balanceamento ainda pendentes |

Isso é uma **base programada de ganchos**, uma aventura estruturada importável e um **contrato técnico**, não uma temporada editorial completa ou uma campanha 100% homologada.

### 4. Integração real nesta branch

- `app/src/main/assets/faerun_seasons/temporada_01_estrada_longa.json`: **fonte única** do conteúdo.
- `FaerunSeasonRuntime.java`: leitor Android com validação de esquema, geração D6 reprodutível, seleção de até dois ganchos, proteção contra mutações e validação do Atlas.
- `BraseiroDndApplication.currentContext()`: entrega apenas o contexto da **Application** para ler o asset local; não retém Activity.
- `WorldStateEngine.compactContext(focus)`: acrescenta `faerun_season_director_only` somente em pesquisas voluntárias por trabalho/boato/aventura, e preserva a chave quando o contexto é reduzido.
- `FaerunSeasonRuntimeRobolectricTest`: cenários de contrato/canonicidade, perfis isolados, demanda explícita, ausência de mudanças mecânicas, D6 auditável e proteção contra edição 2014.

**Regras duras:** só perfil `dnd`; Strixhaven ativo é bloqueio; nome + hex devem coincidir com o Atlas efetivamente instalado; nenhuma recompensa, viagem, teste, sucesso, combate, facção, marca no mapa, NPC ou hora é escrita por esta camada.

### 5. Caminho da narrativa para o mundo vivo

```
AÇÃO DO JOGADOR / PERGUNTA AO MESTRE
    ↓
Estado canônico: campanha, Atlas, cena, relógio, inventário, NPCs
    ↓
Se jogador solicitar oportunidades, temporada valida localização
    ↓
São apresentadas 0–2 POSSIBILIDADES não descobertas
    ↓
Mestre compõe cena sem declarar sucesso, presença ou testemunho
    ↓
Jogador escolhe uma ação real
    ↓
Core D&D 2024 resolve; commit transacional grava apenas fatos confirmados
    ↓
Rumores, economia, facções, calendário, consequências existentes reagem
```

O gerador D6 é uma ferramenta de prévia, não gatilho automático. Nenhuma entrada do catálogo substitui `BraseiroAdventureStore`; importações de aventuras e ledgers existentes continuam separadas. O motor atual não atribui esses ganchos ao diário automaticamente, não os renderiza em uma nova aba e não instancia os quatro relógios do pacote. Essas ações exigem outra etapa de integração e seus próprios testes.

### 6. Sem regressão: critérios de aceite

1. Build Android na branch, incluindo `FaerunSeasonRuntimeRobolectricTest`.
2. Reabrir campanha pós-Strixhaven, confirmar Yartar = DM28 e buscar "há trabalhos?": mostrar proposta **apenas ao Mestre**, sem inserir uma missão.
3. Perguntar "observo os telhados": zero novos ganchos; zero NPCs/eventos inventados.
4. Abrir mapa com posição desconhecida ou incoerente: não inserir ganchos nem deslocar token.
5. Trocar para Mystara ou Strixhaven: zero ganchos da temporada.
6. Gerar a mesma premissa com a mesma semente: resultados idênticos e 12 lançamentos D6 registrados.
7. Propor teste/magia/combate: a temporada não devolve CD, dano, XP, bônus, sucesso nem recursos.
8. Ignorar uma proposta: nenhum relógio de temporada deve avançar sem regra/commit explícito.
9. BACKUP21 exporta/importa a campanha antiga; testes de reabertura não regressivos.
10. Quatro ações sugeridas atuais permanecem adequadas à cena; não converter gancho não descoberto em ação contextual.
11. Verificação com Gemini real, campainha temporal, imagens e Galaxy A57 **obrigatória antes de liberar APK**.

### 7. Expansão após a fundação

Uma temporada nova deve trazer seu próprio JSON com os mesmos limites, novas relações geográficas e sem repetir fórmulas narrativas. Conjuntos futuros possíveis: fronteiras ao norte, Cormanthor e Dalelands, Geleiras do Vale do Vento Gélido, Chult, Subterrâneo e Calimshan — **não implementados** nesta branch. Cada lugar deve ser buscado no Atlas; lugares sem correspondência ficam inativos até revisão. Não assumir ano canônico específico sem configurar a era da campanha.

### 8. Direitos e proveniência

Todos os textos de missões, locais secundários e facções criados neste pacote são **originais**. Não há cópia do texto ou da arte do Clube dos Taberneiros. O uso das regras abertas deve respeitar a licença **CC-BY-4.0/SRD 5.2.1**. **Forgotten Realms e seus mapas não passam a ser CC-BY por causa do SRD**; uma eventual distribuição ou comercialização de material ambientado em Faerûn exige verificar a modalidade/licença cabível, inclusive DMs Guild quando aplicável. Evitar distribuir mapa/editoriais oficiais como se fossem assets autorais.

### 9. Estado de verificação

- Registro dos arquivos na branch: **comprovado pelos commits GitHub**.
- Validação estrutural local do JSON: **executada na criação** (tabelas 6×6×6 e IDs únicos).
- Testes Robolectric: **escritos, ainda sem execução comprovada**.
- Build Android, integração real com Gemini, navegação no Galaxy A57, releitura do diário/backup: **não verificados**.

Não fazer merge automático, não atualizar o APK e não declarar lançamento final sem fechar esses gates.
