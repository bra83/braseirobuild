package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;

/**
 * Safe legal choices for NEW fully random 2024 characters only.
 * Never run on an imported, edited or persisted character.
 * Every dice/feat/species choice is checked against the existing official runtime;
 * a failed choice blocks completion instead of guessing.
 */
public final class CharacterRandom2024Rules {
    private CharacterRandom2024Rules(){}
    private static final String[] KEYS={"str","dex","con","int_score","wis","cha"};
    private static final String[] ABILITIES={"Força","Destreza","Constituição","Inteligência","Sabedoria","Carisma"};
    private static final String[] SKILLS={"Acrobacia","Adestrar Animais","Arcanismo","Atletismo","Atuação","Enganação","Furtividade","História","Intimidação","Intuição","Investigação","Medicina","Natureza","Percepção","Persuasão","Prestidigitação","Religião","Sobrevivência"};
    private static final String[] CORE_CLASSES={"Bárbaro","Bardo","Bruxo","Clérigo","Druida","Feiticeiro","Guerreiro","Ladino","Mago","Monge","Paladino","Patrulheiro"};
    private static final String[] CORE_2024_SPECIES={"Aasimar","Anão","Draconato","Elfo","Gnomo","Golias","Humano","Orc","Pequenino","Tiferino"};
    private static final String[] CORE_2014_SPECIES={"Anão","Elfo","Gnomo","Halfling","Humano","Draconato","Meio-Elfo","Meio-Orc","Tiefling"};
    private static String n(String text){return Normalizer.normalize(text==null?"":text.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();}
    private static boolean same(String a,String b){return n(a).equals(n(b));}
    private static boolean includes(String[] values,String target){for(String x:values)if(same(x,target))return true;return false;}
    private static String choose(String[] values,Random random){return values[random.nextInt(values.length)];}
    private static String classBase(String value){int dash=value==null?-1:value.indexOf(" — ");return dash<0?value:value.substring(0,dash);}
    public static String[] legalClasses(String profile,String[] offered){
        ArrayList<String> out=new ArrayList<>();
        if(offered!=null)for(String x:offered){
            if(includes(CORE_CLASSES,classBase(x))&&!out.contains(classBase(x)))out.add(classBase(x));
        }
        return out.toArray(new String[0]);
    }
    public static String[] legalSpecies(String profile,String[] offered){
        String[] catalog=DndRulesEngine.PROFILE_2024.equals(DndRulesEngine.canonicalProfile(profile))?CORE_2024_SPECIES:CORE_2014_SPECIES;
        ArrayList<String> out=new ArrayList<>();
        if(offered!=null)for(String x:offered){
            // Preserve only exact, compiled PHB names; variants need their own source gates.
            if(includes(catalog,x)&&!out.contains(x))out.add(x);
        }
        return out.toArray(new String[0]);
    }
    private static boolean ok(JSONObject result,String label){
        if(!result.optBoolean("ok",false))throw new IllegalStateException(label+": "+result.optString("reason","opção incompatível"));
        return true;
    }
    private static String abilityKey(String name){
        for(int i=0;i<ABILITIES.length;i++)if(same(ABILITIES[i],name))return KEYS[i];
        throw new IllegalStateException("Atributo de antecedente não reconhecido: "+name);
    }
    public static void chooseBackgroundAbilitiesBeforeMechanics(JSONObject s,Random rng)throws Exception{
        JSONArray allowed=Core2024EberronCharacterEngine.allowedBackgroundAbilities(s.optString("background"));
        if(allowed==null||allowed.length()!=3)throw new IllegalStateException("Antecedente 2024 sem matriz completa de atributos");
        JSONObject base=new JSONObject(),bonus=new JSONObject();
        for(String key:KEYS)base.put(key,s.optInt(key,10));
        // Favor ability scores that improve this class without leaving the allowed set.
        ArrayList<String> ranked=new ArrayList<>();
        for(int i=0;i<allowed.length();i++)ranked.add(allowed.getString(i));
        java.util.Collections.shuffle(ranked,rng);
        java.util.Collections.sort(ranked,(a,b)->Integer.compare(s.optInt(abilityKey(b),10),s.optInt(abilityKey(a),10)));
        String plus2=ranked.get(0),plus1=ranked.get(1);
        ok(Core2024EberronCharacterEngine.applyOriginAbilityChoice(s,plus2,plus1,false),"Bônus 2024");
        bonus.put(abilityKey(plus2),2).put(abilityKey(plus1),1);
        s.put("ability_purchase",new JSONObject().put("method","random_standard_array")
            .put("base",base).put("background_bonus",bonus));
        s.put("random_origin_ability_plus2",plus2).put("random_origin_ability_plus1",plus1);
    }
    private static JSONObject featInput(String feat,Random rng)throws Exception{
        JSONObject input=new JSONObject();
        if(same(feat,"Artifista"))
            input.put("tools",new JSONArray().put("Ferramentas de ferreiro").put("Ferramentas de carpinteiro").put("Ferramentas de pedreiro"));
        else if(same(feat,"Habilidoso"))
            input.put("proficiencies",new JSONArray().put("Acrobacia").put("Percepção").put("Investigação"));
        else if(same(feat,"Músico"))
            input.put("instruments",new JSONArray().put("Flauta").put("Lira").put("Alaúde"));
        else if(same(feat,"Iniciado em Magia")){
            // Fixed, verified examples of two different cantrips and one level-1 spell.
            input.put("ability","Sabedoria");
            input.put("list","Mago");
            input.put("cantrips",new JSONArray().put("Mãos Mágicas").put("Prestidigitação"));
            input.put("first_level_spell","Mísseis Mágicos");
        }
        return input;
    }
    private static void addSkill(JSONObject sheet,String choice,String source)throws Exception{
        if(!includes(SKILLS,choice))throw new IllegalStateException("Perícia inválida: "+choice);
        JSONArray rows=sheet.optJSONArray("skill_proficiencies");
        JSONArray dst=rows==null?new JSONArray():new JSONArray(rows.toString());
        for(int i=0;i<dst.length();i++){
            JSONObject r=dst.optJSONObject(i);
            if(r!=null&&same(r.optString("name"),choice))return;
        }
        dst.put(new JSONObject().put("name",choice).put("source",source).put("rank",1));
        sheet.put("skill_proficiencies",dst);
    }
    private static String unchosenSkill(JSONObject sheet,Random rng){
        HashSet<String> used=new HashSet<>();
        JSONArray rows=sheet.optJSONArray("skill_proficiencies");
        if(rows!=null)for(int i=0;i<rows.length();i++){
            JSONObject r=rows.optJSONObject(i);if(r!=null)used.add(n(r.optString("name")));
        }
        ArrayList<String> choices=new ArrayList<>();
        for(String s:SKILLS)if(!used.contains(n(s)))choices.add(s);
        if(choices.isEmpty())throw new IllegalStateException("Sem perícia livre válida");
        return choices.get(rng.nextInt(choices.size()));
    }
    private static JSONObject speciesChoice(String species,JSONObject sheet,Random rng){
        JSONObject c=new JSONObject();
        try{
            if(same(species,"Humano"))return c.put("size",rng.nextBoolean()?"Médio":"Pequeno")
                .put("skill",unchosenSkill(sheet,rng));
            if(same(species,"Aasimar"))return c.put("size",rng.nextBoolean()?"Médio":"Pequeno");
            if(same(species,"Draconato"))return c.put("ancestry",choose(new String[]{"Vermelho","Azul","Verde","Branco","Negro"},rng));
            if(same(species,"Elfo"))return c.put("lineage",choose(new String[]{"Alto","Drow","Floresta"},rng))
                .put("ability",choose(new String[]{"Inteligência","Sabedoria","Carisma"},rng))
                .put("skill",choose(new String[]{"Intuição","Percepção","Sobrevivência"},rng));
            if(same(species,"Gnomo"))return c.put("lineage",choose(new String[]{"Floresta","Rocha"},rng));
            if(same(species,"Golias"))return c.put("ancestry",choose(new String[]{"Nuvem","Fogo","Gelo","Colina","Pedra","Tempestade"},rng));
            if(same(species,"Tiferino"))return c.put("size",rng.nextBoolean()?"Médio":"Pequeno")
                .put("ability",choose(new String[]{"Inteligência","Sabedoria","Carisma"},rng))
                .put("legacy",choose(new String[]{"Abissal","Ctônico","Infernal"},rng));
        }catch(Exception e){throw new IllegalStateException("Falha ao sortear escolha de espécie",e);}
        return c;
    }
    public static JSONObject finalizeNew2024(JSONObject sheet,Random rng){
        if(!DndRulesEngine.PROFILE_2024.equals(DndRulesEngine.canonicalProfile(sheet.optString("rules_source"))))
            return sheet;
        try{
            String species=sheet.optString("species"),cls=classBase(sheet.optString("class_name"));
            if(!includes(CORE_2024_SPECIES,species)||!includes(CORE_CLASSES,cls))
                throw new IllegalStateException("Espécie/classe fora do catálogo 2024 validado");
            String bgFeat=Core2024EberronCharacterEngine.originFeatForBackground(sheet.optString("background"));
            String baseFeat=bgFeat.contains(" (")?bgFeat.substring(0,bgFeat.indexOf(" (")):bgFeat;
            JSONArray selected=sheet.optJSONArray("selected_origin_feats");
            boolean already=false;
            if(selected!=null)for(int i=0;i<selected.length();i++)if(same(selected.optString(i),baseFeat))already=true;
            if(!already)ok(Core2024EberronCharacterEngine.applyOriginFeat(sheet,baseFeat,featInput(baseFeat,rng)),"Talento do antecedente");

            JSONObject choices=speciesChoice(species,sheet,rng);
            if(choices.length()>0){
                ok(Core2024EberronCharacterEngine.apply2024SpeciesChoice(sheet,choices),"Escolha da espécie");
                if(same(species,"Humano"))addSkill(sheet,choices.getString("skill"),"human:Skillful");
                if(same(species,"Elfo"))addSkill(sheet,choices.getString("skill"),"elf:KeenSenses");
            }
            if(same(species,"Humano")){
                // Extra feat must be DIFFERENT from the background feat. Choose a
                // no-input feat to avoid silently assigning unvalidated spells/tools.
                String[] extras={"Alerta","Atacante Selvagem","Curandeiro","Sortudo","Valentão de Taverna"};
                ArrayList<String> candidates=new ArrayList<>();
                for(String feat:extras)if(!same(feat,baseFeat))candidates.add(feat);
                String extra=candidates.get(rng.nextInt(candidates.size()));
                ok(Core2024EberronCharacterEngine.applyOriginFeat(sheet,extra,new JSONObject()),"Talento extra de Humano");
                sheet.put("human_extra_origin_feat_pending",false);
            }
            if(same(cls,"Monge")){
                String tool=choose(new String[]{"Flauta","Lira","Alaúde","Ferramentas de ferreiro"},rng);
                sheet.put("monk_class_tool_choice",tool);
                sheet.put("tools",sheet.optString("tools","")+"\nProficiência de Monge: "+tool);
                JSONArray tools=sheet.optJSONArray("tool_proficiencies");
                if(tools==null)tools=new JSONArray();
                tools.put(tool);
                sheet.put("tool_proficiencies",tools);
            }
            if(same(cls,"Bardo")){
                sheet.put("bard_starting_instrument","Flauta");
            }
            // Recorded as a new 2024 character's explicit background choice B.
            // It grants 50 GP, never the objects of equipment choice A.
            String inventory=sheet.optString("inventory","");
            String[] lines=inventory.split("\\n",2);
            String classKit=lines.length>0?lines[0]:"";
            if(same(cls,"Monge"))
                classKit=classKit.replace("ferramentas de artesão ou instrumento musical à escolha",
                    sheet.optString("monk_class_tool_choice"));
            if(same(cls,"Bardo"))
                classKit=classKit.replace("instrumento musical à escolha","Flauta");
            java.util.regex.Matcher coins=java.util.regex.Pattern.compile("(?i)(?:, )?(\\d+) PO$").matcher(classKit.trim());
            int classGp=0;
            if(coins.find()){
                classGp=Integer.parseInt(coins.group(1));
                classKit=classKit.substring(0,coins.start()).trim();
            }
            if(classKit.isEmpty()||classKit.contains(" à escolha")||classKit.contains(" ou "))
                throw new IllegalStateException("Equipamento inicial não possui opção única validada");
            sheet.put("inventory",classKit+"\nAntecedente "+sheet.optString("background")+
                ": opção B (50 PO, sem itens da opção A).\nMoedas iniciais totais: "+(classGp+50)+" PO.");
            sheet.put("equipment",sheet.optString("inventory"));
            JSONArray ledger=new JSONArray();
            for(String entry:classKit.split(",\\s*")){
                entry=entry.trim();if(entry.isEmpty())continue;
                int qty=1;String name=entry;
                java.util.regex.Matcher count=java.util.regex.Pattern.compile("^(\\d+)\\s+(.+)$").matcher(entry);
                if(count.matches()){qty=Integer.parseInt(count.group(1));name=count.group(2);}
                if(name.isEmpty()||qty<1)throw new IllegalStateException("Item inicial inválido");
                ledger.put(new JSONObject().put("id","starter_class_"+ledger.length())
                    .put("name",name).put("quantity",qty).put("accessible",true)
                    .put("source","class_2024_option_A"));
            }
            sheet.put("inventory_ledger",ledger);
            sheet.put("inventory_currency_gp",classGp+50);
            sheet.put("inventory_ledger_status","new_character_class_A_background_B");
            sheet.put("inventory_review_required",false);
            sheet.put("equipment_class_option","A").put("equipment_background_option","B");
            // New random character: two standard languages chosen from documented
            // core options. Deliberately no biography/traits are modified.
            sheet.put("languages","Comum, Anão, Élfico");
            sheet.put("languages_pending",false);
            Character2024SheetReconciler.reconcile(sheet);
            if(sheet.optInt("creation_missing_choice_count",0)!=0)
                throw new IllegalStateException("Escolhas mecânicas ainda pendentes: "+sheet.optJSONArray("creation_missing_choices"));
            return sheet;
        }catch(Exception e){
            try{sheet.put("generation_blocked_reason",e.getMessage()).put("creation_mechanically_ready",false)
                .put("creation_completion_status","requires_player_choices");}
            catch(Exception ignored){}
            return sheet;
        }
    }
    public static JSONObject certify(JSONObject sheet){
        try{
            String profile=DndRulesEngine.canonicalProfile(sheet.optString("rules_source"));
            JSONObject gate=new CharacterRulesValidator().validate(sheet,profile);
            JSONArray errors=gate.optJSONArray("errors");
            String reason=sheet.optString("generation_blocked_reason","");
            if(errors!=null&&errors.length()>0)
                reason=(reason.isEmpty()?"":reason+"; ")+errors.toString();
            if(!sheet.optBoolean("creation_mechanically_ready",true) && DndRulesEngine.PROFILE_2024.equals(profile))
                reason=(reason.isEmpty()?"":reason+"; ")+"Escolhas obrigatórias pendentes";
            if(!reason.isEmpty()){
                sheet.put("generation_status","blocked");
                sheet.put("generation_valid",false);
                sheet.put("generation_blocked_reason",reason);
            }else{
                sheet.put("generation_status","mechanically_valid");
                sheet.put("generation_valid",true);
            }
        }catch(Exception error){
            try{sheet.put("generation_status","blocked").put("generation_valid",false)
                .put("generation_blocked_reason","Validação indisponível: "+error.getMessage());}
            catch(Exception ignored){}
        }
        return sheet;
    }
}
