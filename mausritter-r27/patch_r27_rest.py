#!/usr/bin/env python3
"""R27 stage 2: official Mausritter rests, committed events and Atlas clock projections."""
from pathlib import Path
import sys
R=Path(sys.argv[1])
J=R/"android/app/src/main/java/com/braseiro/dnd"
AT=R/"android/app/src/main/assets/mausritter_atlas/runtime/atlas_vivo.js"
rules=J/"MausritterRestEngine.java"
if rules.exists(): raise RuntimeError("Rest engine already exists; refusing accidental overwrite")
rules.write_text(r'''package com.braseiro.dnd;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Random;

/** Executable Mausritter SRD rest rules. Does not know Android, network or story prompts. */
public final class MausritterRestEngine {
    private MausritterRestEngine(){}
    private static boolean hasCondition(JSONObject sheet,String code) {
        JSONArray items=sheet.optJSONArray("inventory");
        for(int i=0;items!=null&&i<items.length();i++){
            JSONObject item=items.optJSONObject(i);
            if(item!=null&&code.equals(item.optString("condition_id")))return true;
        }
        return false;
    }
    private static int clamp(int value,int low,int high) {
        return Math.max(low,Math.min(value,high));
    }
    private static void clearConditions(JSONObject sheet, String... ids)throws Exception {
        JSONArray old=sheet.optJSONArray("inventory"),next=new JSONArray();
        for(int i=0;old!=null&&i<old.length();i++){
            JSONObject item=old.optJSONObject(i);
            if(item==null) continue;
            boolean clear=false;
            for(String id:ids)if(id.equals(item.optString("condition_id")))clear=true;
            if(!clear)next.put(item);
        }
        sheet.put("inventory",next);
    }
    /**
     * sheet is intentionally mutated only on successful validation.
     * The caller must persist returned sheet and receipt in one SQLite transaction.
     */
    public static JSONObject apply(JSONObject sheet,String kind,String attribute,int die,boolean safeSettlement)throws Exception{
        if(sheet==null)throw new IllegalArgumentException("Ficha não encontrada");
        String mode=kind==null?"":kind.trim();
        if(!"short".equals(mode)&&!"long".equals(mode)&&!"full".equals(mode))
            throw new IllegalArgumentException("Descanso inválido");
        int hp=clamp(sheet.optInt("hp",0),0,Math.max(1,sheet.optInt("max_hp",1)));
        int maxHp=Math.max(1,sheet.optInt("max_hp",1));
        int value=clamp(die,1,6);
        JSONObject receipt=new JSONObject().put("kind",mode).put("roll",0)
                .put("durationTurns",0).put("durationWatches",0).put("healedHp",0)
                .put("restoredAttribute","").put("restoredPoints",0).put("spentPips",0);
        if("short".equals(mode)){
            int amount=hasCondition(sheet,"sick")?1:value+1;
            int after=Math.min(maxHp,hp+amount);
            sheet.put("hp",after);
            receipt.put("durationTurns",1).put("roll",hasCondition(sheet,"sick")?0:value)
                .put("healedHp",after-hp).put("message","Descanso curto de um Turno");
        } else if("long".equals(mode)){
            JSONArray items=sheet.optJSONArray("inventory");
            JSONObject ration=null;
            for(int i=0;items!=null&&i<items.length();i++){
                JSONObject item=items.optJSONObject(i);
                if(item!=null && "rations".equals(item.optString("item_id")) &&
                   item.optInt("uses",0)>item.optInt("spent",0)){ration=item;break;}
            }
            if(ration==null)throw new IllegalStateException("Descanso longo requer uma refeição: não há rações disponíveis");
            if(hp==maxHp){
                String stat=attribute==null?"":attribute.toLowerCase(java.util.Locale.ROOT);
                if(!"str".equals(stat)&&!"dex".equals(stat)&&!"wil".equals(stat))
                    throw new IllegalArgumentException("Escolha FOR, DES ou VON para a recuperação");
                int before=sheet.optInt(stat,8),maximum=Math.max(before,sheet.optInt("max_"+stat,before));
                int after=Math.min(maximum,before+value);
                sheet.put(stat,after);
                receipt.put("roll",value).put("restoredAttribute",stat).put("restoredPoints",after-before);
            } else{
                sheet.put("hp",maxHp);
                receipt.put("healedHp",maxHp-hp);
            }
            ration.put("spent",ration.optInt("spent",0)+1);
            receipt.put("durationWatches",1).put("rationId",ration.optString("instance_id",""))
                .put("message","Descanso longo de uma Vigília; refeição e sono consumidos");
            clearConditions(sheet,"hungry","exhausted");
        } else {
            if(!safeSettlement)throw new IllegalStateException("Descanso completo exige uma povoação comprovadamente segura");
            int funds=Math.max(0,sheet.optInt("grains",0));
            if(funds<20)throw new IllegalStateException("Descanso completo exige 20 grãos para uma semana de hospedagem");
            sheet.put("grains",funds-20).put("hp",maxHp);
            for(String stat:new String[]{"str","dex","wil"})
                sheet.put(stat,Math.max(sheet.optInt(stat,0),sheet.optInt("max_"+stat,sheet.optInt(stat,0))));
            clearConditions(sheet,"hungry","exhausted","injured","drained");
            receipt.put("durationWatches",28).put("healedHp",maxHp-hp)
                .put("spentPips",20).put("message","Uma semana em segurança; condições especiais permanecem para tratamento específico");
        }
        return receipt;
    }
}
''',encoding='utf-8')

