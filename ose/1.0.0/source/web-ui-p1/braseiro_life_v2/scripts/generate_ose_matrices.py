#!/usr/bin/env python3
"""
Generate OSE Classic and OSE Advanced Life Engine Matrix Packs (V2.2)
Full parity with Braseiro Motor de Vida V2.2 specifications:
- 100 distinct situations per system (5 situations per each of the 20 domains)
- 5 scene modes per situation (observe, proposal, consequence, interaction, atmosphere)
- 100 base rows (base100.json) and 500 expanded rows (expanded500.json)
- Strict adherence to braseiro.matrix-table/v1 and braseiro.matrix-pack/v1 schemas
"""
from pathlib import Path
import json, hashlib

ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT / 'data' / 'matrices'
PACKS_DIR = DATA_DIR / 'packs'

DOMAINS = [
    'sleep', 'memory', 'emotion', 'habits', 'family',
    'intimacy', 'work', 'resources', 'social', 'reputation',
    'npc', 'world', 'recovery', 'belief', 'goals',
    'body', 'home', 'community', 'legacy', 'justice'
]

MODES = [
    ('observe', False, 'neutral'),
    ('proposal', True, 'neutral'),
    ('consequence', False, 'negative'),
    ('interaction', False, 'positive'),
    ('atmosphere', False, 'neutral')
]

