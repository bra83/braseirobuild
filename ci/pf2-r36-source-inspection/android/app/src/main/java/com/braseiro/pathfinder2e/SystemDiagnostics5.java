package com.braseiro.pathfinder2e;
import android.content.Context;
import android.os.Environment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;

/** Diagnóstico legível da arquitetura 5.0. Executar fora da thread UI. */
public final class SystemDiagnostics5 {
    private SystemDiagnostics5(){}
    public static JSONObject run(Context c,AppDb db,CharonLatencyTelemetry charon,UiResponsivenessMonitor uiMonitor){JSONObject out=new JSONObject();JSONArray checks=new JSONArray();int ok=0,total=0;try{
        JSONObject map=db.getMapState();String pkg=map.optString("map_package_id");
        boolean[] values={db.quickIntegrityCheck(),db.schemaVersion()>=53,!db.listActiveCharacters().isEmpty(),db.isManualSearchIndexReady(),!pkg.isEmpty()&&db.isMapPackageUsable(pkg),!pkg.isEmpty()&&db.countMapReferencedLocations(pkg)>0,db.listKnownNpcs(1)!=null,MasterProtocolV1.VERSION==1,db.getCombatState().has("state")};
        String[] names={"Banco íntegro","Schema 5.0","Herói ativo","Índice local de regras/FTS","Atlas ativo","Pontos catalogados com referência cartográfica","Catálogo de NPC acessível","Protocolo do Mestre v1","Máquina de estados do combate"};
        for(int i=0;i<values.length;i++){checks.put(new JSONObject().put("name",names[i]).put("ok",values[i]));total++;if(values[i])ok++;}
        db.syncPendingActionQueue();checks.put(new JSONObject().put("name","Fila unificada de pendências").put("ok",true).put("count",db.listPendingActions("pending",1000).length()));total++;ok++;
        File files=c.getFilesDir();long free=files.getUsableSpace();boolean disk=free>128L*1024L*1024L;checks.put(new JSONObject().put("name","Espaço livre").put("ok",disk).put("free_mb",free/1024/1024));total++;if(disk)ok++;
        JSONObject hot=EssentialRulesCache.snapshot(db.getActiveMapProfile());if(!hot.optBoolean("ready",false)&&db.isManualSearchIndexReady()){EssentialRulesCache.warm(db);hot=EssentialRulesCache.snapshot(db.getActiveMapProfile());}checks.put(new JSONObject().put("name","Cache quente de regras essenciais").put("ok",hot.optBoolean("ready",false)));total++;if(hot.optBoolean("ready",false))ok++;
        out.put("version","5.0.0").put("ok",ok==total).put("passed",ok).put("total",total).put("checks",checks).put("database",db.databaseHealth5()).put("startup",new AdventureStartupCoordinator().preflight(db)).put("charon_latency",charon==null?new JSONObject():charon.snapshot()).put("ui_responsiveness",new JSONObject().put("lag_count",uiMonitor==null?0:uiMonitor.lagCount()).put("max_lag_ms",uiMonitor==null?0:uiMonitor.maxLagMs())).put("source_of_truth",SourceOfTruthRegistry.manifest()).put("master_protocol",MasterProtocolV1.descriptor()).put("combat_state_machine",CombatStateMachine.descriptor());
    }catch(Exception e){try{out.put("ok",false).put("error",String.valueOf(e.getMessage())).put("checks",checks);}catch(Exception ignored){AppLog.ignored("SystemDiagnostics5","Falha tolerada registrada na consolidação 5.0.",ignored);}}return out;}
}
