from pathlib import Path
p=Path('app/src/main/assets/www/ficha.js')
s=p.read_text()
imp="import { carryStateLabelPtBr } from './core/ptbr-labels.js';"
extra=imp+"\nimport { attributeModifier } from './core/wwn-rules-core.js';\nimport { deriveArmorClass } from './core/wwn-armor-core.js';\nimport { inventoryLoad } from './core/wwn-inventory-core.js';"
if "deriveArmorClass" not in s:
    if imp not in s: raise SystemExit('ptbr import marker missing')
    s=s.replace(imp,extra,1)
s=s.replace("const pmod=(key)=>c.attributeModifiers?.[key]??(String(c.characterId||'').toLowerCase()===String(presentation.characterId||'').toLowerCase()?presentation.attributeModifiers?.[key]:undefined);","const pmod=(key)=>Number.isInteger(attrs?.[key])?attributeModifier(attrs[key]):c.attributeModifiers?.[key];")
s=s.replace("const pskill=(key)=>String(c.characterId||'').toLowerCase()===String(presentation.characterId||'').toLowerCase()?presentation.summarySkillDisplay?.[key]:undefined;","const pskill=(key)=>Number.isInteger(skills?.[key])?skills[key]:undefined;")
marker="const val=(o,k,d='—')=>o?.[k]??d;"
if "const derivedArmor=" not in s:
    s=s.replace(marker,marker+"\nconst derivedArmor=deriveArmorClass(c);const derivedLoad=Array.isArray(c.inventory)?inventoryLoad(c):null;",1)
s=s.replace("text('txt_val_ca',String(c.armorClass??c.ac??10),'value center');","text('txt_val_ca',String(derivedArmor.status==='OK'?derivedArmor.armorClass:(c.armorClass??c.ac??10)),'value center');")
s=s.replace("text('txt_val_readied',String(c.load?.readiedCapacity??3),'value');","text('txt_val_readied',derivedLoad?.status==='OK'?`${derivedLoad.readiedPoints}/${derivedLoad.assessment.capacities.readied}`:String(c.load?.readiedCapacity??'—'),'value');")
s=s.replace("text('txt_val_stowed',String(c.load?.stowedCapacity??7),'value');","text('txt_val_stowed',derivedLoad?.status==='OK'?`${derivedLoad.stowedPoints}/${derivedLoad.assessment.capacities.stowed}`:String(c.load?.stowedCapacity??'—'),'value');")
p.write_text(s)
