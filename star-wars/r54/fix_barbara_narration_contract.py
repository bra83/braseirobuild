#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: fix_barbara_narration_contract.py <android-project-root>')

root = Path(sys.argv[1]).resolve()
path = root / 'app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java'
text = path.read_text(encoding='utf-8')
old = 'A narração deve ser cinematográfica, concisa e em português do Brasil.'
new = ('A narração deve ser cinematográfica, detalhada e em português do Brasil. '
       'Produza 400-700 palavras em 4-7 parágrafos, mantendo foco na situação presente e espaço real para decisão do jogador. '
       'Inclua sons, cheiros, temperatura, texturas e sensações físicas relevantes à cena, sem inventar decisões do personagem do jogador.')
if old not in text:
    if new in text:
        print('R54_BARBARA_NARRATION_CONTRACT_PASS already_applied=1')
        raise SystemExit(0)
    raise SystemExit('Barbara narration prompt anchor not found; refusing blind patch')
text = text.replace(old, new, 1)
path.write_text(text, encoding='utf-8')
print('R54_BARBARA_NARRATION_CONTRACT_PASS words=400-700 paragraphs=4-7 sensory=5')
