import concurrent.futures,subprocess,time,re,json,json as js,os
from pathlib import Path
root=Path(__file__).parent
files=sorted((root/'tests').glob('*.test.mjs'))
regular=[p for p in files if not p.name.startswith('stress-')]
stress=[p for p in files if p.name.startswith('stress-')]
results=[]
def run(p):
    start=time.monotonic()
    try:
        cp=subprocess.run(['node','--test',str(p.relative_to(root))],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=160)
        out=cp.stdout
        # Final TAP summary: exact last line '# tests N', '# pass N', '# fail N'
        vals={k:int(re.findall(r'^# '+k+r' (\d+)\s*$',out,re.M)[-1]) if re.findall(r'^# '+k+r' (\d+)\s*$',out,re.M) else -1 for k in ['tests','pass','fail']}
        failed=cp.returncode!=0 or vals['fail']!=0 or vals['tests']<1 or vals['tests']!=vals['pass']
        return {'file':p.name,'exitCode':cp.returncode,'tests':vals['tests'],'passed':vals['pass'],'failed':vals['fail'],'elapsedSeconds':round(time.monotonic()-start,2),'error':out[-2500:] if failed else None}
    except Exception as e:
        return {'file':p.name,'exitCode':-1,'tests':0,'passed':0,'failed':1,'elapsedSeconds':round(time.monotonic()-start,2),'error':str(e)}
start=time.monotonic()
with concurrent.futures.ThreadPoolExecutor(max_workers=12) as pool:
    for i,r in enumerate(pool.map(run,regular),1):
        results.append(r)
        if i%35==0:print('regular',i,'/',len(regular),'fails',len([x for x in results if x['failed']]),flush=True)
for p in stress:
    r=run(p);results.append(r);print('stress',p.name,'exit',r['exitCode'],'tests',r['tests'],flush=True)
summary={'checkpoint':'P214','mode':'node --test per-file; 12 regular workers, stress sequential; NOT monolithic',
 'regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(x['tests'] for x in results[:len(regular)]),
 'regularPassed':sum(x['passed'] for x in results[:len(regular)]),'stressTests':sum(x['tests'] for x in results[len(regular):]),
 'stressPassed':sum(x['passed'] for x in results[len(regular):]),
 'failedFiles':[x for x in results if x['failed'] or x['exitCode']],'totalFiles':len(results),
 'totalTests':sum(x['tests'] for x in results),'totalPassed':sum(x['passed'] for x in results),
 'focalTests':4,'elapsedSeconds':round(time.monotonic()-start,2)}
evidence=root/'evidence/P214';evidence.mkdir(exist_ok=True)
(evidence/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False)+'\n')
(evidence/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False)+'\n')
print(json.dumps({k:v for k,v in summary.items() if k!='failedFiles'},indent=2));print('FAILED',summary['failedFiles'])