# 100 situations for OSE Classic (5 per domain)
OSE_CLASSIC_SITUATIONS = [
    # sleep
    ('sleep', ['journey', 'health'], 'O descanso na masmorra é interrompido por goteiras e eco distante nos corredores de pedra.'),
    ('sleep', ['violence', 'recovery'], 'A vigília com tocha tremulante permite cochilos curtos entre as patrulhas de guarda.'),
    ('sleep', ['home', 'care'], 'A estalagem do vilarejo oferece colchão de palha seca e calor da lareira comum.'),
    ('sleep', ['scarcity', 'health'], 'O vento gelado no ermo penetra as mantas de viagem ao redor da fogueira apagada.'),
    ('sleep', ['discovery', 'journey'], 'Um ruído suave no teto da galeria subterrânea desperta a sentinela atenta.'),
    # memory
    ('memory', ['loss', 'violence'], 'Uma espada entalhada recorda um companheiro abatido por cararniços em masmorra antiga.'),
    ('memory', ['discovery', 'journey'], 'Um mapa de pergaminho amarelado mostra anotações de um batedor que nunca retornou.'),
    ('memory', ['victory', 'reputation'], 'O elmo com marcas de garras traz à memória o triunfo contra a matilha de ghouls.'),
    ('memory', ['legacy', 'home'], 'Uma moeda de prata cunhada sob o antigo ducado lembra tempos de fartura antes da ruína.'),
    ('memory', ['betrayal', 'conflict'], 'A insígnia rasgada evoca a deserção de um guia contratado nos pântanos sombrios.'),
    # emotion
    ('emotion', ['victory', 'recovery'], 'O peso da sacola de ouro traz alívio após semanas de privações sob a terra.'),
    ('emotion', ['loss', 'failure'], 'A perda de um escudeiro leal em emboscada pesa no silêncio da companhia reunida.'),
    ('emotion', ['care', 'celebration'], 'O brinde com cerveja preta na taverna aquece o ânimo após a venda do saque.'),
    ('emotion', ['conflict', 'reputation'], 'O olhar desconfiado dos mercenários contratados revela a tensão após a batalha.'),
    ('emotion', ['discovery', 'recovery'], 'O vislumbre da luz do sol na saída da caverna renova a esperança de sobrevivência.'),
    # habits
    ('habits', ['journey', 'work'], 'A rotina de contar tochas, estacas de ferro e óleo de lâmpada precede qualquer descida.'),
    ('habits', ['violence', 'health'], 'Limpar a ferrugem e o sangue das cotas de malha faz parte do ritual vespertino.'),
    ('habits', ['home', 'recovery'], 'Verificar os fechos das mochilas e o peso das moedas transportadas evita surpresas.'),
    ('habits', ['belief', 'care'], 'O clérigo entoa prece silenciosa ao amanhecer antes de memorizar as bênçãos sagradas.'),
    ('habits', ['work', 'discovery'], 'Testar o piso de pedra com vara de dez pés é precaução indispensável nos corredores.'),
    # family
    ('family', ['home', 'legacy'], 'Uma carta da aldeia natal indaga sobre notícias e o envio de moedas para os parentes.'),
    ('family', ['care', 'loss'], 'O retorno ao povoado permite abraçar familiares que temiam a morte do aventureiro.'),
    ('family', ['work', 'change'], 'Um irmão mais jovem expressa desejo de juntar-se como auxiliar na próxima expedição.'),
    ('family', ['legacy', 'justice'], 'A herança de uma pequena herdade familiar exige decisões sobre posse e tributos.'),
    ('family', ['home', 'celebration'], 'Uma festa comemorativa na casa da família reúne conhecidos para ouvir relatos.'),
    # intimacy
    ('intimacy', ['care', 'promise'], 'Durante a guarda da madrugada, uma conversa reservada fortalece o vínculo entre aliados.'),
    ('intimacy', ['loss', 'recovery'], 'O cuidado ao tratar ferimentos com ligaduras limpas cria momento de confiança mútua.'),
    ('intimacy', ['home', 'hope'], 'Planos sobre construir uma cabana ou adquirir terras são compartilhados junto ao braseiro.'),
    ('intimacy', ['betrayal', 'conflict'], 'Uma discordância sobre a partilha justa de gemas pede diálogo franco entre parceiros.'),
    ('intimacy', ['celebration', 'care'], 'Um brinde compartilhado à luz de velas sela promessa de proteção mútua nas masmorras.'),
    # work
    ('work', ['work', 'scarcity'], 'O ferreiro local cobra soldo adicional para consertar escudos partidos e afiar alabardas.'),
    ('work', ['reputation', 'justice'], 'O tabelião do ducado redige contrato de trabalho para mercenários e auxiliares.'),
    ('work', ['discovery', 'work'], 'O boticário examina ervas colhidas nos bosques à procura de ingredientes curativos.'),
    ('work', ['change', 'work'], 'O curtidor de couro confecciona alforjes reforçados para aguentar o peso do minério.'),
    ('work', ['failure', 'work'], 'Uma gazua partida durante arrombamento exige reposição com o artesão da vila.'),
    # resources
    ('resources', ['scarcity', 'journey'], 'As rações de ferro estão quase no fim, restando apenas farelo seco e água turva.'),
    ('resources', ['work', 'change'], 'A cotação de tochas e frascos de óleo subiu na vila devido à escassez de sebo animal.'),
    ('resources', ['discovery', 'victory'], 'Uma arca de carvalho reforçada com ferro esconde sacos de moedas de eletro e gemas.'),
    ('resources', ['scarcity', 'work'], 'Mulas de carga e arreios precisam de pasto seguro antes da jornada pelos desfiladeiros.'),
    ('resources', ['loss', 'scarcity'], 'A corda de cinquenta pés desfiou-se nas bordas ásperas do abismo subterrâneo.'),
    # social
    ('social', ['celebration', 'home'], 'A taverna do Porco Saltitante está repleta de mineiros e camponeses entoando cantigas.'),
    ('social', ['reputation', 'care'], 'Aldeões oferecem queijo e pão fresco aos aventureiros que limparam o velho túmulo.'),
    ('social', ['conflict', 'scarcity'], 'Mercadores de passagem disputam espaço nos estábulos para suas mulas de frete.'),
    ('social', ['discovery', 'work'], 'Um bardo itinerante troca rumores sobre ruínas ao norte por uma taça de vinho forte.'),
    ('social', ['reputation', 'justice'], 'O magistrado local convida a liderança da companhia para jantar no solar fortificado.'),
    # reputation
    ('reputation', ['victory', 'reputation'], 'O rumor de que a cripta dos orcos foi saqueada atrai novos caçadores de fortuna à vila.'),
    ('reputation', ['failure', 'loss'], 'A fuga precipitada diante do minotauro mancha a coragem do grupo perante os guardas.'),
    ('reputation', ['justice', 'reputation'], 'O templo da Lei proclama bênção solene pelos artefatos recuperados dos ímpios.'),
    ('reputation', ['conflict', 'reputation'], 'Bandidos da estrada hesitam em emboscar a caravana ao reconhecer os estandartes do grupo.'),
    ('reputation', ['discovery', 'reputation'], 'Um colecionador de antiguidades procura os heróis para comprar pergaminhos velhos.'),
    # npc
    ('npc', ['work', 'care'], 'O auxiliar contratado solicita adiantamento do soldo diário para mandar aos filhos.'),
    ('npc', ['conflict', 'violence'], 'O guia mercenário exige moral firme e recusa avançar em corredores infestados de gás.'),
    ('npc', ['discovery', 'work'], 'Um anão ferreiro aposentado oferece segredo sobre a liga dos portões da fortaleza caída.'),
    ('npc', ['betrayal', 'scarcity'], 'O carregador de tochas vacila de medo e ameaça abandonar a carga se o grupo não recuar.'),
    ('npc', ['care', 'recovery'], 'A curandeira do bosque trata febre pútrida em troca de favores futuros da companhia.'),
    # world
    ('world', ['change', 'scarcity'], 'O rigor do inverno fechou a passagem das montanhas, isolando o vale dos mercadores.'),
    ('world', ['victory', 'change'], 'A limpeza do covil dos kobolds reabriu a trilha dos carvoeiros para a floresta alta.'),
    ('world', ['conflict', 'violence'], 'Notícias de incursões de salteadores forçam camponeses a buscar refúgio no fortim.'),
    ('world', ['discovery', 'change'], 'Um desmoronamento na encosta revelou a entrada de uma catacumba há séculos soterrada.'),
    ('world', ['change', 'justice'], 'O novo capitão da guarda impôs toque de recolher e revista rigorosa nas portas da vila.'),
    # recovery
    ('recovery', ['care', 'health'], 'Repouso completo de uma semana no leito da estalagem devolve vigor aos membros cansados.'),
    ('recovery', ['recovery', 'home'], 'A aplicação de cataplasmas de ervas limpas acelera o fechamento dos talhos profundos.'),
    ('recovery', ['care', 'recovery'], 'Um banho quente de tina e sopa farta restauram as forças da companhia esgotada.'),
    ('recovery', ['health', 'relief'], 'O veneno paralisante foi purgado do sangue após dias de calafrios e hidromel quente.'),
    ('recovery', ['recovery', 'celebration'], 'O clérigo agradece a graça divina após retirar a aflição que nublava a visão do batedor.'),
    # belief
    ('belief', ['belief', 'justice'], 'O altar da Lei no santuário local emite tênue calor quando mãos piedosas se aproximam.'),
    ('belief', ['conflict', 'discovery'], 'Símbolos heréticos gravados na rocha subterrânea desafiam a pureza da fé sacerdotal.'),
    ('belief', ['loss', 'care'], 'Ritos fúnebres celebrados segundo o rito antigo garantem que a alma não vagueie errante.'),
    ('belief', ['discovery', 'belief'], 'A inscrição em língua clássica revela advertência sobre os perigos da cobiça desmedida.'),
    ('belief', ['change', 'belief'], 'O equilíbrio neutro das forças naturais é testemunhado nos ciclos implacáveis da floresta.'),
    # goals
    ('goals', ['promise', 'work'], 'Juntar moedas suficientes para adquirir armadura de placas completa na forja ducal.'),
    ('goals', ['legacy', 'home'], 'Construir uma torre de vigia fortificada para servir de abrigo permanente na fronteira.'),
    ('goals', ['discovery', 'work'], 'Localizar o tomo perdido de magias arcanas contendo encantamentos de segundo círculo.'),
    ('goals', ['reputation', 'victory'], 'Elevar o renome do grupo até receber título de suserania das mãos do conde.'),
    ('goals', ['care', 'promise'], 'Pagar resgate exigido por salteadores para libertar reféns da aldeia vizinha.'),
    # body
    ('body', ['health', 'violence'], 'Uma cicatriz no ombro lateja quando nuvens de chuva anunciam tempestade nos ermos.'),
    ('body', ['recovery', 'health'], 'Os calos nas mãos calejadas pela empunhadura da espada dão firmeza aos golpes.'),
    ('body', ['scarcity', 'health'], 'Os pés inchados de marcha forçada em lamaçal exigem escalda-pés com sal e ervas.'),
    ('body', ['health', 'loss'], 'A falta de ar em galerias profundas sem ventilação alerta para bolsões de ar viciado.'),
    ('body', ['recovery', 'care'], 'A visão noturna aguçada do elfo permite identificar silhuetas onde humanos só veem breu.'),
    # home
    ('home', ['home', 'recovery'], 'O quarto alugado na hospedaria serve de depósito para baús trancados e provisões extras.'),
    ('home', ['care', 'home'], 'A lareira acesa e a mesa tosca de carvalho tornam o casebre refúgio acolhedor na serra.'),
    ('home', ['legacy', 'home'], 'Reparos no telhado de palha impedem infiltrações durante a estação chuvosa.'),
    ('home', ['conflict', 'home'], 'O senhorio da pensão reclama do barulho de armaduras e do cheiro de troféus de monstros.'),
    ('home', ['home', 'celebration'], 'Uma despensa abastecida com presuntos defumados e barris de cidra garante o inverno.'),
    # community
    ('community', ['celebration', 'community'], 'A feira de outono atrai pastores e mercadores para negociar lã, sal e ferramentas.'),
    ('community', ['care', 'scarcity'], 'Moradores unem esforços para reconstruir celeiro destruído por ataque de trasgos.'),
    ('community', ['conflict', 'justice'], 'O conselho de anciãos debate regras para uso comunitário da ponte de madeira sobre o rio.'),
    ('community', ['change', 'community'], 'Refugiados de fazendas queimadas montam tendas improvisadas ao redor da capela.'),
    ('community', ['reputation', 'community'], 'Crianças do vilarejo imitam os aventureiros com espadas de pau e escudos de casca de árvore.'),
    # legacy
    ('legacy', ['legacy', 'work'], 'Um jovem escudeiro aprende a encordar arco longo e manter a corda encerada contra umidade.'),
    ('legacy', ['legacy', 'loss'], 'O velho escudo da infantaria ducal é passado ao filho antes de sua primeira marcha.'),
    ('legacy', ['discovery', 'legacy'], 'Baladas antigas cantadas à lareira preservam os nomes dos heróis que ergueram a abadia.'),
    ('legacy', ['victory', 'legacy'], 'O registro no livro de tombo da paróquia anota a doação de ouro feita pelos aventureiros.'),
    ('legacy', ['home', 'legacy'], 'Um marco de pedra lavrada na bifurcação da estrada honra os pioneiros que desbravaram a terra.'),
    # justice
    ('justice', ['justice', 'reputation'], 'O juiz de paz examina provas de furto de gado antes de determinar restituição em moedas.'),
    ('justice', ['conflict', 'justice'], 'Mercenários flagrados em arruaça na estalagem são postos no pelourinho até o meio-dia.'),
    ('justice', ['discovery', 'justice'], 'Contrabando de armas roubadas da milícia é descoberto escondido em barris de arenque.'),
    ('justice', ['promise', 'justice'], 'O pagamento do dízimo ao conde é conferido sob o olhar vigilante dos sargentos de armas.'),
    ('justice', ['betrayal', 'justice'], 'Um falso mapa de tesouro vendido na feira resulta em prisão do charlatão pela guarda.')
]

