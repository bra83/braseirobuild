package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashSet;
import java.util.Locale;

/**
 * Canonical 2024 student-NPC sheets, promoted to separate playable heroes only
 * at Marco do Percurso (D040) with player and NPC consent. No campus NPC is
 * silently turned into a hero, teleported, or added to the party.
 */
public final class StrixhavenFriends2024 {
    private StrixhavenFriends2024(){}

    private static final String[] NAMES={"Wilem","Simmon","Auri"};
    private static final String[] SPECIES={"Anão","Golias","Elfo"};
    private static final String[] CLASSES={"Bárbaro","Monge","Druida"};
    private static final String[] BACKGROUNDS={"Artesão","Marinheiro","Guia"};
    private static final int[] AGES={18,19,18};
    private static final int[][] BASE={{15,12,14,8,13,10},{10,15,13,8,14,12},{8,14,13,10,15,12}};
    private static final int[][] FINAL={{17,13,14,8,13,10},{10,17,13,8,15,12},{8,14,14,10,17,12}};
    private static final int[][] BONUS={{2,1,0,0,0,0},{0,2,0,0,1,0},{0,0,1,0,2,0}};
    private static final int[] HIT_DICE={12,8,8};
    private static final int[] INITIAL_HP={15,9,10};
    private static final int[] INITIAL_AC={13,15,15};
    private static final double[] INITIAL_GOLD={65,61,59};
    private static final String[] ORIGIN_FEATS={"Artifista","Valentão de Taverna","Iniciado em Magia (Druida)"};
    private static final String[] FUTURE_SUBCLASSES={
        "Caminho da Árvore do Mundo","Caminho das Sombras","Círculo da Terra"
    };
    private static final String[][] CLASS_SKILLS={
        {"Atletismo","Sobrevivência"},{"Furtividade","Intuição"},{"Natureza","Adestrar Animais"}
    };
    private static final String[][] BACKGROUND_SKILLS={
        {"Investigação","Persuasão"},{"Acrobacia","Percepção"},{"Furtividade","Sobrevivência"}
    };
    private static final String[] EXTRA_ELF_SKILLS={"Percepção"};
    private static final String[][] SAVE_SKILLS={
        {"Força","Constituição"},{"Força","Destreza"},{"Inteligência","Sabedoria"}
    };
    private static final String[] ABILITY_KEYS={"str","dex","con","int_score","wis","cha"};
    private static final String[] ABILITY_NAMES={"Força","Destreza","Constituição","Inteligência","Sabedoria","Carisma"};
    private static final String[] ALL_SKILLS={
        "Acrobacia","Adestrar Animais","Arcanismo","Atletismo","Atuação",
        "Enganação","Furtividade","História","Intimidação","Intuição",
        "Investigação","Medicina","Natureza","Percepção","Persuasão",
        "Prestidigitação","Religião","Sobrevivência"
    };
    private static final int[] SKILL_ABILITIES={1,4,3,0,5,5,1,3,5,4,3,4,3,4,5,1,3,4};

    private static String signed(int n){return n<0?String.valueOf(n):"+"+n;}
    private static int mod(int score){return Math.floorDiv(score-10,2);}
    private static int pointCost(int n){
        switch(n){
            case 8:return 0;case 9:return 1;case 10:return 2;case 11:return 3;
            case 12:return 4;case 13:return 5;case 14:return 7;case 15:return 9;
            default:throw new IllegalArgumentException("Atributo fora de point-buy 2024");
        }
    }
    private static int index(String name){
        for(int i=0;i<NAMES.length;i++)if(NAMES[i].equalsIgnoreCase(name))return i;
        return -1;
    }

