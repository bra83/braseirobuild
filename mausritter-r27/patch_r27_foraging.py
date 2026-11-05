#!/usr/bin/env python3
"""R27 stage 5: explicit foraging; one Watch, d3 ration uses, actual inventory receipt."""
from pathlib import Path
import sys
root=Path(sys.argv[1])
java=root/'android/app/src/main/java/com/braseiro/dnd'
new=java/'MausritterForageEngine.java'
if new.exists():raise AssertionError('Mausritter forage source exists already')
new.write_text(r'''package com.braseiro.dnd;

import org.json.JSONArray;
import org.json.JSONObject;

/** Mausritter SRD: spend one Watch outdoors, find 1d3 uses of rations. */
public final class MausritterForageEngine {
    private MausritterForageEngine(){}
    public static JSONObject apply(JSONObject sheet,int d3)throws Exception{
        if(sheet==null)throw new IllegalArgumentException("Rato não encontrado");
        if(d3<1||d3>3)throw new IllegalArgumentException("Resultado d3 inválido");
        MausritterRules.Item rations=MausritterRules.item("rations");
        if(rations==null)throw new IllegalStateException("Equipamento de rações ausente");
        MausritterRules.normalize(sheet);
        JSONArray inventory=sheet.optJSONArray("inventory");
        if(inventory==null)throw new IllegalStateException("Inventário não inicializado");
        JSONObject gained=rations.instance(MausritterRules.PACK);
        gained.put("spent",3-d3);
        inventory.put(gained);
        MausritterRules.syncHeavy(sheet);
        return new JSONObject().put("kind","forage").put("roll",d3)
              .put("durationTurns",0).put("durationWatches",1)
              .put("healedHp",0).put("foundRations",d3)
              .put("rationId",gained.optString("instance_id"))
              .put("message","Forrageio concluído: "+d3+" uso(s) de ração obtido(s) em uma Vigília");
    }
}
''',encoding='utf-8')
app=java/'MainActivity.java'
s=app.read_text(encoding='utf-8')
old='''"Completo · 7 dias seguros · 20 grãos"};'''
new='''"Completo · 7 dias seguros · 20 grãos",
                                            "Forragear · 1 Vigília · 1d3 rações"};'''
if s.count(old)!=1:raise AssertionError('Rest choices missing')
s=s.replace(old,new)
old='''                            if(which==0){commitAtlasRest(request,"short","");return;}
                            if(which==2){commitAtlasRest(request,"full","");return;}'''
new='''                            if(which==0){commitAtlasRest(request,"short","");return;}
                            if(which==2){commitAtlasRest(request,"full","");return;}
                            if(which==3){commitAtlasRest(request,"forage","");return;}'''
if s.count(old)!=1:raise AssertionError('Rest choice handler missing')
s=s.replace(old,new)
old='''                            output=MausritterRestEngine.apply(sheet,mode,attribute,rolled,safe);'''
new='''                            output="forage".equals(mode)
                               ?MausritterForageEngine.apply(sheet,(rolled+1)/2)
                               :MausritterRestEngine.apply(sheet,mode,attribute,rolled,safe);'''
if s.count(old)!=1:raise AssertionError('Rest transaction dispatcher missing')
s=s.replace(old,new)
old='''                                db.addDiceHistory("d6",String.valueOf(output.optInt("roll",0)),
                                  output.optInt("roll",0),"Descanso: "+mode);'''
new='''                                db.addDiceHistory("forage".equals(mode)?"d3":"d6",
                                  String.valueOf(output.optInt("roll",0)),
                                  output.optInt("roll",0),("forage".equals(mode)?"Forrageio":"Descanso")+": "+mode);'''
if s.count(old)!=1:raise AssertionError('Dice ledger handler missing')
s=s.replace(old,new)
app.write_text(s,encoding='utf-8')
print("Foraging: real d3 rations item, one Watch, shared SQLite receipt and Atlas projected clock")
