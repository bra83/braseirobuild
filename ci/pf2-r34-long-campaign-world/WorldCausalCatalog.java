package com.braseiro.pathfinder2e;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Catálogo causal procedural.
 *
 * 16 famílias × 8 acontecimentos × 8 contextos × 4 intensidades = 4096 padrões
 * persistidos como catálogo local. Os padrões não são "flavor": cada um carrega
 * um vetor estrutural de efeitos usado pelo WorldCausalityEngine.
 */
public final class WorldCausalCatalog {
    public static final int EXPECTED_PATTERN_COUNT = 4096;

    private static final String[] FAMILIES={
        "guerra","agricultura","saude","clima","comercio","infraestrutura","politica","crime",
        "religiao","cultura","magia","monstros","trabalho","migracao","diplomacia","ecologia"
    };
    private static final String[][] EVENTS={
        {"Escaramuça de fronteira","Incursão inimiga","Cerco prolongado","Bloqueio militar","Recrutamento em massa","Deserções","Motim militar","Armistício"},
        {"Colheita excepcional","Quebra de safra","Praga agrícola","Seca rural","Inundação de lavouras","Doença de rebanho","Expansão de cultivo","Falha de armazenamento"},
        {"Surto infeccioso","Água contaminada","Escassez de curadores","Quarentena","Doença sazonal","Recuperação sanitária","Campanha de cura","Mortalidade incomum"},
        {"Tempestade severa","Inverno rigoroso","Onda de calor","Granizo destrutivo","Enchente","Incêndio florestal","Estiagem","Período de clima favorável"},
        {"Rota comercial interrompida","Feira regional","Alta de preços","Queda de preços","Caravana perdida","Novo mercado","Embargo","Contrabando em expansão"},
        {"Ponte destruída","Estrada concluída","Moinho parado","Porto interditado","Aqueduto reparado","Armazém incendiado","Fortificação ampliada","Desabamento urbano"},
        {"Crise sucessória","Escândalo de autoridade","Golpe local","Reforma administrativa","Novo imposto","Revolta fiscal","Disputa de conselho","Acordo político"},
        {"Onda de banditismo","Rede de extorsão","Sequestros","Furtos em série","Guerra de gangues","Repressão ao crime","Mercado negro","Corrupção de guardas"},
        {"Peregrinação","Cisma religioso","Festival sagrado","Profecia popular","Conflito entre cultos","Reforma de templo","Relíquia descoberta","Luto religioso"},
        {"Festival popular","Torneio","Feira sazonal","Casamento importante","Funeral público","Celebração de fundação","Apresentação itinerante","Festa de colheita"},
        {"Anomalia arcana","Falha de ritual","Surto de magia","Portal instável","Maldição local","Descoberta mágica","Zona de magia fraca","Fenômeno planar"},
        {"Migração de monstros","Predador incomum","Ataque de criatura","Ninho descoberto","Manada agressiva","Caçada bem-sucedida","Monstro territorial","Criaturas expulsas"},
        {"Greve de artesãos","Falta de mão de obra","Boom de construção","Aprendizes em alta","Acidente de trabalho","Êxodo de trabalhadores","Novas oficinas","Conflito de guilda"},
        {"Chegada de refugiados","Êxodo rural","Migração econômica","Retorno de deslocados","Colonização de fronteira","Fuga de perseguição","Diáspora comercial","Reassentamento"},
        {"Tratado comercial","Ruptura diplomática","Aliança defensiva","Incidente de fronteira","Casamento diplomático","Sanções","Missão diplomática","Pacto de não agressão"},
        {"Erosão do solo","Superpopulação de caça","Colapso de pesca","Recuperação florestal","Poluição de rio","Incêndio ecológico","Espécie invasora","Ano de abundância natural"}
    };
    private static final String[] CONTEXTS={"urbano","rural","fronteira","rota_comercial","rio","floresta","montanha","capital"};
    private static final String[] SEVERITIES={"leve","moderado","grave","critico"};

    private WorldCausalCatalog(){}

    public static int catalogSize(){return FAMILIES.length*8*CONTEXTS.length*SEVERITIES.length;}

