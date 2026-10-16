from concurrent.futures import ThreadPoolExecutor,as_completed
from pathlib import Path
import subprocess,json,time,re,os
root=Path(__file__).resolve().parent.parent
allfiles=sorted((root/'tests').glob('*.test.mjs'))
stress=[x for x in allfiles if x.name.startswith('stress-')]
regular=[x for x in allfiles if x not in stress]
start=time.monotonic()
def run(p):
 t=time.monotonic()
 try:
  cp=subprocess.run(['node','--test',str(p.relative_to(root))],cwd=root,capture_output=True,text=True,timeout=70)
  out=cp.stdout+'\n'+cp.stderr
  match=re.findall(r'^# (tests|pass|fail) (\d+)$',out,re.M)
  fields=dict(match)
  return {'file':p.name,'exitCode':cp.returncode,'tests':int(fields.get('tests',0)),'passed':int(fields.get('pass',0)),'failed':int(fields.get('fail',0)),'elapsedSeconds':round(time.monotonic()-t,2),'tail':out[-1600:] if cp.returncode else ''}
 except Exception as e:return {'file':p.name,'exitCode':-1,'tests':0,'passed':0,'failed':1,'elapsedSeconds':round(time.monotonic()-t,2),'tail':str(e)}
results=[]
with ThreadPoolExecutor(max_workers=12) as pool:
 for fut in as_completed([pool.submit(run,p) for p in regular]):results.append(fut.result())
print('regular completed',len(results),'failed',sum(x['exitCode']!=0 for x in results),flush=True)
for p in stress:results.append(run(p));print('stress',p.name,results[-1]['exitCode'],flush=True)
results.sort(key=lambda x:x['file'])
failed=[x for x in results if x['exitCode']!=0 or x['failed']>0 or x['passed']!=x['tests']]
summary={'checkpoint':'P207','mode':'node --test per-file; 12 regular workers, 6 stress sequential; NOT monolithic','regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(x['tests'] for x in results if not x['file'].startswith('stress-')),'regularPassed':sum(x['passed'] for x in results if not x['file'].startswith('stress-')),'stressTests':sum(x['tests'] for x in results if x['file'].startswith('stress-')),'stressPassed':sum(x['passed'] for x in results if x['file'].startswith('stress-')),'failedFiles':[x['file'] for x in failed],'elapsedSeconds':round(time.monotonic()-start,2),'totalFiles':len(results),'totalTests':sum(x['tests'] for x in results),'totalPassed':sum(x['passed'] for x in results)}
ev=root/'evidence/P207';ev.mkdir(parents=True,exist_ok=True)
(ev/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False)+'\n')
(ev/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(summary,ensure_ascii=False,indent=2),flush=True)
for x in failed:print('FAIL',x['file'],x['tail'][-1000:],flush=True)