main=J/"MainActivity.java"
s=main.read_text(encoding='utf-8')
anchor='''        @JavascriptInterface public String markAtlasItemUses(String raw)'''
assert s.count(anchor)==1, s.count(anchor)
bridge=r'''
        /** Present a real, player-chosen procedure. A narrative prompt never applies rest effects. */
        @JavascriptInterface public String requestAtlasCamp(final String raw) {
            ui.post(new Runnable(){ public void run(){
                try {
                    final JSONObject request=new JSONObject(raw==null?"{}":raw);
                    if(db.getCombatState().optInt("running",0)==1) {
                        toast("Não é possível descansar no meio de um combate."); return;
                    }
                    if(request.optBoolean("routePaused",false)){
                        toast("A travessia está interrompida. Resolva o incidente antes de descansar."); return;
                    }
                    final JSONObject character=findAtlasMouse(request.optString("mouseId"));
                    if(character.length()==0){toast("Selecione um rato válido.");return;}
                    final String[] choices={"Curto · 1 Turno · d6+1 PV",
                                            "Longo · 1 Vigília · refeição e sono",
                                            "Completo · 7 dias seguros · 20 grãos"};
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Acampamento de "+character.optString("name","rato"))
                        .setItems(choices,new DialogInterface.OnClickListener(){public void onClick(DialogInterface dialog,int which){
                            if(which==0){commitAtlasRest(request,"short","");return;}
                            if(which==2){commitAtlasRest(request,"full","");return;}
                            JSONObject sheet=MausritterRules.fromCharacter(character);
                            if(sheet.optInt("hp",0)<sheet.optInt("max_hp",1)){
                                commitAtlasRest(request,"long","str");return;
                            }
                            final String[] attrs={"FOR","DES","VON"};
                            new AlertDialog.Builder(MainActivity.this)
                                .setTitle("PV completos · recuperar d6 de qual atributo?")
                                .setItems(attrs,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int selected){
                                    commitAtlasRest(request,"long",new String[]{"str","dex","wil"}[selected]);
                                }}).setNegativeButton("Cancelar",null).show();
                        }}).setNegativeButton("Cancelar",null).show();
                }catch(Exception error){toast("Acampamento: "+safeUiError(error));}
            }}); return "ok";
        }

        private void commitAtlasRest(final JSONObject request,final String mode,final String attribute){
            AppExecutors.io().execute(new Runnable(){public void run(){
                JSONObject output=new JSONObject();
                try {
                    if(db.getCombatState().optInt("running",0)==1)
                        throw new IllegalStateException("Descanso bloqueado por combate ativo");
                    String requestedId=request.optString("mouseId","");
                    if(!requestedId.matches("[0-9]{1,15}"))
                        throw new IllegalArgumentException("ID de rato inválido");
                    long characterId=Long.parseLong(requestedId);
                    String runId=ensureMausritterCampaignRunId();
                    if(!runId.equals(request.optString("campaignRunId","")))
                        throw new IllegalStateException("Campanha mudou desde o pedido de descanso");
                    String adventureId=request.optString("adventureId","");
                    if(adventureId.length()==0 || adventureId.length()>100)
                        throw new IllegalArgumentException("Aventura inválida");
                    String eventId=request.optString("eventId","");
                    if(!eventId.matches("[A-Za-z0-9_.:-]{10,140}"))
                        throw new IllegalArgumentException("Recibo do descanso inválido");
                    String key="mausritter-rest:"+runId+":"+eventId;
                    android.database.sqlite.SQLiteDatabase sql=db.getWritableDatabase();
                    sql.beginTransaction();
                    try{
                        try(android.database.Cursor c=sql.rawQuery(
                             "SELECT body FROM journal WHERE world_profile=? AND event_key=? LIMIT 1",
                             new String[]{db.getActiveMapProfile(),key})){
                            if(c.moveToFirst()){output=new JSONObject(c.getString(0)).put("replayed",true);}
                        }
                        if(output.length()==0){
                            JSONObject character=db.getCharacter(characterId);
                            if(character.length()==0||!"Rato".equals(character.optString("species","Rato")))
                                throw new IllegalArgumentException("Rato não encontrado na campanha");
                            JSONObject sheet=MausritterRules.fromCharacter(character);
                            String before=sheet.toString();
                            boolean safe=request.optBoolean("safeSettlement",false);
                            int rolled=new java.security.SecureRandom().nextInt(6)+1;
                            output=MausritterRestEngine.apply(sheet,mode,attribute,rolled,safe);
                            output.put("ok",true).put("eventId",eventId)
                                  .put("campaignRunId",runId).put("adventureId",adventureId)
                                  .put("mouseId",requestedId).put("mouseName",sheet.optString("name","Rato"));
                            db.saveMausritterSheet(characterId,sheet);
                            long created=db.addJournalFact("Descanso Mausritter",output.toString(),
                                             "Acampamento",key,"mausritter_rest");
                            if(created<0)throw new IllegalStateException("Recibo duplicado de descanso");
                            if(output.optInt("roll",0)>0)
                                db.addDiceHistory("d6",String.valueOf(output.optInt("roll",0)),
                                  output.optInt("roll",0),"Descanso: "+mode);
                            db.addAuditEvent("mausritter_rest","Descanso confirmado",
                                              sheet.optString("name","Rato")+" · "+mode,
                                              before,sheet.toString(),true);
                        }
                        sql.setTransactionSuccessful();
                    }finally{sql.endTransaction();}
                } catch(Exception error) {
                    output=new JSONObject();
                    try{output.put("ok",false).put("error",safeUiError(error));}
                    catch(Exception ignored){}
                }
                final String result=output.toString();
                ui.post(new Runnable(){public void run(){
                    try{
                        if(mausritterAtlasWebView!=null)
                            mausritterAtlasWebView.evaluateJavascript(
                                "window.MausritterSession&&window.MausritterSession.onRestResolved("+
                                JSONObject.quote(result)+")",null);
                        else toast(new JSONObject(result).optString("ok").equals("true")
                                     ?"Descanso confirmado no Diário.":"Descanso: "+new JSONObject(result).optString("error"));
                    }catch(Exception error){toast("Resultado do acampamento indisponível na tela.");}
                }});
            }});
        }

        @JavascriptInterface public String getAtlasRestReceipts(){
            JSONArray out=new JSONArray();
            try(android.database.Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT body FROM journal WHERE world_profile=? AND source_kind='mausritter_rest' ORDER BY id DESC LIMIT 100",
                new String[]{db.getActiveMapProfile()})){
                while(c.moveToNext()){
                    try{out.put(new JSONObject(c.getString(0)));}catch(Exception ignored){}
                }
            }catch(Exception error){AppLog.w("MausritterRest","Falha ao ler recibos",error);}
            return out.toString();
        }

'''
s=s.replace(anchor,bridge+anchor)
main.write_text(s,encoding='utf-8')

