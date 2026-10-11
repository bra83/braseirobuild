package com.braseiro.dnd5e;
import org.json.JSONObject;
import java.util.List;

/** Única máquina de fases da abertura 5.0. A UI pode observar o status, mas não pular fases. */
public final class AdventureStartupCoordinator {
    public enum Phase{IDLE,VALIDATING,LOCATION,RULES,CONTEXT,NARRATIVE,COMPLETE,BLOCKED}
    private volatile Phase phase=Phase.IDLE;private volatile String detail="";private volatile long changedAt=System.currentTimeMillis();
    public synchronized void phase(Phase p,String d){phase=p==null?Phase.BLOCKED:p;detail=d==null?"":d;changedAt=System.currentTimeMillis();}
    public JSONObject snapshot(){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();try{o.put("phase",phase.name().toLowerCase()).put("detail",detail).put("changed_at",changedAt);}catch(Exception ignored){AppLog.ignored("AdventureStartupCoordinator","Falha tolerada registrada na consolidação 5.0.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public JSONObject preflight(AppDb db){/*BR_JSON_WRAP*/try{JSONObject r=new JSONObject();try{
        phase(Phase.VALIDATING,"Validando campanha e Atlas");
        List<JSONObject> heroes=db.listActiveCharacters();JSONObject map=db.getMapState();String pkg=map.optString("map_package_id");
        boolean heroesOk=heroes!=null&&!heroes.isEmpty(),mapOk=!pkg.isEmpty()&&db.isMapPackageUsable(pkg),dbOk=db.quickIntegrityCheck();
        int candidates=mapOk?db.listAdventureStartHexes(pkg,"",2000).size():0;
        r.put("heroes",heroesOk).put("map",mapOk).put("database",dbOk).put("cataloged_starts",candidates).put("package_id",pkg);
        if(!heroesOk)phase(Phase.BLOCKED,"Ative ao menos um herói.");else if(!dbOk)phase(Phase.BLOCKED,"Banco local precisa de reparo.");else if(!mapOk)phase(Phase.BLOCKED,"Atlas ativo indisponível.");else if(candidates<=0)phase(Phase.BLOCKED,"Atlas sem pontos catalogados posicionados. Use Reparar Atlas.");else phase(Phase.LOCATION,"Escolha ou sorteie um ponto catalogado.");
        r.put("ok",phase!=Phase.BLOCKED).put("startup",snapshot());
    }catch(Exception e){phase(Phase.BLOCKED,"Falha de preparação: "+e.getMessage());try{r.put("ok",false).put("startup",snapshot());}catch(Exception ignored){AppLog.ignored("AdventureStartupCoordinator","Falha tolerada registrada na consolidação 5.0.",ignored);}}return r;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
}
