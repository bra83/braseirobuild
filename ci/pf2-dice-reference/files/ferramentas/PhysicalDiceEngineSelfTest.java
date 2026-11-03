package com.braseiro.pathfinder2e;
import java.util.*;
public final class PhysicalDiceEngineSelfTest {
  private static void ok(boolean c,String m){if(!c)throw new AssertionError(m);}
  public static void main(String[] args){
    String[] formulas={"d4","d6","d8","d10","d12","d20","d100","4d6","2d20"};
    int sessions=0, faces=0;
    for(String f:formulas){
      PhysicalDiceEngine.Formula parsed=PhysicalDiceEngine.parseFormula(f);
      for(int seed=0;seed<80;seed++){
        PhysicalDiceEngine.Session s=PhysicalDiceEngine.create(f,0x9E3779B97F4A7C15L*(seed+1)+f.hashCode());
        int guard=0; while(!s.isComplete()&&guard++<6000)s.step();
        ok(s.isComplete(),f+" não estabilizou seed "+seed);
        PhysicalDiceEngine.RollResult r=s.result(); ok(r!=null,f+" sem resultado");
        ok(r.dice.size()==parsed.count,f+" quantidade lógica divergente");
        for(int v:r.dice){ok(v>=1&&v<=parsed.sides,f+" face inválida "+v);faces++;}
        sessions++;
      }
    }
    // determinismo por seed: mesma condição inicial = mesmas faces.
    long seed=8877665544332211L; PhysicalDiceEngine.Session a=PhysicalDiceEngine.create("4d6",seed),b=PhysicalDiceEngine.create("4d6",seed);
    while(!a.isComplete())a.step(); while(!b.isComplete())b.step(); ok(a.result().dice.equals(b.result().dice),"replay por seed divergiu");
    System.out.println("PHYSICAL_DICE_NATIVE_PASS sessions="+sessions+" faces="+faces+" engine="+PhysicalDiceEngine.ENGINE_ID);
  }
}
