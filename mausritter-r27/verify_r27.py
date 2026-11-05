#!/usr/bin/env python3
"""Executable probes for corrected R26 methods (no source-string PASS shortcuts)."""
from pathlib import Path
import subprocess
import sys
import tempfile
import textwrap

root=Path(sys.argv[1]).resolve()
j=root/'android/app/src/main/java/com/braseiro/dnd'
js=root/'android/app/src/main/assets/mausritter_atlas/runtime/atlas_vivo.js'
def section(path,start,end):
    txt=path.read_text(encoding='utf-8')
    a=txt.index(start)
    b=txt.index(end,a+len(start))
    return txt[a:b]

die=section(j/'MausritterGameMasterEngine.java','    private static int die(String seed','    private static int positiveHash(')
save=section(j/'MausritterRules.java','    public static boolean save(int roll','    public static int rollAttribute(')
armor=section(j/'MausritterRules.java','    public static int armor(JSONObject sheet)','    public static int capacity(')

java = '''
import java.util.*;
public class R27Probe {
    static final String BODY="body", PACK="pack";
    static String normalizeType(String value,String fallback) {
        if(value==null||value.isEmpty())return fallback;
        return value.toLowerCase(Locale.ROOT);
    }
    static class JSONObject {
        Map<String,Object> a=new HashMap<>();
        JSONObject put(String k,Object v) {a.put(k,v);return this;}
        String optString(String k) {return String.valueOf(a.getOrDefault(k,""));}
        int optInt(String k,int fallback) {Object v=a.get(k);return v instanceof Number?((Number)v).intValue():fallback;}
        boolean optBoolean(String k,boolean fallback) {Object v=a.get(k);return v instanceof Boolean?(boolean)v:fallback;}
        JSONArray optJSONArray(String k) {Object v=a.get(k);return v instanceof JSONArray?(JSONArray)v:null;}
    }
    static class JSONArray {
        List<JSONObject> a=new ArrayList<>();
        JSONArray add(JSONObject v) {a.add(v);return this;}
        int length() {return a.size();}
        JSONObject optJSONObject(int i){return a.get(i);}
    }
    /*@@METHODS@@*/
    static void ensure(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    public static void main(String[] args) {
        int[] observed=new int[13];
        for(int i=0;i<10000;i++){
            String seed="scene|"+i+"|reaction";
            int a=die(seed+"|reaction-a",6),b=die(seed+"|reaction-b",6),sum=a+b;
            ensure(sum>=2&&sum<=12,"Invalid 2d6 "+sum);
            ensure(a==die(seed+"|reaction-a",6) && b==die(seed+"|reaction-b",6),"Non-repeatable event");
            observed[sum]++;
        }
        for(int x=2;x<=12;x++)ensure(observed[x]>0,"Reaction total unreachable "+x);
        ensure(observed[2]>50 && observed[12]>50, "RNG suspiciously biased at extremes");
        for(int target=0;target<=21;target++)
            for(int roll=1;roll<=20;roll++)
                ensure(save(roll,target)==(roll<=target),"Save mismatch roll="+roll+" stat="+target);
        ensure(!save(0,10)&&!save(21,10),"Out of bounds rolls accepted");
        ensure(!save(1,0)&&save(20,20),"M04 known regression");
        JSONArray inv=new JSONArray();
        JSONObject sheet=new JSONObject().put("inventory",inv);
        inv.add(new JSONObject().put("type","pack").put("armor",1).put("uses",3).put("spent",0));
        ensure(armor(sheet)==0,"PACK provided armor");
        JSONObject equipped=new JSONObject().put("type","body").put("armor",1).put("uses",3).put("spent",0);
        inv.add(equipped);
        ensure(armor(sheet)==1,"Equipped usable armor did not protect");
        equipped.put("spent",3);
        ensure(armor(sheet)==0,"Depleted armor still protects");
        equipped.put("spent",0).put("type","pack");
        ensure(armor(sheet)==0,"Moving armor to PACK did not remove protection");
        inv.add(new JSONObject().put("type","body").put("armor",3).put("derived",true));
        ensure(armor(sheet)==0,"Derived item erroneously protects");
        System.out.println("R27_EXECUTABLE_PASS: M01 10000 events, 2d6 all totals, M03 equipment, M04 saves");
        System.out.println("reaction_histogram="+Arrays.toString(observed));
    }
}
'''.replace('    /*@@METHODS@@*/',die+save+armor)

atlas=js.read_text(encoding='utf-8')
fn=section(js,'  function advanceWatches(data, count)', '  function syncNativeWorldConsequence')
node=r'''
const vm=require('node:vm');
const fs=require('node:fs');
const source=fs.readFileSync(process.argv[2],'utf8');
const x=source.indexOf('  function advanceWatches(data, count)');
const y=source.indexOf('  function syncNativeWorldConsequence',x);
if(x<0||y<0)throw Error('M05 function missing');
let marks=0, days=0;
const state={worldRules:{time:{watchesPerDay:4,turnsPerWatch:36}}};
const c={state,Math,Number,console,Date, advanceFactions:()=>{days++},
advanceNpcWorld:()=>{},syncSeasonToCalendar:()=>{},
rollWeather:()=>{},ensureSeasonEvent:()=>{},
wildernessCheckDue:()=>false,
reconcileActiveLight:data=>{if(data.activeLight)marks++}};
vm.createContext(c);
vm.runInContext(source.slice(x,y),c);
const d={day:1,watch:0,season:'spring',weather:'',weatherDay:1,localTurns:0,activeLight:{id:1}};
c.advanceWatches(d,2);
if(d.localTurns!==72||d.watch!==2||marks!==1)throw Error('Elapsed watch not charged to light');
d.activeLight=null;
c.advanceWatches(d,0);
if(d.localTurns!==72||d.watch!==2||marks!==1)throw Error('Zero time/light regression');
c.advanceWatches(d,2);
if(d.day!==2||d.watch!==0||d.localTurns!==144||days!==1||marks!==1)throw Error('Day transition/light regression');
console.log('R27_EXECUTABLE_PASS: M05 2 watches, light active/off, no-time, day rollover');
'''
with tempfile.TemporaryDirectory() as d:
    p=Path(d)
    (p/'R27Probe.java').write_text(java,encoding='utf-8')
    (p/'clock_probe.cjs').write_text(node,encoding='utf-8')
    subprocess.run(['javac','R27Probe.java'],cwd=p,check=True)
    subprocess.run(['java','-cp',str(p),'R27Probe'],check=True)
    subprocess.run(['node',str(p/'clock_probe.cjs'),str(js)],check=True)
print('M02: source guards present, but SQLite/process death requires device/instrumented testing.')
print('M07: source classification adjusted, legacy equipment conflict migration unverified.')
