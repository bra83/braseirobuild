# BRASEIRO WWN — POST071 STATE PARITY + POI HOTFIX — 2026-09-13

## Escopo
Hotfix cumulativo sobre POST071 + AUDIO_HOTFIX + SAFEAREA_HOTFIX.

## Correções
1. Barras PV/Tensão: removidos assets com preenchimento colorido baked; agora o preenchimento vem exclusivamente de `characterStatusProjection`.
   - IRIA PV 5/5 => vermelho 100%.
   - IRIA System Strain/Tensão 0/11 => azul 0%.
2. Ficha > Equip.: lê o mesmo estado persistido Android da Mochila via `NativePersistencePort` + campanha ativa.
   - inventário real é renderizado sem imagens individuais inventadas;
   - carga mostra pontos usados/capacidade (ex.: 2/3 preparado, 0/7 guardado).
3. Atlas POI: terreno continua sendo o hex; POI é camada estrutural separada sobre o hex.
   - o artwork legado hexagonal é recortado no renderer para mostrar somente a estrutura central;
   - marcador do grupo continua independente.

## Gates locais
- POST071_INVENTORY_STATUS_POI_HOTFIX_PASS
- POST071_CURRENT_CONTRACT_PASS
- STATIC_GUARD_PASS
- RELEASE_SECURITY_GUARD_PASS
- SQLITE_CONTRACT_PASS
- CLOSURE065_NATIVE_CONTRACT: 35/35 PASS
- CLOSURE069_NATIVE_AUDIO: 19/19 PASS
- JS syntax: 116/116 PASS
- Stress: 33 sessões × 144 = 4.752 turnos PASS
- deterministic replay PASS: e44f81c9530e12bc80b9616b2f7181221008b26c04f448beadd0806e47f1aaca

## Suíte histórica bruta
- 193 testes; 189 PASS; 4 asserts históricos/superseded.
- `combat-closure029`: exige `hp_strain_frame_106x39.png`; superseded porque o asset contém preenchimento baked e foi removido para barras canônicas.
- `shared-navigation-closure`: espera `ficha.html#equip`; superseded pela tela real `mochila.html`.
- `shared-navigation-closure` Atlas: mesma expectativa antiga `ficha.html#equip`.
- `vertical-slice`: espera `DEFERRED_NO_MUTATION`; superseded pelas ações SCR015 implementadas e aprovadas.

Nenhum código mecânico foi regredido para satisfazer asserts obsoletos.
