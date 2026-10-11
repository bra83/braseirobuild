package com.braseiro.dnd5e;

import java.util.Collections;
import java.util.List;

/** Pure pre-commit executor for PHB 2014/2024 spell range, targets and components. */
public final class SpellTargetComponentExecutor {
    private SpellTargetComponentExecutor() {}
    public static final class Request {
        public final String edition,spell,sourceClass,ability;
        public final int characterLevel,gridWidth,gridHeight,centerX,centerY,direction;
        public final long casterId,touchId;
        public final String scene,obstacles;
        public final List<Core2024CastingEligibility.ClassLevel> classes;
        public final List<String> inventory;
        public final List<Core2024TacticalGeometry.Token> tokens;
        public Request(String edition,String spell,String sourceClass,String ability,int characterLevel,
                       int gridWidth,int gridHeight,String scene,String obstacles,long casterId,
                       int centerX,int centerY,int direction,long touchId,
                       List<Core2024CastingEligibility.ClassLevel> classes,List<String> inventory,
                       List<Core2024TacticalGeometry.Token> tokens){
            this.edition=edition;this.spell=spell;this.sourceClass=sourceClass;this.ability=ability;
            this.characterLevel=characterLevel;this.gridWidth=gridWidth;this.gridHeight=gridHeight;
            this.scene=scene;this.obstacles=obstacles;this.casterId=casterId;this.centerX=centerX;
            this.centerY=centerY;this.direction=direction;this.touchId=touchId;this.classes=classes;
            this.inventory=inventory;this.tokens=tokens;
        }
    }
    public static final class Result {
        public final String error,sourceClass,signature; public final List<Long> targetIds;
        private Result(String e,String c,String s,List<Long> ids){error=e;sourceClass=c;signature=s;targetIds=ids;}
        public boolean allowed(){return error.isEmpty();}
    }
    private static Result fail(String e){return new Result(e,"","",Collections.<Long>emptyList());}
    public static Result execute(Request r){
        if(r==null)return fail("request_required");
        // This executor delegates to a catalog explicitly verified for PHB 2024.
        // Accepting "2014" here would silently execute 2024 spell text under the
        // legacy profile; edition support must be explicit and fail closed.
        if(!"2024".equals(r.edition))return fail("requires_2024_rules_profile");
        Core2024SpellRules.Spell spell=Core2024SpellRules.find(r.spell);
        if(spell==null)return fail("spell_not_compiled");
        Core2024CastingEligibility.Result eligibility=Core2024CastingEligibility.check(
            spell.key,r.characterLevel,r.classes,r.ability);
        if(!eligibility.allowed())return fail(eligibility.error);
        if(r.sourceClass!=null&&!r.sourceClass.isEmpty()&&!eligibility.sourceClass.equals(
            Core2024CastingEligibility.baseClass(r.sourceClass)))return fail("source_class_mismatch");
        String material=Core2024CastingEligibility.material(spell.key,eligibility.sourceClass,r.inventory);
        if(!material.isEmpty())return fail(material);
        Core2024TacticalGeometry.Plan plan=Core2024TacticalGeometry.plan(spell.key,r.gridWidth,r.gridHeight,
            r.scene,r.obstacles,r.tokens,r.casterId,r.centerX,r.centerY,r.direction,r.touchId);
        if(!plan.ok())return fail(plan.error);
        return new Result("",eligibility.sourceClass,plan.signature,plan.targetIds);
    }
}
