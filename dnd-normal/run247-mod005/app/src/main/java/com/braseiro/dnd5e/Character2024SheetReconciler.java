package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;

/**
 * Non-destructive 2024 sheet migration. Player selections (not default generator
 * choices) are the source of truth. Only derived modifiers and contradictory
 * pending flags are repaired. No changes to inventory, spells, identity, XP,
 * health, personality, campaign data, or SQLite identifiers.
 */
public final class Character2024SheetReconciler {
    private Character2024SheetReconciler(){}

    private static final String[] ABILITIES={
        "Força","Destreza","Constituição","Inteligência","Sabedoria","Carisma"
    };
    private static final String[] ABILITY_KEYS={
        "str","dex","con","int_score","wis","cha"
    };
    private static final String[] SKILLS={
        "Acrobacia","Adestrar Animais","Arcanismo","Atletismo","Atuação",
        "Enganação","Furtividade","História","Intimidação","Intuição",
        "Investigação","Medicina","Natureza","Percepção","Persuasão",
        "Prestidigitação","Religião","Sobrevivência"
    };
    private static final int[] SKILL_ABILITY={
        1,4,3,0,5,5,1,3,5,4,3,4,3,4,5,1,3,4
    };

    public static boolean applies(JSONObject sheet){
        if(sheet==null)return false;
        String edition=norm(sheet.optString("rules_source",""));
        return edition.contains("2024")||edition.contains("5 5");
    }
    private static String norm(String text){
        return Normalizer.normalize(text==null?"":text.toLowerCase(Locale.ROOT),
            Normalizer.Form.NFD).replaceAll("\\p{M}","")
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static boolean containsWord(String text,String name){
        String n=" "+norm(text)+" ", target=" "+norm(name)+" ";
        return n.contains(target);
    }
    private static int modifier(int score){return Math.floorDiv(score-10,2);}
    private static int pb(int level){return 2+(Math.max(1,Math.min(20,level))-1)/4;}
    private static String signed(int number){return number>=0?"+"+number:String.valueOf(number);}
    private static boolean knownSkill(String s){
        for(String skill:SKILLS)if(norm(skill).equals(norm(s)))return true;
        return false;
    }
    private static String canonicalSkill(String s){
        for(String skill:SKILLS)if(norm(skill).equals(norm(s)))return skill;
        return "";
    }
    private static String canonicalAbility(String s){
        for(String ability:ABILITIES)if(norm(ability).equals(norm(s)))return ability;
        return "";
    }
    private static int abilityIndex(String name){
        for(int i=0;i<ABILITIES.length;i++)if(norm(ABILITIES[i]).equals(norm(name)))return i;
        return -1;
    }
    private static boolean marker(String list,String name,String mark){
        for(String part:(list==null?"":list).split("[•\\n]")){
            if(norm(part).startsWith(norm(name)+" ")&&part.contains(mark))return true;
        }
        return false;
    }
    private static void addSkill(LinkedHashMap<String,JSONObject> selected,String name,String source,int rank)throws Exception{
        String skill=canonicalSkill(name);
        if(skill.isEmpty()||rank<=0)return;
        JSONObject existing=selected.get(skill);
        if(existing!=null){
            if(rank>existing.optInt("rank",1))existing.put("rank",rank);
            return;
        }
        selected.put(skill,new JSONObject().put("name",skill).put("source",source).put("rank",rank));
    }
    private static LinkedHashMap<String,JSONObject> readSkills(JSONObject sheet)throws Exception{
        LinkedHashMap<String,JSONObject> out=new LinkedHashMap<>();
        JSONArray explicit=sheet.optJSONArray("skill_proficiencies");
        if(explicit!=null){
            for(int i=0;i<explicit.length();i++){
                JSONObject row=explicit.optJSONObject(i);
                if(row!=null)addSkill(out,row.optString("name"),row.optString("source","player"),row.optInt("rank",1));
                else addSkill(out,explicit.optString(i),"player",1);
            }
            return out;
        }
        String skills=sheet.optString("skills","");
        for(String skill:SKILLS)
            if(marker(skills,skill,"◈"))addSkill(out,skill,"legacy_marked_expertise",2);
            else if(marker(skills,skill,"★"))addSkill(out,skill,"legacy_marked_proficiency",1);
        // Recover only positively identified choices, not the first entries of
        // the class's default pool. A descriptive 'proficiencies' list alone
        // is accepted for legacy exports that lost all star markers.
        String declared=sheet.optString("proficiencies","");
        if(!declared.trim().isEmpty()){
            for(String skill:SKILLS)
                if(containsWord(declared,skill))addSkill(out,skill,"legacy_declared_proficiency",1);
        }
        if(norm(sheet.optString("background")).equals("andarilho")){
            addSkill(out,"Intuição","background:Andarilho",1);
            addSkill(out,"Furtividade","background:Andarilho",1);
        }
        if(norm(sheet.optString("species")).equals("humano")){
            String human=sheet.optString("human_skill_choice","");
            if(human.isEmpty()){
                String features=sheet.optString("features","");
                // Only the explicit chosen skill in Human: Skillful counts.
                java.util.regex.Matcher m=java.util.regex.Pattern.compile(
                    "(?i)Habilidoso\\s*\\(([^)]+)\\)").matcher(features);
                if(m.find())human=m.group(1);
            }
            addSkill(out,human,"human:Skillful",1);
            if(!canonicalSkill(human).isEmpty())sheet.put("human_skill_choice",canonicalSkill(human));
        }
        return out;
    }
    private static JSONArray toArray(LinkedHashMap<String,JSONObject> values){
        JSONArray rows=new JSONArray();
        for(JSONObject row:values.values())rows.put(row);
        return rows;
    }
    private static LinkedHashSet<String> readSavingThrows(JSONObject sheet){
        LinkedHashSet<String> out=new LinkedHashSet<>();
        // Read-time repair ONLY for first-level 2024 sheets generated by the
        // affected app, and only if both saved entries claim CLASS provenance.
        // Never rewrite player-made or higher-level saving throw choices.
        if(Character2024ClassContract.isGeneratedLevelOne(sheet)){
            JSONArray rows=sheet.optJSONArray("saving_throw_proficiencies");
            String[] required=Character2024ClassContract.initialSavingThrows(sheet.optString("class_name"));
            if(rows!=null && rows.length()==2 && required!=null){
                boolean classOnly=true;
                for(int i=0;i<rows.length();i++){
                    JSONObject row=rows.optJSONObject(i);
                    if(row==null || !"class".equals(row.optString("source")))classOnly=false;
                }
                if(classOnly){
                    out.add(required[0]);
                    out.add(required[1]);
                    return out;
                }
            }
        }
        JSONArray explicit=sheet.optJSONArray("saving_throw_proficiencies");
        if(explicit!=null){
            for(int i=0;i<explicit.length();i++){
                JSONObject row=explicit.optJSONObject(i);
                String value=row!=null?row.optString("name"):explicit.optString(i);
                String ability=canonicalAbility(value);
                if(!ability.isEmpty())out.add(ability);
            }
            if(norm(sheet.optString("class_name")).startsWith("mago")){
                out.add("Inteligência");out.add("Sabedoria");
            }
            return out;
        }
        String saved=sheet.optString("saving_throws","");
        for(String ability:ABILITIES)if(marker(saved,ability,"★"))out.add(ability);
        // Saving throws are fixed class features rather than player options.
        if(norm(sheet.optString("class_name")).startsWith("mago")){
            out.add("Inteligência");
            out.add("Sabedoria");
        }
        return out;
    }
    private static void applyAbilityChoice(JSONObject sheet)throws Exception{
        JSONObject purchase=sheet.optJSONObject("ability_purchase");
        JSONObject base=purchase==null?null:purchase.optJSONObject("base");
        JSONObject bonus=purchase==null?null:purchase.optJSONObject("background_bonus");
        if(base==null||bonus==null)return;
        int plusTwo=-1,plusOne=-1;
        boolean coherent=true;
        int sum=0;
        for(int i=0;i<ABILITY_KEYS.length;i++){
            String key=ABILITY_KEYS[i];
            int n=bonus.optInt(key,0);
            if(n==2)plusTwo=i;
            else if(n==1)plusOne=i;
            else if(n!=0)coherent=false;
            sum+=n;
            if(!base.has(key)||!sheet.has(key)||sheet.optInt(key)!=base.optInt(key)+n)coherent=false;
        }
        if(!coherent||sum!=3||plusTwo<0||plusOne<0||plusTwo==plusOne)return;
        JSONArray allowed=Core2024EberronCharacterEngine.allowedBackgroundAbilities(sheet.optString("background"));
        boolean plusTwoValid=false,plusOneValid=false;
        for(int i=0;i<allowed.length();i++){
            if(norm(allowed.optString(i)).equals(norm(ABILITIES[plusTwo])))plusTwoValid=true;
            if(norm(allowed.optString(i)).equals(norm(ABILITIES[plusOne])))plusOneValid=true;
        }
        if(!plusTwoValid||!plusOneValid)return;
        sheet.put("origin_ability_choice_pending",false);
        sheet.put("origin_ability_choice_plus2",ABILITIES[plusTwo]);
        sheet.put("origin_ability_choice_plus1",ABILITIES[plusOne]);
        sheet.put("origin_ability_bonus","+2 "+ABILITIES[plusTwo]+"; +1 "+ABILITIES[plusOne]);
        sheet.put("origin_ability_scores_include_bonus",true);
    }
    private static void originFeats(JSONObject sheet)throws Exception{
        LinkedHashSet<String> feats=new LinkedHashSet<>();
        JSONArray current=sheet.optJSONArray("selected_origin_feats");
        if(current!=null)for(int i=0;i<current.length();i++){
            String item=current.optString(i).trim();
            if(!item.isEmpty())feats.add(item); // Never discard other 2024 origin feats.
        }
        String declared=sheet.optString("selected_feats","")+
            "; "+sheet.optString("features","");
        if(containsWord(declared,"Sortudo"))feats.add("Sortudo");
        // A generated description is not a completed Musician feat: three
        // instrument selections are mandatory before this can be granted.
        if(containsWord(declared,"Músico")&&!sheet.optBoolean("origin_feat_choice_pending",false))
            feats.add("Músico");
        if(norm(sheet.optString("background")).equals("andarilho"))
            feats.add("Sortudo");
        if(!feats.isEmpty()){
            JSONArray result=new JSONArray();
            for(String feat:feats)result.put(feat);
            sheet.put("selected_origin_feats",result);
            int proficiency=pb(sheet.optInt("level",1));
            if(feats.contains("Sortudo")){
                if(!sheet.has("luck_points_max"))sheet.put("luck_points_max",proficiency);
                // Never refill spent luck during an edit. Only initialize on an
                // explicitly fresh first-level sheet whose ledger has no value.
                JSONObject progress=sheet.optJSONObject("progression");
                boolean fresh=sheet.optInt("level",1)==1 &&
                    (progress==null||progress.optInt("xp",0)==0);
                if(fresh&&!sheet.has("luck_points"))
                    sheet.put("luck_points",proficiency);
            }
            if(feats.contains("Músico")&&!sheet.has("musician_inspiration_target_limit"))
                sheet.put("musician_inspiration_target_limit",proficiency);
        }
        if(norm(sheet.optString("species")).equals("humano")){
            String backgroundFeat=Core2024EberronCharacterEngine.originFeatForBackground(
                sheet.optString("background",""));
            int suffix=backgroundFeat.indexOf(" (");
            if(suffix>=0)backgroundFeat=backgroundFeat.substring(0,suffix);
            boolean extra=false;
            for(String feat:feats){
                // The Human's bonus feat is not necessarily Musician.
                if(!norm(feat).equals(norm(backgroundFeat)))extra=true;
            }
            if(extra)sheet.put("human_extra_origin_feat_pending",false);
            String size=norm(sheet.optString("size",""));
            String skill=canonicalSkill(sheet.optString("human_skill_choice",""));
            if((size.equals("medio")||size.equals("pequeno"))&&!skill.isEmpty()&&extra){
                sheet.put("species_choices_complete",true);
                sheet.put("species_choice_pending","");
            }
        }
    }
    private static void derive(JSONObject sheet,LinkedHashMap<String,JSONObject> skills,
                               LinkedHashSet<String> saves)throws Exception{
        int[] scores=new int[6];
        for(int i=0;i<scores.length;i++)scores[i]=sheet.optInt(ABILITY_KEYS[i],10);
        int proficiency=pb(sheet.optInt("level",1));
        JSONArray saveRows=new JSONArray();
        StringBuilder saveText=new StringBuilder();
        for(int i=0;i<ABILITIES.length;i++){
            boolean proficient=saves.contains(ABILITIES[i]);
            if(i>0)saveText.append(" • ");
            saveText.append(ABILITIES[i]).append(" ").append(signed(modifier(scores[i])+(proficient?proficiency:0)));
            if(proficient){
                saveText.append(" ★");
                saveRows.put(new JSONObject().put("name",ABILITIES[i]).put("source","class"));
            }
        }
        sheet.put("saving_throw_proficiencies",saveRows);
        sheet.put("saving_throws",saveText.toString());
        StringBuilder skillText=new StringBuilder();
        for(int i=0;i<SKILLS.length;i++){
            if(i>0)skillText.append(" • ");
            JSONObject selected=skills.get(SKILLS[i]);
            int rank=selected==null?0:Math.max(0,Math.min(2,selected.optInt("rank",1)));
            int bonus=modifier(scores[SKILL_ABILITY[i]])+rank*proficiency;
            skillText.append(SKILLS[i]).append(" ").append(signed(bonus));
            if(rank==2)skillText.append(" ◈");
            else if(rank==1)skillText.append(" ★");
        }
        sheet.put("skill_proficiencies",toArray(skills));
        sheet.put("skills",skillText.toString());
        sheet.put("passive_senses","Percepção "+
            (10+modifier(scores[4])+rank(skills,"Percepção")*proficiency)+
            " • Investigação "+(10+modifier(scores[3])+rank(skills,"Investigação")*proficiency)+
            " • Intuição "+(10+modifier(scores[4])+rank(skills,"Intuição")*proficiency));
    }
    private static int rank(LinkedHashMap<String,JSONObject> selected,String skill){
        JSONObject row=selected.get(skill);return row==null?0:Math.min(2,row.optInt("rank",1));
    }
    public static JSONObject reconcile(JSONObject sheet){
        if(!applies(sheet))return sheet;
        Character2024SheetConsistencyGuard.reconcile(sheet);
        try{
            LinkedHashMap<String,JSONObject> skills=readSkills(sheet);
            LinkedHashSet<String> saves=readSavingThrows(sheet);
            derive(sheet,skills,saves);
            applyAbilityChoice(sheet);
            originFeats(sheet);
            CharacterMageArmorState.reconcile(sheet);
            Character2024CreationReadiness.evaluate(sheet);
            sheet.put("sheet_2024_reconciler_version",2);
        }catch(Exception error){
            throw new IllegalStateException("A ficha 2024 não pôde ser reconciliada sem perda de dados.",error);
        }
        return sheet;
    }
    /** Explicit editor selections supersede legacy text, but never reallocate ASIs. */
    public static void applySelections(JSONObject sheet,boolean[] saves,int[] ranks,String[] names){
        if(!applies(sheet))return;
        try{
            LinkedHashSet<String> saveSet=new LinkedHashSet<>();
            for(int i=0;i<Math.min(saves.length,ABILITIES.length);i++)
                if(saves[i])saveSet.add(ABILITIES[i]);
            LinkedHashMap<String,JSONObject> original=readSkills(sheet);
            LinkedHashMap<String,JSONObject> selected=new LinkedHashMap<>();
            for(int i=0;i<Math.min(ranks.length,names.length);i++){
                String skill=canonicalSkill(names[i]);
                JSONObject old=original.get(skill);
                String source=(old!=null&&old.optInt("rank",1)==ranks[i])?
                    old.optString("source","player"): "player:editor";
                addSkill(selected,names[i],source,ranks[i]);
            }
            // The initial 2024 class fixes BOTH saving throw proficiencies.
            // The level-one editor must not turn a non-proficient save into a class save.
            if(sheet.optInt("level",1)==1){
                String[] fixed=Character2024ClassContract.initialSavingThrows(sheet.optString("class_name"));
                if(fixed!=null){
                    saveSet.clear();
                    saveSet.add(fixed[0]);
                    saveSet.add(fixed[1]);
                }
            }else if(norm(sheet.optString("class_name")).startsWith("mago")){
                saveSet.add("Inteligência");saveSet.add("Sabedoria");
            }
            if(norm(sheet.optString("background")).equals("andarilho")){
                addSkill(selected,"Intuição","background:Andarilho",1);
                addSkill(selected,"Furtividade","background:Andarilho",1);
            }
            if(norm(sheet.optString("species")).equals("humano")){
                String human=sheet.optString("human_skill_choice","");
                addSkill(selected,human,"human:Skillful",1);
            }
            derive(sheet,selected,saveSet);
            applyAbilityChoice(sheet);
            originFeats(sheet);
            CharacterMageArmorState.reconcile(sheet);
            Character2024CreationReadiness.evaluate(sheet);
        }catch(Exception failure){
            throw new IllegalStateException("Não foi possível registrar as escolhas de perícias e salvaguardas.",failure);
        }
    }
}