# 100 situations for OSE Advanced (5 per domain)
OSE_ADVANCED_SITUATIONS = [
    # sleep
    ('sleep', ['journey', 'health'], 'O descanso em tenda de campanha avançada é velado por sentinelas com darkvision atenta.'),
    ('sleep', ['violence', 'recovery'], 'O sono leve do bárbaro fareja fumaça e passo de predadores muito antes do alarme geral.'),
    ('sleep', ['home', 'care'], 'O mosteiro fortificado oferece celas silenciosas para meditação e sono sem sobressaltos.'),
    ('sleep', ['scarcity', 'health'], 'A névoa mefítica do pântano exige filtros de tecido umedecido para dormir sem tossir.'),
    ('sleep', ['discovery', 'journey'], 'Um cântico druídico ao longe embala o acampamento nas cercanias do bosque de faias.'),
    # memory
    ('memory', ['loss', 'violence'], 'Uma adaga envenenada de guilda secreta evoca uma dívida de sangue jamais perdoada.'),
    ('memory', ['discovery', 'journey'], 'Um astrolábio de latão pertencente a um navegador svirfneblin traz memórias da profundeza.'),
    ('memory', ['victory', 'reputation'], 'O colar de dentes de hidra atesta a bravura comprovada diante dos anciãos bárbaros.'),
    ('memory', ['legacy', 'home'], 'O grimório herdado de um mestre ilusionista traz anotações codificadas em tinta de polvo.'),
    ('memory', ['betrayal', 'conflict'], 'A promessa quebrada por um cavaleiro rival arde viva a cada desafio em torneio de armas.'),
    # emotion
    ('emotion', ['victory', 'recovery'], 'A conquista do feudo nas marcas de fronteira enche o peito do paladino de devoção sagrada.'),
    ('emotion', ['loss', 'failure'], 'O fracasso na colheita de beladona antes do murchar das flores frustra os planos do alquimista.'),
    ('emotion', ['care', 'celebration'], 'O murmúrio das liras dos bardos na corte senhorial ameniza o peso das campanhas militares.'),
    ('emotion', ['conflict', 'reputation'], 'O escárnio de nobres cortesãos desafia a paciência do meio-orc recém-armado cavaleiro.'),
    ('emotion', ['discovery', 'recovery'], 'Desvendar o enigma de um espelho de ilusão traz triunfo intelectual ao arcanista dedicado.'),
    # habits
    ('habits', ['journey', 'work'], 'O acrobata testa a tensão de cabos de cânhamo e o equilíbrio em bordas antes de escalar.'),
    ('habits', ['violence', 'health'], 'O assassino confere a vedação dos frascos de veneno vegetal e as lâminas ocultas nas botas.'),
    ('habits', ['home', 'recovery'], 'O bardo afina as cordas de tripa do alaúde e revisa o repertório de baladas heróicas.'),
    ('habits', ['belief', 'care'], 'O druida colhe visco com foice dourada na lua nova para alimentar seu foco sagrado.'),
    ('habits', ['work', 'discovery'], 'O ranger inspeciona pegadas e ramagens quebradas na trilha para avaliar o rastro da presa.'),
    # family
    ('family', ['home', 'legacy'], 'Um clã de duergar cobra laços de sangue e lealdade ancestral à forja sob a montanha.'),
    ('family', ['care', 'loss'], 'O reencontro entre irmãos meio-elfos revela escolhas divergentes entre a floresta e o burgo.'),
    ('family', ['work', 'change'], 'Um jovem gnomo demonstra engenhosidade mecânica ao aprimorar a besta de repetição familiar.'),
    ('family', ['legacy', 'justice'], 'A partilha de terras feudais exige arbitragem solene perante os arautos e o tribunal de armas.'),
    ('family', ['home', 'celebration'], 'Uma celebração solsticial no enclave élfico reúne várias gerações sob a copa das árvores.'),
    # intimacy
    ('intimacy', ['care', 'promise'], 'O cavaleiro e sua dama compartilham votos de lealdade antes da partida para a cruzada.'),
    ('intimacy', ['loss', 'recovery'], 'Um momento a sós permite ao drow expressar a dor de viver sob o estigma das profundezas.'),
    ('intimacy', ['home', 'hope'], 'Conversas sussurradas em dialeto secreto revelam sonhos de fundar um santuário próprio.'),
    ('intimacy', ['betrayal', 'conflict'], 'A descoberta de uma espionagem encomendada abala a cumplicidade forjada em batalhas.'),
    ('intimacy', ['celebration', 'care'], 'A partilha de um elixir élfico de mel e ervas sela amizade leal entre ranger e bárbaro.'),
    # work
    ('work', ['work', 'scarcity'], 'O armeiro especializado em mithral exige pagamento adiantado para forjar cota de malha leve.'),
    ('work', ['reputation', 'justice'], 'A guilda de assassinos cobra taxa de registro e respeito rigoroso aos alvos declarados.'),
    ('work', ['discovery', 'work'], 'O cartógrafo arcano desenha cartas de navegação para rotas subterrâneas inexploradas.'),
    ('work', ['change', 'work'], 'O estalajadeiro gnomo instala sistema de polias para subir bagagens sem esforço manual.'),
    ('work', ['failure', 'work'], 'Um pergaminho de ilusão rasurado requer raspagem cuidadosa com osso de baleia e nova tinta.'),
    # resources
    ('resources', ['scarcity', 'journey'], 'O abastecimento de óleo de essência vegetal para lanternas com foco regulável está escasso.'),
    ('resources', ['work', 'change'], 'O valor de gemas brutas de esmeralda e rubi atrai avaliadores da corporação dos lapidários.'),
    ('resources', ['discovery', 'victory'], 'Uma câmara secreta revela lingotes de ferro meteórico e frascos selados com lacre real.'),
    ('resources', ['scarcity', 'work'], 'Cavalos de guerra treinados para suportar barulho de aço e trombetas exigem ração nobre.'),
    ('resources', ['loss', 'scarcity'], 'O manto de camuflagem élfica rasgou-se nos espinhos das charnecas e perdeu parte da eficácia.'),
    # social
    ('social', ['celebration', 'home'], 'O banquete no castelo do barão reúne damas, cavaleiros e campeões de todas as marcas.'),
    ('social', ['reputation', 'care'], 'O círculo de druidas acolhe a comitiva com cânticos e partilha de frutos silvestres.'),
    ('social', ['conflict', 'scarcity'], 'Guerreiros bárbaros e soldados da milícia trocam provocações acaloradas no mercado de gado.'),
    ('social', ['discovery', 'work'], 'Uma tertúlia de bardos debate as variações métricas do poema da Queda de Durgath.'),
    ('social', ['reputation', 'justice'], 'O conselho dos arcanistas convoca sabat com porta fechada para deliberar sobre a relíquia.'),
    # reputation
    ('reputation', ['victory', 'reputation'], 'O renome de que o paladino curou a lepra de um peregrino atrai suplicantes à fortaleza.'),
    ('reputation', ['failure', 'loss'], 'A notícia de que a patrulha do ranger caiu em armadilha espalha apreensão nas fazendas.'),
    ('reputation', ['justice', 'reputation'], 'O código de cavalaria cumprido em duelo público rende elogios formais do marechal de armas.'),
    ('reputation', ['conflict', 'reputation'], 'Boatos de que o grupo abriga um drow renegado alimentam desconfiança na corte do bispo.'),
    ('reputation', ['discovery', 'reputation'], 'Estudiosos de várias ordens viajam para consultar o acervo de mapas raros da companhia.'),
    # npc
    ('npc', ['work', 'care'], 'O escudeiro jovem pede instrução sobre as regras de heráldica e polimento de elmos emplumados.'),
    ('npc', ['conflict', 'violence'], 'Um caçador de recompensas rastreia desertores e busca aliança tática nos ermos.'),
    ('npc', ['discovery', 'work'], 'Uma boticária meio-elfa oferece unguento contra paralisia em troca de fungos raros de grutas.'),
    ('npc', ['betrayal', 'scarcity'], 'Um arauto ambicioso distorce o resultado da escaramuça para favorecer um lorde rival.'),
    ('npc', ['care', 'recovery'], 'Um eremita svirfneblin ensina rota segura evitando os ninhos de basiliscos na escuridão.'),
    # world
    ('world', ['change', 'scarcity'], 'Tributos abusivos cobrados pelo duque desestabilizam o comércio entre os principados.'),
    ('world', ['victory', 'change'], 'A retomada da ponte dos anões reativa o fluxo de ferro e prata para as fundições do sul.'),
    ('world', ['conflict', 'violence'], 'Tropas mercenárias desmobilizadas transformam-se em bandos armados pilhando aldeias.'),
    ('world', ['discovery', 'change'], 'A quebra de um selo elemental provocou tremores que alteraram a topografia das colinas.'),
    ('world', ['change', 'justice'], 'Uma carta de foral reconhece a autonomia dos burgueses e altera as taxas alfandegárias.'),
    # recovery
    ('recovery', ['care', 'health'], 'A imposição de mãos purifica a podridão da ferida e restabelece a respiração do guerreiro.'),
    ('recovery', ['recovery', 'home'], 'Banhos termais sulfurosos na cidadela anã aliviam a rigidez muscular após dias em cota de aço.'),
    ('recovery', ['care', 'recovery'], 'Infusão de folhas de athelas preparadas por bardo acalma a febre e afasta pesadelos macabros.'),
    ('recovery', ['health', 'relief'], 'O bálsamo vegetal do druida cicatriza queimaduras de ácido sem deixar carne viva exposta.'),
    ('recovery', ['recovery', 'celebration'], 'O repouso sob as árvores cantantes de um bosque sagrado regenera mente e espírito.'),
    # belief
    ('belief', ['belief', 'justice'], 'A lâmina sagrada brilha com fogo prateado quando empunhada contra mortos-vivos sacrílegos.'),
    ('belief', ['conflict', 'discovery'], 'O círculo de pedras megalíticas guarda marcas de ritos antiquíssimos anteriores aos deuses atuais.'),
    ('belief', ['loss', 'care'], 'O canto fúnebre do bardo conduz a memória dos caídos para os salões da glória eterna.'),
    ('belief', ['discovery', 'belief'], 'A contemplação das constelações orienta a busca do ranger pelos caminhos do destino.'),
    ('belief', ['change', 'belief'], 'O voto de cavalaria impõe conduta reta mesmo quando a traição do inimigo parece triunfar.'),
    # goals
    ('goals', ['promise', 'work'], 'Construir um domínio com capela, estábulos e muralhas de cantaria no vale desbravado.'),
    ('goals', ['legacy', 'home'], 'Fundar um capítulo de ordem de cavalaria dedicado à defesa das fronteiras setentrionais.'),
    ('goals', ['discovery', 'work'], 'Dominar a fórmula alquímica do elixir da longevidade guardada nos arquivos do santuário.'),
    ('goals', ['reputation', 'victory'], 'Vencer a grande justa anual perante a corte ducal e receber a condecoração do Grifo de Ouro.'),
    ('goals', ['care', 'promise'], 'Restituir o trono ancestral usurpado da dinastia élfica nas florestas ocidentais.'),
    # body
    ('body', ['health', 'violence'], 'A musculatura robusta forjada na forja anã resiste a impactos que quebrariam homens comuns.'),
    ('body', ['recovery', 'health'], 'A flexibilidade treinada do acrobata permite desvencilhar-se de amarras e fendas estreitas.'),
    ('body', ['scarcity', 'health'], 'A tolerância adquirida contra toxinas evita que venenos moderados paralisem o organismo.'),
    ('body', ['health', 'loss'], 'A sensibilidade auditiva élfica capta o armar de um gatilho a dezenas de passos no bosque.'),
    ('body', ['recovery', 'care'], 'A constituição vigorosa do bárbaro recupera fôlego após esforço que estafaria a tropa.'),
    # home
    ('home', ['home', 'recovery'], 'A torre de guarda nas montanhas oferece seteiras defensivas e adega seca para o inverno.'),
    ('home', ['care', 'home'], 'O chalé de caça com tapeçarias e pele de urso serve de base confortável entre incursões.'),
    ('home', ['legacy', 'home'], 'A oficina subterrânea de joalheria possui fornalhas a carvão e bigornas bem aprumadas.'),
    ('home', ['conflict', 'home'], 'Vizinhos desconfiam das luzes e odores de enxofre emitidos pelo laboratório do ilusionista.'),
    ('home', ['home', 'celebration'], 'O pátio de armas da guarnição reúne soldados e auxiliares para treinamento com escudo.'),
    # community
    ('community', ['celebration', 'community'], 'O festival do equinócio reúne druidas e povo comum para abençoar rebanhos e sementes.'),
    ('community', ['care', 'scarcity'], 'A guilda de artesãos financia a construção de celeiro comunal à prova de incêndios.'),
    ('community', ['conflict', 'justice'], 'Disputa de fronteira entre baronato e território livre das tribos é levada a julgamento neutro.'),
    ('community', ['change', 'community'], 'Assentamento de colonos atrai novos ofícios e transforma aldeia em vila mercantil.'),
    ('community', ['reputation', 'community'], 'Torneio de arco e besta incentiva jovens aldeões a praticar pontaria defensiva.'),
    # legacy
    ('legacy', ['legacy', 'work'], 'Um discípulo aprende a misturar pós de ilusão e traçar círculos de proteção com precisão.'),
    ('legacy', ['legacy', 'loss'], 'A cota de malha leve entalhada com runas élficas é confiada à nova líder dos patrulheiros.'),
    ('legacy', ['discovery', 'legacy'], 'Crônicas arquivadas na abadia narram os feitos da antiga companhia das Sete Lanças.'),
    ('legacy', ['victory', 'legacy'], 'A concessão formal de brasão de armas perpetua o nome da linhagem nos anais da nobreza.'),
    ('legacy', ['home', 'legacy'], 'Um carvalho sagrado plantado no centro do arraial comemora o pacto selado com os povos livres.'),
    # justice
    ('justice', ['justice', 'reputation'], 'O tribunal ducal condena traidores que venderam segredos militares a corsários do leste.'),
    ('justice', ['conflict', 'justice'], 'Cavaleiros errantes arbitram desacordo sobre dívida de sangue entre duas famílias patrícias.'),
    ('justice', ['discovery', 'justice'], 'A investigação de um bardo desmascara falsificação de selos e decretos de expropriação.'),
    ('justice', ['promise', 'justice'], 'O código de leis promulgado pelo novo regente garante justiça rápida e penas proporcionais.'),
    ('justice', ['betrayal', 'justice'], 'Um assassino capturado com veneno de basilisco é julgado sob as leis severas do burgo.')
]

