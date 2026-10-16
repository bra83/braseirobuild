from pathlib import Path
r=Path(__file__).resolve().parent
v=r/'app/src/main/assets/vtt'
p=v/'js/adventure/jewelArendPatrols.mjs';s=p.read_text();s=s.replace("const clone=structuredClone;", "import {diceFacesForTests} from '../rules/narrativeDice.mjs';\nconst clone=structuredClone;")
s=s.replace("if(pool.difficulty!==3||Number(pool.challenge??0)!==0||!Number.isSafeInteger(pool.ability)||!Number.isSafeInteger(pool.proficiency))", "if(Number(pool.difficulty??0)+Number(pool.challenge??0)!==3||!Number.isSafeInteger(pool.ability)||!Number.isSafeInteger(pool.proficiency))")
s=s.replace("!Number.isSafeInteger(x.index)||x.index<0||!x.face||typeof x.face!=='object'", "!Number.isSafeInteger(x.index)||x.index<0||!equal(x.face,diceFacesForTests[x.type]?.[x.index])")
p.write_text(s)
def patch(path,old,new):
 f=v/path;s=f.read_text();assert s.count(old)==1,(path,old,s.count(old));f.write_text(s.replace(old,new))
f=v/'js/adventure/jewelOfYavin.mjs';s=f.read_text();assert s.count("  validateJewelArendProperty(p);\n  validateJewelHoloPark(p);")==2;s=s.replace("  validateJewelArendProperty(p);\n  validateJewelHoloPark(p);","  validateJewelArendProperty(p);\n  validateJewelArendPatrols(p,world);\n  validateJewelHoloPark(p);");f.write_text(s)
patch('js/adventure/jewelOfYavin.mjs',"    arendProperty: clone(validateJewelArendProperty(p)),", "    arendProperty: clone(validateJewelArendProperty(p)),\n    arendPatrols: clone(jewelArendPatrolStatus(world)),")
patch('js/core/state.mjs',"import {validateJewelArendProperty} from '../adventure/jewelArendProperty.mjs';", "import {validateJewelArendProperty} from '../adventure/jewelArendProperty.mjs';\nimport {validateJewelArendPatrols} from '../adventure/jewelArendPatrols.mjs';")
patch('js/core/state.mjs',"validateJewelArendProperty(next.world.publishedAdventure);}", "validateJewelArendProperty(next.world.publishedAdventure);validateJewelArendPatrols(next.world.publishedAdventure,next.world);}")
patch('js/core/state.mjs',"validateJewelArendProperty(value.world.publishedAdventure);}", "validateJewelArendProperty(value.world.publishedAdventure);validateJewelArendPatrols(value.world.publishedAdventure,value.world);}")
patch('js/adventure/jewelOfYavinOperations.mjs',"'JEWEL_AREND_PROPERTY_ENCOUNTER']);", "'JEWEL_AREND_PROPERTY_ENCOUNTER','JEWEL_AREND_PATROL_RESIDENCE_START','JEWEL_AREND_PATROL_RESIDENCE_CHECK','JEWEL_AREND_PATROL_RESIDENCE_END','JEWEL_AREND_PATROL_VENTURE_ALERT','JEWEL_AREND_PATROL_VENTURE_ARRIVAL']);")
# version
f=r/'app/build.gradle.kts';s=f.read_text();assert 'versionCode = 10220' in s and 'versionName = "1.0.220"' in s;f.write_text(s.replace('versionCode = 10220','versionCode = 10221').replace('versionName = "1.0.220"','versionName = "1.0.221"'))
print('P221 patches applied')
