package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mesa local host-only. Expõe apenas a ficha do próprio herói e fatos
 * compartilhados da cena/combate. O convite é um bearer token aleatório.
 *
 * Segurança deliberada:
 * - servidor vinculado ao endereço LAN escolhido, não a 0.0.0.0;
 * - token fica no fragmento (#token=), portanto não entra na request URL,
 *   histórico do servidor ou access logs;
 * - APIs exigem Authorization: Bearer;
 * - corpos têm limite estrito e respostas não são cacheáveis;
 * - conhecimento privado malformado falha fechado.
 *
 * O transporte ainda é HTTP de rede local; use somente em uma LAN confiável.
 */
public final class LocalTableServer {
    public static final int DEFAULT_PORT=42731;
    private static final int MAX_BODY_BYTES=16384;
    private static final int MAX_TARGET_CHARS=4096;
    private static final SecureRandom RNG=new SecureRandom();

    private final AppDb db;
    private ServerSocket server;
    private final AtomicBoolean running=new AtomicBoolean(false);
    private String sessionCode="";
    private String bindAddress="127.0.0.1";
    private int port=DEFAULT_PORT;

    public LocalTableServer(AppDb database){db=database;AppDb.ensureSixZeroSchema(db.getWritableDatabase());}

    public synchronized JSONObject start(){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            if(running.get())return status();
            sessionCode=String.format(Locale.US,"%06d",100000+RNG.nextInt(900000));
            port=DEFAULT_PORT;bindAddress=localAddress();
            server=new ServerSocket();server.setReuseAddress(true);
            server.bind(new InetSocketAddress(InetAddress.getByName(bindAddress),port),16);
            running.set(true);provisionPlayers();persist(true);
            AppExecutors.io().execute(new Runnable(){public void run(){loop();}});
            return status();
        }catch(Exception e){
            stop();
            try{out.put("running",false).put("error",String.valueOf(e.getMessage()));}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha ao registrar erro de inicialização.",ignored);}
            return out;
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized void stop(){/*BR_JSON_WRAP*/try{
        running.set(false);
        try{if(server!=null)server.close();}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha tolerada ao fechar socket.",ignored);}server=null;
        try{ContentValues inactive=new ContentValues();inactive.put("active",0);db.getWritableDatabase().update("local_table_players",inactive,"world_profile=? AND session_code=?",new String[]{db.getActiveMapProfile(),sessionCode});}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha tolerada ao invalidar convites.",ignored);}
        persist(false);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    public boolean isRunning(){return running.get();}
    public JSONObject status(){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();try{o.put("running",running.get()).put("code",sessionCode).put("port",port).put("address",bindAddress).put("url",running.get()?baseUrl()+"/":"").put("players",invitations()).put("transport","trusted_lan_http");}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha ao montar status.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private String baseUrl(){return "http://"+bindAddress+":"+port;}
    private void provisionPlayers(){/*BR_JSON_WRAP*/try{
        String profile=db.getActiveMapProfile();SQLiteDatabase sql=db.getWritableDatabase();sql.delete("local_table_players","world_profile=?",new String[]{profile});
        for(JSONObject h:db.listActiveCharacters()){
            String token=randomToken();ContentValues v=new ContentValues();v.put("world_profile",profile);v.put("session_code",sessionCode);v.put("character_id",h.optLong("id"));v.put("character_name",h.optString("name"));v.put("player_name",h.optString("name"));v.put("join_token",token);v.put("permissions_json","{\"own_sheet\":true,\"submit_action\":true,\"shared_scene\":true}");v.put("last_seen",0);v.put("active",1);sql.insert("local_table_players",null,v);
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private JSONArray invitations(){/*BR_JSON_WRAP*/try{
        JSONArray a=new JSONArray();
        for(JSONObject p:db.queryList("SELECT * FROM local_table_players WHERE world_profile=? AND active=1 ORDER BY character_name",new String[]{db.getActiveMapProfile()})){
            JSONObject x=new JSONObject();try{x.put("character_id",p.optLong("character_id")).put("character_name",p.optString("character_name")).put("token",p.optString("join_token")).put("url",running.get()?baseUrl()+"/#token="+p.optString("join_token"):"");}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha ao montar convite.",ignored);}a.put(x);
        }
        return a;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private void persist(boolean enabled){/*BR_JSON_WRAP*/try{try{ContentValues v=new ContentValues();v.put("world_profile",db.getActiveMapProfile());v.put("session_code",sessionCode.isEmpty()?"000000":sessionCode);v.put("port",port);v.put("enabled",enabled?1:0);v.put("bind_address",bindAddress);v.put("started_at",enabled?System.currentTimeMillis():0);v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().insertWithOnConflict("local_table_sessions",null,v,SQLiteDatabase.CONFLICT_REPLACE);}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha ao persistir sessão local.",ignored);}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void loop(){while(running.get()){try{final Socket socket=server.accept();AppExecutors.io().execute(new Runnable(){public void run(){handle(socket);}});}catch(Exception e){if(running.get())AppLog.w("LocalTable","Falha ao aceitar cliente local.",e);}}}

    private void handle(Socket socket){/*BR_JSON_WRAP*/try{
        try{
            socket.setSoTimeout(7000);
            BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
            String request=r.readLine();if(request==null)return;
            String[] parts=request.split(" ");String method=parts.length>0?parts[0]:"GET",target=parts.length>1?parts[1]:"/";
            if(target.length()>MAX_TARGET_CHARS){json(socket,414,new JSONObject().put("error","requisição longa demais"));return;}
            HashMap<String,String> headers=new HashMap<String,String>();int contentLength=0;String line;
            while((line=r.readLine())!=null&&!line.isEmpty()){
                int c=line.indexOf(':');if(c<=0)continue;String name=line.substring(0,c).trim().toLowerCase(Locale.ROOT),value=line.substring(c+1).trim();headers.put(name,value);
                if("content-length".equals(name))try{contentLength=Integer.parseInt(value);}catch(Exception ignored){contentLength=-1;}
            }
            if(contentLength<0||contentLength>MAX_BODY_BYTES){json(socket,413,new JSONObject().put("error","corpo inválido ou grande demais"));return;}
            char[] bodyChars=new char[contentLength];int n=0;while(n<bodyChars.length){int got=r.read(bodyChars,n,bodyChars.length-n);if(got<0)break;n+=got;}if(n!=bodyChars.length){json(socket,400,new JSONObject().put("error","corpo incompleto"));return;}
            String body=new String(bodyChars,0,n);
            URI uri=new URI("http://local"+target);String path=uri.getPath();

            // A página é pública apenas como shell vazio: o token vive no fragmento e nunca chega nesta requisição.
            if("/".equals(path)&&"GET".equalsIgnoreCase(method)){reply(socket,200,"text/html; charset=utf-8",playerPage());return;}
            if(!"GET".equalsIgnoreCase(method)&&!"POST".equalsIgnoreCase(method)){json(socket,405,new JSONObject().put("error","método não permitido"));return;}

            String token=bearer(headers.get("authorization"));JSONObject player=player(token);
            if(player.length()==0){json(socket,403,new JSONObject().put("error","convite inválido"));return;}
            if("/state".equals(path)&&"GET".equalsIgnoreCase(method)){
                if(!permission(player,"own_sheet")&&!permission(player,"shared_scene")){json(socket,403,new JSONObject().put("error","sem permissão"));return;}
                touch(player);json(socket,200,playerState(player));return;
            }
            if("/action".equals(path)&&"POST".equalsIgnoreCase(method)){
                if(!permission(player,"submit_action")){json(socket,403,new JSONObject().put("error","sem permissão para agir"));return;}
                Map<String,String> form=query(body);String action=form.get("action");if(action==null||action.trim().isEmpty()){json(socket,400,new JSONObject().put("error","ação vazia"));return;}
                db.addPartyAction(player.optLong("character_id"),player.optString("character_name"),limit(action.trim(),1600),"mesa_local");touch(player);json(socket,200,new JSONObject().put("ok",true).put("message","Ação enviada ao Mestre."));return;
            }
            json(socket,404,new JSONObject().put("error","rota inexistente"));
        }catch(Exception e){try{json(socket,500,new JSONObject().put("error","falha local"));}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha ao responder erro local.",ignored);}}
        finally{try{socket.close();}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha tolerada ao fechar cliente.",ignored);}}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject playerState(JSONObject p){/*BR_JSON_WRAP*/try{JSONObject o=new JSONObject();try{long cid=p.optLong("character_id");syncPrivateKnowledge(cid,p.optString("character_name"));JSONObject hero=db.queryOne("SELECT id,name,species,class_name,level,hp,max_hp,ac,equipment,spells,proficiencies,token_uri,sheet_json FROM characters WHERE world_profile=? AND id=?",new String[]{db.getActiveMapProfile(),String.valueOf(cid)});o.put("hero",hero).put("scene",sharedScene()).put("combat",ownCombat(cid,p.optString("character_name"))).put("rumors",new RumorNetworkEngine(db).knownByHero(cid,40)).put("secrets",new JSONArray(db.queryList("SELECT title,body,created_at FROM local_table_secrets WHERE world_profile=? AND character_id=? AND active=1 ORDER BY created_at DESC LIMIT 50",new String[]{db.getActiveMapProfile(),String.valueOf(cid)}))).put("queued_actions",new JSONArray(db.queryList("SELECT action_text,status,created_at FROM party_action_queue WHERE world_profile=? AND character_id=? ORDER BY created_at DESC LIMIT 10",new String[]{db.getActiveMapProfile(),String.valueOf(cid)}))).put("server_time",System.currentTimeMillis());}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha ao montar estado do jogador.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized boolean addPrivateSecret(long characterId,String title,String body){/*BR_JSON_WRAP*/try{try{String clean=body==null?"":body.trim();if(characterId<=0||clean.isEmpty())return false;ContentValues v=new ContentValues();v.put("world_profile",db.getActiveMapProfile());v.put("character_id",characterId);v.put("title",title==null||title.trim().isEmpty()?"Informação privada":title.trim());v.put("body",limit(clean,4000));v.put("event_key","manual-secret:"+java.util.UUID.randomUUID().toString());v.put("active",1);v.put("created_at",System.currentTimeMillis());return db.getWritableDatabase().insert("local_table_secrets",null,v)>0;}catch(Exception e){AppLog.w("LocalTable","Falha ao registrar segredo privado.",e);return false;}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void syncPrivateKnowledge(long characterId,String characterName){/*BR_JSON_WRAP*/try{
        try{
            String id=String.valueOf(characterId),name=characterName==null?"":characterName.trim();
            for(JSONObject fact:db.listKnowledgeFacts(250)){
                String visibility=fact.optString("visibility","").toLowerCase(Locale.ROOT),known=fact.optString("known_by_json","[]");boolean privateFlag=visibility.matches(".*(privad|individual|segredo|secret|hero).*"),matches=false;
                try{
                    JSONArray a=new JSONArray(known);
                    for(int i=0;i<a.length();i++){
                        Object raw=a.opt(i);
                        if(raw instanceof JSONObject){JSONObject x=(JSONObject)raw;String xid=x.optString("id",x.optString("character_id")),xn=x.optString("name",x.optString("character_name"));if(id.equals(xid)||(!name.isEmpty()&&name.equalsIgnoreCase(xn)))matches=true;}
                        else{String x=String.valueOf(raw);if(id.equals(x)||(!name.isEmpty()&&name.equalsIgnoreCase(x)))matches=true;}
                    }
                }catch(Exception malformed){
                    // Conhecimento privado malformado nunca deve ampliar audiência por substring.
                    AppLog.w("LocalTable","known_by_json inválido; segredo mantido fechado.",malformed);matches=false;
                }
                if(!matches||(!privateFlag&&"grupo".equals(visibility)))continue;
                String key=fact.optString("fact_key").trim();if(key.isEmpty())key="knowledge:"+fact.optLong("id");
                if(db.queryOne("SELECT id FROM local_table_secrets WHERE world_profile=? AND character_id=? AND event_key=?",new String[]{db.getActiveMapProfile(),id,key}).length()>0)continue;
                ContentValues v=new ContentValues();v.put("world_profile",db.getActiveMapProfile());v.put("character_id",characterId);v.put("title",fact.optString("subject","Informação privada"));v.put("body",limit(fact.optString("content"),4000));v.put("event_key",key);v.put("active",1);v.put("created_at",fact.optLong("created_at",System.currentTimeMillis()));db.getWritableDatabase().insert("local_table_secrets",null,v);
            }
        }catch(Exception e){AppLog.w("LocalTable","Falha ao projetar conhecimento privado do herói.",e);}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject sharedScene(){/*BR_JSON_WRAP*/try{JSONObject s=db.getSceneState(),o=new JSONObject();try{o.put("day_number",s.optInt("day_number",1)).put("location",s.optString("location")).put("time_of_day",s.optString("time_of_day")).put("weather",s.optString("weather")).put("lighting",s.optString("lighting")).put("objective",s.optString("objective")).put("story",db.getStoryState().optString("last_narration")).put("options_json",db.getStoryState().optString("options_json","[]"));}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha ao montar cena compartilhada.",ignored);}return o;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private JSONObject ownCombat(long cid,String name){return db.queryOne("SELECT c.id,c.name,c.initiative,c.hp,c.max_hp,c.temp_hp,c.ac,c.conditions,c.active_turn,t.x,t.y FROM combatants c LEFT JOIN tactical_tokens t ON t.combatant_id=c.id AND t.world_profile=c.world_profile WHERE c.world_profile=? AND ((c.source_kind='character' AND c.source_id=?) OR lower(c.name)=lower(?)) LIMIT 1",new String[]{db.getActiveMapProfile(),String.valueOf(cid),name});}
    private JSONObject player(String token){if(token==null||token.length()<32)return new JSONObject();return db.queryOne("SELECT * FROM local_table_players WHERE world_profile=? AND session_code=? AND join_token=? AND active=1",new String[]{db.getActiveMapProfile(),sessionCode,token});}
    private void touch(JSONObject p){/*BR_JSON_WRAP*/try{ContentValues v=new ContentValues();v.put("last_seen",System.currentTimeMillis());db.getWritableDatabase().update("local_table_players",v,"id=?",new String[]{String.valueOf(p.optLong("id"))});}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean permission(JSONObject p,String key){try{return new JSONObject(p.optString("permissions_json","{}")).optBoolean(key,false);}catch(Exception e){return false;}}
    private static String bearer(String authorization){if(authorization==null)return "";String prefix="Bearer ";return authorization.regionMatches(true,0,prefix,0,prefix.length())?authorization.substring(prefix.length()).trim():"";}

    private static void json(Socket s,int code,JSONObject o)throws Exception{reply(s,code,"application/json; charset=utf-8",o.toString());}
    private static void reply(Socket s,int code,String type,String body)throws Exception{
        byte[] data=body.getBytes(StandardCharsets.UTF_8);OutputStream out=s.getOutputStream();String reason=code==200?"OK":(code==400?"Bad Request":(code==403?"Forbidden":(code==404?"Not Found":(code==405?"Method Not Allowed":(code==413?"Payload Too Large":(code==414?"URI Too Long":"Error"))))));
        String h="HTTP/1.1 "+code+" "+reason+"\r\nContent-Type: "+type+"\r\nContent-Length: "+data.length+"\r\nCache-Control: no-store, max-age=0\r\nPragma: no-cache\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nCross-Origin-Resource-Policy: same-origin\r\nConnection: close\r\n\r\n";
        out.write(h.getBytes(StandardCharsets.UTF_8));out.write(data);out.flush();
    }
    private static Map<String,String> query(String raw)throws Exception{/*BR_JSON_WRAP*/try{HashMap<String,String> m=new HashMap<String,String>();if(raw==null)return m;for(String part:raw.split("&")){int i=part.indexOf('=');String k=i<0?part:part.substring(0,i),v=i<0?"":part.substring(i+1);m.put(URLDecoder.decode(k,"UTF-8"),URLDecoder.decode(v,"UTF-8"));}return m;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static String playerPage(){
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><meta name='referrer' content='no-referrer'><style>body{margin:0;background:#071a12;color:#f2f0e7;font-family:system-ui;padding:16px}.card{background:#113323;border:1px solid #6e815f;border-radius:14px;padding:14px;margin:10px 0}h1,h2{color:#d8bd61}textarea{width:100%;min-height:110px;background:#0b2619;color:#fff;border:1px solid #6e815f;border-radius:10px;padding:10px;box-sizing:border-box}button{width:100%;padding:14px;background:#d8bd61;color:#111;border:0;border-radius:10px;font-weight:bold}.err{color:#ffb4ab}</style></head><body><h1 id='heroName'>Braseiro — Mesa Local</h1><div id='state'>Conectando…</div><div class='card'><h2>Sua ação</h2><textarea id='a' maxlength='1600' placeholder='Descreva o que seu herói faz ou diz'></textarea><button onclick='send()'>ENVIAR AO MESTRE</button><p id='msg'></p></div><script>const token=new URLSearchParams(location.hash.slice(1)).get('token')||'';history.replaceState(null,'',location.pathname);const auth={'Authorization':'Bearer '+token};function esc(x){return String(x??'').replace(/[&<>]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));}async function api(path,opt={}){opt.headers=Object.assign({},opt.headers||{},auth);let r=await fetch(path,opt);let j=await r.json().catch(()=>({error:'Resposta local inválida'}));if(!r.ok)throw new Error(j.error||'Convite inválido');return j;}async function load(){try{let s=await api('/state'),h=s.hero||{},sc=s.scene||{},c=s.combat||{};document.getElementById('heroName').textContent=h.name||'Mesa Local';document.getElementById('state').innerHTML=`<div class=card><h2>${esc(h.name)}</h2><b>${esc(h.species)} • ${esc(h.class_name)} ${h.level||''}</b><p>PV ${h.hp||0}/${h.max_hp||0} • CA ${h.ac||0}</p></div><div class=card><h2>${esc(sc.location||'Cena')}</h2><p>Dia ${sc.day_number||1} • ${esc(sc.time_of_day||'')} • ${esc(sc.weather||'')}</p><p>${esc(sc.objective||'')}</p></div>${c.name?`<div class=card><h2>Combate</h2><p>Iniciativa ${c.initiative||0} • PV ${c.hp||0}/${c.max_hp||0} • ${esc(c.conditions||'')}</p></div>`:''}<div class=card><h2>Rumores que você conhece</h2>${(s.rumors||[]).map(x=>`<p>• ${esc(x.statement)}</p>`).join('')||'<p>Nenhum.</p>'}</div><div class=card><h2>Informações privadas</h2>${(s.secrets||[]).map(x=>`<p><b>${esc(x.title)}</b><br>${esc(x.body)}</p>`).join('')||'<p>Nenhuma.</p>'}</div>`;}catch(e){document.getElementById('state').innerHTML='<div class=card><p class=err>'+esc(e.message)+'</p></div>';}}async function send(){let a=document.getElementById('a').value.trim();if(!a)return;try{let j=await api('/action',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'action='+encodeURIComponent(a)});document.getElementById('msg').textContent=j.message||'';document.getElementById('a').value='';}catch(e){document.getElementById('msg').textContent=e.message;}}if(!token){document.getElementById('state').innerHTML='<div class=card><p class=err>Convite inválido. Abra o endereço individual mostrado no aparelho do Mestre.</p></div>';}else{load();setInterval(load,5000);}</script></body></html>";
    }

    private static String localAddress(){try{Enumeration<NetworkInterface> ifs=NetworkInterface.getNetworkInterfaces();while(ifs.hasMoreElements()){NetworkInterface ni=ifs.nextElement();if(!ni.isUp()||ni.isLoopback())continue;Enumeration<InetAddress> as=ni.getInetAddresses();while(as.hasMoreElements()){InetAddress a=as.nextElement();if(a instanceof Inet4Address&&!a.isLoopbackAddress()&&a.isSiteLocalAddress())return a.getHostAddress();}}}catch(Exception ignored){AppLog.ignored("LocalTableServer","Falha ao encontrar endereço LAN.",ignored);}return "127.0.0.1";}
    private static String randomToken(){byte[] bytes=new byte[32];RNG.nextBytes(bytes);StringBuilder b=new StringBuilder(64);for(byte x:bytes)b.append(String.format(Locale.US,"%02x",x&255));return b.toString();}
    private static String limit(String s,int n){return s.length()<=n?s:s.substring(0,n);}
}
