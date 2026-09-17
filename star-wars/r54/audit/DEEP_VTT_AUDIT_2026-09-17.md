# STAR WARS — EDGE OF THE GALAXY — AUDITORIA PROFUNDA R54

Resumo: **17 PASS · 1 WARN · 0 FAIL**

## Achados
- **PASS S01 — Registro de telas**: 31 rotas únicas encontradas; esperado 31.
- **PASS A01 — Integridade física do manifest**: 271 entradas indexadas; 0 caminhos locais ausentes.
- **PASS A02 — Pacote visual histórico presente**: 42/42 assets esperados encontrados fisicamente.
- **PASS A03 — Pacote visual histórico ligado**: 42/42 têm referência explícita em bindings/código. Presença física sem ligação não conta como integração.
- **PASS A04 — R54 armas/equipamentos**: {"weapons": 32, "equipment": 22, "portraits": 1, "maps": 1}
- **WARN A05 — Cobertura do resolver visual R54**: Grupos explicitamente indexados no hotfix: ['weapons']. Isto não prova ausência no resolver legado; a renderização é auditada abaixo.
- **PASS R01 — Matriz de regras**: 54 contratos; {'TESTADO': 54}.
- **PASS R02 — Árvores de talentos AoR no runtime vivo**: Marcador de AoR inativo não encontrado no bundle vivo.
- **PASS R03 — Presença dos subsistemas centrais**: {"dice": true, "character": true, "combat": true, "vehicle_combat": true, "world": true, "travel": true, "master": true, "snapshots": true, "json": true}
- **PASS S02 — Render das 31 telas**: 31/31 visitadas; rotas com crash/erro/imagem quebrada/overflow: [].
- **PASS B01 — Acessibilidade/identificação de botões**: Rotas com botão visível sem texto/aria-label/title: [].
- **PASS B02 — Menu inferior funcional**: {"CENA": {"found": 1, "target": "00", "ok": true}, "PERSONAGEM": {"found": 1, "target": "06", "ok": true}, "ITENS": {"found": 1, "target": "08", "ok": true}, "NAVE": {"found": 1, "target": "10", "ok": true}, "REGISTROS": {"found": 1, "target": "12", "ok": true}}
- **PASS B03 — GERAR ILUSTRAÇÃO**: {"button": 1, "prompt_chars": 1010, "real_contract": true}
- **PASS B04 — Botão DADOS**: {"found": 1, "overlay": true}
- **PASS B05 — Botão REINICIAR**: {"found": 1, "returned_setup": true}
- **PASS B06 — Ficha → Progressão**: {"found": 1, "ok": true}
- **PASS M01 — Âncora do ponto inicial no mapa**: startMapPoint={'x': 80, 'y': 45, 'mapId': 'local.map.galaxy', 'source': 'player_selected'}
- **PASS S03 — 31 telas em 360×800**: bad_routes=[]; errors=[]

## Regras
- Status da matriz: `{"TESTADO": 54}`
- Não TESTADO: `[]`
- AoR árvore inativa no bundle: `False`

## Assets
- Imagens físicas no VTT: **263**
- Entradas de manifest: **271**
- Arquivos ausentes do manifest: **0**
- Pacote visual histórico presente: **42/42**
- Pacote visual histórico com binding/referência explícita: **42/42**
- R54: `{"weapons": 32, "equipment": 22, "portraits": 1, "maps": 1}`

