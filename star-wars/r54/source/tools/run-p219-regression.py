from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import subprocess,json,re,time,os,signal
root=Path(__file__).resolve().parent.parent
files=sorted((root/'tests').glob('*.test.mjs'))
regular=[f for f in files if not f.name.startswith('stress-')]
stress=[f for f in files if f.name.startswith('stress-')]
out=root/'evidence'/'P219';out.mkdir(parents=True,exist_ok=True)
start=time.monotonic()
def run(f):
 p=subprocess.Popen(['node','--test',str(f.relative_to(root))],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,start_new_session=True)
 try: data,_=p.communicate(timeout=30);rc=p.returncode
 except subprocess.TimeoutExpired:
  os.killpg(p.pid,signal.SIGKILL)
  data,_=p.communicate(timeout=5);rc=124
 def get(key):
  m=re.findall(r'^# '+key+r' (\d+)\s*$',data,re.M)
  return int(m[-1]) if m else 0
 return {'file':str(f.relative_to(root)),'exit':rc,'tests':get('tests'),'passed':get('pass'),'failed':get('fail'),'output':data[-6000:] if rc else None}
results=[]
with ThreadPoolExecutor(max_workers=8) as ex:
 for fut in as_completed([ex.submit(run,f) for f in regular]):
  x=fut.result();results.append(x)
  if x['exit'] or x['failed']: print('FAIL',x['file'],x['exit'],str(x['output'])[-300:],flush=True)
  if len(results)%40==0:print('regular',len(results),'/',len(regular),round(time.monotonic()-start,1),flush=True)
for f in stress:
 x=run(f);results.append(x);print('stress',f.name,x['exit'],flush=True)
results.sort(key=lambda x:x['file'])
(out/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False))
summary={'checkpoint':'P219','mode':'node --test each file independently, eight workers; stress sequential','regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(r['tests'] for r in results if not r['file'].startswith('tests/stress-')),'regularPassed':sum(r['passed'] for r in results if not r['file'].startswith('tests/stress-')),'stressTests':sum(r['tests'] for r in results if r['file'].startswith('tests/stress-')),'stressPassed':sum(r['passed'] for r in results if r['file'].startswith('tests/stress-')),'failedFiles':[r['file'] for r in results if r['exit'] or r['failed']],'elapsedSeconds':round(time.monotonic()-start,2),'totalFiles':len(results),'totalTests':sum(r['tests'] for r in results),'totalPassed':sum(r['passed'] for r in results)}
(out/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False));print('FINAL',json.dumps(summary,ensure_ascii=False),flush=True)
