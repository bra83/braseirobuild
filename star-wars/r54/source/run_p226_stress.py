from pathlib import Path
import subprocess,concurrent.futures,json,re,time
root=Path(__file__).parent;paths=sorted((root/'tests').glob('stress-*.test.mjs'));out=root/'evidence/P226/test-results'
def run(p):
 start=time.monotonic();r=subprocess.run(['node','--test',str(p.relative_to(root))],cwd=root,capture_output=True,text=True,timeout=150)
 s=r.stdout+'\n'+r.stderr;(out/(p.stem+'.tap')).write_text(s)
 match=lambda x:int(re.findall(r'^# '+x+r' (\d+)$',s,re.M)[-1]) if re.findall(r'^# '+x+r' (\d+)$',s,re.M) else 0
 return {'file':p.name,'exit':r.returncode,'tests':match('tests'),'pass':match('pass'),'fail':match('fail'),'seconds':round(time.monotonic()-start,2)}
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:res=list(pool.map(run,paths))
report={'stressFiles':len(res),'stressTests':sum(x['tests'] for x in res),'stressPassed':sum(x['pass'] for x in res),'failed':[x for x in res if x['exit'] or x['fail']]}
(root/'evidence/P226/STRESS_RESULTS.json').write_text(json.dumps({'summary':report,'files':res},indent=2));print(json.dumps(report,indent=2))
if report['failed']:raise SystemExit(1)
