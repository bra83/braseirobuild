#!/usr/bin/env python3
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import subprocess,re,json,time,sys
root=Path(__file__).resolve().parent.parent
files=sorted((root/'tests').glob('*.test.mjs'))
regular=[p for p in files if not p.name.startswith('stress') and 'stress' not in p.name]
stress=[p for p in files if p not in regular]
if '--regular' in sys.argv:stress=[]
if '--stress' in sys.argv:regular=[]
def run(path,timeout=45):
 try:
  p=subprocess.run(['node','--test','--test-reporter=tap',str(path.relative_to(root))],cwd=root,capture_output=True,text=True,timeout=timeout)
  output=p.stdout+'\n'+p.stderr
  counts={k:int(re.search(r'^# '+k+r' (\d+)',output,re.M).group(1)) if re.search(r'^# '+k+r' (\d+)',output,re.M) else None for k in ['tests','pass','fail','skipped']}
  okay=p.returncode==0 and counts['tests'] is not None and counts['pass'] is not None and counts['fail']==0 and counts['pass']+(counts['skipped'] or 0)==counts['tests']
  return {'file':path.name,'okay':okay,**counts,'tail':output[-2500:] if not okay else ''}
 except subprocess.TimeoutExpired:return {'file':path.name,'okay':False,'tests':0,'pass':0,'fail':1,'skipped':0,'tail':'TIMEOUT'}
results=[];t=time.monotonic()
with ThreadPoolExecutor(max_workers=8) as pool:
 for fut in as_completed([pool.submit(run,p) for p in regular]):
  results.append(fut.result())
  if not results[-1]['okay']: print('FAIL_EARLY',results[-1]['file'],results[-1]['tail'][-800:],flush=True)
  if len(results)%25==0:print('PROGRESS',len(results),'/',len(files),'bad',sum(not x['okay'] for x in results),flush=True)
for p in stress:
 results.append(run(p,90));print('STRESS',p.name,results[-1]['okay'],flush=True)
summary={'files':len(results),'tests':sum(x['tests'] or 0 for x in results),'pass':sum(x['pass'] or 0 for x in results),'fail':sum(x['fail'] or 0 for x in results),'skipped':sum(x['skipped'] or 0 for x in results),'bad':sum(not x['okay'] for x in results),'seconds':round(time.monotonic()-t,2)}
print('NODE_SUITE_JSON='+json.dumps(summary),flush=True)
for x in results:
 if not x['okay']:print('FAILED',x['file'],x['tail'],flush=True)
sys.exit(bool(summary['bad']))
