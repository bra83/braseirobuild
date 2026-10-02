# PATCH_MANIFEST_ENGINE001

BASE: BRASEIRO_DND_6.6.2_RUN163_HEROES_FAERUN_MEGA_PTBR_PATCH.zip
BASE_SHA256: `a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813`
CHECKPOINT_DRIVE_ID: `1Bz9nXDJ4ZOOYBY38cO0r-1y8NuI_DjRI`
CHECKPOINT_SHA256: `ac5b68c4d33a230d86cb2109f6231d079d4541a7d757dc4fef9d3e97b5aca0c7`
PATCH_SHA256: `84a7ca091900b297ea31d93c08ad52dd714e965fb0327273b665eab467360827`

DELTA: documental/auditável apenas. Nenhum Java alterado e nenhuma feature nova.
PATCH: `ENGINE-001.patch` dentro do checkpoint Drive; representa apenas RUN163 → ENGINE-001 e aplica sobre a raiz `dnd_eberron_full`.

INVARIANTES:
- surface_language=pt-BR
- auto_rolls=false
- atomic_resource_commit=true
- save compatibility preservada
- Runeterra preservado
- Arcana Unleashed RUN147 preservado
- Forgotten Secrets preservado
- Mystara excluído
- APK/AAB ausente
- BOOK_COMPLETE=false

GAPS:
- Eberron bestiary: 20/27 numeric cards = BLOCKED_SOURCE
- Heroes: subclass exception audit + atlas/regional rules = PARTIAL
- Runeterra Pistoleiro/Armeiro = BLOCKED_SOURCE_RULE_TEXT
- Grung = BLOCKED_SOURCE
- Xamã/Ritualista = BLOCKED_SOURCE_RULE_TEXT
