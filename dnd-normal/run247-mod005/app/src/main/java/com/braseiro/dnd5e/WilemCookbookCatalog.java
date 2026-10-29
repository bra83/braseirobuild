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
    // Cada familia de criatura tem um corte, procedimento e risco anatomico proprios.
    // Orientacoes ficticias de jogo: nao sao instrucoes para manipular animais reais.
    private static final String[][] ANATOMY={
        {"javali","Lombo de javali","Separar musculatura longe das presas; descartar tecido lesionado"},
        {"lobo","Corte dorsal de lobo terrivel","Separar nervuras do dorso sem tocar saliva ou presas"},
        {"aranha","Musculatura de patas de aranha","Isolar bolsas glandulares e fiandeiras antes da retirada"},
        {"crocodilo","Musculo da cauda de crocodilo","Evitar placas e espinhos osseos; conferir interior da cauda"},
        {"sapo","Musculatura da coxa de sapo","Isolar pele e secrecoes sem contato com o caldo"},
        {"escorpiao","Musculatura interna da pinça","Manter ferrao e glandula do veneno completamente isolados"},
        {"coruja_urso","Musculatura do peito da coruja-urso","Separar tendons grossos e plumagem em varias camadas"},
        {"ankheg","Corte abdominal de ankheg","Nao misturar secrecoes acidas da mandibula ao alimento"},
        {"bico_machado","Musculatura do peito do bico de machado","Retirar ossos finos, plumas e cartilagem das garras"},
        {"caranguejo","Musculatura das pinças do caranguejo","Abrir carapaca sem danificar a porcao interna"},
        {"cobra","Corte dorsal de cobra gigante","Afastar presas, glandulas e couro antes do corte"},
        {"lagarto","Musculatura da cauda de lagarto","Verificar espinhos e desprendimento da cauda"},
        {"morcego","Musculatura peitoral de morcego","Separar membranas de voo e nao recolher tecido doente"},
        {"lagosta","Musculatura da cauda de lagosta","Abrir carapaca e retirar partes escuras da cavidade"},
        {"urso","Corte do ombro do urso da neblina","Descartar tecido marcado por nevoa sobrenatural desconhecida"},
        {"bulette","Musculatura profunda de bulette","Romper somente placas nao mineralizadas; evitar laminas dorsais"},
        {"chuul","Musculatura de pinça de chuul","Isolar tentaculos e secrecoes paralisantes em saco distinto"},
        {"basilisco","Corte de cauda de basilisco","Exigir identificacao arcana antes de remover tecido petrificado"},
        {"quimera","Musculatura da anca de quimera","Identificar a origem anatomica; descartar glandulas do fogo"},
        {"verme","Fibra muscular do verme purpura","Isolar ferrão e secrecoes do trato subterraneo"},
        {"salamandra","Musculatura do tronco da salamandra","Usar protecao magica contra calor antes da inspecao"},
        {"elemental_agua","Fibra feerica do peixe das aguas","Conservar a amostra em agua da origem e testar estabilidade"},
        {"hidra","Musculatura de pescoco de hidra","Isolar tecido em regeneracao e descartar partes instaveis"},
        {"serpente_mar","Corte dorsal de serpente marinha","Separar placas salinas, espinhos e glandulas costeiras"}
    };
    private static JSONObject anatomy(String id)throws Exception{
        for(String[] row:ANATOMY)if(("carne_"+row[0]).equals(id))
            return new JSONObject().put("cut",row[1]).put("warning",row[2])
                .put("required","Identificar especie, confirmar derrota e realizar um teste fisico unico por corpo.");
        return new JSONObject();
    }
    private static JSONObject ingredient(String id,String name,String sources,int copper,String category,int tier,String note)throws Exception{
        return new JSONObject().put("id",id).put("name",name).put("source_locations",sources)
            .put("suggested_price_cp",copper).put("category",category).put("tier",tier)
            .put("note",note).put("source","braseiro_original");
    }
    public static JSONArray ingredients(){
        JSONArray arr=new JSONArray();
        try{
            for(String[] c:COMMON)arr.put(ingredient(c[0],c[1],c[2],Integer.parseInt(c[3]),c[4],0,"Comum; oferta sujeita a estacao e contexto local."));
            for(String[] m:MONSTERS){
                String id="carne_"+m[0];
                arr.put(ingredient(id,"Parte culinaria de "+m[1],m[2],0,"monstro",
                    Integer.parseInt(m[3]),"Exige colheita, identificacao e respeito a ecologia.")
                    .put("anatomy",anatomy(id)));
            }
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
        // Procedimentos concretos por familia: cozinhar nao e um simples botao de buff.
        JSONArray steps=new JSONArray();
        String method=name.toLowerCase(Locale.ROOT);
        if(kind.equals("isca")){
            steps.put("Verificar a dieta e o comportamento da criatura-alvo antes de escolher o alimento.");
            steps.put("Separar apenas ingredientes reconhecidos no inventario, mantendo componentes arcanos isolados.");
            steps.put("Preparar no caldeirao ou junto ao fogo sem misturar o alimento com o da companhia.");
            steps.put("Posicionar a isca no cenario. O Mestre resolve atracao, ingestao, imunidades e salvaguarda.");
        }else if(method.contains("assado")||method.contains("espetinh")||method.contains("dourado")){
            steps.put("Examinar cada ingrediente, separar partes comestiveis e lavar vegetais com agua potavel.");
            steps.put("Cortar em porcoes regulares; temperar com os ingredientes efetivamente disponiveis.");
            steps.put("Aquecer o fogo e assar ou dourar, virando as porcoes ate estarem cozidas.");
            steps.put("Dividir o rendimento, servir enquanto quente e registrar quem compartilhou a refeicao.");
        }else if(method.contains("torta")||method.contains("pao")||method.contains("bolinho")){
            steps.put("Peneirar a farinha, juntar os ingredientes indicados e formar uma massa uniforme.");
            steps.put("Preparar o recheio separadamente; usar somente partes de criatura comprovadamente comestiveis.");
            steps.put("Fechar a massa e assar sobre calor constante, observando tempo e seguranca do preparo.");
            steps.put("Deixar assentar, cortar o numero de porcoes registrado e compartilhar.");
        }else if(method.contains("defumado")||method.contains("conserva")||method.contains("racao seca")){
            steps.put("Selecionar ingredientes frescos e verificar se o sal e a fonte de calor bastam.");
            steps.put("Cortar em pedacos pequenos, preparar as partes comestiveis e temperar.");
            steps.put("Desidratar, cozinhar ou defumar conforme a tecnica descrita pelo Mestre, sem presumir conservacao indefinida.");
            steps.put("Embalar com data e origem; conferir validade ao servir.");
        }else if(method.contains("compota")||method.contains("doce")||method.contains("calda")){
            steps.put("Lavar as frutas ou ingredientes doces e retirar partes improprias.");
            steps.put("Aquecer lentamente com a agua disponivel, mexendo para nao queimar.");
            steps.put("Reduzir ate alcancar a consistencia desejada, usando mel apenas se estiver entre os ingredientes.");
            steps.put("Servir ou armazenar por curto periodo, registrando quantas porcoes renderam.");
        }else if(method.contains("infusao")||method.contains("cha")){
            steps.put("Conferir identificacao dos vegetais e procedencia da agua; nao usar fungos desconhecidos.");
            steps.put("Aquecer agua potavel ate ferver e retirar da chama.");
            steps.put("Deixar as folhas ou ervas em infusao, coar e provar somente depois de verificar a seguranca.");
            steps.put("Servir quente, oferecendo hospitalidade e anotando reacoes.");
        }else{
            steps.put("Identificar partes comestiveis; lavar os vegetais e separar os ingredientes conforme a lista.");
            steps.put("Cortar raizes, carnes ou fungos em pedacos regulares; separar os temperos.");
            steps.put("Aquecer o caldeirao, cozinhar os ingredientes no caldo ate ficarem prontos e ajustar a consistencia.");
            steps.put("Dividir em porcoes e servir quente. Alimentacao e convivencia valem independentemente de efeitos especiais.");
        }
        r.put("steps",steps).put("ingredients",costs).put("warm_meal",!kind.equals("isca"));
        return r;
    }
    /** Atualiza a quantidade do ingrediente existente em vez de duplicar a variacao. */
    private static String extra(String base,String id){
        StringBuilder out=new StringBuilder();boolean found=false;
        for(String pair:base.split(";")){
            String[] p=pair.split(":");if(p.length!=2)throw new IllegalArgumentException("Ingrediente invalido");
            if(out.length()>0)out.append(';');
            if(id.equals(p[0])){out.append(id).append(':').append(Integer.parseInt(p[1])+1);found=true;}
            else out.append(pair);
        }
        if(!found)out.append(';').append(id).append(":1");
        return out.toString();
    }
    public static JSONArray recipes(){
        JSONArray out=new JSONArray();
        try{
            // 24 pratos base x cinco variacoes de ingredientes = 120 receitas
            // de cozinha caseira plausivel, com insumos facilmente encontrados.
            String[][] bases={
                {"Ensopado de batatas","batata:1;cebola:1;agua:1", "35"},
                {"Sopa de cebola","cebola:2;agua:1;sal:1", "30"},
                {"Creme rustico de nabo","nabo:2;agua:1;sal:1", "35"},
                {"Caldo de cenoura","cenoura:2;agua:1", "30"},
                {"Repolho refogado","repolho:2;oleo:1", "25"},
                {"Alho assado com pao","alho:2;farinha:1;agua:1", "35"},
                {"Sopa de cogumelos","cogumelo:2;agua:1", "35"},
                {"Feijao de panela","feijao:2;agua:1;sal:1", "50"},
                {"Lentilhas guisadas","lentilha:2;agua:1;cebola:1", "40"},
                {"Mingau de cevada","cevada:2;agua:1", "30"},
                {"Arroz soltinho","arroz:2;agua:1", "30"},
                {"Abobora assada","abobora:2;oleo:1", "35"},
                {"Beterrabas cozidas","beterraba:2;agua:1", "35"},
                {"Macas ao forno","maça:2;agua:1", "25"},
                {"Pao de mel","mel:1;farinha:2;agua:1", "45"},
                {"Omelete da fogueira","ovos:2;oleo:1", "20"},
                {"Queijo tostado com pao","queijo:2;farinha:1;agua:1", "25"},
                {"Peixe na brasa","peixe:2;sal:1", "35"},
                {"Assado de caca","carne_caca:2;sal:1", "45"},
                {"Cha de ervas","ervas:1;agua:1", "15"},
                {"Pao de frigideira","farinha:2;agua:1;sal:1", "30"},
                {"Raizes conservadas em sal","nabo:1;cenoura:1;sal:1", "25"},
                {"Caldo de legumes em agua potavel","agua:2;batata:1;cebola:1", "30"},
                {"Legumes dourados no oleo","oleo:1;cenoura:1;batata:1", "25"}
            };
            String[] versions={""," com ervas"," com alho"," com cebola"," com queijo"};
            String[] addons={"","ervas","alho","cebola","queijo"};
            for(int i=0;i<COMMON.length;i++){
                String[] c=COMMON[i],base=bases[i];
                for(int j=0;j<5;j++){
                    String name=base[0]+versions[j];
                    String ingredients=j==0?base[1]:extra(base[1],addons[j]);
                    // Doces e infusoes recebem variantes proprias e comestiveis.
                    if("maça".equals(c[0])){
                        String[] labels={""," com mel"," com queijo"," com crosta de farinha"," com cevada tostada"};
                        String[] adds={"","mel","queijo","farinha","cevada"};
                        name=base[0]+labels[j];ingredients=j==0?base[1]:extra(base[1],adds[j]);
                    }else if("mel".equals(c[0])){
                        String[] labels={""," com maca"," com ovos"," com queijo"," com cevada"};
                        String[] adds={"","maça","ovos","queijo","cevada"};
                        name=base[0]+labels[j];ingredients=j==0?base[1]:extra(base[1],adds[j]);
                    }else if("ervas".equals(c[0])){
                        String[] labels={""," com mel"," com maca"," de infusao forte"," com maca e mel"};
                        name=base[0]+labels[j];
                        if(j==0)ingredients=base[1];
                        else if(j==1)ingredients=extra(base[1],"mel");
                        else if(j==2)ingredients=extra(base[1],"maça");
                        else if(j==3)ingredients=extra(base[1],"ervas");
                        else ingredients=extra(extra(base[1],"maça"),"mel");
                    }
                    out.put(recipe("com_"+c[0]+"_"+j,name,ingredients,"cotidiana",0,
                        j>=3?10:8,Integer.parseInt(base[2])+j*5,4,"alimentacao",0,0,
                        "hortas, mercados e lares","Comida de verdade: saciedade, convivencia e descanso. Sem bonus magico."));
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
