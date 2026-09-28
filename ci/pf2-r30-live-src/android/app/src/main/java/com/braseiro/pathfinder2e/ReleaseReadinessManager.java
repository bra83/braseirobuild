package com.braseiro.pathfinder2e;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.os.StatFs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Auditoria não destrutiva da candidata a lançamento. Não chama Gemini e não altera a campanha. */
public final class ReleaseReadinessManager {
    private final Context context;
    private final AppDb db;
    private final CampaignSafetyManager safety;
    private final SmartAudioLibrary smartAudio;
    private final NarrativeAudioEngine narrativeAudio;
    private final NpcTokenAutoBinder npcTokens;
    private final SecureSecretStore secrets;
    private final ResilientTaskQueue tasks;

    public ReleaseReadinessManager(Context c,AppDb d,CampaignSafetyManager s,SmartAudioLibrary a,
                                   NarrativeAudioEngine n,NpcTokenAutoBinder t,SecureSecretStore secretStore,
                                   ResilientTaskQueue queue){
        context=c.getApplicationContext();db=d;safety=s;smartAudio=a;narrativeAudio=n;npcTokens=t;secrets=secretStore;tasks=queue;
    }

    public JSONObject audit(){
        JSONObject out=new JSONObject();JSONArray checks=new JSONArray();Counter counter=new Counter();StringBuilder human=new StringBuilder();
        checkVersion(checks,counter);
        try{JSONObject h=db.healthCheck();boolean ok=h.optBoolean("database_healthy",false);add(checks,counter,"Banco SQLite",ok?"pass":"fail",ok?"íntegro • versão "+h.optInt("database_version"):h.toString());JSONObject maps=h.optJSONObject("map_assets");boolean mapOk=maps!=null&&maps.optBoolean("healthy",false);add(checks,counter,"Referências físicas dos mapas",mapOk?"pass":"fail",mapOk?"imagens, manifestos e atlas instalados estão legíveis":String.valueOf(maps));}catch(Exception e){add(checks,counter,"Banco e mapas","fail",safe(e));}
        try{JSONObject contract=BackupContractValidator.validate(db.exportAll());boolean ok=contract.optBoolean("valid",false);add(checks,counter,"Contrato de backup de campanha",ok?"pass":"fail",ok?"formato 20 • campanha Pathfinder 2e, Mundo Vivo e dados persistentes; sem bibliotecas globais":contract.toString());}catch(Exception e){add(checks,counter,"Contrato de backup de campanha","fail",safe(e));}
        try{JSONObject rag=db.ragIndexHealth();boolean ok=rag.optBoolean("healthy",false);add(checks,counter,"Coerência dos índices RAG",ok?"pass":"fail",ok?rag.optInt("ready_manuals")+" manual(is) pronto(s), índices coerentes":rag.toString());}catch(Exception e){add(checks,counter,"Coerência dos índices RAG","fail",safe(e));}
        try{JSONObject campaign=db.getCampaign();boolean ok=campaign.length()>0;add(checks,counter,"Campanha",ok?"pass":"fail",ok?campaign.optString("name","campanha carregada"):"registro ausente");}catch(Exception e){add(checks,counter,"Campanha","fail",safe(e));}
        try{JSONObject living=new JSONObject(new LivingWorldEngine(db).compactContext());boolean ok=living.has("calendar")&&living.has("hero_lives")&&living.has("rule_boundary");add(checks,counter,"Mundo Vivo",ok?"pass":"fail",ok?living.optString("calendar")+" • barreira mecânica ativa":living.toString());}catch(Exception e){add(checks,counter,"Mundo Vivo","fail",safe(e));}
        auditWorldSix(checks,counter);
        try{int count=safety.snapshotFileCount();add(checks,counter,"Snapshots",count>0?"pass":"warn",count+" arquivo(s) local(is)");}catch(Exception e){add(checks,counter,"Snapshots","warn",safe(e));}
        try{boolean ok=secrets!=null&&secrets.selfTest();add(checks,counter,"Android Keystore",ok?"pass":"fail",ok?"cifra, leitura e remoção confirmadas":"teste de proteção falhou");}catch(Exception e){add(checks,counter,"Android Keystore","fail",safe(e));}
        try{JSONObject embedded=readAssetJson("audio/biblioteca_sons.json");boolean ok=embedded.length()>=500;add(checks,counter,"Catálogo de áudio empacotado",ok?"pass":"fail",embedded.length()+" entradas");}catch(Exception e){add(checks,counter,"Catálogo de áudio empacotado","fail",safe(e));}
        try{JSONObject tokenManifest=readAssetJson("tokens/tokens_manifest.json");boolean ok=tokenManifest.optInt("manifestVersion",0)>0;add(checks,counter,"Manifesto de tokens",ok?"pass":"warn",ok?"versão "+tokenManifest.optInt("manifestVersion"):"estrutura não reconhecida");}catch(Exception e){add(checks,counter,"Manifesto de tokens","warn",safe(e));}
        try{JSONObject audio=narrativeAudio.diagnosticReport();boolean ok=audio.optInt("catalog_sounds",0)>0;add(checks,counter,"Motor de áudio",ok?"pass":"warn",audio.optString("audio_channels","cinco canais disponíveis")+" • "+smartAudio.summary());}catch(Exception e){add(checks,counter,"Motor de áudio","warn",safe(e));}
        try{PhysicalDiceEngine.Session d20=PhysicalDiceEngine.create("d20",0x5A17BEEFL);int guard=0;while(!d20.isComplete()&&guard++<6000)d20.step();PhysicalDiceEngine.RollResult r=d20.result();boolean ok=d20.isComplete()&&r!=null&&r.dice.size()==1&&r.dice.get(0)>=1&&r.dice.get(0)<=20;add(checks,counter,"Dados 3D físicos nativos",ok?"pass":"fail",ok?"motor GURPS-parity • 120 Hz • face superior autoritativa":"simulação d20 não estabilizou corretamente");}catch(Exception e){add(checks,counter,"Dados 3D físicos nativos","fail",safe(e));}
        try{SharedPreferences visual=context.getSharedPreferences("settings",Context.MODE_PRIVATE);boolean enabled=visual.getBoolean("candle_light_enabled",true);int level=visual.getInt("candle_light_intensity",100);add(checks,counter,"Luz de vela global",enabled?"pass":"warn",enabled?"ativa • intensidade "+level+"%":"desativada pelo usuário nas configurações");}catch(Exception e){add(checks,counter,"Luz de vela global","warn",safe(e));}
        try{int count=npcTokens.catalogSize();add(checks,counter,"Tokens automáticos de NPC",count>0?"pass":"warn",count+" imagem(ns) empacotada(s)");}catch(Exception e){add(checks,counter,"Tokens automáticos de NPC","warn",safe(e));}
        auditMapZip(checks,counter,"Atlas canônico do Mar Interior","pathfinder/atlas/inner_sea_canonical_v6_grid_native_hires_compat.zip");
        try{long size=assetSize("manuals/Mythic 2e.pdf",120L*1024L*1024L);boolean ok=size>1024L*1024L;add(checks,counter,"Manual Mythic incluído",ok?"pass":"fail",humanBytes(size));}catch(Exception e){add(checks,counter,"Manual Mythic incluído","fail",safe(e));}
        auditPersistedFolders(checks,counter);
        auditTasks(checks,counter);
        try{StatFs fs=new StatFs(context.getFilesDir().getAbsolutePath());long free=fs.getAvailableBytes();String status=free>=768L*1024L*1024L?"pass":free>=256L*1024L*1024L?"warn":"fail";add(checks,counter,"Espaço livre interno",status,humanBytes(free));}catch(Exception e){add(checks,counter,"Espaço livre interno","warn",safe(e));}
        long logBytes=AppLog.bytes();add(checks,counter,"Log rotativo",logBytes<=2L*1024L*1024L?"pass":"warn",humanBytes(logBytes));
        try{
            out.put("format","braseiro-release-readiness").put("format_version",4).put("app_version",BuildConfig.VERSION_NAME)
                    .put("created_at",System.currentTimeMillis()).put("pass",counter.pass).put("warn",counter.warn).put("fail",counter.fail).put("checks",checks);
            String summary=counter.fail==0?"Auditoria concluída: "+counter.pass+" aprovado(s), "+counter.warn+" aviso(s), nenhum erro estrutural.":"Auditoria encontrou "+counter.fail+" erro(s), "+counter.warn+" aviso(s) e "+counter.pass+" aprovação(ões).";
            for(int i=0;i<checks.length();i++){JSONObject c=checks.optJSONObject(i);if(c!=null)human.append(c.optString("status").toUpperCase(Locale.US)).append(" • ").append(c.optString("name")).append(": ").append(c.optString("detail")).append('\n');}
            out.put("ready",counter.fail==0).put("summary",summary).put("human_report",human.toString()).put("runtime_log_tail",AppLog.tail(12000));
        }catch(Exception e){AppLog.e("ReleaseReadiness","Falha ao montar o relatório final.",e);}
        return out;
    }

