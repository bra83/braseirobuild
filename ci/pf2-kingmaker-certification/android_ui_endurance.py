#!/usr/bin/env python3
"""R35 real Android UI and production AppDb acceptance; no fake passes."""
import argparse,hashlib,json,re,sqlite3,subprocess,sys,tempfile,time
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
        # Never accept a previous dump after uiautomator reports a null root.
        adb("shell","rm","-f","/sdcard/pf2_acceptance.xml",timeout=15)
        dumped=adb("shell","uiautomator","dump","/sdcard/pf2_acceptance.xml",allow=True,timeout=65)
        fetched=adb("exec-out","cat","/sdcard/pf2_acceptance.xml",allow=True,timeout=35)
        raw=fetched.stdout
        at=raw.find(b"<?xml")
        if at<0:at=raw.find(b"<hierarchy")
        if dumped.returncode==0 and b"ERROR:" not in dumped.stderr and fetched.returncode==0 and at>=0:
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
        cls=e.attrib.get("class","")
        # Unfilled EditTexts have no text/content-desc. Preserve them so
        # the real Android army form can be populated instead of false-failing.
        if m and (txt or cls.endswith("EditText")):
            x1,y1,x2,y2=map(int,m.groups())
            out.append((txt,(x1+x2)//2,(y1+y2)//2,cls,
                        e.attrib.get("clickable","false")=="true"))
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
def viewport():
    out=shell("wm","size")
    m=re.search(r"(\d+)x(\d+)",out)
    if not m:raise RuntimeError("SCREEN_DIMENSIONS_UNAVAILABLE "+repr(out))
    return int(m.group(1)),int(m.group(2))

def scroll_content():
    # Stay strictly inside the document body. The previous swipe could begin
    # outside a 2400-pixel screen and navigate to unrelated content.
    width,height=viewport()
    x=width//2
    shell("input","swipe",str(x),str(int(height*.78)),
          str(x),str(int(height*.34)),"420")
    time.sleep(1)

def start_confirmation_visible(items):
    labels={item[0] for item in items}
    return "Começar Kingmaker solo" in labels and "COMEÇAR A HISTÓRIA" in labels

def close_stale_confirmation():
    # Dismiss only after the AppDb commit. Never cancel an uncommitted start.
    for attempt in range(3):
        current=nodes()
        if not start_confirmation_visible(current):return True
        if snapshot("MODAL_STATE")["state"][0]!=1:
            log("START_CONFIRMATION_UNCOMMITTED","cannot dismiss")
            return False
        log("DISMISS_STALE_START_CONFIRMATION",attempt+1)
        # The Back gesture sent a real emulator to Android HOME in run 38181911106.
        # The confirmation is already committed to AppDb, so dismiss the visible
        # modal with its own CANCELAR button, not Android navigation.
        cancel=next((v for v in current if v[0].casefold()=="cancelar" and v[4]),None)
        if not cancel:
            log("STALE_MODAL_CANCEL_MISSING","no blind back gesture")
            return False
        shell("input","tap",str(cancel[1]),str(cancel[2]))
        time.sleep(3)
    return not start_confirmation_visible(nodes())

def kingmaker():
    if not close_stale_confirmation():return False
    for attempt in range(5):
        current=nodes()
        labels={item[0] for item in current}
        # These are content labels, not the persistent navigation tab.
        if ("KINGMAKER SOLO" in labels or "CONFIGURAÇÃO AVANÇADA DO REINO" in labels
                or "ADMINISTRAÇÃO AVANÇADA" in labels):
            log("KINGMAKER_PAGE_CONFIRMED",attempt+1)
            return True
        tabs=[item for item in current if item[0]=="KINGMAKER"]
        if not tabs:
            log("KINGMAKER_TAB_MISSING",json.dumps(sorted(labels)[:35],ensure_ascii=False))
            pid=adb("shell","pidof",PKG,allow=True).stdout.decode().strip()
            log("KINGMAKER_APP_PID_ON_NAV_FAILURE",pid or "APP_NOT_RUNNING")
            if pid and "Apps list" in labels:
                log("ANDROID_HOME_AFTER_NAV","restore existing application process")
                launch()
                continue
            return False
        tab=min(tabs,key=lambda item:item[2])
        shell("input","tap",str(tab[1]),str(tab[2]))
        time.sleep(3)
    log("KINGMAKER_PAGE_NOT_REACHED",json.dumps([x[0] for x in nodes()][:35],ensure_ascii=False))
    return False

