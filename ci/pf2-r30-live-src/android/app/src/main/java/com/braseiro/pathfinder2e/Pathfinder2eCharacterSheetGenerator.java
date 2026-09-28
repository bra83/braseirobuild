package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Gerador local PF2e compatível com o formulário legado do Braseiro.
 * Mantém as chaves antigas do JSON para não quebrar saves/UI, mas a autoridade
 * mecânica é PF2e Remaster e atributos são modificadores, não scores 5e.
 */
public final class Pathfinder2eCharacterSheetGenerator {
    private final Random random=new Random();
    private final Pathfinder2eRulesEngine rules=new Pathfinder2eRulesEngine();

    private static final String[] CLASSES={"Alquimista","Bárbaro","Bardo","Bruxa","Campeão","Clérigo","Druida","Guerreiro","Inventor","Investigador","Cinético","Mago","Magus","Monge","Oráculo","Patrulheiro","Pistoleiro","Psíquico","Ladino","Feiticeiro","Invocador","Espadachim","Taumaturgo"};
    private static final String[] ANCESTRIES={"Humano","Anão","Elfo","Gnomo","Goblin","Halfling","Orc","Leshy","Kobold","Tengu","Catfolk","Ratfolk"};
    private static final String[] BACKGROUNDS={"Acólito","Artista","Artesão","Batedor","Caçador","Erudito","Fazendeiro","Guarda","Herbalista","Marinheiro","Mercador","Nobre","Órfão","Soldado"};
    private static final String[] ALIGNMENTS={"Sem alinhamento obrigatório"};
    private static final String[] ORIGINS={"Aldeia rural","Bairro urbano","Cidade mercantil","Fronteira selvagem","Templo","Corte nobre","Comunidade itinerante","Porto","Fortaleza","Bosque ancestral"};
    private static final String[] CAREERS={"Milícia e guerra","Erudição e magia","Templo e peregrinação","Crime e submundo","Comércio e ofício","Mar e estradas","Caça e exploração","Corte e diplomacia","Engenharia","Curandeirismo"};

    public String rollName(String species){return randomName(species);}
    public String rollNickname(String cls){return pick(new String[]{"Passo Leve","Olho Vivo","Mão Firme","Voz de Bronze","Poeira Vermelha","Lâmina Serena"});}
    public String rollTitle(String cls){return pick(new String[]{"da Estrada","do Vale","de Sete Pontes","da Vigília","das Cinzas","do Bosque"});}

    public String randomField(String key,JSONObject current){
        JSONObject c=current==null?new JSONObject():current; String k=key==null?"":key.trim();
        try{
            if("name".equals(k))return randomName(c.optString("species","Humano"));
            if("nickname".equals(k))return rollNickname(c.optString("class_name"));
            if("title".equals(k))return rollTitle(c.optString("class_name"));
            if("level".equals(k))return String.valueOf(1+random.nextInt(20));
            if(isAbility(k))return String.valueOf(-1+random.nextInt(6));
            if("subclass".equals(k))return ""; // PF2e não força uma subclasse genérica como 5e.
            JSONObject g=ensureComplete(c);
            if("hp".equals(k))k="max_hp";
            Object v=g.opt(k); return v==null?"":String.valueOf(v);
        }catch(Exception e){return "";}
    }

    public String oracleRuleField(String key,JSONObject current,String wordA,String wordB,int rollA,int rollB){
        long seed=oracleSeed(key,wordA,wordB,rollA,rollB); Random r=new Random(seed);
        if(isAbility(key))return String.valueOf(-1+r.nextInt(6));
        if("level".equals(key))return String.valueOf(1+r.nextInt(20));
        return randomField(key,current);
    }

    public int oracleChoiceIndex(int optionCount,String field,String wordA,String wordB,int rollA,int rollB){return optionCount<=1?0:new Random(oracleSeed(field,wordA,wordB,rollA,rollB)).nextInt(optionCount);}

