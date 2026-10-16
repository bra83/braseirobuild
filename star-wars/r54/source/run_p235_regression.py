from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
import subprocess,json,re,time,os,sys
root=Path(__file__).parent;files=sorted((root/'tests').glob('*.test.mjs'))
regular=[f for f in files if not f.name.startswith('stress-')]
stress=[f for f in files if f.name.startswith('stress-')]
out=root/'evidence/P235/test-results';out.mkdir(exist_ok=True,parents=True)
def run(f):
 path=out/(f.stem+'.tap');t=time.monotonic();rc=0
 try:
  r=subprocess.run(['node','--test',str(f)],cwd=root,capture_output=True,text=True,timeout=115,env={**os.environ,'TERM':'dumb'})
  rc=r.returncode;path.write_text(r.stdout+'\n'+r.stderr)
 except subprocess.TimeoutExpired as e:
  rc=124;path.write_text('TIMEOUT\n'+str(e))
 log=path.read_text(errors='replace')
 def n(k):
  m=re.search(r'^# '+k+r' (\d+)',log,re.M);return int(m.group(1)) if m else 0
 if 'not ok ' in log or '# fail 0' not in log:rc=rc or 1
 return {'file':f.name,'exit':rc,'tests':n('tests'),'pass':n('pass'),'fail':n('fail'),'skipped':n('skipped'),'seconds':round(time.monotonic()-t,2)}
start=time.monotonic();results=[]
print('P235 starting',len(regular),'regular files plus',len(stress),'stress files',flush=True)
with ThreadPoolExecutor(max_workers=7) as pool:
 futures={pool.submit(run,f):f for f in regular}
 for fut in as_completed(futures):
  r=fut.result();results.append(r)
  if len(results)%30==0 or r['exit'] or r['fail']:
   print('REGULAR_PROGRESS',len(results),'/',len(regular),'seconds',round(time.monotonic()-start,1),'latest',r['file'], 'FAIL' if r['exit'] else 'PASS',flush=True)
for f in stress:
 r=run(f);results.append(r);print('STRESS',r,flush=True)
results=sorted(results,key=lambda x:x['file'])
regular_results=[x for x in results if not x['file'].startswith('stress-')]
stress_results=[x for x in results if x['file'].startswith('stress-')]
summary={'checkpoint':'P235','mode':'Each test file individually; regular 7 parallel workers, stress sequential; NOT a monolithic suite',
 'regularFiles':len(regular_results),'regularTests':sum(x['tests'] for x in regular_results),'regularPassed':sum(x['pass'] for x in regular_results),
 'stressFiles':len(stress_results),'stressTests':sum(x['tests'] for x in stress_results),'stressPassed':sum(x['pass'] for x in stress_results),
 'totalFiles':len(results),'totalTests':sum(x['tests'] for x in results),'totalPassed':sum(x['pass'] for x in results),
 'totalSkipped':sum(x['skipped'] for x in results),'failedFiles':[x for x in results if x['exit'] or x['fail'] or x['tests']==0],
 'elapsedSeconds':round(time.monotonic()-start,2)}
(root/'evidence/P235/PER_FILE_RESULTS.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
(root/'evidence/P235/TEST_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print('FINAL_SUMMARY',json.dumps(summary,ensure_ascii=False),flush=True)
sys.exit(0 if not summary['failedFiles'] else 1)
