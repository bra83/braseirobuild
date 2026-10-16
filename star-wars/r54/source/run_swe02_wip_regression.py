import concurrent.futures,subprocess,pathlib,re,json,time,os
root=pathlib.Path(__file__).resolve().parent
files=sorted((root/'tests').glob('*.test.mjs'))
logs=root/'evidence'/'SWE02_FULL_BLOCK_WIP'/'test-results';logs.mkdir(parents=True,exist_ok=True)
start=time.time()
def run(path):
    cached=logs/(path.stem+'.tap')
    if cached.exists() and '# tests ' in cached.read_text() and '# pass ' in cached.read_text():
        t=cached.read_text()
        def cachedval(name):
            m=re.findall(r'^# '+re.escape(name)+r' (\d+)$',t,re.M)
            return int(m[-1]) if m else None
        return {'file':path.name,'exit':0 if cachedval('fail')==0 else 1,'pass':cachedval('pass'),'fail':cachedval('fail'),'skip':cachedval('skipped'),'tests':cachedval('tests')}
    proc=subprocess.run(['node','--test',str(path)],cwd=root,text=True,capture_output=True,timeout=180)
    log=logs/(path.stem+'.tap');log.write_text(proc.stdout+proc.stderr)
    text=proc.stdout+proc.stderr
    def val(name):
        m=re.findall(r'^# '+re.escape(name)+r' (\d+)$',text,re.M)
        return int(m[-1]) if m else None
    return {'file':path.name,'exit':proc.returncode,'pass':val('pass'),'fail':val('fail'),'skip':val('skipped'),'tests':val('tests')}
with concurrent.futures.ThreadPoolExecutor(max_workers=16) as ex:
    futures={ex.submit(run,p):p for p in files}
    results=[]
    for future in concurrent.futures.as_completed(futures):
        try:results.append(future.result())
        except Exception as e:results.append({'file':futures[future].name,'exit':-1,'error':str(e)})
results.sort(key=lambda x:x['file'])
summary={'scope':'SWE02_FULL_BLOCK_WORK_IN_PROGRESS','files':len(files),'passed_files':sum(x.get('exit')==0 for x in results),
 'failed_files':[x for x in results if x.get('exit')!=0 or x.get('fail') is None],
 'tests':sum(x.get('tests') or 0 for x in results),'pass':sum(x.get('pass') or 0 for x in results),
 'fail':sum(x.get('fail') or 0 for x in results),'skip':sum(x.get('skip') or 0 for x in results),
 'duration_seconds':round(time.time()-start,2),'results':results}
(root/'evidence'/'SWE02_FULL_BLOCK_WIP'/'regression_summary.json').write_text(json.dumps(summary,indent=2,ensure_ascii=False))
print(json.dumps({k:v for k,v in summary.items() if k!='results'},indent=2,ensure_ascii=False))
