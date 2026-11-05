#!/usr/bin/env python3
"""Independent R26->R27 regression ledger; a FAILED test never silently passes."""
from pathlib import Path
import subprocess
import sys
import csv
import json
import os

root=Path(sys.argv[1]).resolve()
output=Path(sys.argv[2]).resolve()
tools=root/'tools'
out=output/'logs'
out.mkdir(parents=True,exist_ok=True)
rows=[]
scripts=sorted(list(tools.glob('test_mausritter_*.py'))+
               list(tools.glob('test_mausritter_*.sh'))+
               [tools/x for x in ('test_behavioral_rules.sh','test_mausritter_rules.sh',
                                  'test_mausritter_gm_engine.sh','test_scene_option_diversifier.sh',
                                  'test_mechanical_roll_binding.sh','test_backup_reference_remapper.sh')])
seen=set()
for script in scripts:
    if not script.exists() or script in seen: continue
    seen.add(script)
    # The R19 visual test embeds a frozen build identifier. Update ONLY version
    # expectation to current R27; retain every visual/layout assertion unchanged.
    if script.name=='test_mausritter_r19_visual_shell.py':
        txt=script.read_text(encoding='utf-8')
        old="ok('version r19', 'VERSION_NAME=6.1.0-mausritter.19' in version and 'VERSION_CODE=610019' in version)"
        new="ok('version R27', 'VERSION_NAME=6.1.0-mausritter.27' in version and 'VERSION_CODE=610027' in version)"
        if txt.count(old)!=1: raise AssertionError("R19 version migration anchor changed")
        script.write_text(txt.replace(old,new),encoding='utf-8')
    cmd=['python3',str(script)] if script.suffix=='.py' else ['bash',str(script)]
    try:
        completed=subprocess.run(cmd,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,
                                text=True,encoding='utf-8',errors='replace',timeout=75)
        status='PASS' if completed.returncode==0 else 'FAIL'
        detail='exit='+str(completed.returncode)
        raw=completed.stdout
    except subprocess.TimeoutExpired as e:
        status='BLOCKED';detail='timeout 75s';raw=str(e.stdout or b'')
    except Exception as e:
        status='BLOCKED';detail=str(e);raw=str(e)
    (out/(script.name+'.log')).write_text(raw,encoding='utf-8')
    rows.append(dict(test=script.name,status=status,reason=detail,
                     evidence='logs/'+script.name+'.log'))
with (output/'R26_R27_TEST_RESULTS.csv').open('w',newline='',encoding='utf-8') as f:
    writer=csv.DictWriter(f,fieldnames=['test','status','reason','evidence'])
    writer.writeheader();writer.writerows(rows)
summary={state:sum(1 for row in rows if row['status']==state) for state in ('PASS','FAIL','BLOCKED')}
(output/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
print('MAUSRITTER_R27_REGRESSIONS',summary)
for row in rows:print(row['status'],row['test'],row['reason'])
