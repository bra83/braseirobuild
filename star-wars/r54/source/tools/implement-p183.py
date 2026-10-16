from pathlib import Path
root=Path(__file__).resolve().parents[1]
folder=root/'app/src/main/assets/vtt/js/adventure'
j=folder/'jewelOfYavin.mjs';s=j.read_text()
s=s.replace("productCode: 'SWE10'", "productCode: 'SWE09'")
s=s.replace("  escapePlatform: 'joy.loc.escape_platform'", "  escapePlatform: 'joy.loc.escape_platform',\n  portTown: 'joy.loc.port_town',\n  marketRow: 'joy.loc.market_row',\n  touristDistrict: 'joy.loc.tourist_district',\n  grandPrix: 'joy.loc.grand_prix',\n  centralBank: 'joy.loc.central_bank'")
s=s.replace("  lobot: 'joy.npc.lobot'", "  lobot: 'joy.npc.lobot',\n  aris: 'joy.npc.aris_shen',\n  arend: 'joy.npc.arend_shen',\n  grayson: 'joy.npc.marus_grayson',\n  selis: 'joy.npc.bialar_selis'")
s=s.replace("  startNodeId: 'bespin-arrival',", "  startNodeId: 'port-town-rendezvous',")
s=s.replace("  schemaVersion: 1,", "  schemaVersion: 2,")
s=s.replace("    heistAlarm: { value: 0, max: 6, status: 'ACTIVE' }", "    heistAlarm: { value: 0, max: 6, status: 'ACTIVE' },\n    grandPrixPressure: { value: 0, max: 4, status: 'ACTIVE' }")
# New nodes before existing first node. All new story beats are paraphrased from the official PDF.
new_nodes='''    'port-town-rendezvous': {
      episode: 1, title: 'Encontro em Port Town', locationId: LOC.portTown,
      summary: 'Aris Shen procura a tripulação nas ruas do porto; os homens de Vorse Tabarith não estão longe.',
      narration: 'Em Port Town, a névoa de Bespin se mistura ao vapor das oficinas. Aris chega sem escolta e examina as saídas antes de revelar quem a enviou.',
      visibleNpcIds: [NPC.aris], checks: [{skill:'Percepção',difficulty:'Média',purpose:'identificar perseguidores antes do contato'}],
      choices: [{id:'protect-aris',label:'Ajudar Aris a escapar pelo mercado',to:'market-row-chase'}]
    },
    'market-row-chase': {
      episode: 1, title: 'Fuga por Market Row', locationId: LOC.marketRow,
      summary: 'Uma perseguição entre lojas e passarelas separa Aris dos capangas de seu marido.',
      narration: 'Os corredores estreitos de Market Row se enchem de gritos. Uma porta de serviço oferece cobertura, mas também pode conduzir a um beco sem saída.',
      visibleNpcIds:[NPC.aris], checks:[{skill:'Atletismo',difficulty:'Média',purpose:'escapar dos perseguidores sem abandonar Aris'}],
      choices:[{id:'reach-tourist-district',label:'Conduzir Aris até o Distrito Turístico',to:'shen-contract'}]
    },
    'shen-contract': {
      episode:1,title:'O acordo de Arend Shen',locationId:LOC.touristDistrict,
      summary:'Arend propõe manipular o leilão, desviar o lance vencedor no Banco Central e obter a joia.',
      narration:'O anfitrião espera numa sala isolada. Aris mantém a porta sob observação enquanto o pai explica um plano que exige mais do que um ladrão habilidoso.',
      visibleNpcIds:[NPC.aris,NPC.arend],checks:[{skill:'Negociação',difficulty:'Difícil',purpose:'examinar os riscos e condições do contrato'}],
      choices:[{id:'survey-cloud-city',label:'Investigar a cidade e os preparativos do golpe',to:'bespin-arrival'}]
    },
    'heist-homework': {
      episode:1,title:'Preparação do golpe',locationId:LOC.touristDistrict,
      summary:'Investigar os licitantes, o museu e o Banco Central antes da corrida e do leilão.',
      narration:'Os dossiês de concorrentes se acumulam sobre a mesa. Uma falha no planejamento pode expor o grupo antes da primeira oferta.',
      visibleNpcIds:[NPC.aris],checks:[{skill:'Manha',difficulty:'Média',purpose:'identificar interesses dos licitantes'}],
      choices:[{id:'prepare-grand-prix',label:'Preparar veículo e equipe para o Grande Prêmio',to:'grand-prix-preparation'}]
    },
    'grand-prix-preparation': {
      episode:1,title:'Oficina e credenciais',locationId:LOC.grandPrix,
      summary:'A corrida é uma via de acesso à festa; credenciais, mecânicos e o veículo precisam de atenção.',
      narration:'Nos boxes, as turbinas vibram em marcha lenta. Rivais observam a equipe enquanto os fiscais conferem registros e números de competição.',
      visibleNpcIds:[],checks:[{skill:'Mecânica',difficulty:'Média',purpose:'preparar o cloud car para a prova'}],
      choices:[{id:'take-race-start',label:'Largar no Grande Prêmio de Cloud City',to:'grand-prix-race'}]
    },
    'grand-prix-race': {
      episode:1,title:'Grande Prêmio de Cloud City',locationId:LOC.grandPrix,
      summary:'O piloto enfrenta adversários e obstáculos sobre os níveis de Bespin; o resultado fica registrado.',
      narration:'O ar gelado atravessa a cabine quando o sinal da largada desaparece. Os veículos rivais mergulham entre as plataformas de mineração.',
      visibleNpcIds:[],checks:[{skill:'Pilotagem (Planetária)',difficulty:'Difícil',purpose:'obter colocação suficiente para a recepção'}],
      choices:[{id:'race-aftermath',label:'Encerrar a corrida e seguir ao círculo de vencedores',to:'grand-prix-aftermath'}]
    },
    'grand-prix-aftermath': {
      episode:1,title:'Círculo dos vencedores',locationId:LOC.grandPrix,
      summary:'A equipe apura a classificação e procura uma entrada legítima ou alternativa para a festa.',
      narration:'A multidão vibra junto ao pódio. A festa continua, mas a posição na corrida altera quem recebe os convites e quem desperta suspeitas.',
      visibleNpcIds:[NPC.aris],checks:[{skill:'Charme',difficulty:'Média',purpose:'negociar acesso ao evento após a corrida'}],
      choices:[{id:'go-to-museum',label:'Ir ao museu para o leilão',to:'gala-reconnaissance'}]
    },
    'cloud-nine-party': {
      episode:2,title:'Recepção no Cloud Nine',locationId:LOC.museumGala,
      summary:'Os licitantes disputam a joia; a equipe precisa criar pressão de preço sem revelar o plano.',
      narration:'As conversas cessam quando o brilho da gema surge na vitrine. Marus Grayson observa a plateia, atento a cada oferta.',
      visibleNpcIds:[NPC.grayson],checks:[{skill:'Enganação',difficulty:'Difícil',purpose:'estimular a disputa sem denunciar a manipulação'}],
      choices:[{id:'bank-operation',label:'Executar a operação bancária durante o leilão',to:'central-bank-slice'}]
    },
    'central-bank-slice': {
      episode:2,title:'Invasão ao Banco Central',locationId:LOC.centralBank,
      summary:'Desviar a transferência do lance vencedor exige um teste de Computadores adjudicado.',
      narration:'Linhas de autenticação percorrem o terminal. Um desvio prematuro deixa uma trilha rastreável para a segurança financeira de Bespin.',
      visibleNpcIds:[],checks:[{skill:'Computadores',difficulty:'Difícil',purpose:'desviar a transferência do leilão'}],
      choices:[
        {id:'skip-bank-transfer',label:'Abandonar a transferência e priorizar a joia',to:'vault-infiltration'},
        {id:'divert-winning-bid',label:'Confirmar desvio bancário bem-sucedido',to:'vault-infiltration',condition:'check-success:0',effects:{bankDiversionSecured:true}}
      ]
    },
    'escape-without-jewel': {
      episode:3,title:'Retirada sem a joia',locationId:LOC.escapePlatform,
      summary:'O golpe não recuperou a joia. O grupo pode fugir sem atribuir a si recompensas não obtidas.',
      narration:'Os corredores da plataforma ficam para trás. A joia permanece em Bespin, e as decisões tomadas ainda terão consequências.',
      visibleNpcIds:[],checks:[],choices:[]
    },
'''
s=s.replace("  nodes: {\n    'bespin-arrival':", "  nodes: {\n"+new_nodes+"    'bespin-arrival':")
s=s.replace("{ id: 'enter-gala', label: 'Infiltrar-se na gala de exibição da joia', to: 'gala-reconnaissance' }", "{ id: 'prepare-heist', label: 'Levantar dados dos licitantes e do Grande Prêmio', to: 'heist-homework' }")
s=s.replace("{ id: 'prepare-heist', label: 'Coordenar o plano de invasão ao cofre', to: 'vault-infiltration' }", "{ id: 'attend-cloud-nine', label: 'Acompanhar a recepção e o leilão', to: 'cloud-nine-party' }")
s=s.replace("{ id: 'take-jewel', label: 'Substituir a joia por uma réplica e fugir', to: 'cloud-city-pursuit', effects: { jewelAcquired: true } }", "{ id: 'retreat-without-jewel', label: 'Retirar-se sem a joia', to: 'cloud-city-pursuit' },\n        { id: 'take-jewel', label: 'Retirar a joia após destravar o cofre', to: 'cloud-city-pursuit', condition:'check-success:0', effects: { jewelAcquired: true } }")
s=s.replace("{ id: 'reach-hangar', label: 'Alcançar a plataforma secreta de decolagem', to: 'jewel-epilogue' }", "{ id: 'reach-hangar-empty', label: 'Escapar sem a joia', to: 'escape-without-jewel', condition:'flag-not:jewelAcquired' },\n        { id: 'reach-hangar', label: 'Alcançar a plataforma com a joia', to: 'jewel-epilogue', condition:'flag:jewelAcquired' }")
# Guard against unknown/invalid conditions. Legacy saves with explicit jewelAcquired remain compatible.
a=s.index('function conditionOk(choice, progress) {');b=s.index('\nfunction playerNode',a)
s=s[:a]+'''function conditionOk(choice, progress) {
  const c = choice?.condition;
  if (c == null) return true;
  if (typeof c !== 'string') return false;
  if (c.startsWith('unvisited:')) return !progress.visitedNodeIds.includes(c.slice(10));
  if (c.startsWith('visited:')) return progress.visitedNodeIds.includes(c.slice(8));
  if (c.startsWith('flag:')) return progress.flags?.[c.slice(5)] === true;
  if (c.startsWith('flag-not:')) return progress.flags?.[c.slice(9)] !== true;
  if (c.startsWith('check-success:')) {
    const index = Number(c.slice(14));
    if (!Number.isSafeInteger(index) || index < 0) return false;
    const records = (progress.checkHistory || []).filter(e => e.nodeId === progress.currentNodeId && e.checkIndex === index);
    return records.length > 0 && records.at(-1).success === true;
  }
  return false;
}
''' + s[b:]
s=s.replace("  const entry = { at: now(), nodeId: p.currentNodeId, checkIndex, success: Boolean(result.success) };", """  if (typeof result.success !== 'boolean') throw new Error('Resultado de teste deve indicar sucesso verdadeiro ou falso.');
  const entry = { at: now(), nodeId: p.currentNodeId, checkIndex, success: result.success };
  if (!entry.success && ['grand-prix-race','central-bank-slice','vault-infiltration'].includes(p.currentNodeId)) {
    const clock = p.clocks.heistAlarm;
    clock.value = Math.min(clock.max, clock.value + 1);
    clock.status = clock.value >= clock.max ? 'COMPLETED' : 'ACTIVE';
  }""")
