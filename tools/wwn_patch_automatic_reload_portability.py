from pathlib import Path

p=Path('app/src/main/assets/www/core/equipment-mod-runtime-service.js')
s=p.read_text()
needle="if(automatic){if(!bonuses.automaticReloadOncePerScene)return f({status:'AUTOMATIC_RELOAD_MOD_REQUIRED_NO_MUTATION',revision:l.revision});const sk=sceneKey(s,sceneToken);"
replacement="if(automatic){if(!bonuses.automaticReloadOncePerScene)return f({status:'AUTOMATIC_RELOAD_MOD_REQUIRED_NO_MUTATION',revision:l.revision});if(Number(weapon.encumbrance)>2)return f({status:'AUTOMATIC_RELOAD_ON_TURN_REQUIRES_MAN_PORTABLE_HURLANT_NO_MUTATION',revision:l.revision,encumbrance:weapon.encumbrance});const sk=sceneKey(s,sceneToken);"
if 'AUTOMATIC_RELOAD_ON_TURN_REQUIRES_MAN_PORTABLE_HURLANT_NO_MUTATION' not in s:
    if needle not in s: raise SystemExit('automatic reload marker not found')
    s=s.replace(needle,replacement,1)
p.write_text(s)
