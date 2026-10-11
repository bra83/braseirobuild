package com.braseiro.dnd5e;

/** RUN229 G05: explicit additional hostile in close-ranged combat (SRD 5.1/5.2).
 * The attacker does NOT need to see the hostile. The hostile needs to see the
 * attacker and not be incapacitated. The caller must prove that the selected
 * combatant is an enemy; is_player/name/source_kind alone are not allegiance.
 * Geometry is based on live tactical tokens, not narrated distances.
 */
public final class MonsterAdjacentHostileRules {
    private MonsterAdjacentHostileRules() { }
    public static String check(int width,int height,int ax,int ay,int hx,int hy,
        boolean[][] opaque,String attackerConditions,String hostileConditions,
        boolean playerConfirmedHostility){
        if(!playerConfirmedHostility)return "hostility_not_confirmed";
        if(width<1||height<1||width>100||height>100||opaque==null||opaque.length!=height||
            ax<0||ay<0||hx<0||hy<0||ax>=width||hx>=width||ay>=height||hy>=height)
            return "invalid_hostile_geometry";
        for(int y=0;y<height;y++)if(opaque[y]==null||opaque[y].length!=width)
            return "invalid_hostile_grid";
        if(ax==hx&&ay==hy||Math.max(Math.abs(ax-hx),Math.abs(ay-hy))!=1)
            return "additional_hostile_not_within_five_feet";
        if(opaque[ay][ax]||opaque[hy][hx])return "additional_hostile_line_of_sight_blocked";
        // A diagonally touching cell is not visible through an opaque corner.
        if(ax!=hx&&ay!=hy&&(opaque[ay][hx]||opaque[hy][ax]))
            return "additional_hostile_line_of_sight_blocked";
        if(ConditionTokenGuard.has(hostileConditions,"cego"))return "additional_hostile_cannot_see_attacker";
        for(String condition:new String[]{"incapacitado","aturdido","paralisado","petrificado","inconsciente"})
            if(ConditionTokenGuard.has(hostileConditions,condition))return "additional_hostile_incapacitated";
        if(ConditionTokenGuard.has(attackerConditions,"invisivel"))
            return "attacker_visibility_requires_special_senses_review";
        return "";
    }
}
