package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * RUN136 — opções transversais PHB 2024 + Eberron.
 * Regra estrutural: nenhuma escolha nem rolagem é inventada pelo runtime.
 */
public final class Core2024EberronCharacterEngine {
    public static final String PHB2024="Player's Handbook (2024)";
    public static final String EBERRON="Eberron: Rising from the Last War";
    private static final String[] SPECIES_2024={"Aasimar","Anão","Draconato","Elfo","Gnomo","Golias","Humano","Orc","Pequenino","Tiferino"};
    private static final String[] ORIGIN_FEATS={"Alerta","Artifista","Atacante Selvagem","Curandeiro","Habilidoso","Iniciado em Magia","Músico","Sortudo","Valentão de Taverna","Vigoroso"};
    private static final String[] DRAGONMARK_VARIANTS={
        "Humano — Sinal da Busca (Eberron)","Meio-Orc — Sinal da Busca (Eberron)","Humano — Sinal da Criação (Eberron)",
        "Pequenino — Sinal da Cura (Eberron)","Meio-Elfo — Sinal da Detecção (Eberron)","Gnomo — Sinal da Escritura (Eberron)",
        "Pequenino — Sinal da Hospitalidade (Eberron)","Humano — Sinal da Lida (Eberron)","Anão — Sinal da Proteção (Eberron)",
        "Humano — Sinal da Sentinela (Eberron)","Elfo — Sinal da Sombra (Eberron)","Meio-Elfo — Sinal da Tormenta (Eberron)",
        "Humano — Sinal da Travessia (Eberron)"};
    private static final String[] HOUSES={"Cannith","Deneith","Ghallanda","Jorasco","Kundarak","Lyrandar","Medani","Orien","Phiarlan","Sivis","Tharashk","Thuranni","Vadalis"};
    private Core2024EberronCharacterEngine(){}

    public static JSONArray all2024Species(){return arr(SPECIES_2024);}
    public static JSONArray allOriginFeats(){return arr(ORIGIN_FEATS);}
    public static JSONArray allDragonmarkVariants(){return arr(DRAGONMARK_VARIANTS);}
    public static JSONArray allEberronBackgrounds(){return new JSONArray().put("Agente das Casas");}
    public static JSONArray allHouses(){return arr(HOUSES);}
    public static boolean is2024(String profile){String n=n(profile);return n.startsWith("5 5")||n.contains("2024");}
    public static boolean isDragonmarkVariant(String species){return n(species).contains("sinal da ")&&n(species).contains("eberron");}

    public static JSONObject install(){return descriptor();}
    public static JSONObject descriptor(){JSONObject o=new JSONObject();try{o.put("id","run136_core2024_eberron_character_runtime").put("status","compiled_runtime").put("auto_rolls",false).put("species_2024",SPECIES_2024.length).put("origin_feats",ORIGIN_FEATS.length).put("dragonmarks",12).put("dragonmark_species_variants",DRAGONMARK_VARIANTS.length).put("eberron_backgrounds",1);}catch(Exception ignored){}return o;}

    public static String originFeatForBackground(String background){
        String b=n(background);
        if(b.contains("acolito"))return "Iniciado em Magia (Clérigo)";
        if(b.contains("andarilho")||b.contains("mercador"))return "Sortudo";
        if(b.equals("artesao"))return "Artifista";
        if(b.contains("artista"))return "Músico";
        if(b.contains("charlatao")||b.contains("escriba")||b.contains("nobre"))return "Habilidoso";
        if(b.contains("criminoso")||b.contains("guarda"))return "Alerta";
        if(b.contains("eremita"))return "Curandeiro";
        if(b.contains("fazendeiro"))return "Vigoroso";
        if(b.equals("guia"))return "Iniciado em Magia (Druida)";
        if(b.contains("marinheiro"))return "Valentão de Taverna";
        if(b.contains("sabio"))return "Iniciado em Magia (Mago)";
        return "Atacante Selvagem";
    }

    public static JSONArray allowedBackgroundAbilities(String background){
        String b=n(background);String[] a;
        if(b.contains("acolito"))a=new String[]{"Inteligência","Sabedoria","Carisma"};
        else if(b.contains("andarilho"))a=new String[]{"Destreza","Sabedoria","Carisma"};
        else if(b.equals("artesao"))a=new String[]{"Força","Destreza","Inteligência"};
        else if(b.contains("artista"))a=new String[]{"Força","Destreza","Carisma"};
        else if(b.contains("charlatao"))a=new String[]{"Destreza","Constituição","Carisma"};
        else if(b.contains("criminoso"))a=new String[]{"Destreza","Constituição","Inteligência"};
        else if(b.contains("eremita"))a=new String[]{"Constituição","Sabedoria","Carisma"};
        else if(b.contains("escriba"))a=new String[]{"Destreza","Inteligência","Sabedoria"};
        else if(b.contains("fazendeiro"))a=new String[]{"Força","Constituição","Sabedoria"};
        else if(b.contains("guarda"))a=new String[]{"Força","Inteligência","Sabedoria"};
        else if(b.equals("guia"))a=new String[]{"Destreza","Constituição","Sabedoria"};
        else if(b.contains("marinheiro"))a=new String[]{"Força","Destreza","Sabedoria"};
        else if(b.contains("mercador"))a=new String[]{"Constituição","Inteligência","Carisma"};
        else if(b.contains("nobre"))a=new String[]{"Força","Inteligência","Carisma"};
        else if(b.contains("sabio"))a=new String[]{"Constituição","Inteligência","Sabedoria"};
        else a=new String[]{"Força","Destreza","Constituição"};
        return arr(a);
    }

    /** Marks 2024 background ASI as an unresolved player choice; never mutates scores. */
    public static void prepare2024Background(JSONObject s){
        if(s==null||!is2024(s.optString("rules_source")))return;
        try{
            JSONArray allowed=allowedBackgroundAbilities(s.optString("background"));
            s.put("origin_ability_choice_pending",true).put("origin_ability_allowed",allowed).put("origin_ability_bonus","Escolha manual pendente: +2 e +1 em dois atributos diferentes permitidos pelo antecedente 2024");
            String feat=originFeatForBackground(s.optString("background"));s.put("origin_feat",feat);
            String base=baseOriginFeat(feat);s.put("origin_feat_source",PHB2024).put("origin_feat_source_page",200);
            if(originFeatNeedsChoice(base))s.put("origin_feat_choice_pending",true).put("origin_feat_choice_required",base);
            else applyOriginFeat(s,base,new JSONObject());
            if(n(s.optString("species")).equals("humano"))s.put("human_extra_origin_feat_pending",true);
            Character2024SheetReconciler.reconcile(s);
        }catch(Exception ignored){}
    }

