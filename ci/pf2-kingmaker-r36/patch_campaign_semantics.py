#!/usr/bin/env python3
"""Replace the invented chapter-nine plot with primary-book Candlemere content,
and enrich all 11 official chapters (40 official parts) and secondary companion hooks.

Old identities are retired, not reused as proof of completing different adventures.
"""
import json
from pathlib import Path

ASSETS = Path('android/app/src/main/assets/pathfinder/kingmaker/chapters')


def quest(key, title, objective, steps, page, xp=0, prerequisites=(), extra_progress=None):
    prog = {
        'source_ref': 'Kingmaker pp.' + page,
        'official_chapter_number': 9,
        'definition_revision': 'candlemere-r36-primary',
    }
    if extra_progress:
        prog.update(extra_progress)
    return {
        'quest_key': key,
        'questKey': key,
        'title': title,
        'status': 'unstarted',
        'stage': 'pending',
        'objective': objective,
        'stakes': 'Impedir que o culto de Yog-Sothoth retome sua ameaça a partir de Candlemere.',
        'owner_character_id': 0,
        'faction_id': 0,
        'milestone_xp': xp,
        'milestoneXp': xp,
        'prerequisites': list(prerequisites),
        'objectives': [
            {
                'id': key + '_step_' + str(i + 1),
                'desc': s,
                'description': s,
                'status': 'pending',
                'completed': False,
            }
            for i, s in enumerate(steps)
        ],
        'rewards': {'gold': 0, 'xp': xp, 'items': []},
        'progress_json': prog,
    }


def _npc(key, name, level, hp, ac, perc, fort, ref, will, summary, goals, secrets, attitude, last_seen, faction, page, extra_stats=None):
    stats = {
        'level': level,
        'hp': hp,
        'max_hp': hp,
        'ac': ac,
        'perception': perc,
        'fortitude': fort,
        'reflex': ref,
        'will': will,
        'speed': 25,
        'source_ref': 'Kingmaker p.' + str(page),
    }
    if extra_stats:
        stats.update(extra_stats)
    return {
        'npc_key': key,
        'stable_id': key,
        'name': name,
        'summary': summary,
        'goals': goals,
        'secrets': secrets,
        'attitude': attitude,
        'last_seen': last_seen,
        'hex_id': 'H42',
        'faction': faction,
        'is_companion': False,
        'stats': stats,
    }