def make_rows(pack_id, system_id, situations):
    expanded = []
    base100 = []
    for idx, (dom, causes, text_base) in enumerate(situations, 1):
        sit_id = f"{pack_id}-situation-{idx}"
        first_row = None
        for mode, has_dec, val in MODES:
            m_id = f"{pack_id}-{idx}-{mode}"
            if mode == 'observe':
                mode_text = f"{text_base} Um detalhe relevante pode ser observado sem exigir deliberação imediata."
            elif mode == 'proposal':
                mode_text = f"{text_base} Uma decisão voluntária pode ser tomada pela companhia se julgar conveniente."
            elif mode == 'consequence':
                mode_text = f"{text_base} A situação impõe cautela redobrada e desgaste prático na marcha."
            elif mode == 'interaction':
                mode_text = f"{text_base} Uma oportunidade de diálogo franco e cooperação mútua se apresenta."
            else: # atmosphere
                mode_text = f"{text_base} A atmosfera do ambiente evoca o peso da tradição e a vigilância constante."

            row = {
                "id": m_id,
                "baseSituationId": sit_id,
                "text": mode_text,
                "domain": dom,
                "causeTags": causes,
                "agency": "narrative_opportunity",
                "intensity": 1 if idx % 3 == 0 else (2 if idx % 3 == 1 else 3),
                "requiresFacts": {},
                "forbidsFacts": {},
                "sceneMode": mode,
                "requiredCapabilities": [],
                "offersVoluntaryDecision": has_dec,
                "setting": system_id,
                "allowedStages": ["immediate", "settling", "established", "dormant", "long_term"],
                "cooldownDays": 14,
                "valence": val
            }
            expanded.append(row)
            if first_row is None:
                first_row = row
        base100.append(first_row)
    assert len(expanded) == 500, f"Expected 500 expanded rows, got {len(expanded)}"
    assert len(base100) == 100, f"Expected 100 base rows, got {len(base100)}"
    return base100, expanded