    public static JSONObject applyOriginAbilityChoice(JSONObject s,String plus2,String plus1,boolean scoresAlreadyIncludeBonus){
        JSONObject r=result(s,"origin_ability_choice");try{
            if(s==null||!is2024(s.optString("rules_source")))return fail(r,"Perfil 2024 necessário");
            if(n(plus2).equals(n(plus1)))return fail(r,"+2 e +1 devem ser atribuídos a atributos diferentes");
            JSONArray allowed=allowedBackgroundAbilities(s.optString("background"));if(!contains(allowed,plus2)||!contains(allowed,plus1))return fail(r,"Atributo fora das três opções permitidas pelo antecedente");
            if(!scoresAlreadyIncludeBonus){addAbility(s,plus2,2);addAbility(s,plus1,1);}
            s.put("origin_ability_choice_pending",false).put("origin_ability_choice_plus2",canonicalAbility(plus2)).put("origin_ability_choice_plus1",canonicalAbility(plus1)).put("origin_ability_bonus","+2 "+canonicalAbility(plus2)+"; +1 "+canonicalAbility(plus1)).put("origin_ability_scores_include_bonus",true);
            return ok(r).put("plus2",canonicalAbility(plus2)).put("plus1",canonicalAbility(plus1));
        }catch(Exception e){return fail(r,e.getMessage());}
    }

    public static void apply2024SpeciesMechanics(JSONObject s){
        if(s==null||!is2024(s.optString("rules_source")))return;String sp=canonical2024Species(s.optString("species"));if(sp.length()==0)return;
        try{
            int level=Math.max(1,s.optInt("level",1)),pb=pb(level);
            s.put("species_source",PHB2024).put("species_source_page",186).put("species_mechanics_runtime","Core2024EberronCharacterEngine").put("creature_type","Humanoide").put("species_choices_complete",true);
            if(sp.equals("Aasimar")){s.put("speed_m",9).put("darkvision_m",18).put("resistances",append(s.optString("resistances"),"necrótico, radiante")).put("aasimar_healing_hands_uses",1).put("light_cantrip",true).put("light_spellcasting_ability","Carisma").put("species_choice_pending","Tamanho: Pequeno ou Médio");if(level>=3)s.put("celestial_revelation_uses",1);}
            else if(sp.equals("Anão")){s.put("size","Médio").put("speed_m",9).put("darkvision_m",36).put("resistances",append(s.optString("resistances"),"veneno")).put("poison_save_advantage",true).put("dwarven_toughness",true).put("stonecunning_uses",pb).put("stonecunning_tremorsense_m",18).put("stonecunning_duration_min",10);ensureDwarfHp(s,level);}
            else if(sp.equals("Draconato")){s.put("size","Médio").put("speed_m",9).put("darkvision_m",18).put("dragonborn_breath_uses",pb).put("species_choices_complete",false).put("species_choice_pending","Ancestralidade dracônica (define dano e resistência)");if(level>=5)s.put("draconic_flight_uses",1);}
            else if(sp.equals("Elfo")){s.put("size","Médio").put("speed_m",9).put("darkvision_m",18).put("fey_ancestry_charmed_save_advantage",true).put("trance_hours",4).put("magic_sleep_immune",true).put("species_choices_complete",false).put("species_choice_pending","Linhagem élfica + atributo de conjuração + perícia (Intuição, Percepção ou Sobrevivência)");}
            else if(sp.equals("Gnomo")){s.put("size","Pequeno").put("speed_m",9).put("darkvision_m",18).put("gnome_cunning_save_advantage","Inteligência, Sabedoria, Carisma").put("species_choices_complete",false).put("species_choice_pending","Linhagem gnômica: Floresta ou Rocha");}
            else if(sp.equals("Golias")){s.put("size","Médio").put("speed_m",10.5).put("powerful_build",true).put("giant_ancestry_uses",pb).put("species_choices_complete",false).put("species_choice_pending","Ancestralidade Gigante");if(level>=5)s.put("large_form_uses",1);}
            else if(sp.equals("Humano")){s.put("speed_m",9).put("resourceful_heroic_inspiration_on_long_rest",true).put("species_choices_complete",false).put("species_choice_pending","Tamanho Pequeno/Médio + uma perícia + Talento de Origem adicional").put("human_extra_origin_feat_pending",true);}
            else if(sp.equals("Orc")){s.put("size","Médio").put("speed_m",9).put("darkvision_m",36).put("adrenaline_rush_uses",pb).put("relentless_endurance_uses",1);}
            else if(sp.equals("Pequenino")){s.put("size","Pequeno").put("speed_m",9).put("brave_frightened_save_advantage",true).put("halfling_nimbleness",true).put("halfling_luck_reroll_natural_one",true).put("naturally_stealthy",true);}
            else if(sp.equals("Tiferino")){s.put("speed_m",9).put("darkvision_m",18).put("thaumaturgy_cantrip",true).put("species_choices_complete",false).put("species_choice_pending","Legado Ínfero + atributo de conjuração + tamanho Pequeno/Médio");}
            // Re-applying species mechanics during save or restore must not
            // invalidate choices that are already confirmed and structured.
            // A text-only claim of "complete" is never sufficient.
            boolean ready=false;
            String size=n(s.optString("size",""));
            boolean sizeSelected=size.equals("medio")||size.equals("pequeno");
            if(sp.equals("Aasimar"))ready=sizeSelected;
            else if(sp.equals("Draconato"))ready=!s.optString("dragon_ancestry","").isEmpty();
            else if(sp.equals("Elfo"))ready=!s.optString("species_lineage","").isEmpty()
                && !s.optString("species_spellcasting_ability","").isEmpty()
                && !s.optString("elf_keen_senses_choice","").isEmpty();
            else if(sp.equals("Gnomo"))ready=!s.optString("species_lineage","").isEmpty();
            else if(sp.equals("Golias"))ready=!s.optString("giant_ancestry","").isEmpty();
            else if(sp.equals("Tiferino"))ready=sizeSelected
                && !s.optString("fiendish_legacy","").isEmpty()
                && !s.optString("species_spellcasting_ability","").isEmpty();
            else if(sp.equals("Humano")){
                JSONArray feats=s.optJSONArray("selected_origin_feats");
                String backgroundFeat=originFeatForBackground(s.optString("background",""));
                int suffix=backgroundFeat.indexOf(" (");
                if(suffix>=0)backgroundFeat=backgroundFeat.substring(0,suffix);
                boolean extra=false;
                if(feats!=null)for(int i=0;i<feats.length();i++)
                    if(!n(feats.optString(i)).equals(n(backgroundFeat)))extra=true;
                ready=sizeSelected && !s.optString("human_skill_choice","").isEmpty() && extra;
                if(ready)s.put("human_extra_origin_feat_pending",false);
            }
            if(ready){
                s.put("species_choices_complete",true).put("species_choice_pending","");
            }
            appendFeature(s,"Espécie 2024: "+sp+" — traços compilados; escolhas pendentes permanecem explícitas.");
            Character2024SheetReconciler.reconcile(s);
        }catch(Exception ignored){}
    }

