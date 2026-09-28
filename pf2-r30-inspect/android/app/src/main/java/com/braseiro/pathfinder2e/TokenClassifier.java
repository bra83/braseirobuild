package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Classifica tokens individuais pelo nome do arquivo. O manifesto prevalece,
 * mas esta classe permite descobrir e organizar arquivos adicionados depois. */
public final class TokenClassifier {
    private TokenClassifier() {}

    public static JSONObject classify(String filename) {
        JSONObject out = new JSONObject();
        try {
            String display = cleanDisplayName(filename);
            String key = normalize(display);
            String species = firstMatch(key, speciesRules());
            String creatureType = firstMatch(key, creatureRules());
            String role = firstMatch(key, roleRules());
            if (species.length()==0 && creatureType.length()==0) creatureType="Não identificado";
            if (species.length()>0 && creatureType.length()==0) creatureType="Humanoide";
            if (role.length()==0) role=inferGenericRole(key);
            JSONArray tags=new JSONArray();
            for(String word:key.split(" ")) if(word.length()>2 && tags.length()<16) tags.put(word);
            out.put("label",display);
            out.put("species",species.length()==0?"Não identificada":species);
            out.put("creatureType",creatureType.length()==0?"Humanoide":creatureType);
            out.put("role",role.length()==0?"Geral":role);
            out.put("category",species.length()>0?"Personagem":"Criatura");
            out.put("tags",tags);
            out.put("confidence",species.length()>0||!"Não identificado".equals(creatureType)?"filename-rule":"fallback");
        } catch(Exception ignored){AppLog.ignored("TokenClassifier","Falha tolerada; a execução continuou.",ignored);}
        return out;
    }


    /** Classifica PDFs de folhas de tokens mantidos numa única pasta. Termos de
     * impressão como 1inch, A4, US Letter e 300dpi não viram categorias. */
    public static JSONObject classifyPdf(String filename) {
        JSONObject out=new JSONObject();
        try{
            String raw=filename==null?"":filename;
            String noExt=raw.replaceFirst("(?i)\\.pdf$","");
            String key=normalize(noExt.replace('_',' '));
            int setNumber=extractSetNumber(key);
            String species=firstMatch(key,speciesRules());
            String role=firstMatch(key,roleRules());
            String category;
            String className=role;
            if(role.length()>0)category="Personagens";
            else if(species.length()>0)category="Espécies";
            else {String creature=firstMatch(key,creatureRules());if(creature.length()>0){category="Criaturas";className=creature;}else{category="Não classificados";className="";}}
            String base=removeTechnicalTerms(key);
            String groupBase=role.length()>0?normalize(role):(species.length()>0?normalize(species):(className.length()>0?normalize(className):firstUsefulTerm(base)));
            if(groupBase.length()==0)groupBase="nao-classificados";
            String label=role.length()>0?pluralPt(role):(species.length()>0?pluralPt(species):(className.length()>0?className:titleCase(firstUsefulTerm(base))));
            if("Não classificados".equals(category))label="Não classificados — "+titleCase(base.length()>0?base:noExt);
            JSONArray tags=new JSONArray();for(String word:base.split(" "))if(word.length()>2&&tags.length()<20)tags.put(word);
            out.put("label",label);out.put("groupKey",slug(groupBase));out.put("category",category);out.put("className",className);out.put("species",species);out.put("setNumber",setNumber);out.put("tags",tags);out.put("sourceName",raw);out.put("confidence","Não classificados".equals(category)?"manual-required":"filename-rule");
        }catch(Exception ignored){AppLog.ignored("TokenClassifier","Falha tolerada; a execução continuou.",ignored);}
        return out;
    }

