package com.braseiro.pathfinder2e;
import java.util.*;
public final class PhysicalDiceEngineStressSelfTest {
  private static void ok(boolean c,String m){if(!c)throw new AssertionError(m);}
  private static PhysicalDiceEngine.RollResult run(String formula,long seed){
    PhysicalDiceEngine.Session s=PhysicalDiceEngine.create(formula,seed);
    int guard=0; while(!s.isComplete()&&guard++<2200)s.step();
    ok(s.isComplete(),formula+" não estabilizou seed="+seed+" steps="+s.steps());
    PhysicalDiceEngine.RollResult r=s.result(); ok(r!=null,"resultado nulo "+formula);
    ok(!r.forcedSettle,"forced settle em "+formula+" seed="+seed+" steps="+r.physicsSteps);
    if(r.formula.sides!=100){
      ok(r.dice.size()==s.bodies.size(),"quantidade física/lógica divergente em "+formula);
      for(int i=0;i<r.dice.size();i++) ok(r.dice.get(i)==s.bodies.get(i).rawTopValue(),"resultado não veio da face superior em "+formula);
    }
    return r;
  }
  public static void main(String[] args){
    int cases=0,maxSteps=0; Map<Integer,Set<Integer>> seen=new LinkedHashMap<Integer,Set<Integer>>();
    for(int sides:new int[]{4,6,8,10,12,20}){
      PhysicalDiceEngine.Mesh m=PhysicalDiceEngine.mesh(sides);
      ok(m.faces.size()==sides,"malha d"+sides+" tem "+m.faces.size()+" faces");
      Set<Integer> vals=new HashSet<Integer>(); for(PhysicalDiceEngine.Face f:m.faces)vals.add(f.value);
      ok(vals.size()==sides,"rótulos repetidos d"+sides);
      Set<Integer> obs=new HashSet<Integer>(); seen.put(sides,obs);
      for(int seed=1;seed<=120;seed++){
        PhysicalDiceEngine.RollResult r=run("d"+sides,seed*1000003L+sides);
        int v=r.dice.get(0); ok(v>=1&&v<=sides,"face inválida d"+sides+": "+v); obs.add(v); maxSteps=Math.max(maxSteps,r.physicsSteps); cases++;
      }
      ok(obs.size()>=Math.min(sides,Math.max(4,(int)Math.floor(sides*.65))),"distribuição degenerada d"+sides+": "+obs);
    }
    for(int seed=1;seed<=160;seed++){PhysicalDiceEngine.RollResult r=run("4d6",8000L+seed);ok(r.dice.size()==4,"4d6 inválido");maxSteps=Math.max(maxSteps,r.physicsSteps);cases++;}
    for(int seed=1;seed<=120;seed++){PhysicalDiceEngine.RollResult r=run("2d20+11",18000L+seed);ok(r.dice.size()==2,"2d20 inválido");maxSteps=Math.max(maxSteps,r.physicsSteps);cases++;}
    for(int seed=1;seed<=100;seed++){PhysicalDiceEngine.RollResult r=run("d100",25000L+seed);int v=r.dice.get(0);ok(v>=1&&v<=100,"d100 inválido: "+v);maxSteps=Math.max(maxSteps,r.physicsSteps);cases++;}
    for(int seed=1;seed<=80;seed++){
      long s=33000L+seed; PhysicalDiceEngine.RollResult a=run("d20",s),b=run("d20",s);
      ok(a.dice.equals(b.dice)&&a.physicsSteps==b.physicsSteps,"replay não determinístico seed="+s); cases++;
    }
    System.out.println("DICE3D_STRESS_PASS "+cases+" simulações; maxSteps="+maxSteps+" cobertura="+seen);
  }
}
