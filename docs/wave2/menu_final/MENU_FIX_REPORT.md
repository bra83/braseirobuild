# SCR-015 — correção isolada do menu inferior

Escopo: somente menu inferior. Nenhum pixel acima de y=830 mudou na captura determinística.

## Alterações
- removidos fundos retangulares incorporados aos crops dos itens de navegação;
- Home, Explorar, Sessões e Favoritos passaram a usar componentes estáticos com fundo convertido para transparência, mantendo ícone e lettering aprovados;
- Configurações usa ícone transparente + texto nativo completo, corrigindo o crop original que perdia o primeiro caractere;
- fundo contínuo do menu recalibrado para a referência;
- separador superior reposicionado;
- EXPLORAR permanece o único item ativo;
- geometria/hitboxes originais preservados.

## Evidência
- Ambiente: Chromium 144 headless, viewport 415x915, DPR 1.
- Tolerância do diff binário: 16/255.
- Antes da correção fina do menu (baseline anterior): diff total = 78.208 px / 20,60%.
- Depois: diff total = 76.020 px / 20,02%.
- Região y=830..914: changed ratio caiu de 18,61% para 12,41%.
- Pixels acima de y=830 alterados entre baseline e resultado: 0.

## Regressão
- 27/27 geometrias exatas: PASS.
- apenas EXPLORAR ativo: PASS.
- toque/route no menu: PASS.
- 5 PLAYER_ACTION intents: PASS.
- notas recolher/reabrir: PASS.
- texto longo/scroll interno: PASS.
- assets runtime: PASS (29, 0 erros).
- Node tests: PASS.
