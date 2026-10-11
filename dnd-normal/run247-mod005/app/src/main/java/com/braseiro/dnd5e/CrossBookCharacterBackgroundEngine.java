package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.*;

/**
 * RUN133 - antecedentes reutilizáveis de Strixhaven e Ravnica.
 *
 * Regras determinísticas são aplicadas ao estado da ficha. Escolhas exigidas
 * pelo livro nunca são sorteadas: permanecem em background_choice_pending e,
 * para Strixhaven Initiate, feat_choice_pending.
 */
public final class CrossBookCharacterBackgroundEngine {
    private static final String STRIX="Strixhaven: A Curriculum of Chaos";
    private static final String RAVNICA="Guildmasters' Guide to Ravnica";

    private static final class Def {
        final String name,source,skills,tools,languages,equipment,feature,spells,guild,college; final int page;
        Def(String n,String s,int p,String sk,String t,String l,String e,String f,String sp,String g,String c){name=n;source=s;page=p;skills=sk;tools=t;languages=l;equipment=e;feature=f;spells=sp;guild=g;college=c;}
    }
    private static final LinkedHashMap<String,Def> B=new LinkedHashMap<String,Def>();
    static {
        add(new Def("Estudante de Lorehold",STRIX,32,"História|Religião","","2 idiomas à escolha","Tinta preta; caneta; martelo; lanterna coberta; pederneira; tomo de história; uniforme escolar; 15 po","Lorehold Initiate: recebe Strixhaven Initiate e deve escolher Lorehold","1º Compreender Idiomas, Identificar | 2º Borrowed Knowledge, Localizar Objeto | 3º Falar com os Mortos, Guardiões Espirituais | 4º Olho Arcano, Moldar Rochas | 5º Coluna de Chamas, Lendas e Histórias","","Lorehold"));
        add(new Def("Estudante de Prismari",STRIX,32,"Acrobacia|Atuação","1 instrumento musical OU 1 ferramenta de artesão","1 idioma à escolha","Tinta preta; caneta; ferramenta de artesão OU instrumento musical; uniforme escolar; 10 po","Prismari Initiate: recebe Strixhaven Initiate e deve escolher Prismari","1º Orbe Cromático, Onda Trovejante | 2º Esfera Flamejante, Kinetic Jaunt | 3º Velocidade, Caminhar na Água | 4º Movimentação Livre, Muralha de Fogo | 5º Cone de Frio, Conjurar Elemental","","Prismari"));
        add(new Def("Estudante de Quandrix",STRIX,33,"Arcanismo|Natureza","1 ferramenta de artesão","1 idioma à escolha","Tinta preta; caneta; ábaco; livro de teoria arcana; uniforme escolar; 15 po","Quandrix Initiate: recebe Strixhaven Initiate e deve escolher Quandrix","1º Constrição, Raio Guiador | 2º Aumentar/Reduzir, Vortex Warp | 3º Aura de Vitalidade, Velocidade | 4º Controlar Água, Movimentação Livre | 5º Círculo de Poder, Criar Passagem","","Quandrix"));
        add(new Def("Estudante de Silverquill",STRIX,35,"Intimidação|Persuasão","","2 idiomas à escolha","Tinta preta; caneta; livro de poesia; uniforme escolar; 15 po","Silverquill Initiate: recebe Strixhaven Initiate e deve escolher Silverquill","1º Sussurros Dissonantes, Silvery Barbs | 2º Acalmar Emoções, Escuridão | 3º Sinal de Esperança, Luz do Dia | 4º Compulsão, Confusão | 5º Dominar Pessoa, Ligação Telepática de Rary","","Silverquill"));
        add(new Def("Estudante de Witherbloom",STRIX,36,"Natureza|Sobrevivência","Kit de Herbalismo","1 idioma à escolha","Tinta preta; caneta; livro de identificação de plantas; panela de ferro; kit de herbalismo; uniforme escolar; 15 po","Witherbloom Initiate: recebe Strixhaven Initiate e deve escolher Witherbloom","1º Curar Ferimentos, Infligir Ferimentos | 2º Restauração Menor, Wither and Bloom | 3º Revivificar, Toque Vampírico | 4º Malogro, Proteção contra a Morte | 5º Concha Antivida, Restauração Maior","","Witherbloom"));

        add(new Def("Funcionário Azorius",RAVNICA,33,"Percepção|Intimidação","","2 idiomas à escolha","Insígnia Azorius; pergaminho com lei importante; tinta azul; caneta; roupas finas; 10 po","Autoridade Legal: a insígnia confere autoridade reconhecida para fazer cumprir a lei e obter audiências; abuso pode gerar sanções","Truques Amizade, Mensagem | 1º Comando, Golpe Constritor | 2º Tranca Arcana, Acalmar Emoções, Imobilizar Pessoa | 3º Clarividência, Contramágica | 4º Compulsão, Adivinhação | 5º Dominar Pessoa","Azorius",""));
        add(new Def("Legionário Boros",RAVNICA,40,"Atletismo|Intimidação","1 conjunto de jogos","1 entre Celestial, Dracônico, Goblin ou Minotauro","Insígnia Boros; pena de anjo; fragmento de estandarte; roupas comuns; 2 po","Estação da Legião: requisita equipamento simples temporário; acesso a guarnições para descanso seguro e médicos; salário de 1 po por semana","Truques Raio de Fogo, Chama Sagrada | 1º Raio Guiador, Heroísmo | 2º Ajuda, Raio Ardente | 3º Sinal de Esperança, Destruição Cegante | 4º Proteção contra a Morte, Muralha de Fogo | 5º Coluna de Chamas","Boros",""));
        add(new Def("Operativo Dimir",RAVNICA,46,"Enganação|Furtividade","Kit de Disfarce","1 idioma à escolha","Insígnia Dimir; 3 facas pequenas; roupas comuns escuras; equipamento inicial do antecedente da guilda secundária","Identidade Falsa: mantém identidade/documentação/disfarces como membro de uma guilda secundária","Truques Codificar Pensamentos, Mãos Mágicas | 1º Disfarçar-se, Sono | 2º Detectar Pensamentos, Passos sem Pegadas | 3º Forma Gasosa, Mesclar-se às Rochas, Dificultar Detecção | 4º Olho Arcano, Movimentação Livre | 5º Modificar Memória","Dimir",""));
        add(new Def("Agente Golgari",RAVNICA,53,"Natureza|Sobrevivência","Kit de Veneno","1 entre Élfico, Gigante ou Kraul","Insígnia Golgari; kit de veneno; besouro OU aranha de estimação; roupas comuns; 10 po em moedas variadas","Caminhos Subterrâneos: fora de combate, você e companheiros guiados viajam entre locais da cidade ao dobro da velocidade normal","Truques Globos de Luz, Estabilizar | 1º Constrição, Raio Adoecente | 2º Proteção contra Veneno, Raio do Enfraquecimento, Patas de Aranha | 3º Animar Mortos, Ampliar Plantas | 4º Inseto Gigante, Vinha Agarradora | 5º Névoa Mortal, Praga de Insetos","Golgari",""));
        add(new Def("Anarquista Gruul",RAVNICA,60,"Adestrar Animais|Atletismo","Kit de Herbalismo","1 entre Dracônico, Gigante, Goblin ou Silvestre","Insígnia Gruul; armadilha de caça; kit de herbalismo; crânio de javali; capa de pele; roupas de viajante; 10 po","Refúgio do Cinturão de Destroços: encontra esconderijo/descanso, comida e água em áreas abandonadas para si e até 5 pessoas por dia","Truques Criar Chamas, Raio de Fogo | 1º Duelo Compelido, Falar com Animais, Onda Trovejante | 2º Despedaçar, Sentido Bestial | 3º Conjurar Animais, Conjurar Rajada | 4º Dominar Besta, Pele de Pedra | 5º Onda Destrutiva","Gruul",""));
        add(new Def("Engenheiro Izzet",RAVNICA,66,"Arcanismo|Investigação","1 ferramenta de artesão","1 entre Dracônico, Goblin ou Vedalkeano","Insígnia Izzet; ferramenta de artesão; restos de experiência fracassada; martelo; bloco de polias; roupas comuns; 5 po","Infraestrutura Urbana: conhece estrutura de edifícios e pode localizar plantas, pontos de entrada, fraquezas e espaços secretos quando disponíveis","Truques Criar Chamas, Toque Chocante | 1º Raio do Caos, Criar ou Destruir Água, Servo Invisível | 2º Aquecer Metal, Truque da Corda | 3º Convocar Relâmpago, Arma Elemental, Glifo de Vigilância | 4º Conjurar Elementais Menores, Adivinhação, Esfera Resiliente de Otiluke | 5º Animar Objetos, Conjurar Elemental","Izzet",""));
        add(new Def("Representante Orzhov",RAVNICA,72,"Intimidação|Religião","","2 idiomas à escolha","Insígnia Orzhov; corrente de 30 cm feita de 10 moedas de ouro; vestimentas; roupas finas; 1 pp","Alavancagem: pode exigir ajuda razoável de indivíduos abaixo de você na hierarquia, sujeita à disponibilidade e decisão do Mestre","Truques Amizade, Orientação | 1º Comando, Escrita Ilusória | 2º Cativar, Raio do Enfraquecimento, Zona da Verdade | 3º Rogar Maldição, Falar com os Mortos, Guardiões Espirituais | 4º Malogro, Proteção contra a Morte, Arca Secreta de Leomund | 5º Missão","Orzhov",""));
        add(new Def("Cultista Rakdos",RAVNICA,79,"Acrobacia|Atuação","1 instrumento musical","1 entre Abissal ou Gigante","Insígnia Rakdos; instrumento musical; fantasia; lanterna coberta de ferro; corrente pontiaguda; caixa de pólvora; 10 tochas; roupas comuns; 10 po; vinho","Reputação Temerosa: pessoas reconhecem sua filiação e tendem a evitar provocá-lo; pequenas infrações podem passar sem denúncia se nenhuma autoridade testemunhar","Truques Raio de Fogo, Zombaria Viciosa | 1º Mãos Flamejantes, Repreensão Infernal, Sussurros Dissonantes | 2º Coroa da Loucura, Cativar, Esfera Flamejante | 3º Medo, Velocidade | 4º Confusão, Muralha de Fogo | 5º Dominar Pessoa","Rakdos",""));
        add(new Def("Iniciado Selesnya",RAVNICA,86,"Natureza|Persuasão","1 ferramenta de artesão OU 1 instrumento musical","1 entre Élfico, Loxodonte ou Silvestre","Insígnia Selesnya; kit de medicina; mantos; roupas comuns; 5 po","Abrigo do Conclave: abrigo e ajuda em enclaves Selesnya; cura/cuidados gratuitos, mas componentes materiais de magia continuam a cargo do personagem","Truques Druidismo, Amizade | 1º Amizade Animal, Enfeitiçar Pessoa | 2º Mensageiro Animal, Acalmar Emoções, Ajuda, Vínculo Protetor | 3º Ampliar Plantas, Falar com Plantas | 4º Aura de Vida, Conjurar Elementais Menores | 5º Comunhão com a Natureza, Despertar","Selesnya",""));
        add(new Def("Cientista Simic",RAVNICA,93,"Arcanismo|Medicina","","2 idiomas à escolha","Insígnia Simic; roupas de plebeu; caderno de pesquisa; caneta; tinta de lula; óleo; ácido; escamas de peixe; alga; ferrão de água-viva; limo não identificado; 10 po","Pesquisador: se não sabe um fato mágico/científico, sabe onde ou com quem procurar; obter a informação ainda pode exigir acesso, favor, suborno ou aventura","Truques Espirro Ácido, Druidismo | 1º Detectar Veneno e Doença, Recuo Acelerado, Salto | 2º Alterar-se, Aprimorar Atributo, Aumentar/Reduzir | 3º Forma Gasosa, Respirar na Água, Muralha de Vento | 4º Metamorfose, Movimentação Livre | 5º Criação","Simic",""));
    }

