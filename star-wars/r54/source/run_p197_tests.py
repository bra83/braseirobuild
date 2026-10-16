from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import subprocess,json,re,time,sys
root=Path(__file__).resolve().parent
files=sorted((root/'tests').glob('*.test.mjs'))
regular=[f for f in files if not f.name.startswith('stress-')]
stress=[f for f in files if f.name.startswith('stress-')]
out=root/'evidence'/'P197';out.mkdir(parents=True,exist_ok=True)
start=time.monotonic()
def run(f):
  try:
    p=subprocess.run(['node','--test',str(f.relative_to(root))],cwd=root,text=True,capture_output=True,timeout=110)
    data=p.stdout+'\n'+p.stderr
    def get(key):
      m=re.findall(r'^# '+key+r' (\d+)\s*$',data,re.M)
      return int(m[-1]) if m else 0
    return {'file':str(f.relative_to(root)),'exit':p.returncode,'tests':get('tests'),'passed':get('pass'),'failed':get('fail'),'output':data[-10000:] if p.returncode else None}
  except subprocess.TimeoutExpired as e:
    return {'file':str(f.relative_to(root)),'exit':124,'tests':0,'passed':0,'failed':1,'output':'timeout 110s'}
results=[]
with ThreadPoolExecutor(max_workers=6) as ex:
  for fut in as_completed([ex.submit(run,f) for f in regular]):
    results.append(fut.result())
    if len(results)%40==0: print('regular progress',len(results),'/',len(regular),flush=True)
for f in stress:
  results.append(run(f))
  print('stress',f.name,results[-1]['exit'],flush=True)
results.sort(key=lambda r:r['file'])
(out/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False))
summary={'checkpoint':'P197','mode':'node --test per-file, regular 6 parallel workers; stress sequential, NOT monolithic','regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(r['tests'] for r in results if not r['file'].startswith('tests/stress-')),'regularPassed':sum(r['passed'] for r in results if not r['file'].startswith('tests/stress-')),'stressTests':sum(r['tests'] for r in results if r['file'].startswith('tests/stress-')),'stressPassed':sum(r['passed'] for r in results if r['file'].startswith('tests/stress-')),'failedFiles':[r['file'] for r in results if r['exit'] or r['failed']],'elapsedSeconds':round(time.monotonic()-start,1),'totalFiles':len(results),'totalTests':sum(r['tests'] for r in results),'totalPassed':sum(r['passed'] for r in results)}
(out/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False))
print(json.dumps(summary,indent=2),flush=True)
