# STAR WARS R38 — BLOCK04 BROAD RUNTIME AUDIT

- Findings: **2**
- High: **2**

## Findings
- **OFFSCREEN_CONTROL** — 415x915 REGISTROS: controls outside viewport — `[{'text': 'Contatos conhecidos', 'box': {'x': 231.046875, 'y': 173.875, 'width': 192.28125, 'height': 44}}, {'text': 'Histórico', 'box': {'x': 430.328125, 'y': 173.875, 'width': 94.953125, 'height': 44}}]`
- **OFFSCREEN_CONTROL** — 360x800 REGISTROS: controls outside viewport — `[{'text': 'Contatos conhecidos', 'box': {'x': 231.046875, 'y': 173.875, 'width': 192.28125, 'height': 44}}, {'text': 'Histórico', 'box': {'x': 430.328125, 'y': 173.875, 'width': 94.953125, 'height': 44}}]`

## 415x915
- Document: `{'sw': 415, 'cw': 415, 'sh': 1077, 'ch': 915, 'bodySw': 415}`
- Nav: `{'x': 0, 'y': 836.609375, 'width': 415, 'height': 78.390625}`
- Tabs: `{"CENA": {"active_labels": ["CENA"], "offscreen_interactives": []}, "PERSONAGEM": {"active_labels": ["PERSONAGEM"], "offscreen_interactives": []}, "ITENS": {"active_labels": ["ITENS"], "offscreen_interactives": []}, "NAVE": {"active_labels": ["NAVE"], "offscreen_interactives": []}, "REGISTROS": {"active_labels": ["REGISTROS"], "offscreen_interactives": [{"text": "Contatos conhecidos", "box": {"x": 231.046875, "y": 173.875, "width": 192.28125, "height": 44}}, {"text": "Histórico", "box": {"x": 430.328125, "y": 173.875, "width": 94.953125, "height": 44}}]}}`
- Local HTTP errors: **0**
- JS page errors: **0**

## 360x800
- Document: `{'sw': 360, 'cw': 360, 'sh': 1190, 'ch': 800, 'bodySw': 360}`
- Nav: `{'x': 0, 'y': 721.609375, 'width': 360, 'height': 78.390625}`
- Tabs: `{"CENA": {"active_labels": ["CENA"], "offscreen_interactives": []}, "PERSONAGEM": {"active_labels": ["PERSONAGEM"], "offscreen_interactives": []}, "ITENS": {"active_labels": ["ITENS"], "offscreen_interactives": []}, "NAVE": {"active_labels": ["NAVE"], "offscreen_interactives": []}, "REGISTROS": {"active_labels": ["REGISTROS"], "offscreen_interactives": [{"text": "Contatos conhecidos", "box": {"x": 231.046875, "y": 173.875, "width": 192.28125, "height": 44}}, {"text": "Histórico", "box": {"x": 430.328125, "y": 173.875, "width": 94.953125, "height": 44}}]}}`
- Local HTTP errors: **0**
- JS page errors: **0**

## Static
- Missing local refs: **0**
