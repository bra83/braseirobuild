from pathlib import Path
import subprocess,concurrent.futures,time,json,re,os,sys
root=Path(__file__).parent
out=root/'evidence/P226/test-results';out.mkdir(parents=True,exist_ok=True)
files=sorted(x for x in (root/'tests').glob('*.test.mjs') if not x.name.startswith('stress-'))
start=time.monotonic()
def run(p):
 t=time.monotonic()
 try:
  r=subprocess.run(['node','--test',str(p.relative_to(root))],cwd=root,capture_output=True,text=True,timeout=35)
  s=r.stdout+'\n'+r.stderr
 except subprocess.TimeoutExpired as e:
  s=str(e);r=type('R',(),{'returncode':124})()
 (out/(p.stem+'.tap')).write_text(s)
 counts={k:int(re.findall(r'^# '+k+r' (\d+)$',s,re.M)[-1]) if re.findall(r'^# '+k+r' (\d+)$',s,re.M) else 0 for k in ['tests','pass','fail','skipped']}
 return {'file':p.name,'exit':r.returncode,'seconds':round(time.monotonic()-t,2),**counts}
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
 results=list(pool.map(run,files))
regular=[r for r in results if not r['file'].startswith('stress-')]
stress=[r for r in results if r['file'].startswith('stress-')]
failed=[r for r in results if r['exit']!=0 or r['fail'] or r['tests']!=r['pass']+r['skipped']]
summary={'checkpoint':'P226','mode':'node --test each test file individually, 12 workers, no monolithic suite','regularFiles':len(regular),'regularTests':sum(x['tests'] for x in regular),'regularPassed':sum(x['pass'] for x in regular),'stressFiles':len(stress),'stressTests':sum(x['tests'] for x in stress),'stressPassed':sum(x['pass'] for x in stress),'totalFiles':len(results),'totalTests':sum(x['tests'] for x in results),'totalPassed':sum(x['pass'] for x in results),'failedFiles':failed,'elapsedSeconds':round(time.monotonic()-start,1)}
(root/'evidence/P226/PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2))
(root/'evidence/P226/TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2))
print(json.dumps(summary,indent=2));sys.exit(1 if failed else 0)
