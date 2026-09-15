from pathlib import Path

# Crafting target recognition for projectile weapons and clothing.
p=Path('app/src/main/assets/www/core/crafting-service.js')
s=p.read_text()
old="function assertTarget(mod,item){const kinds=new Set([String(item?.kind??'').toUpperCase(),...(item?.tags??[]).map(x=>String(x).toUpperCase())]);if(item?.kind==='WEAPON'){kinds.add('WEAPON');if(item?.melee===false||item?.weaponModes?.some(m=>m.melee===false))kinds.add('RANGED_WEAPON');if(item?.traits?.includes('T'))kinds.add('THROWN_WEAPON');if(String(item?.catalogId??'').toUpperCase().startsWith('HURLANT_')||item?.ammoType==='HURLANT_BOLT'||String(item?.name??'').toUpperCase().includes('HURLANT'))kinds.add('HURLANT');if(item?.encumbrance===1)kinds.add('ONE_HANDED_WEAPON');}if(item?.kind==='ARMOR')kinds.add('ARMOR');if(item?.kind==='SHIELD')kinds.add('SHIELD');return mod.targets.some(t=>kinds.has(t))||mod.targets.includes('EQUIPMENT');}"
new="function assertTarget(mod,item){const kinds=new Set([String(item?.kind??'').toUpperCase(),...(item?.tags??[]).map(x=>String(x).toUpperCase())]);if(item?.kind==='WEAPON'){kinds.add('WEAPON');if(item?.melee===false||item?.weaponModes?.some(m=>m.melee===false)||item?.modes?.some(m=>m.melee===false))kinds.add('RANGED_WEAPON');if(item?.traits?.includes('T')||item?.weaponModes?.some(m=>m.modeId==='THROWN')||item?.modes?.some(m=>m.modeId==='THROWN'))kinds.add('THROWN_WEAPON');if(['ARROW','QUARREL'].includes(String(item?.ammoType??'').toUpperCase()))kinds.add('PROJECTILE_WEAPON');if(String(item?.catalogId??'').toUpperCase().startsWith('HURLANT_')||item?.ammoType==='HURLANT_BOLT'||String(item?.name??'').toUpperCase().includes('HURLANT'))kinds.add('HURLANT');if(item?.encumbrance===1)kinds.add('ONE_HANDED_WEAPON');}if(item?.kind==='ARMOR')kinds.add('ARMOR');if(item?.kind==='CLOTHING'||item?.weightRule==='NO_ENCUMBRANCE_WHILE_WORN')kinds.add('CLOTHING');if(item?.kind==='SHIELD')kinds.add('SHIELD');return mod.targets.some(t=>kinds.has(t))||mod.targets.includes('EQUIPMENT');}"
if old in s:s=s.replace(old,new,1)
elif "kinds.add('PROJECTILE_WEAPON')" not in s: raise SystemExit('crafting assertTarget marker not found')
p.write_text(s)

# A transformed Assassin's Trinket cannot be used as a weapon until toggled back.
p=Path('app/src/main/assets/www/core/combat-service.js')
s=p.read_text()
marker="  const item=inventory.find(i=>(i.itemId??i.id)===weaponId);if(!item)return{status:'MISSING_EVIDENCE_WEAPON'};"
insert=marker+"\n  if(item?.modRuntime?.assassinsTrinketForm==='JEWELRY')return{status:'WEAPON_IN_JEWELRY_FORM'};"
if "WEAPON_IN_JEWELRY_FORM" not in s:
    if marker not in s: raise SystemExit('combat item marker not found')
    s=s.replace(marker,insert,1)
p.write_text(s)