    public JSONObject lifeDetailsFromPath(String lifePath){
        JSONObject out=new JSONObject(); if(lifePath==null)return out;
        try{for(String raw:lifePath.split("\\n")){String line=raw.trim(); putLife(out,line,"Contato:","contact");putLife(out,line,"Rival:","rival");putLife(out,line,"Dívida ou compromisso:","debt");putLife(out,line,"Motivação:","motivation");putLife(out,line,"Entrada na classe:","class_entry");}}catch(Exception ignored){}
        return out;
    }

    public JSONObject randomComplete(String profile,String[] speciesOptions,List<JSONObject> existing){
        String species=(speciesOptions!=null&&speciesOptions.length>0)?speciesOptions[random.nextInt(speciesOptions.length)]:pick(ANCESTRIES);
        return baseComplete(Pathfinder2eRulesEngine.PROFILE,species,pick(CLASSES),pick(BACKGROUNDS),1);
    }

    public JSONObject baseComplete(String profile,String species,String cls,String background,int level){
        JSONObject o=new JSONObject();
        try{
            int[] mods={4,3,2,1,0,-1}; List<Integer> bag=new ArrayList<Integer>();for(int x:mods)bag.add(x);Collections.shuffle(bag,random);
            o.put("rules_source",Pathfinder2eRulesEngine.PROFILE).put("system",Pathfinder2eSystemModule.ID);
            o.put("name",randomName(species)).put("nickname",rollNickname(cls)).put("title",rollTitle(cls));
            o.put("species",blank(species)?"Humano":species).put("class_name",blank(cls)?"Guerreiro":cls).put("background",blank(background)?"Soldado":background).put("alignment",ALIGNMENTS[0]);
            o.put("level",Math.max(1,Math.min(20,level))).put("str",bag.get(0)).put("dex",bag.get(1)).put("con",bag.get(2)).put("int_score",bag.get(3)).put("wis",bag.get(4)).put("cha",bag.get(5));
            JSONObject am=new JSONObject();am.put("str",bag.get(0)).put("dex",bag.get(1)).put("con",bag.get(2)).put("int_score",bag.get(3)).put("wis",bag.get(4)).put("cha",bag.get(5));
            o.put("attribute_modifiers",am).put("attribute_model","pf2_remaster_modifiers");
            int hp=Math.max(1,8+8+bag.get(2)); o.put("hp",hp).put("max_hp",hp).put("ac",10+bag.get(1));
            o.put("speed","7,5 m").put("initiative","Percepção").put("proficiency_bonus","por nível + graduação");
            o.put("saving_throws","Fortitude, Reflexos e Vontade — confirmar graduações").put("skills","confirmar graduações PF2e").put("passive_senses","Percepção").put("languages","Comum + idiomas da ancestralidade/antecedente");
            o.put("proficiencies","Confirmar proficiências de classe e ancestralidade").put("tools","Não usar proficiência de ferramentas 5e").put("carrying_capacity","Bulk");
            o.put("attacks","Confirmar armas, bônus, dano e MAP").put("features","Talentos e características PF2e").put("spellcasting","Confirmar tradição, CD e ataque mágico quando aplicável").put("spells","Confirmar pelo módulo PF2e").put("inventory","");
            o.put("concept","Aventureiro de Golarion com vínculos e objetivos próprios.").put("role",cls).put("appearance","").put("visual_description","");
            o.put("personality","").put("ideal","").put("bond","").put("fear","").put("flaw","").put("contact","").put("rival","").put("debt","").put("motivation","").put("class_entry","").put("history","").put("mannerisms","").put("quotes","").put("level_up","").put("extra_notes","");
            o.put("life_origin",pick(ORIGINS)).put("life_career",pick(CAREERS)).put("life_terms",2);
            o.put("life_path",generateLifePath(o.optString("life_origin"),o.optString("life_career"),2,species,cls,background));
            o.put("mechanics_pending_verification",true).put("mechanical_authority","pf2e_remaster");
        }catch(Exception ignored){}
        return o;
    }

    public JSONObject fromLegacy(JSONObject character){
        JSONObject o;
        try{o=new JSONObject(character==null?"{}":character.toString());}catch(Exception e){o=new JSONObject();}
        return ensureComplete(o);
    }

