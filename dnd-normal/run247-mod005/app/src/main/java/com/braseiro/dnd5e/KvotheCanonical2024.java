package com.braseiro.dnd5e;

import android.content.ContentValues;
import org.json.JSONArray;
import org.json.JSONObject;

/** Restore the player's original Human 2024 Kvothe without deleting saves,
 * acquired gear, spells, experience, world memories or 39 personality traits.
 * Fixed point-buy scores are restored only at level 1. */
public final class KvotheCanonical2024 {
    private KvotheCanonical2024(){}
    private static final String[] KEYS={"str","dex","con","int_score","wis","cha"};
    private static final int[] BASE={8,14,13,15,10,12};
    private static final int[] FINAL={8,16,13,15,10,13};

    private static JSONObject arrayToSkills()throws Exception{
        JSONObject result=new JSONObject();
        result.put("Arcanismo","class").put("Investigação","class")
            .put("Intuição","background:Andarilho")
            .put("Furtividade","background:Andarilho")
            .put("Atuação","human:Skillful");
        return result;
    }
    private static boolean humanAndCorrect(JSONObject h,JSONObject sheet){
        if(!"Humano".equalsIgnoreCase(h.optString("species")) ||
           !"Humano".equalsIgnoreCase(sheet.optString("species")))
            return false;
        if(h.optInt("level",1)>1)return sheet.optInt("kvothe_human_canonical_guard_version",0)==1;
        for(int i=0;i<KEYS.length;i++)
            if(h.optInt(KEYS[i],0)!=FINAL[i]||sheet.optInt(KEYS[i],0)!=FINAL[i])
                return false;
        return sheet.optInt("kvothe_human_canonical_guard_version",0)==1;
    }
    public static JSONObject restoreIfNeeded(AppDb db)throws Exception{
        JSONObject out=new JSONObject().put("changed",false);
        for(JSONObject hero:db.listActiveCharacters()){
            if(!"Kvothe".equalsIgnoreCase(hero.optString("name")))continue;
            long id=hero.optLong("id",0);
            if(id<=0)throw new IllegalStateException("Ficha Kvothe sem identidade");
            JSONObject sheet=new JSONObject(hero.optString("sheet_json","{}"));
            if(!Character2024SheetReconciler.applies(sheet))
                return out.put("reason","Kvothe fora do perfil 2024; nenhuma edicao aplicada");
            if(humanAndCorrect(hero,sheet))return out.put("reason","already_canonical");
            int level=hero.optInt("level",sheet.optInt("level",1));
            if(level<1)throw new IllegalStateException("Nivel Kvothe invalido");
            final JSONObject next=new JSONObject(sheet.toString());
            next.put("species","Humano").put("size","Médio")
                .put("species_choices_complete",true).put("species_choice_pending","")
                .put("human_skill_choice","Atuação").put("human_extra_origin_feat_pending",false)
                .put("resourceful_heroic_inspiration_on_long_rest",true)
                .put("origin_feat","Sortudo")
                .put("musician_instruments",new JSONArray().put("Alaúde").put("Flauta").put("Lira"))
                .put("kvothe_human_canonical_guard_version",1);
            // No extra infernal bloodline magic, trait or resistance survives a race change.
            for(String key:new String[]{"tiefling_lineage","tiefling_legacy","infernal_legacy",
                  "infernal_resistance","hellish_resistance","tiefling_spellcasting",
                  "tiefling_magic","fiendish_legacy","legacy_cantrip",
                  "legacy_level3_spell","legacy_level5_spell",
                  "thaumaturgy_cantrip","species_spellcasting_ability"})next.remove(key);
            // A racial Darkvision entry is not a general Human trait; remove
            // the old entry only when it was granted by the Tiefling ancestry.
            if("Tiferino".equalsIgnoreCase(hero.optString("species")) ||
                "Tiferino".equalsIgnoreCase(sheet.optString("species")))
                next.remove("darkvision_m");
            if(level==1){
                next.put("features","Humano 2024: Habilidoso (Atuação), Versátil (Músico), Engenhoso. "
                    +"Mago 2024: Conjuração, Adepto em Rituais, Recuperação Arcana. "
                    +"Andarilho 2024: Sortudo; Intuição, Furtividade, ferramentas de ladrão.");
                next.put("selected_origin_feats",
                    new JSONArray().put("Sortudo").put("Músico"));
                JSONObject base=new JSONObject(),bonus=new JSONObject();
                for(int i=0;i<KEYS.length;i++){
                    base.put(KEYS[i],BASE[i]);
                    next.put(KEYS[i],FINAL[i]);
                }
                bonus.put("dex",2).put("cha",1);
                next.put("ability_purchase",new JSONObject()
                    .put("method","point_buy_27").put("cost",27)
                    .put("base",base).put("background_bonus",bonus));
                next.put("origin_ability_choice_plus2","Destreza")
                    .put("origin_ability_choice_plus1","Carisma")
                    .put("origin_ability_choice_pending",false)
                    .put("origin_ability_scores_include_bonus",true)
                    .put("origin_feat_choice_pending",false)
                    .put("ac",13).put("hit_dice","1d6")
                    .put("spellcasting","Inteligência +2; CD de magia 12; ataque +4");
                JSONArray skills=new JSONArray();
                JSONObject wanted=arrayToSkills();
                for(String name:new String[]{"Arcanismo","Investigação","Intuição","Furtividade","Atuação"})
                    skills.put(new JSONObject().put("name",name)
                        .put("source",wanted.getString(name)).put("rank",1));
                next.put("skill_proficiencies",skills)
                    .put("saving_throw_proficiencies",new JSONArray()
                        .put(new JSONObject().put("name","Inteligência").put("source","class"))
                        .put(new JSONObject().put("name","Sabedoria").put("source","class")));
            }
            Character2024SheetReconciler.reconcile(next);
            final boolean first=level==1;
            db.runInTransactionChecked(new AppDb.CheckedTransaction(){
                @Override public void run()throws Exception{
                    ContentValues values=new ContentValues();
                    values.put("species","Humano");
                    if(first){
                        for(int i=0;i<KEYS.length;i++)values.put(KEYS[i],FINAL[i]);
                        values.put("ac",13);
                    }
                    if(db.getWritableDatabase().update("characters",values,
                            "id=? AND world_profile=?",
                            new String[]{String.valueOf(id),db.getActiveMapProfile()})!=1)
                        throw new IllegalStateException("Nao foi possivel restaurar especie Kvothe");
                    db.updateCharacterSheetChecked(id,next.toString());
                }
            });
            return out.put("changed",true).put("species","Humano")
                .put("level",level).put("personality_traits_preserved",
                    next.optJSONObject("personality_profile")!=null);
        }
        return out.put("reason","kvothe_not_found");
    }
}