from pathlib import Path
p=Path('app/src/main/assets/www/core/wwn-focus-catalog.js')
s=p.read_text()
if "from './wwn-srd-class-catalog.js'" not in s:
    s="import { classProfile } from './wwn-srd-class-catalog.js';\n"+s
old="const req=focus.requirements??focus.effects?.[0]?.requirements??[];const classes=new Set([String(character.classId??'').toUpperCase(),...(character.partialClasses??[]).map(x=>String(x).toUpperCase())]);if(req.includes('NOT_MAGE_OR_PARTIAL_MAGE')&&[...classes].some(x=>x.includes('MAGE')))return freeze({ok:false,status:'FOCUS_CLASS_RESTRICTION',focusId:focus.id});if(req.includes('FULL_OR_PARTIAL_EXPERT')&&![...classes].some(x=>x.includes('EXPERT')))return freeze({ok:false,status:'FOCUS_CLASS_RESTRICTION',focusId:focus.id});if(req.includes('MAGE_OR_PARTIAL_MAGE')&&![...classes].some(x=>x.includes('MAGE')))return freeze({ok:false,status:'FOCUS_CLASS_RESTRICTION',focusId:focus.id});"
new="const req=focus.requirements??focus.effects?.[0]?.requirements??[];const classIds=[String(character.classId??'').toUpperCase(),...(character.partialClasses??[]).map(x=>String(x).toUpperCase())].filter(x=>x&&x!=='ADVENTURER');const classes=new Set(classIds);const families=new Set(classIds.map(x=>classProfile(x)?.baseFamily).filter(Boolean));const mageLike=families.has('MAGE');if(req.includes('NOT_MAGE_OR_PARTIAL_MAGE')&&mageLike)return freeze({ok:false,status:'FOCUS_CLASS_RESTRICTION',focusId:focus.id});if(req.includes('FULL_OR_PARTIAL_EXPERT')&&!classes.has('EXPERT'))return freeze({ok:false,status:'FOCUS_CLASS_RESTRICTION',focusId:focus.id});if(req.includes('MAGE_OR_PARTIAL_MAGE')&&!mageLike)return freeze({ok:false,status:'FOCUS_CLASS_RESTRICTION',focusId:focus.id});"
if old not in s:
    if 'const families=new Set(' in s: pass
    else: raise SystemExit('focus class restriction marker not found')
else:s=s.replace(old,new,1)
p.write_text(s)
