import concurrent.futures,subprocess,time,re,json,sys
from pathlib import Path
root=Path(__file__).parent
files=sorted((root/'tests').glob('*.test.mjs'))
regular=[p for p in files if not p.name.startswith('stress-')]
stress=[p for p in files if p.name.startswith('stress-')]
shard=int(sys.argv[1]); all_files=regular+stress
part=[p for i,p in enumerate(all_files) if i%4==shard]
results=[]
def run(p):
 start=time.monotonic()
 try:
  cp=subprocess.run(['node','--test',str(p.relative_to(root))],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=35)
  out=cp.stdout
  vals={k:int(re.findall(r'^# '+k+r' (\d+)\s*$',out,re.M)[-1]) if re.findall(r'^# '+k+r' (\d+)\s*$',out,re.M) else -1 for k in ['tests','pass','fail']}
  failed=cp.returncode!=0 or vals['fail']!=0 or vals['tests']<1 or vals['tests']!=vals['pass']
  return {'file':p.name,'exitCode':cp.returncode,'tests':vals['tests'],'passed':vals['pass'],'failed':vals['fail'],'elapsedSeconds':round(time.monotonic()-start,2),'error':out[-4000:] if failed else None}
 except Exception as e:return {'file':p.name,'exitCode':-1,'tests':0,'passed':0,'failed':1,'elapsedSeconds':round(time.monotonic()-start,2),'error':str(e)}
start=time.monotonic()
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
 for r in pool.map(run,part):
  results.append(r)
  if r['failed'] or r['exitCode']:print('FAIL',r['file'],r['error'],flush=True)
  if len(results)%20==0:print('SHARD',shard,len(results),'/',len(part),flush=True)
out=root/f'evidence/P215/shard-{shard}.json';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
print('SHARD COMPLETE',shard,len(results),'tests',sum(r['tests'] for r in results),'passed',sum(r['passed'] for r in results),'failedFiles',sum(bool(r['failed'] or r['exitCode']) for r in results),'seconds',round(time.monotonic()-start,1))