    public static JSONObject apply2024SpeciesChoice(JSONObject s,JSONObject c){JSONObject r=result(s,"species_choice");try{
        if(s==null||!is2024(s.optString("rules_source")))return fail(r,"Perfil 2024 necessário");String sp=canonical2024Species(s.optString("species"));if(sp.length()==0)return fail(r,"Espécie 2024 não reconhecida");if(c==null)c=new JSONObject();
        if(sp.equals("Aasimar")){String size=c.optString("size");if(!one(size,"Pequeno","Médio"))return fail(r,"Escolha tamanho Pequeno ou Médio");s.put("size",size);}
        else if(sp.equals("Draconato")){String ancestry=c.optString("ancestry");String type=dragonDamage(ancestry);if(type.length()==0)return fail(r,"Ancestralidade dracônica inválida");s.put("dragon_ancestry",ancestry).put("dragon_damage_type",type).put("resistances",append(s.optString("resistances"),type));}
        else if(sp.equals("Elfo")){String lin=c.optString("lineage"),ability=canonicalAbility(c.optString("ability")),skill=c.optString("skill");if(!one(lin,"Alto","Drow","Floresta"))return fail(r,"Linhagem élfica inválida");if(ability.length()==0)return fail(r,"Escolha Inteligência, Sabedoria ou Carisma");if(!one(skill,"Intuição","Percepção","Sobrevivência"))return fail(r,"Escolha Intuição, Percepção ou Sobrevivência");s.put("species_lineage",lin).put("species_spellcasting_ability",ability)
                .put("elf_keen_senses_choice",skill);appendSkill(s,skill);if(lin.equals("Alto")){s.put("lineage_cantrip","Prestidigitação").put("lineage_level3_spell","Detectar Magia").put("lineage_level5_spell","Passo Nebuloso");}else if(lin.equals("Drow")){s.put("darkvision_m",36).put("lineage_cantrip","Luzes Dançantes").put("lineage_level3_spell","Fogo das Fadas").put("lineage_level5_spell","Escuridão");}else{s.put("speed_m",10.5).put("lineage_cantrip","Druidismo").put("lineage_level3_spell","Passos Longos").put("lineage_level5_spell","Passos sem Pegadas");}}
        else if(sp.equals("Gnomo")){String lin=c.optString("lineage");if(!one(lin,"Floresta","Rocha"))return fail(r,"Linhagem gnômica inválida");s.put("species_lineage",lin);if(lin.equals("Floresta")){s.put("lineage_cantrip","Ilusão Menor").put("speak_with_animals_prepared",true).put("speak_with_animals_free_uses",pb(Math.max(1,s.optInt("level",1))));}else{s.put("lineage_cantrips","Consertar, Prestidigitação").put("clockwork_device_limit",3);}}
        else if(sp.equals("Golias")){String a=c.optString("ancestry");if(!one(a,"Nuvem","Fogo","Gelo","Colina","Pedra","Tempestade"))return fail(r,"Ancestralidade Gigante inválida");s.put("giant_ancestry",a);}
        else if(sp.equals("Humano")){String size=c.optString("size"),skill=c.optString("skill");if(!one(size,"Pequeno","Médio"))return fail(r,"Escolha tamanho Pequeno ou Médio");if(skill.trim().length()==0)return fail(r,"Escolha uma perícia");s.put("size",size);appendSkill(s,skill);s.put("human_skill_choice",skill);}
        else if(sp.equals("Tiferino")){String legacy=c.optString("legacy"),ability=canonicalAbility(c.optString("ability")),size=c.optString("size");if(!one(legacy,"Abissal","Ctônico","Infernal"))return fail(r,"Legado inválido");if(ability.length()==0)return fail(r,"Escolha Inteligência, Sabedoria ou Carisma");if(!one(size,"Pequeno","Médio"))return fail(r,"Escolha tamanho Pequeno ou Médio");s.put("fiendish_legacy",legacy).put("species_spellcasting_ability",ability).put("size",size);if(legacy.equals("Abissal")){s.put("resistances",append(s.optString("resistances"),"veneno")).put("legacy_cantrip","Rajada de Veneno").put("legacy_level3_spell","Raio de Enfermidade").put("legacy_level5_spell","Imobilizar Pessoa");}else if(legacy.equals("Ctônico")){s.put("resistances",append(s.optString("resistances"),"necrótico")).put("legacy_cantrip","Toque Arrepiante").put("legacy_level3_spell","Vitalidade Vazia").put("legacy_level5_spell","Raio do Enfraquecimento");}else{s.put("resistances",append(s.optString("resistances"),"fogo")).put("legacy_cantrip","Raio de Fogo").put("legacy_level3_spell","Repreensão Infernal").put("legacy_level5_spell","Escuridão");}}
        s.put("species_choices_complete",true).put("species_choice_pending","");return ok(r);
    }catch(Exception e){return fail(r,e.getMessage());}}

