from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
import subprocess,re,json,time,hashlib,os
root=Path(__file__).resolve().parent
out=root/'evidence/P221';(out/'test-results').mkdir(parents=True,exist_ok=True)
files=sorted((root/'tests').glob('*.test.mjs'))
assert len(files)>=346
start=time.monotonic()
def one(f):
 log=out/'test-results'/f'{f.stem}.tap'
 if os.getenv('P221_FORCE')!='1' and log.exists() and re.search(r'^# fail 0$',log.read_text(),re.M):
  output=log.read_text();code=0
 else:
  r=subprocess.run(['node','--test',str(f)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=90)
  output=r.stdout;code=r.returncode;log.write_text(output)
 def count(k):
  m=re.findall(r'^# '+k+r' (\d+)$',output,re.M)
  return int(m[-1]) if m else None
 return {'file':f.relative_to(root).as_posix(),'exitCode':code,'tests':count('tests'),'passed':count('pass'),'failed':count('fail'),
  'stress':f.name.startswith('stress-'),'log':log.relative_to(root).as_posix()}
results=[]
with ThreadPoolExecutor(max_workers=4) as pool:
 futures={pool.submit(one,f):f for f in files}
 for future in as_completed(futures):
  try:results.append(future.result())
  except Exception as e:results.append({'file':str(futures[future].relative_to(root)),'exitCode':-1,'error':str(e)})
results.sort(key=lambda x:x['file'])
failed=[r for r in results if r.get('exitCode')!=0 or r.get('tests') is None or r.get('failed')!=0 or r.get('tests')!=r.get('passed')]
reg=[r for r in results if not r.get('stress')];stress=[r for r in results if r.get('stress')]
summary={'checkpoint':'P221','mode':'node --test each test file separately, four nonmonolithic workers',
 'regularFiles':len(reg),'stressFiles':len(stress),'regularTests':sum(r.get('tests') or 0 for r in reg),
 'regularPassed':sum(r.get('passed') or 0 for r in reg),'stressTests':sum(r.get('tests') or 0 for r in stress),
 'stressPassed':sum(r.get('passed') or 0 for r in stress),'failedFiles':failed,
 'totalFiles':len(results),'totalTests':sum(r.get('tests') or 0 for r in results),
 'totalPassed':sum(r.get('passed') or 0 for r in results),'focalTests':next((r.get('tests') for r in results if 'uae-p221-arend-patrols' in r['file']),0),
 'elapsedSeconds':round(time.monotonic()-start,2)}
(out/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False))
(out/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False))
print(json.dumps(summary,ensure_ascii=False))
if failed:raise SystemExit(1)
