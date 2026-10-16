"""P181: source-anchored Mask of the Pirate Queen episode graph (SWE13 pp. 11-92).
All prose is new concise gameplay copy; no passages copied from the book.
"""
from pathlib import Path
import json,re
p=Path('app/src/main/assets/vtt/js/adventure/maskOfThePirateQueen.mjs')
s=p.read_text()
# scene helper: skill checks are prompts to GM, not fabricated auto-successes
nodes={}
def node(id, ep, title, loc, summary, npc=[], checks=[], choices=[], page=''):
    nodes[id]={'episode':ep,'title':title,'locationId':f'LOC.{loc}','summary':summary,
               'narration':summary,'visibleNpcIds':[f'NPC.{x}' for x in npc],
               'checks':[{'skill':a,'difficulty':b,'purpose':c} for a,b,c in checks],
               'choices':[{'id':a,'label':b,'to':c,**({'effects':d} if d else {})} for a,b,c,*e in choices for d in [e[0] if e else {}]],
               'sourceSection':page}
def ch(id,label,to,flags=None):return (id,label,to,flags or {})
node('saleucami-contract-offer',1,'Contrato da Irmandade Oculta','saleucamiPort','O grupo recebe um contato seguro de Venlana Sipal, do Consórcio Zann, propondo uma caçada à Rainha Pirata.', ['venlana'], [('Negociação','Média','esclarecer condições do adiantamento e a intermediação do contrato')], [ch('accept-venlana','Aceitar conversar em Saleucami','taleucema-arrival',{'contractAccepted':True})], 'Episode I, pp. 16-17')
node('taleucema-arrival',1,'Desembarque em Taleucema','saleucamiPort','A capital craterizada de Saleucami recebe a tripulação; a reunião com Venlana ocorrerá na cantina Paradise.',[],[('Manha','Média','avaliar os riscos de seguir os representantes do Consórcio')],[ch('meet-at-paradise','Encontrar Venlana na Paradise','paradise-briefing')],'Episode I, pp. 17-19')
node('paradise-briefing',1,'Reunião na Paradise','paradise','Venlana apresenta o alvo, os ataques contra o Consórcio e as primeiras pistas de investigação em Saleucami.',['venlana'],[('Percepção','Média','avaliar a postura e os interesses da contratante')],[ch('follow-saleucami-leads','Investigar as pistas oferecidas','saleucami-leads')],'Episode I, pp. 18-20')
node('saleucami-leads',1,'Três pistas em Saleucami','saleucamiPort','As informações apontam para um ex-pirata, um receptador de carga e uma ligação com os arredores de Blackwind Crater.',[],[('Manha','Média','identificar intermediários e rotas de mercadorias desviadas')],[ch('question-graf','Procurar o ex-pirata Graf','graf-exile'),ch('question-krezo','Procurar o receptador Krezo','krezo-fence')],'Episode I, pp. 20-28')
node('graf-exile',1,'O exilado Graf','saleucamiPort','Graf afirma conhecer a Irmandade, mas sua intenção de voltar às fileiras piratas exige cautela.',['graf'],[('Disciplina','Difícil','avaliar a sinceridade do ex-pirata')],[ch('visit-krezo-after-graf','Seguir a pista de mercadorias','krezo-fence',{'grafInterviewed':True})],'Episode I, pp. 20-24')
node('krezo-fence',1,'O receptador Krezo Wasanti','krezoShop','A loja de Krezo guarda mercadorias de procedência suspeita; uma compradora chamada Mandi liga os negócios à região de Blackwind Crater.',['krezo'],[('Coerção','Média','obter informação sobre a remessa e a intermediária'),('Percepção','Difícil','localizar bens ocultos na loja')],[ch('trace-blackwind','Seguir as pistas até Blackwind Crater','scrap-zone-investigation',{'blackwindLead':True})],'Episode I, pp. 24-28')
node('scrap-zone-investigation',1,'Blackwind Crater','blackwind','O grupo atravessa o terreno acidentado de Saleucami em direção à instalação secreta conhecida como Vault.',[],[('Sobrevivência','Difícil','alcançar o acesso sem alertar patrulhas')],[ch('enter-the-vault','Preparar a incursão ao Vault','veiled-stronghold')],'Episode I, pp. 28-31')
node('veiled-stronghold',1,'A incursão ao Vault','vault','A base da Irmandade exige planejamento, infiltração e superação de guardas antes do encontro com a figura mascarada.', ['revah'],[('Furtividade','Difícil','atravessar a segurança do complexo')],[ch('confront-decoy','Chegar até a figura mascarada','revah-decoy')],'Episode I, pp. 31-36')
node('revah-decoy',1,'Revah, a Rainha aparente','vault','O alvo encontrado é Revah, uma impostora usada como Rainha Pirata em Saleucami; sua captura não encerra a ameaça.', ['revah'],[('Vigilância','Média','observar indícios de identidade falsa')],[ch('deliver-revah','Levar o resultado da incursão ao Consórcio','venlana-return',{'revahLocated':True})],'Episode I, pp. 35-37')
node('venlana-return',1,'Recompensa prematura','paradise','O retorno ao Consórcio revela que os ataques continuam: a verdadeira Rainha opera a partir de Ord Mantell.', ['venlana'],[('Computadores','Média','analisar a origem da nova transmissão')],[ch('travel-ord-mantell','Seguir o rastro até Ord Mantell','ord-mantell-arrival',{'decoyExposed':True})],'Episode I conclusion / Episode II opening, pp. 37-40')
node('ord-mantell-arrival',2,'Chegada a Worlport, Ord Mantell','worlport','O rastro da transmissão leva a Worlport, onde o contato Jander e o circuito de apostas de Illo Vandin oferecem novas pistas.', ['jander'],[('Manha','Média','localizar o contato do Consórcio sem revelar a investigação')],[ch('meet-jander','Encontrar Jander em Worlport','jander-contact')],'Episode II, pp. 38-49')
node('jander-contact',2,'O contato Jander','worlport','Jander orienta a tripulação a procurar Illo Vandin, um poderoso apostador e criminoso local.',['jander'],[('Negociação','Média','obter uma apresentação ao círculo de Vandin')],[ch('visit-vandin','Procurar Illo Vandin','vandin-casino')],'Episode II, pp. 48-52')
node('vandin-casino',2,'A barganha de Illo Vandin','vandinCasino','Illo Vandin aceita fornecer uma pista decisiva somente após o grupo cumprir um dos dois serviços que exige.',['illo'],[('Negociação','Difícil','negociar os termos de um favor')],[ch('fix-the-fight','Investigar o esquema de lutas','fight-fixing'),ch('reach-rusty','Seguir a missão de Rusty Reach','rusty-reach')],'Episode II, pp. 50-59')
node('fight-fixing',2,'Manipulação das lutas','vandinCasino','A primeira opção de Vandin envolve um circuito de lutas e apostas; a decisão do grupo deixa uma dívida com o criminoso.',['illo'],[('Enganação','Difícil','lidar com os interesses de apostadores e lutadores')],[ch('collect-vandin-favor-fight','Cobrar a informação prometida','consort-lead',{'vandinFavor':'fight-fixing'})],'Episode II, pp. 53-58')
node('rusty-reach',2,'Rusty Reach','rustyReach','A segunda opção leva a um assentamento isolado nas terras de sucata, com riscos ambientais e guardas.',['illo'],[('Sobrevivência','Difícil','chegar ao local com recursos suficientes')],[ch('collect-vandin-favor-reach','Voltar com o favor concluído','consort-lead',{'vandinFavor':'rusty-reach'})],'Episode II, pp. 59-64')
node('consort-lead',2,'A pista do consorte','worlport','A informação de Vandin permite localizar um aliado íntimo da Rainha e demonstrar que ela o está enganando.',[],[('Persuasão','Difícil','convencer o consorte a revelar o acesso ao esconderijo')],[ch('enter-queen-palace','Localizar o palácio subterrâneo','palace-infiltration')],'Episode II, pp. 64-67')
node('palace-infiltration',2,'Palácio subterrâneo de Worlport','queenPalace','A equipe invade ou infiltra o palácio secreto da Irmandade, protegido por seus seguidores e por Ryale Wei.',['ryale'],[('Furtividade','Difícil','alcançar o centro do palácio sem ser cercado')],[ch('face-ryale','Confrontar Ryale Wei','ryale-revelation')],'Episode II, pp. 66-71')
node('ryale-revelation',2,'O segredo da máscara','queenPalace','Ryale Wei revela a natureza sucessória do título de Rainha Pirata e o paradeiro da nave Renegade’s Blood.',['ryale'],[('Coerção','Difícil','obter informações sobre a frota sem destruir possíveis alianças')],[ch('pursue-renegades-blood','Preparar interceptação da nau-capitânia','fleet-intercept',{'trueQueenIdentified':True})],'Episode II, pp. 66-71')
node('fleet-intercept',3,'Preparar a interceptação','spaceBattle','O grupo planeja como localizar a Renegade’s Blood em meio a uma batalha com as forças do Consórcio Zann.',['sykes'],[('Astrogação','Difícil','traçar uma aproximação à nave pirata')],[ch('approach-flagship','Aproximar-se da nau-capitânia','flagship-systems')],'Episode III, pp. 72-75')
node('flagship-systems',3,'Defesas e comunicações','spaceBattle','Desabilitar defesas e comunicações da nau pode tornar o embarque menos arriscado; a batalha continua ao redor.',[],[('Artilharia','Difícil','atingir subsistemas em vez de destruir o casco')],[ch('board-the-ship','Executar abordagem da nave','renegades-blood-boarding')],'Episode III, pp. 75-84')
node('renegades-blood-boarding',3,'Abordagem à Renegade’s Blood','shipCorridors','A tripulação atravessa corredores avariados e a resistência da Irmandade para chegar à ponte.',[],[('Coordenação','Difícil','superar os perigos da nave danificada')],[ch('reach-queen-bridge','Alcançar a ponte de comando','flagship-showdown')],'Episode III, pp. 84-90')
node('flagship-showdown',3,'Noira, a verdadeira Rainha Pirata','flagshipBridge','A Rainha Noira oferece uma alternativa à captura: aliança com a Irmandade ou confronto pelo controle do navio.',['noira'],[('Frieza','Difícil','resistir à pressão e avaliar a proposta da Rainha')],[ch('capture-noira','Concluir a caçada pelo contrato','mpq-epilogue',{'queenCaptured':True,'resolutionMode':'capture'}),ch('join-sorority','Aceitar uma aliança com a Irmandade','mpq-epilogue',{'queenAlliance':True,'resolutionMode':'alliance'})],'Episode III, pp. 90-92')
node('mpq-epilogue',3,'O destino da máscara','worlport','O resultado do confronto define a relação futura do grupo com o Consórcio Zann e com a Irmandade Oculta.',[],[],[],'Episode III conclusion, pp. 92-93')
# Serialize with JS identifiers instead of quoted literals
lines=[]
for id, n in nodes.items():
    loc=n.pop('locationId'); npcs=n.pop('visibleNpcIds')
    body=json.dumps(n,ensure_ascii=False,indent=6)
    body=body.replace('"episode":','episode:').replace('"title":','title:').replace('"summary":','summary:').replace('"narration":','narration:').replace('"checks":','checks:').replace('"choices":','choices:').replace('"sourceSection":','sourceSection:')
    body=body.replace('"visibleNpcIds":','visibleNpcIds:').replace('"locationId":','locationId:')
    body=body.replace('"id":','id:').replace('"label":','label:').replace('"to":','to:').replace('"effects":','effects:')
    body=body.replace('"skill":','skill:').replace('"difficulty":','difficulty:').replace('"purpose":','purpose:')
    body=body.replace('"contractAccepted":','contractAccepted:').replace('"grafInterviewed":','grafInterviewed:').replace('"blackwindLead":','blackwindLead:').replace('"revahLocated":','revahLocated:').replace('"decoyExposed":','decoyExposed:').replace('"vandinFavor":','vandinFavor:').replace('"trueQueenIdentified":','trueQueenIdentified:').replace('"queenCaptured":','queenCaptured:').replace('"resolutionMode":','resolutionMode:').replace('"queenAlliance":','queenAlliance:')
    body=body.replace('"checks":','checks:')
    body=body[:-2]+f',\n      locationId: {loc},\n      visibleNpcIds: [{", ".join(npcs)}]\n    }}'
    lines.append(f'    {json.dumps(id)}: {body}')
