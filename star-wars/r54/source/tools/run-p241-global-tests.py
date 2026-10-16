from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
import subprocess,re,json,time,sys
root=Path(__file__).resolve().parents[1];files=sorted((root/'tests').glob('*.test.mjs'));out=root/'evidence/P241/test-results';out.mkdir(parents=True,exist_ok=True)
def run(f):
 try:
  cached=out/(f.name+'.tap')
  if cached.exists():
   data=cached.read_text()
   m={k:re.findall(r'^# '+k+r' (\d+)$',data,re.M) for k in ['tests','pass','fail','skip']}
   n={k:int(v[-1]) if v else (0 if k in ['fail','skip'] else None) for k,v in m.items()}
   if n['tests'] is not None and n['pass']==n['tests'] and n['fail']==0 and n['skip']==0:return {'file':f.name,'ok':True,'exit':0,**n}
  r=subprocess.run(['node','--test',str(f)],cwd=root,text=True,capture_output=True,timeout=65)
  data=r.stdout+'\n'+r.stderr
  (out/(f.name+'.tap')).write_text(data)
  m={k:re.findall(r'^# '+k+r' (\d+)$',data,re.M) for k in ['tests','pass','fail','skip']}
  n={k:int(v[-1]) if v else (0 if k in ['fail','skip'] else None) for k,v in m.items()}
  ok=r.returncode==0 and n['tests'] is not None and n['pass']==n['tests'] and n['fail']==0 and n['skip']==0
  return {'file':f.name,'ok':ok,'exit':r.returncode,**n}
 except Exception as e:return {'file':f.name,'ok':False,'error':str(e)}
start=time.monotonic();results=[]
with ThreadPoolExecutor(max_workers=8) as pool:
 for future in as_completed([pool.submit(run,f) for f in files]):results.append(future.result())
results.sort(key=lambda x:x['file']);failed=[r for r in results if not r['ok']];summary={'files':len(files),'elapsed_seconds':round(time.monotonic()-start,2),'passed_files':len(files)-len(failed),'failed_files':len(failed),'tests':sum(x.get('tests') or 0 for x in results),'pass':sum(x.get('pass') or 0 for x in results),'fail':sum(x.get('fail') or 0 for x in results),'skip':sum(x.get('skip') or 0 for x in results),'stress_files':sum(x['file'].startswith('stress') for x in results),'stress_pass':sum(x.get('pass') or 0 for x in results if x['file'].startswith('stress')),'failed':failed}
(root/'evidence/P241/regression_summary.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False)+'\n');print(json.dumps(summary,ensure_ascii=False));sys.exit(bool(failed))
