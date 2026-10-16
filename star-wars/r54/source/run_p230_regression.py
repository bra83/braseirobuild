import os,json,subprocess,re,time,concurrent.futures,pathlib,sys
root=pathlib.Path(__file__).parent;os.chdir(root)
out=root/'evidence/P230';(out/'test-results').mkdir(parents=True,exist_ok=True)
files=sorted(p for p in (root/'tests').glob('*.test.mjs'))
regular=[p for p in files if 'stress' not in p.name.lower()];stress=[p for p in files if 'stress' in p.name.lower()]
def run(p):
 start=time.time()
 try:proc=subprocess.run(['node','--test',str(p)],capture_output=True,text=True,timeout=120)
 except subprocess.TimeoutExpired as e:
  data=(e.stdout or b'').decode(errors='replace') if isinstance(e.stdout,bytes) else (e.stdout or '')
  data+='\nTIMEOUT\n';exitcode=124
 else:data=proc.stdout+'\n'+proc.stderr;exitcode=proc.returncode
 (out/'test-results'/f'{p.stem}.tap').write_text(data)
 def get(k):
  m=re.findall(r'^# '+k+r' (\d+)\s*$',data,re.M);return int(m[-1]) if m else 0
 return dict(file=p.name,exit=exitcode,tests=get('tests'),pass_=get('pass'),fail=get('fail'),skipped=get('skipped'),seconds=round(time.time()-start,2))
start=time.time();results=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
 for r in pool.map(run,regular):results.append(r)
for p in stress:results.append(run(p))
results.sort(key=lambda x:x['file']);bad=[r for r in results if r['exit'] or r['fail'] or r['pass_']!=r['tests']]
summary={'checkpoint':'P230','mode':'node --test per file, regular four workers, stress sequential; no monolithic suite','regularFiles':len(regular),'regularTests':sum(x['tests'] for x in results if 'stress' not in x['file']),'regularPassed':sum(x['pass_'] for x in results if 'stress' not in x['file']),'stressFiles':len(stress),'stressTests':sum(x['tests'] for x in results if 'stress' in x['file']),'stressPassed':sum(x['pass_'] for x in results if 'stress' in x['file']),'totalFiles':len(files),'totalTests':sum(x['tests'] for x in results),'totalPassed':sum(x['pass_'] for x in results),'totalSkipped':sum(x['skipped'] for x in results),'failedFiles':[x['file'] for x in bad],'elapsedSeconds':round(time.time()-start,2)}
(out/'PER_FILE_RESULTS.json').write_text(json.dumps([{('pass' if k=='pass_' else k):v for k,v in r.items()} for r in results],indent=2))
(out/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2))
print(json.dumps(summary,indent=2),flush=True)
sys.exit(1 if bad else 0)