OFFICIAL_PARTS_BY_ASSET = {
    'chapter_00_prologue_swordlords.json': [
        {
            'part': 1,
            'official_name': "The Swordlord's Feast",
            'title_pt': 'O Banquete da Senhora das Espadas',
            'page_start': 16,
            'page_end_inclusive': 23,
            'scenes': ['Salão de Banquetes da Mansão Aldori'],
            'npcs': ['Jamandi Aldori', 'Amiri', 'Harrim', 'Jaethal', 'Linzi', 'Tartuccio', 'Valerie', 'Maegar Varn'],
            'skill_checks': [{'topic': 'Influenciar Convidados no Banquete', 'dc': 15, 'skills': ['Diplomacia', 'Sociedade', 'Enganação', 'Intimidação']}],
            'consequences': 'Estabelece afinidades iniciais de influência com os companheiros antes do ataque dos Lágrimas Negras.',
        },
        {
            'part': 2,
            'official_name': 'Blood and Blades',
            'title_pt': 'Sangue e Lâminas',
            'page_start': 24,
            'page_end_inclusive': 31,
            'scenes': ['Ala Norte da Mansão Aldori (A1–A14)'],
            'npcs': ['Lacaio das Lágrimas Negras', 'Gromog', 'Jurgrindor'],
            'skill_checks': [{'topic': 'Compartimento oculto da estátua de Milani em A4', 'dc': 18, 'skills': ['Percepção']}, {'topic': 'Fechadura do baú de Jamandi em A7', 'dc': 20, 'skills': ['Ladroagem']}],
            'consequences': 'Recuperação ou devolução das 800 PO do baú de A7 define a confiança de Jamandi Aldori.',
        },
        {
            'part': 3,
            'official_name': 'Into the Fire',
            'title_pt': 'No Meio do Fogo',
            'page_start': 32,
            'page_end_inclusive': 41,
            'scenes': ['Ala Sul em Chamas e Salão Principal (A15–A23)'],
            'npcs': ['Nishkiv, a Faca', 'Volodmyra'],
            'skill_checks': [{'topic': 'Combater o Incêndio nos Quartos A16/A17', 'dc': 15, 'skills': ['Atletismo', 'Natureza', 'Sobrevivência']}, {'topic': 'Resistir à Fumaça', 'dc': 18, 'skills': ['Fortitude']}],
            'consequences': 'Derrotar Volodmyra garante a Carta de Concessão para explorar as Terras Roubadas.',
        },
    ],
    'chapter_01_stolen_lands_part1.json': [
        {
            'part': 1,
            'official_name': "The Swordlord's Feast",
            'title_pt': 'O Banquete da Senhora das Espadas (Transição para os Ermos)',
            'page_start': 16,
            'page_end_inclusive': 23,
            'scenes': ['Restov e Estrada para o Entreposto de Oleg'],
            'npcs': ['Jamandi Aldori', 'Oleg Leveton', 'Svetlana Leveton'],
            'skill_checks': [{'topic': 'Diplomacia com Jamandi e Oleg', 'dc': 15, 'skills': ['Diplomacia']}],
            'consequences': 'Inicia a expedição oficial pelo Cinturão Verde.',
        },
        {
            'part': 2,
            'official_name': 'Blood and Blades',
            'title_pt': 'Sangue e Lâminas (Consequências do Ataque em Restov)',
            'page_start': 24,
            'page_end_inclusive': 31,
            'scenes': ['Mansão Aldori A1–A14'],
            'npcs': ['Gromog', 'Jurgrindor'],
            'skill_checks': [{'topic': 'Rastrear mandantes dos Lágrimas Negras', 'dc': 16, 'skills': ['Sociedade', 'Percepção']}],
            'consequences': 'Revela o envolvimento de Pitax e dos bandidos do Cinturão Verde.',
        },
        {
            'part': 3,
            'official_name': 'Into the Fire',
            'title_pt': 'No Meio do Fogo (Partida de Restov)',
            'page_start': 32,
            'page_end_inclusive': 41,
            'scenes': ['Mansão Aldori A15–A23'],
            'npcs': ['Volodmyra', 'Nishkiv'],
            'skill_checks': [{'topic': 'Interrogatório dos sobreviventes', 'dc': 17, 'skills': ['Intimidação', 'Diplomacia']}],
            'consequences': 'Concede 30 XP por quarto salvo do incêndio e confirma a Carta de Concessão.',
        },
        {
            'part': 4,
            'official_name': 'Hexploring the Stolen Lands',
            'title_pt': 'Hexplorando as Terras Roubadas',
            'page_start': 44,
            'page_end_inclusive': 49,
            'scenes': ['Mapa Hexagonal das Terras Roubadas'],
            'npcs': ['Bokken', 'Tartuccio'],
            'skill_checks': [{'topic': 'Reconhecimento e Mapeamento de Hexágono', 'dc': 15, 'skills': ['Sobrevivência', 'Natureza']}],
            'consequences': 'Mapeamento de hexágonos concede XP de marco e prepara a futura fundação do Reino.',
        },
        {
            'part': 5,
            'official_name': 'Hex Encounters',
            'title_pt': 'Encontros de Hexágono',
            'page_start': 50,
            'page_end_inclusive': 119,
            'scenes': ['Clareiras, Ruínas, Pontes e Covis das Terras Roubadas'],
            'npcs': ['Tuskgutter', 'Tartuk', 'Chefe Sootscale', 'Davik Nettles'],
            'skill_checks': [{'topic': 'Rastrear Tuskgutter e Locais Ocultos', 'dc': 16, 'skills': ['Sobrevivência', 'Percepção']}],
            'consequences': 'Alianças com kobolds/mitflits e eliminação de ameaças regionais.',
        },
        {
            'part': 6,
            'official_name': 'The Old Sycamore',
            'title_pt': 'O Velho Sicômoro',
            'page_start': 120,
            'page_end_inclusive': 125,
            'scenes': ['Raízes e Câmaras Subterrâneas do Velho Sicômoro'],
            'npcs': ['Rei Grunnck', 'Tartuk'],
            'skill_checks': [{'topic': 'Navegar pelas galerias de raízes e fendas', 'dc': 16, 'skills': ['Acrobacia', 'Atletismo']}],
            'consequences': 'Resolução da guerra entre mitflits e kobolds Sootscale.',
        },
        {
            'part': 7,
            'official_name': 'Sootscale Caverns',
            'title_pt': 'Cavernas Sootscale',
            'page_start': 126,
            'page_end_inclusive': 131,
            'scenes': ['Complexo de Cavernas da Tribo Sootscale'],
            'npcs': ['Chefe Sootscale', 'Tartuk', 'Nok-Nok'],
            'skill_checks': [{'topic': 'Desmascarar a maldição forjada por Tartuk', 'dc': 18, 'skills': ['Enganação', 'Diplomacia', 'Religião']}],
            'consequences': 'Aliança pacífica ou subjugação da tribo Sootscale.',
        },
        {
            'part': 8,
            'official_name': "Trouble at Oleg's",
            'title_pt': 'Problemas no Entreposto de Oleg',
            'page_start': 162,
            'page_end_inclusive': 167,
            'scenes': ['Entreposto Comercial de Oleg (Pátio e Empaliçada)'],
            'npcs': ['Oleg Leveton', 'Svetlana Leveton', 'Happs Bydon', 'Kesten Garess'],
            'skill_checks': [{'topic': 'Preparar emboscada e defesas no Entreposto', 'dc': 15, 'skills': ['Furtividade', 'Enganação', 'Manufatura']}],
            'consequences': 'Liberta o entreposto da cobrança de tributos dos bandidos e estabelece a primeira base.',
        },
        {
            'part': 9,
            'official_name': 'Bandits of the Greenbelt',
            'title_pt': 'Bandidos do Cinturão Verde',
            'page_start': 168,
            'page_end_inclusive': 171,
            'scenes': ['Acampamento do Rio Thorn'],
            'npcs': ['Kressle', 'Falchos'],
            'skill_checks': [{'topic': 'Infiltrar-se no acampamento de Kressle', 'dc': 17, 'skills': ['Furtividade', 'Percepção']}],
            'consequences': 'Obtém a senha e localização da fortaleza do Senhor dos Cervos e o anel de Svetlana.',
        },
        {
            'part': 10,
            'official_name': 'Against the Stag Lord',
            'title_pt': 'Contra o Senhor dos Cervos',
            'page_start': 172,
            'page_end_inclusive': 183,
            'scenes': ['Fortaleza em Ruínas do Senhor dos Cervos'],
            'npcs': ['Senhor dos Cervos', 'Akiros Ismort', 'Dovan From Nisroch', 'Auchs', 'Nugrah'],
            'skill_checks': [{'topic': 'Convencer Akiros Ismort a mudar de lado', 'dc': 19, 'skills': ['Diplomacia']}],
            'consequences': 'Derrota do Senhor dos Cervos encerra o Capítulo 3 oficial e habilita a fundação do Reino.',
        },
    ],
    'chapter_02_stolen_lands_part2.json': [
        {
            'part': 1,
            'official_name': 'The Lonely Barrow',
            'title_pt': 'O Túmulo Solitário',
            'page_start': 132,
            'page_end_inclusive': 135,
            'scenes': ['Cripta Antiga do Túmulo Solitário'],
            'npcs': ['Guerreiro Esqueleto Campeão'],
            'skill_checks': [{'topic': 'Identificar selos funerários antigos', 'dc': 18, 'skills': ['Religião', 'Sociedade']}],
            'consequences': 'Purificação do túmulo e obtenção de relíquias antigas.',
        },
        {
            'part': 2,
            'official_name': 'Isle of the Lizard King',
            'title_pt': 'Ilha do Rei Lagarto',
            'page_start': 136,
            'page_end_inclusive': 141,
            'scenes': ['Vila Fortificada de Homens-Lagarto nas Terras de Narl'],
            'npcs': ['Rei Vesket', 'Espírito Fogo-Fátuo'],
            'skill_checks': [{'topic': 'Perceber a manipulação do fogo-fátuo sobre Vesket', 'dc': 20, 'skills': ['Percepção', 'Ocultismo']}],
            'consequences': 'Libertação da criança sequestrada e pacificação ou derrota do clã.',
        },
        {
            'part': 3,
            'official_name': 'Candlemere Island',
            'title_pt': 'Ilha de Candlemere (Superfície)',
            'page_start': 142,
            'page_end_inclusive': 147,
            'scenes': ['Ruínas Superficiais da Torre de Candlemere e Urtigas'],
            'npcs': ['Fogos-Fátuos de Candlemere'],
            'skill_checks': [{'topic': 'Examinar as runas de Yog-Sothoth na torre', 'dc': 22, 'skills': ['Ocultismo', 'Arcanismo']}],
            'consequences': 'Descoberta preliminar do Selo Espiral que será aberto no Capítulo 9.',
        },
        {
            'part': 4,
            'official_name': 'The Forgotten Keep',
            'title_pt': 'A Fortaleza Esquecida',
            'page_start': 148,
            'page_end_inclusive': 153,
            'scenes': ['Torre Arruinada nas Montanhas Branthlend'],
            'npcs': ['Guardiões da Fortaleza Esquecida'],
            'skill_checks': [{'topic': 'Desativar armadilhas da fortaleza antiga', 'dc': 24, 'skills': ['Ladroagem']}],
            'consequences': 'Acesso aos salões superiores e tesouros esquecidos.',
        },
        {
            'part': 5,
            'official_name': "M'Botuu",
            'title_pt': "M'Botuu",
            'page_start': 154,
            'page_end_inclusive': 159,
            'scenes': ["Covil Pantanoso de M'Botuu no Pântano Hooktongue"],
            'npcs': ["M'Botuu (Boggard Rei-Sacerdote)"],
            'skill_checks': [{'topic': 'Navegar pelo pântano profundo e totens de Rovagug', 'dc': 25, 'skills': ['Sobrevivência', 'Religião']}],
            'consequences': "Eliminação da ameaça de M'Botuu na fronteira ocidental.",
        },
        {
            'part': 6,
            'official_name': "Against the Stag Lord",
            'title_pt': 'Contra o Senhor dos Cervos (Consolidação da Fronteira)',
            'page_start': 172,
            'page_end_inclusive': 183,
            'scenes': ['Fortaleza do Senhor dos Cervos e Templo do Alce'],
            'npcs': ['Senhor dos Cervos', 'Tristian'],
            'skill_checks': [{'topic': 'Purificar o Templo do Alce de Erastil', 'dc': 18, 'skills': ['Religião', 'Natureza']}],
            'consequences': 'Consolida o domínio sobre o Cinturão Verde.',
        },
    ],
    'chapter_03_rivers_run_red.json': [
        {
            'part': 1,
            'official_name': 'Home Sweet Home',
            'title_pt': 'Lar Doce Lar',
            'page_start': 186,
            'page_end_inclusive': 199,
            'scenes': ['Capital Fundada no Forte do Senhor dos Cervos e Ameaças Cívicas'],
            'npcs': ['Grigori', 'Kundal', 'Octavia', 'Regongar'],
            'skill_checks': [{'topic': 'Debater com o bardo agitador Grigori na praça', 'dc': 22, 'skills': ['Diplomacia', 'Performance', 'Intimidação']}],
            'consequences': 'Fundação formal do Reino e resolução das primeiras crises políticas e de agitação.',
        },
        {
            'part': 2,
            'official_name': 'Troll Trouble',
            'title_pt': 'Problemas com Trolls',
            'page_start': 200,
            'page_end_inclusive': 207,
            'scenes': ['Fortaleza dos Trolls nas Montanhas Tors of Levenies'],
            'npcs': ['Hargulka', 'Nagrundi'],
            'skill_checks': [{'topic': 'Bloquear regeneração ácida/ígnea e táticas de Hargulka', 'dc': 23, 'skills': ['Natureza', 'Atletismo']}],
            'consequences': 'Derrota do reino monstro de Hargulka e recuperação do tesouro saqueado.',
        },
        {
            'part': 3,
            'official_name': 'Hunting the Beast',
            'title_pt': 'Caçando a Fera',
            'page_start': 208,
            'page_end_inclusive': 213,
            'scenes': ['Covil do Owlbear Gigante nas Colinas Kamelands'],
            'npcs': ['O Urso-Coruja Enfurecido', 'Feiticeiro Manipulador'],
            'skill_checks': [{'topic': 'Seguir o rastro de destruição até o covil da Fera', 'dc': 22, 'skills': ['Sobrevivência', 'Natureza']}],
            'consequences': 'Descobre os primeiros indícios da influência feérica de Nyrissa sobre as criaturas.',
        },
    ],
    'chapter_04_cult_of_bloom.json': [
        {
            'part': 1,
            'official_name': 'Seeds of Ruin',
            'title_pt': 'Sementes da Ruína',
            'page_start': 216,
            'page_end_inclusive': 227,
            'scenes': ['Vilarejo de Darivan e Locais de Eclosão das Sementes'],
            'npcs': ['Cultistas da Mãe dos Monstros', 'Jubilost'],
            'skill_checks': [{'topic': 'Diagnosticar a infecção das Sementes da Ruína', 'dc': 24, 'skills': ['Medicina', 'Natureza', 'Religião']}],
            'consequences': 'Identifica a origem mágica do Florescimento antes que consuma os assentamentos.',
        },
        {
            'part': 2,
            'official_name': 'Full Bloom',
            'title_pt': 'Pleno Florescimento',
            'page_start': 228,
            'page_end_inclusive': 237,
            'scenes': ['Capital Sob Ataque de Feras do Primeiro Mundo e Fendas Feéricas'],
            'npcs': ['Hidras e Manticoras do Florescimento'],
            'skill_checks': [{'topic': 'Conter o pânico na capital e fechar fendas menores', 'dc': 26, 'skills': ['Natureza', 'Ocultismo', 'Diplomacia']}],
            'consequences': 'Protege a população e localiza o santuário oculto do culto.',
        },
        {
            'part': 3,
            'official_name': 'The Cradle of Lamashtu',
            'title_pt': 'O Berço de Lamashtu',
            'page_start': 238,
            'page_end_inclusive': 247,
            'scenes': ['Santuário Subterrâneo do Berço de Lamashtu e Câmara da Flor Gigante'],
            'npcs': ['Alta Sacerdotisa do Florescimento', 'A Flor Eterna'],
            'skill_checks': [{'topic': 'Destruir as raízes planares da Flor Eterna', 'dc': 27, 'skills': ['Natureza', 'Religião', 'Atletismo']}],
            'consequences': 'Extingue a praga do Florescimento e preserva a estabilidade do Reino.',
        },
    ],
    'chapter_05_varnhold_vanishing.json': [
        {
            'part': 1,
            'official_name': 'Before the Vanishing',
            'title_pt': 'Antes do Desaparecimento',
            'page_start': 250,
            'page_end_inclusive': 251,
            'scenes': ['Fronteira Oriental e Rota Comercial de Varnhold'],
            'npcs': ['Maegar Varn', 'Cephal Lorentus', 'Willas Gunderson'],
            'skill_checks': [{'topic': 'Investigar o silêncio diplomático de Varnhold', 'dc': 24, 'skills': ['Sociedade', 'Diplomacia']}],
            'consequences': 'Estabelece o vínculo com Maegar Varn antes do sumiço súbito da colônia.',
        },
        {
            'part': 2,
            'official_name': 'The Vanished Village',
            'title_pt': 'O Vilarejo Desaparecido',
            'page_start': 252,
            'page_end_inclusive': 265,
            'scenes': ['Assentamento Deserto de Varnhold'],
            'npcs': ['Invasores Spriggans', 'Casca de Maegar'],
            'skill_checks': [{'topic': 'Reconstruir o rapto em massa pelas pistas nas casas', 'dc': 26, 'skills': ['Percepção', 'Ocultismo', 'Sociedade']}],
            'consequences': 'Descobre a ligação com os centauros Nomen e o Vale dos Mortos.',
        },
        {
            'part': 3,
            'official_name': 'Among the Nomen',
            'title_pt': 'Entre os Nomen',
            'page_start': 266,
            'page_end_inclusive': 269,
            'scenes': ['Acampamento da Tribo Nomen nas Planícies Dunsward'],
            'npcs': ['Aecora Silverfire', 'Xamãs Nomen'],
            'skill_checks': [{'topic': 'Conquistar a confiança de Aecora Silverfire', 'dc': 28, 'skills': ['Diplomacia', 'Natureza']}],
            'consequences': 'Revela a localização exata do Sepulcro de Vordakai no Vale dos Mortos.',
        },
        {
            'part': 4,
            'official_name': "Vordakai's Tomb",
            'title_pt': 'O Sepulcro de Vordakai',
            'page_start': 270,
            'page_end_inclusive': 285,
            'scenes': ['Complexo Ciclópico do Sepulcro de Vordakai e Salão dos Jarros de Almas'],
            'npcs': ['Vordakai (Lich Ciclope)', 'Maegar Varn', 'Horagnamon (Piscodaemon)'],
            'skill_checks': [{'topic': 'Libertar as almas dos colonos nos Jarros de Almas e derrotar o Oculus', 'dc': 30, 'skills': ['Religião', 'Ocultismo', 'Arcanismo']}],
            'consequences': 'Derrota de Vordakai, resgate de Maegar Varn e anexação pacífica de Varnhold ao Reino.',
        },
    ],
    'chapter_06_blood_for_blood.json': [
        {
            'part': 1,
            'official_name': 'Trouble in Tatzlford',
            'title_pt': 'Problemas em Tatzlford',
            'page_start': 288,
            'page_end_inclusive': 299,
            'scenes': ['Assentamento de Tatzlford Sob Cerco'],
            'npcs': ['Bárbaros Senhores dos Tigres', 'Mercenários de Drelev'],
            'skill_checks': [{'topic': 'Coordenar a defesa de Tatzlford e interrogar cativos', 'dc': 28, 'skills': ['Guerra', 'Intimidação', 'Diplomacia']}],
            'consequences': 'Impede a destruição de Tatzlford e revela a traição do Barão Hannis Drelev.',
        },
        {
            'part': 2,
            'official_name': 'Fort Drelev',
            'title_pt': 'Forte Drelev',
            'page_start': 300,
            'page_end_inclusive': 317,
            'scenes': ['Cidade Ocupada e Castelo de Forte Drelev'],
            'npcs': ['Barão Hannis Drelev', 'Irmã Kallatha', 'Kisandra Numesti'],
            'skill_checks': [{'topic': 'Infiltrar-se em Forte Drelev e resgatar os reféns', 'dc': 30, 'skills': ['Furtividade', 'Ladroagem', 'Diplomacia']}],
            'consequences': 'Libertação de Forte Drelev e localização do Túmulo de Armag.',
        },
        {
            'part': 3,
            'official_name': 'The Twice-Born Warlord',
            'title_pt': 'O Senhor da Guerra Duas-Vezes-Nascido',
            'page_start': 318,
            'page_end_inclusive': 329,
            'scenes': ['Túmulo de Armag e Santuário de Gorum'],
            'npcs': ['Armag Duas-Vezes-Nascido', 'Zorek', 'Dugath'],
            'skill_checks': [{'topic': 'Superar as Provas de Gorum no Túmulo de Armag', 'dc': 31, 'skills': ['Atletismo', 'Religião', 'Acrobacia']}],
            'consequences': 'Derrota ou redenção de Armag, neutralização de Ovinrbaane e aliança com os Senhores dos Tigres sob Dugath.',
        },
    ],
    'chapter_07_war_of_river_kings.json': [
        {
            'part': 1,
            'official_name': 'The Rushlight Treachery',
            'title_pt': 'A Traição de Rushlight',
            'page_start': 332,
            'page_end_inclusive': 347,
            'scenes': ['Campos do Torneio de Rushlight'],
            'npcs': ['Rei Castruccio Irovetti', 'Campeões de Pitax'],
            'skill_checks': [{'topic': 'Provas de Justa, Arquearia, Oratória e Alquimia de Rushlight', 'dc': 32, 'skills': ['Atletismo', 'Performance', 'Manufatura', 'Diplomacia']}],
            'consequences': 'Vitória no torneio e sobrevivência à emboscada da meia-noite ordenada por Irovetti.',
        },
        {
            'part': 2,
            'official_name': 'Behind Enemy Lines',
            'title_pt': 'Atrás das Linhas Inimigas',
            'page_start': 348,
            'page_end_inclusive': 353,
            'scenes': ['Território de Pitax e Rotas Fluviais'],
            'npcs': ['Comandantes de Guerra de Pitax'],
            'skill_checks': [{'topic': 'Sabotar linhas de suprimento e contra-propaganda', 'dc': 33, 'skills': ['Furtividade', 'Sociedade', 'Enganação']}],
            'consequences': 'Enfraquece os exércitos de Pitax antes do cerco à capital.',
        },
        {
            'part': 3,
            'official_name': 'The Ghost of Whiterose',
            'title_pt': 'O Fantasma de Rosa Branca',
            'page_start': 354,
            'page_end_inclusive': 365,
            'scenes': ['Abadia de Rosa Branca (Whiterose Abbey)'],
            'npcs': ['Evindra', 'Irmão Gardensen', 'Guardiões de Rosa Branca'],
            'skill_checks': [{'topic': 'Purificar a Abadia e recuperar Briar', 'dc': 34, 'skills': ['Religião', 'Natureza', 'Ocultismo']}],
            'consequences': 'Evindra revela a verdade sobre Nyrissa e entrega a lâmina Briar ao grupo.',
        },
        {
            'part': 4,
            'official_name': 'Infiltration',
            'title_pt': 'Infiltração',
            'page_start': 366,
            'page_end_inclusive': 379,
            'scenes': ['Cidade de Pitax, Academia e Teatro'],
            'npcs': ['Atalia Neven', 'Guardião Vencarlo', 'Estevan Vargas'],
            'skill_checks': [{'topic': 'Mobilizar as facções dissidentes de Pitax contra Irovetti', 'dc': 34, 'skills': ['Diplomacia', 'Sociedade', 'Furtividade']}],
            'consequences': 'Abre caminho seguro até o Palácio Real de Pitax.',
        },
        {
            'part': 5,
            'official_name': "The King's Palace",
            'title_pt': 'O Palácio do Rei',
            'page_start': 380,
            'page_end_inclusive': 395,
            'scenes': ['Palácio Real de Castruccio Irovetti'],
            'npcs': ['Rei Castruccio Irovetti', 'Avinash'],
            'skill_checks': [{'topic': 'Desativar as armadilhas de relógio do palácio e encontrar as notas de Candlemere', 'dc': 35, 'skills': ['Ladroagem', 'Percepção', 'Ocultismo']}],
            'consequences': 'Queda de Irovetti, anexação de Pitax e obtenção das notas de pesquisa sobre o Selo Espiral de Candlemere (+4 de item).',
        },
    ],
    'chapter_08_they_lurk_below.json': [
        {
            'part': 1,
            'official_name': 'Threshold of Dread',
            'title_pt': 'O Limiar do Pavor',
            'page_start': 398,
            'page_end_inclusive': 411,
            'scenes': ['Candlemere — Nível Disputado (A1–A17)'],
            'npcs': ['Korog', 'Kellid Graveknight', 'Morrigna Psychopomp', 'Thresholder Hermeticist', 'Thresholder Disciple'],
            'skill_checks': [{'topic': 'Remover o Selo Espiral', 'dc': 37, 'skills': ['Ocultismo', 'Religião']}, {'topic': 'Decifrar o quadro negro de A9 sobre o Teorema da Hélice', 'dc': 36, 'skills': ['Ocultismo']}],
            'consequences': 'Abre a passagem para a Espiral inferior e revela a existência do Teorema da Hélice.',
        },
        {
            'part': 2,
            'official_name': 'The Depth of Fear',
            'title_pt': 'A Profundidade do Medo',
            'page_start': 412,
            'page_end_inclusive': 421,
            'scenes': ['Candlemere — A Espiral de Yog-Sothoth (B1–B14)'],
            'npcs': ['Foras', 'Kyetriel', 'Niodrhast', 'Leng Envoy', 'Thresholder Mystic', 'Candlemere Shoggoth'],
            'skill_checks': [{'topic': 'Executar o Teorema da Hélice (B14->B13->B12->B10->B8->B4->B3->B2->B1)', 'dc': 36, 'skills': ['Ocultismo', 'Manufatura']}],
            'consequences': 'Derrota de Foras impede a chegada de Yog-Sothoth; concluir o Teorema da Hélice concede 120 XP e +4 de status contra teletransporte/banimento na Casa no Limiar do Tempo.',
        },
    ],
    'chapter_09_sound_thousand_screams.json': [
        {
            'part': 1,
            'official_name': 'A Month of Destruction',
            'title_pt': 'Um Mês de Destruição',
            'page_start': 424,
            'page_end_inclusive': 441,
            'scenes': ['Terras Roubadas Assoladas por Pragas Planares e Tempestades Feéricas'],
            'npcs': ['Arautos de Nyrissa', 'Jabberwock Menor'],
            'skill_checks': [{'topic': 'Deter os treze eventos de destruição do Reino e fechar portais', 'dc': 38, 'skills': ['Natureza', 'Ocultismo', 'Arcanismo']}],
            'consequences': 'Salva o Reino do colapso total e abre o portal para Mil Respiros (Thousandbreaths).',
        },
        {
            'part': 2,
            'official_name': 'A Thousand Breaths',
            'title_pt': 'Mil Respiros',
            'page_start': 442,
            'page_end_inclusive': 455,
            'scenes': ['Floresta Primordial de Thousandbreaths (Mil Respiros) no Primeiro Mundo'],
            'npcs': ['O Príncipe Enrugado', 'Guardiões de Mil Respiros'],
            'skill_checks': [{'topic': 'Navegar pelas distorções espaciais de Mil Respiros', 'dc': 39, 'skills': ['Natureza', 'Sobrevivência', 'Percepção']}],
            'consequences': 'Permite alcançar as portas da Casa no Limiar do Tempo.',
        },
        {
            'part': 3,
            'official_name': 'House at the Edge of Time',
            'title_pt': 'A Casa no Limiar do Tempo',
            'page_start': 456,
            'page_end_inclusive': 477,
            'scenes': ['Labirinto Extradimensional da Casa no Limiar do Tempo e Salão do Trono de Nyrissa'],
            'npcs': ['Nyrissa', 'O Rei Enrugado', 'Irmãs Cegas'],
            'skill_checks': [{'topic': 'Resistir às névoas de teletransporte (bônus de +4 se concluiu o Teorema da Hélice) e decidir o destino de Briar', 'dc': 40, 'skills': ['Vontade', 'Diplomacia', 'Ocultismo']}],
            'consequences': 'Derrota ou redenção de Nyrissa com a entrega de Briar; destrava o Capítulo 11 contra o Rei Lanterna.',
        },
    ],
    'chapter_10_curse_lantern_king.json': [
        {
            'part': 1,
            'official_name': 'A Broken Apology',
            'title_pt': 'Uma Apologia Quebrada',
            'page_start': 480,
            'page_end_inclusive': 483,
            'scenes': ['Cálice de A Apologia (The Apology) e Sonho de Caça (Hunting Dream)'],
            'npcs': ['Avatares do Rei Lanterna', 'Nyrissa (se redimida)'],
            'skill_checks': [{'topic': 'Superar o pesadelo de Hunting Dream e inverter a maldição de A Apologia', 'dc': 42, 'skills': ['Ocultismo', 'Natureza', 'Vontade']}],
            'consequences': 'Reverte a maldição sobre o Reino e abre a travessia para o Reino da Lanterna.',
        },
        {
            'part': 2,
            'official_name': 'The Lantern Kingdom',
            'title_pt': 'O Reino da Lanterna',
            'page_start': 484,
            'page_end_inclusive': 501,
            'scenes': ['Domínio Feérico do Reino da Lanterna (Lantern Kingdom) e Corte do Rei Lanterna'],
            'npcs': ['O Rei Lanterna (The Lantern King)'],
            'skill_checks': [{'topic': 'Confronto mítico final contra as formas do Rei Lanterna', 'dc': 45, 'skills': ['Ocultismo', 'Natureza', 'Religião', 'Arcanismo']}],
            'consequences': 'Derrota definitiva da maldição do Rei Lanterna e consagração eterna do Reino nas Terras Roubadas.',
        },
    ],
    'chapter_11_hex_atlas_pois.json': [
        {
            'part': 1,
            'official_name': 'Hexploring the Stolen Lands',
            'title_pt': 'Hexplorando as Terras Roubadas (Compêndio Geográfico)',
            'page_start': 44,
            'page_end_inclusive': 49,
            'scenes': ['Atlas Hexagonal Completo das Terras Roubadas'],
            'npcs': [],
            'skill_checks': [{'topic': 'Reconhecimento e Reivindicação de Hexágonos', 'dc': 15, 'skills': ['Sobrevivência', 'Natureza']}],
            'consequences': 'Catálogo de referência geográfica das zonas das Terras Roubadas.',
        },
        {
            'part': 2,
            'official_name': 'Hex Encounters',
            'title_pt': 'Encontros de Hexágono (Atlas de Pontos de Interesse)',
            'page_start': 50,
            'page_end_inclusive': 119,
            'scenes': ['Pontos de Interesse do Cinturão Verde, Narlmarches, Kamelands, Hooktongue, Glenebon e Numéria'],
            'npcs': [],
            'skill_checks': [{'topic': 'Descoberta de marcos, ruínas e recursos', 'dc': 18, 'skills': ['Percepção', 'Sobrevivência']}],
            'consequences': 'Suporte de exploração contínua do mapa-múndi.',
        },
    ],
}