    public JSONObject mergeNarrativeAi(JSONObject base,JSONObject ai){
        JSONObject out;try{out=new JSONObject(base==null?"{}":base.toString());}catch(Exception e){out=new JSONObject();}
        if(ai==null)return out;
        String[] narrative={"name","nickname","title","concept","role","appearance","personality","ideal","bond","fear","flaw","contact","rival","debt","motivation","class_entry","history","mannerisms","quotes","level_up","extra_notes"};
        try{for(String k:narrative)if(ai.has(k)&&!blank(ai.optString(k)))out.put(k,sanitizeSpeciesCoherence(ai.optString(k),out.optString("species")));out.put("visual_description",out.optString("appearance"));}catch(Exception ignored){}
        return out;
    }

    public JSONObject ensureComplete(JSONObject input){
        JSONObject in;try{in=new JSONObject(input==null?"{}":input.toString());}catch(Exception e){in=new JSONObject();}
        JSONObject out=baseComplete(Pathfinder2eRulesEngine.PROFILE,in.optString("species","Humano"),in.optString("class_name","Guerreiro"),in.optString("background","Soldado"),in.optInt("level",1));
        try{
            java.util.Iterator<String> it=in.keys(); while(it.hasNext()){String k=it.next();out.put(k,in.opt(k));}
            JSONObject mods=new JSONObject(); for(String a:new String[]{"str","dex","con","int_score","wis","cha"}){int raw=out.optInt(a,0);int mod=(raw>=-5&&raw<=10)?raw:Math.floorDiv(raw-10,2);out.put(a,mod);mods.put(a,mod);}out.put("attribute_modifiers",mods).put("attribute_model","pf2_remaster_modifiers").put("rules_source",Pathfinder2eRulesEngine.PROFILE).put("system",Pathfinder2eSystemModule.ID);
        }catch(Exception ignored){}
        return out;
    }

    public String generateLifePath(String origin,String career,int terms,String species,String cls,String background){
        return "Origem: "+safe(origin,"Não definida")+"\nCarreira anterior: "+safe(career,"Não definida")+"\nContato: alguém ligado a "+safe(background,"seu passado")+".\nRival: uma pessoa cujos objetivos entram em choque com os seus.\nDívida ou compromisso: uma promessa ainda não cumprida.\nMotivação: descobrir o que existe além do próximo horizonte.\nEntrada na classe: uma experiência decisiva levou ao caminho de "+safe(cls,"aventureiro")+".";
    }

    public String sanitizeSpeciesCoherence(String text,String species){return text==null?"":text.trim();}

    private String randomName(String species){String s=norm(species);if(s.contains("anao"))return pick(new String[]{"Dori","Borin","Kelda","Ragna"});if(s.contains("elf"))return pick(new String[]{"Aelir","Lethiel","Saria","Theren"});if(s.contains("gob"))return pick(new String[]{"Rikka","Zib","Mog","Tikka"});return pick(new String[]{"Ari","Caio","Dara","Ivo","Lina","Mara","Nilo","Téo"});}
    private String pick(String[] a){return a[random.nextInt(a.length)];}
    private static boolean isAbility(String k){return "str".equals(k)||"dex".equals(k)||"con".equals(k)||"int_score".equals(k)||"wis".equals(k)||"cha".equals(k);}
    private static boolean blank(String s){return s==null||s.trim().isEmpty();}
    private static String safe(String s,String fallback){return blank(s)?fallback:s.trim();}
    private static String norm(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}
    private static long oracleSeed(String a,String b,String c,int d,int e){String m=String.valueOf(a)+"|"+String.valueOf(b)+"|"+String.valueOf(c)+"|"+d+"|"+e;long x=1125899906842597L;for(int i=0;i<m.length();i++)x=31*x+m.charAt(i);return x;}
    private static void putLife(JSONObject o,String line,String prefix,String key){try{if(line.startsWith(prefix))o.put(key,line.substring(prefix.length()).trim());}catch(Exception ignored){}}
}
