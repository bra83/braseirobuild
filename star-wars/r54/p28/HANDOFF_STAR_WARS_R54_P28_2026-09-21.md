# HANDOFF STAR WARS R54 P28 — MIXED + SOB UM SOL NEGRO

Data: 2026-09-21
Branch: star-wars-r54-p28-adventure-engine

## Baseline
Não recriar. Montagem autoritativa:
P25 Android Studio completo -> P26 validado -> patches P27 -> patches P28.

## Correção MIXED
O motor base já suporta simultaneamente Obligation e Duty em sessão MIXED. O defeito estava na ponte de criação, que tratava MIXED como EotE.

P28:
- EOTE continua usando criação EotE nativa.
- AOR continua usando criação AoR nativa.
- MIXED oferece as duas origens de criação no mesmo fluxo.
- Espécie e carreira precisam pertencer à mesma origem.
- EotE usa Obrigação e sua economia de criação.
- AoR usa Dever e sua economia de criação.
- Os bônus de Obrigação e Dever NÃO são somados na criação.
- A campanha MIXED continua mantendo ambos os subsistemas ativos em sessão e no grupo.

## Sob um Sol Negro
- hot-pursuit -> chop-talk;
- Episódio II aberto: Umbra Club / The Spyder / Zelcomm Tower;
- Setor 943 só é desbloqueado quando a pista correspondente é conquistada no Spyder;
- first-impressions permanece opcional;
- Setor 943 -> NovaCORE -> showdown;
- estado persistido em localStorage e retomável após reinício;
- Barbara recebe apenas o nó atual e contexto pertinente;
- consequências numéricas de Obrigação/Dever continuam no motor de regras.

## Critério de fechamento
Gate P27 + gate P28 + teste do engine + regressão Node + node --check + assembleDebug + assembleRelease no BRELGA + persistência/publicação.
