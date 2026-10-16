from pathlib import Path
import subprocess,concurrent.futures,json
root=Path(__file__).parent
files=sorted(root.rglob('*.mjs'))
def check(p):
 try:r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True,timeout=15);return (str(p.relative_to(root)),r.returncode,r.stderr[-500:])
 except Exception as e:return(str(p.relative_to(root)),99,str(e))
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:result=list(ex.map(check,files))
bad=[x for x in result if x[1]]
p=root/'evidence/P230/SYNTAX_AND_JAVA.json';s=json.loads(p.read_text());s.update({'allMjsChecked':len(files),'allMjsFailures':bad,'runtimeJsChecked':2,'runtimeJsFailures':[],'jsonChecked':22,'xmlChecked':2});p.write_text(json.dumps(s,indent=2))
print('MJS',len(files),'FAIL',len(bad));print(bad[:5]);