    public static JSONObject applyOriginFeat(JSONObject s,String feat,JSONObject choices){JSONObject r=result(s,"origin_feat");try{
        if(s==null)return fail(r,"Ficha ausente");String f=baseOriginFeat(feat);if(!contains(arr(ORIGIN_FEATS),f))return fail(r,"Talento de Origem desconhecido");if(choices==null)choices=new JSONObject();int level=Math.max(1,s.optInt("level",1)),pb=pb(level);JSONArray already=s.optJSONArray("selected_origin_feats");if(already!=null&&contains(already,f)&&!f.equals("Habilidoso")&&!f.equals("Iniciado em Magia"))return fail(r,"Este Talento de Origem não é repetível");
        if(f.equals("Alerta")){s.put("initiative_proficiency_bonus",true).put("initiative_swap_with_willing_ally",true);}
        else if(f.equals("Artifista")){JSONArray tools=choices.optJSONArray("tools");if(tools==null||distinctNonEmpty(tools)<3)return fail(r,"Escolha três Ferramentas de Artesão diferentes");for(int i=0;i<tools.length();i++)appendTool(s,tools.optString(i));s.put("nonmagical_purchase_discount_percent",20).put("quick_crafting_on_long_rest",true);}
        else if(f.equals("Atacante Selvagem")){s.put("savage_attacker_once_per_turn",true);}
        else if(f.equals("Curandeiro")){s.put("battle_medic",true).put("healing_reroll_ones",true);}
        else if(f.equals("Habilidoso")){JSONArray ps=choices.optJSONArray("proficiencies");if(ps==null||distinctNonEmpty(ps)<3)return fail(r,"Escolha três perícias ou ferramentas");for(int i=0;i<ps.length();i++)appendGenericProficiency(s,ps.optString(i));}
        else if(f.equals("Iniciado em Magia")){String list=choices.optString("list"),ability=canonicalAbility(choices.optString("ability")),spell=choices.optString("first_level_spell");JSONArray can=choices.optJSONArray("cantrips");if(!one(list,"Clérigo","Druida","Mago")||ability.length()==0||can==null||distinctNonEmpty(can)<2||spell.trim().length()==0)return fail(r,"Escolha lista, dois truques diferentes, magia de 1º círculo e atributo");JSONArray used=s.optJSONArray("magic_initiate_lists");if(used==null)used=new JSONArray();if(contains(used,list))return fail(r,"Iniciado em Magia só pode ser repetido com uma lista diferente");used.put(list);s.put("magic_initiate_lists",used).put("magic_initiate_list",list).put("magic_initiate_ability",ability).put("magic_initiate_cantrips",can).put("magic_initiate_first_level_spell",spell).put("magic_initiate_free_cast_available",true);appendSpells(s,can);appendSpell(s,spell);}
        else if(f.equals("Músico")){JSONArray instruments=choices.optJSONArray("instruments");if(instruments==null||distinctNonEmpty(instruments)<3)return fail(r,"Escolha três instrumentos diferentes");for(int i=0;i<instruments.length();i++)appendTool(s,"Instrumento musical: "+instruments.optString(i));s.put("musician_inspiration_target_limit",pb);}
        else if(f.equals("Sortudo")){s.put("luck_points_max",pb).put("luck_points",pb);}
        else if(f.equals("Valentão de Taverna")){s.put("unarmed_damage_die","1d4").put("unarmed_damage_reroll_ones",true).put("improvised_weapon_proficiency",true).put("unarmed_push_m",1.5).put("unarmed_push_once_per_turn",true);}
        else if(f.equals("Vigoroso")){int applied=s.optInt("tough_origin_applied_level",0);if(applied<level){int delta=2*(level-applied);s.put("max_hp",s.optInt("max_hp",1)+delta).put("hp",s.optInt("hp",1)+delta).put("tough_origin_applied_level",level);}}
        appendSelectedOriginFeat(s,f);appendFeature(s,"Talento de Origem: "+f);s.put("origin_feat_choice_pending",false);return ok(r).put("feat",f);
    }catch(Exception e){return fail(r,e.getMessage());}}

    public static JSONObject executeOriginFeat(JSONObject s,String feat,JSONObject input){JSONObject r=result(s,"origin_feat_action");try{String f=baseOriginFeat(feat);if(input==null)input=new JSONObject();int pb=pb(Math.max(1,s.optInt("level",1)));
        if(f.equals("Sortudo")){int cur=s.optInt("luck_points",0);if(cur<=0)return fail(r,"Sem Pontos de Sorte");String mode=input.optString("mode");if(!one(mode,"vantagem","desvantagem"))return pending(r,"mode",new JSONArray().put("vantagem").put("desvantagem"));s.put("luck_points",cur-1);return ok(r).put("effect",mode);}
        if(f.equals("Curandeiro")){if(!input.has("hit_die_roll"))return pending(r,"hit_die_roll",new JSONArray());int roll=input.optInt("hit_die_roll",0);if(roll<=0)return fail(r,"Informe o resultado real do Dado de Vida");if(roll==1)return pending(r,"healing_die_reroll_or_accept_new_roll",new JSONArray());return ok(r).put("healing",roll+pb);}
        if(f.equals("Atacante Selvagem")){if(!input.has("damage_roll_1")||!input.has("damage_roll_2"))return pending(r,"two_weapon_damage_rolls",new JSONArray());int pick=input.optInt("chosen_roll",0);if(pick!=1&&pick!=2)return pending(r,"chosen_roll",new JSONArray().put(1).put(2));return ok(r).put("damage",input.optInt(pick==1?"damage_roll_1":"damage_roll_2",0));}
        if(f.equals("Alerta")){String ally=input.optString("willing_ally");if(ally.trim().length()==0)return pending(r,"willing_ally",new JSONArray());return ok(r).put("swap_initiative_with",ally);}
        if(f.equals("Músico")){int count=input.optInt("allies",-1);if(count<0)return pending(r,"allies",new JSONArray());if(count>pb)return fail(r,"Máximo de aliados igual ao Bônus de Proficiência");return ok(r).put("heroic_inspiration_allies",count);}
        return ok(r).put("passive_effect",true);
    }catch(Exception e){return fail(r,e.getMessage());}}

