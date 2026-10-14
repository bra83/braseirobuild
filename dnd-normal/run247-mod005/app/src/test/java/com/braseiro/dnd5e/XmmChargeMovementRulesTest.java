package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class XmmChargeMovementRulesTest {
    private JSONObject action(String distance) throws Exception {
        return new JSONObject().put("to_hit",5).put("source_bound",true)
            .put("extra_damage", "1d6").put("extra_damage_type","piercing")
            .put("extra_damage_condition","moved_"+distance+"ft_straight_toward_target_immediately_before_hit")
            .put("extra_damage_condition_on_hit","prone")
            .put("extra_damage_max_target_size","large");
    }
    private JSONObject proof(int[][] route) throws Exception {
        JSONArray cells=new JSONArray();
        for(int[] cell:route)cells.put(new JSONObject().put("x",cell[0]).put("y",cell[1]));
        return new JSONObject().put("schema",TacticalGridMovementProofRules.SCHEMA)
            .put("actor_id",7).put("scene_key","scene-a").put("grid_fingerprint","grid-a")
            .put("from_x",route[0][0]).put("from_y",route[0][1])
            .put("to_x",route[route.length-1][0]).put("to_y",route[route.length-1][1])
            .put("distance_ft",(route.length-1)*5).put("movement_cost_ft",(route.length-1)*5)
            .put("route_cells",cells);
    }

    @Test public void sourceFeetStraightnessAndTargetDirectionAreRequired() throws Exception {
        JSONObject action=action("20");
        assertTrue(XmmChargeMovementRules.supported(action));
        assertEquals(20,XmmChargeMovementRules.requiredFeet(action));
        assertTrue(XmmChargeMovementRules.qualifies(action,
            proof(new int[][]{{0,2},{1,2},{2,2},{3,2},{4,2}}),7,"scene-a",7,2));
        assertFalse("short route",XmmChargeMovementRules.qualifies(action,
            proof(new int[][]{{0,2},{1,2},{2,2},{3,2}}),7,"scene-a",7,2));
        assertFalse("bent route",XmmChargeMovementRules.qualifies(action,
            proof(new int[][]{{0,2},{1,2},{2,3},{3,3},{4,3}}),7,"scene-a",7,2));
        assertFalse("wrong scene",XmmChargeMovementRules.qualifies(action,
            proof(new int[][]{{0,2},{1,2},{2,2},{3,2},{4,2}}),7,"scene-b",7,2));
        assertFalse("away from target",XmmChargeMovementRules.qualifies(action,
            proof(new int[][]{{0,2},{1,2},{2,2},{3,2},{4,2}}),7,"scene-a",-5,2));
        assertTrue(XmmChargeMovementRules.qualifies(action("10"),
            proof(new int[][]{{0,2},{1,2},{2,2}}),7,"scene-a",7,2));
        assertFalse("unrecognized condition string",XmmChargeMovementRules.supported(
            action.put("extra_damage_condition","moved_15ft_toward_target")));
    }

    @Test public void proneSizeCapIsIndependentFromExtraDamageQualification() throws Exception {
        JSONObject action=action("20");
        assertTrue(XmmChargeMovementRules.proneEligible(action,"large"));
        assertFalse(XmmChargeMovementRules.proneEligible(action,"huge"));
        assertFalse(XmmChargeMovementRules.proneEligible(action,""));
        action.remove("extra_damage_max_target_size");
        assertTrue(XmmChargeMovementRules.proneEligible(action,"huge"));
        action.put("save_dc",12);
        assertFalse("mixed unimplemented save rider",XmmChargeMovementRules.supported(action));
    }
}