    public static void seed(SQLiteDatabase db){
        if(db==null)return;
        android.database.Cursor existing=null;
        try{existing=db.rawQuery("SELECT COUNT(*) FROM world_causal_patterns",null);if(existing.moveToFirst()&&existing.getInt(0)>=EXPECTED_PATTERN_COUNT)return;}catch(Exception ignored){}finally{if(existing!=null)existing.close();}
        for(int f=0;f<FAMILIES.length;f++){
            for(int e=0;e<EVENTS[f].length;e++){
                for(int c=0;c<CONTEXTS.length;c++){
                    for(int s=0;s<SEVERITIES.length;s++){
                        String family=FAMILIES[f],name=EVENTS[f][e],context=CONTEXTS[c],severity=SEVERITIES[s];
                        String key=patternKey(family,name,context,severity);
                        ContentValues v=new ContentValues();
                        v.put("pattern_key",key);v.put("family",family);v.put("event_name",name);v.put("context",context);
                        v.put("severity",severity);v.put("polarity",isPositive(name)?1:-1);
                        try{v.put("base_effect_json",effectVector(family,name,s+1,context).toString());v.put("tags_json",new JSONArray().put(family).put(context).put(severity).toString());}
                        catch(Exception ignored){v.put("base_effect_json","{}");v.put("tags_json","[]");}
                        db.insertWithOnConflict("world_causal_patterns",null,v,SQLiteDatabase.CONFLICT_IGNORE);
                    }
                }
            }
        }
    }

    public static JSONObject choose(String regionKey,String context,int day,JSONObject pressure){
        try{
            String family=chooseFamily(regionKey,day,pressure);
            int fi=indexOf(FAMILIES,family);if(fi<0)fi=0;
            int eventIndex=stableIndex(regionKey+"|"+day+"|event|"+family,EVENTS[fi].length);
            String name=EVENTS[fi][eventIndex];
            int severity=chooseSeverity(regionKey,day,pressure);
            String ctx=normalizeContext(context,regionKey,day);
            String key=patternKey(family,name,ctx,SEVERITIES[severity-1]);
            return new JSONObject().put("pattern_key",key).put("family",family).put("event_name",name).put("context",ctx)
                    .put("severity",SEVERITIES[severity-1]).put("severity_level",severity).put("polarity",isPositive(name)?1:-1)
                    .put("effects",effectVector(family,name,severity,ctx));
        }catch(Exception ignored){return new JSONObject();}
    }