SECONDARY_COMPANION_HOOKS_BY_ASSET = {
    'chapter_01_stolen_lands_part1.json': {
        'quest_key': 'q_templo_alce',
        'companion_hooks': [
            {
                'companion': 'Harrim',
                'npc_key': 'npc_harrim',
                'source_ref': 'Kingmaker Companion Guide pp.94-95',
                'hook': 'Harrim acompanha a expedição ao Templo do Alce e às ruínas anãs das Terras Roubadas, meditando sobre o fim inevitável de todas as obras sob o olhar de Groetus (desbloqueia Infectious Ennui com Influência 6 no nível 5 e Proteção Noturna de Acampamento).',
            },
            {
                'companion': 'Jaethal',
                'npc_key': 'npc_jaethal',
                'source_ref': 'Kingmaker Companion Guide pp.96-97',
                'hook': 'Jaethal examina os mortos-vivos e os segredos de Urgathoa durante a exploração do Cinturão Verde e do Túmulo Solitário (desbloqueia runas de arma exclusivas com Influência 8 no nível 7 e investimento automático como Emissária).',
            },
        ],
    },
    'chapter_03_rivers_run_red.json': {
        'quest_key': 'q_fundacao_baronia',
        'companion_hooks': [
            {
                'companion': 'Kalikke and Kanerah',
                'npc_key': 'npc_kalikke_kanerah',
                'source_ref': 'Kingmaker Companion Guide pp.98-101',
                'hook': 'Kalikke and Kanerah estabelecem moradia na nova capital do Reino, compartilhando o vínculo planar de suas almas gêmeas (com Influência 8 permite escolher a irmã ativa nas preparações diárias e abre o gancho de separação de nível 13).',
            },
            {
                'companion': 'Octavia',
                'npc_key': 'npc_octavia',
                'source_ref': 'Kingmaker Companion Guide pp.102-103',
                'hook': 'Octavia liberta-se da Liga Técnica com a ajuda do grupo na nova baronia e apoia o desenvolvimento mágico da capital (desbloqueia Vision of Beauty com Influência 4 no nível 3 e investimento automático como Magistra).',
            },
            {
                'companion': 'Regongar',
                'npc_key': 'npc_regongar',
                'source_ref': 'Kingmaker Companion Guide pp.104-105',
                'hook': 'Regongar rompe as correntes da Liga Técnica ao lado de Octavia e lidera as forças de choque da baronia contra os trolls de Hargulka (desbloqueia Energy-Absorbing Rune com Influência 6 no nível 11 e investimento automático como General).',
            },
        ],
    },
}