def click(label,attempts=5,horizontal=False):
    seek=label.casefold()
    for i in range(attempts):
        n=nodes()
        if i==0:log("VISIBLE_FOR_"+label,json.dumps([v[0] for v in n][:35],ensure_ascii=False)[:1300])
        matches=[v for v in n if v[0].casefold()==seek]
        hit=next((v for v in matches if v[4]),None) or next(iter(matches),None)
        # Prefer actionable accessibility nodes over identically named headings.
        if hit:
            width,height=viewport()
            if 0<=hit[1]<width and 0<=hit[2]<height:
                shell("input","tap",str(hit[1]),str(hit[2]))
                time.sleep(2)
                log("CLICK",label);return True
            log("OUT_OF_VIEW_ANDROID_NODE",label+" x="+str(hit[1])+" y="+str(hit[2]))
        if i==attempts-1:break
        if (horizontal or start_confirmation_visible(n)
                or any(v[0]=="Configurações" for v in n)):
            log("SCROLL_BLOCKED_WRONG_CONTEXT",label)
            break
        scroll_content()
        time.sleep(1)
    log("NOT_FOUND",label);return False
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
        if con.execute("PRAGMA quick_check").fetchone()[0]!="ok":
            raise RuntimeError("APPDB_QUICK_CHECK_FAILED")
        violations=con.execute("PRAGMA foreign_key_check").fetchall()
        if violations:raise RuntimeError("APPDB_FOREIGN_KEY_VIOLATIONS "+repr(violations[:5]))
        columns=[row[1] for row in con.execute('PRAGMA table_info("kingmaker_state")')]
        kingdom_rows=con.execute('select * from kingmaker_state order by rowid').fetchall()
        # updated_at is a write timestamp; all actual kingdom state remains hashed.
        kingdom_values=[[value for column,value in zip(columns,row) if column!="updated_at"]
                        for row in kingdom_rows]
        kingdom_sha256=hashlib.sha256(json.dumps(kingdom_values,ensure_ascii=False,
                           default=str).encode("utf-8")).hexdigest()
        state=con.execute("select enabled,turn_number,active_turn_id from kingmaker_state limit 1").fetchone()
        tables={row[0] for row in con.execute("select name from sqlite_master where type='table'")}
        counts={t:con.execute("select count(*) from "+t).fetchone()[0] for t in ("kingmaker_turns","kingmaker_armies","kingmaker_war_encounters") if t in tables}
        army=[]
        if "kingmaker_armies" in tables:
            army=con.execute("select name,hp,max_hp from kingmaker_armies").fetchall()
        row_sha256={}
        # A static count is not sufficient to prove saves: hash every real
        # column in real nonempty production tables, including turn and war.
        for table in ("kingmaker_turns","kingmaker_armies","kingmaker_war_encounters"):
            if table not in tables:
                continue
            rows=con.execute('select * from "'+table+'" order by rowid').fetchall()
            encoded=json.dumps(rows,ensure_ascii=False,sort_keys=True,default=str).encode("utf-8")
            row_sha256[table]=hashlib.sha256(encoded).hexdigest()
        turn_details=[dict(zip(("id","phase","status","resource_roll_json"),row)) for row in
                      con.execute("select id,phase,status,resource_roll_json from kingmaker_turns order by id")]
        war_details=[dict(zip(("encounter_key","name","status","log_json"),row)) for row in
                     con.execute("select encounter_key,name,status,log_json from kingmaker_war_encounters order by id")]
        s={"state":state,"counts":counts,"army":army,"row_sha256":row_sha256,
           "kingdom_state_sha256":kingdom_sha256,"turn_details":turn_details,"war_details":war_details}
        log("PRODUCTION_APPDB_"+label,json.dumps(s,ensure_ascii=False))
        return s
    finally:con.close()
