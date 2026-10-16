from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import subprocess,json,re,time,os,sys
root=Path(__file__).parent;files=sorted((root/'tests').glob('*.test.mjs'));regular=[f for f in files if not f.name.startswith('stress-')];stress=[f for f in files if f.name.startswith('stress-')];out=root/'evidence/P233/test-results';out.mkdir(exist_ok=True,parents=True)
def run(f):
 logpath=out/(f.stem+'.tap');rc=0;t=time.monotonic()
 if not logpath.exists() or '# tests ' not in logpath.read_text(errors='replace'):
  try:
   r=subprocess.run(['node','--test',str(f)],cwd=root,capture_output=True,text=True,env={**os.environ,'TERM':'dumb'},timeout=65)
   rc=r.returncode;logpath.write_text(r.stdout+'\n'+r.stderr)
  except subprocess.TimeoutExpired as e:
   rc=124;logpath.write_text('TIMEOUT\n'+str(e))
 log=logpath.read_text(errors='replace')
 def n(k):
  m=re.search(r'^# '+k+r' (\d+)',log,re.M);return int(m.group(1)) if m else 0
 if 'not ok ' in log or '# fail 0' not in log:rc=rc or 1
 return {'file':f.name,'exit':rc,'tests':n('tests'),'pass':n('pass'),'fail':n('fail'),'skipped':n('skipped'),'seconds':round(time.monotonic()-t,2)}
start=time.monotonic()
with ThreadPoolExecutor(max_workers=4) as pool:results=list(pool.map(run,regular))
for f in stress:results.append(run(f))
summary={'checkpoint':'P233','mode':'node --test per file; four regular workers, six stress sequential; no monolithic suite','regularFiles':len(regular),'regularTests':sum(x['tests'] for x in results[:len(regular)]),'regularPassed':sum(x['pass'] for x in results[:len(regular)]),'stressFiles':len(stress),'stressTests':sum(x['tests'] for x in results[len(regular):]),'stressPassed':sum(x['pass'] for x in results[len(regular):]),'totalFiles':len(files),'totalTests':sum(x['tests'] for x in results),'totalPassed':sum(x['pass'] for x in results),'totalSkipped':sum(x['skipped'] for x in results),'failedFiles':[x for x in results if x['exit'] or x['fail'] or x['tests']==0],'elapsedSecondsThisResume':round(time.monotonic()-start,2)}
(root/'evidence/P233/PER_FILE_RESULTS.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
(root/'evidence/P233/TEST_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(summary,ensure_ascii=False,indent=2),flush=True)
sys.exit(0 if not summary['failedFiles'] else 1)
