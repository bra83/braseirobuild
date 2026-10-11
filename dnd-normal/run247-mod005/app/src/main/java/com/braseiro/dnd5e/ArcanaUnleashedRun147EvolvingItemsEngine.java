package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.Locale;

/** RUN147 — efeitos específicos dos nove grupos de itens evolutivos de Arcana Unleashed. */
public final class ArcanaUnleashedRun147EvolvingItemsEngine {
    public static final String SOURCE="Arcana Unleashed";
    public static final String UI_LANGUAGE="pt-BR";

    private static final String[][] STAGES={
        {"Budding Blossom Rod","Bastão da Floração Brotante","Blossom Rod","Incomum"},
        {"Flowering Blossom Rod","Bastão da Floração Florido","Blossom Rod","Raro"},
        {"Somniferous Blossom Rod","Bastão da Floração Sonífero","Blossom Rod","Muito Raro"},
        {"Blemished Idol of Good Fortunes","Ídolo Maculado das Boas Fortunas","Idol of Good Fortunes","Incomum"},
        {"Tarnished Idol of Good Fortunes","Ídolo Empanado das Boas Fortunas","Idol of Good Fortunes","Raro"},
        {"Golden Idol of Good Fortunes","Ídolo Dourado das Boas Fortunas","Idol of Good Fortunes","Muito Raro"},
        {"Three Keyholes Dagger","Adaga das Três Fechaduras","Keyholes Dagger","Incomum"},
        {"Ten Keyholes Dagger","Adaga das Dez Fechaduras","Keyholes Dagger","Raro"},
        {"Many Keyholes Dagger","Adaga das Muitas Fechaduras","Keyholes Dagger","Muito Raro"},
        {"Ominous Staff of Skulls","Cajado Ominoso de Crânios","Staff of Skulls","Incomum"},
        {"Chattering Staff of Skulls","Cajado Tagarela de Crânios","Staff of Skulls","Raro"},
        {"Pulverizing Staff of Skulls","Cajado Pulverizador de Crânios","Staff of Skulls","Muito Raro"},
        {"Barnacled Wave-Swept Weapon","Arma Crustada Moldada pelas Ondas","Wave-Swept Weapon","Incomum"},
        {"Aquatic Wave-Swept Weapon","Arma Aquática Moldada pelas Ondas","Wave-Swept Weapon","Raro"},
        {"Ascendant Wave-Swept Weapon","Arma Ascendente Moldada pelas Ondas","Wave-Swept Weapon","Muito Raro"},
        {"Studious Blade of the Guardian","Lâmina Estudiosa do Guardião","Blade of the Guardian","Raro"},
        {"Resistant Blade of the Guardian","Lâmina Resistente do Guardião","Blade of the Guardian","Muito Raro"},
        {"All-Seeing Blade of the Guardian","Lâmina Onividente do Guardião","Blade of the Guardian","Lendário"},
        {"Regal Breastplate of the Tyrant (*)","Peitoral Régio do Tirano","Breastplate of the Tyrant","Muito Raro"},
        {"Subjugating Breastplate of the Tyrant (*)","Peitoral Subjugador do Tirano","Breastplate of the Tyrant","Lendário"},
        {"Vigilant Rod of the Honed Mind","Bastão Vigilante da Mente Afiada","Rod of the Honed Mind","Raro"},
        {"Psionic Rod of the Honed Mind","Bastão Psiônico da Mente Afiada","Rod of the Honed Mind","Muito Raro"},
        {"Tranquil Rod of the Honed Mind","Bastão Tranquilo da Mente Afiada","Rod of the Honed Mind","Lendário"},
        {"Crusading Wand of Celestial Prowess","Varinha Cruzada da Proeza Celestial","Wand of Celestial Prowess","Raro"},
        {"Lunar Wand of Celestial Prowess","Varinha Lunar da Proeza Celestial","Wand of Celestial Prowess","Muito Raro"},
        {"Heliacal Wand of Celestial Prowess","Varinha Heliacal da Proeza Celestial","Wand of Celestial Prowess","Lendário"}
    };

    private ArcanaUnleashedRun147EvolvingItemsEngine(){}

    public static JSONArray allStages(){JSONArray a=new JSONArray();for(String[]s:STAGES)a.put(s[1]);return a;}
    public static boolean supports(String value){return stage(value)!=null;}