    public static JSONObject effectVector(String family,String name,int severity,String context){
        JSONObject e=new JSONObject();
        try{
            int sign=isPositive(name)?1:-1;
            int s=Math.max(1,Math.min(4,severity));
            // Todas as famílias têm consequências materiais; os detalhes são
            // refinados por família e depois amplificados por intensidade.
            e.put("prosperity_delta",0).put("security_delta",0).put("scarcity_delta",0).put("unrest_delta",0)
                    .put("political_delta",0).put("trade_delta",0).put("health_delta",0).put("disease_delta",0)
                    .put("infrastructure_delta",0).put("migration_delta",0).put("war_delta",0).put("price_delta_pct",0d)
                    .put("food_production_delta_pct",0d).put("food_stock_delta_pct",0d).put("drink_stock_delta_pct",0d)
                    .put("medicine_stock_delta_pct",0d).put("population_delta_ppm",0).put("adult_men_extra_loss_ppm",0)
                    .put("soldier_loss_ppm",0).put("displaced_delta_ppm",0).put("aid_pressure",0)
                    .put("commodity_shock",new JSONObject());

            if("guerra".equals(family)){
                e.put("security_delta",-3*s).put("scarcity_delta",2*s).put("unrest_delta",2*s).put("political_delta",2*s)
                        .put("trade_delta",-4*s).put("infrastructure_delta",-2*s).put("war_delta",5*s)
                        .put("food_production_delta_pct",-2.5d*s).put("food_stock_delta_pct",-1.5d*s)
                        .put("population_delta_ppm",-35*s).put("adult_men_extra_loss_ppm",-95*s).put("soldier_loss_ppm",-140*s)
                        .put("displaced_delta_ppm",120*s).put("disease_delta",2*s).put("aid_pressure",20*s)
                        .put("price_delta_pct",2.5d*s).put("commodity_shock",new JSONObject().put("alimentos",.12d*s).put("bebidas",.05d*s).put("ferramentas",.08d*s));
                if(isPositive(name)){invertRelief(e,s);e.put("war_delta",-5*s).put("aid_pressure",0);}
            }else if("agricultura".equals(family)){
                e.put("food_production_delta_pct",sign*5d*s).put("food_stock_delta_pct",sign*3d*s).put("scarcity_delta",-sign*3*s)
                        .put("prosperity_delta",sign*2*s).put("price_delta_pct",-sign*3d*s).put("migration_delta",-sign*s)
                        .put("commodity_shock",new JSONObject().put("alimentos",sign<0?.15d*s:-.10d*s).put("bebidas",sign<0?.05d*s:-.03d*s));
            }else if("saude".equals(family)){
                e.put("health_delta",sign*4*s).put("disease_delta",-sign*5*s).put("prosperity_delta",sign*s)
                        .put("population_delta_ppm",sign>0?8*s:-45*s).put("medicine_stock_delta_pct",sign>0?-1d*s:-4d*s)
                        .put("migration_delta",sign<0?2*s:-s).put("aid_pressure",sign<0?15*s:0).put("commodity_shock",new JSONObject().put("medicamentos",sign<0?.14d*s:-.05d*s));
            }else if("clima".equals(family)){
                e.put("infrastructure_delta",sign*2*s).put("food_production_delta_pct",sign*3d*s).put("security_delta",sign*s)
                        .put("scarcity_delta",-sign*2*s).put("trade_delta",sign*2*s).put("price_delta_pct",-sign*1.5d*s)
                        .put("commodity_shock",new JSONObject().put("alimentos",sign<0?.09d*s:-.04d*s).put("madeira",sign<0?.04d*s:-.02d*s));
            }else if("comercio".equals(family)){
                e.put("trade_delta",sign*5*s).put("prosperity_delta",sign*3*s).put("scarcity_delta",-sign*2*s)
                        .put("price_delta_pct",-sign*3d*s).put("political_delta",sign<0?s:0)
                        .put("commodity_shock",new JSONObject().put("alimentos",sign<0?.06d*s:-.03d*s).put("bebidas",sign<0?.06d*s:-.03d*s).put("luxos",sign<0?.08d*s:-.04d*s));
            }else if("infraestrutura".equals(family)){
                e.put("infrastructure_delta",sign*5*s).put("trade_delta",sign*3*s).put("security_delta",sign*2*s)
                        .put("prosperity_delta",sign*2*s).put("price_delta_pct",-sign*s)
                        .put("commodity_shock",new JSONObject().put("ferramentas",sign<0?.08d*s:-.04d*s).put("madeira",sign<0?.05d*s:-.02d*s));
            }else if("politica".equals(family)){
                e.put("political_delta",sign<0?5*s:-3*s).put("unrest_delta",sign<0?4*s:-2*s).put("security_delta",sign*2*s)
                        .put("trade_delta",sign*s).put("prosperity_delta",sign*s).put("migration_delta",sign<0?s:0);
            }else if("crime".equals(family)){
                e.put("criminality_delta",sign<0?5*s:-4*s).put("security_delta",sign*3*s).put("trade_delta",sign*2*s)
                        .put("prosperity_delta",sign*s).put("unrest_delta",sign<0?2*s:-s).put("price_delta_pct",sign<0?1.5d*s:-1d*s);
            }else if("religiao".equals(family)){
                e.put("faith_delta",sign*4*s).put("unrest_delta",sign<0?3*s:-s).put("prosperity_delta",sign*s)
                        .put("migration_delta",sign<0?s:0).put("trade_delta",sign>0?s:0);
            }else if("cultura".equals(family)){
                e.put("prosperity_delta",2*s).put("unrest_delta",-s).put("trade_delta",2*s).put("crime_delta",s)
                        .put("disease_delta",s>2?1:0).put("price_delta_pct",1d*s);
            }else if("magia".equals(family)){
                e.put("security_delta",sign*2*s).put("political_delta",sign<0?s:0).put("health_delta",sign*s)
                        .put("infrastructure_delta",sign*s).put("migration_delta",sign<0?s:0);
            }else if("monstros".equals(family)){
                e.put("security_delta",sign*4*s).put("food_production_delta_pct",sign*2d*s).put("trade_delta",sign*2*s)
                        .put("population_delta_ppm",sign<0?-18*s:3*s).put("migration_delta",sign<0?2*s:-s).put("aid_pressure",sign<0?10*s:0);
            }else if("trabalho".equals(family)){
                e.put("prosperity_delta",sign*3*s).put("trade_delta",sign*2*s).put("infrastructure_delta",sign*2*s)
                        .put("migration_delta",sign<0?2*s:-s).put("price_delta_pct",sign<0?1.5d*s:-1d*s);
            }else if("migracao".equals(family)){
                e.put("migration_delta",sign>0?-2*s:3*s).put("unrest_delta",sign<0?2*s:0).put("trade_delta",sign*s)
                        .put("population_delta_ppm",sign*35*s).put("displaced_delta_ppm",sign<0?80*s:-50*s);
            }else if("diplomacia".equals(family)){
                e.put("political_delta",sign<0?4*s:-3*s).put("trade_delta",sign*3*s).put("war_delta",sign<0?3*s:-4*s)
                        .put("security_delta",sign*2*s).put("prosperity_delta",sign*2*s);
            }else if("ecologia".equals(family)){
                e.put("food_production_delta_pct",sign*3d*s).put("health_delta",sign*2*s).put("scarcity_delta",-sign*2*s)
                        .put("trade_delta",sign*s).put("commodity_shock",new JSONObject().put("alimentos",sign<0?.08d*s:-.04d*s).put("madeira",sign<0?.06d*s:-.03d*s));
            }
            if("capital".equals(context)){e.put("political_delta",e.optInt("political_delta")+s).put("trade_delta",e.optInt("trade_delta")+sign*s);}
            if("fronteira".equals(context)){e.put("security_delta",e.optInt("security_delta")-s).put("war_delta",e.optInt("war_delta")+s);}
            if("rota_comercial".equals(context))e.put("trade_delta",e.optInt("trade_delta")+sign*2*s);
            if("rural".equals(context))e.put("food_production_delta_pct",e.optDouble("food_production_delta_pct")+sign*1.5d*s);
        }catch(Exception ignored){}
        return e;
    }

