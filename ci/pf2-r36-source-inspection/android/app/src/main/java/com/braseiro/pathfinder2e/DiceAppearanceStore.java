package com.braseiro.pathfinder2e;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Cascata: padrão global -> ator -> ator+dado. */
public final class DiceAppearanceStore {
    private static final String PREF="braseiro_dice_atelier_v1";
    private final SharedPreferences p;
    public DiceAppearanceStore(Context c){p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    private static String key(long actor,int sides){return actor<=0?(sides<=0?"global":"global_d"+sides):(sides<=0?"actor_"+actor:"actor_"+actor+"_d"+sides);}
    public DiceAppearance resolve(long actor,int sides){DiceAppearance d=DiceAppearance.defaults();String global=p.getString(key(0,0),"");if(global.length()>0)d=DiceAppearance.fromJson(global);String die=p.getString(key(0,sides),"");if(die.length()>0)d=DiceAppearance.fromJson(new JSONObjectSafe(die).o,d);if(actor>0){String a=p.getString(key(actor,0),"");if(a.length()>0)d=DiceAppearance.fromJson(new JSONObjectSafe(a).o,d);String ad=p.getString(key(actor,sides),"");if(ad.length()>0)d=DiceAppearance.fromJson(new JSONObjectSafe(ad).o,d);}return d;}
    public DiceAppearance exact(long actor,int sides){String raw=p.getString(key(actor,sides),"");return raw.length()==0?resolve(actor,sides):DiceAppearance.fromJson(raw);}
    public void save(long actor,int sides,DiceAppearance d){p.edit().putString(key(actor,sides),d.toJson().toString()).apply();}
    public void reset(long actor,int sides){p.edit().remove(key(actor,sides)).apply();}
    public String exportJson(){JSONObject out=new JSONObject();try{for(String k:p.getAll().keySet())out.put(k,p.getString(k,""));}catch(Exception error){AppLog.ignored("Dados3D","Falha ao exportar Ateliê de Dados.",error);}return out.toString();}
    private static final class JSONObjectSafe{JSONObject o;JSONObjectSafe(String raw){try{o=new JSONObject(raw);}catch(Exception e){AppLog.ignored("Dados3D","JSON de aparência inválido; ignorando camada corrompida.",e);o=new JSONObject();}}}
}
