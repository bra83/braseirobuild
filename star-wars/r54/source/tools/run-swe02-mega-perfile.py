from concurrent.futures import ThreadPoolExecutor,as_completed
from pathlib import Path
import subprocess,json,time,re
root=Path(__file__).resolve().parents[1]
tests=sorted((root/'tests').glob('*.test.mjs'))
dest=root/'evidence/SWE02_FULL_BLOCK_WIP5/test-results'
dest.mkdir(parents=True,exist_ok=True)
stress={'stress-persistent-adversary-r17.test.mjs','stress-factions-r18.test.mjs','stress-33-faction-plan-r19.test.mjs','stress-faction-plans-r19.test.mjs','stress-33-aftermath-r20.test.mjs','stress-33-sessions-r15.test.mjs'}
def run(p):
 try:
  r=subprocess.run(['node','--test',str(p)],cwd=root,capture_output=True,text=True,timeout=120)
  log=r.stdout+'\n'+r.stderr;(dest/(p.stem+'.tap')).write_text(log)
  def num(k):
   m=re.findall(r'^# '+k+r' (\d+)\s*$',log,re.M);return int(m[-1]) if m else None
  return {'file':p.name,'stress':p.name in stress,'exit':r.returncode,'pass':num('pass'),'fail':num('fail'),'skip':num('skipped'),'tests':num('tests')}
 except Exception as e:
  (dest/(p.stem+'.tap')).write_text(str(e));return {'file':p.name,'stress':p.name in stress,'exit':-1,'error':str(e)}
start=time.monotonic();results=[]
with ThreadPoolExecutor(max_workers=8) as ex:
 fs={ex.submit(run,p):p for p in tests}
 for fut in as_completed(fs):
  result=fut.result();results.append(result)
  if result['exit'] or result.get('fail') or result.get('pass') is None:print('FAIL',result,flush=True)
  if len(results)%40==0:print('PROGRESS',len(results),'/',len(tests),'elapsed',round(time.monotonic()-start),flush=True)
summary={'scope':'SWE02_PALACE_WHOLE_ZONE_FAMILY_WIP5','files':len(tests),'regular_files':len(tests)-len(stress),'stress_files':len(stress),'regular_pass':sum(x.get('pass') or 0 for x in results if not x['stress']),'stress_pass':sum(x.get('pass') or 0 for x in results if x['stress']),'total_pass':sum(x.get('pass') or 0 for x in results),'total_fail':sum(x.get('fail') or 0 for x in results),'total_skipped':sum(x.get('skip') or 0 for x in results),'failed_files':[x for x in results if x['exit'] or x.get('fail') or x.get('pass') is None],'duration_seconds':round(time.monotonic()-start,2)}
(root/'evidence/SWE02_FULL_BLOCK_WIP5/regression_summary.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False))
print(json.dumps(summary,indent=2,ensure_ascii=False),flush=True)
