# WWN — auditoria de jogabilidade de Facções

- Corrigido defeito de runtime: `gestao.js` importava `WorldService`, símbolo inexistente; o export real é `WorldActionService`. Isso podia impedir a tela Gestão inteira de carregar como ES Module.
- Adicionado `FactionRegistryService` com criação/edição/remoção persistente de facções.
- Force/Cunning/Wealth são evidência explícita do Mestre (1–8); o VTT não inventa estatísticas para facções narrativas.
- PV máximo é calculado pelo motor WWN (`factionMaximumHp`).
- Edição/remoção ficam bloqueadas durante turno de facções ativo para preservar integridade transacional.
- Facções narrativas do pack podem preencher ID/nome/objetivo, mas continuam exigindo F/C/W do Mestre.
- Auditoria global de imports nomeados: 0 símbolos ausentes após correção.
- Gestão: 0 referências DOM ausentes.
- 144 módulos JS: syntax check PASS.
- 19 suítes QA: PASS, incluindo 66 sessões simuladas acumuladas e nova regressão de criação de facções → início de turno.
