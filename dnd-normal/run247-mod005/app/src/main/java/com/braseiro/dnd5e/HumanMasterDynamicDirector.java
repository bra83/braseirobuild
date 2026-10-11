package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Random;

/** RUN187 dynamic tone/humor/NPC/location director — narrative proposals only, no rules or state mutations. */
public final class HumanMasterDynamicDirector {
    private HumanMasterDynamicDirector() {}
    private static final String[] TONES={"cotidiano","acolhedor","cômico discreto","constrangedor","curioso",
        "misterioso","inquieto","tenso","melancólico","dramático","intimista","brincalhão","contemplativo","leve"};
    private static String text(JSONObject o,String...keys){
        for(String k:keys){String v=o==null?"":o.optString(k,"").trim();if(!v.isEmpty())return v;}
        return "local não especificado";
    }
    private static String fingerprint(String location,String input,String prior,JSONObject scene){
        String stable=location+"|"+input+"|"+prior+"|"+scene.optInt("day_number",0)+"|"+scene.optString("time_of_day","");
        return Integer.toHexString(stable.hashCode());
    }
    private static String chooseTone(JSONObject state, String fp, boolean combat, boolean locked){
        if(combat)return "tenso";
        if(locked)return "dramático";
        JSONArray recent=DynamicMasterState.recent(state,"recent_tones");
        Random r=new Random(((long)fp.hashCode()<<32)^0x1babe1);
        int i=r.nextInt(TONES.length);
        if(TONES[i].equals(recent.optString(0,"")) || TONES[i].equals(recent.optString(1,"")))i=(i+3)%TONES.length;
        return TONES[i];
    }
    private static String localCharacter(JSONObject state,String location){
        String k=Integer.toHexString(location.toLowerCase(Locale.ROOT).hashCode());
        JSONObject locations=state.optJSONObject("locations");
        JSONObject profile=locations==null?null:locations.optJSONObject(k);
        if(profile!=null)return profile.optString("identity","");
        String[] textures={"passos, vozes e pequenos hábitos de pessoas que vivem aqui",
            "rotina diferente conforme horário e clima", "objetos gastos pelo uso e frequentadores com compromissos próprios",
            "pequenas mudanças no ambiente sem objetivo de missão", "sons e movimentos ligados às tarefas do lugar"};
        return textures[Math.floorMod(location.hashCode(),textures.length)];
    }
    public static String instruction(Context context, AppDb db, JSONObject scene, String input,String previousNarration,
                                     String world,boolean nonAdvancing,boolean combat,boolean canonicalLocked) {
        if(context==null||nonAdvancing)return "";
        JSONObject s=scene==null?new JSONObject():scene, state=DynamicMasterState.read(context,db);
        String location=text(s,"location","location_name","current_location");
        String fp=fingerprint(location,input==null?"":input,previousNarration==null?"":previousNarration,s);
        String tone=chooseTone(state,fp,combat,canonicalLocked);
        JSONObject proposal=SpontaneousEventEngine.candidate(state,location,world==null?"":world,fp,
            combat||canonicalLocked,nonAdvancing);
        StringBuilder b=new StringBuilder("\nRUN187 PERFIL UNIVERSAL — MESTRE HUMANO DINÂMICO. ");
        b.append("Direção de tom sugerida: ").append(tone).append(". Não force mudança de tom durante luto, escolha crítica ou clímax. ");
        b.append("Ambiente recorrente de ").append(location).append(": ").append(localCharacter(state,location)).append(". ");
        b.append(npcPersonalityFragment(db,s));
        b.append("NPCs: preservar personality_json, modo de falar, objetivos e memória do catálogo local de 300 perfis. ")
         .append("Diferenciar pessoas por formalidade, paciência, humor, ritmo, fala, hesitação, erros e silêncio; não tornar todos simpáticos, explicativos e educados. ")
         .append("Mundo fora da câmera: demonstrar rotinas e relações existentes sem inventar deslocamentos impossíveis, mortes, itens ou vantagens. ")
         .append("Humor: contextual e ocasional, não obrigatório; evitar piadas de IA, metalinguagem e interromper drama sensível. ")
         .append("Nem tudo é missão: acontecimentos cotidianos podem ficar apenas na paisagem. ")
         .append("Agência, mecânica, regras, fatos secretos e campanha oficial prevalecem sempre. ");
        if(proposal.length()>0) b.append("JANELA OPCIONAL DE IMPREVISTO: ").append(proposal.optString("idea"))
           .append(" Categoria ").append(proposal.optString("category"))
           .append(". Use somente SE plausível. Se virar fato persistente, inclua nas atualizações estruturadas que passarão pelos validadores e revisão transacional do jogo; nunca aplique custo/rolagem automaticamente. ");
        else b.append("Nesta cena, NÃO forçar acontecimento espontâneo. ");
        JSONArray recent=DynamicMasterState.recent(state,"openings");
        if(recent.length()>0)b.append("Aberturas usadas recentemente e a evitar: ").append(recent.toString()).append(". ");
        b.append("Evitar bordões repetidos como 'de repente', 'o ar parece pesado', 'algo chama sua atenção'; variar ritmo, entrada e encerramento da cena. ");
        b.append("Este bloco é direção artística e nunca substitui o JSON técnico, o mestre canônico nem a validação de regras.\n");
        return b.toString();
    }
    /** Called only after accepted, advancing narrative; never on GM_HELP/failed/repaired attempts. */
    public static void accepted(Context context,AppDb db,JSONObject scene,String action,String narration,JSONObject envelope,String world) {
        if(context==null||narration==null||narration.trim().isEmpty())return;
        JSONObject state=DynamicMasterState.read(context,db);
        String location=text(scene,"location","location_name","current_location");
        String fp=fingerprint(location,action==null?"":action,"",scene==null?new JSONObject():scene);
        // Proposals in the AI envelope are NOT facts until the SQLite commit.
        // This callback only records narrative form and pacing.
        String lower=narration.toLowerCase(new Locale("pt","BR"));
        String tone="cotidiano";
        if(lower.contains("risada")||lower.contains("riso")||lower.contains("constrang"))tone="cômico discreto";
        else if(lower.contains("ameaç")||lower.contains("perigo")||lower.contains("ataque"))tone="tenso";
        else if(lower.contains("luto")||lower.contains("morte")||lower.contains("triste"))tone="melancólico";
        String opening=narration.replaceAll("\\s+"," ").trim();
        if(opening.length()>100)opening=opening.substring(0,100);
        int dot=opening.indexOf('.');if(dot>30)opening=opening.substring(0,dot+1);
        int cd=Math.max(0,state.optInt("event_cooldown",0)-1);
        double pr=state.optDouble("surprise_pressure",0.20);
        // Proposed world_events may be discarded by the player: only confirmed() records them.
        pr=Math.min(0.72,pr+0.045);
        try{
            state.put("turns",state.optInt("turns",0)+1);
            state.put("event_cooldown",cd);
            state.put("surprise_pressure",pr);
            state.put("recent_tones",DynamicMasterState.prepend(DynamicMasterState.recent(state,"recent_tones"),tone,10));
            state.put("openings",DynamicMasterState.prepend(DynamicMasterState.recent(state,"openings"),opening,10));
        }catch(Exception ignored){}
        DynamicMasterState.rememberLocation(state,location,localCharacter(state,location));
        DynamicMasterState.write(context,db,state);
    }
    /** Render real NPC profiles already persisted by the existing 300-profile catalog. */
    public static String npcPersonalityFragment(AppDb db,JSONObject scene){
        if(db==null||scene==null)return "";
        String raw=scene.optString("present_npcs_json","[]");
        JSONArray cast;
        try{cast=new JSONArray(raw);}catch(Exception ex){cast=new JSONArray();}
        StringBuilder b=new StringBuilder();
        int found=0;
        for(int i=0;i<cast.length()&&found<6;i++){
            Object value=cast.opt(i);
            String name=value instanceof JSONObject?((JSONObject)value).optString("name",""):cast.optString(i,"");
            if(name.trim().isEmpty())continue;
            try{
                JSONObject npc=db.getNpcCognitiveState(name);
                if(npc==null||npc.length()==0)continue;
                JSONObject p=new JSONObject(npc.optString("personality_json","{}"));
                if(p.length()==0)continue;
                String voice=p.optString("modo_de_falar",p.optString("speech_style",""));
                String motive=p.optString("desejo_atual",p.optString("goal",""));
                String fear=p.optString("medo_ou_limite",p.optString("fear",""));
                b.append("NPC PRESENTE ").append(name)
                 .append(": fala=").append(voice).append("; objetivo=").append(motive)
                 .append("; limite=").append(fear).append(". ");found++;
            }catch(Exception ignored){}
        }
        return b.toString();
    }