def corrected_chapter(old):
    d = dict(old)
    d.update(
        title='Eles Espreitam Abaixo — Candlemere',
        level_min=16,
        levelMin=16,
        level_max=17,
        levelMax=17,
        summary=(
            'Sob Candlemere, os Pharasmins mortos-vivos liderados por Korog mantêm um impasse com o culto dos '
            'Thresholders. Foras prepara a chegada de Yog-Sothoth. O nível disputado (Threshold of Dread) e a '
            'Espiral de Yog-Sothoth (The Depth of Fear) formam as duas partes deste capítulo. O Reino permanece em '
            'segundo plano. Derrotar o culto interrompe sua tentativa; neutralizar a Espiral definitivamente exige '
            'o Teorema da Hélice.'
        ),
    )
    d['quests'] = [
        quest(
            'q_candlemere_spiral_seal',
            'O Selo Espiral de Candlemere',
            'Investigar e remover o Selo Espiral para abrir a descida ao nível disputado (Threshold of Dread).',
            [
                'Pesquisar o selo; as notas de Irovetti concedem +4 de item aos testes de pesquisa.',
                'Remover o selo com Ocultismo mestre CD 37 ou Religião mestre CD 40 em 10 minutos; dissipar magia de 9º nível usa CD de contramágica 40.',
                'Conduzir a descida de 30 metros até A1; Escalar exige CD 30.',
            ],
            '399-400',
            60,
        ),
        quest(
            'q_candlemere_contested',
            'O Limiar do Pavor',
            'Explorar o nível disputado de Candlemere (Threshold of Dread, A1–A17) e compreender o conflito entre Korog, os Pharasmins e os Thresholders.',
            [
                'Alcançar o nível disputado pela abertura do selo ou pela entrada onírica descrita no capítulo.',
                'Investigar os defensores de Pharasma (Korog em A4, Kellid Graveknight, Morrigna Psychopomp), as armadilhas e as passagens que separam os dois lados.',
                'Examinar o quadro de A9 para descobrir a existência do Teorema da Hélice e alcançar a Espiral inferior.',
            ],
            '400-411',
        ),
        quest(
            'q_candlemere_helix',
            'O Teorema da Hélice',
            'Neutralizar permanentemente a Espiral de Yog-Sothoth com o ritual da Hélice.',
            [
                'Obter as instruções completas em B13 e o portão em miniatura de B14, ou preparar um ícone com Manufatura CD 38.',
                'Fechar o ícone, com Ocultismo CD 36 a cada etapa, em B14, B13, B12, B10, B8, B4, B3, B2 e B1, nesta ordem. Falhar ou errar a ordem reinicia em B14.',
                'Depositar o ícone ainda fechado na alcova de B1. Registrar os personagens presentes em Candlemere para a proteção temporal posterior (+4 de status contra teletransporte/banimento na Casa no Limiar do Tempo).',
            ],
            '413',
            120,
            ('q_candlemere_contested',),
        ),
        quest(
            'q_candlemere_foras',
            'Foras e os Thresholders',
            'Impedir Foras e seu culto de trazer Yog-Sothoth ao Plano Material na Profundidade do Medo (The Depth of Fear, B1–B14).',
            [
                'Explorar a Espiral inferior e enfrentar ou contornar os perigos e aliados de Leng (Kyetriel, Niodrhast, Leng Envoy, Candlemere Shoggoth).',
                'Confrontar Foras em B14 e interromper a tentativa do culto.',
                'Distinguir a derrota do culto da neutralização permanente da Espiral; o ritual é uma resolução adicional, não um pré-requisito obrigatório para enfrentar Foras.',
            ],
            '412-421',
            0,
            ('q_candlemere_contested',),
        ),
        quest(
            'q_candlemere_absolution',
            'O Destino dos Pharasmins',
            'Resolver o destino dos Pharasmins após a queda do culto, com absolvição opcional.',
            [
                'Após a derrota de Foras, os Pharasmins mortos-vivos se desfazem e suas almas seguem para julgamento.',
                'Se houver um devoto de Pharasma, considerar uma absolvição com consagrar ou ritual semelhante. A bênção narrativa é opcional do Mestre e não concede imortalidade.',
            ],
            '421',
            0,
            ('q_candlemere_foras',),
        ),
    ]
    d['hazards'] = [
        {
            'hazard_key': 'hazard_candlemere_praise',
            'hazardKey': 'hazard_candlemere_praise',
            'name': 'Louvor a Yog-Sothoth',
            'level': 19,
            'traits': ['complexo', 'magico', 'armadilha'],
            'stealth_dc': 47,
            'perception_dc': 47,
            'saving_throw_type': 'Fortitude',
            'saving_throw_dc': 41,
            'disable_skills': [
                {
                    'skill': 'Ocultismo',
                    'dc': 47,
                    'proficiency': 'mestre',
                    'description': 'Três sucessos seguidos ao recitar as frases; desativa por 1 hora.',
                },
                {
                    'skill': 'Religião',
                    'dc': 49,
                    'proficiency': 'mestre',
                    'description': 'Três sucessos seguidos; dissipar magia de 9º nível usa CD de contramágica 38.',
                },
            ],
            'hazard_type': 'magico',
            'complexity': 'complexo',
            'routine_actions': 1,
            'xp': 0,
            'physical_defenses_published': False,
            'source_ref': 'Kingmaker p.415',
            'description': 'Armadilha de B4: permanecer mais de uma rodada sem recitar as frases em Aklo provoca rajadas de frio.',
            'reaction': '12d6 de frio, Fortitude básica CD 41; falha crítica também imobiliza por 1 minuto. Em seguida rola iniciativa.',
            'routine': [
                {
                    'action_cost': 1,
                    'name': 'Rajada de frio',
                    'description': 'Todas as criaturas em B4 sofrem 12d6 de frio, Fortitude básica CD 41; falha crítica imobiliza por 1 minuto.',
                }
            ],
            'reset': 'Desativa uma rodada após ficar sem alvos e rearma após 1 minuto.',
        },
        {
            'hazard_key': 'hazard_candlemere_spiral_seal',
            'hazardKey': 'hazard_candlemere_spiral_seal',
            'name': 'Selo Espiral de Contenção de Candlemere',
            'level': 16,
            'traits': ['magico', 'armadilha'],
            'stealth_dc': 38,
            'perception_dc': 38,
            'saving_throw_type': 'Vontade',
            'saving_throw_dc': 38,
            'disable_skills': [
                {
                    'skill': 'Ocultismo',
                    'dc': 37,
                    'proficiency': 'mestre',
                    'description': 'Remover o Selo Espiral em 10 minutos (notas de Irovetti concedem +4 de item).',
                },
                {
                    'skill': 'Religião',
                    'dc': 40,
                    'proficiency': 'mestre',
                    'description': 'Desfazer as runas de contenção de Pharasma ou dissipar magia de 9º nível (CD 40).',
                },
            ],
            'hazard_type': 'armadilha',
            'complexity': 'simples',
            'ac': 39,
            'hardness': 27,
            'hp': 106,
            'bt': 53,
            'routine_actions': 1,
            'xp': 60,
            'source_ref': 'Kingmaker pp.399-400',
            'description': 'Barreira ocultista que veda o poço de 30 metros sob a Torre de Candlemere até A1.',
            'reaction': 'Pulso de rejeição planar contra tentativas forçadas de violação.',
            'routine': [
                {
                    'action_cost': 1,
                    'name': 'Rejeição do Selo Espiral',
                    'description': '8d10 de dano de força e mental contra quem força a barreira (Vontade básica CD 38).',
                }
            ],
            'reset': 'Permanece ativo até ser ritualmente removido.',
        },
        {
            'hazard_key': 'hazard_candlemere_orb_blast',
            'hazardKey': 'hazard_candlemere_orb_blast',
            'name': 'Orbe de Ressonância Planar de B11',
            'level': 18,
            'traits': ['magico', 'armadilha'],
            'stealth_dc': 40,
            'perception_dc': 40,
            'saving_throw_type': 'Reflexos',
            'saving_throw_dc': 40,
            'disable_skills': [
                {
                    'skill': 'Ocultismo',
                    'dc': 40,
                    'proficiency': 'mestre',
                    'description': 'Estabilizar a frequência dimensional do orbe em B11.',
                },
                {
                    'skill': 'Ladroagem',
                    'dc': 42,
                    'proficiency': 'mestre',
                    'description': 'Desacoplar os filamentos de controle sem detonar a matriz.',
                },
            ],
            'hazard_type': 'armadilha',
            'complexity': 'simples',
            'ac': 42,
            'hardness': 30,
            'hp': 40,
            'bt': 20,
            'routine_actions': 1,
            'xp': 80,
            'source_ref': 'Kingmaker p.418',
            'description': 'Orbe instável na câmara B11 da Espiral de Yog-Sothoth que descarrega energia do vazio.',
            'reaction': 'Detona um pulso dimensional quando manipulado sem sintonização Aklo.',
            'routine': [
                {
                    'action_cost': 1,
                    'name': 'Descarga do Vazio de B11',
                    'description': '10d10 de dano de força em emanação de 9 metros (Reflexos básico CD 40).',
                }
            ],
            'reset': 'Rearma após 10 minutos se não for destruído ou desativado.',
        },
    ]
    d['pois'] = [
        {
            'poi_id': 'poi_candlemere_contested_level',
            'poi_key': 'poi_candlemere_contested_level',
            'campaign_key': 'chapter_08_they_lurk_below',
            'map_id': 'stolen_lands',
            'name': 'Candlemere — Nível Disputado (O Limiar do Pavor)',
            'name_key': 'candlemere_nivel_disputado',
            'type': 'dungeon',
            'poi_type': 'dungeon',
            'hex_id': 'H42',
            'location_id': 'candlemere_level_a',
            'q': 4,
            'r': 8,
            'source': 'campaign',
            'origin': 'campaign_discovery',
            'canonical': 0,
            'hidden': False,
            'discovered': True,
            'created_by': 'campaign',
            'created_tick': 0,
            'status': 'active',
            'description': 'Complexo superior sob Candlemere (Áreas A1–A17, Threshold of Dread), dividido entre os Pharasmins de Korog e os cultistas Thresholders.',
            'tags': ['candlemere', 'dungeon', 'threshold_of_dread', 'pharasma'],
            'development_level': 1,
            'icon': '🕯️',
            'notes': 'Kingmaker pp. 398-411',
        },
        {
            'poi_id': 'poi_candlemere_spiral_yog_sothoth',
            'poi_key': 'poi_candlemere_spiral_yog_sothoth',
            'campaign_key': 'chapter_08_they_lurk_below',
            'map_id': 'stolen_lands',
            'name': 'Candlemere — A Espiral de Yog-Sothoth (A Profundidade do Medo)',
            'name_key': 'candlemere_espiral_yog_sothoth',
            'type': 'dungeon',
            'poi_type': 'dungeon',
            'hex_id': 'H42',
            'location_id': 'candlemere_level_b',
            'q': 4,
            'r': 8,
            'source': 'campaign',
            'origin': 'campaign_discovery',
            'canonical': 0,
            'hidden': False,
            'discovered': True,
            'created_by': 'campaign',
            'created_tick': 0,
            'status': 'active',
            'description': 'Santuário inferior sob Candlemere (Áreas B1–B14, The Depth of Fear), onde Foras prepara a invocação de Yog-Sothoth e onde o Teorema da Hélice é executado.',
            'tags': ['candlemere', 'dungeon', 'the_depth_of_fear', 'yog_sothoth'],
            'development_level': 1,
            'icon': '🌀',
            'notes': 'Kingmaker pp. 412-421',
        },
    ]
    d['scenes'] = [
        {
            'scene_key': 'scene_candlemere_contested_level',
            'title': 'O Limiar do Pavor — Nível Disputado de Candlemere (A1–A17)',
            'location': 'Candlemere — Nível Disputado (Threshold of Dread)',
            'hex_id': 'H42',
            'environment': 'masmorra',
            'width_cells': 24,
            'height_cells': 18,
            'seed': 901,
            'status': 'ready',
            'zones': [
                {'zone_key': 'zone_a1_seal_cavern', 'name': 'A1: Caverna sob o Selo Espiral', 'x': 1, 'y': 1, 'width': 6, 'height': 5, 'terrain': 'normal', 'description': 'Base da descida de 30 metros após abrir o Selo Espiral.'},
                {'zone_key': 'zone_a4_korog_sanctum', 'name': 'A4: Comando de Korog e Pharasmins', 'x': 7, 'y': 2, 'width': 7, 'height': 6, 'terrain': 'normal', 'description': 'Reduto dos defensores mortos-vivos de Pharasma que contêm os cultistas.'},
                {'zone_key': 'zone_a9_helix_chalkboard', 'name': 'A9: Sala do Quadro do Teorema da Hélice', 'x': 14, 'y': 4, 'width': 5, 'height': 5, 'terrain': 'normal', 'description': 'Contém as anotações cruciais que revelam a existência do Teorema da Hélice.'},
                {'zone_key': 'zone_a17_spiral_descent', 'name': 'A13–A17: Fronteira dos Thresholders e Descida', 'x': 16, 'y': 10, 'width': 7, 'height': 7, 'terrain': 'difficult', 'description': 'Guarnecida por Hermeticistas, Discípulos e enviadas planares.'},
            ],
            'obstacles': [
                {'obstacle_key': 'obs_candlemere_barricade_a', 'x': 12, 'y': 8, 'width': 2, 'height': 2, 'type': 'wall', 'blocks_movement': True, 'blocks_vision': True, 'cover': 'greater'},
            ],
            'entry_points': [
                {'point_key': 'ep_candlemere_a1_players', 'x': 2, 'y': 2, 'team': 'player', 'description': 'Descida do Selo Espiral em A1'},
                {'point_key': 'ep_candlemere_a_enemies', 'x': 18, 'y': 12, 'team': 'enemy', 'description': 'Guarnição dos Thresholders'},
            ],
            'combatants': ['npc_korog', 'npc_kellid_graveknight', 'npc_morrigna_psychopomp', 'npc_thresholder_hermeticist', 'npc_thresholder_disciple'],
            'source_context': {'official_part': 'Threshold of Dread', 'pages': '398-411'},
        },
        {
            'scene_key': 'scene_candlemere_spiral_yog_sothoth',
            'title': 'A Profundidade do Medo — A Espiral de Yog-Sothoth (B1–B14)',
            'location': 'Candlemere — Espiral de Yog-Sothoth (The Depth of Fear)',
            'hex_id': 'H42',
            'environment': 'masmorra',
            'width_cells': 28,
            'height_cells': 20,
            'seed': 902,
            'status': 'ready',
            'zones': [
                {'zone_key': 'zone_b1_alcove', 'name': 'B1: Alcova Final do Teorema da Hélice', 'x': 1, 'y': 1, 'width': 5, 'height': 5, 'terrain': 'normal', 'description': 'Alcova onde o ícone fechado deve ser depositado ao concluir a sequência B14->B1.'},
                {'zone_key': 'zone_b4_praise_trap', 'name': 'B4: Câmara do Louvor a Yog-Sothoth', 'x': 7, 'y': 3, 'width': 6, 'height': 6, 'terrain': 'normal', 'description': 'Protegida pela armadilha complexa Louvor a Yog-Sothoth (Nv 19) e Quelaunt de elite.'},
                {'zone_key': 'zone_b13_helix_notes', 'name': 'B13: Câmara das Instruções do Teorema da Hélice', 'x': 15, 'y': 6, 'width': 6, 'height': 6, 'terrain': 'normal', 'description': 'Guarda as instruções completas do ritual da Hélice.'},
                {'zone_key': 'zone_b14_foras_sanctum', 'name': 'B14: Altar de Foras e Portão em Miniatura', 'x': 21, 'y': 10, 'width': 6, 'height': 8, 'terrain': 'difficult', 'description': 'Santuário central onde Foras lidera o ritual de chamada de Yog-Sothoth.'},
            ],
            'obstacles': [
                {'obstacle_key': 'obs_candlemere_spiral_pillar', 'x': 14, 'y': 10, 'width': 2, 'height': 2, 'type': 'pillar', 'blocks_movement': True, 'blocks_vision': True, 'cover': 'standard'},
            ],
            'entry_points': [
                {'point_key': 'ep_candlemere_b1_players', 'x': 2, 'y': 2, 'team': 'player', 'description': 'Entrada superior da Espiral em B1'},
                {'point_key': 'ep_candlemere_b14_foras', 'x': 24, 'y': 14, 'team': 'enemy', 'description': 'Altar de Foras em B14'},
            ],
            'combatants': ['npc_foras', 'npc_kyetriel', 'npc_niodrhast', 'npc_leng_envoy', 'npc_thresholder_mystic', 'npc_mastiff_of_tindalos', 'npc_elite_quelaunt', 'npc_candlemere_shoggoth'],
            'source_context': {'official_part': 'The Depth of Fear', 'pages': '412-421'},
        },
    ]
    d['npcs'] = [
        {
            'npc_key': 'npc_foras',
            'stable_id': 'npc_foras',
            'name': 'Foras',
            'summary': 'Feiticeiro humano e líder dos Thresholders sob Candlemere.',
            'goals': 'Trazer Yog-Sothoth ao Plano Material.',
            'secrets': 'Seu irmão Korog liderou a oposição dos Pharasmins.',
            'attitude': 'hostil',
            'last_seen': 'Candlemere B14',
            'hex_id': 'H42',
            'faction': 'Thresholders',
            'is_companion': False,
            'stats': {
                'level': 19,
                'hp': 355,
                'max_hp': 355,
                'ac': 43,
                'perception': 33,
                'fortitude': 31,
                'reflex': 31,
                'will': 35,
                'speed': 25,
                'spell_dc': 41,
                'spell_attack': 36,
                'mental_resistance': 20,
                'strength': 0,
                'dexterity': 4,
                'constitution': 2,
                'intelligence': 5,
                'wisdom': 4,
                'charisma': 6,
                'staff_attack': 31,
                'staff_damage': '3d4+10',
                'staff_damage_type': 'bludgeoning',
                'quickened_actions': ['step', 'stride', 'sustain_spell'],
                'resolve': 'Will success becomes critical success',
                'time_siphon': {
                    'range_feet': 60,
                    'will_dc': 38,
                    'failure': 'slowed 1 for 1 round',
                    'critical_failure': 'slowed 2 for 1 round',
                    'immune': 'Yog-Sothoth worshippers',
                },
                'source_ref': 'Kingmaker p.595',
            },
        },
        _npc('npc_korog', 'Korog', 16, 290, 38, 29, 30, 24, 29, 'Comandante Pharasmin morto-vivo que lidera o bloqueio contra seu irmão Foras.', 'Impedir que os Thresholders alcancem a superfície.', 'Permanece preso à não-vida pelo juramento a Pharasma até que Foras caia.', 'indiferente', 'Candlemere A4', 'Pharasmins de Korog', 402),
        _npc('npc_kyetriel', 'Kyetriel', 18, 340, 41, 31, 33, 28, 30, 'Gug sábio deformado a serviço dos Thresholders na Espiral.', 'Dilacerar intrusos nas galerias inferiores da Espiral.', 'Conhece os atalhos planares entre B8 e B12.', 'hostil', 'Candlemere B8', 'Thresholders', 416),
        _npc('npc_niodrhast', 'Niodrhast', 19, 375, 43, 34, 35, 32, 33, 'Guardião dracônico corrompido pelo vazio na Espiral de Yog-Sothoth.', 'Proteger o acesso final ao santuário de Foras.', 'Sua essência está vinculada às distorções temporais da Espiral.', 'hostil', 'Candlemere B12', 'Thresholders', 418),
        _npc('npc_leng_envoy', 'Emissário de Leng (Leng Envoy)', 18, 310, 41, 30, 30, 33, 31, 'Negociador planar de Leng aliado aos Thresholders.', 'Garantir a abertura da fenda para Yog-Sothoth.', 'Possui rotas de fuga planares caso o culto fracasse.', 'hostil', 'Candlemere B10', 'Thresholders', 414),
        _npc('npc_thresholder_mystic', 'Místico Thresholder (Thresholder Mystic)', 17, 275, 39, 29, 26, 28, 31, 'Alto ocultista do culto de Yog-Sothoth na Espiral inferior.', 'Sustentar os cânticos em Aklo e proteger B13–B14.', 'Carrega notas parciais sobre a sequência reversa do Teorema da Hélice.', 'hostil', 'Candlemere B13', 'Thresholders', 409),
        _npc('npc_thresholder_hermeticist', 'Hermeticista Thresholder (Thresholder Hermeticist)', 16, 240, 38, 27, 25, 27, 30, 'Conjurador erudito dos Thresholders no nível disputado.', 'Romper as defesas dos Pharasmins de Korog.', 'Estudou o quadro de A9 antes da chegada dos heróis.', 'hostil', 'Candlemere A13', 'Thresholders', 406),
        _npc('npc_thresholder_disciple', 'Discípulo Thresholder (Thresholder Disciple)', 14, 195, 35, 24, 23, 25, 26, 'Guarda fanático do culto dos Thresholders.', 'Defender os corredores do nível disputado.', 'Recita as frases de passe de B4 em Aklo.', 'hostil', 'Candlemere A11', 'Thresholders', 404),
        _npc('npc_kellid_graveknight', 'Cavaleiro do Túmulo Kellid (Kellid Graveknight)', 14, 215, 36, 25, 26, 22, 25, 'Campeão morto-vivo aliado aos Pharasmins de Korog.', 'Manter a linha de frente contra as aberrações do culto.', 'Busca redenção final após a destruição do culto.', 'indiferente', 'Candlemere A3', 'Pharasmins de Korog', 401),
        _npc('npc_morrigna_psychopomp', 'Psicopompo Morrigna (Morrigna Psychopomp)', 15, 250, 37, 28, 25, 27, 28, 'Agente de Pharasma que observa o impasse sob Candlemere.', 'Julgar as almas retidas assim que o culto for derrotado.', 'Apoia grupos que respeitem os ritos de Pharasma.', 'indiferente', 'Candlemere A6', 'Pharasmins de Korog', 403),
        _npc('npc_mastiff_of_tindalos', 'Mastim de Tindalos de Elite (Mastiff of Tindalos)', 15, 230, 37, 28, 25, 29, 26, 'Predador dimensional que emerge pelos ângulos agudos de Candlemere.', 'Caçar intrusos que atravessem os portais angulares.', 'Vulnerável a espaços curvos sem ângulos.', 'hostil', 'Candlemere B3', 'Thresholders', 413),
        _npc('npc_elite_quelaunt', 'Quelaunt de Elite (Elite Quelaunt)', 16, 285, 39, 28, 28, 26, 30, 'Aberração devoradora de emoções postada na Espiral.', 'Alimentar-se do pavor gerado pelas armadilhas de B4.', 'Atua em sincronia com a armadilha Louvor a Yog-Sothoth.', 'hostil', 'Candlemere B4', 'Thresholders', 415),
        _npc('npc_candlemere_shoggoth', 'Shoggoth da Espiral (Candlemere Shoggoth)', 18, 375, 40, 32, 33, 30, 29, 'Massa protoplasmática colossal contida nas profundezas da Espiral.', 'Engolfar e dissolver qualquer criatura em seu caminho.', 'Evitado até pelos próprios cultistas.', 'hostil', 'Candlemere B9', 'Thresholders', 417),
    ]
    d['factions'] = [
        {
            'name': 'Thresholders',
            'agenda': 'Concluir a chegada de Yog-Sothoth.',
            'resources': 'Cultistas e aliados de Leng.',
            'relationship': -5,
            'current_plan': 'Trabalhar na Espiral sob Candlemere.',
            'location': 'Candlemere',
            'active': True,
        },
        {
            'name': 'Pharasmins de Korog',
            'agenda': 'Manter os Thresholders separados da superfície.',
            'resources': 'Defensores mortos-vivos e antigas proteções.',
            'relationship': 0,
            'current_plan': 'Preservar o impasse no nível disputado.',
            'location': 'Candlemere',
            'active': True,
        },
    ]
    d['clocks'] = []
    d['events'] = []
    d['milestones'] = [
        {
            'milestone_key': 'candlemere_spiral_seal',
            'name': 'Romper o Selo Espiral',
            'xp': 60,
            'source_ref': 'Kingmaker p.400',
        },
        {
            'milestone_key': 'candlemere_helix',
            'name': 'Concluir o Teorema da Hélice',
            'xp': 120,
            'source_ref': 'Kingmaker p.413',
        },
    ]
    metadata = dict(d.get('metadata', {}))
    metadata.update(
        official_chapter_number=9,
        source_ref='Kingmaker pp.396-421',
        definition_revision='candlemere-r36-primary',
        parts=[
            {'key': 'threshold_of_dread', 'title': 'O Limiar do Pavor', 'official_name': 'Threshold of Dread', 'page': 398, 'minimum_level': 16},
            {'key': 'depth_of_fear', 'title': 'A Profundidade do Medo', 'official_name': 'The Depth of Fear', 'page': 412, 'minimum_level': 16},
        ],
        official_parts=OFFICIAL_PARTS_BY_ASSET['chapter_08_they_lurk_below.json'],
        retirement={
            'quests': [{'key': q.get('quest_key'), 'title': q.get('title')} for q in old.get('quests', [])],
            'world_clocks': [x.get('name') for x in old.get('clocks', [])],
            'world_events': [x.get('title') for x in old.get('events', [])],
            'npc_memory_profiles': [x.get('name') for x in old.get('npcs', [])],
            'campaign_pois': [x.get('name') for x in old.get('pois', [])],
            'tactical_scenes': [x.get('scene_key') for x in old.get('scenes', [])],
        },
        coverage={'plot_correction': True, 'all_encounters_compiled': True, 'all_maps_compiled': False},
    )
    d['metadata'] = metadata
    return d


