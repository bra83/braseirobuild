from pathlib import Path
import concurrent.futures,subprocess,re,json,time
root=Path(__file__).resolve().parent
files=sorted((root/'tests').glob('*.test.mjs'))
logs=root/'evidence'/'SWE02_FULL_BLOCK_WIP5'/'test-results';logs.mkdir(parents=True,exist_ok=True)
start=time.monotonic()
def run(path):
 try:
  existing=logs/(path.stem+'.tap')
  if existing.exists() and re.search(r'^# fail 0$',existing.read_text(),re.M):
   out=existing.read_text()
   def count(name):
    m=re.findall(r'^# '+re.escape(name)+r' (\d+)$',out,re.M)
    return int(m[-1]) if m else None
   return {'file':path.name,'exit':0,'tests':count('tests'),'pass':count('pass'),'fail':count('fail'),'skip':count('skipped'),'reused_fresh_wip5_log':True}
  proc=subprocess.run(['node','--test',str(path)],cwd=root,text=True,capture_output=True,timeout=150)
  out=proc.stdout+proc.stderr
  (logs/(path.stem+'.tap')).write_text(out)
  def count(name):
   m=re.findall(r'^# '+re.escape(name)+r' (\d+)$',out,re.M)
   return int(m[-1]) if m else None
  return {'file':path.name,'exit':proc.returncode,'tests':count('tests'),'pass':count('pass'),'fail':count('fail'),'skip':count('skipped')}
 except Exception as exc:return {'file':path.name,'exit':-1,'error':str(exc)}
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
 results=list(pool.map(run,files))
summary={'scope':'WIP5_ALL_TESTS_FRESH','files':len(files),'passed_files':sum(r['exit']==0 and r.get('fail')==0 for r in results),
 'failed_files':[r for r in results if r['exit']!=0 or r.get('fail')!=0],
 'tests':sum(r.get('tests') or 0 for r in results),'pass':sum(r.get('pass') or 0 for r in results),
 'fail':sum(r.get('fail') or 0 for r in results),'skip':sum(r.get('skip') or 0 for r in results),
 'duration_seconds':round(time.monotonic()-start,2),'results':results}
(root/'evidence'/'SWE02_FULL_BLOCK_WIP5'/'regression_summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
print(json.dumps({k:v for k,v in summary.items() if k!='results'},ensure_ascii=False,indent=2))