    public static JSONObject descriptor(){try{return new JSONObject()
        .put("id","run147_arcana_unleashed_evolving_items_ptbr")
        .put("source",SOURCE).put("surface_language",UI_LANGUAGE)
        .put("groups_executable",9).put("stage_variants_executable",STAGES.length)
        .put("specific_effects_complete",true).put("canonical_english_aliases",true)
        .put("auto_rolls",false).put("mechanical_book_complete_pre_adventure",true);
    }catch(Exception e){return new JSONObject();}}

    public static JSONObject coverage(){try{return new JSONObject()
        .put("groups",9).put("stage_variants",STAGES.length)
        .put("groups_executable",9).put("stage_variants_executable",STAGES.length)
        .put("pending_specific_effects",0).put("surface_language",UI_LANGUAGE)
        .put("auto_rolls",false).put("complete",true);
    }catch(Exception e){return new JSONObject();}}

    public static JSONObject install(){JSONObject out=new JSONObject();JSONArray failed=new JSONArray();int ok=0;try{
        for(String[]s:STAGES){JSONObject ev=new JSONObject().put("evidence_id","run147.au.evolving."+id(s[0]))
            .put("content_kind","magic_item").put("content_name",s[0]).put("source_book",SOURCE)
            .put("source_locator","AU cap. 5 — Evolving Magic Items / dados estruturados revisados em 2026-09-29")
            .put("revision",1).put("ui_language",UI_LANGUAGE);
            boolean reg=AuthorizedMechanicalEvidenceRegistry.registerReviewed(ev).optBoolean("registered",false);
            JSONObject mech=new JSONObject().put("effect_pipeline_complete",true)
                .put("execution_status","compiled_evolving_magic_item_pipeline")
                .put("runtime_resolver","ArcanaUnleashedRun147EvolvingItemsEngine.resolve")
                .put("passive_resolver","ArcanaUnleashedRun147EvolvingItemsEngine.applyInventoryPassives")
                .put("auto_rolls",false).put("surface_language",UI_LANGUAGE);
            if(reg&&CompiledContentMechanicsEngine.register("magic_item",s[0],mech,ev).optBoolean("registered",false))ok++;else failed.put(s[1]);
        }
        return out.put("installed",ok).put("expected",STAGES.length).put("complete",ok==STAGES.length).put("failed",failed).put("auto_rolls",false);
    }catch(Exception e){try{return out.put("installed",ok).put("expected",STAGES.length).put("complete",false).put("failed",failed);}catch(Exception ignored){return out;}}}

    public static JSONObject itemInfo(String value){String[]s=stage(value);JSONObject o=new JSONObject();try{
        if(s==null)return o.put("known",false);
        o.put("known",true).put("nome",s[1]).put("nome_canonico",s[0]).put("grupo",groupPt(s[2])).put("grupo_canonico",s[2]).put("raridade",s[3]).put("fonte",SOURCE).put("auto_rolls",false);
        JSONArray pass=new JSONArray(),act=new JSONArray();fillInfo(s[0],pass,act,o);return o.put("passivos",pass).put("acoes",act);
    }catch(Exception e){return o;}}

    public static JSONObject evolveToNext(JSONObject item,boolean confirmar){JSONObject x=copy(item);try{
        if(!confirmar)return x.put("au_evolving_resolved",false).put("au_evolving_status","aguardando_confirmacao");
        String current=x.optString("name",x.optString("nome",""));String next=nextStage(current);
        if(next.isEmpty())return x.put("au_evolving_resolved",false).put("au_evolving_status","sem_proximo_estagio");
        String[]s=stage(next);x.put("name",s[0]).put("nome",s[1]).put("raridade",s[3]).put("au_evolving_resolved",true).put("au_evolving_status","evoluido").put("surface_language",UI_LANGUAGE);
        return x;
    }catch(Exception e){return x;}}

    public static JSONObject resolve(JSONObject input,String item,String action,JSONObject payload){JSONObject state=copy(input),p=payload==null?new JSONObject():payload;String[]s=stage(item);try{
        if(s==null)return blocked(state,"item_evolutivo_desconhecido");
        String c=s[0],a=n(action);JSONObject effect=new JSONObject().put("item",s[1]).put("acao",action).put("auto_rolls",false);
        if(!actionAllowed(c,a))return blocked(state,"acao_nao_disponivel_neste_estagio");
        String cooldown=cooldown(c,a);String key=id(c)+"::"+id(actionKey(c,a));
        if(!cooldown.isEmpty()&&used(state,cooldown,key))return blocked(state,"uso_indisponivel_ate_"+cooldown);
        String req=manualRequirements(c,a);if(!req.isEmpty()&&!manualProvided(req,p))return pending(state,s[1],action,req,effect);
        buildEffect(c,a,p,effect);
        if(!cooldown.isEmpty())markUsed(state,cooldown,key);
        append(state,effect);
        return state.put("au_evolving_item_resolved",true).put("au_evolving_item_status","resolvido").put("au_evolving_item_effect",effect).put("au_evolving_item_auto_rolls",false);
    }catch(Exception e){return blocked(state,"falha_no_resolver");}}

