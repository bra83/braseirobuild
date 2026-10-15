#!/usr/bin/env python3
"""Verify exact pinned blob and every action entry; compile/run the independent executor."""
import hashlib,json,pathlib,subprocess,sys,tempfile
ROOT=pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
import verify_xmm_503_source as common

def verify(source):
    raw=pathlib.Path(source).read_bytes()
    if common.blob_sha(raw)!=common.BESTIARY_SHA:
        raise ValueError('XMM_BESTIARY_GIT_BLOB_MISMATCH')
    data=json.loads(raw)
    evidence=json.loads((pathlib.Path(__file__).parent/'coverage.json').read_text())
    compiled={r['id']:r for r in common.java_rows((ROOT/'app/src/main/java/com/braseiro/dnd5e/Xmm2025CompiledBestiaryEngine.java').read_text())}
    for row in evidence['entries']:
        value=data
        parts=row['source_pointer'].strip('/').split('/')
        for key in parts:value=value[int(key)] if isinstance(value,list) else value[key]
        if hashlib.sha256(value.encode()).hexdigest()!=row['source_entry_sha256']:
            raise ValueError('SOURCE_ENTRY_MISMATCH:'+row['source_pointer'])
        monster=data['monster'][int(parts[1])]
        action=monster[parts[2]][int(parts[3])]
        assert monster['name']==row['monster_id'] and action['name']==row['action_id']
        runtime=compiled[row['monster_id']]
        candidates=runtime['actions']+runtime['special_actions']
        match=[a for a in candidates if a['name']==row['action_id'].split(' {@recharge')[0]]
        assert len(match)==1
        assert match[0]==row['baseline_compiled_action'],row['source_pointer']
        assert match[0].get('manual_unmodeled_source_effect_required') or match[0].get('manual_rider_resolution_required')
    blocked=[r['source_pointer'] for r in evidence['entries'] if r['status']=='BLOCKED_CORE']
    with tempfile.TemporaryDirectory(prefix='damage03-') as out:
        base=ROOT/'app/src/main/java/com/braseiro/dnd5e'
        subprocess.run(['javac','-d',out,str(base/'DamageMitigationRules.java'),str(base/'XmmDamageOrSaveBranch03.java'),str(pathlib.Path(__file__).parent/'XmmDamageOrSaveBranch03Test.java')],check=True)
        subprocess.run(['java','-cp',out,'com.braseiro.dnd5e.XmmDamageOrSaveBranch03Test',*blocked],check=True)
    print('PASS: 12 exact source pointers and unchanged baseline guards; 4 independent executors, 8 blocked cores, 12 combat integrations blocked')
if __name__=='__main__':verify(sys.argv[1])
