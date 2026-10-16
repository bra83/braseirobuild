# R54 P241 — fechamento e bloqueios verificáveis

**Base:** P240, SHA256 `cfd8abfcdcbb8a10d933c29cf086e3727ccbdd30c774fb73f8b19d8186abbd5c`.

## Implementado (sem certificação total da SWE02)

- Nabat p.13: médico twi'lek realiza **uma rolagem real por Ferimento Crítico**, pool oficial 1 proficiência e 2 dificuldades; tentativa, sucesso/falha, faces, cura real e recibo persistidos. Sem ressuscitar ferimentos mortais e sem repetir tentativa.
- Nabat p.14: Nyn aceita Charme Médio ou Coerção Difícil, usando perícia e característica da ficha real, dados narrativos e resultados oficiais. Cada PC pode receber uma granada de fragmentação ou atordoante; a armadura acolchoada e o fuzil são concessões únicas. Todas usam perfis primários revisados do EotE Core: Table 5-6 pp.162–163 e 165; Table 5-7 p.169. A distribuição é explícita para PC com identidade estável no roster e persiste em recibos/ficha. Equipamento recebe marcador de defeito com duas Ameaças; a remoção ao fim do primeiro encontro em que for usado ainda está pendente.
- New Meen p.23: suborno de **cada** capanga por 400–500 créditos, Negociação oposta a uma dificuldade; sucesso debita a carteira real e reduz efetivamente o tamanho dos grupos no encontro da cantina; falha não debita nem concede vitória. Drombb continua adversário e não pode ser subornado. Todos os recibos verificam faces e resultados de dados, e há rollback para ações inválidas.
- UI Android offline e bridge conectadas a controlador real. Menus em PT-BR. Três reaberturas nos cenários de teste.

## Ainda bloqueado

1. Integrar ciclo de descarte de equipamento defeituoso ao fim do primeiro encontro em que cada item for usado e completar as transições/dialogue gates de Nabat p.14.
2. New Meen: Coerção conforme mortes verificadas, consequências da derrota, escolta/evacuação, aliados Twi'lek.
3. SWE02: nave Beginner Game, combustível/carga/relógio, iniciativa veicular, atos 2 e 3, Teemo e todos os finais oficiais, auditoria página por página.
4. Outras quatro aventuras oficiais e todas as regras EotE/AoR/expansões, incluindo os 360 consumidores AoR individualmente.
5. APK, Gradle/CI BRELGA, ADB, Android físico e Kokoro físico **somente após** fechamento editorial e funcional dos itens anteriores.

**Não excluir nenhum backup sem prova de redundância.**
