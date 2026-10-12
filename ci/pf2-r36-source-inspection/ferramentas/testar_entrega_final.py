#!/usr/bin/env python3
from pathlib import Path
import tempfile, subprocess, textwrap, sys, re
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
checks=[]
def check(name, ok):
    checks.append((name,bool(ok))); print(('OK  ' if ok else 'FALHA  ')+name)

main=(SRC/'MainActivity.java').read_text('utf-8')
living=(SRC/'LivingWorldSimulationEngine.java').read_text('utf-8')
world=(SRC/'WorldAutonomyPlanner.java').read_text('utf-8')
offline=(SRC/'OfflineMasterEngine.java').read_text('utf-8')
dnd=(SRC/'DndRulesEngine.java').read_text('utf-8')
context=(SRC/'MasterGameContext.java').read_text('utf-8')
options=(SRC/'SceneOptionDiversifier.java').read_text('utf-8')

check('Mestre local é inicializado', 'offlineMaster=new OfflineMasterEngine()' in main)
check('chave ausente usa Mestre local', 'runOfflineMaster(mode,userText,localHits,"chave Gemini ausente")' in main)
check('falha de rede usa Mestre local', 'runOfflineMaster(mode,userText,localHits,"falha de conexão:' in main)
check('resposta remota incompleta usa Mestre local', '"resposta remota incompleta"' in main)
check('Mestre local não consome cota remota', 'boolean offline=envelope!=null&&envelope.optJSONObject("_offline_master")!=null;if(!offline)quota.recordCall' in (SRC/'MasterOrchestrator.java').read_text('utf-8'))
check('Mestre local não cria mutações mecânicas', all(x in offline for x in ('"state_updates",new JSONArray()', '"combat",new JSONObject().put("action","none")', 'mechanics_authority')))
check('consulta local de regras funciona offline', 'manualAnswer' in offline and 'rules_verified' in offline and 'sources' in offline)
check('facções recebem plano autônomo', 'autonomy.factionPlan' in living and 'plan_steps_json' in world)
check('NPCs recebem ação autônoma', 'autonomy.npcAction' in living and 'generated!=null' in living)
check('Mundo Vivo continua transacional', 'runInTransactionChecked' in living and 'commit_error' in living)
check('núcleo 5e cobre dano e resistências', 'applyDamage' in dnd and 'resistances' in dnd and 'vulnerabilities' in dnd and 'immunities' in dnd)
check('núcleo 5e cobre ataques, salvaguardas e concentração', all(x in dnd for x in ('resolveAttack','savingThrowBonus','concentrationDc')))
check('núcleo 5e cobre descanso, espaços e morte', all(x in dnd for x in ('restPreview','fullCasterSlots','resolveDeathSave')))
check('barramento declara cobertura real', 'state_coverage' in context and '"omitted"' in context and '"total"' in context)
check('histórico de sugestões cobre 60 ações', 'MAX_HISTORY = 60' in options)
check('filtro semântico usa intenção, radical e trigramas', all(x in options for x in ('intentSignature','stem(','trigramDice')))