    private static String removeTechnicalTerms(String key){
        String s=" "+key+" ";String[] ignored={"1inch","1 inch","one inch","universal","a4","a 4","usletter","us letter","letter","300dpi","300 dpi","600dpi","600 dpi","printable","print","tokens","token","sheet","sheets","page","pages","pdf","portrait","landscape","color","colour","set","pack","volume","vol","part","parte"};
        for(String x:ignored)s=s.replace(" "+x+" "," ");
        s=s.replaceAll("\\b(19|20)\\d{2}\\b"," ").replaceAll("\\b\\d+\\b"," ").replaceAll("\\s+"," ").trim();return s;
    }
    private static int extractSetNumber(String key){java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:^| )(?:set|pack|vol(?:ume)?|part|parte)?[ _-]*0*(\\d{1,3})(?: |$)").matcher(key);int last=0;while(m.find()){try{int n=Integer.parseInt(m.group(1));if(n!=1&&n!=4&&n!=300&&n!=600)last=n;}catch(Exception ignored){AppLog.ignored("TokenClassifier","Falha tolerada; a execução continuou.",ignored);}}return last;}
    private static String firstUsefulTerm(String base){if(base==null)return "";String[] w=base.trim().split(" ");StringBuilder b=new StringBuilder();for(String x:w){if(x.length()<2)continue;if(b.length()>0)b.append(' ');b.append(x);if(b.length()>42)break;}return b.toString();}
    private static String slug(String value){return normalize(value).replace(' ','-').replaceAll("-+","-");}
    private static String titleCase(String value){StringBuilder b=new StringBuilder();for(String w:value.split(" ")){if(w.length()==0)continue;if(b.length()>0)b.append(' ');b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));}return b.toString();}
    private static String pluralPt(String value){if("Bardo".equals(value))return "Bardos";if("Bárbaro".equals(value))return "Bárbaros";if("Campeão".equals(value))return "Campeões";if("Clérigo".equals(value))return "Clérigos";if("Druida".equals(value))return "Druidas";if("Guerreiro".equals(value))return "Guerreiros";if("Inventor".equals(value))return "Inventores";if("Investigador".equals(value))return "Investigadores";if("Monge".equals(value))return "Monges";if("Patrulheiro".equals(value))return "Patrulheiros";if("Ladino".equals(value))return "Ladinos";if("Feiticeiro".equals(value))return "Feiticeiros";if("Bruxa".equals(value))return "Bruxas";if("Mago".equals(value))return "Magos";if("Alquimista".equals(value))return "Alquimistas";if("Anão".equals(value))return "Anões";if("Elfo".equals(value))return "Elfos";return value;}

    public static String cleanDisplayName(String filename) {
        String s=filename==null?"":filename.trim();
        s=s.replaceFirst("(?i)^c[oó]pia de\\s+","").replaceFirst("(?i)^copy of\\s+","");
        s=s.replaceFirst("(?i)\\.(png|jpe?g|webp)$","");
        s=s.replace('_',' ').replaceAll("\\s+"," ").trim();
        return s;
    }

    private static String normalize(String input){
        String s=Normalizer.normalize(input==null?"":input,Normalizer.Form.NFD).replaceAll("\\p{M}","");
        return s.toLowerCase(Locale.US).replace('-', ' ').replaceAll("[^a-z0-9' ]"," ").replaceAll("\\s+"," ").trim();
    }
    private static String firstMatch(String text, LinkedHashMap<String,String[]> rules){
        for(Map.Entry<String,String[]> e:rules.entrySet()) for(String k:e.getValue()) if(containsPhrase(text,k)) return e.getKey();
        return "";
    }
    private static boolean containsPhrase(String text,String phrase){return (" "+text+" ").contains(" "+phrase+" ")||text.contains(phrase);}

    private static LinkedHashMap<String,String[]> speciesRules(){
        LinkedHashMap<String,String[]> m=new LinkedHashMap<String,String[]>();
        m.put("Elfo cavernícola (visual legado)",new String[]{"drow","dark elf","dusk elf"});
        m.put("Elfo",new String[]{"elf","elven","elfo","elfa"});m.put("Anão",new String[]{"dwarf","dward","dwerym","anao"});m.put("Humano",new String[]{"human","humano","humana","homem","mulher"});
        m.put("Halfling",new String[]{"halfling","halfing"});m.put("Gnomo",new String[]{"gnome","gnomo","gnoma"});m.put("Sangue dracônico (visual legado)",new String[]{"dragonborn","draconato"});m.put("Nephilim (visual legado)",new String[]{"tiefling","aasimar"});
        m.put("Orc",new String[]{"orc"});m.put("Goblin",new String[]{"goblin"});m.put("Kobold",new String[]{"kobold"});m.put("Tengu",new String[]{"tengu"});m.put("Leshy",new String[]{"leshy"});m.put("Kholo",new String[]{"kholo","gnoll"});
        return m;
    }
    private static LinkedHashMap<String,String[]> creatureRules(){
        LinkedHashMap<String,String[]> m=new LinkedHashMap<String,String[]>();
        m.put("Aberração",new String[]{"aberration","aberrant","aboleth","tentacled horror"});m.put("Elemental",new String[]{"elemental"});m.put("Dragão",new String[]{"dragon","drake"});
        m.put("Morto-vivo",new String[]{"undead","zombie","skeleton","lich","vampire","wraith","ghost","banshee"});m.put("Corruptor",new String[]{"fiend","demon","devil"});m.put("Celestial",new String[]{"celestial","angel"});
        m.put("Constructo",new String[]{"construct","golem"});m.put("Planta",new String[]{"plant","blight","shrieker"});m.put("Limo",new String[]{"ooze","slime","gelatinous"});m.put("Fera",new String[]{"wolf","bear","fox","elk","stag","hound","hawk","panther","beast"});m.put("Monstruosidade",new String[]{"griffin","werewolf","nightmare","monster"});
        return m;
    }
    private static LinkedHashMap<String,String[]> roleRules(){
        LinkedHashMap<String,String[]> m=new LinkedHashMap<String,String[]>();
        m.put("Alquimista",new String[]{"alchemist","potion maker","alquimista"});m.put("Bárbaro",new String[]{"barbarian","barbaro","barbara"});m.put("Bardo",new String[]{"bard","bardo","barda"});m.put("Bruxa",new String[]{"witch","warlock","bruxa","bruxo"});m.put("Campeão",new String[]{"champion","paladin","paladino","paladina"});
        m.put("Clérigo",new String[]{"cleric","priest","clerigo","sacerdote","sacerdotisa"});m.put("Druida",new String[]{"druid","druida"});m.put("Feiticeiro",new String[]{"sorcerer","feiticeiro","feiticeira"});m.put("Guerreiro",new String[]{"fighter","warrior","duelist","guerreiro","guerreira","soldado"});
        m.put("Inventor",new String[]{"inventor","artificer","armorsmith","ferreiro","ferreira"});m.put("Investigador",new String[]{"investigator","detective","investigador"});m.put("Ladino",new String[]{"rogue","thief","assassin","ladino","ladina","ladrao"});m.put("Mago",new String[]{"wizard","mage","mago","maga","necromante"});m.put("Magus",new String[]{"magus","spellblade"});m.put("Monge",new String[]{"monk","monge"});
        m.put("Oráculo",new String[]{"oracle","oraculo"});m.put("Patrulheiro",new String[]{"ranger","archer","scout","tracker","patrulheiro","arqueiro","cacador"});m.put("Pistoleiro",new String[]{"gunslinger","pistoleiro"});m.put("Psíquico",new String[]{"psychic","psiquico"});m.put("Taumaturgo",new String[]{"thaumaturge","taumaturgo"});m.put("Invocador",new String[]{"summoner","invocador"});
        m.put("Líder",new String[]{"leader","king","queen","ambassador","noble","lider","rei","rainha","nobre"});m.put("Mercador",new String[]{"merchant","negotiator","mercador","mercadora","comerciante"});m.put("Sentinela",new String[]{"sentinel","guard","guarda"});m.put("Explorador",new String[]{"explorer","scout","rider","explorador","viajante"});m.put("Cultista",new String[]{"cultist","herald","cultista"});
        return m;
    }
    private static String inferGenericRole(String key){
        if(key.contains("combat")||key.contains("fighting")||key.contains("armor")||key.contains("axe")||key.contains("sword")||key.contains("mace"))return "Combatente";
        if(key.contains("elder"))return "Ancião";if(key.contains("mother"))return "Civil";return "Geral";
    }
}
