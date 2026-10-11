# Módulos GitHub incorporados — Braseiro Universal

**Portas Java** (não dependências originais no APK) derivadas/reimplementadas a partir de repositórios MIT; conserva-se o aviso de atribuição.

1. **Caryyon/tome**, https://github.com/Caryyon/tome, *Tome contributors*, MIT. Fontes consultadas: `src/Entity.ts`, `src/validation.ts`, `converters/dnd5e-to-tome.ts`. Porta: `TomeEntityBridge.java`.
2. **1A7432/loreweaver**, https://github.com/1A7432/loreweaver, MIT. Fonte: `core/check_outcome.py` e `docs/protocol.md`. Porta: `LoreweaverOutcomePort.java`.
3. **Stefouch/foundry-year-zero-roller**, https://github.com/Stefouch/foundry-year-zero-roller, MIT. Fon​te: README de `YearZeroRoll`; porta funcional independente de Foundry: `YearZeroDicePort.java`.
4. **IPedrax/AetherTable**, https://github.com/IPedrax/AetherTable, *Pedro Medeiros* (2026), MIT. Fonte: contrato `[[ROLL:...]]` documentado em `README.md`; porta estrita para rolagem d20: `AetherTableRollIntentPort.java`.
5. **swrpg-online/dice**, https://github.com/swrpg-online/dice, MIT. Fonte: `src/diceFaces.ts`. Porta: `StarWarsNarrativeDicePort.java`.

License text applicable to the above MIT code: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

**Não copiados nesta etapa:** Gravewright (GPL-3.0), Forbidden Lands Foundry (GPL e conteúdo protegido), Steward (restrição não comercial), PlanarAlly (mapa exige camada de renderização e não resolve necessidade atual), Open-VTT (Godot). AetherTable (servidor Python/React): nenhum módulo foi portado, só analisado para futura integração do Gemini.
