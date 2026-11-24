package br.com.tutorclinico;
import java.util.*;
/** Retrieval policy, NOT clinical validation. Thresholds are provisional experimental filters. */
final class SemanticRanking {
 static final double QUERY_MIN=0.35, RECURRENCE_MIN=0.45;
 static boolean eligible(String text){return text!=null&&text.trim().split("\\s+").length>=3;}
 static boolean canCompare(String a,String b,Long at,Long bt,int i,int j){
  if(!Objects.equals(a,b)||a==null)return false;
  if(at!=null&&bt!=null)return Math.abs(at-bt)>=45000L;
  return Math.abs(i-j)>=3;
 }
 static boolean relevant(double score,boolean query){return Double.isFinite(score)&&score>=(query?QUERY_MIN:RECURRENCE_MIN);}
 static boolean acceptPair(int i,int j,int[] usage){if(usage[i]>=2||usage[j]>=2)return false;usage[i]++;usage[j]++;return true;}
}