atlas=AT.read_text(encoding='utf-8')
old="gmOpeningSeen: false, factionGoalIndexes:"
assert atlas.count(old)==1
atlas=atlas.replace(old,"gmOpeningSeen: false, restReceiptIds: [], factionGoalIndexes:")
old='''    campCurrent() {
      if (state.phase !== 'ready') return false;
      const data=runtime(), current=hexByMicro(activePosition(data));
      if (!current) return false;
      state.selected=current;
      const content=activeContent(current,data);
      const decision={authority:'mausritter-gm-r26-session',decision:'camp-intent',allowed:true,narrate:true,message:'A expedição declarou que quer acampar no local atual. Resolva segurança do acampamento, descanso, vigia, consumo, clima e riscos segundo as regras integradas. Não teleporte, não avance a rota automaticamente e, se houver teste ou escolha, pare para o jogador.'};
      bridge('requestNarration',JSON.stringify(enrichPayload('gm-camp-intent',current,content,{masterDecision:decision,market:marketAtHex(current)})));
      return true;
    },'''
new='''    campCurrent() {
      if(state.phase!=='ready'||state.gmBusy)return false;
      const data=runtime(),current=hexByMicro(activePosition(data)),mouse=activeMouse();
      if(!current||!mouse)return false;
      const market=marketAtHex(current),safe=Boolean(market&&Object.keys(market).length>0);
      const request={mouseId:String(mouse.id),campaignRunId:String(state.native.campaignRunId||''),
        adventureId:state.adventure.id,routePaused:Boolean(data.pendingJourney),
        safeSettlement:safe,
        eventId:'r'+Date.now().toString(36)+Math.random().toString(36).slice(2)};
      return bridge('requestAtlasCamp',JSON.stringify(request))==='ok';
    },
    onRestResolved(raw) {
      let receipt={};try{receipt=typeof raw==='string'?JSON.parse(raw):raw||{};}catch(_){}
      if(!receipt.ok)return toast(receipt.error||'O descanso não pôde ser realizado.');
      if(applyRestReceipt(receipt)) {
        refreshNativeState();renderAll();
        const data=runtime(),current=hexByMicro(activePosition(data));
        if(current){
          const outcome={authority:'mausritter-local-r27',decision:'rest-confirmed',allowed:true,
            message:receipt.message,receiptId:receipt.eventId,clockAlreadyApplied:true};
          bridge('requestNarration',JSON.stringify(enrichPayload('gm-rest-confirmed',current,
            activeContent(current,data),{masterDecision:outcome,restResult:receipt})));
        }
      }else{toast('Descanso já confirmado; nenhum efeito duplicado.');}
    },'''