# Comportamento executável em JVM, com JSON mínimo funcional e sem Android SDK.
with tempfile.TemporaryDirectory(prefix='braseiro-final-test-') as td:
    td=Path(td); (td/'org/json').mkdir(parents=True); (td/'com/braseiro/pathfinder2e').mkdir(parents=True)
    (td/'org/json/JSONException.java').write_text('package org.json; public class JSONException extends Exception { public JSONException(String m){super(m);} }')
    (td/'org/json/JSONObject.java').write_text(r'''package org.json; import java.util.*; public class JSONObject {
      public static final Object NULL=new Object(); private final Map<String,Object> m=new LinkedHashMap<>();
      public JSONObject(){} public JSONObject(String s) throws JSONException {}
      public JSONObject put(String k,Object v) throws JSONException {m.put(k,v);return this;} public int length(){return m.size();}
      public boolean has(String k){return m.containsKey(k);} public boolean isNull(String k){return !m.containsKey(k)||m.get(k)==NULL||m.get(k)==null;}
      public Object opt(String k){return m.get(k);} public String optString(String k){return optString(k,"");} public String optString(String k,String d){Object v=m.get(k);return v==null||v==NULL?d:String.valueOf(v);}
      public int optInt(String k){return optInt(k,0);} public int optInt(String k,int d){Object v=m.get(k);if(v instanceof Number)return ((Number)v).intValue();try{return Integer.parseInt(String.valueOf(v));}catch(Exception e){return d;}}
      public long optLong(String k){return optLong(k,0);} public long optLong(String k,long d){Object v=m.get(k);if(v instanceof Number)return ((Number)v).longValue();try{return Long.parseLong(String.valueOf(v));}catch(Exception e){return d;}}
      public double optDouble(String k){return optDouble(k,0);} public double optDouble(String k,double d){Object v=m.get(k);if(v instanceof Number)return ((Number)v).doubleValue();try{return Double.parseDouble(String.valueOf(v));}catch(Exception e){return d;}}
      public boolean optBoolean(String k){return optBoolean(k,false);} public boolean optBoolean(String k,boolean d){Object v=m.get(k);return v instanceof Boolean?(Boolean)v:(v==null?d:Boolean.parseBoolean(String.valueOf(v)));}
      public JSONObject optJSONObject(String k){Object v=m.get(k);return v instanceof JSONObject?(JSONObject)v:null;} public JSONArray optJSONArray(String k){Object v=m.get(k);return v instanceof JSONArray?(JSONArray)v:null;}
      public Object remove(String k){return m.remove(k);} public Object get(String k) throws JSONException {if(!m.containsKey(k))throw new JSONException(k);return m.get(k);} public Iterator<String> keys(){return m.keySet().iterator();} public String toString(){return m.toString();}
    }''')
    (td/'org/json/JSONArray.java').write_text(r'''package org.json; import java.util.*; public class JSONArray { private final List<Object> a=new ArrayList<>();
      public JSONArray(){} public JSONArray(String s) throws JSONException {} public JSONArray(Collection<?> c){a.addAll(c);} public JSONArray(Object array) throws JSONException {if(array instanceof Object[])for(Object o:(Object[])array)a.add(o);}
      public JSONArray put(Object v){a.add(v);return this;} public int length(){return a.size();} public Object opt(int i){return i>=0&&i<a.size()?a.get(i):null;} public JSONObject optJSONObject(int i){Object v=opt(i);return v instanceof JSONObject?(JSONObject)v:null;} public JSONArray optJSONArray(int i){Object v=opt(i);return v instanceof JSONArray?(JSONArray)v:null;}
      public JSONObject getJSONObject(int i) throws JSONException {JSONObject o=optJSONObject(i);if(o==null)throw new JSONException("index");return o;} public String optString(int i){return optString(i,"");} public String optString(int i,String d){Object v=opt(i);return v==null?d:String.valueOf(v);} public int optInt(int i){return optInt(i,0);} public int optInt(int i,int d){Object v=opt(i);return v instanceof Number?((Number)v).intValue():d;} public long optLong(int i){Object v=opt(i);return v instanceof Number?((Number)v).longValue():0;} public Object remove(int i){return i>=0&&i<a.size()?a.remove(i):null;} public String toString(){return a.toString();}
    }''')
    (td/'com/braseiro/pathfinder2e/AppLog.java').write_text('package com.braseiro.dnd; public final class AppLog { public static void ignored(String a,String b,Throwable t){} public static void w(String a,String b,Throwable t){} }')
    (td/'com/braseiro/pathfinder2e/AppDb.java').write_text('package com.braseiro.dnd; public class AppDb { public java.util.List<org.json.JSONObject> listWorldEventsForMaster(int n){return new java.util.ArrayList<org.json.JSONObject>();} public java.util.List<org.json.JSONObject> listWorldClocks(boolean m,int n){return new java.util.ArrayList<org.json.JSONObject>();} public java.util.List<org.json.JSONObject> listQuests(String s,int n){return new java.util.ArrayList<org.json.JSONObject>();} public java.util.List<org.json.JSONObject> listFactions(int n){return new java.util.ArrayList<org.json.JSONObject>();} public org.json.JSONObject getSceneState(){return new org.json.JSONObject();} }')
    (td/'com/braseiro/pathfinder2e/MasterOrchestrator.java').write_text('package com.braseiro.dnd; public final class MasterOrchestrator { public static final class Plan { public final String compactContext; public Plan(String s){compactContext=s;} } }')
    test=textwrap.dedent('''
      package com.braseiro.dnd; import org.json.*; import java.util.*;
      public final class FinalBehaviorTest {
        private static void ok(boolean v,String m){if(!v)throw new RuntimeException(m);}
        public static void main(String[] args)throws Exception{
          DndRulesEngine r=new DndRulesEngine(); ok(r.proficiencyBonus(1)==2,"pb1");ok(r.proficiencyBonus(5)==3,"pb5");ok(r.proficiencyBonus(17)==6,"pb17");
          ok(r.concentrationDc(30)==15,"concentration");ok(r.chooseD20(4,17,true,false)==17,"advantage");ok(r.chooseD20(4,17,false,true)==4,"disadvantage");ok(r.carryingCapacity(10)==150,"capacity");
          ok(r.fullCasterSlots(20).length()==9,"slots");
          JSONObject f=new JSONObject().put("name","Guilda").put("agenda","controlar as rotas").put("relationship",-20);JSONObject fp=new WorldAutonomyPlanner().factionPlan(f,5);ok(fp.optString("current_plan").length()>0,"faction plan");ok(fp.optJSONArray("plan_steps_json").length()==4,"faction steps");
          JSONObject n=new JSONObject().put("npc_name","Lina").put("urgency",85).put("current_location","Porto");JSONObject np=new WorldAutonomyPlanner().npcAction(n,5);ok(np.optString("action").length()>0,"npc action");
          OfflineMasterEngine om=new OfflineMasterEngine();JSONObject env=om.generate("Narrador","Observar a porta",null,new ArrayList<JSONObject>());ok(env.optString("narration").length()>250,"offline narration");ok(env.optJSONObject("_offline_master")!=null,"offline flag");ok(env.optJSONArray("state_updates").length()==0,"offline mechanics");
          List<JSONObject> hits=new ArrayList<>();hits.add(new JSONObject().put("title","Manual").put("page",10).put("text","Trecho local verificável sobre a regra consultada."));JSONObject raw=om.generate("Regras RAW/RAI","regra",null,hits);ok(raw.optBoolean("rules_verified"),"manual verified");ok(raw.optJSONArray("sources").length()==1,"manual source");
          ok(SceneOptionDiversifier.similar("Conversar com o ferreiro sobre a chave","Questionar o ferreiro a respeito da chave"),"semantic options");
          JSONArray generated=SceneOptionDiversifier.diversify(new JSONArray(),new JSONArray(),"Uma porta secreta se abre atrás do altar.","Observar o altar",new JSONObject().put("location","Cripta"),3);ok(generated.length()<=2,"fallback max two");for(int i=0;i<generated.length();i++){JSONObject op=generated.optJSONObject(i);String act=op==null?"":op.optString("action",op.optString("description",op.optString("label","")));ok(SceneOptionDiversifier.isGroundedInScene(act,"Uma porta secreta se abre atrás do altar.",new JSONObject().put("location","Cripta")),"fallback grounded");}
          ok(DndRulesEngine.PROFILE_HYBRID.equals(DndRulesEngine.canonicalProfile("Hibrido")),"hybrid alias");
          ok(DndRulesEngine.PROFILE_HYBRID.equals(DndRulesEngine.canonicalProfile("Híbrido compatível")),"hybrid preserved");
          JSONArray h=new JSONArray();for(int i=0;i<80;i++)h.put(UUID.randomUUID().toString());ok(SceneOptionDiversifier.appendHistory(h,new JSONArray()).length()==60,"history 60");
          System.out.println("OK: testes comportamentais finais executados.");
        }
      }
    ''')
    (td/'com/braseiro/pathfinder2e/FinalBehaviorTest.java').write_text(test)
    cmd=['javac','-encoding','UTF-8','-d',str(td/'out'),str(td/'org/json/JSONException.java'),str(td/'org/json/JSONObject.java'),str(td/'org/json/JSONArray.java'),str(td/'com/braseiro/pathfinder2e/AppLog.java'),str(td/'com/braseiro/pathfinder2e/AppDb.java'),str(td/'com/braseiro/pathfinder2e/MasterOrchestrator.java'),str(SRC/'DndRulesEngine.java'),str(SRC/'WorldRuleGuidance.java'),str(SRC/'WorldAutonomyPlanner.java'),str(SRC/'SourceOfTruthRegistry.java'),str(SRC/'MasterProtocolV1.java'),str(SRC/'LiveTableNarrativeDirector.java'),str(SRC/'BraseiroNarrativeDirector.java'),str(SRC/'OfflineMasterEngine.java'),str(SRC/'PortugueseNarrationSanitizer.java'),str(SRC/'GeneratedTextSanitizer.java'),str(SRC/'SceneOptionDiversifier.java'),str(td/'com/braseiro/pathfinder2e/FinalBehaviorTest.java')]
    cp=subprocess.run(cmd,text=True,capture_output=True)
    check('Javac comportamental final compila',cp.returncode==0)
    if cp.returncode!=0: print(cp.stdout+cp.stderr)
    else:
        rp=subprocess.run(['java','-cp',str(td/'out'),'com.braseiro.dnd.FinalBehaviorTest'],text=True,capture_output=True)
        check('comportamento local executa',rp.returncode==0 and 'OK:' in rp.stdout)
        if rp.returncode!=0: print(rp.stdout+rp.stderr)

failed=[n for n,v in checks if not v]
print(f'RESULTADO FINAL: {len(checks)-len(failed)}/{len(checks)} verificações aprovadas.')
if failed:
    print('FALHAS:'); [print(' - '+x) for x in failed]; sys.exit(1)
