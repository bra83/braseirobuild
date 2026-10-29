package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Catalogo ORIGINAL do Braseiro. Inspirado na estrutura de pesquisa/colheita
 * de suplementos de culinaria de monstros, SEM transcrever suas receitas.
 * Os efeitos aqui sao regras opcionais da campanha; nao sao D&D 2024 RAW.
 */
public final class WilemCookbookCatalog {
    private WilemCookbookCatalog(){}
    private static final String[][] COMMON={
        {"batata","Batata","horta,mercado,feira","2","raiz"},
        {"cebola","Cebola","horta,mercado,feira","2","raiz"},
        {"nabo","Nabo","horta,mercado,feira","2","raiz"},
        {"cenoura","Cenoura","horta,mercado,feira","2","raiz"},
        {"repolho","Repolho","horta,mercado,feira","3","folha"},
        {"alho","Alho","horta,mercado,feira","2","tempero"},
        {"cogumelo","Cogumelos comestiveis","floresta,mercado","4","fungo"},
        {"feijao","Feijao","mercado,feira,horta","2","grao"},
        {"lentilha","Lentilha","mercado,feira","3","grao"},
        {"cevada","Cevada","mercado,feira","2","grao"},
        {"arroz","Arroz","mercado,feira","3","grao"},
        {"abobora","Abobora","horta,mercado","3","fruto"},
        {"beterraba","Beterraba","horta,mercado","3","raiz"},
        {"maça","Maca","pomar,mercado","3","fruta"},
        {"mel","Mel","mercado,apicultor","6","doce"},
        {"ovos","Ovos","fazenda,mercado","4","proteina"},
        {"queijo","Queijo","mercado,fazenda","5","laticinio"},
        {"peixe","Peixe fresco","rio,lago,mercado","5","proteina"},
        {"carne_caca","Carne de caca comum","cacada,mercado","7","proteina"},
        {"ervas","Ervas culinarias","horta,prado,mercado","2","tempero"},
        {"farinha","Farinha","mercado,moinho","3","grao"},
        {"sal","Sal","mercado,mercador","1","tempero"},
        {"agua","Agua potavel","poco,fonte,mercado","0","agua"},
        {"oleo","Oleo de cozinha","mercado,mercador","4","gordura"}
    };
    // Nivel recomendado e tema de efeito sao deliberadamente conservadores.
    private static final String[][] MONSTERS={
        {"javali","Javali selvagem","floresta","1","vigor"},
        {"lobo","Lobo terrivel","floresta","2","sentidos"},
        {"aranha","Aranha gigante","caverna","3","toxina"},
        {"crocodilo","Crocodilo gigante","pantano","3","resistencia"},
        {"sapo","Sapo gigante","pantano","3","saltos"},
        {"escorpiao","Escorpiao gigante","deserto","4","toxina"},
        {"coruja_urso","Coruja-urso","floresta","4","vigor"},
        {"ankheg","Ankheg","campo","4","escavacao"},
        {"bico_machado","Bico de machado","campo","3","velocidade"},
        {"caranguejo","Caranguejo gigante","litoral","2","resistencia"},
        {"cobra","Cobra gigante","pantano","4","sentidos"},
        {"lagarto","Lagarto gigante","deserto","2","resistencia"},
        {"morcego","Morcego gigante","caverna","3","sentidos"},
        {"lagosta","Lagosta gigante","litoral","3","vigor"},
        {"urso","Urso-coruja de neblina","bosque","5","sentidos"},
        {"bulette","Bulette","planicie","6","escavacao"},
        {"chuul","Chuul","pantano","7","toxina"},
        {"basilisco","Basilisco","caverna","8","resistencia"},
        {"quimera","Quimera","montanha","9","resistencia"},
        {"verme","Verme purpura","deserto","11","escavacao"},
        {"salamandra","Salamandra de brasa","vulcao","10","calor"},
        {"elemental_agua","Peixe das aguas feericas","lago feerico","8","agua"},
        {"hidra","Hidra","pantano","12","vigor"},
        {"serpente_mar","Serpente marinha","mar","9","agua"}
    };
    private static final String[][] BAITS={
        {"isca_mel","Trilha doce de mel","mel:1;maça:1","chamar","0","8","onivoro","Atrai curiosos pelo aroma; nao causa condicoes."},
        {"isca_peixe","Isca de peixe fresco","peixe:1;sal:1","chamar","0","8","carnivoro","Atrai predadores que comam peixe."},
        {"isca_tuberculo","Raizes para herbivoros","batata:1;cenoura:1","chamar","0","8","herbivoro","Atrai sem ferir; nao confere controle mental."},
        {"isca_mel_fantasia","Mel de bruma feerica","mel:1;essencia_feerica:1","sonolencia","5","15","onivoro","Requer ingrediente totalmente ficticio; salvaguarda CON e consumo confirmado."},
        {"isca_esporo_fantasia","Farofa de esporo onirico","farinha:1;esporo_onirico:1","sonolencia","6","16","onivoro","Componente fantastico; nao descreve sedativos reais."},
        {"isca_cristal_fantasia","Biscoito de cristal giratorio","farinha:1;cristal_vertigem:1","tontura","5","15","onivoro","Desorientacao fantastica por 1 rodada em falha de salvaguarda."},
        {"isca_lua_fantasia","Bolinho de lua palida","farinha:1;po_lua_palido:1","lentidao","7","17","onivoro","Movimento reduzido por 1 rodada; nao paralisa."},
        {"isca_sal_azul","Petisco de sal azul","carne_caca:1;sal_azul_arcano:1","tontura","8","18","carnivoro","Resistencia a CON evita a desorientacao."},
        {"isca_fungo_sonho","Cogumelos do sono breve","cogumelo:1;fungo_sonho:1","sonolencia","9","18","onivoro","Falha na salvaguarda pode deixar sonolento; imunes a sono nao sao afetados."},
        {"isca_aroma","Sementes de passagem","cevada:1;ervas:1","dispersar","0","11","herbivoro","Pode deslocar curiosidade em direcao a comida, nao subjugar criaturas."},
        {"isca_fermento","Massa de perfume feerico","farinha:1;mel:1;oleo:1","chamar","0","12","onivoro","Aroma distante dependente de vento e apetite."},
        {"isca_brasa","Costela defumada de trilha","carne_caca:1;ervas:1;sal:1","chamar","0","12","carnivoro","Fumaca e aroma tambem podem chamar criaturas indesejadas."}
    };
    private static JSONObject ingredient(String id,String name,String sources,int copper,String category,int tier,String note)throws Exception{
        return new JSONObject().put("id",id).put("name",name).put("source_locations",sources)
            .put("suggested_price_cp",copper).put("category",category).put("tier",tier)
            .put("note",note).put("source","braseiro_original");
    }
    public static JSONArray ingredients(){
        JSONArray arr=new JSONArray();
        try{
            for(String[] c:COMMON)arr.put(ingredient(c[0],c[1],c[2],Integer.parseInt(c[3]),c[4],0,"Comum; oferta sujeita a estacao e contexto local."));
            for(String[] m:MONSTERS)arr.put(ingredient("carne_"+m[0],"Parte culinaria de "+m[1],m[2],0,"monstro",Integer.parseInt(m[3]),"Exige colheita, identificacao e respeito a ecologia."));
            String[][] arcane={{"essencia_feerica","Essencia feerica"},{"esporo_onirico","Esporo onirico"},{"cristal_vertigem","Cristal da vertigem"},{"po_lua_palido","Po de lua palida"},{"sal_azul_arcano","Sal azul arcano"},{"fungo_sonho","Fungo do sonho"}};
            for(String[] a:arcane)arr.put(ingredient(a[0],a[1],"pesquisa,boato,aventura",0,"magico",5,"Material inventado para fantasia; nunca corresponde a substancia real."));
        }catch(Exception e){throw new IllegalStateException("Catalogo de ingredientes invalido",e);}
        return arr;
    }
    public static JSONObject ingredientById(String id){
        JSONArray all=ingredients();for(int i=0;i<all.length();i++){JSONObject x=all.optJSONObject(i);if(x!=null&&x.optString("id").equals(id))return x;}return new JSONObject();
    }
    private static JSONObject recipe(String id,String name,String ingredients,String kind,int tier,int dc,int minutes,int servings,String effect,int value,int duration,String origin,String description)throws Exception{
        JSONObject r=new JSONObject().put("id",id).put("name",name).put("type",kind).put("tier",tier)
          .put("dc",dc).put("minutes",minutes).put("servings",servings).put("effect_key",effect)
          .put("effect_value",value).put("effect_minutes",duration).put("origin",origin)
          .put("description",description).put("source","braseiro_original").put("original_rule",true);
        JSONObject costs=new JSONObject();for(String raw:ingredients.split(";")){String[] pair=raw.split(":");if(pair.length==2)costs.put(pair[0],Integer.parseInt(pair[1]));}
        return r.put("ingredients",costs).put("warm_meal",!kind.equals("isca"));
    }
    public static JSONArray recipes(){
        JSONArray out=new JSONArray();
        try{
            // 24 x 5 = 120 receitas cotidianas; nutrientes sao distintos de poderes.
            String[] methods={"Ensopado","Caldo","Assado","Torta rustica","Refeicao de viagem"};
            int m=0;
            for(String[] c:COMMON){
                for(int j=0;j<methods.length;j++){
                    String ingredients=c[0]+":1"+(c[0].equals("agua")?"":";agua:1");
                    if(j==0&&!c[0].equals("cebola"))ingredients+=";cebola:1";
                    if(j==3&&!c[0].equals("farinha"))ingredients+=";farinha:1";
                    if(j==4&&!c[0].equals("sal"))ingredients+=";sal:1";
                    String name=methods[j]+" de "+c[1].toLowerCase(Locale.ROOT);
                    out.put(recipe("com_"+c[0]+"_"+j,name,ingredients,"cotidiana",0,
                        j==3?10:8,20+j*10,j==4?2:4,"alimentacao",0,0,
                        "mercados e lares","Sacia a fome, oferece abrigo social e permite uma conversa a mesa; efeitos magicos: nenhum."));
                    m++;
                }
            }
            // 24 x 7 = 168 receitas de criatura, com variacao de tecnica.
            String[] prep={"Ensopado","Espetinhos","Caldo de ossos","Defumado","Assado","Torta de campanha","Sopa espessa"};
            String[] effects={"pv_temporarios","sobrevivencia","resistencia_frio","percepcao","agilidade","vigor","concentracao"};
            for(String[] b:MONSTERS){
                int level=Integer.parseInt(b[3]);
                for(int j=0;j<prep.length;j++){
                    String e=effects[j];
                    int cap=Math.min(5,1+(level/5));
                    String materials="carne_"+b[0]+":1;sal:1;agua:1";
                    if(j==0||j==6)materials+=";batata:1;cebola:1";
                    if(j==5)materials+=";farinha:1;ovos:1";
                    if(j==3)materials+=";ervas:1";
                    out.put(recipe("mon_"+b[0]+"_"+j,prep[j]+" de "+b[1],
                        materials,"monstruosa",level,Math.min(21,10+level+j/3),35+j*12,3,
                        e,cap,j==0?60:Math.min(120,30+level*5),
                        b[2],"Efeito opcional somente depois da identificacao, preparo e consumo; um beneficio de refeicao por descanso longo."));
                }
            }
            // 12 iscas: preparacoes do mundo ficticio, nunca ingredientes reais para sedacao.
            for(String[] b:BAITS){
                JSONObject item=recipe(b[0],b[1],b[2],"isca",Integer.parseInt(b[4]),Integer.parseInt(b[5]),
                    25,2,"isca_"+b[3],0,b[3].equals("chamar")?60:1,"tradicao e boatos",b[7]);
                item.put("diet",b[6]).put("requires_consumption",true).put("requires_save",b[3].equals("sonolencia")||b[3].equals("tontura")||b[3].equals("lentidao"));
                out.put(item);
            }
        }catch(Exception e){throw new IllegalStateException("Catalogo de receitas invalido",e);}
        return out;
    }
    public static JSONObject recipeById(String id){
        JSONArray all=recipes();for(int i=0;i<all.length();i++){JSONObject r=all.optJSONObject(i);if(r!=null&&r.optString("id").equals(id))return r;}return new JSONObject();
    }
    public static int catalogSize(){return 24*5+24*7+BAITS.length;}
    public static JSONArray starterIds(){
        return new JSONArray().put("com_batata_0").put("com_cebola_1").put("com_nabo_0")
            .put("com_cenoura_0").put("com_feijao_0").put("com_cevada_1")
            .put("com_ervas_1").put("com_farinha_3").put("isca_mel").put("isca_tuberculo");
    }
}
