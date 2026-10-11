package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Structured, source-bound Strixhaven: A Curriculum of Chaos campaign data.
 * Stores mechanics/facts needed at runtime, not reproduced book prose.
 */
public final class StrixhavenCompiledCampaignEngine {
    private StrixhavenCompiledCampaignEngine(){}

    private static JSONArray strings(String... values){
        JSONArray a=new JSONArray();if(values!=null)for(String v:values)a.put(v);return a;
    }
    private static JSONObject chapter(int year,String title,int startLevel,int endLevel,String... phases){
        JSONObject o=new JSONObject();try{o.put("year",year).put("title",title).put("start_level",startLevel).put("end_level",endLevel).put("phases",strings(phases));}catch(Exception ignored){}return o;
    }
    public static JSONArray chapters(){
        JSONArray a=new JSONArray();
        a.put(chapter(1,"Campus Kerfuffle",1,4,
            "First Day on Campus","Orientation Challenge","Frog's the Word","Magical Physiologies — Exam: Slaadi",
            "Work Hard, Play Harder","Magical Physiologies — Exam: Owlbears","Campus Daredevils",
            "Magical Physiologies — Exam: Otyughs","All the World's a Stage","Secrets in Sedgemoor"));
        a.put(chapter(2,"Hunt for Mage Tower",4,6,
            "Back in Session","Mascots Everywhere!","Scrivening and Symbology — Exam: Glyph of Warding",
            "Party at the Rose Stage","Practice Makes Perfect","Scrivening and Symbology — Exam: Symbol Glyphs",
            "Getting Some Air","Dangerous Knowledge","Scrivening and Symbology — Exam: Augury",
            "Tell Us in Song!","Battle of Strixhaven"));
        a.put(chapter(3,"The Magister's Masquerade",6,8,
            "Cordially Invited","Reception at Bow's End","Fate at Furygale","Advanced Arcane History — Exam: Relics",
            "Dressing for Success","Party Preparations","Advanced Arcane History — Exam: Devices","Suspicions",
            "Fancying Up Footwork","Preparing the Hall","Advanced Arcane History — Exam: Unearthed Arcana",
            "A Starlit Night","Midnight at Lorehold","Aftermath"));
        a.put(chapter(4,"A Reckoning in Ruins",8,10,
            "A Sinister Shadow","A Brief Distraction","Arcane Herbology — Exam: Vegetation Identification",
            "Clue in the Bayou","No Time to Lose","Into the Badlands","This Is Surreal, Isn't It?",
            "Ruins of Caerdoon","Confronting Murgaxor"));
        return a;
    }
    public static JSONObject chapterForYear(int year){
        JSONArray a=chapters();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optInt("year")==year)return o;}return a.optJSONObject(0);
    }
    public static String yearSummary(int year){
        JSONObject c=chapterForYear(year);JSONArray p=c.optJSONArray("phases");StringBuilder b=new StringBuilder();
        b.append("Ano ").append(c.optInt("year",1)).append(" • ").append(StrixhavenPtBrAcademicCatalog.campaignTitle(c.optString("title"))).append(" • níveis ")
         .append(c.optInt("start_level",1)).append("→").append(c.optInt("end_level",4)).append("\n");
        if(p!=null)for(int i=0;i<p.length();i++){if(i>0)b.append("  ›  ");b.append(StrixhavenPtBrAcademicCatalog.eventTitle(p.optString(i)));}
        return b.toString();
    }
    public static String masterContext(int year,int semester){
        JSONObject c=chapterForYear(year);JSONArray phases=c.optJSONArray("phases");StringBuilder b=new StringBuilder();
        b.append(" CAMPANHA OFICIAL STRIXHAVEN COMPILADA: ano ").append(c.optInt("year",1))
         .append(", semestre ").append(Math.max(1,Math.min(2,semester))).append(", arco '").append(c.optString("title")).append("'. ");
        b.append("Níveis esperados neste ano: ").append(c.optInt("start_level")).append(" a ").append(c.optInt("end_level")).append(". ");
        b.append("Sequência canônica disponível no runtime: ");
        if(phases!=null)for(int i=0;i<phases.length();i++){if(i>0)b.append(" -> ");b.append(phases.optString(i));}
        b.append(". Não substitua esses marcos por uma campanha escolar genérica; preserve pistas, relações, exames e progressão já estabelecidos e não avance marco sem resolução da ficção correspondente. ");
        return b.toString();
    }

    private static JSONObject exam(String id,int year,String course,String title,int studyDc,
                                   String ability1,String skill1,int dc1,String ability2,String skill2,int dc2){
        JSONObject o=new JSONObject();try{
            o.put("id",id).put("year",year).put("course",course).put("title",title).put("study_dc",studyDc)
             .put("ability1",ability1).put("skill1",skill1).put("dc1",dc1)
             .put("ability2",ability2).put("skill2",skill2).put("dc2",dc2);
        }catch(Exception ignored){}return o;
    }
    public static JSONArray exams(){
        JSONArray a=new JSONArray();
        a.put(exam("slaadi",1,"Magical Physiologies","Slaadi",12,"Inteligência","Arcana",12,"Sabedoria","Intuição",12));
        a.put(exam("owlbears",1,"Magical Physiologies","Owlbears",12,"Inteligência","Natureza",12,"Sabedoria","Adestrar Animais",12));
        a.put(exam("otyughs",1,"Magical Physiologies","Otyughs",13,"Inteligência","Investigação",13,"Carisma","Persuasão",13));
        a.put(exam("glyph_of_warding",2,"Scrivening and Symbology","Glyph of Warding",15,"Inteligência","Investigação",15,"Inteligência","Arcana",15));
        a.put(exam("symbol_glyphs",2,"Scrivening and Symbology","Symbol Glyphs",16,"Inteligência","Investigação",16,"Carisma","Persuasão",16));
        a.put(exam("augury",2,"Scrivening and Symbology","Augury",17,"Inteligência","História",17,"Sabedoria","Intuição",17));
        a.put(exam("relics",3,"Advanced Arcane History","Relics",17,"Inteligência","Arcana",17,"Inteligência","História",17));
        a.put(exam("devices",3,"Advanced Arcane History","Devices",18,"Inteligência","História",18,"Sabedoria","Intuição",18));
        a.put(exam("unearthed_arcana",3,"Advanced Arcane History","Unearthed Arcana",19,"Inteligência","Arcana ou História",19,"Carisma","Persuasão",19));
        a.put(exam("vegetation_identification",4,"Arcane Herbology","Vegetation Identification",20,"Inteligência","Natureza",13,"Sabedoria","Sobrevivência",20));
        return a;
    }
    public static JSONArray examsForYear(int year){
        JSONArray out=new JSONArray(),all=exams();for(int i=0;i<all.length();i++){JSONObject e=all.optJSONObject(i);if(e!=null&&e.optInt("year")==year)out.put(e);}return out;
    }
    public static String[] examLabels(int year){
        JSONArray a=examsForYear(year);String[] out=new String[a.length()];for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);out[i]=StrixhavenPtBrAcademicCatalog.examCourse(e.optString("course"))+" — "+StrixhavenPtBrAcademicCatalog.examTitle(e.optString("title"));}return out;
    }
    public static JSONObject examAt(int year,int index){
        JSONArray a=examsForYear(year);if(a.length()==0)return new JSONObject();int safe=Math.max(0,Math.min(index,a.length()-1));JSONObject e=a.optJSONObject(safe);return e==null?new JSONObject():e;
    }
    public static String examRuleText(JSONObject e){
        if(e==null||e.length()==0)return "";
        return "Estudo: CD "+e.optInt("study_dc")+" • Teste 1: "+e.optString("ability1")+" ("+e.optString("skill1")+") CD "+e.optInt("dc1")
            +" • Teste 2: "+e.optString("ability2")+" ("+e.optString("skill2")+") CD "+e.optInt("dc2");
    }

    private static JSONObject student(String name,String college,int startingYear,String boon,String bane){
        JSONObject o=new JSONObject();try{o.put("name",name).put("college",college).put("starting_year",startingYear).put("bond_boon",boon).put("bond_bane",bane);}catch(Exception ignored){}return o;
    }
    public static JSONArray fellowStudents(){
        JSONArray a=new JSONArray();
        a.put(student("Aurora Luna Wynterstarr","Witherbloom",1,"Músicos do campus cobrem suas despesas uma noite por semana em locais com música ao vivo.","Músicas ao vivo no campus passam a incluir provocações dirigidas a você."));
        a.put(student("Bhedum “Rampart” Sooviij","Lorehold",1,"Colegas ajudam você a se esconder da segurança do campus.","Livros importantes, sobretudo de história, somem quando você precisa deles; não é possível usar a opção de virar a noite estudando."));
        a.put(student("Cadoras Damellawar","Quandrix",1,"O círculo de LARP compartilha informações sobre Aberrações, Monstruosidades e Gosmas.","Você vira pária do LARP do campus e monstros de jogo passam a receber seu nome."));
        a.put(student("Drazhomir Yarnask","Quandrix",2,"A poesia de Drazhomir melhora sua recepção social com quase todos no campus.","Funcionários e bibliotecários da Biblioplex dificultam sua busca por livros."));
        a.put(student("Grayson Wildemere","Silverquill",1,"Ao comprar no campus, há 25% de chance de Grayson conseguir o item pela metade do preço.","Colunas de fofoca publicam versões exageradas de seus piores traços."));
        a.put(student("Greta Gorunn","Lorehold",1,"Quando você precisa mover algo pesado, integrantes dos Iron-Lifters aparecem para ajudar.","Objetos pesados surgem em lugares inconvenientes, inclusive bloqueando sua porta."));
        a.put(student("Javenesh Stoutclaw","Lorehold",2,"A reputação de Javenesh desencoraja colegas que pretendam intimidar ou agredir você.","No Bow's End Tavern, qualquer sinal de confusão é atribuído a você e os servidores pedem que saia."));
        a.put(student("Larine Arneza","Quandrix",1,"Larine ou colegas criam distrações para você sair de um lugar do campus sem ser notado.","Você recebe os piores ingressos para apresentações, quando recebe algum."));
        a.put(student("Melwythorne","Witherbloom",1,"A serenidade de Melwythorne ajuda você a acalmar colegas e obter informação deles.","Silkball Club e Student-Mages of Faith ignoram sua presença."));
        a.put(student("Mina Lee","Silverquill",2,"Mina pesquisa informação obscura que não seja secreta e retorna com a resposta em até uma semana.","O Firejolt Cafe nunca tem o primeiro pedido de bebida que você faz."));
        a.put(student("Nora Ann Wu","Prismari",2,"O respeito da faculdade por Nora se estende a você, facilitando respostas diretas a pedidos básicos.","Poucos professores se dispõem a ajudar você com pedidos básicos."));
        a.put(student("Quentillius Antiphiun Melentor III","Prismari",1,"Sua voz ganha autoridade e colegas tendem a obedecer comandos em situações urgentes.","Pequenos grupos aparecem em suas apresentações apenas para vaiar você."));
        a.put(student("Rosimyffenbip “Rosie” Wuzfeddlims","Lorehold",1,"Viagens de distância significativa podem ser concluídas em metade do tempo normal.","Em esportes do campus, fora dos grandes eventos, faltas são marcadas constantemente contra você."));
        a.put(student("Rubina Larkingdale","Silverquill",1,"Você consegue prender a atenção de uma sala inteira e fazer com que escutem suas palavras favoravelmente.","Bandas que se apresentam em público demonstram reprovação quando percebem você."));
        a.put(student("Shuvadri Glintmantle","Silverquill",1,"Quando precisa rapidamente de ajuda de colegas, eles interrompem o que fazem para ajudar por consideração a Shuvadri.","Grafites com alusões ofensivas a você aparecem repetidamente pelo campus."));
        a.put(student("Tilana Kapule","Quandrix",1,"Tilana oferece análise e sugere uma solução útil que você ainda não tinha considerado.","Professores passam a desconfiar de você e ajudam menos."));
        a.put(student("Urzmaktok Grojsh","Witherbloom",2,"Você sempre consegue um parceiro de estudos, diretamente ou por indicação de Urzmaktok.","NPCs do campus recusam estudar com você."));
        a.put(student("Zanther Bowen","Prismari",1,"Sua associação com Zanther eleva seu status social e colegas se oferecem para fazer favores.","Você deixa de receber convites para festas e eventos, embora seus amigos continuem recebendo."));
        return a;
    }
    private static String norm(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replace("“","").replace("”","").replace("\"","").trim();}
    public static String[] fellowStudentNames(){
        JSONArray a=fellowStudents();String[] n=new String[a.length()];for(int i=0;i<a.length();i++)n[i]=a.optJSONObject(i).optString("name");return n;
    }
    public static JSONObject fellowStudent(String name){
        String q=norm(name);JSONArray a=fellowStudents();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null){String n=norm(o.optString("name"));if(n.equals(q)||n.contains(q)||q.contains(n))return o;}}return new JSONObject();
    }

    public static JSONObject coverage(){
        JSONObject o=new JSONObject();try{
            o.put("campaign_years",4).put("campaign_start_level",1).put("campaign_end_level",10)
             .put("canonical_exams",10).put("fellow_students",18)
             .put("compiled_block_rag_required",false).put("full_book_closure",false)
             .put("source","Strixhaven: A Curriculum of Chaos");
        }catch(Exception ignored){}return o;
    }
}