    public static JSONObject executeSpeciesTrait(JSONObject s,String trait,JSONObject input){JSONObject r=result(s,"species_trait");try{if(input==null)input=new JSONObject();String t=n(trait),sp=canonical2024Species(s.optString("species"));int level=Math.max(1,s.optInt("level",1)),pb=pb(level);
        if(t.contains("maos curativas")||t.contains("healing hands")){if(s.optInt("aasimar_healing_hands_uses",0)<=0)return fail(r,"Uso já gasto");if(!input.has("healing_roll_total"))return pending(r,"healing_roll_total (jogue "+pb+"d4)",new JSONArray());int h=input.optInt("healing_roll_total",0);s.put("aasimar_healing_hands_uses",0);return ok(r).put("healing",h);}
        if(t.contains("revelacao celestial")){if(s.optInt("celestial_revelation_uses",0)<=0)return fail(r,"Uso já gasto");String form=input.optString("form");if(!one(form,"Asas Celestiais","Manto Necrótico","Consumo Radiante"))return pending(r,"form",new JSONArray().put("Asas Celestiais").put("Manto Necrótico").put("Consumo Radiante"));s.put("celestial_revelation_uses",0).put("celestial_revelation_form",form).put("celestial_revelation_rounds",10);return ok(r).put("form",form).put("bonus_damage",pb);}
        if(t.contains("conhecimento da pedra")){int u=s.optInt("stonecunning_uses",0);if(u<=0)return fail(r,"Sem usos");s.put("stonecunning_uses",u-1);return ok(r).put("tremorsense_m",18).put("duration_min",10);}
        if(t.contains("sopro")){int u=s.optInt("dragonborn_breath_uses",0);if(u<=0)return fail(r,"Sem usos de Sopro");if(!input.has("damage_roll"))return pending(r,"damage_roll",new JSONArray());if(!input.has("save_success"))return pending(r,"save_success",new JSONArray().put(true).put(false));int dmg=input.optInt("damage_roll",0);if(input.optBoolean("save_success",false))dmg=dmg/2;s.put("dragonborn_breath_uses",u-1);return ok(r).put("damage",dmg).put("damage_type",s.optString("dragon_damage_type")).put("save_dc",8+mod(s.optInt("con",10))+pb);}
        if(t.contains("voo draconico")){if(s.optInt("draconic_flight_uses",0)<=0)return fail(r,"Uso já gasto");s.put("draconic_flight_uses",0);return ok(r).put("duration_min",10).put("fly_speed_m",s.optDouble("speed_m",9));}
        if(t.contains("ancestralidade gigante")){int u=s.optInt("giant_ancestry_uses",0);if(u<=0)return fail(r,"Sem usos");String a=s.optString("giant_ancestry");JSONObject x=resolveGiantGift(s,a,input,r);if(!x.optBoolean("ok",false))return x;s.put("giant_ancestry_uses",u-1);return x;}
        if(t.contains("forma grande")){if(s.optInt("large_form_uses",0)<=0)return fail(r,"Uso já gasto");s.put("large_form_uses",0);return ok(r).put("duration_min",10).put("size","Grande").put("speed_bonus_m",3).put("strength_check_advantage",true);}
        if(t.contains("surto de adrenalina")||t.contains("adrenaline")){int u=s.optInt("adrenaline_rush_uses",0);if(u<=0)return fail(r,"Sem usos");s.put("adrenaline_rush_uses",u-1).put("temp_hp",Math.max(s.optInt("temp_hp",0),pb));return ok(r).put("dash_bonus_action",true).put("temp_hp",pb);}
        if(t.contains("resistencia implacavel")){if(s.optInt("relentless_endurance_uses",0)<=0)return fail(r,"Uso já gasto");if(!input.optBoolean("reduced_to_zero_not_killed",false))return fail(r,"Só pode usar ao cair a 0 PV sem morrer instantaneamente");s.put("relentless_endurance_uses",0).put("hp",1);return ok(r).put("hp",1);}
        return fail(r,"Traço ativo não reconhecido");
    }catch(Exception e){return fail(r,e.getMessage());}}

    private static JSONObject resolveGiantGift(JSONObject s,String a,JSONObject input,JSONObject r)throws Exception{String x=n(a);int con=mod(s.optInt("con",10));
        if(x.equals("nuvem"))return ok(r).put("teleport_m",9);
        if(x.equals("colina"))return ok(r).put("knock_prone_large_or_smaller",true);
        if(x.equals("gelo")){if(!input.has("d6_roll"))return pending(r,"d6_roll",new JSONArray());return ok(r).put("extra_cold_damage",input.optInt("d6_roll",0)).put("speed_reduction_m",3);}
        if(x.equals("fogo")){if(!input.has("d10_roll"))return pending(r,"d10_roll",new JSONArray());return ok(r).put("extra_fire_damage",input.optInt("d10_roll",0));}
        if(x.equals("pedra")){if(!input.has("d12_roll"))return pending(r,"d12_roll",new JSONArray());return ok(r).put("damage_reduction",input.optInt("d12_roll",0)+con);}
        if(x.equals("tempestade")){if(!input.has("d8_roll"))return pending(r,"d8_roll",new JSONArray());return ok(r).put("thunder_damage",input.optInt("d8_roll",0));}
        return fail(r,"Ancestralidade Gigante não configurada");}

    public static boolean applyEberronDragonmarkAbilityBonuses(JSONObject s){if(s==null||!isDragonmarkVariant(s.optString("species")))return false;try{String mark=dragonmarkName(s.optString("species"));s.put("dragonmark",mark).put("origin_ability_bonus","Dracossinal de Eberron substitui os bônus raciais normais");if(mark.equals("Sinal da Busca")){addAbility(s,"Sabedoria",2);addAbility(s,"Constituição",1);}else if(mark.equals("Sinal da Criação")){addAbility(s,"Inteligência",2);s.put("dragonmark_flexible_asi_pending",true).put("dragonmark_flexible_asi_exclude","Inteligência");}else if(mark.equals("Sinal da Cura")){addAbility(s,"Sabedoria",1);}else if(mark.equals("Sinal da Detecção")){addAbility(s,"Sabedoria",2);s.put("dragonmark_flexible_asi_pending",true).put("dragonmark_flexible_asi_exclude","Sabedoria");}else if(mark.equals("Sinal da Escritura")){addAbility(s,"Carisma",1);}else if(mark.equals("Sinal da Hospitalidade")){addAbility(s,"Carisma",1);}else if(mark.equals("Sinal da Lida")){addAbility(s,"Sabedoria",2);s.put("dragonmark_flexible_asi_pending",true).put("dragonmark_flexible_asi_exclude","Sabedoria");}else if(mark.equals("Sinal da Proteção")){addAbility(s,"Inteligência",1);}else if(mark.equals("Sinal da Sentinela")){addAbility(s,"Constituição",2);addAbility(s,"Sabedoria",1);}else if(mark.equals("Sinal da Sombra")){addAbility(s,"Carisma",1);}else if(mark.equals("Sinal da Tormenta")){addAbility(s,"Carisma",2);addAbility(s,"Destreza",1);}else if(mark.equals("Sinal da Travessia")){addAbility(s,"Destreza",2);s.put("dragonmark_flexible_asi_pending",true).put("dragonmark_flexible_asi_exclude","Destreza");}return true;}catch(Exception ignored){return true;}}