def stage(name,fn):
    try: G[name]=bool(fn())
    except Exception as ex:G[name]=False;log(name+"_ERROR",repr(ex)[:600])
    log(name,"PASS" if G[name] else "NOT_PROVEN")
    if not G[name]:
        try: log(name+"_VISIBLE_AT_FAILURE",
                 json.dumps([v[0] for v in nodes()][:55],ensure_ascii=False)[:2000])
        except Exception as diag:log(name+"_UI_DIAGNOSTIC_ERROR",repr(diag)[:250])
        try:
            activity=shell("dumpsys","activity","activities")
            log(name+"_ACTIVITY_FOREGROUND"," ".join(
                x.strip() for x in activity.splitlines() if "mResumedActivity" in x or "topResumedActivity" in x)[:800])
            fatal=adb("logcat","-d","-s","AndroidRuntime:E",allow=True,timeout=30).stdout.decode(errors="replace")
            log(name+"_ANDROID_RUNTIME_FATAL",fatal[-1800:] if fatal.strip() else "none")
        except Exception as diag:log(name+"_LOGCAT_DIAGNOSTIC_ERROR",repr(diag)[:250])
def activate():
    if snapshot("BEFORE_ACTIVATION")["state"][0]==1:return True
    # A valid entry point may already have opened the uncommitted confirmation.
    # Confirm the observed dialog rather than looking for a background button.
    current=nodes()
    if not start_confirmation_visible(current):
        labels={item[0] for item in current}
        entry=next((label for label in ("ATIVAR E COMEÇAR A HISTÓRIA","ATIVAR KINGMAKER SOLO")
                    if label in labels),None)
        if entry is None:
            if not kingmaker():return False
            current=nodes()
            if not start_confirmation_visible(current):
                labels={item[0] for item in current}
                entry=next((label for label in ("ATIVAR E COMEÇAR A HISTÓRIA","ATIVAR KINGMAKER SOLO")
                            if label in labels),None)
        if not start_confirmation_visible(current):
            if entry is None or not click(entry,1):return False
    if not start_confirmation_visible(nodes()):
        log("START_CONFIRMATION_MISSING","no blind confirmation click")
        return False
    if not click("COMEÇAR A HISTÓRIA",2):return False
    activated=snapshot("AFTER_ACTIVATION")["state"][0]==1
    if activated:
        try:
            if not close_stale_confirmation():
                log("ACTIVATED_MODAL_NAVIGATION_PENDING","turn UI stage must prove navigation")
        except RuntimeError as ex:
            # Activation is already proven by production AppDb. A transient
            # accessibility failure belongs to the subsequent navigation gate.
            log("ACTIVATED_UI_NAVIGATION_PENDING",str(ex)[:350])
    return activated
def kingdom_turn():
    if not kingmaker():return False
    before=snapshot("BEFORE_TURN")
    n=before["counts"].get("kingmaker_turns",0)
    # The solo guided button may correctly gate turns until kingdom founding.
    # Use the actual technical Kingdom admin route for an isolated engine gate.
    visible={x[0] for x in nodes()}
    if "ADMINISTRAÇÃO AVANÇADA" not in visible:
        if not click("ABRIR ADMINISTRAÇÃO AVANÇADA",16):return False
        if "ADMINISTRAÇÃO AVANÇADA" not in {x[0] for x in nodes()}:
            log("ADMIN_MODE_NOT_ENTERED","advanced button did not switch view")
            return False
    if before["state"][2]:
        log("RESUME_PERSISTED_TURN",before["state"][2])
        started=before
    else:
        if not click("INICIAR NOVO TURNO",8):return False
        if not click("INICIAR COM ESTA ROLAGEM",3):return False
        started=snapshot("AFTER_TURN")
        if started["counts"].get("kingmaker_turns",0)<=n:return False
    turn_id=started["state"][2]
    # Complete each real production phase; creating a row alone is not a turn.
    for step in range(8):
        current=snapshot("TURN_PHASE_"+str(step))
        turn=next((row for row in current["turn_details"] if row["id"]==turn_id),None)
        if turn is None:return False
        if turn["status"]=="complete":
            return current["state"][2]==0 and json.loads(turn["resource_roll_json"]).get("consumption_resolved") is True
        if turn["phase"]=="UPKEEP" and not json.loads(turn["resource_roll_json"]).get("consumption_resolved"):
            if not click("RESOLVER CONSUMO",8) or not click("CONFIRMAR ESCOLHA",2):return False
        phase_button=None
        for attempt in range(8):
            phase_button=next((item[0] for item in nodes()
                              if item[0].startswith("CONCLUIR ") and item[0].endswith(" E AVANÇAR")),None)
            if phase_button:break
            scroll_content()
        if phase_button is None or not click(phase_button,1):return False
    log("COMPLETE_KINGDOM_TURN_NOT_PROVEN","phase limit reached")
    return False
