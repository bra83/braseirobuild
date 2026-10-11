package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persisted PHB 2024 prepared-spell state machine, stored inside characters.sheet_json.
 * No schema migration, RAG, Gemini, manual JSON, or trusting client-side checkboxes.
 * Each class owns its preparation, even when multiclass slots are shared.
 */
public final class Core2024PreparedSpellLedger {
    public static final String FIELD="core2024_prepared_spell_ledger";
    private Core2024PreparedSpellLedger(){}
    public static final class ClassEntry {
        public final String name;public final int level;
        public ClassEntry(String name,int level){this.name=name;this.level=level;}
    }
    public static List<ClassEntry> classes(JSONObject character,JSONObject sheet){
        ArrayList<ClassEntry> result=new ArrayList<ClassEntry>();
        if(character==null||sheet==null)return result;
        JSONArray a=sheet.optJSONArray("class_levels");
        if(a!=null){
            for(int i=0;i<a.length();i++){
                JSONObject x=a.optJSONObject(i);if(x==null)return Collections.emptyList();
                result.add(new ClassEntry(Core2024CastingEligibility.baseClass(
                    x.optString("class_name",x.optString("class",""))),x.optInt("level",0)));
            }
        }else{
            String raw=character.optString("class_name","");
            if(raw.matches(".*[/+,].*"))return Collections.emptyList();
            result.add(new ClassEntry(Core2024CastingEligibility.baseClass(raw),character.optInt("level",0)));
        }
        if(!validClasses(result,character.optInt("level",0)))return Collections.emptyList();
        return result;
    }
    public static boolean validClasses(List<ClassEntry> entries,int characterLevel){
        if(entries==null||entries.isEmpty()||characterLevel<1||characterLevel>20)return false;
        int total=0;Set<String> seen=new HashSet<String>();
        for(ClassEntry e:entries){
            if(e==null||(!Core2024PreparedSpellRules.supportedClass(e.name)&&
                 !"fighter".equals(e.name)&&!"rogue".equals(e.name)&&
                 !"monk".equals(e.name)&&!"barbarian".equals(e.name)&&
                 !"warlock".equals(e.name))||
                e.level<1||e.level>20||!seen.add(e.name))return false;
            total+=e.level;
        }
        return total==characterLevel;
    }
    public static JSONObject read(JSONObject sheet){
        JSONObject state=sheet==null?null:sheet.optJSONObject(FIELD);
        return state==null?new JSONObject():state;
    }
    private static JSONObject entries(JSONObject ledger)throws Exception{
        JSONObject all=ledger.optJSONObject("classes");
        if(all==null){all=new JSONObject();ledger.put("classes",all);}return all;
    }
    private static JSONObject entry(JSONObject ledger,String cls){return ledger.optJSONObject("classes")==null?
        null:ledger.optJSONObject("classes").optJSONObject(cls);}
    public static List<String> list(JSONArray a){
        ArrayList<String> out=new ArrayList<String>();
        if(a!=null)for(int i=0;i<a.length();i++)out.add(a.optString(i,""));
        return out;
    }
    private static JSONArray array(List<String> list){return new JSONArray(list);}
    private static boolean contains(JSONArray a,String key){
        if(a==null)return false;
        for(int i=0;i<a.length();i++)if(key.equals(a.optString(i)))return true;
        return false;
    }
    private static JSONObject fail(String reason){
        JSONObject result=new JSONObject();try{result.put("preparation_error",reason);}catch(Exception ignored){}
        return result;
    }
    private static ClassEntry find(List<ClassEntry> classes,String cls){
        if(classes!=null)for(ClassEntry c:classes)if(c.name.equals(cls))return c;
        return null;
    }
    /** No mutation: safe for UI previews and cast guards. */
    public static JSONObject preview(JSONObject sheet,List<ClassEntry> classes){
        JSONObject out=new JSONObject();
        try{
            if(classes==null||classes.isEmpty())return fail("class_levels_not_verified");
            JSONObject ledger=read(sheet),views=new JSONObject();
            for(ClassEntry c:classes){
                if(!Core2024PreparedSpellRules.supportedClass(c.name))continue;
                JSONObject stored=entry(ledger,c.name),view=new JSONObject();
                boolean initialized=stored!=null&&stored.optBoolean("configured",false);
                JSONObject window=stored==null?null:stored.optJSONObject("window");
                boolean open=!initialized||window!=null&&window.optBoolean("open",false);
                String event=!initialized?"setup":window==null?"":window.optString("event","");
                int rev=stored==null?0:stored.optInt("revision",0);
                view.put("class",c.name).put("class_level",c.level)
                    .put("capacity",Core2024PreparedSpellRules.capacity(c.name,c.level))
                    .put("max_circle",Core2024PreparedSpellRules.maxCircle(c.name,c.level))
                    .put("configured",initialized).put("can_edit",open)
                    .put("window_event",event).put("window_id",!initialized?"setup":
                        window==null?"":window.optString("id",""))
                    .put("revision",rev).put("free_spell_grants",stored==null?0:stored.optInt("free_spell_grants",0))
                    .put("prepared",stored==null?new JSONArray():new JSONArray(
                        stored.optJSONArray("prepared")==null?"[]":stored.optJSONArray("prepared").toString()))
                    .put("spellbook",stored==null?new JSONArray():new JSONArray(
                        stored.optJSONArray("spellbook")==null?"[]":stored.optJSONArray("spellbook").toString()))
                    .put("baseline",window==null?new JSONArray():new JSONArray(
                        window.optJSONArray("baseline")==null?"[]":window.optJSONArray("baseline").toString()));
                views.put(c.name,view);
            }
            return out.put("preparation_valid",true).put("classes",views);
        }catch(Exception e){return fail("preparation_preview_failed");}
    }
    /** Event is only called from an actual committed long rest or a successful level-up. */
    public static JSONObject openEvent(JSONObject sheet,List<ClassEntry> classes,String event,String eventId){
        if(sheet==null||classes==null||classes.isEmpty()||eventId==null||eventId.isEmpty()||
            !("long_rest".equals(event)||"short_rest".equals(event)||"level_up".equals(event)))return sheet;
        try{
            // Work on an isolated snapshot.  A malformed later class must not leave
            // an earlier class with an opened window/revision (atomic rollback).
            JSONObject ledger=new JSONObject(read(sheet).toString()),all=entries(ledger);
            for(ClassEntry c:classes){
                if(!Core2024PreparedSpellRules.supportedClass(c.name))continue;
                JSONObject e=all.optJSONObject(c.name);
                if(e==null)continue; // never silently mark an old character's spells prepared
                if(!e.optBoolean("configured",false))continue;
                if(!Core2024PreparedSpellRules.canOpen(c.name,event)||
                    "short_rest".equals(event)&&c.level<5)continue;
                JSONObject previous=e.optJSONObject("window");
                String windowId=event+":"+eventId+":"+c.name;
                if(previous!=null&&windowId.equals(previous.optString("id")))continue;
                // PHB 2024 p.147: every Wizard class level gained after first
                // grants TWO free spells, separately from copying found magic.
                // Class level, never total multiclass level, drives the grant.
                if("wizard".equals(c.name)&&"level_up".equals(event)){
                    int prior=e.optInt("class_level",c.level);
                    if(c.level>prior){
                        int grants=Math.min(38,(c.level-prior)*2);
                        e.put("free_spell_grants",Math.min(38,e.optInt("free_spell_grants",0)+grants));
                    }
                }
                JSONArray old=e.optJSONArray("prepared");
                e.put("window",new JSONObject().put("id",windowId).put("event",event)
                    .put("open",true).put("baseline",old==null?new JSONArray():new JSONArray(old.toString())));
                e.put("class_level",c.level).put("revision",e.optInt("revision",0)+1);
            }
            ledger.put("version",1);
            sheet.put(FIELD,ledger); // single commit after every class validated
        }catch(Exception ignored){}
        return sheet;
    }
    /** Atomic caller must verify character identity/profile, persist and roll back on errors. */
    public static JSONObject configure(JSONObject sheet,List<ClassEntry> classes,String cls,
        JSONArray selected,JSONArray book,boolean bookConfirmed,int expectedRevision,String expectedWindow){
        if(sheet==null||find(classes,cls)==null||
            !Core2024PreparedSpellRules.supportedClass(cls))return fail("class_not_owned");
        ClassEntry owned=find(classes,cls);
        try{
            JSONObject ledger=new JSONObject(read(sheet).toString()),all=entries(ledger);
            JSONObject e=all.optJSONObject(cls);boolean initialized=e!=null&&e.optBoolean("configured",false);
            if(e==null)e=new JSONObject();
            if(e.optInt("revision",0)!=expectedRevision)return fail("stale_preparation_revision");
            JSONObject window=e.optJSONObject("window");
            String event=initialized?(window==null?"":window.optString("event","")):"setup";
            String windowId=initialized?(window==null?"":window.optString("id","")):"setup";
            if(!windowId.equals(expectedWindow))return fail("stale_preparation_window");
            if(initialized&&(window==null||!window.optBoolean("open",false)))return fail("preparation_window_closed");
            if("wizard".equals(cls)&&e.optJSONObject(Core2024WizardBookCopy.PENDING)!=null)
                return fail("wizard_copy_in_progress");
            List<String> baseline=initialized?list(window.optJSONArray("baseline")):new ArrayList<String>();
            List<String> next=list(selected),books=list(book);
            String valid=Core2024PreparedSpellRules.validateSelection(cls,owned.level,baseline,next,event);
            if(!valid.isEmpty())return fail(valid);
            Set<String> bookSet=new HashSet<String>(books);
            if(bookSet.size()!=books.size())return fail("duplicate_spellbook_entry");
            if(!"wizard".equals(cls)&&!books.isEmpty())return fail("spellbook_only_wizard");
            for(String key:books){
                Core2024SpellRules.Spell sp=Core2024SpellRules.find(key);
                if(sp==null||!key.equals(sp.key)||!Core2024CastingEligibility.listed(key,cls)||
                    sp.level>Core2024PreparedSpellRules.maxCircle(cls,owned.level))return fail("invalid_spellbook_spell");
            }
            List<String> previousBook=list(e.optJSONArray("spellbook"));
            if(!bookSet.containsAll(previousBook))return fail("spellbook_entries_cannot_be_erased");
            int addedBookEntries=0;
            for(String key:books)if(!previousBook.contains(key))addedBookEntries++;
            if(addedBookEntries>0){
                if(!bookConfirmed)return fail("spellbook_source_unconfirmed");
                if(initialized){
                    if(!"level_up".equals(event)||e.optInt("free_spell_grants",0)<addedBookEntries)
                        return fail("wizard_spellbook_requires_paid_copy_or_verified_level_grant");
                    e.put("free_spell_grants",e.optInt("free_spell_grants",0)-addedBookEntries);
                }
            }
            if("wizard".equals(cls)&&!bookSet.containsAll(next))return fail("spell_not_in_wizard_spellbook");
            e.put("class_level",owned.level).put("prepared",array(next))
                .put("spellbook",array(books)).put("configured",true)
                .put("revision",expectedRevision+1);
            e.put("window",new JSONObject().put("id",windowId).put("event",event)
                .put("open",false).put("baseline",array(next)));
            all.put(cls,e);ledger.put("version",1).put("classes",all);
            sheet.put(FIELD,ledger);
            return new JSONObject().put("preparation_saved",true).put("class",cls)
                .put("prepared_count",next.size()).put("capacity",Core2024PreparedSpellRules.capacity(cls,owned.level))
                .put("revision",expectedRevision+1);
        }catch(Exception ex){return fail("preparation_write_failed");}
    }
    public static boolean isPrepared(JSONObject sheet,String cls,String spell){
        Core2024SpellRules.Spell sp=Core2024SpellRules.find(spell);
        JSONObject e=entry(read(sheet),cls);
        return sp!=null&&e!=null&&e.optBoolean("configured",false)&&
            contains(e.optJSONArray("prepared"),sp.key)&&
            (!"wizard".equals(cls)||contains(e.optJSONArray("spellbook"),sp.key));
    }
    /** Any successful cast closes the remaining preparation windows for this character. */
    public static JSONObject lockOnCast(JSONObject sheet){
        if(sheet==null)return sheet;
        try{
            JSONObject ledger=sheet.optJSONObject(FIELD),all=ledger==null?null:ledger.optJSONObject("classes");
            if(all==null)return sheet;
            java.util.Iterator<String> it=all.keys();
            while(it.hasNext()){
                JSONObject e=all.optJSONObject(it.next());if(e==null)continue;
                JSONObject w=e.optJSONObject("window");
                if(w!=null&&w.optBoolean("open",false)){
                    w.put("open",false);e.put("revision",e.optInt("revision",0)+1);
                }
            }
        }catch(Exception ignored){}return sheet;
    }
}