node_source=',\n'.join(lines)
# Replace source constants and authored nodes only
start=s.index('const LOC = {');end=s.index('\nfunction clone',start)
s=s[:start]+'''const LOC = {
  saleucamiPort:'mpq.loc.taleucema',paradise:'mpq.loc.paradise',krezoShop:'mpq.loc.krezo_shop',
  blackwind:'mpq.loc.blackwind_crater',vault:'mpq.loc.vault',worlport:'mpq.loc.worlport',
  vandinCasino:'mpq.loc.vandin_casino',rustyReach:'mpq.loc.rusty_reach',
  queenPalace:'mpq.loc.queen_palace',spaceBattle:'mpq.loc.renegades_blood_orbit',
  shipCorridors:'mpq.loc.renegades_blood_corridors',flagshipBridge:'mpq.loc.flagship_bridge'
};
const NPC = {
  venlana:'mpq.npc.venlana_sipal',graf:'mpq.npc.graf',krezo:'mpq.npc.krezo_wasanti',
  revah:'mpq.npc.revah',jander:'mpq.npc.jander',illo:'mpq.npc.illo_vandin',
  ryale:'mpq.npc.ryale_wei',noira:'mpq.npc.noira',sykes:'mpq.npc.jerid_sykes'
};
const FACTION = { veiledSorority:'mpq.fac.veiled_sorority',zannConsortium:'mpq.fac.zann_consortium' };
''' +s[end:]
s=s.replace("startNodeId: 'ord-mantell-arrival'", "startNodeId: 'saleucami-contract-offer'")
s=s.replace('  schemaVersion: 1,','  schemaVersion: 2,',1)
start=s.index('  nodes: {');end=s.index('\n});\n\nfunction conditionOk',start)
s=s[:start]+'  nodes: {\n'+node_source+'\n  }'+s[end:]
# ensure no trailing syntax broken; writes
p.write_text(s)
print('Wrote',len(nodes),'source-anchored nodes, size',len(s))
