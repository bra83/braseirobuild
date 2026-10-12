package com.braseiro.pathfinder2e;
import java.util.Arrays;
public final class DiceRollSpecSelfTest {
  private static void ok(boolean x,String m){if(!x)throw new AssertionError(m);}
  public static void main(String[] args){
    DiceRollSpec d=DiceRollSpec.parse("1d20+11","Normal");ok(d.count==1&&d.sides==20&&d.modifier==11,"parse d20");
    DiceRollSpec.Result r=d.resolve(Arrays.asList(17));ok(r.total==28&&r.natural==17,"17+11=28");
    DiceRollSpec f=DiceRollSpec.parse("d20+4","Fortuna");ok(f.isFortuneRoll()&&f.physicalBodyCount()==2,"fortuna 2d20");ok(f.resolve(Arrays.asList(3,18)).natural==18,"fortuna escolhe maior");
    DiceRollSpec mi=DiceRollSpec.parse("d20+4","Infortúnio");ok(mi.resolve(Arrays.asList(3,18)).natural==3,"infortúnio escolhe menor");
    DiceRollSpec pct=DiceRollSpec.parse("2d100","Normal");ok(pct.supportsPhysical3d()&&pct.physicalBodyCount()==4,"d100 físico");
    ok("Fortuna".equals(DiceRollSpec.normalizeMode("fortune")),"normaliza fortune");ok("Infortúnio".equals(DiceRollSpec.normalizeMode("misfortune")),"normaliza misfortune");
    System.out.println("DICE_ROLL_SPEC_PF2_PASS");
  }
}