    public static JSONObject applyInventoryPassives(JSONObject input){JSONObject s=copy(input);try{
        JSONArray ledger=s.optJSONArray("inventory_ledger"),pass=new JSONArray();int ac=0;
        for(int i=0;ledger!=null&&i<ledger.length();i++){JSONObject row=ledger.optJSONObject(i);if(row==null||row.optInt("quantity",1)<=0)continue;String name=row.optString("name",row.optString("nome",""));String[]st=stage(name);if(st==null)continue;boolean equipped=row.optBoolean("equipped",row.optBoolean("equipado",true));boolean attuned=row.optBoolean("attuned",row.optBoolean("sintonizado",true));if(!equipped)continue;String c=st[0];JSONObject q=new JSONObject().put("item",st[1]);
            if(c.equals("Tarnished Idol of Good Fortunes")||c.equals("Golden Idol of Good Fortunes")){if(attuned){q.put("bonus_ca",1);ac=Math.max(ac,1);}}
            if(c.contains("Blade of the Guardian")){if(attuned){q.put("vantagem_iniciativa",true).put("alarme_aliados_9m",true);if(c.startsWith("Resistant")||c.startsWith("All-Seeing"))q.put("resistencia_fogo",true);if(c.startsWith("All-Seeing"))q.put("visao_verdadeira_m",9);}}
            if(c.contains("Breastplate of the Tyrant")){if(attuned)q.put("resistencia_psiquico",true);}
            if(c.contains("Rod of the Honed Mind")){if(attuned){q.put("bonus_ataque_magia_bruxo",1).put("bonus_cd_magias_bruxo",1);if(c.startsWith("Vigilant")||c.startsWith("Psionic")||c.startsWith("Tranquil"))q.put("vantagem_iniciativa",true).put("vantagem_percepcao",true);if(c.startsWith("Psionic")||c.startsWith("Tranquil"))q.put("telepatia_m",9);}}
            if(c.contains("Wand of Celestial Prowess")){if(attuned)q.put("bonus_ataque_magia",1).put("ignora_meia_cobertura_ataque_magia",true);}
            if(c.startsWith("Aquatic Wave-Swept")||c.startsWith("Ascendant Wave-Swept")){if(attuned)q.put("respira_submerso",true).put("deslocamento_natacao_m",9);}
            if(c.startsWith("Ascendant Wave-Swept")&&attuned)q.put("deslocamento_voo_m",9);
            if(q.length()>1)pass.put(q);
        }
        return s.put("au_evolving_item_passives",pass).put("au_evolving_item_ac_bonus",ac);
    }catch(Exception e){return s;}}

    public static JSONObject dawn(JSONObject input){JSONObject s=copy(input);try{s.put("au_evolving_item_dawn_uses",new JSONObject()).put("au_evolving_item_dawn_reset",true);return s;}catch(Exception e){return s;}}
    public static JSONObject longRest(JSONObject input){JSONObject s=dawn(input);try{s.put("au_evolving_item_long_rest_uses",new JSONObject()).put("au_evolving_item_long_rest_reset",true);return s;}catch(Exception e){return s;}}

