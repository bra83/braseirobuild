package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pinned XMM grapple source contracts and explicit XPHB escape-check inputs. */
public final class XmmGrappleEscapeRulesTest {
    @Test public void eachSimpleXmmGrappleIsTypedAndAdditionalEffectsStayManual()throws Exception{
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        JSONArray rows=Xmm2025AttackConditionEngine.catalog();int grapples=0,manual=0,executable=0;
        String[] sizes={"Tiny","Small","Medium","Large","Huge","Gargantuan"};
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);if(!"grappled".equals(row.optString("condition")))continue;
            grapples++;String monster=row.getString("id"),name=row.getString("name");
            JSONObject compiled=CompiledStatblockEngine.runtimeProfile(monster).getJSONArray("actions")
                .getJSONObject(row.getInt("slot"));
            boolean valid=Xmm2025GrappleEscapeRules.validSource(monster,compiled);
            boolean specialMimicDisadvantage="Mimic".equals(monster)&&"Pseudopod".equals(name)&&
                compiled.optBoolean("escape_check_disadvantage",false);
            boolean linkedRestraint=Xmm2025GrappleEscapeRules.linkedRestraint(monster,compiled)&&
                compiled.optBoolean("linked_condition_bundle_until_grapple_ends",false);
            assertEquals(monster+" / "+name,!row.optBoolean("other")||specialMimicDisadvantage||linkedRestraint,valid);
            assertEquals(monster+" / "+name,row.getInt("dc"),compiled.optInt("escape_dc"));
            assertEquals(monster+" / "+name,row.getString("size").toLowerCase(java.util.Locale.ROOT),
                compiled.optString("condition_max_target_size"));
            if(valid){
                executable++;
                int maximum=-1;String declared=compiled.optString("condition_max_target_size");
                for(int sizeIndex=0;sizeIndex<sizes.length;sizeIndex++){
                    if(sizes[sizeIndex].toLowerCase(java.util.Locale.ROOT).equals(declared))maximum=sizeIndex;
                }
                assertTrue(monster+" / "+name+" has a valid maximum size",maximum>=0);
                for(int sizeIndex=0;sizeIndex<sizes.length;sizeIndex++){
                    assertEquals(monster+" / "+name+" / "+sizes[sizeIndex],sizeIndex<=maximum,
                        Xmm2025GrappleEscapeRules.targetEligible(monster,compiled,sizes[sizeIndex]));
                }
            }else manual++;
        }
        assertEquals(37,grapples);assertEquals(8,manual);assertEquals(29,executable);
    }

    @Test public void escapeUsesAnExplicitAthleticsOrAcrobaticsTotal()throws Exception{
        JSONObject athletics=Xmm2025GrappleEscapeRules.resolveEscape("athletics",13,13);
        assertTrue(athletics.toString(),athletics.optBoolean("resolved"));
        assertTrue(athletics.optBoolean("success"));assertEquals("action",athletics.optString("action_cost"));
        assertFalse(athletics.optBoolean("auto_rolls"));assertFalse(athletics.optBoolean("auto_mutation_allowed"));
        JSONObject acrobatics=Xmm2025GrappleEscapeRules.resolveEscape("acrobatics",12,13);
        assertTrue(acrobatics.optBoolean("resolved"));assertFalse(acrobatics.optBoolean("success"));
        JSONObject invalid=Xmm2025GrappleEscapeRules.resolveEscape("strength_save",20,13);
        assertFalse(invalid.optBoolean("resolved"));
        JSONObject disadvantaged=Xmm2025GrappleEscapeRules.resolveEscape("athletics",19,10,13,true);
        assertTrue(disadvantaged.optBoolean("resolved"));assertEquals(10,disadvantaged.optInt("check_total"));
        assertFalse(disadvantaged.optBoolean("success"));assertTrue(disadvantaged.optBoolean("disadvantage"));
        JSONObject missingSecond=Xmm2025GrappleEscapeRules.resolveEscape("athletics",19,Integer.MIN_VALUE,13,true);
        assertFalse(missingSecond.optBoolean("resolved"));
    }

    @Test public void sourceLinkedRestraintEndsWithItsGrapple()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        String[][] supported={{"Aberrant Cultist","Tentacle Lash"},{"Chain Devil","Chain"},
            {"Crocodile","Bite"},{"Giant Octopus","Tentacles"},{"Mezzoloth","Claws"},
            {"Purple Worm","Bite"},{"Remorhaz","Bite"},
            {"Roc","Talons"},{"Water Weird","Surge"},
            {"Giant Crocodile","Bite"},{"Kraken","Tentacle"},{"Tyrannosaurus Rex","Bite"},
            {"Behir","Constrict"},{"Couatl","Constrict"},{"Marilith","Constrict"},
            {"Salamander","Constrict"},{"Yuan-ti Abomination","Constrict"},
            {"Yuan-ti Malison (Type 3)","Constrict"}};
        for(String[] pair:supported){
            JSONObject profile=CompiledStatblockEngine.runtimeProfile(pair[0]);
            JSONObject action=null;JSONArray actions=profile.optJSONArray("actions");
            for(int i=0;actions!=null&&i<actions.length();i++){
                JSONObject candidate=actions.optJSONObject(i);
                if(candidate!=null&&pair[1].equals(candidate.optString("name")))action=candidate;
            }
            assertNotNull(pair[0]+" / "+pair[1],action);
            assertTrue(pair[0]+" / "+pair[1]+" "+action+" pin="+
                Xmm2025GrappleEscapeRules.profile(pair[0],pair[1]),
                Xmm2025GrappleEscapeRules.validSource(pair[0],action));
            assertEquals("grappled, restrained",action.optString(
                action.has("condition_on_fail")?"condition_on_fail":"condition_on_hit"));
            assertEquals(new JSONArray().put("grappled").put("restrained").toString(),
                Xmm2025GrappleEscapeRules.conditions(pair[0],action).toString());
            if("Constrict".equals(pair[1]))
                assertEquals(pair[0]+" / "+pair[1],0,
                    Xmm2025GrappleEscapeRules.targetCapacity(pair[0],action));
            else if("Kraken".equals(pair[0]))
                assertEquals(pair[0]+" / "+pair[1],10,
                    Xmm2025GrappleEscapeRules.targetCapacity(pair[0],action));
            else assertTrue(pair[0]+" / "+pair[1],Xmm2025GrappleEscapeRules.targetCapacity(pair[0],action)>=1);
        }
    }
}
