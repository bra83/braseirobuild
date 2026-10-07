package com.braseiro.dnd5e;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Source-bound Strixhaven Chapter 7 bestiary. The original source block is bundled in the APK. */
public final class StrixhavenBestiaryRuntimeEngine {
    private StrixhavenBestiaryRuntimeEngine(){}
    public static final String SOURCE_BOOK="Strixhaven: A Curriculum of Chaos";

    private static final class E {
        final String name,type,cr;final int ac,hp;
        E(String n,String t,String c,int a,int h){name=n;type=t;cr=c;ac=a;hp=h;}
    }
    private static E e(String n,String t,String c,int a,int h){return new E(n,t,c,a,h);}
    private static final E[] ALL=new E[]{
        e("Archaic","Celestial","18",20,245),e("Art Elemental Mascot","Elemental","1/4",11,18),
        e("Beledros Witherbloom","Dragon","24",22,444),e("Brackish Trudge","Plant","3",12,68),
        e("Cogwork Archivist","Construct","4",17,90),e("Daemogoth","Fiend","10",16,157),
        e("Daemogoth Titan","Fiend","16",17,203),e("First-Year Student","Humanoid","1/2",11,33),
        e("Fractal Mascot","Construct","1/4",12,27),e("Galazeth Prismari","Dragon","23",21,402),
        e("Groff","Plant","4",13,68),e("Inkling Mascot","Ooze","1/4",13,18),
        e("Lorehold Apprentice","Humanoid","2",12,33),e("Lorehold Pledgemage","Humanoid","4",13,60),
        e("Lorehold Professor of Chaos","Humanoid","7",12,110),e("Lorehold Professor of Order","Humanoid","7",12,104),
        e("Mage Hunter","Monstrosity","5",15,85),e("Oracle of Strixhaven","Humanoid","15",12,150),
        e("Oriq Blood Mage","Humanoid","9",16,127),e("Oriq Recruiter","Humanoid","4",16,55),
        e("Pest Mascot","Monstrosity","1/4",13,22),e("Prismari Apprentice","Humanoid","2",12,44),
        e("Prismari Pledgemage","Humanoid","4",12,66),e("Prismari Professor of Expression","Humanoid","7",13,97),
        e("Prismari Professor of Perfection","Humanoid","7",13,97),e("Quandrix Apprentice","Humanoid","2",11,44),
        e("Quandrix Pledgemage","Humanoid","4",12,66),e("Quandrix Professor of Substance","Humanoid","7",12,104),
        e("Quandrix Professor of Theory","Humanoid","7",12,97),e("Relic Sloth","Beast","2",13,76),
        e("Ruin Grinder","Construct","5",16,82),e("Shadrix Silverquill","Dragon","22",21,363),
        e("Silverquill Apprentice","Humanoid","2",12,44),e("Silverquill Pledgemage","Humanoid","4",13,66),
        e("Silverquill Professor of Radiance","Humanoid","7",12,97),e("Silverquill Professor of Shadow","Humanoid","7",12,97),
        e("Spirit Statue Mascot","Construct","1/4",14,26),e("Strixhaven Campus Guide","Construct","1",17,31),
        e("Tanazir Quandrix","Dragon","24",21,444),e("Velomachus Lorehold","Dragon","25",21,487),
        e("Witherbloom Apprentice","Humanoid","2",13,44),e("Witherbloom Pledgemage","Humanoid","4",12,66),
        e("Witherbloom Professor of Decay","Humanoid","7",14,105),e("Witherbloom Professor of Growth","Humanoid","7",13,112)
    };
    public static int count(){return ALL.length;}
    private static String norm(String s){String n=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.US);return n.replaceAll("[^a-z0-9]+"," ").trim();}
    public static String id(String name){return "strixhaven_"+norm(name).replace(' ','_');}
    private static String read(Context c){
        try(InputStream in=c.getAssets().open("strixhaven_runtime/ch7.txt");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }catch(Exception e){return "";}
    }
    private static E entry(String name){String q=norm(name);for(E e:ALL){String n=norm(e.name);if(n.equals(q)||n.contains(q)||q.contains(n))return e;}return null;}
    private static String[] aliases(E e){
        if(e==null)return new String[0];
        String n=e.name;
        if(n.contains("Professor of ")){String x="Professor of ";return new String[]{n,n.replace(x,"Prof. of "),n.replace(x,"Prof, of ")};}
        return new String[]{n};
    }
    public static String sourceContext(Context c,String name){
        E e=entry(name);if(e==null)return "";String src=read(c);if(src.length()==0)return "";
        String ns=norm(src);int best=-1,bestScore=-1;
        for(String alias:aliases(e)){String q=norm(alias);int from=0;while(true){int p=ns.indexOf(q,from);if(p<0)break;
            int lo=Math.max(0,p-4200),hi=Math.min(ns.length(),p+5000);String around=ns.substring(lo,hi);
            int score=(around.contains("armor class")?5:0)+(around.contains("hit points")?5:0)+(around.contains("challenge "+e.cr)?8:0)+(p>5000?3:0);
            if(score>bestScore){bestScore=score;best=p;}from=p+Math.max(1,q.length());
        }}
        if(best<0)return "";
        int a=Math.max(0,best-4800),b=Math.min(src.length(),best+6200);return src.substring(a,b);
    }
    private static JSONArray fallbackActions(E e,String source){
        JSONArray a=new JSONArray();try{
            JSONObject action=new JSONObject().put("name","Bloco oficial de "+e.name)
                .put("special",true).put("source_bound",true)
                .put("rules_text",source.substring(0,Math.min(3500,source.length())));
            a.put(action);
        }catch(Exception ignored){}return a;
    }
    public static synchronized JSONObject install(Context c){
        JSONObject report=new JSONObject();JSONArray failures=new JSONArray();int ok=0;
        for(E e:ALL)try{
            String source=sourceContext(c,e.name);
            JSONObject input=new JSONObject().put("excerpt",source).put("rule_id",id(e.name)).put("book",SOURCE_BOOK).put("system","dnd5e").put("edition","5e");JSONArray compiled=CreatureStatBlockCompiler.compile(input);JSONObject raw=compiled.length()>0&&compiled.optJSONObject(0)!=null?compiled.optJSONObject(0):new JSONObject();
            raw.put("id",id(e.name)).put("name",e.name).put("source_book",SOURCE_BOOK).put("creature_type",e.type)
               .put("armor_class",e.ac).put("hit_points",e.hp).put("challenge_rating",e.cr)
               .put("source_locator","Chapter 7 • "+e.name).put("source_bound",true).put("full_statblock_claim",false)
               .put("source_text",source);
            JSONArray actions=raw.optJSONArray("actions");if(actions==null||actions.length()==0)raw.put("actions",fallbackActions(e,source));
            if(raw.optJSONObject("movement")==null)raw.put("movement",new JSONObject().put("walk_ft",30));
            if(raw.optJSONObject("abilities")==null)raw.put("abilities",new JSONObject());
            if(raw.optJSONObject("senses")==null)raw.put("senses",new JSONObject());
            if(raw.optJSONArray("traits")==null)raw.put("traits",new JSONArray());
            raw.put("mechanical_basis",new JSONObject().put("source_type","compiled_statblock").put("content_kind","statblock")
                .put("content_name",id(e.name)).put("source_book",SOURCE_BOOK).put("source_locator","Chapter 7 • "+e.name));
            JSONObject r=CompiledStatblockEngine.registerAuthorized(raw);if(r.optBoolean("registered",false))ok++;else failures.put(e.name+":"+r.toString());
        }catch(Exception ex){failures.put(e.name+":"+String.valueOf(ex.getMessage()));}
        try{report.put("expected",ALL.length).put("registered",ok).put("failures",failures).put("source_bound",true).put("rag_required",false);}catch(Exception ignored){}
        return report;
    }
    public static JSONArray manifest(){
        JSONArray a=new JSONArray();for(E e:ALL)try{a.put(new JSONObject().put("id",id(e.name)).put("name",e.name).put("type",e.type).put("cr",e.cr).put("ac",e.ac).put("hp",e.hp));}catch(Exception ignored){}return a;
    }
    public static String sourceForMentions(Context c,String text,int maxEntries,int maxChars){
        String n=norm(text);StringBuilder out=new StringBuilder();int count=0;
        for(E e:ALL){if(count>=maxEntries||out.length()>=maxChars)break;String en=norm(e.name);if(n.contains(en)||n.contains(en.replace(" professor of "," prof of "))){
            String src=sourceContext(c,e.name);if(src.length()==0)continue;int room=Math.max(0,maxChars-out.length());if(room==0)break;
            out.append("\n--- ").append(e.name).append(" | statblock_id=").append(id(e.name)).append(" | CR ").append(e.cr).append(" ---\n");
            out.append(src.substring(0,Math.min(room,Math.min(src.length(),5200))));count++;
        }}
        return out.toString();
    }
    public static String catalogInstruction(){
        return "BESTIÁRIO STRIXHAVEN: 44 blocos oficiais estão compilados no runtime com IDs strixhaven_<nome_normalizado>. Ao criar encontro com criatura do livro, use o statblock_id canônico; não improvise CA/PV/CR.";
    }
}
