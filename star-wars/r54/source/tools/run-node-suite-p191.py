#!/usr/bin/env python3
"""P191 resumable, per-file Node test runner. Never declares success on timeout."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
import subprocess,re,json,sys,time,hashlib,argparse
p=argparse.ArgumentParser();p.add_argument('--shard',type=int,default=0);p.add_argument('--shards',type=int,default=1);p.add_argument('--workers',type=int,default=3);p.add_argument('--timeout',type=int,default=65);p.add_argument('--stress',action='store_true');p.add_argument('--resume',action='store_true');args=p.parse_args()
root=Path(__file__).resolve().parent.parent
out=root/'evidence/P191/test-results';out.mkdir(parents=True,exist_ok=True)
files=sorted((root/'tests').glob('*.test.mjs'))
files=[x for x in files if (x.name.lower().startswith('stress') or 'stress' in x.name.lower())==args.stress]
files=[x for i,x in enumerate(files) if i%args.shards==args.shard]
def run(path):
 previous=out/(path.name+'.json')
 if args.resume and previous.exists():
  try:
   cached=json.loads(previous.read_text())
   if cached.get('okay') and cached.get('file')==path.name:return cached
  except (ValueError,OSError):pass
 start=time.monotonic()
 try:
  result=subprocess.run(['node','--test','--test-reporter=tap',str(path.relative_to(root))],cwd=root,text=True,capture_output=True,timeout=args.timeout)
  output=result.stdout+'\n'+result.stderr
  nums={k:int(m.group(1)) if (m:=re.search(r'^# '+k+r' (\d+)',output,re.M)) else None for k in ['tests','pass','fail','skipped']}
  okay=result.returncode==0 and nums['tests'] is not None and nums['pass'] is not None and nums['fail']==0 and nums['pass']+(nums['skipped'] or 0)==nums['tests']
  r={'file':path.name,'okay':okay,**nums,'exit':result.returncode,'seconds':round(time.monotonic()-start,2),'tail':output[-4000:] if not okay else ''}
 except subprocess.TimeoutExpired:
  r={'file':path.name,'okay':False,'tests':0,'pass':0,'fail':1,'skipped':0,'exit':'TIMEOUT','seconds':round(time.monotonic()-start,2),'tail':'TIMEOUT'}
 (out/(path.name+'.json')).write_text(json.dumps(r,ensure_ascii=False,indent=2))
 return r
with ThreadPoolExecutor(max_workers=args.workers) as pool:
 futures=[pool.submit(run,x) for x in files]
 results=[]
 for f in as_completed(futures):
  r=f.result();results.append(r)
  if len(results)%10==0: print('PROGRESS',args.shard,len(results),'/',len(files),'bad',sum(not x['okay'] for x in results),flush=True)
summary={'shard':args.shard,'shards':args.shards,'files':len(results),'tests':sum(x['tests'] or 0 for x in results),'passed':sum(x['pass'] or 0 for x in results),'failed':sum(x['fail'] or 0 for x in results),'badFiles':[x['file'] for x in results if not x['okay']]}
(out/f'shard_{args.shard}_{args.shards}_{"stress" if args.stress else "regular"}.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False))
print('SHARD_JSON='+json.dumps(summary),flush=True)
sys.exit(bool(summary['badFiles']))