    public static void applyDragonmarkMechanics(JSONObject s){if(s==null||!isDragonmarkVariant(s.optString("species")))return;try{String mark=dragonmarkName(s.optString("species"));s.put("dragonmark",mark).put("dragonmark_source",EBERRON).put("dragonmark_source_page",40).put("dragonmark_runtime","Core2024EberronCharacterEngine").put("dragonmark_choices_complete",!s.optBoolean("dragonmark_flexible_asi_pending",false));
        if(mark.equals("Sinal da Busca")){d4(s,"Percepção, Sobrevivência");markSpells(s,"Sabedoria","Marca do Caçador","Localizar Objeto","Fogo das Fadas; Passos Longos; Localizar Animais ou Plantas; Localizar Objeto; Clarividência; Falar com Plantas; Adivinhação; Localizar Criatura; Comunhão com a Natureza");s.put("darkvision_m",18);}
        else if(mark.equals("Sinal da Criação")){d4(s,"Arcanismo, ferramenta de artesão escolhida");markSpells(s,"Inteligência","Consertar","Arma Mágica","Identificar; Disco Flutuante de Tenser; Chama Contínua; Arma Mágica; Conjurar Barragem; Arma Elemental; Fabricar; Moldar Rochas; Criação");s.put("dragonmark_choice_pending","Ferramenta de Artesão + atributo do +1");}
        else if(mark.equals("Sinal da Cura")){d4(s,"Medicina, kit de herbalismo");markSpells(s,"Sabedoria","Curar Ferimentos","Restauração Menor","Curar Ferimentos; Palavra Curativa; Restauração Menor; Oração de Cura; Aura de Vitalidade; Palavra Curativa em Massa; Aura de Pureza; Aura de Vida; Restauração Maior");}
        else if(mark.equals("Sinal da Detecção")){d4(s,"Investigação, Intuição");markSpells(s,"Sabedoria","Detectar Magia; Detectar Veneno e Doença","Ver o Invisível","Detectar Bem e Mal; Detectar Veneno e Doença; Detectar Pensamentos; Encontrar Armadilhas; Clarividência; Indetectável; Olho Arcano; Adivinhação; Lendas e Histórias");s.put("dragonmark_choice_pending","Atributo do +1");}
        else if(mark.equals("Sinal da Escritura")){d4(s,"História, suprimentos de calígrafo");markSpells(s,"Inteligência","Mensagem; Compreender Idiomas","Boca Encantada","Compreender Idiomas; Escrita Ilusória; Mensageiro Animal; Silêncio; Enviar Mensagem; Idiomas; Olho Arcano; Confusão; Sonho");s.put("mark_comprehend_languages_short_rest_available",true);}
        else if(mark.equals("Sinal da Hospitalidade")){d4(s,"Persuasão, suprimentos de cervejeiro/utensílios de cozinheiro");markSpells(s,"Carisma","Prestidigitação; Purificar Alimentos e Bebidas; Servo Invisível","","Bom Fruto; Sono; Ajuda; Acalmar Emoções; Criar Alimentos e Água; Pequena Cabana de Leomund; Aura de Pureza; Santuário Particular de Mordenkainen; Consagrar");}
        else if(mark.equals("Sinal da Lida")){d4(s,"Adestrar Animais, Natureza");markSpells(s,"Sabedoria","Amizade Animal; Falar com Animais","","Amizade Animal; Falar com Animais; Sentido Bestial; Acalmar Emoções; Sinal de Esperança; Conjurar Animais; Aura de Devoção; Dominar Besta; Despertar");s.put("mark_level3_can_target_low_int_monstrosity",true).put("dragonmark_choice_pending","Atributo do +1");}
        else if(mark.equals("Sinal da Proteção")){d4(s,"Investigação, ferramentas de ladrão");markSpells(s,"Inteligência","Alarme; Armadura Arcana","Tranca Arcana","Alarme; Armadura de Agathys; Tranca Arcana; Arrombar; Glifo de Vigilância; Círculo Mágico; Arca Secreta de Leomund; Cão Fiel de Mordenkainen; Concha Antivida");}
        else if(mark.equals("Sinal da Sentinela")){d4(s,"Intuição, Percepção");markSpells(s,"Sabedoria","Escudo","","Duelo Compelido; Escudo da Fé; Vínculo de Proteção; Zona da Verdade; Contramágica; Proteção contra Energia; Proteção contra a Morte; Guardião da Fé; Mão de Bigby");s.put("vigilant_guardian_uses",1);}
        else if(mark.equals("Sinal da Sombra")){d4(s,"Atuação, Furtividade");markSpells(s,"Carisma","Ilusão Menor","Invisibilidade","Disfarçar-se; Imagem Silenciosa; Escuridão; Passos sem Pegadas; Clarividência; Imagem Maior; Invisibilidade Maior; Terreno Alucinatório; Despistar");}
        else if(mark.equals("Sinal da Tormenta")){d4(s,"Acrobacia, ferramentas de navegador");markSpells(s,"Carisma","Lufada","Lufada de Vento","Queda Suave; Névoa Obscurecente; Lufada de Vento; Levitação; Granizo; Muralha de Vento; Conjurar Elementais Menores; Controlar Água; Conjurar Elemental");s.put("resistances",append(s.optString("resistances"),"elétrico"));}
        else if(mark.equals("Sinal da Travessia")){d4(s,"Acrobacia, veículos terrestres");markSpells(s,"Destreza","Passo Nebuloso","","Recuo Acelerado; Salto; Passo Nebuloso; Passos sem Pegadas; Piscar; Montaria Fantasmagórica; Porta Dimensional; Movimentação Livre; Círculo de Teleporte");s.put("speed_m",10.5).put("dragonmark_choice_pending","Atributo do +1");}
        appendFeature(s,"Dracossinal: "+mark+" — traços e lista expandida persistidos.");
    }catch(Exception ignored){}}

    public static JSONObject applyDragonmarkChoice(JSONObject s,JSONObject c){JSONObject r=result(s,"dragonmark_choice");try{if(!isDragonmarkVariant(s.optString("species")))return fail(r,"Espécie não é variante dracoassinalada");if(c==null)c=new JSONObject();if(s.optBoolean("dragonmark_flexible_asi_pending",false)){String a=canonicalAbility(c.optString("ability_plus1"));if(a.length()==0||n(a).equals(n(s.optString("dragonmark_flexible_asi_exclude"))))return fail(r,"Escolha atributo válido para +1");if(!c.optBoolean("ability_applied_to_editor",false))addAbility(s,a,1);s.put("dragonmark_flexible_asi_pending",false).put("dragonmark_flexible_asi",a);}if(dragonmarkName(s.optString("species")).equals("Sinal da Criação")){String tool=c.optString("artisan_tool");if(tool.trim().length()==0)return fail(r,"Escolha uma Ferramenta de Artesão");appendTool(s,tool);s.put("dragonmark_artisan_tool",tool);}s.put("dragonmark_choices_complete",true).put("dragonmark_choice_pending","");return ok(r);}catch(Exception e){return fail(r,e.getMessage());}}

