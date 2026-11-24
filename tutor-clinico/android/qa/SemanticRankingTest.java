package br.com.tutorclinico;
public final class SemanticRankingTest{
 public static void main(String[] args){
  check(!SemanticRanking.canCompare("paciente","terapeuta",0L,90000L,5,1));
  check(!SemanticRanking.canCompare("paciente","paciente",0L,10000L,5,1));
  check(SemanticRanking.canCompare("paciente","paciente",0L,90000L,5,1));
  check(!SemanticRanking.canCompare("paciente","paciente",null,null,2,1));
  check(SemanticRanking.canCompare("paciente","paciente",null,null,4,1));
  check(!SemanticRanking.relevant(.25,false));check(SemanticRanking.relevant(.7,false));
  check(!SemanticRanking.eligible("Sim"));check(SemanticRanking.eligible("Tenho muito medo"));
  int[] u=new int[4];check(SemanticRanking.acceptPair(0,1,u));check(SemanticRanking.acceptPair(0,2,u));check(!SemanticRanking.acceptPair(0,3,u));
  System.out.println("PASS semantic ranking policy; no clinical interpretation");
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
}
