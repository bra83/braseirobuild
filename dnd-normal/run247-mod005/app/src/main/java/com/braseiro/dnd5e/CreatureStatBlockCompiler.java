package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Separa blocos de criaturas antes que qualquer estatística seja executada.
 *
 * O limite do bloco faz parte da prova: CA, PV e ataques só podem ser lidos do
 * intervalo pertencente ao mesmo cabeçalho. Quando o texto extraído não
 * preserva cabeçalhos/limites suficientes, nenhum bloco executável é criado.
 */
public final class CreatureStatBlockCompiler {
    private static final Pattern AC=Pattern.compile("(?iu)\\b(?:CA|AC|classe de armadura|armor class)\\s*[:=]?\\s*(\\d{1,2})\\b");
    private static final Pattern HP=Pattern.compile("(?iu)\\b(?:PV|HP|pontos de vida|hit points)\\s*[:=]?\\s*(\\d{1,4})\\b");
    private static final Pattern SPEED=Pattern.compile("(?iu)\\b(?:deslocamento|movimento|speed)\\s*[:=]?\\s*([^\\n]{1,140})");
    private static final Pattern ATTACK=Pattern.compile("(?iu)\\b(?:ataque|attack|mordida|bite|garra|claw|espada|sword|arco|bow)\\b");
    private static final Pattern DICE=Pattern.compile("(?iu)\\b(?:\\d{1,3})?d\\d{1,4}(?:\\s*[+\\-]\\s*\\d{1,3})?\\b");
    private static final Pattern SAVE_DC=Pattern.compile("(?iu)\\b(?:CD|DC|save dc)\\s*[:=]?\\s*(\\d{1,2})\\b");

    private CreatureStatBlockCompiler(){}