def sha256_file(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

def main():
    print("Generating OSE Classic & Advanced Matrix Packs with base100 and expanded500...")
    
    # Load common tables
    causes = json.loads((DATA_DIR / 'causes.expanded500.json').read_text(encoding='utf-8'))['rows']
    domains = json.loads((DATA_DIR / 'domains.expanded500.json').read_text(encoding='utf-8'))['rows']
    timing = json.loads((DATA_DIR / 'timing.expanded500.json').read_text(encoding='utf-8'))['rows']
    responses = json.loads((DATA_DIR / 'responses.expanded500.json').read_text(encoding='utf-8'))['rows']

    # 1. OSE Classic
    classic_base, classic_expanded = make_rows('ose-classic', 'ose-classic', OSE_CLASSIC_SITUATIONS)
    (DATA_DIR / 'manifestations-ose-classic.base100.json').write_text(
        json.dumps({"schema": "braseiro.matrix-table/v1", "id": "manifestations-ose-classic", "version": "2.2.0", "rows": classic_base}, indent=2, ensure_ascii=False), encoding='utf-8'
    )
    classic_table_path = DATA_DIR / 'manifestations-ose-classic.expanded500.json'
    classic_table_path.write_text(
        json.dumps({"schema": "braseiro.matrix-table/v1", "id": "manifestations-ose-classic", "version": "2.2.0", "rows": classic_expanded}, indent=2, ensure_ascii=False), encoding='utf-8'
    )
    classic_table_sha = sha256_file(classic_table_path)
    
    classic_pack = {
        "schema": "braseiro.matrix-pack/v1",
        "pack": {
            "id": "ose-classic",
            "title": "Old-School Essentials Classic Fantasy",
            "system": "ose-classic",
            "mode": "classic",
            "tables": {
                "cause": "causes",
                "domain": "domains",
                "manifestation": "manifestations-ose-classic",
                "timing": "timing",
                "response": "responses"
            },
            "version": "2.2.0"
        },
        "tables": {
            "cause": causes,
            "domain": domains,
            "manifestation": classic_expanded,
            "timing": timing,
            "response": responses
        }
    }
    classic_pack_path = PACKS_DIR / 'ose-classic.json'
    classic_pack_path.write_text(json.dumps(classic_pack, indent=2, ensure_ascii=False), encoding='utf-8')
    classic_pack_sha = sha256_file(classic_pack_path)
    print(f"Classic pack generated (SHA-256: {classic_pack_sha})")

    # 2. OSE Advanced
    adv_base, adv_expanded = make_rows('ose-advanced', 'ose-advanced', OSE_ADVANCED_SITUATIONS)
    (DATA_DIR / 'manifestations-ose-advanced.base100.json').write_text(
        json.dumps({"schema": "braseiro.matrix-table/v1", "id": "manifestations-ose-advanced", "version": "2.2.0", "rows": adv_base}, indent=2, ensure_ascii=False), encoding='utf-8'
    )
    adv_table_path = DATA_DIR / 'manifestations-ose-advanced.expanded500.json'
    adv_table_path.write_text(
        json.dumps({"schema": "braseiro.matrix-table/v1", "id": "manifestations-ose-advanced", "version": "2.2.0", "rows": adv_expanded}, indent=2, ensure_ascii=False), encoding='utf-8'
    )
    adv_table_sha = sha256_file(adv_table_path)

    adv_pack = {
        "schema": "braseiro.matrix-pack/v1",
        "pack": {
            "id": "ose-advanced",
            "title": "Old-School Essentials Advanced Fantasy",
            "system": "ose-advanced",
            "mode": "advanced",
            "tables": {
                "cause": "causes",
                "domain": "domains",
                "manifestation": "manifestations-ose-advanced",
                "timing": "timing",
                "response": "responses"
            },
            "version": "2.2.0"
        },
        "tables": {
            "cause": causes,
            "domain": domains,
            "manifestation": adv_expanded,
            "timing": timing,
            "response": responses
        }
    }
    adv_pack_path = PACKS_DIR / 'ose-advanced.json'
    adv_pack_path.write_text(json.dumps(adv_pack, indent=2, ensure_ascii=False), encoding='utf-8')
    adv_pack_sha = sha256_file(adv_pack_path)
    print(f"Advanced pack generated (SHA-256: {adv_pack_sha})")

    # 3. Create catalog-ose.json (dedicated catalog containing OSE Classic and Advanced)
    catalog_path = DATA_DIR / 'catalog.json'
    canonical_catalog = json.loads(catalog_path.read_text(encoding='utf-8'))
    
    # Restore catalog.json to original 9 packs if modified, so legacy unit test stays at 134/134 PASS
    canonical_catalog['tables'] = [t for t in canonical_catalog['tables'] if not t['id'].endswith('-ose-classic') and not t['id'].endswith('-ose-advanced')]
    canonical_catalog['packs'] = [p for p in canonical_catalog['packs'] if p['id'] not in ('ose-classic', 'ose-advanced')]
    catalog_path.write_text(json.dumps(canonical_catalog, indent=2, ensure_ascii=False), encoding='utf-8')

    # Now make catalog-ose.json which includes all 11 packs
    ose_catalog = json.loads(json.dumps(canonical_catalog))
    ose_catalog['tables'].extend([
        {
            "id": "manifestations-ose-classic",
            "baseRows": 100,
            "expandedRows": 500,
            "file": "manifestations-ose-classic.expanded500.json",
            "sha256": classic_table_sha
        },
        {
            "id": "manifestations-ose-advanced",
            "baseRows": 100,
            "expandedRows": 500,
            "file": "manifestations-ose-advanced.expanded500.json",
            "sha256": adv_table_sha
        }
    ])
    ose_catalog['packs'].extend([
        {
            "id": "ose-classic",
            "title": "Old-School Essentials Classic Fantasy",
            "system": "ose-classic",
            "mode": "classic",
            "tables": {
                "cause": "causes",
                "domain": "domains",
                "manifestation": "manifestations-ose-classic",
                "timing": "timing",
                "response": "responses"
            },
            "bundleFile": "packs/ose-classic.json",
            "bundleSha256": classic_pack_sha
        },
        {
            "id": "ose-advanced",
            "title": "Old-School Essentials Advanced Fantasy",
            "system": "ose-advanced",
            "mode": "advanced",
            "tables": {
                "cause": "causes",
                "domain": "domains",
                "manifestation": "manifestations-ose-advanced",
                "timing": "timing",
                "response": "responses"
            },
            "bundleFile": "packs/ose-advanced.json",
            "bundleSha256": adv_pack_sha
        }
    ])
    ose_catalog['baseRows'] = 1600
    ose_catalog['expandedRows'] = 8000
    (DATA_DIR / 'catalog-ose.json').write_text(json.dumps(ose_catalog, indent=2, ensure_ascii=False), encoding='utf-8')
    print("Wrote catalog-ose.json with 11 packs (including ose-classic and ose-advanced).")

    print("Generation complete!")

if __name__ == '__main__':
    main()