def create_army(name,allegiance):
    if not kingmaker():return False
    if "ADMINISTRAÇÃO AVANÇADA" not in {x[0] for x in nodes()}:
        if not click("ABRIR ADMINISTRAÇÃO AVANÇADA",16):return False
    if not click("EXÉRCITOS E BATALHAS",12):return False
    if not click("CRIAR EXÉRCITO",2):return False
    n=nodes()
    edits=[v for v in n if v[3].endswith("EditText")]
    if not edits:
        log("ARMY_FIELDS_MISSING",[v[0] for v in n[:40]])
        return False
    # Production form order comes from MainActivity:
    # Name, Type, Allegiance, Level...; no database injection.
    shell("input","tap",str(edits[0][1]),str(edits[0][2]))
    shell("input","text",name)
    if name not in {item[0] for item in nodes() if item[3].endswith("EditText")}:
        log("ARMY_NAME_INPUT_NOT_VERIFIED",name)
        return False
    if allegiance!="kingdom":
        current=nodes()
        edits=[v for v in current if v[3].endswith("EditText")]
        allegiance_field=next((v for v in edits if v[0]=="kingdom"),None)
        if allegiance_field is None:
            log("ARMY_ALLEGIANCE_FIELD_MISSING",[v[0] for v in edits[:8]])
            return False
        shell("input","tap",str(allegiance_field[1]),str(allegiance_field[2]))
        shell("input","keyevent","123") # KEYCODE_MOVE_END: clear known value fully
        # Replace only the known initial 7-character value, then verify it.
        for _ in range(7):shell("input","keyevent","67")
        shell("input","text",allegiance)
        changed={v[0] for v in nodes() if v[3].endswith("EditText")}
        if allegiance not in changed:
            log("ARMY_ALLEGIANCE_NOT_SET",sorted(changed)[:10])
            return False
    # Never send KEYCODE_BACK blindly: an earlier emulator ended on HOME.
    saved=False
    for label in ("SALVAR EXÉRCITO","SALVAR","REGISTRAR EXÉRCITO"):
        if label in {x[0] for x in nodes()} and click(label,2):
            saved=True
            break
    if not saved:
        log("ARMY_SAVE_CONTROL_MISSING","positive action not detected in real dialog")
        return False
    return any(x[0]==name for x in snapshot("AFTER_ARMY_"+name)["army"])
def army():
    before=snapshot("BEFORE_ARMY")["counts"].get("kingmaker_armies",0)
    if not create_army("qaplayer","kingdom"):return False
    if not create_army("qaenemy","enemy"):return False
    after=snapshot("AFTER_OPPOSING_ARMIES")
    expected={"qaplayer","qaenemy"}
    names={x[0] for x in after["army"]}
    return after["counts"].get("kingmaker_armies",0)>=before+2 and expected.issubset(names)