    private CrossBookCharacterBackgroundEngine(){}
    private static void add(Def d){B.put(norm(d.name),d);}
    public static JSONArray allBackgrounds(){JSONArray a=new JSONArray();for(Def d:B.values())a.put(d.name);return a;}
    public static boolean isKnownBackground(String bg){return B.containsKey(norm(bg));}
    public static JSONObject lookup(String bg){JSONObject o=new JSONObject();try{Def d=B.get(norm(bg));if(d==null)return o.put("known",false);return o.put("known",true).put("background",d.name).put("source",d.source).put("page",d.page).put("skills",d.skills).put("tools",d.tools).put("languages",d.languages).put("feature",d.feature).put("expanded_spell_list",d.spells).put("guild",d.guild).put("college",d.college).put("mechanics_compiled",true);}catch(Exception e){return o;}}

    public static JSONObject applyBackgroundMechanics(JSONObject o){
        if(o==null)return new JSONObject();Def d=B.get(norm(o.optString("background")));if(d==null)return o;
        try{
            o.put("background_source",d.source).put("background_source_page",d.page).put("background_mechanics_runtime",true);
            for(String sk:d.skills.split("\\|"))if(sk.trim().length()>0)grantSkill(o,sk.trim());
            if(d.tools.length()>0){if(d.tools.contains(" OU "))pending(o,"Ferramenta: escolha "+d.tools);else if(d.tools.startsWith("1 "))pending(o,"Ferramenta: escolha "+d.tools.substring(2));else appendToken(o,"tools",d.tools);}
            applyLanguageRule(o,d.languages);
            appendInventory(o,d.equipment);appendFeature(o,"Antecedente — "+d.feature);o.put("background_feature",d.feature);
            if(d.spells.length()>0)o.put("expanded_spell_list",d.spells);
            if(d.guild.length()>0){o.put("guild",d.guild).put("guild_spell_list_available",true);applyGuildRuntimeFlags(o,d.guild);}
            if(d.college.length()>0){
                o.put("strixhaven_college",d.college);appendToken(o,"granted_feats","Strixhaven Initiate ("+d.college+")");
                JSONArray fp=o.optJSONArray("feat_choice_pending");if(fp==null)fp=new JSONArray();putUnique(fp,"Strixhaven Initiate: escolha 2 truques permitidos por "+d.college);putUnique(fp,"Strixhaven Initiate: escolha 1 magia de 1º círculo permitida por "+d.college);putUnique(fp,"Strixhaven Initiate: escolha atributo de conjuração entre Inteligência, Sabedoria ou Carisma");o.put("feat_choice_pending",fp).put("feat_choices_complete",false).put("strixhaven_initiate_college_locked",d.college);
            }
            JSONArray bp=o.optJSONArray("background_choice_pending");o.put("background_choices_complete",bp==null||bp.length()==0);
        }catch(Exception ignored){}
        return o;
    }

