package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Constrói uma versão narrativa do Diário sem alterar fatos da campanha.
 * O modo local é inteiramente determinístico e preserva todas as entradas.
 * Quando Gemini é usado, esta classe também produz o contrato/prompt e saneia o manuscrito devolvido.
 */
public final class BardChronicleComposer {
    private BardChronicleComposer(){}

    public static JSONObject localManuscript(JSONObject campaign,List<JSONObject> journal,List<JSONObject> heroes,String bardName,String tone){/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();
        try{
            String campaignName=human(campaign==null?"":campaign.optString("name"),"Crônicas do Braseiro");
            String bard=human(bardName,"Bardo do Braseiro");
            String mood=human(tone,"Épico e caloroso");
            out.put("title","A Balada de "+campaignName);
            out.put("subtitle","Uma crônica de estrada, aço, promessas e escolhas, contada à luz de uma taverna.");
            out.put("bard_name",bard);
            out.put("tone",mood);
            out.put("opening",openingForTone(bard,mood,campaignName));
            out.put("heroes",heroLine(heroes));
            JSONArray chapters=new JSONArray();
            ArrayList<JSONObject> entries=new ArrayList<JSONObject>(journal==null?Collections.<JSONObject>emptyList():journal);
            Collections.reverse(entries); // AppDb entrega os registros mais recentes primeiro.
            StringBuilder chapterText=new StringBuilder();
            int chapterNo=1,entriesInChapter=0;
            String chapterTitle="Onde a canção começa";
            for(int i=0;i<entries.size();i++){
                JSONObject entry=entries.get(i);if(entry==null)continue;
                String body=human(entry.optString("body"),"");if(body.length()==0)continue;
                String title=human(entry.optString("title"),"Um novo acontecimento");
                String kind=human(entry.optString("kind"),"Cena");
                if(entriesInChapter>=5||chapterText.length()>5200){
                    chapters.put(chapter(chapterNo++,chapterTitle,chapterText.toString()));
                    chapterText.setLength(0);entriesInChapter=0;chapterTitle=chapterName(kind,title,chapterNo);
                }
                if(entriesInChapter==0)chapterTitle=chapterName(kind,title,chapterNo);
                if(chapterText.length()>0)chapterText.append("\n\n");
                chapterText.append(bardLead(kind,title,i)).append(' ').append(body);
                entriesInChapter++;
            }
            if(chapterText.length()>0)chapters.put(chapter(chapterNo,chapterTitle,chapterText.toString()));
            if(chapters.length()==0)chapters.put(chapter(1,"Uma canção ainda por começar","Até esta noite, nenhum feito havia sido registrado no diário da companhia."));
            out.put("chapters",chapters);
            out.put("closing",closingForTone(bard,mood));
            out.put("source_policy","Os fatos vieram somente dos registros persistentes do Diário; moldura e transições do bardo são estilísticas.");
        }catch(Exception ignored){AppLog.ignored("BardChronicle","Falha tolerada ao montar crônica local.",ignored);}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static String aiSystem(){
        return "BRAS_SINGLE_CALL. Você é um cronista de fantasia que transforma um diário de campanha de RPG em uma história oral contada por um bardo numa taverna. " +
                "IDIOMA OBRIGATÓRIO: português do Brasil. FATOS SÃO SAGRADOS: use somente personagens, lugares, ações, decisões, resultados, descobertas e consequências presentes na FONTE. " +
                "Não invente combates, tesouros, falas, mortes, vitórias, romances, locais, regras, parentescos ou desfechos. Não mude a ordem causal. " +
                "Você pode acrescentar somente moldura performática genérica do bardo (canecas, lareira, alaúde, reação indistinta da plateia) e transições que não sejam fatos da campanha. " +
                "Condense repetições administrativas, mas preserve todos os acontecimentos relevantes e nomes próprios. Se a fonte for ambígua, mantenha a ambiguidade. " +
                "Retorne SOMENTE JSON válido, sem markdown, nesta estrutura exata: {\"title\":\"\",\"subtitle\":\"\",\"bard_name\":\"\",\"opening\":\"\",\"chapters\":[{\"title\":\"\",\"text\":\"\"}],\"closing\":\"\"}.";
    }

    public static String aiPrompt(JSONObject campaign,List<JSONObject> journal,List<JSONObject> heroes,String bardName,String tone){/*BR_JSON_WRAP*/try{
        JSONObject src=new JSONObject();
        try{
            src.put("campaign",new JSONObject().put("name",campaign==null?"":campaign.optString("name")).put("tone",campaign==null?"":campaign.optString("tone")).put("rules_profile",campaign==null?"":campaign.optString("rules_profile")));
            src.put("bard_name",human(bardName,"Bardo do Braseiro"));src.put("desired_tone",human(tone,"Épico e caloroso"));
            JSONArray hs=new JSONArray();if(heroes!=null)for(JSONObject hero:heroes){if(hero==null)continue;hs.put(new JSONObject().put("name",human(hero.optString("name"),"Herói")).put("species",human(hero.optString("species"),"")).put("class_name",human(hero.optString("class_name"),"")));}src.put("heroes",hs);
            JSONArray entries=new JSONArray();ArrayList<JSONObject> ordered=new ArrayList<JSONObject>(journal==null?Collections.<JSONObject>emptyList():journal);Collections.reverse(ordered);
            for(JSONObject e:ordered){if(e==null)continue;String body=human(e.optString("body"),"");if(body.length()==0)continue;entries.put(new JSONObject().put("title",human(e.optString("title"),"Cena")).put("kind",human(e.optString("kind"),"Cena")).put("body",body).put("created_at",e.optLong("created_at")));}
            src.put("journal",entries);
        }catch(Exception ignored){AppLog.ignored("BardChronicle","Falha tolerada ao preparar fonte para IA.",ignored);}
        return "Transforme a FONTE abaixo numa crônica contínua, agradável de ler em PDF e claramente narrada por um bardo diante de uma taverna. " +
                "Crie de 2 a 8 capítulos conforme o volume de acontecimentos. Não explique regras nem mencione banco de dados, aplicativo, JSON, logs, prompts ou 'diário'. " +
                "Não trate uma sugestão ou intenção como fato consumado. Preserve nomes e consequências exatamente como registrados.\nFONTE:\n"+src.toString();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static JSONObject sanitizeAiManuscript(JSONObject candidate,JSONObject fallback){/*BR_JSON_WRAP*/try{
        if(candidate==null)candidate=new JSONObject();if(fallback==null)fallback=new JSONObject();JSONObject out=new JSONObject();
        try{
            out.put("title",human(candidate.optString("title"),fallback.optString("title","Crônica do Braseiro")));
            out.put("subtitle",human(candidate.optString("subtitle"),fallback.optString("subtitle","Uma história contada numa taverna.")));
            out.put("bard_name",human(candidate.optString("bard_name"),fallback.optString("bard_name","Bardo do Braseiro")));
            out.put("tone",fallback.optString("tone",""));
            out.put("opening",human(candidate.optString("opening"),fallback.optString("opening","")));
            JSONArray clean=new JSONArray();JSONArray chapters=candidate.optJSONArray("chapters");
            if(chapters!=null)for(int i=0;i<chapters.length();i++){JSONObject c=chapters.optJSONObject(i);if(c==null)continue;String text=human(c.optString("text"),"");if(text.length()==0)continue;clean.put(new JSONObject().put("number",clean.length()+1).put("title",human(c.optString("title"),"Capítulo "+(clean.length()+1))).put("text",text));}
            if(clean.length()==0){JSONArray local=fallback.optJSONArray("chapters");if(local!=null)for(int i=0;i<local.length();i++)clean.put(local.opt(i));}
            out.put("chapters",clean);
            out.put("closing",human(candidate.optString("closing"),fallback.optString("closing","")));
            out.put("heroes",fallback.optString("heroes",""));
            out.put("source_policy",fallback.optString("source_policy",""));
        }catch(Exception ignored){AppLog.ignored("BardChronicle","Falha tolerada ao sanear manuscrito da IA.",ignored);return fallback;}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public static boolean aiSourceFits(String prompt){return prompt!=null&&prompt.length()<=62000;}

    private static JSONObject chapter(int number,String title,String text)throws Exception{/*BR_JSON_WRAP*/try{return new JSONObject().put("number",number).put("title",human(title,"Capítulo "+number)).put("text",human(text,""));}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String bardLead(String kind,String title,int index){
        String k=norm(kind);String[] generic={"Pois bem, escutem o que veio depois em","E a canção seguiu até","Foi então que as mesas silenciaram ao ouvir sobre","Guardem este nome, pois a história alcançou"};String lead=generic[Math.abs(index)%generic.length];
        if(k.contains("combat")||k.contains("encontro"))lead="Então o aço cantou quando chegou o momento de";
        else if(k.contains("npc"))lead="Foi nessa altura que outro nome entrou na balada:";
        else if(k.contains("local"))lead="A estrada levou a companhia até";
        else if(k.contains("miss")||k.contains("quest"))lead="Um novo chamado ganhou voz em";
        else if(k.contains("evento"))lead="E o mundo, que jamais fica parado, respondeu em";
        return lead+" “"+title+"”.";
    }
    private static String chapterName(String kind,String title,int number){String k=norm(kind);if(k.contains("combat"))return "Quando o aço respondeu";if(k.contains("npc"))return "Nomes que mudaram a estrada";if(k.contains("local"))return "Pelos caminhos do mundo";if(k.contains("miss"))return "Promessas e chamados";if(number==1)return "Onde a canção começa";return human(title,"Novos versos da jornada");}
    private static String openingForTone(String bard,String tone,String campaign){String t=norm(tone);if(t.contains("sombr"))return "A lareira ardia baixa quando "+bard+" pousou a caneca e pediu silêncio. 'Nem toda canção termina em triunfo', advertiu, antes de começar a história de "+campaign+".";if(t.contains("bem-hum")||t.contains("humor"))return bard+" afinou o instrumento, reclamou do preço da cerveja e sorriu para a sala. 'Agora, meus amigos, deixem as moedas para depois e ouçam a história de "+campaign+".'";if(t.contains("sobri"))return bard+" ergueu os olhos para a taverna e começou sem floreios desnecessários: 'Ouçam e lembrem. Esta é a história de "+campaign+".'";return "Sob a luz quente da lareira, "+bard+" dedilhou as primeiras notas. As conversas baixaram de tom. 'Cheguem mais perto', disse. 'Esta é a balada de "+campaign+".'";}
    private static String closingForTone(String bard,String tone){String t=norm(tone);if(t.contains("sombr"))return bard+" deixou a última nota morrer entre as sombras da taverna. A história terminava ali apenas porque a noite precisava terminar.";if(t.contains("bem-hum")||t.contains("humor"))return bard+" esvaziou a caneca e abriu um sorriso. 'O resto da canção? Ora, para isso teremos de esperar que esses aventureiros façam mais confusão.'";return bard+" abafou as cordas com a palma da mão. 'E foi assim que estes versos chegaram até nós. Enquanto houver estrada adiante, a canção ainda não acabou.'";}
    private static String heroLine(List<JSONObject> heroes){StringBuilder b=new StringBuilder();if(heroes!=null)for(JSONObject h:heroes){if(h==null)continue;String n=human(h.optString("name"),"");if(n.length()==0)continue;if(b.length()>0)b.append(", ");b.append(n);String s=human(h.optString("species"),"");if(s.length()>0)b.append(" (").append(s).append(')');}return b.toString();}
    private static String human(String value,String fallback){String s=GeneratedTextSanitizer.humanText(value==null?"":value).trim();if(s.length()==0)s=fallback==null?"":fallback;return s;}
    private static String norm(String value){String n=java.text.Normalizer.normalize(value==null?"":value,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);return n;}
}
