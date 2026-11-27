package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Mestre local de contingência. Ele mantém a campanha utilizável sem internet
 * e sem chave Gemini. O motor não inventa valores mecânicos: descreve a cena,
 * propõe decisões e encaminha incertezas ao rolador local.
 */
public final class OfflineMasterEngine {
    public JSONObject generate(String mode,String action,MasterOrchestrator.Plan plan,List<JSONObject> localHits){
        String safeMode=mode==null?"Narrador":mode;
        String inputChannel=plan==null?null:plan.inputChannel;
        BraseiroNarrativeDirector.Intent intent=BraseiroNarrativeDirector.classify(action,inputChannel);
        if("Regras RAW/RAI".equals(safeMode)||"Sortear dos livros".equals(safeMode))return manualAnswer(safeMode,action,localHits);
        if(BraseiroNarrativeDirector.isNonAdvancing(action,inputChannel))return clarification(action,plan,intent);
        return narrative(action,plan);
    }

    private JSONObject clarification(String action,MasterOrchestrator.Plan plan,BraseiroNarrativeDirector.Intent intent){/*BR_JSON_WRAP*/try{
        JSONObject root=parseObject(plan==null?"{}":plan.compactContext);JSONObject scene=root.optJSONObject("scene");if(scene==null)scene=new JSONObject();
        JSONObject memory=new JSONObject();JSONObject checkpoint=new JSONObject();
        String previous=root.optJSONObject("story")==null?"":root.optJSONObject("story").optString("last_narration","");
        try{checkpoint.put("narration",previous).put("location",scene.optString("location","esta região"));memory.put("checkpoint",checkpoint);}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar checkpoint local.",ignored);}
        StringBuilder text=new StringBuilder();
        if(intent==BraseiroNarrativeDirector.Intent.RECAP_QUERY){text.append(previous.length()>0?"Até o ponto em que a cena foi interrompida: "+clean(previous,1000):"Ainda não há uma cena anterior suficiente para recapitular com segurança.");}
        else if(intent==BraseiroNarrativeDirector.Intent.SPATIAL_CLARIFICATION){text.append("Os arredores imediatos continuam sendo ").append(clean(scene.optString("location","esta região"),100)).append(". A posição relativa, as passagens visíveis e a disposição dos presentes permanecem as mesmas de antes.");}
        else if(intent==BraseiroNarrativeDirector.Intent.PLANNING){text.append("A ideia permanece considerada em pensamento, sem execução imediata. O ambiente e os presentes continuam exatamente no mesmo instante.");}
        else{text.append("Diante do que pode ser observado no momento, os detalhes visíveis confirmam o que já foi percebido na cena, sem novos acontecimentos por enquanto.");}
        JSONObject out=emptyEnvelopeWithNarration(text.toString());try{out.put("_offline_master",new JSONObject().put("enabled",true).put("non_advancing",true));out.put("_non_advancing",true).put("_narrative_intent",intent.name());}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao marcar esclarecimento local.",ignored);}return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private JSONObject narrative(String action,MasterOrchestrator.Plan plan){/*BR_JSON_WRAP*/try{
        JSONObject root=parseObject(plan==null?"{}":plan.compactContext);
        JSONObject bus=root.optJSONObject("game_bus");if(bus==null)bus=new JSONObject();
        JSONObject scene=firstObject(root.optJSONObject("scene"),bus.optJSONObject("scene"));
        JSONObject story=root.optJSONObject("story");if(story==null)story=bus.optJSONObject("story");if(story==null)story=new JSONObject();
        JSONObject rules=root.optJSONObject("rules_frame");if(rules==null)rules=new JSONObject();
        JSONArray heroes=bus.optJSONArray("active_heroes_full");if(heroes==null)heroes=new JSONArray();
        JSONArray npcs=bus.optJSONArray("npcs");if(npcs==null)npcs=new JSONArray();

        String catalogLocation=extractBetween(action,"LOCAL NOMEADO:",";");
        String location=clean(catalogLocation.length()>0?catalogLocation:scene.optString("location","esta região"),90);if(location.length()==0)location="esta região";
        String weather=clean(scene.optString("weather",""),70);
        String danger=clean(scene.optString("danger",""),120);
        String objective=clean(scene.optString("objective",""),150);
        String previous=clean(story.optString("last_narration",""),480);
        String declared=playerFacingAction(action);
        JSONObject hero0=heroes.length()>0?heroes.optJSONObject(0):null;
        String actor=hero0!=null?clean(hero0.optString("name",""),70):"";
        String address=heroes.length()==1?"você":"vocês";
        String heroArmor=hero0!=null?clean(hero0.optString("visual_description",hero0.optString("armor","")),90):"";
        String heroEquip=hero0!=null?clean(hero0.optString("equipment",hero0.optString("weapons","")),90):"";
        String npc=presentNpcName(scene);
        String verified=resultMarker(action);
        String occasion=plan==null?"continuation":plan.barbaraStoryOccasion;if(occasion==null||occasion.trim().length()==0)occasion="continuation";
        boolean opening=isOpeningDirective(action)||"campaign_opening".equals(occasion);
        boolean presentation=WorldOnboardingNarrativeGuard.isPresentation(occasion);
        String catalog=clean(extractBetween(action,"REFERÊNCIA DO CATÁLOGO", "Use este local como âncora"),520);boolean localWater=!action.toUpperCase(new Locale("pt","BR")).contains("HIDROGRAFIA LOCAL CONFIRMADA PELO ATLAS: NENHUMA");

        StringBuilder text=new StringBuilder();
        if(presentation){
            text.append(heroes.length()==1?"Você está em ":"Vocês estão em ").append(location).append(". ");
            if(weather.length()>0)text.append("O tempo está ").append(lowerFirst(weather)).append(". ");
            if(catalog.length()>0){String fact=naturalizeCatalog(catalog,localWater);if(fact.length()>0)text.append(clean(fact,320)).append(' ');}
            if(hero0!=null){
                if(!actor.isEmpty()&&heroArmor.length()>0)text.append(actor).append(", você veste ").append(lowerFirst(heroArmor)).append(". ");
            }
            if(npc.length()>0)text.append(npc).append(" está presente. ");
            if(danger.length()>0)text.append("Há um sinal concreto de perigo: ").append(lowerFirst(danger)).append(". ");
            if(objective.length()>0)text.append("O objetivo já estabelecido continua sendo ").append(lowerFirst(objective)).append(". ");
            text.append(heroes.length()==1?"O que você faz?":"O que vocês fazem?");
        }else{
            text.append("Em ").append(location).append(", ");
            if(weather.length()>0)text.append("sob o tempo ").append(lowerFirst(weather)).append(", ");
            if(declared.length()>0)text.append(lowerFirst(declared)).append(". ");
            else text.append(address).append(" permanece").append(heroes.length()==1?"":"m").append(" no local. ");
            if(hero0!=null&&declared.length()>0&&heroEquip.length()>0){
                // No additional movement, grip or weather is inferred from equipment.
            }
            if(npc.length()>0)text.append(npc).append(" está presente. ");
            String speaking=situatedSocialReply(declared,npc,danger);
            if(speaking.length()>0)text.append("\n\n").append(speaking).append("\n\n");
            if(verified.length()>0){
                if("sucesso".equals(verified))text.append("A tentativa funciona, e a situação se abre a partir desse resultado. ");
                else if("falha".equals(verified))text.append("A tentativa não alcança o efeito pretendido, e a situação responde a essa ocorrência. ");
                else text.append("O resultado obtido passa a definir o que acontece a seguir. ");
            }else if(danger.length()>0)text.append("A ameaça continua presente: ").append(lowerFirst(danger)).append(". ");
            if(previous.length()>0)text.append("A atenção se volta para os detalhes imediatos ao redor. ");
            if(objective.length()>0)text.append("Adiante, a busca por ").append(lowerFirst(objective)).append(" continua no horizonte. ");
            text.append(heroes.length()==1?"O que você faz?":"O que vocês fazem?");
        }

        JSONObject out=emptyEnvelope();
        try{
            String narration=GeneratedTextSanitizer.narrative(text.toString());
            out.put("narration",narration);
            out.put("visual_prompt","Cena de fantasia em "+location+", atmosfera "+(weather.length()>0?weather:"coerente com o local")+", ação concreta, composição cinematográfica, sem texto na imagem");
            out.put("options",options(location,npc,danger,objective,declared));
            JSONObject suggested=rules.optJSONObject("suggested_check");
            if(!presentation&&verified.length()==0&&suggested!=null&&suggested.length()>0){
                JSONObject pending=new JSONObject();pending.put("required",true).put("action",declared.length()>0?declared:"ação declarada");
                copy(suggested,pending,"type","ability","skill","mode","save_type","thief_skill","general_skill","mechanic");
                JSONArray actors=rules.optJSONArray("actors");if(actors!=null&&actors.length()==1)pending.put("character",actors.optJSONObject(0).optString("name"));
                if(!pending.has("type"))pending.put("type","ability");
                if(!pending.has("ability"))pending.put("ability","Sabedoria");
                if(!"mystara".equalsIgnoreCase(bus.optString("world_profile")))pending.put("dc",10);
                out.put("pending_test",pending);out.put("options",new JSONArray());
            }
            out.put("_offline_master",new JSONObject().put("enabled",true).put("mechanics_authority","local").put("reason","contingency"));
        }catch(Exception failure){return emptyEnvelopeWithNarration("A cena não pôde ser concluída com segurança. Nenhuma consequência foi aplicada.");}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static String completePresentationWithBarbara(String existingNarration,String occasion,MasterOrchestrator.Plan plan,JSONObject sceneState){
        // A valid beat is never extended using a second, unrelated story.
        // No local source can invent the missing hook or new surroundings.
        String text=existingNarration==null?"":existingNarration.trim();
        if(!"first_arrival".equals(occasion)||sceneState==null||text.isEmpty()||WorldOnboardingNarrativeGuard.problem(text,occasion).isEmpty())return text;
        // Do not append anything after a completed beat with dialogue or an offered decision.
        // Only an explicitly incomplete arrival draft may be oriented from confirmed geography.
        String folded=NpcIdentityNormalizer.key(text);
        if(text.contains("?")||text.contains("—")||!folded.matches(".*\\b(chegou|chegam|chegamos|chegada|aproxima|aproximam|entrada)\\b.*"))return text;
        String location=sceneState.optString("location","").trim(),region=sceneState.optString("region","").trim(),geography=sceneState.optString("geography","").trim();
        if(location.isEmpty()||region.isEmpty()||geography.isEmpty())return text;
        // Reuse only confirmed scene facts; never create people, events or travel.
        String candidate=text+"\n\nA chegada a "+location+", na região de "+region+", tem esta referência geográfica: "+geography+".";
        return WorldOnboardingNarrativeGuard.problem(candidate,occasion).isEmpty()?candidate:text;
    }

    private static boolean isOpeningDirective(String action){String n=action==null?"":action.toUpperCase(new Locale("pt","BR"));return n.contains("INÍCIO AUTOMÁTICO")||n.contains("INICIO AUTOMATICO")||n.contains("PONTO INICIAL CATALOGADO DESTA AVENTURA");}
    private static String playerFacingAction(String action){
        String value=clean(action,1400);if(value.length()==0)return "";
        if(isOpeningDirective(value))return "";
        int marker=value.indexOf("MECANICA_LOCAL_VERIFICADA");if(marker>=0)value=value.substring(0,marker).trim();
        value=value.replaceAll("(?i)BRAS_[A-Z0-9_]+","").replaceAll("\\s+"," ").trim();
        if(value.length()>320)value=value.substring(0,320).trim()+"…";
        return value;
    }
    private static String extractBetween(String source,String start,String end){
        if(source==null)return "";String low=source.toLowerCase(new Locale("pt","BR")),a=start.toLowerCase(new Locale("pt","BR")),b=end.toLowerCase(new Locale("pt","BR"));int i=low.indexOf(a);if(i<0)return "";i+=a.length();int j=low.indexOf(b,i);if(j<0)j=source.length();return clean(source.substring(i,j).replaceAll("^[ :.-]+|[ .;-]+$",""),700);
    }
    private static String naturalizeCatalog(String catalog,boolean localWater){
        String c=clean(catalog,520);if(c.length()==0)return "";c=c.replaceFirst("^[^:]{0,80}:\\s*","");
        if(!localWater){StringBuilder keep=new StringBuilder();for(String sentence:c.split("(?<=[.!?])\\s+")){String n=sentence.toLowerCase(new Locale("pt","BR"));if(n.matches(".*\\b(rio|rios|riacho|riachos|c[oó]rrego|c[oó]rregos|canal|canais|margem|margens|costa|lago|lagos|mar|mares|delta|estu[aá]rio|porto|portos|doca|docas|navega[cç][aã]o|chionthar)\\b.*"))continue;if(keep.length()>0)keep.append(' ');keep.append(sentence.trim());}c=clean(keep.toString(),520);}
        if(c.length()==0)return "";c=c.replaceAll("(?i)^cidade de ","É uma cidade de ");if(!c.matches(".*[.!?…]$"))c+=".";return c;
    }
    private static String weatherSentence(String weather){String w=clean(weather,70);if(w.length()==0)return "";return "O tempo acompanha a chegada: "+lowerFirst(w)+".";}

    private JSONObject manualAnswer(String mode,String query,List<JSONObject> hits){/*BR_JSON_WRAP*/try{
        if(hits==null||hits.isEmpty())return emptyEnvelopeWithNarration("Não há trecho local verificável para responder a esse pedido. Nenhuma regra foi inventada.");
        JSONObject out=emptyEnvelope();JSONArray sources=new JSONArray();StringBuilder narration=new StringBuilder();
        narration.append("Consulta local aos manuais — ").append(clean(query,180)).append("\n\n");
        int limit=Math.min(4,hits.size());
        for(int i=0;i<limit;i++){
            JSONObject hit=hits.get(i);String book=clean(hit.optString("title",hit.optString("manual_title","Manual")),100);int page=hit.optInt("page",hit.optInt("page_number",0));String excerpt=clean(hit.optString("text",hit.optString("excerpt",hit.optString("content",""))),650);
            if("Sortear dos livros".equals(mode)&&i>0)break;
            narration.append(book);if(page>0)narration.append(", p. ").append(page);narration.append(": ").append(excerpt.length()>0?excerpt:"trecho indexado sem prévia textual").append("\n\n");
            try{sources.put(new JSONObject().put("book",book).put("page",page).put("excerpt",excerpt));}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar campo JSON local.",ignored);}
        }
        narration.append("A resposta acima reproduz somente o material encontrado no índice do aparelho. Interpretação que não esteja explícita deve ser decidida pelo jogador ou pelo Mestre humano.");
        try{out.put("narration",narration.toString().trim()).put("sources",sources).put("rules_verified",sources.length()>0).put("_offline_master",new JSONObject().put("enabled",true).put("manual_lookup",true));}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar campo JSON local.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray options(String location,String npc,String danger,String objective,String action)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();
        String cleanLoc=clean(location.length()>0?location:"o local",42);
        if(npc.length()>0)out.put(option("FALAR COM "+npc.toUpperCase(Locale.ROOT),"Falo com "+npc+" sobre o que está acontecendo agora em "+cleanLoc+"."));
        if(danger.length()>0&&out.length()<3)out.put(option("REAGIR A "+clean(danger,48).toUpperCase(Locale.ROOT),"Mudo minha posição e preparo uma resposta direta a "+clean(danger,70)+" em "+cleanLoc+"."));
        if(objective.length()>0&&out.length()<3)out.put(option("AVANÇAR NO OBJETIVO","Dou um passo concreto em direção a "+objective+" a partir de "+cleanLoc+"."));
        if(out.length()<3)out.put(option("EXAMINAR "+cleanLoc.toUpperCase(Locale.ROOT),"Examino os detalhes, objetos e avisos visíveis ao alcance em "+cleanLoc+"."));
        if(out.length()<3)out.put(option("SONDAR OS PRESENTES EM "+cleanLoc.toUpperCase(Locale.ROOT),"Abordo quem está próximo em "+cleanLoc+" para ouvir o que comentam e observar suas reações."));
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject option(String label,String action)throws Exception{/*BR_JSON_WRAP*/try{return new JSONObject().put("label",label).put("description",action).put("action",action);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject emptyEnvelope(){/*BR_JSON_WRAP*/try{try{return new JSONObject().put("protocol_version",MasterProtocolV1.VERSION).put("narration","").put("visual_prompt","").put("options",new JSONArray()).put("pending_test",JSONObject.NULL).put("state_updates",new JSONArray()).put("combat",new JSONObject().put("action","none").put("enemies",new JSONArray()).put("updates",new JSONArray())).put("scene_state",new JSONObject()).put("npc_updates",new JSONArray()).put("npc_cognition_updates",new JSONArray()).put("knowledge_updates",new JSONArray()).put("quest_updates",new JSONArray()).put("faction_updates",new JSONArray()).put("guild_updates",new JSONArray()).put("clock_updates",new JSONArray()).put("world_events",new JSONArray()).put("event_resolutions",new JSONArray()).put("pending_resolutions",new JSONArray()).put("map_updates",new JSONArray()).put("tactical_updates",new JSONArray()).put("journal_updates",new JSONArray()).put("narrative_state",new JSONObject()).put("audio_plan",new JSONObject()).put("sources",new JSONArray()).put("rules_verified",false);}catch(Exception e){AppLog.w("OfflineMasterEngine","Falha ao criar envelope local.",e);return new JSONObject();}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject emptyEnvelopeWithNarration(String text){/*BR_JSON_WRAP*/try{JSONObject out=emptyEnvelope();try{out.put("narration",text);}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar campo JSON local.",ignored);}return out;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){AppLog.w("OfflineMasterEngine","Falha ao criar envelope local.",e);return new JSONObject();}}
    private static JSONObject firstObject(JSONObject a,JSONObject b){return a!=null?a:(b!=null?b:new JSONObject());}
    private static void copy(JSONObject from,JSONObject to,String... keys)throws Exception{/*BR_JSON_WRAP*/try{for(String key:keys)if(from.has(key))to.put(key,from.opt(key));}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String resultMarker(String action){String n=clean(action,900).toLowerCase(new Locale("pt","BR"));if(!n.contains("mecanica_local_verificada"))return "";if(n.contains("resultado=sucesso")||n.contains("resultado: sucesso"))return "sucesso";if(n.contains("resultado=falha")||n.contains("resultado: falha"))return "falha";return "resultado registrado";}
    /** Only publicly present and named NPCs may speak in the offline fallback. */
    static String presentNpcName(JSONObject scene) {
        if(scene==null)return "";
        JSONArray present=scene.optJSONArray("present_npcs");
        if(present==null)try{present=new JSONArray(scene.optString("present_npcs_json","[]"));}catch(Exception ignored){return "";}
        for(int i=0;present!=null&&i<present.length();i++){
            Object value=present.opt(i);
            if(value instanceof JSONObject){
                JSONObject n=(JSONObject)value;
                if(n.optBoolean("anonymous",false)||n.optBoolean("transient",false))continue;
                String name=clean(n.optString("name",""),70);
                if(!name.isEmpty())return name;
            }else if(value instanceof String){
                String name=clean((String)value,70);
                if(!name.isEmpty()&&!name.equalsIgnoreCase("null"))return name;
            }
        }
        return "";
    }

    /** An existing speaker can respond to an explicit approach without invented secrets. */
    static String situatedSocialReply(String action,String npc,String danger) {
        if(npc==null||npc.trim().isEmpty()||danger!=null&&!danger.trim().isEmpty())return "";
        String a=action==null?"":action.toLowerCase(new Locale("pt","BR"));
        if(!(a.contains("falo com")||a.contains("falar com")||a.contains("converso com")
                ||a.contains("pergunto")||a.contains("cumprimento")||a.contains("saúdo")
                ||a.contains("saudo")||a.contains("dialogo com")))return "";
        return "— Estou ouvindo — responde "+clean(npc,70)+", deixando espaço para a conversa.";
    }

    private static String lowerFirst(String value){if(value==null||value.length()==0)return "";return Character.toLowerCase(value.charAt(0))+value.substring(1);}
    private static String clean(String value,int max){String out=value==null?"":value.replace('\n',' ').replace('\r',' ').replaceAll("\\s+"," ").trim();if(out.length()<=max)return out;return out.substring(0,Math.max(1,max-1)).trim()+"…";}
}
