from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import subprocess,json,re,time
root=Path(__file__).resolve().parents[1]
ev=root/'evidence/P234';logs=ev/'test-results';logs.mkdir(parents=True,exist_ok=True)
files=sorted((root/'tests').glob('*.test.mjs'))
stress=[p for p in files if p.name.startswith('stress-')]
regular=[p for p in files if p not in stress]
print('P234 starting individual tests:',len(regular),'regular +',len(stress),'stress',flush=True)
def run(p):
 try:
  r=subprocess.run(['node','--test',str(p)],cwd=root,capture_output=True,encoding='utf-8',errors='replace',timeout=160)
  out=r.stdout+r.stderr
  numbers=dict(re.findall(r'^# (tests|pass|fail|skipped) (\d+)\s*$',out,re.M))
  item={'file':p.name,'exit':r.returncode,'tests':int(numbers.get('tests',-1)),'pass':int(numbers.get('pass',-1)),'fail':int(numbers.get('fail',-1)),'skipped':int(numbers.get('skipped',0))}
 except subprocess.TimeoutExpired as e:
  out=(e.stdout or b'').decode(errors='replace') if isinstance(e.stdout,bytes) else (e.stdout or '')
  item={'file':p.name,'exit':124,'tests':-1,'pass':-1,'fail':-1,'skipped':0}
 (logs/(p.name+'.tap')).write_text(out)
 return item
start=time.time();allres=[]
with ThreadPoolExecutor(max_workers=4) as pool:
 futures={pool.submit(run,p):p for p in regular}
 for i,f in enumerate(as_completed(futures),1):
  x=f.result();allres.append(x)
  if x['exit'] or x['tests']!=x['pass']+x['skipped'] or x['fail']:
   print('REGRESSION_FAILURE',x,flush=True)
  if i%30==0 or i==len(regular):print('REGULAR_PROGRESS',i,'/',len(regular),'seconds',round(time.time()-start,2),flush=True)
for p in stress:
 x=run(p);allres.append(x);print('STRESS',x,flush=True)
allres.sort(key=lambda x:x['file'])
failures=[x for x in allres if x['exit'] or x['tests']<0 or x['fail'] or x['tests']!=x['pass']+x['skipped']]
reg={p.name for p in regular};st={p.name for p in stress}
summary={'checkpoint':'P234','mode':'Each test file individually; regular four parallel workers, stress sequential; not a monolithic suite','regularFiles':len(regular),'regularTests':sum(x['tests'] for x in allres if x['file'] in reg),'regularPassed':sum(x['pass'] for x in allres if x['file'] in reg),'stressFiles':len(stress),'stressTests':sum(x['tests'] for x in allres if x['file'] in st),'stressPassed':sum(x['pass'] for x in allres if x['file'] in st),'totalFiles':len(allres),'totalTests':sum(x['tests'] for x in allres),'totalPassed':sum(x['pass'] for x in allres),'totalSkipped':sum(x['skipped'] for x in allres),'failedFiles':failures,'elapsedSeconds':round(time.time()-start,2)}
(ev/'TEST_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
(ev/'PER_FILE_RESULTS.json').write_text(json.dumps(allres,ensure_ascii=False,indent=2)+'\n')
print('FINAL_SUMMARY',json.dumps(summary,ensure_ascii=False),flush=True)
if failures:raise SystemExit(1)
