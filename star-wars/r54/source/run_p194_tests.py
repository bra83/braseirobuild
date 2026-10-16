import subprocess,glob,re,json,time,concurrent.futures,pathlib,os
root=pathlib.Path(__file__).parent
files=sorted((root/'tests').glob('*.test.mjs'))
stress=[p for p in files if p.name.startswith('stress-')]
regular=[p for p in files if p not in stress]
print('FILES',len(regular),len(stress),flush=True)
def run(p):
 start=time.time()
 try:
  r=subprocess.run(['node','--test',str(p)],cwd=root,capture_output=True,text=True,timeout=90)
  output=r.stdout+'\n'+r.stderr
  def n(field):
   m=re.findall(r'^# '+field+r' (\d+)$',output,re.M)
   return int(m[-1]) if m else None
  return {'file':str(p.relative_to(root)),'exit':r.returncode,'durationSec':round(time.time()-start,2),'tests':n('tests'),'pass':n('pass'),'fail':n('fail'),'cancelled':n('cancelled'),'skipped':n('skipped'),'tail':output[-1300:] if r.returncode else ''}
 except Exception as e:return {'file':str(p.relative_to(root)),'exit':-1,'durationSec':round(time.time()-start,2),'error':str(e)}
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as ex:results=list(ex.map(run,regular))
print('REGULAR DONE',len(results),'FAILED',[r['file'] for r in results if r['exit']],flush=True)
for p in stress:
 r=run(p);results.append(r);print('STRESS',r['file'],r['exit'],flush=True)
summary={'checkpoint':'P194','executionMode':'node --test one file per invocation; regular parallel 6, stress sequential; NOT monolithic','regularFiles':len(regular),'regularTests':sum(r.get('tests') or 0 for r in results[:len(regular)]),'regularPassed':sum(r.get('pass') or 0 for r in results[:len(regular)]),'stressFiles':len(stress),'stressTests':sum(r.get('tests') or 0 for r in results[len(regular):]),'stressPassed':sum(r.get('pass') or 0 for r in results[len(regular):]),'failedFiles':[r['file'] for r in results if r['exit']]}
summary['totalFiles']=len(results);summary['totalTests']=summary['regularTests']+summary['stressTests'];summary['totalPassed']=summary['regularPassed']+summary['stressPassed']
e=root/'evidence/P194';e.mkdir(exist_ok=True)
(e/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False));(e/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False));print(json.dumps(summary,indent=2),flush=True)