    public static void applyHouseAgentBackground(JSONObject s){if(s==null||!n(s.optString("background")).equals("agente das casas"))return;try{s.put("background_source",EBERRON).put("background_source_page",53).put("background_mechanics_runtime","Core2024EberronCharacterEngine").put("background_skill_proficiencies","Investigação, Persuasão").put("background_feature","Contatos Dracoassinalados").put("house_agent_house_pending",true).put("background_choices_complete",false).put("background_choice_pending","Escolha a casa dracoassinalada");appendSkill(s,"Investigação");appendSkill(s,"Persuasão");appendFeature(s,"Contatos Dracoassinalados: apoio, alimento/alojamento em enclaves e suprimentos de missão conforme posição e decisão da casa.");appendInventory(s,"Traje fino; anel-sinete da casa; papéis de identificação; bolsa com 20 po");}catch(Exception ignored){}}

    public static JSONObject applyHouseAgentChoice(JSONObject s,String house){JSONObject r=result(s,"house_agent_choice");try{if(!n(s.optString("background")).equals("agente das casas"))return fail(r,"Antecedente Agente das Casas necessário");String h=canonicalHouse(house);if(h.length()==0)return fail(r,"Casa inválida");String tools=houseTools(h);for(String x:tools.split(";"))if(x.trim().length()>0)appendTool(s,x.trim());s.put("dragonmarked_house",h).put("house_agent_house_pending",false).put("background_choices_complete",true).put("background_choice_pending","");return ok(r).put("house",h).put("tools",tools);}catch(Exception e){return fail(r,e.getMessage());}}

    public static void onLongRest(JSONObject s){if(s==null)return;try{int level=Math.max(1,s.optInt("level",1)),pb=pb(level);String sp=canonical2024Species(s.optString("species"));if(sp.equals("Aasimar")){s.put("aasimar_healing_hands_uses",1);if(level>=3)s.put("celestial_revelation_uses",1);}if(sp.equals("Anão"))s.put("stonecunning_uses",pb);if(sp.equals("Draconato")){s.put("dragonborn_breath_uses",pb);if(level>=5)s.put("draconic_flight_uses",1);}if(sp.equals("Golias")){s.put("giant_ancestry_uses",pb);if(level>=5)s.put("large_form_uses",1);}if(sp.equals("Humano"))s.put("heroic_inspiration",true);if(sp.equals("Orc")){s.put("adrenaline_rush_uses",pb).put("relentless_endurance_uses",1);}if(sp.equals("Gnomo")&&n(s.optString("species_lineage")).equals("floresta"))s.put("speak_with_animals_free_uses",pb);if(s.has("luck_points_max"))s.put("luck_points",s.optInt("luck_points_max",pb));if(s.has("magic_initiate_first_level_spell"))s.put("magic_initiate_free_cast_available",true);if(s.optBoolean("quick_crafting_on_long_rest",false))s.put("quick_crafting_choice_pending",true);if(isDragonmarkVariant(s.optString("species"))){s.put("dragonmark_free_spells_available",true).put("vigilant_guardian_uses",dragonmarkName(s.optString("species")).equals("Sinal da Sentinela")?1:s.optInt("vigilant_guardian_uses",0));}s.put("musician_inspiration_target_limit",s.has("musician_inspiration_target_limit")?pb:s.optInt("musician_inspiration_target_limit",0));}catch(Exception ignored){}}
    public static void onShortRest(JSONObject s){if(s==null)return;try{String sp=canonical2024Species(s.optString("species"));if(sp.equals("Orc"))s.put("adrenaline_rush_uses",pb(Math.max(1,s.optInt("level",1))));if(dragonmarkName(s.optString("species")).equals("Sinal da Escritura"))s.put("mark_comprehend_languages_short_rest_available",true);}catch(Exception ignored){}}

    private static void markSpells(JSONObject s,String ability,String free1,String free3,String expanded)throws Exception{s.put("dragonmark_spellcasting_ability",ability).put("dragonmark_free_spells",free1+(free3.length()>0?"; "+free3:"")).put("dragonmark_free_spells_available",true).put("expanded_spell_list",expanded);}
    private static void d4(JSONObject s,String checks)throws Exception{s.put("dragonmark_intuition_d4",checks);}
    private static String dragonmarkName(String species){String x=species==null?"":species;int p=x.indexOf("Sinal da ");if(p<0)return "";int e=x.indexOf(" (Eberron)",p);return e>p?x.substring(p,e).trim():"";}
    private static String canonicalHouse(String house){for(String h:HOUSES)if(n(h).equals(n(house)))return h;return "";}
    private static String houseTools(String h){if(h.equals("Cannith"))return "Suprimentos de alquimista; Ferramentas de funileiro";if(h.equals("Deneith"))return "Um jogo; Veículos terrestres";if(h.equals("Ghallanda"))return "Suprimentos de cervejeiro; Utensílios de cozinheiro";if(h.equals("Jorasco"))return "Suprimentos de alquimista; Kit de herbalismo";if(h.equals("Kundarak"))return "Ferramentas de ladrão; Ferramentas de funileiro";if(h.equals("Lyrandar"))return "Ferramentas de navegador; Veículos aéreos ou aquáticos";if(h.equals("Medani"))return "Kit de disfarce; Ferramentas de ladrão";if(h.equals("Orien"))return "Um jogo; Veículos terrestres";if(h.equals("Phiarlan"))return "Kit de disfarce; Um instrumento musical";if(h.equals("Sivis"))return "Suprimentos de calígrafo; Kit de falsificação";if(h.equals("Tharashk"))return "Um jogo; Ferramentas de ladrão";if(h.equals("Thuranni"))return "Um instrumento musical; Kit de venenos";if(h.equals("Vadalis"))return "Kit de herbalismo; Veículos terrestres";return "";}

