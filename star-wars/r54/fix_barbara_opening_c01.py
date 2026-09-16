#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv)!=2: raise SystemExit('usage: fix_barbara_opening_c01.py <android-project-root>')
root=Path(sys.argv[1]).resolve()
p=root/'app/src/main/assets/vtt/js/core/controller.mjs'
s=p.read_text('utf-8')
old="""        const parts = [`A campanha começa em ${locationName}.`];
        if (knownOpportunity?.summary) parts.push(String(knownOpportunity.summary));
        else if (opening.location.description) parts.push(String(opening.location.description));
        if (knownNpc?.name) parts.push(`${knownNpc.name} está presente e pode saber mais sobre o que está acontecendo.`);
        if (knownFaction?.name) parts.push(`A presença de ${knownFaction.name} já influencia a situação local.`);
        parts.push('Nada obriga você a aceitar uma missão: investigar, negociar, partir ou ignorar a situação são caminhos válidos, e o mundo continuará se movendo.');
"""
new="""        const openingFacts = [
            String(opening.location.description ?? '').trim(),
            String(knownOpportunity?.summary ?? '').trim(),
            knownNpc?.name ? `${knownNpc.name} está presente e faz parte da situação que já se move ao redor do personagem.` : '',
            knownFaction?.name ? `${knownFaction.name} exerce influência perceptível sobre o local e sobre as pessoas que circulam por ele.` : ''
        ].filter(Boolean);
        const narration = [
            `Em ${locationName}, a campanha já começa em movimento. O lugar não espera pelo personagem: tráfego, rumores, interesses locais e sinais de tensão continuam existindo antes de qualquer decisão do jogador.`,
            openingFacts.join(' '),
            'A cena inicial apresenta o ambiente e seus agentes como uma situação aberta, não como uma missão obrigatória. Há coisas acontecendo, pessoas com objetivos próprios e consequências possíveis, mas nenhuma escolha é presumida em nome do personagem.',
            'Investigar, conversar, negociar, observar, procurar trabalho, deixar o local ou simplesmente ignorar o que parece urgente continuam sendo decisões válidas. O mundo vivo deve reagir ao que for escolhido e também ao que for deixado para trás.',
            'A partir deste ponto, Barbara assume a condução narrativa usando o estado persistente do mundo, revelando apenas o que o personagem pode perceber ou descobrir e encerrando cada intervenção com espaço real para a ação do jogador.'
        ].filter(Boolean).join('\\n\\n');
"""
if old not in s:
    if 'const openingFacts = [' in s and 'A partir deste ponto, Barbara assume a condução narrativa' in s:
        print('R54_BARBARA_OPENING_C01_PASS already_applied=1'); raise SystemExit(0)
    raise SystemExit('C01 short-opening anchor not found; refusing blind patch')
s=s.replace(old,new,1).replace("            narration: parts.join(' '),","            narration,",1)
p.write_text(s,'utf-8')
print('R54_BARBARA_OPENING_C01_PASS template_removed=1 paragraphs=5')