    private void auditWorldSix(JSONArray checks,Counter counter){
        try{
            String profile=db.getActiveMapProfile();
            JSONObject seed=db.queryOne("SELECT * FROM world_seeds WHERE world_profile=?",new String[]{profile});
            add(checks,counter,"Mundo persistente 6.0",seed.length()>0?"pass":"fail",seed.length()>0?"seed "+seed.optString("seed_id")+" • geração "+seed.optInt("generation",1):"world seed ausente para "+profile);
            add(checks,counter,"Perfil único Pathfinder 2e",("pf2".equals(profile)||"pathfinder_2e".equals(profile))?"pass":"fail","world_profile ativo: "+profile);
            String[] tables={"emergent_arcs","settlement_states","world_history_events","rumors","rumor_knowledge","campaign_legacies","npc_voice_profiles","tactical_scenes","local_table_sessions","local_table_players","local_table_secrets"};
            int accessible=0;for(String table:tables){try{db.queryOne("SELECT COUNT(*) n FROM "+table+" WHERE world_profile=?",new String[]{profile});accessible++;}catch(Exception ignored){AppLog.ignored("ReleaseReadinessManager","Falha tolerada na camada 6.0.",ignored);}}
            add(checks,counter,"Persistência dos módulos 6.0",accessible==tables.length?"pass":"fail",accessible+"/"+tables.length+" tabelas acessíveis");
            JSONObject six=new WorldSixCoordinator(db).context();boolean contextOk=six.has("campaign_director")&&six.has("settlements")&&six.has("rumors")&&six.has("history")&&six.has("legacy")&&six.has("npc_voices")&&six.has("tactical_generation");
            add(checks,counter,"Diretor emergente e memória mundial",contextOk?"pass":"fail",contextOk?"contexto 6.0 conectado ao Mestre":"contexto incompleto");
            JSONObject session=db.queryOne("SELECT enabled,port FROM local_table_sessions WHERE world_profile=?",new String[]{profile});
            add(checks,counter,"Mesa Local",session.length()==0||session.optInt("enabled",0)==0?"pass":"warn",session.length()==0?"servidor parado; pronto para iniciar":session.optInt("enabled",0)==1?"sessão local marcada como ativa na porta "+session.optInt("port",42731):"servidor parado; estado persistente íntegro");
        }catch(Exception e){add(checks,counter,"Arquitetura 6.0","fail",safe(e));}
    }

