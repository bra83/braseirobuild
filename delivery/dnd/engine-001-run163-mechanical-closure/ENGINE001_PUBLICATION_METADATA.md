# ENGINE001_PUBLICATION_METADATA

CHECKPOINT_NAME: BRASEIRO_DND_6.6.2_ENGINE001_RUN162_RUN163_MECHANICAL_CLOSURE_PTBR_PATCH.zip
DRIVE_ID: `1Bz9nXDJ4ZOOYBY38cO0r-1y8NuI_DjRI`
DRIVE_URL: https://drive.google.com/file/d/1Bz9nXDJ4ZOOYBY38cO0r-1y8NuI_DjRI/view?usp=drivesdk
CHECKPOINT_SHA256: `ac5b68c4d33a230d86cb2109f6231d079d4541a7d757dc4fef9d3e97b5aca0c7`
PATCH_SHA256: `84a7ca091900b297ea31d93c08ad52dd714e965fb0327273b665eab467360827`
BASE_NAME: BRASEIRO_DND_6.6.2_RUN163_HEROES_FAERUN_MEGA_PTBR_PATCH.zip
BASE_SHA256: `a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813`
STATUS: PARTIAL

## Aplicação exata sobre RUN163

1. Verificar o ZIP base:
   `sha256sum BRASEIRO_DND_6.6.2_RUN163_HEROES_FAERUN_MEGA_PTBR_PATCH.zip`
   deve resultar em `a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813`.
2. Extrair o RUN163 limpo.
3. Extrair o checkpoint ENGINE-001 em local separado e obter `dnd_eberron_full/ENGINE-001.patch`.
4. Entrar na raiz `dnd_eberron_full` da extração limpa do RUN163.
5. Executar:
   `git apply --check /caminho/para/ENGINE-001.patch`
6. Somente se o check passar, executar:
   `git apply /caminho/para/ENGINE-001.patch`
7. Não aplicar o patch sobre RUN110, RUN149, RUN162 ou `main`.
8. Não fazer merge automático; MASTER deve reconciliar com GAMEPLAY-001 e ANDROID-001.
9. Não há delta Java ENGINE nesta entrega; os seis arquivos compartilhados críticos devem permanecer como no RUN163 salvo reconciliação explícita do MASTER.

GAPS: Eberron 20/27 cartões numéricos BLOCKED_SOURCE; Heroes source-fidelity/subclass fine audit + atlas PARTIAL; Runeterra Pistoleiro/Armeiro BLOCKED_SOURCE_RULE_TEXT; Grung BLOCKED_SOURCE; Xamã/Ritualista BLOCKED_SOURCE_RULE_TEXT.
