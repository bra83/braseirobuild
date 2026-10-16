import concurrent.futures,subprocess,os,json,re,time,pathlib,sys
root=pathlib.Path(__file__).resolve().parent;files=sorted((root/'tests').glob('*.test.mjs'))
stress=[f for f in files if f.name.startswith('stress-')];regular=[f for f in files if f not in stress]
print('regular',len(regular),'stress',len(stress),flush=True)
results=[];start=time.monotonic()
def run(p):
 try:
  r=subprocess.run(['node','--test',str(p.relative_to(root))],cwd=root,capture_output=True,text=True,timeout=240)
  output=r.stdout+'\n'+r.stderr
  def metric(key):
   x=re.findall(r'^# '+key+r' (\d+)\s*$',output,re.M)
   return int(x[-1]) if x else None
  return {'file':str(p.relative_to(root)),'exit':r.returncode,'tests':metric('tests'),'passed':metric('pass'),'failed':metric('fail'),'output':output[-6500:] if r.returncode else None}
 except Exception as e:return {'file':str(p.relative_to(root)),'exit':-1,'tests':0,'passed':0,'failed':1,'output':str(e)}
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as ex:
 for i,r in enumerate(ex.map(run,regular),1):
  results.append(r)
  if r['exit']!=0:print('FAIL',r['file'],r['output'][-1300:],flush=True)
  if i%50==0:print('PROGRESS',i,'/',len(regular),'seconds',int(time.monotonic()-start),flush=True)
for p in stress:
 r=run(p);results.append(r);print('STRESS',r['file'],r['exit'],r['passed'],flush=True)
summary={'checkpoint':'P195','mode':'node --test one file per invocation; regular parallel 6; stress sequential; NOT monolithic','regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(x['tests'] or 0 for x in results[:len(regular)]),'regularPassed':sum(x['passed'] or 0 for x in results[:len(regular)]),'stressTests':sum(x['tests'] or 0 for x in results[len(regular):]),'stressPassed':sum(x['passed'] or 0 for x in results[len(regular):]),'failedFiles':[x['file'] for x in results if x['exit']!=0 or x['failed'] or x['passed']!=x['tests']],'elapsedSeconds':round(time.monotonic()-start,1)}
evidence=root/'evidence/P195';evidence.mkdir(parents=True,exist_ok=True);(evidence/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False));(evidence/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False));print(json.dumps(summary,indent=2),flush=True)
