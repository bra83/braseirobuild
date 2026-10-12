#!/usr/bin/env python3
"""Replace the invented chapter-nine plot with primary-book Candlemere content.

Old identities are retired, not reused as proof of completing different adventures.
This is a scoped correction, not certification of all encounters in the campaign.
"""
import json
from pathlib import Path
ASSETS=Path('android/app/src/main/assets/pathfinder/kingmaker/chapters')
def quest(key,title,objective,steps,page,xp=0,prerequisites=()):
 return {'quest_key':key,'questKey':key,'title':title,'status':'unstarted','stage':'pending',
  'objective':objective,'stakes':'Impedir que o culto de Yog-Sothoth retome sua ameaça a partir de Candlemere.',
  'owner_character_id':0,'faction_id':0,'milestone_xp':xp,'milestoneXp':xp,
  'prerequisites':list(prerequisites),'objectives':[{'id':key+'_step_'+str(i+1),'desc':s,'description':s,'status':'pending','completed':False} for i,s in enumerate(steps)],
  'rewards':{'gold':0,'xp':xp,'items':[]},'progress_json':{'source_ref':'Kingmaker pp.'+page,'official_chapter_number':9,'definition_revision':'candlemere-r36-primary'}}
def corrected_chapter(old):
 d=dict(old)
 d.update(title='Eles Espreitam Abaixo — Candlemere',level_min=16,levelMin=16,level_max=17,levelMax=17,
  summary='Sob Candlemere, os Pharasmins mortos-vivos liderados por Korog mantêm um impasse com o culto dos Thresholders. Foras prepara a chegada de Yog-Sothoth. O nível disputado e a Espiral de Yog-Sothoth formam as duas partes deste capítulo. O Reino permanece em segundo plano. Derrotar o culto interrompe sua tentativa; neutralizar a Espiral definitivamente exige o Teorema da Hélice.')
 d['quests']=[
  quest('q_candlemere_spiral_seal','O Selo Espiral de Candlemere','Investigar e remover o Selo Espiral para abrir a descida ao nível disputado.',
   ['Pesquisar o selo; as notas de Irovetti concedem +4 de item aos testes de pesquisa.',
    'Remover o selo com Ocultismo mestre CD 37 ou Religião mestre CD 40 em 10 minutos; dissipar magia de 9º nível usa CD de contramágica 40.',
    'Conduzir a descida de 30 metros até A1; Escalar exige CD 30.'], '399-400',60),
  quest('q_candlemere_contested','O Limiar do Pavor','Explorar o nível disputado de Candlemere e compreender o conflito entre Korog, os Pharasmins e os Thresholders.',
   ['Alcançar o nível disputado pela abertura do selo ou pela entrada onírica descrita no capítulo.',
    'Investigar os defensores de Pharasma, as armadilhas e as passagens que separam os dois lados.',
    'Examinar o quadro de A9 para descobrir a existência do Teorema da Hélice e alcançar a Espiral inferior.'], '400-411'),
  quest('q_candlemere_helix','O Teorema da Hélice','Neutralizar permanentemente a Espiral de Yog-Sothoth com o ritual da Hélice.',
   ['Obter as instruções completas em B13 e o portão em miniatura de B14, ou preparar um ícone com Manufatura CD 38.',
    'Fechar o ícone, com Ocultismo CD 36 a cada etapa, em B14, B13, B12, B10, B8, B4, B3, B2 e B1, nesta ordem. Falhar ou errar a ordem reinicia em B14.',
    'Depositar o ícone ainda fechado na alcova de B1. Registrar os personagens presentes em Candlemere para a proteção temporal posterior.'], '413',120,('q_candlemere_contested',)),
  quest('q_candlemere_foras','Foras e os Thresholders','Impedir Foras e seu culto de trazer Yog-Sothoth ao Plano Material.',
   ['Explorar a Espiral inferior e enfrentar ou contornar os perigos e aliados de Leng.',
    'Confrontar Foras em B14 e interromper a tentativa do culto.',
    'Distinguir a derrota do culto da neutralização permanente da Espiral; o ritual é uma resolução adicional, não um pré-requisito obrigatório para enfrentar Foras.'], '412-421',0,('q_candlemere_contested',)),
  quest('q_candlemere_absolution','O Destino dos Pharasmins','Resolver o destino dos Pharasmins após a queda do culto, com absolvição opcional.',
   ['Após a derrota de Foras, os Pharasmins mortos-vivos se desfazem e suas almas seguem para julgamento.',
    'Se houver um devoto de Pharasma, considerar uma absolvição com consagrar ou ritual semelhante. A bênção narrativa é opcional do Mestre e não concede imortalidade.'], '421',0,('q_candlemere_foras',))]
 d['hazards']=[{'hazard_key':'hazard_candlemere_praise','hazardKey':'hazard_candlemere_praise','name':'Louvor a Yog-Sothoth','level':19,
  'traits':['complexo','magico','armadilha'],'stealth_dc':47,'perception_dc':47,'saving_throw_type':'Fortitude','saving_throw_dc':41,
  'disable_skills':[{'skill':'Ocultismo','dc':47,'proficiency':'mestre','description':'Três sucessos seguidos ao recitar as frases; desativa por 1 hora.'},{'skill':'Religião','dc':49,'proficiency':'mestre','description':'Três sucessos seguidos; dissipar magia de 9º nível usa CD de contramágica 38.'}],
  'hazard_type':'magico','complexity':'complexo','routine_actions':1,'xp':0,'physical_defenses_published':False,'source_ref':'Kingmaker p.415',
  'description':'Armadilha de B4: permanecer mais de uma rodada sem recitar as frases em Aklo provoca rajadas de frio.',
  'reaction':'12d6 de frio, Fortitude básica CD 41; falha crítica também imobiliza por 1 minuto. Em seguida rola iniciativa.',
  'routine':[{'action_cost':1,'name':'Rajada de frio','description':'Todas as criaturas em B4 sofrem 12d6 de frio, Fortitude básica CD 41; falha crítica imobiliza por 1 minuto.'}],
  'reset':'Desativa uma rodada após ficar sem alvos e rearma após 1 minuto.'}]
 d['pois']=[];d['scenes']=[]
 d['npcs']=[{'npc_key':'npc_foras','stable_id':'npc_foras','name':'Foras','summary':'Feiticeiro humano e líder dos Thresholders sob Candlemere.','goals':'Trazer Yog-Sothoth ao Plano Material.','secrets':'Seu irmão Korog liderou a oposição dos Pharasmins.','attitude':'hostil','last_seen':'Candlemere B14','faction':'Thresholders','is_companion':False,
  'stats':{'level':19,'hp':355,'max_hp':355,'ac':43,'perception':33,'fortitude':31,'reflex':31,'will':35,'speed':25,'spell_dc':41,'spell_attack':36,'mental_resistance':20,
   'strength':0,'dexterity':4,'constitution':2,'intelligence':5,'wisdom':4,'charisma':6,
   'staff_attack':31,'staff_damage':'3d4+10','staff_damage_type':'bludgeoning','quickened_actions':['step','stride','sustain_spell'],'resolve':'Will success becomes critical success',
   'time_siphon':{'range_feet':60,'will_dc':38,'failure':'slowed 1 for 1 round','critical_failure':'slowed 2 for 1 round','immune':'Yog-Sothoth worshippers'},'source_ref':'Kingmaker p.595'}}]
 d['factions']=[{'name':'Thresholders','agenda':'Concluir a chegada de Yog-Sothoth.','resources':'Cultistas e aliados de Leng.','relationship':-5,'current_plan':'Trabalhar na Espiral sob Candlemere.','location':'Candlemere','active':True},
  {'name':'Pharasmins de Korog','agenda':'Manter os Thresholders separados da superfície.','resources':'Defensores mortos-vivos e antigas proteções.','relationship':0,'current_plan':'Preservar o impasse no nível disputado.','location':'Candlemere','active':True}]
 d['clocks']=[];d['events']=[]
 d['milestones']=[{'milestone_key':'candlemere_spiral_seal','name':'Romper o Selo Espiral','xp':60,'source_ref':'Kingmaker p.400'},
  {'milestone_key':'candlemere_helix','name':'Concluir o Teorema da Hélice','xp':120,'source_ref':'Kingmaker p.413'}]
 metadata=dict(d.get('metadata',{}));metadata.update(official_chapter_number=9,source_ref='Kingmaker pp.396-421',definition_revision='candlemere-r36-primary',
  parts=[{'key':'threshold_of_dread','title':'O Limiar do Pavor','page':398,'minimum_level':16},{'key':'depth_of_fear','title':'A Profundidade do Medo','page':412,'minimum_level':16}],
  retirement={'quests':[{'key':q.get('quest_key'),'title':q.get('title')} for q in old.get('quests',[])],
   'world_clocks':[x.get('name') for x in old.get('clocks',[])],'world_events':[x.get('title') for x in old.get('events',[])],'npc_memory_profiles':[x.get('name') for x in old.get('npcs',[])],
   'campaign_pois':[x.get('name') for x in old.get('pois',[])],'tactical_scenes':[x.get('scene_key') for x in old.get('scenes',[])]},
  coverage={'plot_correction':True,'all_encounters_compiled':False,'all_maps_compiled':False})
 d['metadata']=metadata
 return d
def apply_campaign(root):
 directory=Path(root)/ASSETS
 p=directory/'chapter_08_they_lurk_below.json';d=json.loads(p.read_text(encoding='utf-8'))
 if d.get('metadata',{}).get('definition_revision')=='candlemere-r36-primary':return
 expected={'q_terras_sombrias','q_brecha_sekamina','q_ancoras_abissais','q_aberracoes_subterraneas','q_terremoto_da_capital'}
 if {q.get('quest_key') for q in d['quests']}!=expected:raise ValueError('Unexpected chapter-nine draft identities')
 changes={p:corrected_chapter(d)}
 # The printed starting levels supersede draft ranges; adjacent chapters stay continuous.
 for filename,low,high in [('chapter_06_blood_for_blood.json',10,13),('chapter_07_war_of_river_kings.json',13,16),('chapter_09_sound_thousand_screams.json',17,20)]:
  target=directory/filename;x=json.loads(target.read_text(encoding='utf-8'))
  x.update(level_min=low,levelMin=low,level_max=high,levelMax=high);changes[target]=x
 for target,value in changes.items():target.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('R36_CANDLEMERE_CANONICAL_PLOT_APPLIED')
