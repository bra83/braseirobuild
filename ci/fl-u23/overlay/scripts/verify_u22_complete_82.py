#!/usr/bin/env python3
"""Complete release gate: exact official 82 IDs, each with nine browser stages.
Partial batches may pass independently. This final gate only passes at 82/82.
"""
import argparse,json,os
from verify_u14_browser_evidence import verify as verify_initial
from pathlib import Path
STAGES=('reachable_UI','preconditions','dice_or_combat_or_adjudication','canonical_transaction','receipt','reload','campaign_isolation','idempotency','rollback')
REPORTS=('u20-downstream-paths.json','u21-parley-rescue.json','u22-downstream-batch.json','u23-downstream-combat.json','u24-source-adjudications.json')
def audit(root,source):
 matrix=json.loads(source.read_text(encoding='utf8'));raw=matrix['consequencePaths'];keys=[p['key'] for p in raw]
 if len(keys)!=82 or len(set(keys))!=82:raise ValueError('Expected exactly 82 unique source IDs')
 proofs={};invalid=[]
 initial=verify_initial(root)
 if initial['status']!='PASS':invalid.append('Initial 43/121 browser evidence failed or is missing')
 suite_file=root/'u13-browser-suite-summary.json'
 suite=json.loads(suite_file.read_text()) if suite_file.exists() else {}
 if suite.get('status')!='PASS' or not suite.get('browser_coverage_verified'):invalid.append('Fresh complete browser suite did not pass')
 suite_reports={c.get('report'):c for c in suite.get('cases',[])}
 for filename in REPORTS:
  file=root/filename
  if not file.exists():continue
  report=json.loads(file.read_text(encoding='utf8'))
  if filename not in suite_reports or suite_reports[filename].get('status')!='PASS':invalid.append(f'{filename}: not proven by current suite runner')
  if not report.get('browserExecuted'):invalid.append(f'{filename}: browser not executed');continue
  rows=report.get('paths',[])
  if len(rows)!=report.get('expected'):invalid.append(f'{filename}: missing or repeated execution rows')
  for p in rows:
   key=p.get('pathKey')
   if key not in keys or key in proofs:invalid.append(f'{filename}: unknown or repeated key {key}');continue
   proofs[key]={**p,'report':filename}
 output=[]
 for p in raw:
  key=p['key'];proof=proofs.get(key);stages=proof.get('evidencePerStage',{}) if proof else {}
  certified=bool(proof and proof.get('status')=='PASS' and all(stages.get(s)=='PASS' for s in STAGES))
  output.append({**p,'status':'PASS' if certified else 'FAIL' if proof else 'NOT_RUN','report':proof.get('report') if proof else None,
   'evidencePerStage':{s:stages.get(s,'NOT_RUN') for s in STAGES},'blocker':None if certified else proof.get('error','Incomplete stage evidence') if proof else 'No executed downstream recipe; implement and execute real browser continuation'})
 count=sum(p['status']=='PASS' for p in output)
 result={'status':'PASS' if count==82 and not invalid else 'FAIL','complete':count==82 and not invalid,
  'totalDownstream':82,'certified':count,'pending':82-count,'sourceCommit':os.environ.get('GITHUB_SHA','local-working-tree'),
  'invalidEvidence':invalid,'paths':output,'encounters':[{'id':id,'downstreamTotal':sum(p['encounterId']==id for p in output),
    'downstreamPassed':sum(p['encounterId']==id and p['status']=='PASS' for p in output)} for id in range(1,44)]}
 root.mkdir(parents=True,exist_ok=True);(root/'u22-complete-82.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
 print('FORBIDDEN_LANDS_RELEASE_GATE',json.dumps({k:v for k,v in result.items() if k not in ('paths','encounters')},ensure_ascii=False))
 for p in output:
  if p['status']!='PASS':print(p['status'],p['key'],p['blocker'])
 return result
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('root',type=Path);parser.add_argument('source',type=Path);parser.add_argument('--report-only',action='store_true');args=parser.parse_args()
 result=audit(args.root,args.source)
 raise SystemExit(0 if args.report_only or result['complete'] else 1)
