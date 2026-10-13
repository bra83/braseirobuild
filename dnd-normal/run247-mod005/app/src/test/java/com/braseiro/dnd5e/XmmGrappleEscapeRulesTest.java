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
            assertEquals(monster+" / "+name,!row.optBoolean("other"),valid);
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
        assertEquals(36,grapples);assertEquals(21,manual);assertEquals(15,executable);
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
    }
}