    /** Verifiable level-one 2024 point-buy sheet. The NPC is NOT a party hero. */
    public static JSONObject initialSheet(String name)throws Exception{
        int i=index(name);if(i<0)throw new IllegalArgumentException("Amigo desconhecido");
        JSONObject sheet=new JSONObject(),base=new JSONObject(),bonus=new JSONObject();
        int cost=0;
        for(int x=0;x<6;x++){
            cost+=pointCost(BASE[i][x]);
            if(FINAL[i][x]!=BASE[i][x]+BONUS[i][x])
                throw new IllegalStateException("Bonus de antecedente divergente");
            base.put(ABILITY_KEYS[x],BASE[i][x]);
            if(BONUS[i][x]>0)bonus.put(ABILITY_KEYS[x],BONUS[i][x]);
            sheet.put(ABILITY_KEYS[x],FINAL[i][x]);
        }
        if(cost!=27)throw new IllegalStateException("Compra de atributos nao soma 27");
        sheet.put("name",NAMES[i]).put("species",SPECIES[i])
            .put("class_name",CLASSES[i]).put("background",BACKGROUNDS[i])
            .put("age",AGES[i]).put("level",1).put("rules_source","5e 2024")
            .put("rules_profile","5e 2024")
            .put("ability_purchase",new JSONObject().put("method","point_buy_27")
                .put("cost",27).put("base",base).put("background_bonus",bonus))
            .put("origin_ability_choice_pending",false)
            .put("origin_ability_scores_include_bonus",true)
            .put("origin_feat_choice_pending",false)
            .put("origin_feat",ORIGIN_FEATS[i])
            .put("selected_origin_feats",new JSONArray().put(ORIGIN_FEATS[i]))
            .put("hp",INITIAL_HP[i]).put("max_hp",INITIAL_HP[i])
            .put("ac",INITIAL_AC[i]).put("hit_dice","1d"+HIT_DICE[i])
            .put("hit_dice_max",1).put("hit_dice_spent",0)
            .put("proficiency_bonus","+2")
            .put("initiative",signed(mod(FINAL[i][1])))
            .put("size","Médio")
            .put("speed_m",i==0?9d:10.5d)
            .put("speed",i==0?"9 m":"10,5 m")
            .put("class_levels",new JSONArray().put(new JSONObject()
                .put("class_name",CLASSES[i]).put("level",1)))
            .put("subclass_future_at_level_3",FUTURE_SUBCLASSES[i])
            .put("subclass_name","")
            .put("species_choices_complete",true)
            .put("species_choice_pending","")
            .put("languages","Comum, Anão, Élfico")
            .put("languages_pending",false)
            .put("inventory_currency_gp",INITIAL_GOLD[i])
            .put("equipment_class_option","A")
            .put("equipment_background_option","B")
            .put("npc_lifecycle",new JSONObject()
                .put("phase","pre_marco_npc").put("rendezvous_hex","D040")
                .put("sync_to_kvothe_before_marco",true)
                .put("independent_progression_after_marco",true))
            .put("progression",DndProgressionEngine.state(new JSONObject().put("level",1),1));

        JSONArray skills=new JSONArray(),saves=new JSONArray();
        LinkedHashSet<String> selected=new LinkedHashSet<String>();
        for(String v:CLASS_SKILLS[i]){selected.add(v);skills.put(new JSONObject()
            .put("name",v).put("source","class").put("rank",1));}
        for(String v:BACKGROUND_SKILLS[i]){selected.add(v);skills.put(new JSONObject()
            .put("name",v).put("source","background:"+BACKGROUNDS[i]).put("rank",1));}
        if(i==2)for(String v:EXTRA_ELF_SKILLS){
            selected.add(v);skills.put(new JSONObject().put("name",v)
                .put("source","elf:KeenSenses").put("rank",1));
        }
        sheet.put("skill_proficiencies",skills);
        LinkedHashSet<String> saveNames=new LinkedHashSet<String>();
        for(String v:SAVE_SKILLS[i]){saveNames.add(v);saves.put(new JSONObject()
            .put("name",v).put("source","class"));}
        sheet.put("saving_throw_proficiencies",saves);
        StringBuilder skillText=new StringBuilder(),saveText=new StringBuilder();
        for(int s=0;s<ALL_SKILLS.length;s++){
            if(s>0)skillText.append(" • ");
            boolean trained=selected.contains(ALL_SKILLS[s]);
            skillText.append(ALL_SKILLS[s]).append(" ")
                .append(signed(mod(FINAL[i][SKILL_ABILITIES[s]])+(trained?2:0)));
            if(trained)skillText.append(" ★");
        }
        for(int a=0;a<6;a++){
            if(a>0)saveText.append(" • ");
            boolean trained=saveNames.contains(ABILITY_NAMES[a]);
            saveText.append(ABILITY_NAMES[a]).append(" ")
                .append(signed(mod(FINAL[i][a])+(trained?2:0)));
            if(trained)saveText.append(" ★");
        }
        sheet.put("skills",skillText.toString()).put("saving_throws",saveText.toString());
        sheet.put("passive_senses","Percepção "+(10+mod(FINAL[i][4])+(selected.contains("Percepção")?2:0))+
            " • Investigação "+(10+mod(FINAL[i][3])+(selected.contains("Investigação")?2:0))+
            " • Intuição "+(10+mod(FINAL[i][4])+(selected.contains("Intuição")?2:0)));

        JSONArray inventory=new JSONArray();
        if(i==0){
            addItem(inventory,"machado_grande","Machado grande",1,"class_2024_option_A");
            addItem(inventory,"machadinhas","Machadinha",4,"class_2024_option_A");
            addItem(inventory,"pacote_explorador","Pacote de explorador",1,"class_2024_option_A");
            addItem(inventory,"caldeirao_wilem","Caldeirão de Wilem",1,"history_confirmed_preexisting");
            addItem(inventory,"utensilios_cozinha","Utensílios de cozinheiro",1,"history_confirmed_preexisting");
            sheet.put("tools","Utensílios de cozinheiro; ferramentas de ferreiro; ferramentas de carpinteiro")
                .put("proficiencies","Armas simples e marciais; armaduras leves e médias; escudos; utensílios de cozinheiro.")
                .put("features","Anão 2024: PV adicionais iguais ao nível; visão no escuro 36 m; Conhecimento das Pedras. Bárbaro 2024: Fúria, Defesa sem Armadura, Maestria em Armas. Artesão: Artifista.");
        }else if(i==1){
            addItem(inventory,"lanca","Lança",1,"class_2024_option_A");
            addItem(inventory,"adagas","Adaga",5,"class_2024_option_A");
            addItem(inventory,"flauta","Flauta",1,"class_2024_option_A");
            addItem(inventory,"pacote_explorador","Pacote de explorador",1,"class_2024_option_A");
            sheet.put("tools","Flauta; ferramentas de navegador")
                .put("proficiencies","Armas simples e marciais Leves; flauta; ferramentas de navegador.")
                .put("features","Golias 2024: Ancestralidade Gigante (Nuvem), porte Médio no nível 1. Monge 2024: Artes Marciais, Defesa sem Armadura. Marinheiro: Brigão de Taverna.");
        }else{
            addItem(inventory,"armadura_couro","Armadura de couro",1,"class_2024_option_A");
            addItem(inventory,"escudo","Escudo",1,"class_2024_option_A");
            addItem(inventory,"foice","Foice",1,"class_2024_option_A");
            addItem(inventory,"bordao_foco","Bordão (foco druídico)",1,"class_2024_option_A");
            addItem(inventory,"pacote_explorador","Pacote de explorador",1,"class_2024_option_A");
            addItem(inventory,"kit_herbalismo","Kit de herbalismo",1,"class_2024_option_A");
            sheet.put("tools","Kit de herbalismo; ferramentas de cartógrafo")
                .put("proficiencies","Armas simples e marciais (Ordem Primal Guardião); armadura leve e média; escudos; kit de herbalismo; ferramentas de cartógrafo.")
                .put("features","Elfa da Floresta 2024: Transe, Sentidos Aguçados (Percepção), Druidismo. Druida 2024: Conjuração, Druídico, Ordem Primal (Guardião: armas marciais, armaduras médias). Guia: Iniciado em Magia (Druida).")
                .put("druid_primal_order_choice","Guardião")
                .put("druid_medium_armor_training",true)
                .put("druid_martial_weapon_training",true)
                .put("spell_slots_max",new JSONArray().put(2).put(0).put(0).put(0).put(0).put(0).put(0).put(0).put(0))
                .put("spell_slots_current",new JSONArray().put(2).put(0).put(0).put(0).put(0).put(0).put(0).put(0).put(0))
                .put("spells","Truques: Orientação, Produzir Chamas. Preparadas: Curar Ferimentos, Emaranhar, Fogo Feérico, Falar com Animais. Iniciado em Magia: Resistência, Bordão Místico, Palavra Curativa.")
                .put("spellcasting","Sabedoria +3; CD 13; ataque +5; 2 espaços de 1º círculo.");
        }
        sheet.put("inventory_ledger",inventory).put("inventory_ledger_status","structured_entries_only");
        sheet.put("inventory","Equipamento inicial do pacote de classe A; antecedente B (50 PO); itens pessoais identificados")
            .put("equipment",sheet.optString("inventory"));
        if(i==1)sheet.put("monk_class_tool_choice","Flauta");
        // Every recorded starting choice is run through the compiled PHB 2024
        // interpreter, rather than being claimed only in human-readable text.
        if(i==0)sheet.put("dwarven_toughness_applied_level",1);
        Core2024EberronCharacterEngine.apply2024SpeciesMechanics(sheet);
        JSONObject speciesChoice=new JSONObject();
        if(i==1)speciesChoice.put("ancestry","Nuvem");
        if(i==2)speciesChoice.put("lineage","Floresta")
            .put("ability","Sabedoria").put("skill","Percepção");
        if(i>0 && !Core2024EberronCharacterEngine
                .apply2024SpeciesChoice(sheet,speciesChoice).optBoolean("ok",false))
            throw new IllegalStateException("Escolha de espécie 2024 incompleta: "+NAMES[i]);
        sheet.put("selected_origin_feats",new JSONArray());
        JSONObject featChoice=new JSONObject();
        if(i==0)featChoice.put("tools",new JSONArray()
            .put("Utensílios de cozinheiro")
            .put("Ferramentas de ferreiro")
            .put("Ferramentas de carpinteiro"));
        if(i==2)featChoice.put("list","Druida").put("ability","Sabedoria")
            .put("cantrips",new JSONArray().put("Resistência").put("Bordão Místico"))
            .put("first_level_spell","Palavra Curativa");
        if(!Core2024EberronCharacterEngine.applyOriginFeat(sheet,
                ORIGIN_FEATS[i],featChoice).optBoolean("ok",false))
            throw new IllegalStateException("Talento 2024 incompleto: "+NAMES[i]);
        if(i==0)sheet.put("selected_weapon_masteries",new JSONArray()
            .put("Machado grande").put("Machadinha"))
            .put("weapon_masteries_known",2);
        Core2024EberronCharacterEngine.apply2024SpeciesMechanics(sheet);
        JSONObject complete=CharacterResourceStateEngine.ensure(sheet,CLASSES[i],1,"5e 2024");
        Character2024SheetReconciler.reconcile(complete);
        if(!complete.optBoolean("creation_mechanically_ready",false))
            throw new IllegalStateException("Ficha 2024 ainda pendente: "+NAMES[i]+
                " "+complete.optJSONArray("creation_missing_choices"));
        return complete;
    }
    private static void addItem(JSONArray inventory,String id,String name,int amount,String source)throws Exception{
        inventory.put(new JSONObject().put("id",id).put("name",name).put("quantity",amount)
            .put("source",source));
    }

