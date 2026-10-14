package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Every independently detected untyped source action must stop before a
 * stateless runtime can return damage or before a typed action path is chosen. */
public final class XmmUnmodeledSourceEffectRuntimeTest {
    @Test public void all215UnmodeledSourceActionsFailClosedAtRuntimePreflight(){
        JSONObject installed=Xmm2025CompiledBestiaryEngine.install();
        assertTrue(installed.toString(),installed.optBoolean("complete"));
        JSONArray names=Xmm2025SourceIndex.ids();
        int marked=0,attackRoutes=0,saveRoutes=0,preflightOnly=0;
        for(int i=0;i<names.length();i++){
            String id=names.optString(i);
            JSONObject raw=CompiledStatblockEngine.get(id);
            for(String field:new String[]{"actions","special_actions"}){
                JSONArray actions=raw.optJSONArray(field);
                if(actions==null)continue;
                for(int j=0;j<actions.length();j++){
                    JSONObject action=actions.optJSONObject(j);
                    if(action==null||!action.optBoolean("manual_unmodeled_source_effect_required",false))continue;
                    marked++;
                    assertEquals(id+" / "+action.optString("name"),
                        "manual_source_action_resolution_required",
                        MonsterActionEconomyGuard.attackRiderBlock(action));
                    JSONObject result=null;
                    if(action.has("to_hit")){
                        attackRoutes++;
                        result=StatblockCombatRuntimeEngine.attack(id,action.optString("name"),20,1,40,80);
                    }else if(action.optInt("save_dc",0)>0){
                        saveRoutes++;
                        result=StatblockCombatRuntimeEngine.savingThrowAction(id,action.optString("name"),99);
                    }else{
                        preflightOnly++;
                    }
                    if(result!=null){
                        assertTrue(id+" / "+action.optString("name")+": "+result,result.optBoolean("blocked"));
                        assertEquals("manual_source_action_resolution_required",result.optString("reason"));
                        assertFalse(result.has("damage_expression"));
                    }
                }
            }
        }
        assertEquals(215,marked);
        assertEquals(97,attackRoutes);
        assertEquals(92,saveRoutes);
        assertEquals(26,preflightOnly);
    }
}
