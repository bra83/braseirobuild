package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** RUN192: deterministic presentation catalog. These descriptors never resolve or mutate game mechanics. */
public final class SupplementGuidedActionSchema {
    private SupplementGuidedActionSchema(){}
    public static final class Action {
        public final String id,label;
        Action(String id,String label){this.id=id;this.label=label;}
    }
    public static final class Field {
        public final String id,label,type;public final boolean required;public final int min,max;
        Field(String id,String label,String type,boolean required,int min,int max){this.id=id;this.label=label;this.type=type;this.required=required;this.min=min;this.max=max;}
    }
    private static final Map<String,Action[]> ACTIONS=new LinkedHashMap<>();
    private static final Map<String,String> FIELDS=new LinkedHashMap<>();
    private static void actions(String kind,String... pairs){Action[] a=new Action[pairs.length];for(int i=0;i<pairs.length;i++){
        int n=pairs[i].indexOf('=');a[i]=new Action(pairs[i].substring(0,n),pairs[i].substring(n+1));
    }ACTIONS.put(kind,a);}
    private static void fields(String kind,String action,String... parts){FIELDS.put(kind+":"+action,String.join(";",parts));}
    // Field notation: C=checkbox, N=number, T=text, A=comma-separated list. Last segment ! means required.
    private static final String CONFIRM="C:confirm:Confirmo que desejo executar esta ação";
    private static final String GM="C:gm_confirmed:O Mestre confirmou esta alteração";
    private static final String REACT="C:reaction_confirmed:Confirmo o uso da reação";
    private static final String SPEND="C:spell_slot_spend_confirmed:Confirmo o gasto do espaço de magia";
    private static final String HIT="C:hit_confirmed:O ataque acertou (resultado conferido)";
    private static final String SLOT="N:slot_level:Nível do espaço de magia:1:9:!";
    private static final String DELTA="N:delta:Variação do valor (-20 a +20):-20:20:!";
    static {
        actions("eb_artificer", "tinkers_magic=Truques de artífice", "replicate_magic_items=Replicar item mágico",
          "charge_magic_item=Carregar item", "drain_magic_item=Drenar item", "transmute_magic_item=Transmutar item",
          "learn_magic_item_plan=Aprender projeto", "flash_of_genius=Lampejo de Gênio", "spell_storing_item=Armazenar magia",
          "cheat_death=Escapar da morte");
        actions("eb_subclass", "experimental_elixir=Preparar elixir", "lesser_restoration_free=Restauração Menor gratuita",
          "set_armor_model=Escolher modelo de armadura", "eldritch_cannon=Preparar canhão místico",
          "create_steel_defender=Criar Defensor de Aço", "arcane_jolt=Impacto Arcano", "create_atlas_maps=Criar mapas do atlas",
          "portal_jump=Salto pelo portal", "guided_precision=Precisão Guiada", "ingenious_movement=Movimento Engenhoso",
          "safe_haven=Refúgio Seguro");
        actions("eb_species",
          "choose_shifting_trait=Escolher traço mutável",
          "fey_gift_cantrip=Dom feérico",
          "severed_from_dreams_skill=Perícia dos Sonhos",
          "shapechange=Mudar forma",
          "shift=Ativar transformação",
          "skill_versatility=Versatilidade de perícia",
          "telepathy=Usar telepatia");
        actions("eb_feat",
          "acquire=Adquirir talento",
          "free_spell_cast=Conjuração gratuita",
          "gust_of_wind_free_cast=Rajada de Vento gratuita",
          "intuition_die=Usar dado de intuição",
          "vigilant_guardian=Guardião Vigilante",
          "windwright_intuition=Intuição do Navegador");
        actions("eb_homunculus", "summon=Criar servo homúnculo");
        actions("eb_item",
          "dazzle=Ofuscar",
          "protect_concentration=Proteger concentração",
          "recover_slot=Recuperar espaço de magia",
          "repel=Repulsão do escudo",
          "teleport=Teletransportar",
          "transform=Transformar ferramenta");
        actions("eb_bastion", "build=Construir instalação", "order=Dar ordem à instalação");
        actions("eb_airship",
          "assign_crew=Designar tripulação",
          "commission=Registrar nave",
          "component_damage=Dano ao componente",
          "maneuver=Executar manobra",
          "repair=Reparar componente",
          "upgrade=Melhorar nave");
        actions("eb_framework", "activate=Ativar estrutura de campanha", "record_fact=Registrar acontecimento",
          "advance=Avançar progresso");
        actions("eb_creature", "attack=Registrar ataque", "saving_throw=Registrar resistência", "apply_damage=Aplicar dano");
        actions("fr_subclass",
          "absorb_spells=Absorver magia",
          "bladesong=Canção da Lâmina",
          "bloodthirst=Sede de Sangue",
          "divine_foreknowledge=Presságio Divino",
          "dread_allegiance=Aliança Sombria",
          "elemental_smite=Golpe Elemental",
          "group_recovery=Recuperação do grupo",
          "hunters_rime=Rime do Caçador",
          "mind_magic=Magia da Mente",
          "moons_inspiration=Inspiração Lunar",
          "rallying_surge=Surto de Apoio",
          "shared_resilience=Resiliência Compartilhada",
          "song_of_defense=Canção de Defesa",
          "song_of_victory=Canção da Vitória",
          "spellfire_burst=Explosão da Chama",
          "strike_fear=Impor Medo");
        actions("fr_feat",
          "acquire=Adquirir talento",
          "bloodied_resistance=Resistência ao Sangramento",
          "distracting_melody=Melodia Distraidora",
          "dragon_terror=Terror do Dragão",
          "exploit_opening=Explorar abertura",
          "flee_fools=Fujam, Tolos!",
          "inspiring_strike=Golpe Inspirador",
          "magic_absorption=Absorção Mágica",
          "rallying_cry=Grito de Reagrupamento",
          "saving_throw_reroll=Repetir resistência",
          "siphon_life=Drenar Vida",
          "spellfire_flame=Chama Mágica",
          "stand_as_one=Resistir como um só",
          "tag_team=Trabalho em Equipe",
          "telepathy=Telepatia",
          "wish_magic=Magia do Desejo");
        actions("fr_spell", "cast=Conjurar magia selecionada");
        actions("fr_circle", "cast=Realizar magia de círculo");
        actions("fr_faction", "join=Ingressar na facção", "leave=Sair da facção",
          "adjust_renown=Atualizar renome da facção", "relationship=Registrar relacionamento");
        actions("fr_renown", "adjust=Atualizar renome com divindade");
        actions("fr_aurora", "purchase=Comprar item ou serviço", "toggle=Acender/apagar anel",
          "deploy=Ativar embarcação mágica", "teleport=Usar viagem mágica", "cover=Adotar cobertura da carroça");

        fields("eb_artificer","tinkers_magic", "T:tool:Ferramenta utilizada:!",CONFIRM);
        fields("eb_artificer","replicate_magic_items", "A:items:Itens a replicar (separados por vírgulas):!",CONFIRM);
        fields("eb_artificer","charge_magic_item","T:item:Nome do item:!",CONFIRM);
        fields("eb_artificer","drain_magic_item","T:item:Nome do item:!",CONFIRM);
        fields("eb_artificer","transmute_magic_item","T:item:Nome do item:!", "T:new_item:Novo item:!",CONFIRM);
        fields("eb_artificer","learn_magic_item_plan","T:item:Nome do projeto:!",CONFIRM);
        fields("eb_artificer","flash_of_genius","C:failed_check_or_save:O teste ou resistência falhou",REACT,"T:target_id:Alvo (nome/identificador reconhecido)");
        fields("eb_artificer","spell_storing_item","T:spell:Magia armazenada:!","N:spell_level:Círculo da magia:1:3:!",CONFIRM);
        fields("eb_artificer","cheat_death","C:would_drop_to_zero:O personagem cairia a zero PV","A:dismiss_items:Itens replicados a dispensar:!",CONFIRM);

        fields("eb_subclass","experimental_elixir","T:effect:Efeito escolhido ou rolado:!",CONFIRM);
        fields("eb_subclass","lesser_restoration_free",CONFIRM,"T:target_id:Alvo (opcional)");
        fields("eb_subclass","set_armor_model","T:model:Modelo: Guardian ou Infiltrator:!",CONFIRM);
        fields("eb_subclass","eldritch_cannon","T:mode:Modo: Flamethrower, Force Ballista ou Protector:!","T:size:Tamanho: Tiny ou Small",CONFIRM);
        fields("eb_subclass","create_steel_defender",CONFIRM);
        fields("eb_subclass","arcane_jolt","T:mode:Modo: damage ou healing:!",HIT,"N:roll:Total da rolagem do Impacto:0:200:!");
        fields("eb_subclass","create_atlas_maps","A:holders:Portadores dos mapas:!",CONFIRM);
        fields("eb_subclass","portal_jump","N:teleport_feet:Distância do salto em pés:1:10:!","C:spend_half_speed_confirmed:Confirme gasto de metade do deslocamento");
        fields("eb_subclass","guided_precision","C:qualifying_hit_or_spell_damage:O ataque ou magia satisfaz o gatilho");
        fields("eb_subclass","ingenious_movement","C:flash_of_genius_used:Lampejo de Gênio usado","N:teleport_feet:Distância em pés:0:30:!","T:target_id:Alvo (opcional)");
        fields("eb_subclass","safe_haven","C:map_holder_at_zero_hp:Portador de mapa com zero PV","C:destroy_map_confirmed:Confirmo destruir um mapa");
        fields("eb_species","vigilant_guardian",REACT);
        fields("eb_species","free_spell_cast",CONFIRM,"T:spell:Magia conhecida:!");
        fields("eb_species","choose_shifting_trait","T:trait:Traço escolhido:!",CONFIRM);
        fields("eb_feat","acquire",CONFIRM);
        fields("eb_feat","free_spell_cast","T:spell:Magia conhecida:!",CONFIRM);
        fields("eb_feat","gust_of_wind_free_cast",CONFIRM);
        fields("eb_feat","intuition_die","N:manual_roll:Resultado do dado de Intuição:1:20:!");
        fields("eb_feat","vigilant_guardian",REACT);
        fields("eb_feat","windwright_intuition","T:check:Teste: Dexterity (Acrobatics) ou Navigator Tools:!","N:manual_roll:Resultado do dado:1:20:!");
        fields("eb_species","skill_versatility","T:skill:Perícia escolhida:!",CONFIRM);
        fields("eb_species","telepathy","T:target_id:Alvo da telepatia");
        fields("eb_species","shift",CONFIRM);
        fields("eb_species","shapechange",CONFIRM);
        fields("eb_species","fey_gift_cantrip",CONFIRM);
        
        fields("eb_item","dazzle",CONFIRM);
        fields("eb_item","transform","T:tool:Tipo de ferramenta:!",CONFIRM);
        fields("eb_item","protect_concentration","C:failed_concentration_save:Concentração falhou",REACT);
        fields("eb_item","repel",REACT,"N:push_feet:Empurrar em pés:0:15:!");
        fields("eb_item","recover_slot","N:slot_level:Espaço a recuperar:1:9:!","C:bonus_action_confirmed:Confirmo usar ação bônus");
        fields("eb_item","teleport","N:distance_feet:Distância em pés:1:15:!","C:bonus_action_confirmed:Confirmo usar ação bônus","C:space_previously_occupied_confirmed:Espaço já ocupado nesta rodada");
        fields("eb_homunculus","summon","N:slot_level:Nível do espaço de magia:2:9:!","C:material_100gp_confirmed:Material de 100 po disponível","C:space_confirmed:Espaço livre para servo","T:appearance:Aparência do servo:!",SPEND);
        fields("eb_bastion","build","C:requirements_confirmed:Requisitos de construção cumpridos",CONFIRM);
        fields("eb_bastion","order","T:order:Ordem à instalação:!","T:result:Resultado narrativo (opcional)","C:manual_resolution_confirmed:Resultado da ordem foi verificado");
        fields("eb_airship","commission",CONFIRM);
        fields("eb_airship","maneuver","T:maneuver:Manobra escolhida:!","C:pilot_check_confirmed:Teste do piloto resolvido");
        fields("eb_airship","assign_crew","A:crew:Tripulantes:!",CONFIRM);
        fields("eb_airship","component_damage","T:component:Componente:!","N:damage:Dano confirmado:0:100000:!","C:damage_roll_confirmed:O dano foi rolado/verificado");
        fields("eb_airship","repair","T:component:Componente:!","N:amount:Reparo realizado:0:100000:!","C:repair_confirmed:Reparo e custos conferidos");
        fields("eb_airship","upgrade","T:upgrade:Melhoria instalada:!","C:requirements_confirmed:Requisitos cumpridos",CONFIRM);
        fields("eb_framework","activate",CONFIRM);
        fields("eb_framework","record_fact","T:fact:Acontecimento confirmado:!",CONFIRM);
        fields("eb_framework","advance",DELTA,CONFIRM);
        fields("eb_creature","attack","N:manual_attack_total:Resultado final do ataque:-20:150:!","N:manual_damage:Dano rolado:0:100000:!");
        fields("eb_creature","saving_throw","N:manual_save_total:Resultado final da resistência:-20:150:!");
        fields("eb_creature","apply_damage","N:current_hp:PV atual da criatura:0:100000:!","N:damage:Dano aplicado:0:100000:!",CONFIRM);

        fields("fr_spell","cast",SLOT,SPEND);
        fields("fr_circle","cast","N:secondary_casters:Conjuradores auxiliares:1:30:!","C:primary_magic_action_confirmed:Conjurador principal gastou ação mágica","C:secondary_magic_actions_confirmed:Conjuradores auxiliares gastaram ações mágicas","C:eligible_spell_confirmed:Magia elegível confirmada","N:spell_level:Nível da magia (se Supplant):1:9", "C:secondary_slots_level_ok_confirmed:Espaços dos auxiliares elegíveis","C:commit:Confirmo concluir a magia de círculo");
        fields("fr_faction","join",CONFIRM);
        fields("fr_faction","leave",CONFIRM);
        fields("fr_faction","adjust_renown",DELTA,GM);
        fields("fr_faction","relationship","T:relationship:Descrição do vínculo:!",CONFIRM);
        fields("fr_renown","adjust","T:deity:Nome da divindade:!",DELTA,GM);
        fields("fr_aurora","purchase","C:gold_spend_confirmed:Confirmo pagamento do preço verificado",CONFIRM);
        fields("fr_aurora","toggle","C:open:Anel ligado");
        fields("fr_aurora","deploy","C:magic_action_confirmed:Confirmo uso da ação mágica");
        fields("fr_aurora","teleport","T:sigil_sequence:Sequência do sigilo (ou use opção aleatória)","C:random_sequence:Usar sequência aleatória","C:magic_action_confirmed:Confirmo uso da ação mágica");
        fields("fr_aurora","cover");
        // Generic curated confirmations are always explicit; no boolean is switched on automatically.
        fields("fr_subclass","*",REACT,CONFIRM,"C:hit_confirmed:Ataque acertou","C:failed_save_confirmed:A resistência falhou","N:roll:Resultado manual da rolagem:0:999","T:target_id:Alvo (quando exigido)");
        fields("fr_feat","*",REACT,CONFIRM,"C:hit_confirmed:Ataque acertou","N:roll:Resultado manual da rolagem:0:999","T:target_id:Alvo (quando exigido)");
        fields("eb_feat","*",REACT,CONFIRM,"N:roll:Resultado manual da rolagem:0:999","T:target_id:Alvo (quando exigido)");
    }
    public static Action[] actions(String kind){Action[] found=ACTIONS.get(kind);return found==null?new Action[0]:found.clone();}
    public static boolean isAllowed(String kind,String action){for(Action a:actions(kind))if(a.id.equals(action))return true;return false;}
    private static Field parse(String x){String[] p=x.split(":",-1);if(p.length<3)throw new IllegalStateException("Invalid schema: "+x);
        String t=p[0];String type="C".equals(t)?"boolean":"N".equals(t)?"integer":"A".equals(t)?"array":"text";
        boolean required=p.length>3&&"!".equals(p[p.length-1]);
        int last=p.length-(required?1:0);
        int min=Integer.MIN_VALUE,max=Integer.MAX_VALUE;
        if("integer".equals(type)){
            if(last<5)throw new IllegalStateException("Invalid numeric schema: "+x);
            min=Integer.parseInt(p[last-2]);max=Integer.parseInt(p[last-1]);
            last-=2;
        }
        StringBuilder label=new StringBuilder(p[2]);
        for(int i=3;i<last;i++)label.append(":").append(p[i]);
        return new Field(p[1],label.toString(),type,required,min,max);
    }
    public static Field[] fields(String kind,String action,String option){
        if(!isAllowed(kind,action))return new Field[0];
        String data=FIELDS.get(kind+":"+action);
        if(data==null)data=FIELDS.get(kind+":*");
        ArrayList<Field> out=new ArrayList<>();
        if(data!=null&&!data.isEmpty())for(String f:data.split(";"))out.add(parse(f));
        if("fr_spell".equals(kind)){
            if("Spellfire Flare".equals(option)){out.add(parse("N:attack_roll:Total do ataque:-20:150:!"));out.add(parse("N:damage_roll:Dano rolado:0:100000:!"));}
            if("Wardaway".equals(option)){out.add(parse("C:save_succeeded:O alvo passou na resistência"));out.add(parse("N:damage_roll:Dano rolado:0:100000:!"));}
            if("Deryan's Helpful Homunculi".equals(option))out.add(parse("C:material_cost_100gp_confirmed:Material de 100 po gasto"));
            if("Laeral's Silver Lance".equals(option)){out.add(parse("C:dex_save_succeeded:Alvo passou na resistência de Destreza"));out.add(parse("N:damage_roll:Dano rolado:0:100000:!"));}
            if("Backlash".equals(option))out.add(parse("N:damage_reduction_roll:Redução de dano rolada:0:100000:!"));
            if("Songol's Elemental Suffusion".equals(option))out.add(parse("T:element:Tipo de elemento:!"));
        }
        return out.toArray(new Field[0]);
    }
}
