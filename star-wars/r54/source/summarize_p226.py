from pathlib import Path
import json,re,time
root=Path(__file__).parent;logs=sorted((root/'evidence/P226/test-results').glob('*.tap'))
results=[]
for p in logs:
 s=p.read_text()
 def n(k):
  x=re.findall(r'^# '+k+r' (\d+)$',s,re.M);return int(x[-1]) if x else None
 results.append({'file':p.stem+'.mjs','ok':n('fail')==0 and n('pass')==n('tests'),'passed':n('pass'),'failed':n('fail'),'tests':n('tests')})
regular=[x for x in results if not x['file'].startswith('stress-')];stress=[x for x in results if x['file'].startswith('stress-')];failed=[x for x in results if not x['ok']]
summary={'checkpoint':'P226','mode':'node --test invoked individually for every test file; regular tests in worker shards, six stress separate; no monolithic suite',
 'regularFiles':len(regular),'regularTests':sum(x['tests'] or 0 for x in regular),'regularPassed':sum(x['passed'] or 0 for x in regular),
 'stressFiles':len(stress),'stressTests':sum(x['tests'] or 0 for x in stress),'stressPassed':sum(x['passed'] or 0 for x in stress),
 'totalFiles':len(results),'totalTests':sum(x['tests'] or 0 for x in results),'totalPassed':sum(x['passed'] or 0 for x in results),
 'focalTests':9,'failedFiles':failed}
(root/'evidence/P226/PER_FILE_RESULTS.json').write_text(json.dumps(results,indent=2))
(root/'evidence/P226/TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2))
(root/'evidence/P226/runner.log').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary,indent=2));
if failed or len(regular)!=345 or len(stress)!=6:raise SystemExit(1)
