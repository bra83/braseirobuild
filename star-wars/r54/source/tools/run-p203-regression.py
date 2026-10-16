from pathlib import Path
import subprocess,re,time,json,concurrent.futures,os
root=Path(__file__).resolve().parent.parent
files=sorted((root/'tests').glob('*.test.mjs'))
stress=[f for f in files if f.name.startswith('stress-')]
regular=[f for f in files if f not in stress]
start=time.time()
def run(p):
    try:
        cp=subprocess.run(['node','--test',str(p.relative_to(root))],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=90)
        out=cp.stdout
        get=lambda k:int(re.findall(r'^# '+k+r' (\d+)$',out,re.M)[-1]) if re.search(r'^# '+k+r' (\d+)$',out,re.M) else 0
        return dict(file=str(p.relative_to(root)),exit=cp.returncode,tests=get('tests'),passed=get('pass'),failed=get('fail'),output=out[-5000:] if cp.returncode else None)
    except Exception as e:return dict(file=str(p.relative_to(root)),exit=1,tests=0,passed=0,failed=1,output=str(e))
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    result=list(ex.map(run,regular))
result+=list(map(run,stress))
e=root/'evidence/P203';e.mkdir(exist_ok=True)
(e/'PER_FILE_RESULTS.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
r=[x for x in result if x['file'] not in [str(f.relative_to(root)) for f in stress]]
s=[x for x in result if x not in r]
summary={'checkpoint':'P203','mode':'node --test per-file; 8 regular workers, 6 stress sequential; NOT monolithic','regularFiles':len(r),'stressFiles':len(s),'regularTests':sum(x['tests'] for x in r),'regularPassed':sum(x['passed'] for x in r),'stressTests':sum(x['tests'] for x in s),'stressPassed':sum(x['passed'] for x in s),'failedFiles':[x['file'] for x in result if x['exit'] or x['failed']],'elapsedSeconds':round(time.time()-start,2),'totalFiles':len(result),'totalTests':sum(x['tests'] for x in result),'totalPassed':sum(x['passed'] for x in result)}
(e/'TEST_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
print(json.dumps(summary,ensure_ascii=False,indent=2))
for x in result:
 if x['exit'] or x['failed']:print('FAILED',x['file'],x['output'][-1800:])
