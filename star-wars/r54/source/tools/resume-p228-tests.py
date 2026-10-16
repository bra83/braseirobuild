import subprocess,json,re,time
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
root=Path(__file__).resolve().parents[1];logdir=root/'evidence/P228/test-results';files=sorted((root/'tests').glob('*.test.mjs'))
def run(p):
 try:
  r=subprocess.run(['node','--test',str(p)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=45,encoding='utf-8',errors='replace');out=r.stdout;code=r.returncode
 except subprocess.TimeoutExpired as e:
  out=(e.stdout or b'').decode(errors='replace') if isinstance(e.stdout,bytes) else (e.stdout or '');code=124
 (logdir/(p.name+'.tap')).write_text(out)
 return p.name,code
missing=[p for p in files if not (logdir/(p.name+'.tap')).exists()]
print('MISSING',len(missing),flush=True)
with ThreadPoolExecutor(max_workers=8) as ex:
 fut=[ex.submit(run,p) for p in missing]
 for f in as_completed(fut):
  name,code=f.result();print('DONE',name,code,flush=True)
results=[]
for p in files:
 out=(logdir/(p.name+'.tap')).read_text();match=dict(re.findall(r'^# (tests|pass|fail|skipped) (\d+)\s*$',out,re.M));r={'file':p.name,'tests':int(match.get('tests',-1)),'pass':int(match.get('pass',-1)),'fail':int(match.get('fail',-1)),'skipped':int(match.get('skipped',0))};results.append(r)
failed=[x for x in results if x['tests']<0 or x['fail'] or x['pass']+x['skipped']!=x['tests']]
reg=[x for x in results if not x['file'].startswith('stress-')];stress=[x for x in results if x['file'].startswith('stress-')]
summary={'checkpoint':'P228','mode':'each test file separately in node --test; no monolithic suite','regularFiles':len(reg),'regularTests':sum(x['tests'] for x in reg),'regularPassed':sum(x['pass'] for x in reg),'stressFiles':len(stress),'stressTests':sum(x['tests'] for x in stress),'stressPassed':sum(x['pass'] for x in stress),'totalFiles':len(results),'totalTests':sum(x['tests'] for x in results),'totalPassed':sum(x['pass'] for x in results),'totalSkipped':sum(x['skipped'] for x in results),'failedFiles':failed}
evidence=root/'evidence/P228';(evidence/'TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False));(evidence/'PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2,ensure_ascii=False));print('SUMMARY',json.dumps(summary,ensure_ascii=False),flush=True)
if failed:raise SystemExit(1)