s=s.replace("    credits: Number(details.credits) || 5000,\n    obligationReduced: Number(details.obligationReduced) || 10,", "    credits: Number.isFinite(Number(details.credits)) ? Number(details.credits) : 0,\n    obligationReduced: Number.isFinite(Number(details.obligationReduced)) ? Number(details.obligationReduced) : 0,")
s=s.replace("    note: details.note || 'Recompensa pelo roubo histórico da Joia de Yavin'", "    note: details.note || 'Desfecho da Joia de Yavin: valores dependem de adjudicação e recibos'" )
j.write_text(s)
# Long Arm: expand original source-specific optional route while retaining established IDs and save paths.
l=folder/'longArmOfTheHutt.mjs';s=l.read_text()
s=s.replace("  teemoPalace: 'laoth.loc.teemo_palace'", "  teemoPalace: 'laoth.loc.teemo_palace',\n  nabat: 'laoth.loc.nabat',\n  lylekDen: 'laoth.loc.lylek_den',\n  newMeen: 'laoth.loc.new_meen',\n  piddockParty: 'laoth.loc.piddock_party'")
s=s.replace("  vorn: 'laoth.npc.vorn'", "  vorn: 'laoth.npc.vorn',\n  ota: 'laoth.npc.ota',\n  nyn: 'laoth.npc.nyn',\n  bura: 'laoth.npc.bura_ban',\n  piddock: 'laoth.npc.duke_piddock',\n  dimmock: 'laoth.npc.duke_dimmock',\n  thwheek: 'laoth.npc.thwheek'")
s=s.replace("  schemaVersion: 1,", "  schemaVersion: 2,")
# Additional route nodes; original start and two legacy choice IDs are retained.
new='''    'krayt-fang-logs': {
      episode:1,title:'Registros da Krayt Fang',locationId:LOC.rylosHangar,
      summary:'Antes de pousar, a tripulação examina os registros da nave e um possível sinalizador de Teemo.',
      narration:'O ruído dos motores encobre um aviso no terminal de bordo. A rota de Ryloth ainda não está segura.',
      visibleNpcIds:[],checks:[{skill:'Computadores',difficulty:'Média',purpose:'localizar registros e sinais de rastreamento'}],
      choices:[{id:'finish-ship-inspection',label:'Prosseguir até Nabat',to:'ryloth-safehouse'}]
    },
    'nabat-contact': {
      episode:1,title:'Contato em Nabat',locationId:LOC.nabat,
      summary:'Nyn e B’ura B’an procuram ajuda para os mineiros de New Meen.',
      narration:'Entre passagens subterrâneas, trabalhadores deslocados descrevem a pressão crescente sobre as minas de ryll.',
      visibleNpcIds:[NPC.nyn,NPC.bura],checks:[{skill:'Charme',difficulty:'Média',purpose:'conquistar a confiança dos representantes twi’lek'}],
      choices:[{id:'travel-lylek-den',label:'Viajar para New Meen',to:'lylek-den-ambush'}]
    },
    'lylek-den-ambush': {
      episode:1,title:'Emboscada no covil de lylek',locationId:LOC.lylekDen,
      summary:'O caminho das minas atravessa um antigo covil onde caçadores de Teemo aguardam a tripulação.',
      narration:'A poeira cobre a trilha. O eco de um disparo distante anuncia que os perseguidores encontraram a rota.',
      visibleNpcIds:[],checks:[{skill:'Percepção',difficulty:'Difícil',purpose:'identificar o ataque antes de entrar na passagem'}],
      choices:[{id:'reach-new-meen',label:'Continuar até New Meen após resolver a ameaça',to:'new-meen-miners'}]
    },
    'new-meen-miners': {
      episode:1,title:'A comunidade de New Meen',locationId:LOC.newMeen,
      summary:'Os mineiros precisam de proteção contra os capangas que buscam controlar a extração de ryll.',
      narration:'Ferramentas paradas e moradias danificadas mostram o preço da coerção. Os trabalhadores aguardam uma decisão.',
      visibleNpcIds:[NPC.bura],checks:[{skill:'Liderança',difficulty:'Média',purpose:'organizar a defesa e a retirada dos civis'}],
      choices:[{id:'meet-bothan-ota',label:'Ouvir a informação trazida pelo agente Bothan',to:'ota-briefing'}]
    },
    'ota-briefing': {
      episode:2,title:'Informações de Ota',locationId:LOC.nabat,
      summary:'O Bothan revela as compras de equipamentos de Teemo e suas negociações com Geonosianos.',
      narration:'Ota aguarda a saída dos demais antes de abrir seu arquivo. A oportunidade de enfraquecer Teemo depende de contatos perigosos.',
      visibleNpcIds:[NPC.ota],checks:[{skill:'Manha',difficulty:'Média',purpose:'avaliar a credibilidade e as ligações da informação'}],
      choices:[{id:'follow-ota-geonosis',label:'Partir para Geonosis',to:'geonosis-approach'}]
    },
    'piddock-party': {
      episode:2,title:'Recepção de Duke Piddock',locationId:LOC.piddockParty,
      summary:'A festa do duque permite colher provas das transações e da rivalidade entre fornecedores.',
      narration:'Em meio a convidados e mercadores, cada conversa pode revelar um novo elo entre Teemo e a indústria de armas.',
      visibleNpcIds:[NPC.piddock],checks:[{skill:'Enganação',difficulty:'Difícil',purpose:'recolher informações sem alertar os presentes'}],
      choices:[{id:'speak-with-dimmock',label:'Buscar o apoio de Duke Dimmock',to:'dimmock-negotiation'}]
    },
    'dimmock-negotiation': {
      episode:2,title:'A posição de Duke Dimmock',locationId:LOC.piddockParty,
      summary:'Dimmock desconfia de Teemo, mas só ajudará se as evidências forem convincentes.',
      narration:'O duque escuta em silêncio. Uma acusação precipitada pode fechar a única via de apoio diplomático.',
      visibleNpcIds:[NPC.dimmock],checks:[{skill:'Negociação',difficulty:'Difícil',purpose:'persuadir o duque com os registros reunidos'}],
      choices:[{id:'geonosis-final-evidence',label:'Reunir os documentos antes do retorno',to:'geonosis-surface'}]
    },
'''
s=s.replace("  nodes: {\n    'rylos-escape-aftermath':", "  nodes: {\n"+new+"    'rylos-escape-aftermath':")
s=s.replace("{ id: 'dock-ryloth', label: 'Aterrissar nos hangares subterrâneos de Ryloth', to: 'ryloth-safehouse' }", "{ id: 'dock-ryloth', label: 'Aterrissar nos hangares subterrâneos de Ryloth', to: 'ryloth-safehouse' },\n        {id:'inspect-krayt-fang',label:'Examinar registros e rastreadores antes do pouso',to:'krayt-fang-logs'}")
s=s.replace("{ id: 'head-to-geonosis', label: 'Seguir para Geonosis em busca de aliados', to: 'geonosis-approach' }", "{ id: 'head-to-geonosis', label: 'Seguir para Geonosis em busca de aliados', to: 'geonosis-approach' },\n        {id:'help-new-meen',label:'Investigar a situação dos mineiros de New Meen',to:'nabat-contact'}")
s=s.replace("{ id: 'enter-canyon', label: 'Descer até os desfiladeiros da colmeia', to: 'geonosis-surface' }", "{ id: 'enter-canyon', label: 'Descer até os desfiladeiros da colmeia', to: 'geonosis-surface' },\n        {id:'attend-piddock-party',label:'Buscar informações na recepção de Duke Piddock',to:'piddock-party'}")
# No unknown condition bypass on legacy adapter.
s=s.replace("  if (!c) return true;", "  if (c == null) return true;\n  if (typeof c !== 'string') return false;")
s=s.replace("  return true;\n}\n\nfunction playerNode", "  return false;\n}\n\nfunction playerNode")
s=s.replace("  const entry = { at: now(), nodeId: p.currentNodeId, checkIndex, success: Boolean(result.success) };", "  if (typeof result.success !== 'boolean') throw new Error('Resultado de teste deve ser booleano.');\n  const entry = { at: now(), nodeId: p.currentNodeId, checkIndex, success: result.success };")
l.write_text(s)
# Unsourced automatic rewards: remove generic defaults for NEW campaigns, preserve historical receipts in saves.
reg=folder/'publishedAdventureRegistry.mjs';s=reg.read_text().replace("rewardPolicy: Object.freeze({ credits: 1500, obligationReduction: 5 }),", "rewardPolicy: Object.freeze({ credits: 0, obligationReduction: 0 }),").replace("rewardPolicy: Object.freeze({ credits: 5000, obligationReduction: 10 }),", "rewardPolicy: Object.freeze({ credits: 0, obligationReduction: 0 }),")
reg.write_text(s)
print('P183 changes authored')
