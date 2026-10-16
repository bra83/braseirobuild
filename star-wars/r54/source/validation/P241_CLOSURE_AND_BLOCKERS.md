# R54 P241 — fechamento e bloqueios verificáveis

**Base:** P240, SHA256 `cfd8abfcdcbb8a10d933c29cf086e3727ccbdd30c774fb73f8b19d8186abbd5c`.

## Implementado (sem certificação total da SWE02)

- Nabat p.13: médico twi'lek realiza **uma rolagem real por Ferimento Crítico**, pool oficial 1 proficiência e 2 dificuldades; tentativa, sucesso/falha, faces, cura real e recibo persistidos. Sem ressuscitar ferimentos mortais e sem repetir tentativa.
- Nabat p.14: Nyn aceita Charme Médio ou Coerção Difícil, usando perícia e característica da ficha real, dados narrativos e resultados oficiais de Vantagens/Triunfos/Ameaças. Armadura acolchoada (catálogo primário EotE Core) é entregue à ficha quando concedida. Granadas/fuzil são **direitos registrados sem efeitos de combate**, porque o catálogo atual só contém perfis AoR não certificados para EotE; nunca criar armas funcionais a partir de fontes insuficientes.
- New Meen p.23: suborno de **cada** capanga por 400–500 créditos, Negociação oposta a uma dificuldade; sucesso debita a carteira real e reduz efetivamente o tamanho dos grupos no encontro da cantina; falha não debita nem concede vitória. Drombb continua adversário e não pode ser subornado. Todos os recibos verificam faces e resultados de dados, e há rollback para ações inválidas.
- UI Android offline e bridge conectadas a controlador real. Menus em PT-BR. Três reaberturas nos cenários de teste.

## Ainda bloqueado

1. Certificar perfis de granadas/fuzil EotE e consumos/defeitos no primeiro encontro, distribuir uma granada por PJ quando elegível; não utilizar estatísticas AoR como prova EotE.
2. New Meen: Coerção conforme mortes verificadas, consequências da derrota, escolta/evacuação, aliados Twi'lek.
3. SWE02: nave Beginner Game, combustível/carga/relógio, iniciativa veicular, atos 2 e 3, Teemo e todos os finais oficiais, auditoria página por página.
4. Outras quatro aventuras oficiais e todas as regras EotE/AoR/expansões, incluindo os 360 consumidores AoR individualmente.
5. APK, Gradle/CI BRELGA, ADB, Android físico e Kokoro físico **somente após** fechamento editorial e funcional dos itens anteriores.

**Não excluir nenhum backup sem prova de redundância.**