    private static void applyGuildRuntimeFlags(JSONObject o,String guild)throws Exception{
        String g=norm(guild);
        if(g.equals("azorius"))o.put("legal_authority",true);
        else if(g.equals("boros"))o.put("boros_garrison_access",true).put("boros_weekly_salary_gp",1);
        else if(g.equals("dimir")){o.put("false_identity_available",true);pending(o,"Dimir: escolha guilda secundária/identidade falsa; não sortear");}
        else if(g.equals("golgari"))o.put("ravnica_city_travel_multiplier",2);
        else if(g.equals("gruul"))o.put("gruul_refuge_party_capacity",6);
        else if(g.equals("izzet"))o.put("izzet_infrastructure_knowledge",true);
        else if(g.equals("orzhov"))o.put("orzhov_leverage",true);
        else if(g.equals("rakdos"))o.put("rakdos_fearsome_reputation",true);
        else if(g.equals("selesnya"))o.put("selesnya_shelter_access",true).put("selesnya_free_care_components_required",true);
        else if(g.equals("simic"))o.put("simic_researcher_network",true);
    }
    private static void applyLanguageRule(JSONObject o,String rule)throws Exception{
        if(rule==null||rule.trim().isEmpty())return;
        String r=norm(rule);
        if(r.startsWith("1 idioma")||r.startsWith("2 idiomas")||r.startsWith("1 entre")){pending(o,"Idioma: "+rule);return;}
        appendToken(o,"languages",rule);
    }
    private static void grantSkill(JSONObject o,String skill)throws Exception{
        String raw=o.optString("skills","");String[] parts=raw.split("\\s*•\\s*");StringBuilder b=new StringBuilder();boolean found=false;String target=norm(skill);int bonus=skillMod(o,skill)+pb(o);
        for(String p:parts){if(p.trim().isEmpty())continue;String out=p.trim();if(norm(out).startsWith(target+" ")||norm(out).equals(target)){out=skill+" "+signed(bonus)+" ★";found=true;}if(b.length()>0)b.append(" • ");b.append(out);}if(!found){if(b.length()>0)b.append(" • ");b.append(skill).append(' ').append(signed(bonus)).append(" ★");}o.put("skills",b.toString());appendToken(o,"background_skill_proficiencies",skill);
    }
    private static int skillMod(JSONObject o,String skill){String n=norm(skill);int score;if(n.equals("atletismo"))score=o.optInt("str",10);else if(n.equals("acrobacia")||n.equals("furtividade")||n.equals("prestidigitacao"))score=o.optInt("dex",10);else if(n.equals("arcanismo")||n.equals("historia")||n.equals("investigacao")||n.equals("natureza")||n.equals("religiao"))score=o.optInt("int_score",10);else if(n.equals("adestrar animais")||n.equals("intuicao")||n.equals("medicina")||n.equals("percepcao")||n.equals("sobrevivencia"))score=o.optInt("wis",10);else score=o.optInt("cha",10);return mod(score);}
    private static int mod(int score){return (int)Math.floor((score-10)/2.0);} private static int pb(JSONObject o){int l=Math.max(1,Math.min(20,o.optInt("level",1)));return 2+(l-1)/4;} private static String signed(int n){return n>=0?"+"+n:String.valueOf(n);}
    private static void appendFeature(JSONObject o,String feature)throws Exception{String f=o.optString("features","");if(!norm(f).contains(norm(feature)))o.put("features",f+(f.trim().isEmpty()?"":"\n")+feature+".");}
    private static void appendInventory(JSONObject o,String value)throws Exception{if(value==null||value.trim().isEmpty())return;String raw=o.optString("inventory","");if(!norm(raw).contains(norm(value)))o.put("inventory",raw+(raw.trim().isEmpty()?"":" • ")+value);}
    private static void pending(JSONObject o,String choice)throws Exception{JSONArray a=o.optJSONArray("background_choice_pending");if(a==null)a=new JSONArray();putUnique(a,choice);o.put("background_choice_pending",a).put("background_choices_complete",false);}
    private static void putUnique(JSONArray a,String v){for(int i=0;i<a.length();i++)if(norm(v).equals(norm(a.optString(i))))return;a.put(v);}
    private static void appendToken(JSONObject o,String key,String value)throws Exception{String raw=o.optString(key,"");String nv=norm(value);for(String p:raw.split("[|,;•]"))if(norm(p).equals(nv))return;o.put(key,raw.trim().isEmpty()?value:raw+"|"+value);}
    private static String norm(String s){String x=Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.replaceAll("[^a-z0-9]+"," ").trim();}
}
