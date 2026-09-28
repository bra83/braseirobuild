package com.braseiro.pathfinder2e;
import org.json.JSONObject;
import java.util.List;

/** Máquina de fases da abertura PF2e. Atlas melhora grounding, mas nunca bloqueia a ficção. */
public final class AdventureStartupCoordinator {
    public enum Phase{IDLE,VALIDATING,LOCATION,RULES,CONTEXT,NARRATIVE,COMPLETE,BLOCKED}
    private volatile Phase phase=Phase.IDLE;private volatile String detail="";private volatile long changedAt=System.currentTimeMillis();
    public synchronized void phase(Phase p,String d){phase=p==null?Phase.BLOCKED:p;detail=d==null?"":d;changedAt=System.currentTimeMillis();}
    public JSONObject snapshot(){JSONObject o=new JSONObject();try{o.put("phase",phase.name().toLowerCase()).put("detail",detail).put("changed_at",changedAt);}catch(Exception ignored){AppLog.ignored("AdventureStartupCoordinator","Falha tolerada ao registrar fase de abertura.",ignored);}return o;}
    public JSONObject preflight(AppDb db){JSONObject r=new JSONObject();try{
        phase(Phase.VALIDATING,"Validando campanha e banco local");
        List<JSONObject> heroes=db.listActiveCharacters();JSONObject map=db.getMapState();String pkg=map.optString("map_package_id");
        boolean heroesOk=heroes!=null&&!heroes.isEmpty(),mapOk=!pkg.isEmpty()&&db.isMapPackageUsable(pkg),dbOk=db.quickIntegrityCheck();
        int candidates=mapOk?db.listAdventureStartHexes(pkg,"",2000).size():0;
        r.put("heroes",heroesOk).put("map",mapOk).put("database",dbOk).put("cataloged_starts",candidates).put("package_id",pkg).put("atlas_optional",true);
        if(!heroesOk)phase(Phase.BLOCKED,"Ative ao menos um personagem.");
        else if(!dbOk)phase(Phase.BLOCKED,"Banco local precisa de reparo.");
        else if(mapOk&&candidates>0)phase(Phase.LOCATION,"Atlas pronto; usando ponto catalogado.");
        else phase(Phase.LOCATION,"Atlas ainda indisponível; abertura seguirá em modo narrativo não-cartográfico.");
        r.put("ok",phase!=Phase.BLOCKED).put("startup",snapshot());
    }catch(Exception e){phase(Phase.BLOCKED,"Falha de preparação: "+e.getMessage());try{r.put("ok",false).put("startup",snapshot());}catch(Exception ignored){AppLog.ignored("AdventureStartupCoordinator","Falha tolerada ao registrar preflight.",ignored);}}return r;}
}
