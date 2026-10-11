#!/usr/bin/env python3
"""R35 real Android UI and production AppDb acceptance; no fake passes."""
import argparse,json,re,sqlite3,subprocess,sys,tempfile,time
from pathlib import Path
import xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument("--adb",required=True);args=p.parse_args()
A=args.adb;PKG="com.braseiro.pathfinder2e";G={};SYSTEM_UI_RETRIES=0
def log(k,x):print(k+"="+str(x),flush=True)
def adb(*parts,allow=False,timeout=48):
    try:r=subprocess.run([A,"-e",*parts],capture_output=True,timeout=timeout)
    except subprocess.TimeoutExpired:raise RuntimeError("ADB_TIMEOUT "+str(parts))
    if r.returncode and not allow:raise RuntimeError("ADB_ERROR "+str(parts)+" "+r.stderr.decode(errors="replace")[:220])
    return r
def shell(*args):return adb("shell",*args).stdout.decode(errors="replace").strip()
def launch():
    shell("am","start","-n",PKG+"/.MainActivity")
    time.sleep(4)
    pid=adb("shell","pidof",PKG,allow=True).stdout.decode().strip()
    if not pid:raise RuntimeError("APP_NOT_RUNNING")
    log("APP_PID",pid)
def nodes():
    # Android's uiautomator can intermittently emit an empty/partial hierarchy
    # while WebView or System UI is drawing. This is a HARNESS retry, never a
    # synthetic success and never a replacement for the real on-device dump.
    last=""
    root=None
    for attempt in range(5):
        dumped=adb("shell","uiautomator","dump","/sdcard/pf2_acceptance.xml",allow=True,timeout=65)
        fetched=adb("exec-out","cat","/sdcard/pf2_acceptance.xml",allow=True,timeout=35)
        raw=fetched.stdout
        at=raw.find(b"<?xml")
        if at<0:at=raw.find(b"<hierarchy")
        if dumped.returncode==0 and fetched.returncode==0 and at>=0:
            try:
                root=ET.fromstring(raw[at:])
                break
            except ET.ParseError as ex:
                last="xml="+str(ex)+" bytes="+repr(raw[:180])
        else:
            last=("dump="+str(dumped.returncode)+" fetch="+str(fetched.returncode)
                +" raw="+repr(raw[:180])+" stderr="+repr(dumped.stderr[:180]))
        log("UI_DUMP_RETRY",str(attempt+1)+" "+last[:250])
        time.sleep(2)
    if root is None:raise RuntimeError("UI_DUMP_UNAVAILABLE_AFTER_5_RETRIES "+last[:350])
    out=[]
    for e in root.iter("node"):
        txt=e.attrib.get("text","").strip() or e.attrib.get("content-desc","").strip()
        m=re.search(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",e.attrib.get("bounds",""))
        if txt and m:
            x1,y1,x2,y2=map(int,m.groups())
            out.append((txt,(x1+x2)//2,(y1+y2)//2,e.attrib.get("class","")))
    global SYSTEM_UI_RETRIES
    if any("isn't responding" in item[0].lower() for item in out):
        if SYSTEM_UI_RETRIES>=3: raise RuntimeError("ANDROID_SYSTEM_UI_REPEATED_ANR")
        wait=next((item for item in out if item[0].casefold()=="wait"),None)
        if wait is None: raise RuntimeError("ANDROID_SYSTEM_UI_ANR_WITHOUT_WAIT")
        SYSTEM_UI_RETRIES+=1
        log("SYSTEM_UI_ANR_DISMISSED",SYSTEM_UI_RETRIES)
        shell("input","tap",str(wait[1]),str(wait[2]))
        time.sleep(12)
        return nodes()
    return out
def click(label,attempts=5,horizontal=False):
    seek=label.casefold()
    for i in range(attempts):
        n=nodes()
        if i==0:log("VISIBLE_FOR_"+label,json.dumps([v[0] for v in n][:35],ensure_ascii=False)[:1300])
        hit=next((v for v in n if v[0].casefold()==seek),None)
        if hit is None:hit=next((v for v in n if seek in v[0].casefold()),None)
        if hit:
            shell("input","tap",str(hit[1]),str(hit[2]))
            time.sleep(2)
            log("CLICK",label);return True
        if i==attempts-1:break
        if horizontal and i<=2:shell("input","swipe","1250","310","260","310","440")
        else:shell("input","swipe","730","2490","730","720","380")
        time.sleep(1)
    log("NOT_FOUND",label);return False
def kingmaker():return click("Kingmaker",6,True)
def snapshot(label):
    directory=Path(tempfile.mkdtemp(prefix="pf2-real-appdb-"))
    for suffix in ("","-wal","-shm"):
        name="braseiro_pathfinder2e.db"+suffix
        r=adb("exec-out","run-as",PKG,"cat","databases/"+name,allow=True,timeout=60)
        if r.returncode==0 and r.stdout:(directory/name).write_bytes(r.stdout)
        elif not suffix:raise RuntimeError("APPDB_UNREADABLE "+r.stderr.decode(errors="replace")[:220])
    db=directory/"braseiro_pathfinder2e.db"
    if not db.read_bytes().startswith(b"SQLite format 3"):raise RuntimeError("INVALID_APPDB_BYTES")
    con=sqlite3.connect("file:"+db.as_posix()+"?mode=ro",uri=True)
    try:
        assert con.execute("PRAGMA quick_check").fetchone()[0]=="ok"
        state=con.execute("select enabled,turn_number,active_turn_id from kingmaker_state limit 1").fetchone()
        tables={row[0] for row in con.execute("select name from sqlite_master where type='table'")}
        counts={t:con.execute("select count(*) from "+t).fetchone()[0] for t in ("kingmaker_turns","kingmaker_armies","kingmaker_war_encounters") if t in tables}
        army=[]
        if "kingmaker_armies" in tables:
            army=con.execute("select name,hp,max_hp from kingmaker_armies").fetchall()
        s={"state":state,"counts":counts,"army":army}
        log("PRODUCTION_APPDB_"+label,json.dumps(s,ensure_ascii=False))
        return s
    finally:con.close()
def stage(name,fn):
    try: G[name]=bool(fn())
    except Exception as ex:G[name]=False;log(name+"_ERROR",repr(ex)[:600])
    log(name,"PASS" if G[name] else "NOT_PROVEN")
def activate():
    if not kingmaker():return False
    if snapshot("BEFORE_ACTIVATION")["state"][0]==1:return True
    if not click("ATIVAR E COMEÇAR A HISTÓRIA"):return False
    if not click("COMEÇAR A HISTÓRIA",2):return False
    return snapshot("AFTER_ACTIVATION")["state"][0]==1
def kingdom_turn():
    if not kingmaker():return False
    n=snapshot("BEFORE_TURN")["counts"].get("kingmaker_turns",0)
    if not click("INICIAR TURNO QUANDO VOLTAR AO REINO",8):
        if not click("INICIAR NOVO TURNO",6):return False
    if not click("INICIAR COM ESTA ROLAGEM",3):return False
    return snapshot("AFTER_TURN")["counts"].get("kingmaker_turns",0)>n
def army():
    if not kingmaker():return False
    if not click("ABRIR ADMINISTRAÇÃO AVANÇADA",7):return False
    if not click("EXÉRCITOS E BATALHAS",10):return False
    if not click("CRIAR EXÉRCITO",2):return False
    n=nodes()
    edits=[v for v in n if v[3].endswith("EditText")]
    if not edits:log("ARMY_FIELDS_MISSING",[v[0] for v in n[:40]]);return False
    shell("input","tap",str(edits[0][1]),str(edits[0][2]))
    shell("input","text","Braseiro_QA_R35")
    shell("input","keyevent","4")
    if not click("SALVAR",3):return False
    return any(x[0]=="Braseiro_QA_R35" for x in snapshot("AFTER_ARMY")["army"])
def restart():
    before=snapshot("BEFORE_RESTART")
    shell("am","force-stop",PKG)
    launch()
    after=snapshot("AFTER_RESTART")
    return before["state"]==after["state"] and before["counts"]==after["counts"] and before["army"]==after["army"]
def main():
    log("TEST_PACKAGE",PKG);launch()
    stage("ACTIVATION_ANDROID_APPDB",activate)
    if G["ACTIVATION_ANDROID_APPDB"]:
        stage("KINGDOM_TURN_ANDROID_APPDB",kingdom_turn)
        stage("ARMY_ANDROID_APPDB",army)
    else:
        G["KINGDOM_TURN_ANDROID_APPDB"]=False
        G["ARMY_ANDROID_APPDB"]=False
    stage("PERSISTENCE_AFTER_PROCESS_RESTART",restart)
    log("FOUR_LIVE_CHECKS",json.dumps(G))
    return 0 if all(G.values()) else 2
if __name__=="__main__":sys.exit(main())
