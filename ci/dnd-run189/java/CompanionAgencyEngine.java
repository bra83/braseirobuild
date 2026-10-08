package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/** RUN189: character-scoped agency. No direct world, combat, inventory or rule mutations. */
public final class CompanionAgencyEngine {
    private CompanionAgencyEngine() {}
    public static String mode(String stored) {
        if ("companion".equals(stored) || "autonomous".equals(stored)) return "companion";
        // Legacy automated and assisted modes cannot silently gain new powers.
        if ("curated".equals(stored) || "assisted".equals(stored) || "combat_auto".equals(stored) || "system".equals(stored)) return "curated";
        return "manual";
    }
    public static String sceneFingerprint(AppDb db, JSONObject hero) {
        JSONObject scene=db.getSceneState(), map=db.getMapState(), combat=db.getCombatState();
        String value=db.getActiveMapProfile()+"|"+scene.optString("location","")+"|"+scene.optString("hex_id", "")+"|"+
          scene.optString("map_package_id", "")+"|"+scene.optInt("day_number",0)+"|"+scene.optDouble("clock_hour",0)+"|"+
          map.optString("current_hex_id","")+"|"+combat.optInt("running",0)+"|"+combat.optInt("round_no",0)+"|"+
          combat.optInt("turn_index",0)+"|"+hero.optLong("id",0)+"|"+hero.optInt("hp",0)+"|"+
          hero.optString("sheet_json", "")+"|"+hero.optString("spells", "")+"|"+hero.optString("equipment", "");
        return digest(value);
    }
    public static String digest(String input){
        try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] b=md.digest(input.getBytes("UTF-8"));StringBuilder out=new StringBuilder();for(int i=0;i<16;i++)out.append(String.format(Locale.US,"%02x", b[i]&255));return out.toString();}
        catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }
    public static boolean characterCanPropose(AppDb db, JSONObject hero) {
        if(hero==null||hero.optLong("id",0)<=0||hero.optInt("active",1)!=1||hero.optInt("hp",1)<=0)return false;
        if(db.getCombatState().optInt("running",0)!=1)return true;
        for(JSONObject c:db.listCombatants()){
            if(c.optInt("active_turn",0)==1 && c.optString("name","").equalsIgnoreCase(hero.optString("name",""))) {
                String conditions=c.optString("conditions","").toLowerCase(Locale.ROOT);
                return c.optInt("hp",0)>0 && !conditions.contains("incapacitado") && !conditions.contains("inconsciente") && !conditions.contains("paralisado") && !conditions.contains("atordoado");
            }
        }
        return false;
    }
    public static String instruction(AppDb db, List<JSONObject> heroes, boolean adviceOnly){
        StringBuilder b=new StringBuilder("\nRUN189 — COMPANHEIROS COM AGÊNCIA, sem override das regras compiladas.\n");
        b.append("Não narre nem escolha fala intencional ou ação por personagem EU CONTROLO. Silêncio não é autorização.\n");
        b.append("Companheiro autônomo só age por percepção acessível, interpretação falível, motivo individual e oportunidade ficcional. Não invente percepções sensoriais, magias, recursos ou segredos. Não force ação a cada batida; preserve o protagonismo do jogador.\n");
        b.append("COMPANHEIRO COM CURADORIA: jamais narre a ação como concluída, gaste recurso, mova no mapa, role ou crie efeito. Apresente proposta com razão percebida no campo companion_proposals. Uma proposta é intenção, não fato. Sua resolução aguarda autorização e validação mecânica separadas.\n");
        b.append("Em combate somente o combatente do turno pode iniciar ação. Sem rolagem automática quando a mesa exige rolagem manual.\n");
        if(adviceOnly)b.append("CONSULTA NÃO AVANÇANTE: apenas opiniões e hipóteses sem alterar tempo, mundo ou recursos; não execute nenhuma proposta nem cause eventos novos.\n");
        b.append("ELENCO CONTROLÁVEL E FATOS PERCEBIDOS (não inclui segredos do Mestre):\n");
        JSONArray known=new JSONArray();
        List<JSONObject> facts=db.listKnowledgeFacts(60);
        for(JSONObject hero:heroes){
            long id=hero.optLong("id",0);if(id<=0)continue;
            JSONObject ctl=db.getCharacterControl(id);String normalized=mode(ctl.optString("control_mode","manual"));
            b.append(" - [").append(id).append("] ").append(hero.optString("name", "Herói"))
              .append(" | modo=").append(normalized).append(" | classe=").append(hero.optString("class_name", ""))
              .append(" | objetivo=").append(shorten(ctl.optString("personal_goal",""),180))
              .append(" | tendências=").append(shorten(ctl.optString("arc_notes",""),200))
              .append(" | limites=").append(shorten(ctl.optString("tactics_json","{}"),220)).append("\n");
            if("manual".equals(normalized))continue;
            for(JSONObject fact:facts){
                String visibility=fact.optString("visibility", "").toLowerCase(Locale.ROOT);
                boolean access="grupo".equals(visibility)||"party".equals(visibility);
                if("individual".equals(visibility)){
                    JSONArray receivers=new JSONArray();try{receivers=new JSONArray(fact.optString("known_by_json","[]"));}catch(Exception ignored){}
                    for(int i=0;i<receivers.length();i++)if(hero.optString("name", "").equalsIgnoreCase(receivers.optString(i)))access=true;
                }
                if(!access)continue;
                if(known.length()<30)known.put("["+hero.optString("name", "")+"] Relato conhecido: "+shorten(fact.optString("content",""),190));
            }
        }
        b.append("Relatos conhecidos (podem ser falsos; não validar ocultamente): ").append(known.toString()).append("\n");
        b.append("Se houver iniciativa material de personagem curated, inclua campo adicional JSON: companion_proposals:[{character_id:ID,action:'',reason_observed:'',cost_risk:''}]. Não inclua essas ações em state_updates, combat.updates, deslocamentos ou eventos como efeitos já ocorridos. Para diálogo/observação gratuita sem custos, pode narrar com discrição.\n");
        return b.toString();
    }
    private static String shorten(String s,int size){String t=(s==null?"":s).replaceAll("[\\r\\n]+"," ").trim();return t.length()>size?t.substring(0,size):t;}
    /** Persist validated curated *intentions* only; no mechanical effects are committed. */
    public static int stageCurated(AppDb db, JSONObject rawEnvelope, List<JSONObject> heroes){
        JSONArray proposals=rawEnvelope==null?null:rawEnvelope.optJSONArray("companion_proposals");if(proposals==null)return 0;
        int n=0;
        for(int i=0;i<Math.min(6,proposals.length());i++){
            JSONObject p=proposals.optJSONObject(i);if(p==null)continue;
            long heroId=p.optLong("character_id",0);JSONObject hero=null;
            for(JSONObject h:heroes){if(h.optLong("id",0)==heroId){hero=h;break;}}
            if(hero==null||!"curated".equals(mode(db.getCharacterControl(heroId).optString("control_mode"))))continue;
            if(!characterCanPropose(db,hero))continue;
            String action=shorten(p.optString("action",""),400),reason=shorten(p.optString("reason_observed",""),400),risk=shorten(p.optString("cost_risk",""),240);
            if(action.length()<6||reason.length()<8||risk.length()<3)continue;
            String fp=sceneFingerprint(db,hero);
            String proposalId=digest(db.getActiveMapProfile()+"|"+heroId+"|"+fp+"|"+action+"|"+reason);
            db.stageCompanionProposal(proposalId,heroId,hero.optString("name"),fp,action,reason,risk);
            n++; // duplicate response remains blocked: never apply effects without approval
        }
        return n;
    }
}
