from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
import subprocess,re,json,time
root=Path(__file__).resolve().parent;out=root/'evidence/P222/test-results';out.mkdir(parents=True,exist_ok=True)
files=sorted((root/'tests').glob('*.test.mjs'))
def parse(s):
 p=re.search(r'^# pass (\d+)$',s,re.M);f=re.search(r'^# fail (\d+)$',s,re.M)
 return (int(p.group(1)),int(f.group(1))) if p and f else None
def run(p):
 try:r=subprocess.run(['node','--test',str(p)],cwd=root,capture_output=True,text=True,timeout=170)
 except subprocess.TimeoutExpired:return {'file':p.name,'ok':False,'passed':0,'failed':1,'error':'TIMEOUT'}
 t=r.stdout+r.stderr;(out/(p.stem+'.tap')).write_text(t);n=parse(t)
 return {'file':p.name,'ok':r.returncode==0,'passed':n[0] if n else 0,'failed':n[1] if n else 1,'returncode':r.returncode}
need=[]
for p in files:
 tap=out/(p.stem+'.tap')
 if not tap.exists() or not parse(tap.read_text()) or parse(tap.read_text())[1]>0:need.append(p)
print('FILES_TO_RUN',len(need),flush=True)
with ThreadPoolExecutor(max_workers=5) as pool:
 for fut in as_completed([pool.submit(run,p) for p in need]):
  x=fut.result();print('RESULT',x['file'],x['passed'],x['failed'],flush=True)
results=[]
for p in files:
 n=parse((out/(p.stem+'.tap')).read_text()) if (out/(p.stem+'.tap')).exists() else None
 results.append({'file':p.name,'ok':bool(n and n[1]==0),'passed':n[0] if n else 0,'failed':n[1] if n else 1})
regular=[x for x in results if not x['file'].startswith('stress')];stress=[x for x in results if x['file'].startswith('stress')]
summary={'checkpoint':'P222','mode':'node --test each test file separately, 5 parallel workers, cached per-file TAP','regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(x['passed']+x['failed'] for x in regular),'regularPassed':sum(x['passed'] for x in regular),'stressTests':sum(x['passed']+x['failed'] for x in stress),'stressPassed':sum(x['passed'] for x in stress),'failedFiles':[x for x in results if not x['ok']],'totalFiles':len(results),'totalTests':sum(x['passed']+x['failed'] for x in results),'totalPassed':sum(x['passed'] for x in results),'focalTests':6}
(root/'evidence/P222/PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2))
(root/'evidence/P222/TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2))
print(json.dumps(summary,indent=2),flush=True)
if summary['failedFiles']:raise SystemExit(1)
