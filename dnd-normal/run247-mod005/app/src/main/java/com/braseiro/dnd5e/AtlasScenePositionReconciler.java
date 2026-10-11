package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contrato único Atlas <-> cena.
 *
 * O Atlas é a autoridade para posição macro. A cena pode manter um sublocal
 * narrativo (taverna, quarto, praça etc.), mas nunca pode persistir como local
 * atual uma cidade/fortaleza/local catalogado que pertença a outro hex.
 */
public final class AtlasScenePositionReconciler {
    private static final String REPAIR_KEY="_atlas_location_repair";

    private AtlasScenePositionReconciler() { }

    /** Último current_hex_id já validado no envelope; caso contrário usa o Atlas persistido. */
    public static String effectiveHex(AppDb db, JSONObject envelope) {
        String hex=db==null?"":db.getMapState().optString("current_hex_id","").trim();
        if(envelope==null)return hex;
        JSONArray maps=envelope.optJSONArray("map_updates");
        if(maps==null)return hex;
        for(int i=0;i<maps.length();i++){
            JSONObject update=maps.optJSONObject(i);if(update==null)continue;
            String proposed=update.optString("current_hex_id","").trim();if(proposed.length()>0)hex=proposed;
        }
        return hex;
    }

    /** Projeção de mapa usada antes do commit, evitando aplicar a cena contra o hex antigo. */
    public static JSONObject projectedMapState(AppDb db, JSONObject envelope) {/*BR_JSON_WRAP*/try{
        JSONObject base=db==null?new JSONObject():db.getMapState();
        try{
            JSONObject out=new JSONObject(base.toString());
            String effective=effectiveHex(db,envelope);if(effective.length()>0)out.put("current_hex_id",effective);
            return out;
        }catch(Exception ignored){return base;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /**
     * Corrige a proposta estruturada antes de gerar o delta final.
     * Retorna true quando houve reparo geográfico material.
     */
    public static boolean reconcileProposal(AppDb db, JSONObject envelope, JSONObject currentScene, JSONObject proposedScene, JSONArray warnings) throws Exception {/*BR_JSON_WRAP*/try{
        if(db==null||proposedScene==null)return false;
        JSONObject map=db.getMapState();String packageId=map.optString("map_package_id","").trim();String hex=effectiveHex(db,envelope);
        if(packageId.length()==0||hex.length()==0)return false;
        if(db.getRawMapHex(packageId,hex).length()==0)return false;

        boolean changed=false;
        String beforeHex=currentScene==null?"":currentScene.optString("hex_id","").trim();
        boolean moving=!hex.equalsIgnoreCase(beforeHex);
        String proposedLocation=proposedScene.optString("location","").trim();
        String previousLocation=currentScene==null?"":currentScene.optString("location","").trim();

        if(!hex.equalsIgnoreCase(proposedScene.optString("hex_id","").trim())){
            proposedScene.put("hex_id",hex);changed=true;
            if(warnings!=null)warnings.put("O hex da cena foi sincronizado com o Atlas, que é a fonte de verdade da posição.");
        }

        JSONObject bestHere=bestConfirmedAtHex(db,packageId,hex);
        JSONObject proposedKnown=exactConfirmedLocation(db,packageId,proposedLocation);
        JSONObject previousKnown=exactConfirmedLocation(db,packageId,previousLocation);

        if(proposedLocation.length()>0){
            if(proposedKnown.length()>0){
                String locationHex=resolvedHex(proposedKnown);
                if(locationHex.equalsIgnoreCase(hex)){
                    String canonical=proposedKnown.optString("name",proposedLocation).trim();
                    if(!canonical.equals(proposedLocation)){proposedScene.put("location",canonical);changed=true;}
                }else{
                    String replacement=bestHere.length()>0?bestHere.optString("name","Esta região"):"Esta região";
                    proposedScene.put("location",replacement);recordRepair(envelope,proposedLocation,replacement,hex,"catalog_location_on_other_hex");changed=true;
                    if(warnings!=null)warnings.put("O local da cena foi corrigido porque "+proposedLocation+" pertence a outro ponto confirmado do Atlas.");
                }
            }
            // Local não catalogado é permitido como sublocal: quarto, taverna, rua etc.
        }else if(moving){
            String replacement=bestHere.length()>0?bestHere.optString("name","Esta região"):"Esta região";
            proposedScene.put("location",replacement);changed=true;
            if(previousLocation.length()>0)recordRepair(envelope,previousLocation,replacement,hex,"map_moved_without_new_scene_location");
        }else if(previousLocation.length()>0&&previousKnown.length()>0&&!resolvedHex(previousKnown).equalsIgnoreCase(hex)){
            String replacement=bestHere.length()>0?bestHere.optString("name","Esta região"):"Esta região";
            proposedScene.put("location",replacement);recordRepair(envelope,previousLocation,replacement,hex,"persisted_scene_drift");changed=true;
            if(warnings!=null)warnings.put("A localização persistida da cena divergia do Atlas e foi reconciliada.");
        }
        return changed;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Repara imediatamente o estado persistido após movimento manual/viagem/restauração. */
    public static JSONObject reconcilePersistedScene(AppDb db, String reason) throws Exception {/*BR_JSON_WRAP*/try{
        if(db==null)return new JSONObject();
        JSONObject map=db.getMapState(),scene=db.getSceneState();String packageId=map.optString("map_package_id","").trim(),hex=map.optString("current_hex_id","").trim();
        if(packageId.length()==0||hex.length()==0||db.getRawMapHex(packageId,hex).length()==0)return scene;
        String sceneHex=scene.optString("hex_id","").trim();String location=scene.optString("location","").trim();boolean moving=!hex.equalsIgnoreCase(sceneHex);boolean changed=false;
        if(moving){scene.put("hex_id",hex);changed=true;JSONObject best=bestConfirmedAtHex(db,packageId,hex);scene.put("location",best.length()>0?best.optString("name","Esta região"):"Esta região");}
        else if(location.length()>0){JSONObject known=exactConfirmedLocation(db,packageId,location);if(known.length()>0&&!resolvedHex(known).equalsIgnoreCase(hex)){JSONObject best=bestConfirmedAtHex(db,packageId,hex);scene.put("location",best.length()>0?best.optString("name","Esta região"):"Esta região");changed=true;}}
        if(changed){db.saveSceneStateChecked(scene);try{db.addAuditEvent("atlas","Cena reconciliada com a posição canônica",reason==null?"posição atual":reason,"{}",scene.toString(),true);}catch(Exception ignored){AppLog.ignored("AtlasScenePosition","Falha tolerada ao auditar reconciliação.",ignored);}}
        return scene;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /**
     * Repara a última narração já persistida quando o próprio scene_state ainda
     * carrega um local catalogado comprovadamente incompatível com o Atlas atual.
     * Não tenta inferir sublocais desconhecidos e não substitui menções de destino.
     */
    public static String repairPersistedNarration(String text, AppDb db) {
        if(text==null||text.length()==0||db==null)return text==null?"":text;
        try{
            JSONObject map=db.getMapState(),scene=db.getSceneState();
            String packageId=map.optString("map_package_id","").trim(),hex=map.optString("current_hex_id","").trim(),from=scene.optString("location","").trim();
            if(packageId.length()==0||hex.length()==0||from.length()<2||db.getRawMapHex(packageId,hex).length()==0)return text;
            JSONObject known=exactConfirmedLocation(db,packageId,from);
            if(known.length()==0||resolvedHex(known).equalsIgnoreCase(hex))return text;
            JSONObject best=bestConfirmedAtHex(db,packageId,hex);String to=best.length()>0?best.optString("name","Esta região"):"Esta região";
            JSONObject envelope=new JSONObject();recordRepair(envelope,from,to,hex,"stored_narration_scene_drift");
            return repairNarration(text,envelope);
        }catch(Exception ignored){AppLog.ignored("AtlasScenePosition","Falha tolerada ao reparar narração persistida.",ignored);return text;}
    }

    /** Snapshot sem escrita, usado no barramento do Mestre para nunca enviar uma cena geograficamente obsoleta. */
    public static JSONObject authoritativeSceneSnapshot(AppDb db) {/*BR_JSON_WRAP*/try{
        if(db==null)return new JSONObject();
        JSONObject scene=db.getSceneState(),map=db.getMapState();String packageId=map.optString("map_package_id","").trim(),hex=map.optString("current_hex_id","").trim();
        try{
            JSONObject out=new JSONObject(scene.toString());if(packageId.length()==0||hex.length()==0||db.getRawMapHex(packageId,hex).length()==0)return out;
            String sceneHex=out.optString("hex_id","").trim(),location=out.optString("location","").trim();boolean moving=!hex.equalsIgnoreCase(sceneHex);out.put("hex_id",hex);
            if(moving){JSONObject best=bestConfirmedAtHex(db,packageId,hex);out.put("location",best.length()>0?best.optString("name","Esta região"):"Esta região");}
            else if(location.length()>0){JSONObject known=exactConfirmedLocation(db,packageId,location);if(known.length()>0&&!resolvedHex(known).equalsIgnoreCase(hex)){JSONObject best=bestConfirmedAtHex(db,packageId,hex);out.put("location",best.length()>0?best.optString("name","Esta região"):"Esta região");}}
            out.put("atlas_position_authority",true);return out;
        }catch(Exception ignored){return scene;}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /**
     * Corrige somente referências que descrevem o local ATUAL quando o validador
     * comprovou um drift. Não substitui menções remotas como "viajar para Yartar".
     */
    public static String repairNarration(String text, JSONObject validatedEnvelope) {
        if(text==null||text.length()==0||validatedEnvelope==null)return text==null?"":text;
        JSONObject repair=validatedEnvelope.optJSONObject(REPAIR_KEY);if(repair==null)return text;
        String from=repair.optString("from","").trim(),to=repair.optString("to","").trim();if(from.length()<2||to.length()<2||same(from,to))return text;
        String target=narrativeTarget(to);
        String quoted=Pattern.quote(from);
        Pattern anchored=Pattern.compile("(\\b(?:estamos|está|estao|estão|permanece|continua|acorda|chega|encontra-se|se encontra)\\s+(?:em|na|no|nas|nos)\\s+)(?:a\\s+cidade\\s+de\\s+)?"+quoted+"\\b|(\\b(?:em|na|no|nas|nos|pelas ruas de|nas ruas de|no centro de|na cidade de|dentro de)\\s+)"+quoted+"\\b",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
        Matcher matcher=anchored.matcher(text);StringBuffer out=new StringBuffer();
        while(matcher.find()){
            String prefix=matcher.group(1)!=null?matcher.group(1):matcher.group(2);String replacement=prefix+target;matcher.appendReplacement(out,Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);String fixed=out.toString();
        // Se o texto abre declarando diretamente o local incorreto, corrige apenas a abertura.
        Pattern opening=Pattern.compile("^(\\s*(?:#+\\s*)?)(?:a\\s+cidade\\s+de\\s+)?"+quoted+"(?=\\b|[,.:—–-])",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
        Matcher first=opening.matcher(fixed);if(first.find())fixed=first.replaceFirst(Matcher.quoteReplacement(first.group(1)+displayTarget(to)));
        return fixed;
    }

    public static JSONObject bestConfirmedAtHex(AppDb db,String packageId,String hex){
        if(db==null||packageId==null||hex==null)return new JSONObject();List<JSONObject> rows=db.getMapLocationsForHex(packageId,hex);JSONObject best=null;int score=Integer.MIN_VALUE;
        for(JSONObject row:rows){if(!confirmed(row)||row.optString("name","").trim().length()==0)continue;int p=priority(row.optString("type",""));if(row.optInt("manual_correction",0)==1)p+=8;if(p>score){best=row;score=p;}}
        return best==null?new JSONObject():best;
    }

    private static JSONObject exactConfirmedLocation(AppDb db,String packageId,String name){
        if(db==null||packageId==null||name==null||name.trim().length()<2)return new JSONObject();String wanted=normalize(name);List<JSONObject> rows=db.findConfirmedLocationsByQuery(packageId,name,10);
        for(JSONObject row:rows){if(!confirmed(row))continue;if(wanted.equals(normalize(row.optString("name",""))))return row;try{JSONObject raw=new JSONObject(row.optString("raw_json","{}"));JSONArray aliases=raw.optJSONArray("aliases");if(aliases!=null)for(int i=0;i<aliases.length();i++)if(wanted.equals(normalize(aliases.optString(i,""))))return row;}catch(Exception ignored){AppLog.ignored("AtlasScenePosition","Alias de local inválido.",ignored);}}
        return new JSONObject();
    }

    private static boolean confirmed(JSONObject row){if(row==null)return false;String hex=resolvedHex(row);if(hex.length()==0)return false;String s=row.optString("placement_status","").toLowerCase(Locale.ROOT);return !s.contains("review")&&!s.contains("approx")&&!s.contains("unassigned");}
    private static String resolvedHex(JSONObject row){String h=row==null?"":row.optString("hex_id","").trim();if(h.length()==0&&row!=null)h=row.optString("linked_hex_id","").trim();return h;}
    private static int priority(String type){String v=normalize(type);if(v.equals("cidade")||v.equals("city")||v.equals("capital"))return 140;if(v.equals("vila")||v.equals("town")||v.equals("village"))return 120;if(v.equals("porto")||v.equals("port"))return 108;if(v.equals("fortaleza")||v.equals("castelo")||v.equals("fortress")||v.equals("castle"))return 100;if(v.equals("templo")||v.equals("ruina")||v.equals("ruína")||v.equals("masmorra")||v.equals("dungeon")||v.equals("caverna"))return 82;return 60;}
    private static void recordRepair(JSONObject envelope,String from,String to,String hex,String reason)throws Exception{/*BR_JSON_WRAP*/try{if(envelope==null||from==null||from.trim().length()==0||same(from,to))return;envelope.put(REPAIR_KEY,new JSONObject().put("from",from.trim()).put("to",to).put("effective_hex",hex).put("reason",reason));}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String normalize(String value){String n=Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();return n;}
    private static boolean same(String a,String b){return normalize(a).equals(normalize(b));}
    private static String displayTarget(String to){return "Esta região".equalsIgnoreCase(to)?"Esta região":to;}
    private static String narrativeTarget(String to){return "Esta região".equalsIgnoreCase(to)?"esta região":to;}
}