    /** A deliberately selected legal Ability Score Improvement feat for
     * autonomous NPCs, not for the player's Kvothe. No ability rises above 20.
     * Original point-buy provenance remains untouched; later ASIs are logged. */
    private static void chooseNpcAsi(JSONObject sheet,int index,int level)throws Exception{
        if(level!=4 && level!=8 && level!=12 && level!=16)return;
        int[][] preferences={{0,2,1,4,5,3},{1,4,2,0,5,3},{4,2,1,3,5,0}};
        JSONArray increases=new JSONArray();
        int beforeCon=mod(sheet.optInt("con",10));
        for(int point=0;point<2;point++){
            boolean assigned=false;
            for(int ability:preferences[index]){
                String key=ABILITY_KEYS[ability];
                int current=sheet.optInt(key,10);
                if(current>=20)continue;
                sheet.put(key,current+1);
                increases.put(ABILITY_NAMES[ability]);
                assigned=true;break;
            }
            if(!assigned)throw new IllegalStateException("Nenhum atributo elegivel para ASI");
        }
        int constitutionDelta=mod(sheet.optInt("con",10))-beforeCon;
        if(constitutionDelta>0){
            sheet.put("max_hp",sheet.optInt("max_hp",1)+level*constitutionDelta);
            sheet.put("hp",sheet.optInt("hp",1)+level*constitutionDelta);
        }
        JSONArray choices=sheet.optJSONArray("npc_level_decisions");
        if(choices==null){choices=new JSONArray();sheet.put("npc_level_decisions",choices);}
        choices.put(new JSONObject().put("level",level)
            .put("feat","Incremento no Valor de Habilidade")
            .put("ability_increases",increases).put("source","PHB2024_NPC_autonomous"));
    }

