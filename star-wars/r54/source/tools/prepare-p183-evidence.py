import json,hashlib,subprocess,pathlib,zipfile
root=pathlib.Path(__file__).resolve().parents[1];e=root/'evidence/P183';e.mkdir(parents=True,exist_ok=True)
regular=json.loads((e/'test-results/shard_0_1_regular.json').read_text());stress=json.loads((e/'test-results/shard_0_1_stress.json').read_text())
assert not regular['badFiles'] and not stress['badFiles']
summary={'checkpoint':'P183','method':'node --test --test-reporter=tap per file, eight workers regular, one stress worker','regular':regular,'stress':stress,'total':{'files':regular['files']+stress['files'],'passed':regular['passed']+stress['passed'],'failed':0},'focalP183':{'file':'tests/uae-p183-jewel-longarm-pdf-branches.test.mjs','passed':17},'limitations':['Not a monolithic Node run','No APK, Gradle/SDK/adb or real-device TTS validation','No full editorial PDF certification']}
(e/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False)+'\n')
source={'checkpoint':'P183','kind':'targeted editorial crosscheck (NOT complete certification)','officialDriveFiles':[
{'id':'1Lm26krELYpWU0Rx0WgPj__g72YU_F2lC','name':'Long Arm of the Hutt.pdf','knownSHA256FromP181':'18fb9880242743d11765eaca442168fc3441a3b0d17aa92bc769057734b45489','scope':'Adventure Summary Acts 1-3 and source terms Nabat, New Meen, lylek den, Ota, Piddock, Dimmock','confirmed':'Ryloth mine conflict before Geonosian negotiation and Teemo palace return','notCertified':['full scene details','combat stat blocks','encounter outcome fidelity','all NPCs']},
{'id':'1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K','name':'The Jewel of Yavin.pdf','scope':'Title/credits, table of contents, adventure overview and episode I summary','confirmed':'SWE09; opening Aris Shen in Port Town, Market Row chase, Arend Shen briefing, Grand Prix before gala, bank diversion and jewel theft as distinct tasks','notCertified':['race rules','bank financial amounts','all encounters','all NPC stats','all scenes']}
], 'sourceNotIncludedInCheckpoint':True,'implementation':{'LongArmNodesBefore':7,'LongArmNodesAfter':15,'JewelNodesBefore':5,'JewelNodesAfter':15,'jewelMechanics':'check-success gates for bank diversion and vault acquisition; alarm increases on failed attempts; alternative retreat without jewel','longArmMechanics':'New Meen and Geonosian investigation branches; Teemo defeat only after active combat and adjudicated victory','legacySaves':'Old Jewel bespin-arrival and Long Arm first two choice IDs remain accepted; no retroactive reward rewrite','rewards':'Remove fabricated fixed defaults for newly completed Long Arm and Jewel sessions; preserve existing saved receipts'},'editoriallyCertified':False}
(e/'PDF_TARGETED_SOURCE_CROSSCHECK.json').write_text(json.dumps(source,indent=2,ensure_ascii=False)+'\n')
old=zipfile.ZipFile('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P182_SOURCE_CHECKPOINT.zip')
java=[p for p in root.rglob('*.java') if 'node_modules' not in p.parts]
assert len(java)==8
for p in java:assert p.read_bytes()==old.read(p.relative_to(root).as_posix()),str(p)
print('KOKORO_JAVA_PRESERVED',len(java))