    private static String canonical2024Species(String raw){String x=n(raw);if(x.contains("eberron")||x.contains("multiverse"))return "";if(x.equals("aasimar"))return "Aasimar";if(x.equals("anao"))return "Anão";if(x.equals("draconato"))return "Draconato";if(x.equals("elfo"))return "Elfo";if(x.equals("gnomo"))return "Gnomo";if(x.equals("golias"))return "Golias";if(x.equals("humano"))return "Humano";if(x.equals("orc"))return "Orc";if(x.equals("pequenino")||x.equals("halfling"))return "Pequenino";if(x.equals("tiferino")||x.equals("tiefling"))return "Tiferino";return "";}
    private static String dragonDamage(String ancestry){String a=n(ancestry);if(a.contains("negro")||a.contains("cobre"))return "ácido";if(a.contains("azul")||a.contains("bronze"))return "elétrico";if(a.contains("latão")||a.contains("dourado")||a.contains("vermelho"))return "fogo";if(a.contains("prateado")||a.contains("branco"))return "gélido";if(a.contains("verde"))return "veneno";return "";}
    private static boolean originFeatNeedsChoice(String f){return f.equals("Artifista")||f.equals("Habilidoso")||f.equals("Iniciado em Magia")||f.equals("Músico");}
    private static String baseOriginFeat(String f){String x=f==null?"":f.trim();int p=x.indexOf(" (");return p>0?x.substring(0,p):x;}
    private static String canonicalAbility(String a){String x=n(a);if(x.equals("forca"))return "Força";if(x.equals("destreza"))return "Destreza";if(x.equals("constituicao"))return "Constituição";if(x.equals("inteligencia"))return "Inteligência";if(x.equals("sabedoria"))return "Sabedoria";if(x.equals("carisma"))return "Carisma";return "";}
    private static void addAbility(JSONObject s,String a,int d)throws Exception{String x=canonicalAbility(a),k=x.equals("Força")?"str":x.equals("Destreza")?"dex":x.equals("Constituição")?"con":x.equals("Inteligência")?"int_score":x.equals("Sabedoria")?"wis":x.equals("Carisma")?"cha":"";if(k.length()>0)s.put(k,Math.min(20,s.optInt(k,10)+d));}
    private static int mod(int score){return (int)Math.floor((score-10)/2.0);}
    private static int pb(int level){return 2+(Math.max(1,Math.min(20,level))-1)/4;}
    private static void ensureDwarfHp(JSONObject s,int level)throws Exception{if(s.optInt("dwarven_toughness_applied_level",0)<level){int d=level-s.optInt("dwarven_toughness_applied_level",0);s.put("max_hp",s.optInt("max_hp",1)+d).put("hp",s.optInt("hp",1)+d).put("dwarven_toughness_applied_level",level);}}
    private static void appendSelectedOriginFeat(JSONObject s,String f)throws Exception{JSONArray a=s.optJSONArray("selected_origin_feats");if(a==null)a=new JSONArray();if(!contains(a,f))a.put(f);s.put("selected_origin_feats",a);String text=s.optString("selected_feats");if(!textContains(text,f))s.put("selected_feats",text.trim().length()==0?f:text+"\n"+f);}
    private static void appendFeature(JSONObject s,String v)throws Exception{String x=s.optString("features");if(!textContains(x,v))s.put("features",x.trim().length()==0?v:x+"\n"+v);}
    private static void appendSkill(JSONObject s,String v)throws Exception{String x=s.optString("skills");if(!textContains(x,v))s.put("skills",x.trim().length()==0?v+" • proficiente":x+"\n"+v+" • proficiente");}
    private static void appendTool(JSONObject s,String v)throws Exception{String x=s.optString("tools");if(!textContains(x,v))s.put("tools",x.trim().length()==0?v:x+"\n"+v);}
    private static void appendGenericProficiency(JSONObject s,String v)throws Exception{if(v.toLowerCase(Locale.ROOT).contains("ferrament")||v.toLowerCase(Locale.ROOT).contains("kit")||v.toLowerCase(Locale.ROOT).contains("instrument")||v.toLowerCase(Locale.ROOT).contains("veículo"))appendTool(s,v);else appendSkill(s,v);}
    private static void appendInventory(JSONObject s,String v)throws Exception{String x=s.optString("inventory");if(!textContains(x,v))s.put("inventory",x.trim().length()==0?v:x+"\n"+v).put("equipment",x.trim().length()==0?v:x+"\n"+v);}
    private static void appendSpell(JSONObject s,String v)throws Exception{String x=s.optString("spells");if(!textContains(x,v))s.put("spells",x.trim().length()==0?v:x+"\n"+v);}
    private static void appendSpells(JSONObject s,JSONArray a)throws Exception{for(int i=0;i<a.length();i++)appendSpell(s,a.optString(i));}
    private static String append(String current,String values){String out=current==null?"":current.trim();for(String v:values.split(",")){String q=v.trim();if(q.length()==0||textContains(out,q))continue;out=out.length()==0?q:out+", "+q;}return out;}
    private static boolean textContains(String text,String token){return n(text).contains(n(token));}
    private static boolean contains(JSONArray a,String v){if(a==null)return false;for(int i=0;i<a.length();i++)if(n(a.optString(i)).equals(n(v)))return true;return false;}
    private static int distinctNonEmpty(JSONArray a){java.util.HashSet<String>x=new java.util.HashSet<String>();for(int i=0;i<a.length();i++){String v=n(a.optString(i));if(v.length()>0)x.add(v);}return x.size();}
    private static JSONArray arr(String[] xs){JSONArray a=new JSONArray();for(String x:xs)a.put(x);return a;}
    private static boolean one(String v,String... xs){for(String x:xs)if(n(v).equals(n(x)))return true;return false;}
    private static String n(String s){return Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","").replaceAll("[^a-z0-9]+"," ").trim();}
    private static JSONObject result(JSONObject s,String action){JSONObject r=new JSONObject();try{r.put("ok",false).put("auto_rolls",false).put("runtime","Core2024EberronCharacterEngine").put("action",action).put("state",s==null?new JSONObject():s);}catch(Exception ignored){}return r;}
    private static JSONObject ok(JSONObject r)throws Exception{return r.put("ok",true).put("resolved",true);}
    private static JSONObject fail(JSONObject r,String reason){try{r.put("ok",false).put("resolved",false).put("reason",reason==null?"Falha":reason);}catch(Exception ignored){}return r;}
    private static JSONObject pending(JSONObject r,String input,JSONArray options){try{r.put("ok",false).put("resolved",false).put("required_input",input).put("options",options==null?new JSONArray():options);}catch(Exception ignored){}return r;}
}