def enrich_all_chapters_and_hooks(directory):
    for filename, official_parts in OFFICIAL_PARTS_BY_ASSET.items():
        target = directory / filename
        if not target.is_file():
            continue
        doc = json.loads(target.read_text(encoding='utf-8'))
        meta = dict(doc.get('metadata') or {})
        meta['official_parts'] = official_parts
        doc['metadata'] = meta

        hook_spec = SECONDARY_COMPANION_HOOKS_BY_ASSET.get(filename)
        if hook_spec and doc.get('quests'):
            q_key = hook_spec['quest_key']
            target_q = next((q for q in doc['quests'] if isinstance(q, dict) and q.get('quest_key') == q_key), doc['quests'][0])
            if isinstance(target_q, dict):
                prog = dict(target_q.get('progress_json') or {})
                prog['companion_hooks'] = hook_spec['companion_hooks']
                target_q['progress_json'] = prog

        target.write_text(json.dumps(doc, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def apply_campaign(root):
    directory = Path(root) / ASSETS
    p = directory / 'chapter_08_they_lurk_below.json'
    d = json.loads(p.read_text(encoding='utf-8'))
    if d.get('metadata', {}).get('definition_revision') != 'candlemere-r36-primary':
        expected = {'q_terras_sombrias', 'q_brecha_sekamina', 'q_ancoras_abissais', 'q_aberracoes_subterraneas', 'q_terremoto_da_capital'}
        if {q.get('quest_key') for q in d['quests']} != expected:
            raise ValueError('Unexpected chapter-nine draft identities')
        changes = {p: corrected_chapter(d)}
        for filename, low, high in [
            ('chapter_06_blood_for_blood.json', 10, 13),
            ('chapter_07_war_of_river_kings.json', 13, 16),
            ('chapter_09_sound_thousand_screams.json', 17, 20),
        ]:
            target = directory / filename
            x = json.loads(target.read_text(encoding='utf-8'))
            x.update(level_min=low, levelMin=low, level_max=high, levelMax=high)
            changes[target] = x
        for target, value in changes.items():
            target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    enrich_all_chapters_and_hooks(directory)
    print('R36_CANDLEMERE_CANONICAL_PLOT_APPLIED')