def war():
    if not kingmaker():return False
    before=snapshot("BEFORE_WAR")["counts"].get("kingmaker_war_encounters",0)
    if "ADMINISTRAÇÃO AVANÇADA" not in {x[0] for x in nodes()}:
        if not click("ABRIR ADMINISTRAÇÃO AVANÇADA",16):return False
    if not click("EXÉRCITOS E BATALHAS",12):return False
    if not click("BATALHAS",3):return False
    if not click("NOVA BATALHA",3):return False
    form=nodes()
    fields=[v for v in form if v[3].endswith("EditText")]
    if not fields:
        log("WAR_NAME_FIELD_MISSING",[x[0] for x in form[:30]])
        return False
    shell("input","tap",str(fields[0][1]),str(fields[0][2]))
    shell("input","text","qawar")
    # Avoid Android KEYCODE_BACK: it previously sent the test process HOME.
    if not click("INICIAR",3):return False
    after=snapshot("AFTER_WAR")
    if after["counts"].get("kingmaker_war_encounters",0)<=before:return False
    battle=next((row for row in after["war_details"] if row["name"]=="qawar"),None)
    if battle is None:return False
    battle_key=battle["encounter_key"]
    for attempt in range(20):
        current=snapshot("WAR_ATTACK_"+str(attempt))
        live=next((row for row in current["war_details"] if row["encounter_key"]==battle_key),None)
        if live is None:return False
        if live["status"]=="complete":
            entries=json.loads(live["log_json"])
            return any(entry.get("kind")=="attack" and entry.get("damage",0)>0 for entry in entries)
        if not kingmaker():return False
        if not click("EXÉRCITOS E BATALHAS",12) or not click("BATALHAS",2):return False
        label=next((item[0] for item in nodes() if item[0].startswith("qawar •")),None)
        if label is None or not click(label,1) or not click("JOGAR RODADA",2):return False
        edits=[item for item in nodes() if item[3].endswith("EditText")]
        if not edits:return False
        # The production dialog's first EditText is its editable physical d20.
        # Use a legal deterministic natural roll, without SQL/state injection.
        field=edits[0]
        if not field[0].isdigit():return False
        shell("input","tap",str(field[1]),str(field[2]))
        shell("input","keyevent","123")
        for _ in field[0]:shell("input","keyevent","67")
        shell("input","text","20")
        if not click("ATACAR",2) or not click("Fechar",2):return False
    log("RESOLVED_WAR_NOT_PROVEN","attack limit reached")
    return False

def restart():
    before=snapshot("BEFORE_RESTART")
    # Empty-table persistence cannot certify the nonempty Kingdom/Army/War gate.
    mandatory=("kingmaker_turns","kingmaker_armies","kingmaker_war_encounters")
    if any(before["counts"].get(table,0)<1 for table in mandatory):
        log("PERSISTENCE_NONEMPTY_NOT_PROVEN",json.dumps(before["counts"]))
        return False
    for attempt in range(1,4):
        shell("am","force-stop",PKG)
        launch()
        after=snapshot("AFTER_RESTART_"+str(attempt))
        if (before["state"]!=after["state"] or before["counts"]!=after["counts"]
                or before["army"]!=after["army"] or before["row_sha256"]!=after["row_sha256"]
                or before["kingdom_state_sha256"]!=after["kingdom_state_sha256"]):
            log("PERSISTENCE_DRIFT_ON_RESTART",attempt)
            return False
    return True
def main():
    log("TEST_PACKAGE",PKG);launch()
    stage("ACTIVATION_ANDROID_APPDB",activate)
    if G["ACTIVATION_ANDROID_APPDB"]:
        stage("KINGDOM_TURN_ANDROID_APPDB",kingdom_turn)
        stage("ARMY_ANDROID_APPDB",army)
        if G["ARMY_ANDROID_APPDB"]:
            stage("WAR_ANDROID_APPDB",war)
        else:
            G["WAR_ANDROID_APPDB"]=False
    else:
        G["KINGDOM_TURN_ANDROID_APPDB"]=False
        G["ARMY_ANDROID_APPDB"]=False
        G["WAR_ANDROID_APPDB"]=False
    stage("PERSISTENCE_AFTER_PROCESS_RESTART",restart)
    log("KINGDOM_ARMY_WAR_AND_PERSISTENCE_LIVE_CHECKS",json.dumps(G))
    return 0 if all(G.values()) else 2
if __name__=="__main__":sys.exit(main())