    /** Only call after a successful SQLite commit with the FILTERED, applied envelope. */
    public static void confirmed(Context context,AppDb db,JSONObject committed){
        if(context==null||db==null||committed==null)return;
        JSONArray events=committed.optJSONArray("world_events");
        // Journal updates without spontaneous events do not trigger surprise cooldown.
        if(events==null||events.length()==0)return;
        JSONObject state=DynamicMasterState.read(context,db);
        JSONArray history=DynamicMasterState.recent(state,"recent_events");
        JSONObject scene=db.getSceneState();
        String location=text(scene,"location","location_name","current_location");
        for(int i=0;i<events.length();i++){
            JSONObject event=events.optJSONObject(i);
            if(event==null)continue;
            String category=event.optString("category",event.optString("type","evento")).trim();
            if(category.length()==0)category="evento";
            history=DynamicMasterState.prepend(history,category,10);
            String description=event.optString("summary",event.optString("description",event.optString("title",""))).trim();
            if(!description.isEmpty())DynamicMasterState.rememberCommittedLocationEvent(state,location,description);
        }
        try{
            state.put("event_cooldown",2);
            state.put("surprise_pressure",Math.max(0.08,state.optDouble("surprise_pressure",0.20)-0.35));
            state.put("recent_events",history);
        }catch(Exception ignored){}
        DynamicMasterState.write(context,db,state);
    }
}