## Telas
- `00` Cena — Exploração: h1=`` · buttons=12 · broken=0 · overflow=0 · errors=0
- `01` Cena — Social: h1=`` · buttons=12 · broken=0 · overflow=0 · errors=0
- `02` Cena — Combate pessoal: h1=`` · buttons=12 · broken=0 · overflow=0 · errors=0
- `03` Cena — Combate veicular / espacial: h1=`` · buttons=12 · broken=0 · overflow=0 · errors=0
- `04` Cena — Viagem / hiperespaço: h1=`` · buttons=12 · broken=0 · overflow=0 · errors=0
- `05` Cena — Consequência / recuperação: h1=`` · buttons=12 · broken=0 · overflow=0 · errors=0
- `06` Personagem — Ficha: h1=`Ficha` · buttons=11 · broken=0 · overflow=0 · errors=0
- `07` Personagem — Progressão: h1=`Progressão` · buttons=10 · broken=0 · overflow=0 · errors=0
- `08` Itens — Inventário: h1=`Inventário` · buttons=10 · broken=0 · overflow=0 · errors=0
- `09` Itens — Aquisição pendente: h1=`Aquisição pendente` · buttons=41 · broken=0 · overflow=0 · errors=0
- `10` Nave — Resumo: h1=`Resumo` · buttons=10 · broken=0 · overflow=0 · errors=0
- `11` Nave — Danos críticos: h1=`Danos críticos` · buttons=10 · broken=0 · overflow=0 · errors=0
- `12` Registros — Missão: h1=`Missão` · buttons=12 · broken=0 · overflow=0 · errors=0
- `13` Registros — Grupo / Base: h1=`Grupo / Base` · buttons=12 · broken=0 · overflow=0 · errors=0
- `14` Registros — Contatos conhecidos: h1=`Contatos conhecidos` · buttons=12 · broken=0 · overflow=0 · errors=0
- `15` Registros — Histórico: h1=`Histórico` · buttons=12 · broken=0 · overflow=0 · errors=0
- `16` Mapa — Galáctico: h1=`Galáctico` · buttons=14 · broken=0 · overflow=0 · errors=0
- `17` Mapa — Missão: h1=`Missão` · buttons=50 · broken=0 · overflow=0 · errors=0
- `18` Criação — Identidade: h1=`Identidade` · buttons=20 · broken=0 · overflow=0 · errors=0
- `19` Criação — Espécie: h1=`Espécie` · buttons=19 · broken=0 · overflow=0 · errors=0
- `20` Criação — Carreira: h1=`Carreira` · buttons=19 · broken=0 · overflow=0 · errors=0
- `21` Criação — Especialização: h1=`Especialização` · buttons=19 · broken=0 · overflow=0 · errors=0
- `22` Criação — Perícias: h1=`Perícias` · buttons=19 · broken=0 · overflow=0 · errors=0
- `23` Criação — Características: h1=`Características` · buttons=19 · broken=0 · overflow=0 · errors=0
- `24` Criação — Vínculo de campanha: h1=`Vínculo de campanha` · buttons=19 · broken=0 · overflow=0 · errors=0
- `25` Criação — Motivação: h1=`Motivação` · buttons=20 · broken=0 · overflow=0 · errors=0
- `26` Criação — Equipamento / orçamento: h1=`Equipamento / orçamento` · buttons=20 · broken=0 · overflow=0 · errors=0
- `27` Ajustes — Interface / acessibilidade: h1=`Interface / acessibilidade` · buttons=12 · broken=0 · overflow=0 · errors=0
- `28` Ajustes — Narração / voz: h1=`Narração / voz` · buttons=14 · broken=0 · overflow=0 · errors=0
- `29` Ajustes — Gemini / provedor: h1=`Gemini / provedor` · buttons=19 · broken=0 · overflow=0 · errors=0
- `30` Ajustes — Dados / diagnóstico: h1=`Dados / diagnóstico` · buttons=80 · broken=0 · overflow=0 · errors=0

## Botões-chave
- Menu inferior: `{"CENA": {"found": 1, "target": "00", "ok": true}, "PERSONAGEM": {"found": 1, "target": "06", "ok": true}, "ITENS": {"found": 1, "target": "08", "ok": true}, "NAVE": {"found": 1, "target": "10", "ok": true}, "REGISTROS": {"found": 1, "target": "12", "ok": true}}`
- Ilustração: `{"button": 1, "prompt_chars": 1010, "real_contract": true}`
- Dados: `{"found": 1, "overlay": true}`
- Reiniciar: `{"found": 1, "returned_setup": true}`
- Progressão: `{"found": 1, "ok": true}`
- Ajustes: `{"tts": true, "gemini": true, "library": true, "diagnostic": true, "json": true}`

## Arquivos de prova
- `DEEP_AUDIT.json`: inventário completo de telas, botões, assets, regras e runtime.
- `DEEP_AUDIT.md`: este resumo.
