"""Re-run only failures after fixes, then aggregate independently verified per-file results."""
import json,re,subprocess,sys,time
from pathlib import Path
root=Path(__file__).resolve().parent.parent
out=root/'evidence/P181/test-results'
for name in ['r54-p74-campaign-lifecycle-insets-version.test.mjs','r54-p77-android-build-fix.test.mjs']:
 t=time.monotonic()
 r=subprocess.run(['node','--test','--test-reporter=tap','tests/'+name],cwd=root,text=True,capture_output=True,timeout=90)
 fields={k:int(m.group(1)) if (m:=re.search(r'^# '+k+r' (\d+)',r.stdout,re.M)) else None for k in ['tests','pass','fail','skipped']}
 okay=r.returncode==0 and fields['fail']==0 and fields['pass']+fields['skipped']==fields['tests']
 row={'file':name,'okay':okay,**fields,'exit':r.returncode,'seconds':round(time.monotonic()-t,2),'tail':r.stdout[-4000:] if not okay else ''}
 (out/(name+'.json')).write_text(json.dumps(row,indent=2))
 if not okay: print(row);sys.exit(1)
files=sorted((root/'tests').glob('*.test.mjs'))
rows=[json.loads((out/(p.name+'.json')).read_text()) for p in files]
bad=[r['file'] for r in rows if not r['okay']]
summary={'checkpoint':'P181','method':'per-file regression; two failed version tests corrected and rerun individually',
 'files':len(rows),'tests':sum(r['tests'] for r in rows),'passed':sum(r['pass'] for r in rows),
 'failed':sum(r['fail'] for r in rows),'badFiles':bad,
 'regularFiles':sum('stress' not in r['file'].lower() for r in rows),
 'stressFiles':sum('stress' in r['file'].lower() for r in rows)}
(root/'evidence/P181/TEST_SUMMARY.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False))
print(json.dumps(summary,ensure_ascii=False))
sys.exit(bool(bad))
