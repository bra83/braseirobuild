from concurrent.futures import ThreadPoolExecutor,as_completed
from pathlib import Path
import subprocess,time,re,json,os
root=Path(__file__).parent
files=sorted((root/'tests').glob('*.test.mjs'))
evidence=root/'evidence'/'P220';evidence.mkdir(parents=True,exist_ok=True)
(evidence/'test-results').mkdir(exist_ok=True)
start=time.monotonic()
def run(path):
 t=time.monotonic()
 try:
  p=subprocess.run(['node','--test',str(path)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=75)
  out=p.stdout;status=p.returncode
 except subprocess.TimeoutExpired as e:
  out=(e.stdout or b'').decode(errors='replace') if isinstance(e.stdout,bytes) else (e.stdout or '')
  status=124
 (evidence/'test-results'/(path.name+'.txt')).write_text(out)
 def count(kind):
  matches=re.findall(r'^# '+kind+r' (\d+)\s*$',out,re.M)
  return int(matches[-1]) if matches else 0
 return {'file':path.name,'exitCode':status,'tests':count('tests'),'passed':count('pass'),'failed':count('fail'),'elapsedSeconds':round(time.monotonic()-t,2),'error':out[-2500:] if status else None}
results=[]
with ThreadPoolExecutor(max_workers=4) as pool:
 futures={pool.submit(run,p):p for p in files}
 for f in as_completed(futures):
  r=f.result();results.append(r)
  if r['exitCode']!=0:print('FAIL',r['file'],r['error'][-500:],flush=True)
  elif len(results)%50==0:print('PROGRESS',len(results),'/',len(files),round(time.monotonic()-start,1),flush=True)
results.sort(key=lambda x:x['file'])
(evidence/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False))
stress=[r for r in results if r['file'].startswith('stress-')]
regular=[r for r in results if not r['file'].startswith('stress-')]
summary={'checkpoint':'P220','mode':'node --test each test file separately, four nonmonolithic workers','regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(r['tests'] for r in regular),'regularPassed':sum(r['passed'] for r in regular),'stressTests':sum(r['tests'] for r in stress),'stressPassed':sum(r['passed'] for r in stress),'failedFiles':[r['file'] for r in results if r['exitCode']!=0],'totalFiles':len(results),'totalTests':sum(r['tests'] for r in results),'totalPassed':sum(r['passed'] for r in results),'focalTests':8,'elapsedSeconds':round(time.monotonic()-start,2)}
(evidence/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False))
print('FINAL',json.dumps(summary,ensure_ascii=False),flush=True)