    public static JSONArray compile(JSONObject source)throws Exception{/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();
        String text=source==null?"":source.optString("excerpt","");
        if(text.trim().length()<20)return out;
        List<Line> lines=lines(text);List<Integer> headers=new ArrayList<Integer>();
        for(int i=0;i<lines.size();i++)if(isHeader(lines,i,text))headers.add(i);
        for(int i=0;i<headers.size();i++){
            Line header=lines.get(headers.get(i));
            int start=header.start,end=i+1<headers.size()?lines.get(headers.get(i+1)).start:text.length();
            if(end<=start)continue;
            String block=text.substring(start,end).trim();Matcher ac=AC.matcher(block),hp=HP.matcher(block);
            boolean hasAc=ac.find(),hasHp=hp.find();if(!hasAc||!hasHp)continue;
            JSONObject creature=new JSONObject();String name=header.value.trim();
            creature.put("block_id",source.optString("rule_id","")+":creature:"+(out.length()+1));
            creature.put("name",name).put("system",source.optString("system",inferSystem(source))).put("edition",source.optString("edition",source.optString("rule_profile","")));
            creature.put("book",source.optString("book",source.optString("title",""))).put("page_start",source.optInt("page",0)).put("page_end",source.optInt("page_end",source.optInt("page",0)));
            creature.put("block_start",start).put("block_end",end).put("scope_text",block);
            creature.put("armor_class",Integer.parseInt(ac.group(1))).put("hit_points",Integer.parseInt(hp.group(1)));
            creature.put("speed",capture(SPEED,block,1));
            creature.put("attributes",extractAttributes(block));
            creature.put("saving_throws",labeledText(block,"salvaguardas","saving throws"));
            creature.put("skills",labeledText(block,"perícias","pericias","skills"));
            creature.put("resistances",labeledText(block,"resistências a dano","resistencias a dano","damage resistances"));
            creature.put("immunities",labeledText(block,"imunidades a dano","imunidades a condição","imunidades a condicao","damage immunities","condition immunities"));
            creature.put("vulnerabilities",labeledText(block,"vulnerabilidades a dano","damage vulnerabilities"));
            creature.put("senses",labeledText(block,"sentidos","senses"));
            creature.put("languages",labeledText(block,"idiomas","languages"));
            creature.put("challenge_or_hit_dice",firstNonEmpty(labeledText(block,"nível de desafio","nivel de desafio","challenge"),labeledText(block,"dados de vida","hit dice")));
            creature.put("morale",labeledInt(block,"moral","morale"));
            creature.put("attacks",attackLines(block));
            creature.put("damage",diceTerms(block)).put("attack_count",attackCount(block));
            creature.put("special_actions",section(block,"ações","acoes","actions"));
            creature.put("reactions",section(block,"reações","reacoes","reactions"));
            creature.put("recharges",containsAny(norm(block),"recarga","recharge")?matchingLines(block,"recarga","recharge"):new JSONArray());
            creature.put("spells",matchingLines(block,"magia","spellcasting","spells"));
            creature.put("conditions_applied",matchingLines(block,"fica ","torna se ","becomes "));
            int confidence=65+(hasAc?12:0)+(hasHp?12:0)+(creature.optJSONArray("attacks").length()>0?8:0);
            creature.put("confidence",Math.min(100,confidence)).put("executable",confidence>=89);
            out.put(creature);
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static boolean isHeader(List<Line> lines,int index,String text){
        Line line=lines.get(index);String value=line.value.trim(),n=norm(value);
        if(value.length()<2||value.length()>80||value.contains(":")||value.endsWith(".")||value.matches(".*\\d.*"))return false;
        if(containsAny(n," ca "," ac "," pv "," hp ","armor class","classe de armadura","hit points","pontos de vida","speed","deslocamento","ataque","attack","actions","acoes","reactions","reacoes","saving throws","salvaguardas"))return false;
        int end=Math.min(text.length(),line.end+900);String look=text.substring(line.end,end);
        return AC.matcher(look).find()&&HP.matcher(look).find();
    }

    private static List<Line> lines(String text){
        ArrayList<Line> out=new ArrayList<Line>();Matcher m=Pattern.compile("(?m)^([^\\r\\n]+)").matcher(text);
        while(m.find())out.add(new Line(m.start(1),m.end(1),m.group(1)));return out;
    }

    private static JSONObject extractAttributes(String block)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject();String[] labels={"FOR","DES","CON","INT","SAB","CAR","STR","DEX","WIS","CHA"};
        for(String label:labels){Matcher m=Pattern.compile("(?iu)\\b"+label+"\\b\\s*[:=]?\\s*(\\d{1,2})\\b").matcher(block);if(m.find())out.put(label.toLowerCase(Locale.ROOT),Integer.parseInt(m.group(1)));}
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static JSONArray attackLines(String block)throws Exception{JSONArray out=new JSONArray();for(String line:block.split("\\r?\\n"))if(ATTACK.matcher(line).find()&&(DICE.matcher(line).find()||line.toLowerCase(Locale.ROOT).contains("to hit")||norm(line).contains("para atingir")))putUnique(out,line.trim());return out;}
    private static JSONArray diceTerms(String block)throws Exception{JSONArray out=new JSONArray();Matcher m=DICE.matcher(block);while(m.find())putUnique(out,m.group().replaceAll("\\s+",""));return out;}
    private static int attackCount(String block){Matcher m=Pattern.compile("(?iu)\\b(?:multiataque|multiattack)\\b[^\\n.]*?\\b(\\d{1,2}|dois|duas|two|três|tres|three)\\b").matcher(block);if(!m.find())return attackLinesUnchecked(block)>0?1:0;String v=norm(m.group(1));if("dois".equals(v)||"duas".equals(v)||"two".equals(v))return 2;if("tres".equals(v)||"three".equals(v))return 3;try{return Integer.parseInt(v);}catch(Exception ignored){return 1;}}
    private static int attackLinesUnchecked(String block){int count=0;for(String line:block.split("\\r?\\n"))if(ATTACK.matcher(line).find()&&DICE.matcher(line).find())count++;return count;}
    private static String section(String block,String... labels){String n=norm(block);for(String label:labels){String wanted=norm(label);int at=n.indexOf(wanted);if(at>=0)return block;}return "";}
    private static JSONArray matchingLines(String block,String... terms)throws Exception{JSONArray out=new JSONArray();for(String line:block.split("\\r?\\n")){String n=norm(line);if(containsAny(n,terms))putUnique(out,line.trim());}return out;}
    private static String labeledText(String block,String... labels){for(String label:labels){Matcher m=Pattern.compile("(?ium)^\\s*"+Pattern.quote(label)+"\\s*[:=]?\\s*([^\\r\\n]+)").matcher(block);if(m.find())return m.group(1).trim();}return "";}
    private static int labeledInt(String block,String... labels){String value=labeledText(block,labels);Matcher m=Pattern.compile("\\d{1,3}").matcher(value);return m.find()?Integer.parseInt(m.group()):0;}
    private static String capture(Pattern p,String text,int group){Matcher m=p.matcher(text);return m.find()?m.group(group).trim():"";}
    private static String inferSystem(JSONObject source){String n=norm(source==null?"":source.optString("rule_profile")+" "+source.optString("book"));return n.contains("mystara")||n.contains("becmi")?"mystara":"dnd5e";}
    private static String firstNonEmpty(String a,String b){return a==null||a.length()==0?(b==null?"":b):a;}
    private static void putUnique(JSONArray out,String value)throws Exception{/*BR_JSON_WRAP*/try{String n=norm(value);if(n.length()==0)return;for(int i=0;i<out.length();i++)if(norm(out.optString(i)).equals(n))return;out.put(value);}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean containsAny(String source,String... values){for(String value:values)if(source.contains(norm(value)))return true;return false;}
    private static String norm(String value){String n=Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}

    private static final class Line{final int start,end;final String value;Line(int s,int e,String v){start=s;end=e;value=v;}}
}