    /**
     * Once D040 has promoted the NPC, individual confirmed achievements award
     * XP to ONLY this hero. No party-wide level mirroring or automatic choices.
     * Receipt replay is blocked inside the existing backed-up sheet JSON.
     */
    public static JSONObject awardIndependentHeroXp(AppDb db,final long characterId,
            final int points,final String achievement,final String receipt)throws Exception{
        if(db==null||characterId<=0||points<1||points>100000)
            throw new IllegalArgumentException("Experiencia ou heroi invalido");
        if(achievement==null||achievement.trim().length()<8||
            receipt==null||receipt.trim().length()<12)
            throw new IllegalArgumentException("Feito confirmado e recibo unico obrigatorios");
        JSONObject hero=db.getCharacter(characterId);
        if(hero.length()==0||index(hero.optString("name"))<0||
           db.findActivePartyHero(characterId).length()==0)
            throw new IllegalStateException("XP independente reservado aos herois recrutados em D040");
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){
            @Override public void run()throws Exception{
                JSONObject current=db.getCharacter(characterId);
                JSONObject sheet=new JSONObject(current.optString("sheet_json","{}"));
                JSONObject lifecycle=sheet.optJSONObject("npc_lifecycle");
                if(lifecycle==null||!"independent_hero".equals(lifecycle.optString("phase")))
                    throw new IllegalStateException("Heroi nao foi promovido em D040");
                JSONArray history=sheet.optJSONArray("independent_achievement_receipts");
                if(history==null)history=new JSONArray();
                for(int i=0;i<history.length();i++)
                    if(receipt.equals(history.optString(i)))
                        throw new IllegalStateException("Feito ja recebeu experiencia");
                JSONObject awarded=DndProgressionEngine.awardXp(sheet,points);
                if(awarded==null)throw new IllegalStateException("XP invalida");
                history.put(receipt);
                awarded.put("independent_achievement_receipts",history);
                JSONArray deeds=awarded.optJSONArray("individual_achievements");
                if(deeds==null)deeds=new JSONArray();
                deeds.put(new JSONObject().put("receipt",receipt)
                    .put("event",achievement.trim()).put("xp",points)
                    .put("level_at_event",current.optInt("level",1))
                    .put("day",db.getSceneState().optInt("day_number",1)));
                awarded.put("individual_achievements",deeds);
                awarded.put("progression",DndProgressionEngine.state(
                    awarded,current.optInt("level",1)));
                db.updateCharacterSheetChecked(characterId,awarded.toString());
                db.addJournalFact("Feito individual: "+current.optString("name"),
                    achievement.trim()+"; "+points+" XP",
                    "progressao",receipt,"strixhaven_friends");
                out[0]=new JSONObject().put("character_id",characterId)
                    .put("xp_after",awarded.optInt("xp"))
                    .put("level",current.optInt("level",1))
                    .put("level_up_pending",awarded.getJSONObject("progression")
                        .optBoolean("level_up_pending"));
            }
        });
        return out[0];
    }

    public static JSONArray studentNames(){return new JSONArray().put("Wilem").put("Simmon").put("Auri");}

    /** Keep NPCs persistent outside the party; never replace memories or move them. */
    public static JSONArray syncBeforeMarco(AppDb db,int kvotheLevel)throws Exception{
        if(kvotheLevel<1||kvotheLevel>20)throw new IllegalArgumentException("Nível do protagonista inválido");
        JSONArray result=new JSONArray();
        for(String name:NAMES){
            if(activeHeroNamed(db,name))continue; // Already promoted: no more synchronization.
            JSONObject npc=db.findNpcMemoryByName(name);
            JSONObject stats=new JSONObject(npc.optString("stats_json","{}"));
            JSONObject sheet=stats.optJSONObject("strixhaven_2024_sheet");
            if(sheet==null)sheet=initialSheet(name);
            if(sheet.optInt("level",1)>kvotheLevel)
                throw new IllegalStateException("NPC ultrapassou o protagonista antes de D040: "+name);
            if(sheet.optInt("level",1)<kvotheLevel){
                JSONObject upgraded=new JSONObject(sheet.toString());
                while(upgraded.optInt("level",1)<kvotheLevel){
                    int index=index(name);
                    JSONObject transition=CharacterLevelUpTransitionEngine.apply(upgraded,
                        CLASSES[index],HIT_DICE[index],mod(FINAL[index][2]),false,false);
                    if(!transition.optBoolean("accepted"))
                        throw new IllegalStateException("Progressão NPC inválida: "+name+
                            " "+transition.optString("level_up_blocked"));
                    upgraded=transition.getJSONObject("state_after");
                    int level=upgraded.optInt("level",1);
                    // Dwarf Toughness grants one additional hit point for every
                    // Barbarian level, including levels earned while still an NPC.
                    if(index==0){
                        upgraded.put("max_hp",upgraded.optInt("max_hp")+1);
                        upgraded.put("hp",upgraded.optInt("hp")+1);
                        upgraded.put("dwarven_toughness_applied_level",level);
                    }
                    if(level>=3){
                        String subclass=FUTURE_SUBCLASSES[index];
                        if(!Core2024SubclassRuntimeEngine.isSubclassSelectionValid(
                            CLASSES[index],level,subclass))
                            throw new IllegalStateException("Subclasse 2024 nao validada: "+subclass);
                        upgraded.put("subclass_name",subclass);
                    }
                    upgraded=CharacterResourceStateEngine.ensure(upgraded,
                        CLASSES[index],level,"5e 2024");
                    chooseNpcAsi(upgraded,index,level);
                    // A level-matched NPC enters the hero phase at the actual
                    // 2024 XP floor of its current level, not with zero XP.
                    upgraded.put("xp",Math.max(upgraded.optInt("xp",0),
                        DndProgressionEngine.thresholdForLevel(level)));
                    upgraded.put("progression",DndProgressionEngine.state(upgraded,level));
                    if(level<=18){
                        // These NPCs make their own 2024 level choices. Levels
                        // 4/8/12/16 take the logged, legal Ability Score
                        // Improvement feat. No automatic PC choice is touched.
                        upgraded.put("level_up_choices_pending",false)
                            .put("pending_level_up_choices",new JSONArray());
                    }
                    Character2024SheetReconciler.reconcile(upgraded);
                }
                sheet=upgraded;
            }
            stats.put("strixhaven_2024_sheet",sheet);
            stats.put("strixhaven_friend_status","pre_marco_npc");
            stats.put("friend_of","Kvothe");
            // A real NPC needs usable combat/roleplay mechanics NOW, not only
            // a character sheet hidden inside stats_json awaiting D040.
            JSONObject abilities=new JSONObject();
            for(String abilityKey:ABILITY_KEYS)
                abilities.put(abilityKey,sheet.optInt(abilityKey,10));
            stats.put("level",sheet.optInt("level",1))
                .put("class_name",sheet.optString("class_name"))
                .put("species",sheet.optString("species"))
                .put("ability_scores",abilities)
                .put("saving_throw_proficiencies",
                    sheet.optJSONArray("saving_throw_proficiencies"))
                .put("skill_proficiencies",sheet.optJSONArray("skill_proficiencies"));
            // Preserve the NPC's existing wounds (missing HP) even when its
            // maximum grows through a real level-up. Re-rendering at the
            // same level must not heal or reset combat damage.
            int oldMax=Math.max(1,stats.optInt("max_hp",sheet.optInt("max_hp",1)));
            int oldHp=Math.max(0,Math.min(oldMax,stats.optInt("hp",oldMax)));
            int missingHp=oldMax-oldHp;
            int nextMax=Math.max(1,sheet.optInt("max_hp",1));
            stats.put("max_hp",nextMax)
                .put("hp",Math.max(0,nextMax-missingHp));
            if(!stats.has("ac"))stats.put("ac",sheet.optInt("ac",10));
            JSONObject updated=npc.length()>0?new JSONObject(npc.toString()):new JSONObject()
                .put("name",name).put("active",1)
                .put("last_seen","Strixhaven • Campus Central")
                .put("summary","Estudante e amigo de Kvothe; colega independente antes de D040");
            updated.put("stats_json",stats.toString());
            db.upsertNpcMemory(updated);
            result.put(new JSONObject().put("name",name)
                .put("kind","npc").put("level",sheet.optInt("level"))
                .put("requires_level_choices",sheet.optBoolean("level_up_choices_pending",false)));
        }
        return result;
    }
    private static boolean activeHeroNamed(AppDb db,String name){
        for(JSONObject c:db.listActiveCharacters())if(name.equalsIgnoreCase(c.optString("name")))
            return true;
        return false;
    }

    /** The transfer must not create heroes unless all three NPCs are there and
     * both sides explicitly consent. Level choices are not silently invented. */
    public static JSONObject promoteAtMarco(AppDb db,boolean playerConfirmed,
                                            boolean allAccepted)throws Exception{
        JSONObject result=new JSONObject().put("joined",false);
        if(!KvotheCampaignPartyBridge.atMarco(db.getSceneState(),db.getMapState())||
            KvotheCampaignPartyBridge.inCampus(db.getSceneState()))
            return result.put("reason","not_at_marco_d040");
        if(!playerConfirmed||!allAccepted)
            return result.put("reason","meeting_and_consent_not_confirmed");
        int kvotheLevel=0;
        for(JSONObject hero:new PartyDirector(db).activeHeroRoster())
            if("Kvothe".equalsIgnoreCase(hero.optString("name")))
                kvotheLevel=db.getCharacter(hero.optLong("id")).optInt("level",0);
        if(kvotheLevel<1)return result.put("reason","protagonist_sheet_missing");
        // Validate all three before writing anything.
        JSONArray all=new JSONArray();
        JSONArray present=new JSONArray(db.getSceneState().optString("present_npcs_json","[]"));
        for(String name:NAMES){
            JSONObject npc=db.findNpcMemoryByName(name);
            if(npc.length()==0||npc.optInt("active",0)!=1)
                return result.put("reason","student_npc_not_present").put("name",name);
            boolean witnessed=false;
            for(int i=0;i<present.length();i++){
                JSONObject entry=present.optJSONObject(i);
                String label=entry==null?present.optString(i):entry.optString("name");
                if(name.equalsIgnoreCase(label))witnessed=true;
            }
            if(!witnessed)return result.put("reason","student_npc_not_present").put("name",name);
            JSONObject stats=new JSONObject(npc.optString("stats_json","{}"));
            JSONObject source=stats.optJSONObject("strixhaven_2024_sheet");
            if(source==null)return result.put("reason","student_sheet_missing").put("name",name);
            JSONObject sheet=new JSONObject(source.toString());
            // Carry the NPC's actual injuries and defenses into the playable
            // hero. The canonical NPC sheet may still contain maximum HP.
            int maxHp=Math.max(1,sheet.optInt("max_hp",1));
            sheet.put("hp",Math.max(0,Math.min(maxHp,stats.optInt("hp",
                sheet.optInt("hp",maxHp)))));
            sheet.put("ac",stats.optInt("ac",sheet.optInt("ac",10)));
            if(sheet.optInt("level",0)!=kvotheLevel)
                return result.put("reason","student_level_mismatch").put("name",name)
                    .put("expected_level",kvotheLevel).put("actual_level",sheet.optInt("level",0));
            if(sheet.optBoolean("level_up_choices_pending",false))
                return result.put("reason","student_level_choices_pending").put("name",name);
            all.put(new JSONObject().put("name",name).put("npc",npc).put("sheet",sheet));
        }
        JSONArray ids=new JSONArray();
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){
            @Override public void run()throws Exception{
                PartyDirector party=new PartyDirector(db);
                for(int i=0;i<all.length();i++){
                    JSONObject record=all.getJSONObject(i);
                    JSONObject sheet=record.getJSONObject("sheet");
                    String name=record.getString("name");
                    long id=db.addCharacter(name,sheet.getString("species"),
                        sheet.getString("class_name"),sheet.getInt("level"),
                        sheet.getInt("hp"),sheet.getInt("max_hp"),sheet.getInt("ac"),
                        sheet.getInt("str"),sheet.getInt("dex"),sheet.getInt("con"),
                        sheet.getInt("int_score"),sheet.getInt("wis"),sheet.getInt("cha"),
                        true,"Reencontro consentido no Marco do Percurso D040");
                    if(id<=0)throw new IllegalStateException("Não foi possível criar a ficha de "+name);
                    JSONObject actual=new JSONObject(sheet.toString());
                    actual.put("control_mode","manual");
                    actual.put("npc_lifecycle",new JSONObject()
                        .put("phase","independent_hero").put("recruited_at","D040")
                        .put("sync_to_kvothe_before_marco",false)
                        .put("independent_progression_after_marco",true));
                    db.updateCharacterSheetChecked(id,actual.toString());
                    if(party.recruitHero(id,"marco_d040_player_choice")<=0)
                        throw new IllegalStateException("Recrutamento falhou: "+name);
                    db.saveCharacterControl(id,"manual","{}");
                    JSONObject npc=record.getJSONObject("npc");
                    JSONObject stats=new JSONObject(npc.optString("stats_json","{}"));
                    stats.put("strixhaven_friend_status","promoted_to_hero")
                        .put("promoted_character_id",id);
                    JSONObject archived=new JSONObject(npc.toString());
                    archived.put("active",0).put("stats_json",stats.toString());
                    db.upsertNpcMemory(archived);
                    ids.put(id);
                }
            }
        });
        return result.put("joined",true).put("hero_ids",ids)
            .put("control_mode","manual").put("independent_progression",true);
    }
}