    private void checkVersion(JSONArray checks,Counter counter){
        try{PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);String installed=info.versionName==null?"":info.versionName;boolean ok=installed.equals(BuildConfig.VERSION_NAME);add(checks,counter,"Versão do pacote",ok?"pass":"fail","BuildConfig "+BuildConfig.VERSION_NAME+" • pacote "+installed);}catch(Exception e){add(checks,counter,"Versão do pacote","fail",safe(e));}
    }

    private void auditMapZip(JSONArray checks,Counter counter,String label,String path){
        ZipInputStream zip=null;try{
            zip=new ZipInputStream(context.getAssets().open(path));int entries=0;long bytes=0;boolean manifest=false,image=false,atlas=false;ZipEntry entry;byte[] buffer=new byte[65536];
            while((entry=zip.getNextEntry())!=null){if(entry.isDirectory())continue;entries++;String name=entry.getName().toLowerCase(Locale.US);manifest|=name.contains("map_manifest")&&name.endsWith(".json");image|=name.endsWith(".png")||name.endsWith(".jpg")||name.endsWith(".jpeg");atlas|=name.contains("terrain_atlas")||name.contains("hex_atlas");int n;while((n=zip.read(buffer))>0){bytes+=n;if(bytes>1500L*1024L*1024L)throw new Exception("conteúdo descompactado excede 1,5 GB");}}
            boolean ok=entries>=4&&manifest&&image&&atlas;add(checks,counter,label,ok?"pass":"fail",entries+" arquivo(s) • "+humanBytes(bytes)+(ok?"":" • manifesto/imagem/atlas incompletos"));
        }catch(Exception e){add(checks,counter,label,"fail",safe(e));}
        finally{try{if(zip!=null)zip.close();}catch(Exception ignored){AppLog.ignored("ReleaseReadinessManager","Falha tolerada; a execução continuou.",ignored);}}
    }

    private void auditPersistedFolders(JSONArray checks,Counter counter){
        try{
            LibraryConnectionManager connections=new LibraryConnectionManager(context);
            JSONArray statuses=connections.allStatuses(true);
            int configured=0,healthy=0;
            for(int i=0;i<statuses.length();i++){
                JSONObject status=statuses.optJSONObject(i);if(status==null)continue;
                boolean linked=status.optBoolean("configured",false);
                boolean ok=status.optBoolean("healthy",false);
                if(linked)configured++;if(ok)healthy++;
                String label=status.optString("label","Pasta externa");
                String code=status.optString("code","provider_error");
                String result=!linked?"pass":ok?"pass":"warn";
                String detail=!linked?"não vinculada; uso opcional":connections.humanStatus(status)+" • "+status.optString("detail",code);
                add(checks,counter,label,result,detail);
            }
            add(checks,counter,"Gerenciador unificado de bibliotecas","pass",configured+" pasta(s) vinculada(s) • "+healthy+" estado(s) saudáveis ou opcionais");
            auditManualIsolation(checks,counter);
        }catch(Exception e){add(checks,counter,"Permissões persistentes de pastas","warn",safe(e));}
    }

    private void auditManualIsolation(JSONArray checks,Counter counter){
        try{
            int authoritative=0,lore=0,blocked=0,unknown=0;
            for(JSONObject manual:db.listManuals("")){
                String title=manual.optString("title","").trim(),profile=manual.optString("rule_profile","");
                if(ManualIdentityPolicy.isExternalSystemTitle(title+" "+profile))blocked++;
                else if(ManualIdentityPolicy.mechanicalAuthorityAllowed(title,profile))authoritative++;
                else if(ManualIdentityPolicy.isPf2LoreTitle(title))lore++;
                else unknown++;
            }
            add(checks,counter,"Firewall de fontes Pathfinder 2e","pass",authoritative+" fonte(s) mecânica(s) PF2e • "+lore+" de lore/mapa • "+blocked+" externa(s) bloqueada(s) • "+unknown+" sem autoridade mecânica");
        }catch(Exception e){add(checks,counter,"Firewall de fontes Pathfinder 2e","warn",safe(e));}
    }

    private void auditTasks(JSONArray checks,Counter counter){
        try{JSONObject state=tasks==null?new JSONObject():tasks.state();JSONArray list=state.optJSONArray("tasks");int failed=0,interrupted=0,running=0;if(list!=null)for(int i=0;i<list.length();i++){String status=list.optJSONObject(i)==null?"":list.optJSONObject(i).optString("status");if("failed".equals(status))failed++;else if("interrupted".equals(status))interrupted++;else if("running".equals(status))running++;}String result=failed>0?"warn":"pass";add(checks,counter,"Fila de tarefas",result,running+" em execução • "+failed+" falha(s) • "+interrupted+" interrompida(s)");}catch(Exception e){add(checks,counter,"Fila de tarefas","warn",safe(e));}
    }

    private JSONObject readAssetJson(String path)throws Exception{InputStream in=context.getAssets().open(path);byte[] data=readAll(in,32L*1024L*1024L);return new JSONObject(new String(data,"UTF-8"));}
    private long assetSize(String path,long limit)throws Exception{InputStream in=context.getAssets().open(path);long total=0;byte[] buffer=new byte[65536];int n;try{while((n=in.read(buffer))>0){total+=n;if(total>limit)break;}}finally{in.close();}return total;}
    private static byte[] readAll(InputStream in,long max)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] data=new byte[32768];int n;long total=0;try{while((n=in.read(data))>0){total+=n;if(total>max)throw new Exception("asset excede o limite seguro de leitura");out.write(data,0,n);}}finally{in.close();}return out.toByteArray();}
    private static void add(JSONArray a,Counter c,String name,String status,String detail){try{a.put(new JSONObject().put("name",name).put("status",status).put("detail",detail));if("pass".equals(status))c.pass++;else if("fail".equals(status))c.fail++;else c.warn++;}catch(Exception ignored){AppLog.ignored("ReleaseReadinessManager","Falha tolerada; a execução continuou.",ignored);}}
    private static String safe(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    private static String humanBytes(long n){if(n>=1024L*1024L*1024L)return String.format(Locale.US,"%.1f GB",n/(1024d*1024d*1024d));if(n>=1024L*1024L)return String.format(Locale.US,"%.1f MB",n/(1024d*1024d));if(n>=1024L)return String.format(Locale.US,"%.1f KB",n/1024d);return n+" B";}
    private static final class Counter{int pass;int warn;int fail;}
}
