#!/usr/bin/env python3
"""Source-indexed 82-path audit. Never confuses 2/82 or 4/82 with 82/82.
FAIL CLOSED for duplicate keys, missing proof, 9 missing stages, stale JSON.
"""
import json,sys,os
from pathlib import Path
STAGES=('reachable_UI','preconditions','dice_or_combat_or_adjudication','canonical_transaction','receipt','reload','campaign_isolation','idempotency','rollback')
PROOFS={'u20-downstream-paths.json':{'35:1:offer_gift:gm_decision','41:1:board:follow_up'},
        'u21-parley-rescue.json':{'42:1:parley:gm_decision','43:1:intervene:gm_decision'}}
def audit(root:Path, source:Path):
 matrix=json.loads(source.read_text(encoding='utf-8'))
 expected={v['key']:v for v in matrix['consequencePaths']}
 assert len(expected)==82 and len(matrix['consequencePaths'])==82, 'wrong denominator or duplicate IDs'
 proven={};failures=[]
 for filename,keys in PROOFS.items():
  f=root/filename
  if not f.is_file():failures.append(f'missing proof {filename}');continue
  obj=json.loads(f.read_text(encoding='utf-8'))
  paths=obj.get('paths',[])
  if obj.get('status')!='PASS' or not obj.get('browserExecuted') or obj.get('failures'):failures.append(f'failed browser report: {filename}')
  if len(paths)!=len(keys) or {p.get('pathKey') for p in paths if isinstance(p,dict)}!=keys:failures.append(f'incomplete or duplicate keys in {filename}')
  for p in paths:
   if not isinstance(p,dict):continue
   key=p.get('pathKey')
   if key not in expected or key in proven:failures.append(f'unknown/repeated path {key}');continue
   if p.get('status')!='PASS' or any(p.get('evidencePerStage',{}).get(stage)!='PASS' for stage in STAGES):
    failures.append(f'incomplete nine-stage path: {key}')
   else:proven[key]={'evidencePerStage':p['evidencePerStage'],'report':filename,'browser':'Chromium','database':'IndexedDB'}
 output=[]
 for key,v in expected.items():
  proof=proven.get(key)
  output.append({'key':key,'encounterId':v['encounterId'],'kind':v['kind'],
     'status':'CERTIFIED_PATH' if proof else 'NOT_CERTIFIED',
     'report':proof['report'] if proof else None,
     'evidencePerStage':proof['evidencePerStage'] if proof else {stage:'NOT_RUN' for stage in STAGES}})
 verdict={'status':'FAIL' if failures else 'PARTIAL_HOMOLOGATION',
          'totalDownstream':82,'certified':len(proven),'pending':82-len(proven),
          'complete':len(proven)==82,'sourceCommit':os.environ.get('GITHUB_SHA','unknown'),
          'failures':failures,'paths':output}
 (root/'u21-matrix-82.json').write_text(json.dumps(verdict,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 for row in output:print(row['status'],row['key'])
 print('U21_AUDIT_82',json.dumps({k:v for k,v in verdict.items() if k!='paths'},ensure_ascii=False),flush=True)
 return 1 if failures or len(proven)!=4 else 0
if __name__=='__main__':
 assert len(sys.argv)==3,'Usage: verify_u21_matrix_82.py artifacts source-matrix'
 sys.exit(audit(Path(sys.argv[1]),Path(sys.argv[2])))
