#!/usr/bin/env python3
from pathlib import Path
import subprocess, re, sys, json, time, concurrent.futures

ROOT = Path(__file__).resolve().parent.parent
FILES = sorted((ROOT / 'tests').glob('*.test.mjs'))
MODE = sys.argv[1] if len(sys.argv)>1 else 'all'
STRESS = [p for p in FILES if re.search(r'(?:^|/)stress(?:-|_)', p.relative_to(ROOT).as_posix(), re.I)]
REGULAR = [p for p in FILES if p not in STRESS]
if MODE == '--regular': STRESS = []
elif MODE == '--stress': REGULAR = []

def run_one(path):
    rel = str(path.relative_to(ROOT))
    started = time.time()
    try:
        # Pin the machine-readable reporter. Node 24 defaults to the
        # human-oriented spec reporter, whose `ℹ tests ...` footer is not
        # parseable by the canonical `# tests ...` summary below.
        result = subprocess.run(['node','--test','--test-reporter=tap',rel], cwd=ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=180)
    except subprocess.TimeoutExpired:
        return {'file':rel,'code':124,'tests':0,'pass':0,'fail':1,'seconds':round(time.time()-started,3),'tail':'TIMEOUT'}
    tap=(result.stdout or '')+(result.stderr or '')
    def count(label):
        m=re.search(rf'^# {label} (\d+)', tap, re.M)
        return int(m.group(1)) if m else None
    tests=count('tests'); passed=count('pass'); failed=count('fail'); skipped=count('skipped') or 0
    bad = result.returncode != 0 or tests is None or passed is None or failed is None or failed != 0 or (passed + skipped) != tests
    return {'file':rel,'code':result.returncode,'tests':tests or 0,'pass':passed or 0,'fail':failed if failed is not None else (1 if bad else 0),'skipped':skipped,'seconds':round(time.time()-started,3),'tail':tap[-5000:] if bad else ''}

started=time.time(); results=[]
# Regular tests are safe to parallelize and complete quickly.
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
    results.extend(pool.map(run_one, REGULAR))
# Stress suites are deliberately sequential to avoid host contention/false timeouts.
for path in STRESS:
    results.append(run_one(path))
results.sort(key=lambda x:x['file'])
summary={
    'files':len(results),
    'regular_files':len(REGULAR),
    'stress_files':len(STRESS),
    'tests':sum(r['tests'] for r in results),
    'pass':sum(r['pass'] for r in results),
    'fail':sum(r['fail'] for r in results),
    'skipped':sum(r.get('skipped',0) for r in results),
    'bad':sum(1 for r in results if r['code']!=0 or r['fail']!=0 or (r['pass']+r.get('skipped',0))!=r['tests']),
    'seconds':round(time.time()-started,3),
}
(ROOT/'evidence/P198/PER_FILE_RESULTS.json').write_text(json.dumps({'summary':summary,'results':results},ensure_ascii=False,indent=2)+'\n')
print('NODE_SUITE_JSON='+json.dumps(summary,separators=(',',':')))
print(f'# tests {summary["tests"]}')
print(f'# pass {summary["pass"]}')
print(f'# fail {summary["fail"]}')
print(f'# skipped {summary["skipped"]}')
if summary['bad']:
    for r in results:
        if r['code']!=0 or r['fail']!=0 or (r['pass']+r.get('skipped',0))!=r['tests']:
            print(f'=== {r["file"]} ===',file=sys.stderr); print(r['tail'],file=sys.stderr)
    sys.exit(1)
sys.exit(0)
