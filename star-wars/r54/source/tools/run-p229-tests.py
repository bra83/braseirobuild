import subprocess,json,re,time
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
root=Path(__file__).resolve().parents[1]
evidence=root/'evidence/P229';logs=evidence/'test-results';logs.mkdir(parents=True,exist_ok=True)
files=sorted((root/'tests').glob('*.test.mjs'))
stress=[x for x in files if x.name.startswith('stress-')]
regular=[x for x in files if x not in stress]
print(f'P229 regular={len(regular)} stress={len(stress)}',flush=True)
def run(p):
 try:
  result=subprocess.run(['node','--test',str(p)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=120,encoding='utf-8',errors='replace')
  out=result.stdout
  matches=dict(re.findall(r'^# (tests|pass|fail|skipped) (\d+)\s*$',out,re.M))
  entry={'file':p.name,'exit':result.returncode,'tests':int(matches.get('tests',-1)),'pass':int(matches.get('pass',-1)),'fail':int(matches.get('fail',-1)),'skipped':int(matches.get('skipped',0))}
 except subprocess.TimeoutExpired as e:
  out=(e.stdout or b'').decode(errors='replace') if isinstance(e.stdout,bytes) else (e.stdout or '')
  entry={'file':p.name,'exit':124,'tests':-1,'pass':-1,'fail':-1,'skipped':0}
 (logs/(p.name+'.tap')).write_text(out)
 return entry
start=time.time()
results=[]
with ThreadPoolExecutor(max_workers=4) as pool:
 future={pool.submit(run,p):p for p in regular}
 for i,f in enumerate(as_completed(future),1):
  item=f.result();results.append(item)
  if item['exit'] or item['fail'] or item['tests']<0:print('FAIL',item,flush=True)
  if i%30==0:print('REGULAR',i,'/',len(regular),'seconds',int(time.time()-start),flush=True)
for p in stress:
 item=run(p);results.append(item);print('STRESS',item,flush=True)
results.sort(key=lambda x:x['file'])
regularnames={p.name for p in regular};stressnames={p.name for p in stress}
failures=[x for x in results if x['exit'] or x['fail'] or x['tests']<0 or x['tests']!=x['pass']+x['skipped']]
summary={'checkpoint':'P229','mode':'node --test each file in own process, regular in four workers, stress sequential; no monolithic suite','regularFiles':len(regular),'regularTests':sum(x['tests'] for x in results if x['file'] in regularnames),'regularPassed':sum(x['pass'] for x in results if x['file'] in regularnames),'stressFiles':len(stress),'stressTests':sum(x['tests'] for x in results if x['file'] in stressnames),'stressPassed':sum(x['pass'] for x in results if x['file'] in stressnames),'totalFiles':len(results),'totalTests':sum(x['tests'] for x in results),'totalPassed':sum(x['pass'] for x in results),'totalSkipped':sum(x['skipped'] for x in results),'failedFiles':failures,'elapsedSeconds':round(time.time()-start,2)}
(evidence/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False))
(evidence/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False))
print('SUMMARY',json.dumps(summary,ensure_ascii=False),flush=True)
if failures:raise SystemExit(1)
