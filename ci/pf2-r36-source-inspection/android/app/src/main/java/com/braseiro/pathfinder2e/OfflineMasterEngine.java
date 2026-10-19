package com.braseiro.pathfinder2e;

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
        BraseiroNarrativeDirector.Intent intent=BraseiroNarrativeDirector.classify(action);
        if("Regras RAW/RAI".equals(safeMode)||"Sortear dos livros".equals(safeMode)||intent==BraseiroNarrativeDirector.Intent.RULE_QUERY)return manualAnswer(safeMode,action,localHits);
        if(BraseiroNarrativeDirector.isNonAdvancing(intent))return clarification(action,plan,intent);
        return narrative(action,plan);
    }

    private JSONObject clarification(String action,MasterOrchestrator.Plan plan,BraseiroNarrativeDirector.Intent intent){
        JSONObject root=parseObject(plan==null?"{}":plan.compactContext);JSONObject scene=root.optJSONObject("scene");if(scene==null)scene=new JSONObject();
        JSONObject memory=new JSONObject();JSONObject checkpoint=new JSONObject();
        String previous=root.optJSONObject("story")==null?"":root.optJSONObject("story").optString("last_narration","");
        try{checkpoint.put("narration",previous).put("location",scene.optString("location","esta região"));memory.put("checkpoint",checkpoint);}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar checkpoint local.",ignored);}
        StringBuilder text=new StringBuilder();
        if(intent==BraseiroNarrativeDirector.Intent.RECAP_QUERY){text.append(previous.length()>0?"Até o ponto em que a cena foi interrompida: "+clean(previous,1000):"Ainda não há uma cena anterior suficiente para recapitular com segurança.");}
        else if(intent==BraseiroNarrativeDirector.Intent.SPATIAL_CLARIFICATION){text.append("A cena continua em ").append(clean(scene.optString("location","esta região"),100)).append(". O Mestre local só confirma a geometria já estabelecida; a pergunta não faz o tempo passar nem cria um novo obstáculo.");}
        else if(intent==BraseiroNarrativeDirector.Intent.PLANNING){text.append("Isso fica tratado apenas como uma possibilidade de plano. Nada foi executado e a cena permanece exatamente no mesmo instante.");}
        else{text.append("A pergunta é uma clarificação sobre a situação atual. Sem uma fonte local ou um detalhe já estabelecido que permita afirmar mais, o Mestre preserva o ponto da cena em vez de inventar uma resposta que altere o mundo.");}
        JSONObject out=emptyEnvelopeWithNarration(text.toString());try{out.put("_offline_master",new JSONObject().put("enabled",true).put("non_advancing",true));out.put("_non_advancing",true).put("_narrative_intent",intent.name());}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao marcar esclarecimento local.",ignored);}return out;
    }

    private JSONObject narrative(String action,MasterOrchestrator.Plan plan){
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
        JSONObject primaryHero=heroes.length()>0?heroes.optJSONObject(0):null;
        String actor=primaryHero!=null?clean(primaryHero.optString("name","A companhia"),70):"A companhia";
        String armor=primaryHero!=null?clean(primaryHero.optString("armor",primaryHero.optString("armor_name","")),60):"";
        if(armor.isEmpty())armor="as roupas reforçadas de viagem";
        String weapons=primaryHero!=null?clean(primaryHero.optString("weapons",primaryHero.optString("weapon","")),60):"";
        if(weapons.isEmpty())weapons="as armas de prontidão";
        String npc=npcs.length()>0?clean(npcs.optJSONObject(0).optString("name",""),70):"";
        String verified=resultMarker(action);
        boolean opening=isOpeningDirective(action);
        String catalog=clean(extractBetween(action,"REFERÊNCIA DO CATÁLOGO", "Use este local como âncora"),520);boolean localWater=!action.toUpperCase(new Locale("pt","BR")).contains("HIDROGRAFIA LOCAL CONFIRMADA PELO ATLAS: NENHUMA");

        StringBuilder text=new StringBuilder();
        if(opening){
            text.append("A atmosfera de ").append(location).append(" se impõe antes de qualquer gesto. ");
            if(weather.length()>0)text.append(weatherSentence(weather)).append(" ");
            else text.append("O ar traz consigo o cheiro denso de terra úmida, lenha e vegetação selvagem. ");
            if(catalog.length()>0)text.append(naturalizeCatalog(catalog,localWater)).append(" ");
            else text.append("O chão sob as botas revela a rudeza do terreno, enquanto construções e trilhas ao redor mostram pessoas em pleno movimento — atarefadas em seus afazeres, vigiando ou descarregando fardos. ");
            text.append("Vocês mantêm a postura firme. ");
            if(primaryHero!=null){
                text.append(actor).append(", vestindo ").append(armor).append(", você sente o peso familiar de ").append(weapons)
                    .append(" ao alcance dos dedos, com o vento frio fazendo tremular a barra do manto enquanto o vapor da respiração se dissipa no ar. ");
            }else{
                text.append(actor).append(" avança mantendo os sentidos despertos e o equipamento firme ao corpo. ");
            }
            if(npc.length()>0)text.append(npc).append(" está em atividade por perto, erguendo o olhar e reagindo à presença da companhia. ");
            if(danger.length()>0)text.append("Um sinal nítido de tensão exige vigilância: ").append(lowerFirst(danger)).append(". ");
            if(objective.length()>0)text.append("O motivo que guiou o grupo até aqui permanece à vista: ").append(lowerFirst(objective)).append(". ");
            text.append("Diante de vocês, o relevo e as escolhas imediatas se abrem com peso real.");
        }else{
            text.append("Em ").append(location).append(", ");
            if(declared.length()>0)text.append(lowerFirst(declared)).append(". ");
            else text.append(actor).append(" ajusta a postura e examina o entorno com atenção. ");
            if(weather.length()>0)text.append("O clima segue evidente na pele e na respiração. ");
            if(primaryHero!=null){
                text.append("O peso de ").append(weapons).append(" e a rigidez de ").append(armor).append(" ancoram cada movimento na lama e no cascalho sob as botas. ");
            }
            if(npc.length()>0)text.append(npc).append(" percebe a mudança, interrompe o que fazia e reage de forma direta, sem antecipar o passo seguinte da companhia. ");
            if(verified.length()>0){
                if("sucesso".equals(verified))text.append("A tentativa funciona, e a situação se abre exatamente a partir desse resultado. ");
                else if("falha".equals(verified))text.append("A tentativa não alcança o efeito pretendido, e a situação responde a essa falha. ");
                else text.append("O resultado obtido passa a definir o rumo imediato. ");
            }else if(danger.length()>0)text.append("A ameaça continua presente: ").append(lowerFirst(danger)).append(". ");
            if(previous.length()>0)text.append("O que já estava em jogo continua reconhecível, mas a cena muda com a ação atual. ");
            if(objective.length()>0)text.append("Adiante, ainda está em jogo ").append(lowerFirst(objective)).append(". ");
            text.append("O próximo passo permanece nas mãos da companhia.");
        }

        JSONObject out=emptyEnvelope();
        try{
            String narration=GeneratedTextSanitizer.narrative(text.toString());
            out.put("narration",narration);
            out.put("visual_prompt","Cena de fantasia em "+location+", atmosfera "+(weather.length()>0?weather:"coerente com o local")+", ação concreta, composição cinematográfica, sem texto na imagem");
            out.put("options",options(location,npc,danger,objective,declared));
            JSONObject suggested=rules.optJSONObject("suggested_check");
            // Fail-closed: o modo offline nunca inventa CD, atributo ou procedimento.
            // Só cria um teste quando o quadro PF2e local já traz uma CD comprovada.
            if(!opening&&verified.length()==0&&suggested!=null&&suggested.length()>0&&suggested.optInt("dc",0)>0){
                JSONObject pending=new JSONObject();pending.put("required",true).put("system","pathfinder_2e").put("action",declared.length()>0?declared:"ação declarada");
                copy(suggested,pending,"type","skill","mode","save_type","dc");
                JSONArray actors=rules.optJSONArray("actors");if(actors!=null&&actors.length()==1)pending.put("character",actors.optJSONObject(0).optString("name"));
                if(!pending.has("type"))pending.put("type","check");
                out.put("pending_test",pending);out.put("options",new JSONArray());
            }
            out.put("_offline_master",new JSONObject().put("enabled",true).put("mechanics_authority","pf2_engine_only").put("reason","contingency").put("invented_dc",false));
        }catch(Exception failure){return emptyEnvelopeWithNarration("A cena não pôde ser concluída com segurança. Nenhuma consequência foi aplicada.");}
        return out;
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

    private JSONObject manualAnswer(String mode,String query,List<JSONObject> hits){
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
    }

    private static JSONArray options(String location,String npc,String danger,String objective,String action)throws Exception{
        JSONArray out=new JSONArray();
        if(npc.length()>0)out.put(option("OUVIR "+npc.toUpperCase(Locale.ROOT),"Perguntar o que mudou para "+npc+" e comparar a resposta com a cena atual."));
        if(danger.length()>0)out.put(option("MUDAR A POSIÇÃO","Buscar cobertura ou distância em "+location+" antes de lidar com "+danger+"."));
        if(objective.length()>0)out.put(option("AVANÇAR NO OBJETIVO","Executar uma etapa concreta para "+objective+", preservando uma alternativa segura."));
        out.put(option("INVESTIGAR O ELEMENTO NOVO","Examinar o detalhe que surgiu após: "+clean(action,120)+"."));
        out.put(option("CRIAR OUTRA ABORDAGEM","Combinar observação, conversa e movimento para produzir uma solução diferente da ação anterior."));
        JSONArray limited=new JSONArray();for(int i=0;i<out.length()&&limited.length()<2;i++)limited.put(out.opt(i));return limited;
    }
    private static JSONObject option(String label,String action)throws Exception{return new JSONObject().put("label",label).put("description",action).put("action",action);}
    private static JSONObject emptyEnvelope(){try{return new JSONObject().put("protocol_version",MasterProtocolV1.VERSION).put("narration","").put("visual_prompt","").put("options",new JSONArray()).put("pending_test",JSONObject.NULL).put("state_updates",new JSONArray()).put("combat",new JSONObject().put("action","none").put("enemies",new JSONArray()).put("updates",new JSONArray())).put("scene_state",new JSONObject()).put("npc_updates",new JSONArray()).put("npc_cognition_updates",new JSONArray()).put("knowledge_updates",new JSONArray()).put("quest_updates",new JSONArray()).put("faction_updates",new JSONArray()).put("guild_updates",new JSONArray()).put("clock_updates",new JSONArray()).put("world_events",new JSONArray()).put("event_resolutions",new JSONArray()).put("pending_resolutions",new JSONArray()).put("map_updates",new JSONArray()).put("tactical_updates",new JSONArray()).put("journal_updates",new JSONArray()).put("narrative_state",new JSONObject()).put("audio_plan",new JSONObject()).put("sources",new JSONArray()).put("rules_verified",false);}catch(Exception e){AppLog.w("OfflineMasterEngine","Falha ao criar envelope local.",e);return new JSONObject();}}
    private static JSONObject emptyEnvelopeWithNarration(String text){JSONObject out=emptyEnvelope();try{out.put("narration",text);}catch(Exception ignored){AppLog.ignored("OfflineMasterEngine","Falha ao montar campo JSON local.",ignored);}return out;}
    private static JSONObject parseObject(String raw){try{return new JSONObject(raw==null?"{}":raw);}catch(Exception e){AppLog.w("OfflineMasterEngine","Falha ao criar envelope local.",e);return new JSONObject();}}
    private static JSONObject firstObject(JSONObject a,JSONObject b){return a!=null?a:(b!=null?b:new JSONObject());}
    private static void copy(JSONObject from,JSONObject to,String... keys)throws Exception{for(String key:keys)if(from.has(key))to.put(key,from.opt(key));}
    private static String resultMarker(String action){String n=clean(action,900).toLowerCase(new Locale("pt","BR"));if(!n.contains("mecanica_local_verificada"))return "";if(n.contains("resultado=sucesso")||n.contains("resultado: sucesso"))return "sucesso";if(n.contains("resultado=falha")||n.contains("resultado: falha"))return "falha";return "resultado registrado";}
    public static String generateLocalImmersiveFallback(JSONObject hero, String currentPeriod, String weather, String location) {
        String name = hero == null ? "Aventureiro" : hero.optString("name", "Aventureiro");
        String armor = hero == null ? "" : hero.optString("armor", hero.optString("armor_name", ""));
        if (armor.isEmpty()) armor = "suas roupas reforçadas de viagem";
        String weapons = hero == null ? "" : hero.optString("weapons", hero.optString("weapon", ""));
        if (weapons.isEmpty()) weapons = "suas armas de prontidão";

        boolean morning = currentPeriod != null && (currentPeriod.toLowerCase(Locale.ROOT).contains("manhã") || currentPeriod.toLowerCase(Locale.ROOT).contains("morning") || currentPeriod.toLowerCase(Locale.ROOT).contains("alvorada"));
        String timeSense = morning 
            ? "A luz pálida da manhã corta a bruma que rasteja pelo solo"
            : "O crepúsculo avermelhado estende sombras compridas sobre o relevo";

        String place = (location == null || location.trim().isEmpty() || "esta região".equalsIgnoreCase(location.trim())) ? "" : (" em " + location.trim());
        String weatherStr = (weather == null || weather.trim().isEmpty()) ? "O ar traz consigo o cheiro denso de terra úmida e vegetação selvagem." : ("O clima se faz sentir: " + weather.trim().toLowerCase(new Locale("pt", "BR")) + ". O ar traz consigo o cheiro denso de terra úmida e vento frio.");

        return timeSense + place + ". " + weatherStr + "\n\n" +
                "Vocês mantêm a postura firme no terreno irregular. " + name + ", vestindo " + armor + ", " +
                "você sente o peso familiar de " + weapons + " ao alcance das mãos, enquanto o vento frio faz tremular " +
                "a barra de sua capa e o vapor da sua respiração se dissipa no ar. Ao redor, a terra segue seu próprio ritmo rústico.\n\n" +
                "Diante de vocês, trilhas antigas e contornos recortados no horizonte exigem uma decisão de avanço. Como vocês pretendem seguir?";
    }

    private static String lowerFirst(String value){if(value==null||value.length()==0)return "";return Character.toLowerCase(value.charAt(0))+value.substring(1);}
    private static String clean(String value,int max){String out=value==null?"":value.replace('\n',' ').replace('\r',' ').replaceAll("\\s+"," ").trim();if(out.length()<=max)return out;return out.substring(0,Math.max(1,max-1)).trim()+"…";}
}
