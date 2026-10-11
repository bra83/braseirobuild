import com.braseiro.dnd5e.MonsterForcedMovementRules;

/** Pure deterministic G05 regressions; executable with JDK 21, no Android. */
public final class Run245ForcedMovementPureHarness {
    private static int checks;
    private static void require(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static boolean[][] grid(){return new boolean[8][12];}
    private static MonsterForcedMovementRules.Plan plan(MonsterForcedMovementRules.Spec spec,
            boolean[][] blocked,boolean[][] occupied,int ax,int ay,int tx,int ty,int dx,int dy){
        return MonsterForcedMovementRules.plan(spec,ax,ay,tx,ty,dx,dy,12,8,blocked,occupied);
    }
    public static void main(String[] args) {
        MonsterForcedMovementRules.Spec push=MonsterForcedMovementRules.compiled("push:10ft","");
        MonsterForcedMovementRules.Spec pull=MonsterForcedMovementRules.compiled("pull:10ft","");
        require(push!=null&&pull!=null,"source-compiled specs");
        boolean[][] b=grid(),o=grid();
        require(plan(push,b,o,1,1,2,2,3,3).valid(),"open diagonal push");
        b[2][3]=true;
        require("forced_movement_corner_blocked".equals(plan(push,b,o,1,1,2,2,3,3).blocked),"first blocked corner");
        b[2][3]=false;b[3][2]=true;
        require("forced_movement_corner_blocked".equals(plan(push,b,o,1,1,2,2,3,3).blocked),"second blocked corner");
        b[3][2]=false;o[2][3]=true;
        require("forced_movement_corner_blocked".equals(plan(push,b,o,1,1,2,2,3,3).blocked),"first occupied corner");
        o[2][3]=false;o[3][2]=true;
        require("forced_movement_corner_blocked".equals(plan(push,b,o,1,1,2,2,3,3).blocked),"second occupied corner");
        o[3][2]=false;b[3][3]=true;
        require("forced_movement_path_blocked".equals(plan(push,b,o,1,1,2,2,3,3).blocked),"blocked destination");
        b[3][3]=false;
        require(plan(push,b,o,1,1,2,2,4,4).valid(),"multi-cell open diagonal");
        b[4][3]=true;
        require("forced_movement_corner_blocked".equals(plan(push,b,o,1,1,2,2,4,4).blocked),"second-step corner");
        b[4][3]=false;
        require(plan(pull,b,o,1,1,4,4,3,3).valid(),"open pull");
        b[3][4]=true;
        require("forced_movement_corner_blocked".equals(plan(pull,b,o,1,1,4,4,3,3).blocked),"blocked pull corner");
        b[3][4]=false;
        require(plan(push,b,o,1,1,2,2,3,2).valid(),"orthogonal push unaffected");
        require("forced_movement_wrong_direction".equals(plan(push,b,o,0,0,2,2,1,1).blocked),"no push toward source");
        System.out.println("RUN245_G05_FORCED_MOVEMENT_PURE_PASS checks="+checks);
    }
}