    private static void fillInfo(String c,JSONArray pass,JSONArray act,JSONObject o)throws Exception{
        if(c.equals("Budding Blossom Rod")){act.put("Conjurar Druidismo").put("Conjurar Crescimento de Plantas").put("Rejuvenescer plantas");}
        else if(c.equals("Flowering Blossom Rod")){act.put("Conjurar Druidismo").put("Conjurar Crescimento de Plantas").put("Rejuvenescer plantas").put("Conjurar Praga de Insetos");}
        else if(c.equals("Somniferous Blossom Rod")){act.put("Conjurar Druidismo").put("Conjurar Crescimento de Plantas").put("Rejuvenescer plantas").put("Conjurar Praga de Insetos").put("Pólen sonífero");}
        else if(c.equals("Blemished Idol of Good Fortunes")){act.put("Conjurar Augúrio");}
        else if(c.equals("Tarnished Idol of Good Fortunes")){pass.put("+1 CA enquanto sintonizado");act.put("Conjurar Augúrio").put("Converter barra ou gema em moedas");}
        else if(c.equals("Golden Idol of Good Fortunes")){pass.put("+1 CA enquanto sintonizado");act.put("Conjurar Augúrio").put("Converter barra ou gema em moedas").put("Conjurar Globo de Invulnerabilidade");}
        else if(c.equals("Three Keyholes Dagger")){pass.put("+1 em ataque e dano");act.put("Transformar arma");}
        else if(c.equals("Ten Keyholes Dagger")){pass.put("+2 em ataque e dano");act.put("Transformar arma");}
        else if(c.equals("Many Keyholes Dagger")){pass.put("+3 em ataque e dano");act.put("Transformar arma").put("Rerrolar ataque perdido");}
        else if(c.equals("Ominous Staff of Skulls")){pass.put("Vantagem em Intimidação enquanto empunhado");act.put("Conjurar Toque Arrepiante");}
        else if(c.equals("Chattering Staff of Skulls")){pass.put("Vantagem em Intimidação enquanto empunhado");act.put("Conjurar Toque Arrepiante").put("Tagarelar contra ataque");}
        else if(c.equals("Pulverizing Staff of Skulls")){pass.put("Vantagem em Intimidação enquanto empunhado");act.put("Conjurar Toque Arrepiante").put("Tagarelar contra ataque").put("Pulverizar humanoide");}
        else if(c.equals("Barnacled Wave-Swept Weapon")){pass.put("+1 em ataque e dano");act.put("Infundir salmoura");}
        else if(c.equals("Aquatic Wave-Swept Weapon")){pass.put("+2 em ataque e dano").put("Respira sob a água").put("Deslocamento de natação 9 m");act.put("Infundir salmoura");}
        else if(c.equals("Ascendant Wave-Swept Weapon")){pass.put("+3 em ataque e dano").put("Respira sob a água").put("Deslocamento de natação 9 m").put("Deslocamento de voo 9 m");act.put("Infundir salmoura");}
        else if(c.contains("Blade of the Guardian")){pass.put("Benefícios cumulativos de Arma de Alerta");if(c.startsWith("Studious"))act.put("Conhecimento estudioso");if(c.startsWith("Resistant")){act.put("Conhecimento estudioso");pass.put("Resistência a Fogo");}if(c.startsWith("All-Seeing")){act.put("Conhecimento estudioso");pass.put("Resistência a Fogo").put("Visão Verdadeira 9 m");}}
        else if(c.contains("Breastplate of the Tyrant")){pass.put("Resistência a dano Psíquico");act.put("Conjurar Presença Régia de Yolande");if(c.startsWith("Subjugating"))act.put("Conjurar Dominar Monstro");}
        else if(c.contains("Rod of the Honed Mind")){pass.put("+1 em ataques mágicos e CD das magias de Bruxo");act.put("Recuperar espaço de magia");if(c.startsWith("Vigilant")||c.startsWith("Psionic")||c.startsWith("Tranquil"))pass.put("Vantagem em Iniciativa e Percepção");if(c.startsWith("Psionic")||c.startsWith("Tranquil")){pass.put("Telepatia 9 m");act.put("Conjurar Estática Sináptica");}if(c.startsWith("Tranquil"))act.put("Conjurar Mente Vazia em si");}
        else if(c.contains("Wand of Celestial Prowess")){pass.put("+1 em ataques mágicos").put("Ignora Meia Cobertura em ataques mágicos");act.put("Conjurar Manto do Cruzado");if(c.startsWith("Lunar")||c.startsWith("Heliacal"))act.put("Conjurar Fonte de Luar").put("Conjurar Raio Lunar");if(c.startsWith("Heliacal"))act.put("Conjurar Explosão Solar");}
        o.put("sintonizacao_requerida",!c.startsWith("Blemished")&&!c.startsWith("Ominous")?true:true);
    }