    public static String patternKey(String family,String event,String context,String severity){
        return "causal:"+uuid((family+"|"+event+"|"+context+"|"+severity).toLowerCase(Locale.ROOT));
    }

    private static String chooseFamily(String regionKey,int day,JSONObject p){
        int war=p==null?0:p.optInt("war_exposure"),disease=p==null?0:p.optInt("disease_pressure"),scarcity=p==null?0:p.optInt("scarcity"),politics=p==null?0:p.optInt("political_pressure"),crime=p==null?0:p.optInt("criminality");
        String[] weighted;
        if(war>=45)weighted=new String[]{"guerra","guerra","migracao","saude","comercio","diplomacia","infraestrutura","crime"};
        else if(disease>=45)weighted=new String[]{"saude","saude","migracao","trabalho","comercio","religiao","politica","cultura"};
        else if(scarcity>=50)weighted=new String[]{"agricultura","comercio","crime","migracao","politica","saude","ecologia","clima"};
        else if(politics>=55||crime>=55)weighted=new String[]{"politica","crime","diplomacia","cultura","religiao","comercio","trabalho","guerra"};
        else weighted=FAMILIES;
        return weighted[stableIndex(regionKey+"|"+day+"|family",weighted.length)];
    }

    private static int chooseSeverity(String regionKey,int day,JSONObject p){
        int pressure=0;if(p!=null)pressure=Math.max(Math.max(p.optInt("war_exposure"),p.optInt("disease_pressure")),Math.max(p.optInt("scarcity"),p.optInt("unrest")));
        int roll=stableIndex(regionKey+"|"+day+"|severity",100);
        if(pressure>=75||roll>=96)return 4;
        if(pressure>=55||roll>=80)return 3;
        if(pressure>=30||roll>=45)return 2;
        return 1;
    }

    private static String normalizeContext(String context,String regionKey,int day){
        String c=context==null?"":context.trim().toLowerCase(Locale.ROOT);
        for(String x:CONTEXTS)if(x.equals(c))return x;
        return CONTEXTS[stableIndex(regionKey+"|"+day+"|context",CONTEXTS.length)];
    }

    private static boolean isPositive(String name){
        String n=name==null?"":name.toLowerCase(Locale.ROOT);
        return n.matches(".*(armistício|armisticio|excepcional|expansão|expansao|recuperação|recuperacao|campanha de cura|favorável|favoravel|feira|queda de preços|novo mercado|concluída|concluida|reparado|ampliada|reforma|acordo|repressão|repressao|peregrinação|peregrinacao|festival|relíquia|reliquia|torneio|casamento|celebração|celebracao|festa|descoberta|bem-sucedida|expulsas|boom|aprendizes|novas oficinas|retorno|colonização|colonizacao|reassentamento|tratado|aliança|alianca|missão diplomática|missao diplomatica|pacto|recuperação florestal|recuperacao florestal|abundância|abundancia).*");
    }

    private static void invertRelief(JSONObject e,int s)throws Exception{
        e.put("security_delta",2*s).put("scarcity_delta",-s).put("unrest_delta",-2*s).put("political_delta",-s)
                .put("trade_delta",2*s).put("infrastructure_delta",s).put("food_production_delta_pct",1.5d*s)
                .put("food_stock_delta_pct",1d*s).put("population_delta_ppm",0).put("adult_men_extra_loss_ppm",0)
                .put("soldier_loss_ppm",0).put("displaced_delta_ppm",-50*s).put("disease_delta",-s).put("price_delta_pct",-1.5d*s)
                .put("commodity_shock",new JSONObject().put("alimentos",-.05d*s).put("bebidas",-.02d*s));
    }

    private static int stableIndex(String seed,int size){return size<=1?0:Math.floorMod(seed==null?0:seed.hashCode(),size);}
    private static int indexOf(String[] a,String v){for(int i=0;i<a.length;i++)if(a[i].equals(v))return i;return -1;}
    private static String uuid(String raw){return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();}
}
