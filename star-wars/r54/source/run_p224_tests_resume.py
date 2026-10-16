from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
import subprocess,re,json,time
root=Path(__file__).resolve().parent
regular=sorted(p for p in (root/'tests').glob('*.test.mjs') if not p.name.startswith('stress'))
stress=sorted(p for p in (root/'tests').glob('stress*.test.mjs'))
out=root/'evidence/P224/test-results';out.mkdir(parents=True,exist_ok=True)
start=time.monotonic()
def parse(p,s,rc):
 m=re.search(r'^# pass (\d+)$',s,re.M);f=re.search(r'^# fail (\d+)$',s,re.M)
 return {'file':p.name,'ok':rc==0,'passed':int(m.group(1)) if m else 0,'failed':int(f.group(1)) if f else 1,'returncode':rc}
def run(p):
 try:r=subprocess.run(['node','--test',str(p)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=180)
 except subprocess.TimeoutExpired as e:return {'file':p.name,'ok':False,'passed':0,'failed':1,'error':'TIMEOUT'}
 (out/(p.stem+'.tap')).write_text(r.stdout)
 return parse(p,r.stdout,r.returncode)
results=[];todo=[]
for p in regular+stress:
 path=out/(p.stem+'.tap')
 if path.exists():
  s=path.read_text();r=parse(p,s,0 if re.search(r'^# fail 0$',s,re.M) else 1)
  if r['ok'] and r['passed']>0:results.append(r);continue
 todo.append(p)
print('RESUME',len(results),'TODO',len(todo),flush=True)
with ThreadPoolExecutor(max_workers=6) as pool:
 for f in as_completed([pool.submit(run,p) for p in todo]):
  r=f.result();results.append(r)
  if not r['ok']:print('FAILED',r['file'],r,flush=True)
  if len(results)%25==0:print('FILES',len(results),'FAILURES',sum(not x['ok'] for x in results),flush=True)
results.sort(key=lambda x:x['file'])
summary={'checkpoint':'P224','mode':'node --test each test file separately, six workers; resume previous TAP logs','regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(x['passed']+x['failed'] for x in results if not x['file'].startswith('stress')),'regularPassed':sum(x['passed'] for x in results if not x['file'].startswith('stress')),'stressTests':sum(x['passed']+x['failed'] for x in results if x['file'].startswith('stress')),'stressPassed':sum(x['passed'] for x in results if x['file'].startswith('stress')),'failedFiles':[x for x in results if not x['ok']],'totalFiles':len(results),'totalTests':sum(x['passed']+x['failed'] for x in results),'totalPassed':sum(x['passed'] for x in results),'focalTests':8,'elapsedSeconds':round(time.monotonic()-start,1)}
(root/'evidence/P224/PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2))
(root/'evidence/P224/TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2))
print(json.dumps(summary,indent=2),flush=True)
if summary['failedFiles']:raise SystemExit(1)