    private static void buildEffect(String c,String a,JSONObject p,JSONObject e)throws Exception{
        if(a.contains("druidismo"))e.put("magia","Druidismo");
        else if(a.contains("crescimento de plantas"))e.put("magia","Crescimento de Plantas").put("modo","Supercrescimento");
        else if(a.contains("rejuvenescer"))e.put("area","cubo de 1,5 m a até 4,5 m").put("duracao_dias",p.optInt("resultado_1d4_dias"));
        else if(a.contains("praga de insetos"))e.put("magia","Praga de Insetos").put("usa_cd_conjurador",true);
        else if(a.contains("polen sonifero"))e.put("area","emanação de 9 m").put("salvamento","Constituição CD 17").put("falha","Inconsciente por 1 hora; termina com dano ou ao ser despertado").put("resultados_salvamentos",p.optJSONArray("resultados_salvamentos"));
        else if(a.contains("augurio"))e.put("magia","Augúrio").put("oraculo_falado_pelo_idolo",true);
        else if(a.contains("converter barra"))e.put("efeito","Converte barra comercial ou gema não mágica tocada em moedas de valor equivalente").put("valor_moedas",p.optInt("valor_em_po"));
        else if(a.contains("globo de invulnerabilidade"))e.put("magia","Globo de Invulnerabilidade");
        else if(a.contains("transformar arma")){e.put("forma_escolhida",p.optString("forma_arma"));if(c.equals("Three Keyholes Dagger"))e.put("formas_permitidas","Machadinha ou Maça");else if(c.equals("Ten Keyholes Dagger"))e.put("formas_permitidas","qualquer arma corpo a corpo Simples");else e.put("formas_adicionais","Machado de Batalha, Espada Longa, Rapieira, Cimitarra ou Martelo de Guerra");}
        else if(a.contains("rerrolar ataque"))e.put("novo_resultado_ataque",p.optInt("nova_rolagem_ataque")).put("deve_usar_segundo_resultado",true);
        else if(a.contains("toque arrepiante"))e.put("magia","Toque Arrepiante");
        else if(a.contains("tagarelar"))e.put("gatilho","criatura visível a até 9 m faz ataque").put("efeito","impõe Desvantagem ao ataque como Reação");
        else if(a.contains("pulverizar"))e.put("alvo","Humanoide visível").put("salvamento","Constituição CD 17").put("dano","10d8 necrótico — resultado manual").put("resultado_salvamento",p.optInt("resultado_salvamento")).put("dano_total",p.optInt("dano_total")).put("falha_extra","Caído");
        else if(a.contains("salmoura"))e.put("efeito","Transforma até um galão de líquido tocado em líquido salgado");
        else if(a.contains("conhecimento estudioso"))e.put("gatilho","falha em teste da ação Estudar").put("bonus","1d6 manual").put("resultado_1d6",p.optInt("resultado_1d6"));
        else if(a.contains("presenca regia"))e.put("magia","Presença Régia de Yolande").put("cd",17);
        else if(a.contains("dominar monstro"))e.put("magia","Dominar Monstro").put("cd",18);
        else if(a.contains("recuperar espaco"))e.put("efeito","Recupera um espaço de magia de Bruxo gasto");
        else if(a.contains("estatica sinaptica"))e.put("magia","Estática Sináptica").put("cd",17);
        else if(a.contains("mente vazia"))e.put("magia","Mente Vazia").put("alvo","somente o portador");
        else if(a.contains("manto do cruzado"))e.put("magia","Manto do Cruzado");
        else if(a.contains("fonte de luar"))e.put("magia","Fonte de Luar").put("cd",17);
        else if(a.contains("raio lunar"))e.put("magia","Raio Lunar").put("cd",17);
        else if(a.contains("explosao solar"))e.put("magia","Explosão Solar").put("cd",18);
    }