assert atlas.count(old)==1
atlas=atlas.replace(old,new)
anchor='''  // R26 — API mínima para a SCREEN_SESSION aprovada. Não altera posição por conta própria.'''
block='''  function applyRestReceipt(receipt){
    if(!receipt||!receipt.ok||!receipt.eventId||!state.adventure)return false;
    if(receipt.adventureId!==state.adventure.id||
       receipt.campaignRunId!==String(state.native.campaignRunId||''))return false;
    const data=runtime();
    data.restReceiptIds=Array.isArray(data.restReceiptIds)?data.restReceiptIds:[];
    if(data.restReceiptIds.includes(receipt.eventId))return false;
    const watches=Math.max(0,Math.trunc(Number(receipt.durationWatches||0)));
    const turns=Math.max(0,Math.trunc(Number(receipt.durationTurns||0)));
    if(watches>28||turns>1)return false;
    if(watches)advanceWatches(data,watches);
    if(turns){data.localTurns=Number(data.localTurns||0)+turns;reconcileActiveLight(data);}
    data.restReceiptIds.push(receipt.eventId);
    if(data.restReceiptIds.length>200)data.restReceiptIds=data.restReceiptIds.slice(-200);
    saveRuntime(data);
    return true;
  }
  function recoverCommittedRestReceipts(){
    let list=[];
    try{const raw=bridge('getAtlasRestReceipts');list=typeof raw==='string'?JSON.parse(raw):raw||[];}
    catch(error){console.warn('rest receipts unavailable',error);return;}
    if(!Array.isArray(list))return;
    for(const receipt of list.reverse())applyRestReceipt(receipt);
  }

'''
assert atlas.count(anchor)==1
atlas=atlas.replace(anchor,block+anchor)
# Avoid double counting when an old native callback retries.
# The initial load must replay only already committed rests before UI projection.
anchor="      const data=runtime();data.numbers=false;data.grid=true;saveRuntime(data);"
assert atlas.count(anchor)==1
atlas=atlas.replace(anchor,"      recoverCommittedRestReceipts();\n"+anchor)
AT.write_text(atlas,encoding='utf-8')
print("R27 stage2 rest: pure rules, native SQLite receipt and UI picker, Atlas projection/recovery")
