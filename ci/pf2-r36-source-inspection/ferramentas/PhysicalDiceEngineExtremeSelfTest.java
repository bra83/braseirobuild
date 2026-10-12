package com.braseiro.pathfinder2e;
import java.util.*;
public final class PhysicalDiceEngineExtremeSelfTest {
  private static void ok(boolean c,String m){if(!c)throw new AssertionError(m);}
  private static PhysicalDiceEngine.Session run(String formula,long seed){
    PhysicalDiceEngine.Session s=PhysicalDiceEngine.create(formula,seed); int guard=0;
    while(!s.isComplete()&&guard++<2200)s.step();
    ok(s.isComplete(),"não assentou: "+formula+" seed="+seed+" steps="+s.steps());
    PhysicalDiceEngine.RollResult r=s.result(); ok(r!=null,"resultado nulo");
    ok(!r.forcedSettle,"forced settle: "+formula+" seed="+seed+" steps="+r.physicsSteps);
    for(PhysicalDiceEngine.DieBody d:s.bodies){ok(d.settled,"corpo não assentado");ok(d.topFace()!=null,"face superior ausente");int v=d.rawTopValue();ok(v>=1&&v<=d.sides,"face física inválida");}
    return s;
  }
  private static int catastrophicOverlaps(PhysicalDiceEngine.Session s){
    int n=0; for(int i=0;i<s.bodies.size();i++)for(int j=i+1;j<s.bodies.size();j++){
      PhysicalDiceEngine.DieBody a=s.bodies.get(i),b=s.bodies.get(j);
      double dx=b.position.x-a.position.x,dy=b.position.y-a.position.y,dz=b.position.z-a.position.z;
      double dist=Math.sqrt(dx*dx+dy*dy+dz*dz), min=(a.scale+b.scale)*.35;
      if(dist<min)n++;
    } return n;
  }
  public static void main(String[] args){
    int cases=0,max4=0,max20=0,maxPct=0;
    for(int seed=1;seed<=1200;seed++){PhysicalDiceEngine.Session s=run("4d6",100000L+seed);max4=Math.max(max4,s.result().physicsSteps);ok(catastrophicOverlaps(s)==0,"4d6 terminou catastroficamente sobreposto seed="+seed);cases++;}
    for(int seed=1;seed<=350;seed++){PhysicalDiceEngine.Session s=run("20d6",200000L+seed);max20=Math.max(max20,s.result().physicsSteps);ok(catastrophicOverlaps(s)==0,"20d6 terminou catastroficamente sobreposto seed="+seed);cases++;}
    for(int seed=1;seed<=180;seed++){PhysicalDiceEngine.Session s=run("10d100",300000L+seed);maxPct=Math.max(maxPct,s.result().physicsSteps);ok(s.bodies.size()==20,"10d100 precisa de 20 corpos");ok(s.result().dice.size()==10,"10d100 quantidade lógica errada");for(int v:s.result().dice)ok(v>=1&&v<=100,"percentil fora da faixa");ok(catastrophicOverlaps(s)==0,"10d100 terminou catastroficamente sobreposto seed="+seed);cases++;}
    int[] freq=new int[21]; int n=10000;
    for(int seed=1;seed<=n;seed++){PhysicalDiceEngine.Session s=run("d20",500000L+seed);int v=s.result().dice.get(0);freq[v]++;cases++;}
    double expected=n/20.0,chi=0; for(int i=1;i<=20;i++){double d=freq[i]-expected;chi+=d*d/expected;ok(freq[i]>expected*.70&&freq[i]<expected*1.30,"viés grosseiro d20 face "+i+": "+freq[i]);}
    ok(chi<50,"chi² d20 suspeito: "+chi);
    System.out.println("DICE3D_EXTREME_PASS cases="+cases+" max4d6="+max4+" max20d6="+max20+" max10d100="+maxPct+" chi2="+String.format(java.util.Locale.ROOT,"%.3f",chi)+" freq="+Arrays.toString(Arrays.copyOfRange(freq,1,21)));
  }
}