    private static boolean actionAllowed(String c,String a){JSONObject info=itemInfo(c);JSONArray acts=info.optJSONArray("acoes");for(int i=0;acts!=null&&i<acts.length();i++)if(n(acts.optString(i)).equals(a))return true;return false;}
    private static String cooldown(String c,String a){
        if(a.contains("crescimento de plantas")||a.contains("praga de insetos")||a.contains("polen sonifero")||a.contains("augurio")||a.contains("globo de invulnerabilidade")||a.contains("rerrolar ataque")||a.contains("pulverizar")||a.contains("presenca regia")||a.contains("dominar monstro")||a.contains("estatica sinaptica")||a.contains("mente vazia")||a.contains("manto do cruzado")||a.contains("fonte de luar")||a.contains("raio lunar")||a.contains("explosao solar"))return "alvorada";
        if(a.contains("recuperar espaco"))return "descanso_longo";
        if(a.contains("conhecimento estudioso"))return "alvorada_3_usos";
        return "";
    }
    private static String actionKey(String c,String a){return c+" "+a;}
    private static String manualRequirements(String c,String a){
        if(a.contains("rejuvenescer"))return "resultado_1d4_dias";
        if(a.contains("polen sonifero"))return "resultados_salvamentos";
        if(a.contains("converter barra"))return "valor_em_po";
        if(a.contains("transformar arma"))return "forma_arma";
        if(a.contains("rerrolar ataque"))return "nova_rolagem_ataque";
        if(a.contains("pulverizar"))return "resultado_salvamento,dano_total";
        if(a.contains("conhecimento estudioso"))return "resultado_1d6";
        return "";
    }
    private static boolean manualProvided(String req,JSONObject p){for(String k:req.split(",")){if(k.equals("resultados_salvamentos")){if(p.optJSONArray(k)==null)return false;}else if(!p.has(k))return false;}return true;}
    private static boolean used(JSONObject s,String cd,String key){JSONObject root=s.optJSONObject(cd.startsWith("alvorada")?"au_evolving_item_dawn_uses":"au_evolving_item_long_rest_uses");if(root==null)return false;if(cd.equals("alvorada_3_usos"))return root.optInt(key,0)>=3;return root.optBoolean(key,false);}
    private static void markUsed(JSONObject s,String cd,String key)throws Exception{String field=cd.startsWith("alvorada")?"au_evolving_item_dawn_uses":"au_evolving_item_long_rest_uses";JSONObject root=s.optJSONObject(field);if(root==null)root=new JSONObject();if(cd.equals("alvorada_3_usos"))root.put(key,root.optInt(key,0)+1);else root.put(key,true);s.put(field,root);}
    private static JSONObject pending(JSONObject s,String item,String action,String req,JSONObject effect)throws Exception{return s.put("au_evolving_item_resolved",false).put("au_evolving_item_status","aguardando_entrada_manual").put("au_evolving_item_name",item).put("au_evolving_item_action",action).put("au_evolving_item_requires",req).put("au_evolving_item_effect_preview",effect).put("au_evolving_item_auto_rolls",false);}
    private static JSONObject blocked(JSONObject s,String why){try{return s.put("au_evolving_item_resolved",false).put("au_evolving_item_status","bloqueada").put("au_evolving_item_reason",why).put("au_evolving_item_auto_rolls",false);}catch(Exception e){return s;}}
    private static void append(JSONObject s,JSONObject e)throws Exception{JSONArray a=s.optJSONArray("au_evolving_item_effect_log");if(a==null)a=new JSONArray();a.put(e);s.put("au_evolving_item_effect_log",a);}
    private static String nextStage(String value){String[]s=stage(value);if(s==null){String q=n(ArcanaUnleashedPtBr.canonical(value));for(String[]x:STAGES)if(n(x[2]).equals(q))return x[0];return "";}for(int i=0;i<STAGES.length;i++)if(STAGES[i][0].equals(s[0])){if(i+1<STAGES.length&&STAGES[i+1][2].equals(s[2]))return STAGES[i+1][0];return "";}return "";}
    private static String[] stage(String value){String q=n(value);String can=n(ArcanaUnleashedPtBr.canonical(value));for(String[]s:STAGES)if(n(s[0]).equals(q)||n(s[1]).equals(q)||n(s[0]).equals(can))return s;return null;}
    private static String groupPt(String g){if(g.equals("Blossom Rod"))return "Bastão da Floração";if(g.equals("Idol of Good Fortunes"))return "Ídolo das Boas Fortunas";if(g.equals("Keyholes Dagger"))return "Adaga das Fechaduras";if(g.equals("Staff of Skulls"))return "Cajado de Crânios";if(g.equals("Wave-Swept Weapon"))return "Arma Moldada pelas Ondas";if(g.equals("Blade of the Guardian"))return "Lâmina do Guardião";if(g.equals("Breastplate of the Tyrant"))return "Peitoral do Tirano";if(g.equals("Rod of the Honed Mind"))return "Bastão da Mente Afiada";if(g.equals("Wand of Celestial Prowess"))return "Varinha da Proeza Celestial";return g;}
    private static JSONObject copy(JSONObject o){try{return o==null?new JSONObject():new JSONObject(o.toString());}catch(Exception e){return new JSONObject();}}
    private static String id(String s){return n(s).replace(' ','_');}
    private static String n(String s){String x=Normalizer.normalize(s==null?"":s.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}+","");return x.replaceAll("[^a-z0-9]+"," ").trim();}
}
