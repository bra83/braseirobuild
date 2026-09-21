# STAR WARS R54 P27 — checkpoint verificável

Data: 2026-09-21
Branch: `star-wars-r54-p27-eote-aor-mixed`

## Base imutável
Não recriar projeto. Recuperar P25 release `STAR-WARS-EDGE-R54-P25`, asset `STAR_WARS_EDGE_OF_THE_GALAXY_R54_P25_ANDROID_STUDIO.zip`, SHA-256 `7bfab40d0b021872cf3132cba5fd49848a3424f274ecd633e02c512e0efe3585`, e sobrepor P26 release `12r`, asset `STAR_WARS_R54_P26_PLANET_CAROUSEL_VALIDATED_PATCH.zip`, SHA-256 `bef342d81c7a125bf6a9562ed5c1ff6dce35b62f99ee71daf8ec6c2a767eb6bb`.

## Concluído e persistido
- `apply_p27_campaign_ui.py`: UI de criação sensível ao modo, em PT-BR.
- EOTE mostra Obrigação; AOR mostra Dever + Contribuição; MIXED mantém Obrigação + Dever + Contribuição.
- `under_a_black_sun_adventure.pt-BR.json`: aventura **Sob um Sol Negro** estruturada a partir do PDF do usuário, sem substituir regras EotE.
- Estrutura implementada: Episódio I (perseguição + Chopper), Episódio II aberto (Umbra Club / The Spyder / Zelcomm Tower) e Episódio III (Setor 943 / NovaCORE / confronto com Kaa’to).
- `apply_p27_under_black_sun.py`: instala o data pack nos assets, adiciona seletor Mundo Vivo / Sob um Sol Negro e usa o canal canônico da Mestre Barbara. AoR puro bloqueia a aventura; EotE e MIXED aceitam. MIXED preserva os vínculos dos dois jogos.
- `gate_p27.py`: verifica marcadores de campanha, integração da aventura, compatibilidade e nós canônicos.
- Workflow `.github/workflows/build-star-wars-r54-p27.yml`: recupera P25/P26 por release com hash fixo, aplica P27, roda gates + `node --check`, compila `assembleDebug` e publica artifact somente se tudo passar.

## Evidência / estado real
Commits deste bloco: `da0fc1ac636b59b1a168f0ea1c9302b2a9548e1b`, `1b0cb91292c2de4f1de08d58877d059353fa37c9`, `d0c3ac347272eb52c1b9af5731b91cff6a4096e5`, `7a37d7d3997ab62d9470cc4f62b205d4c62c1f3d`.

Não declarar P27 compilado/PASS até existir execução bem-sucedida do workflow P27. O workflow foi criado nesta branch; não houve evidência de execução automática porque ele é `workflow_dispatch` e não foi colocado em `main`.

## Próximo bloco
1. Executar o workflow P27 (ou promover workflow/patches para uma branch disparável pelo runner BRELGA) e guardar run ID, logs, APK SHA-256 e artifact.
2. Browser/device regression 415x915: criação EOTE/AOR/MIXED, persistência após reinício, alternância correta Obrigação/Dever/Contribuição, início de Sob um Sol Negro e retorno à campanha.
3. Continuar bindings de NPCs/locais/imagens da aventura usando apenas material autorizado/disponível; manter segredos de GM fora da UI do jogador.
4. Só depois gerar release/checkpoint final P27.
