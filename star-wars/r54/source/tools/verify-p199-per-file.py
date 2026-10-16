#!/usr/bin/env python3
import os,subprocess,re,json,time,concurrent.futures,pathlib,sys
root=pathlib.Path(__file__).resolve().parents[1]
files=sorted((root/'tests').glob('*.test.mjs'))
stress=[p for p in files if p.name.startswith('stress-')]
regular=[p for p in files if p not in stress]
out=root/'evidence'/'P199';out.mkdir(parents=True,exist_ok=True)

def run(p):
 try:
  r=subprocess.run(['node','--test','--test-reporter=tap',str(p)],cwd=root,capture_output=True,text=True,timeout=100)
  log=r.stdout+'\n'+r.stderr
  def n(k):
   m=re.findall(r'^# '+k+r'\s+(\d+)\s*$',log,re.M)
   return int(m[-1]) if m else None
  return {'file':p.name,'exit':r.returncode,'tests':n('tests'),'pass':n('pass'),'fail':n('fail'),'duration':n('duration_ms'),'error': '\n'.join(log.splitlines()[-90:]) if r.returncode else ''}
 except subprocess.TimeoutExpired:
  return {'file':p.name,'exit':124,'tests':None,'pass':0,'fail':1,'error':'timeout 100s'}

def summary(items):
 return {'files':len(items),'tests':sum(i.get('tests') or 0 for i in items),'pass':sum(i.get('pass') or 0 for i in items),'fail':sum(i.get('fail') or 0 for i in items),'failedFiles':[i['file'] for i in items if i['exit']!=0]}

print('regular',len(regular),'stress',len(stress),flush=True)
start=time.time()
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as ex:
 results=list(ex.map(run,regular))
print('REGULAR',summary(results),'elapsed',int(time.time()-start),flush=True)
for i in results:
 if i['exit']!=0:print('FAIL',i['file'],i['error'][-1500:],flush=True)
# stress sequential
for p in ([] if '--regular-only' in sys.argv else stress):
 x=run(p);results.append(x);print('STRESS',x['file'],x['exit'],x['pass'],x['fail'],flush=True)
(out/'PER_FILE_RESULTS.json').write_text(json.dumps({'files':results,'regular':summary(results[:len(regular)]),'stress':summary(results[len(regular):]),'total':summary(results),'elapsedSeconds':round(time.time()-start,2)},ensure_ascii=False,indent=2))
print('TOTAL',summary(results),'elapsed',int(time.time()-start),flush=True)
sys.exit(1 if any(x['exit'] for x in results) else 0)
