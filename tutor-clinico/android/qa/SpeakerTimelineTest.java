package br.com.tutorclinico;
public final class SpeakerTimelineTest {
 public static void main(String[] args){
  SpeakerTimeline t=new SpeakerTimeline();
  t.add(1000,5000,"falante1");t.add(5000,9000,"falante2");t.add(9000,12000,"");
  check(t.label(1800,2200),"falante1");check(t.label(5300,5800),"falante2");
  check(t.label(4500,5500),"");check(t.label(9000,11000),"");check(t.label(0,1800),"");
  check(t.label(5000,9000),"falante2");check(t.label(10000,10000),"");
  t.add(11000,13000,"falante1");check(t.label(12000,13000),"");
  SpeakerTimeline four=new SpeakerTimeline();
  four.add(0,1000,"falante1");four.add(1000,2000,"falante2");
  four.add(2000,3000,"falante3");four.add(3000,4000,"falante4");
  check(four.label(2000,3000),"falante3");
  check(four.label(3000,4000),"falante4");
  check(four.label(2500,3500),"");
  four.add(3100,3900,"falante3");
  check(four.label(3200,3800),"");
  check(four.label(2000,3000),"falante3");
  SpeakerTimeline neural=new SpeakerTimeline();
  neural.add(0,3000,"falante3");neural.addOverlap(1000,1400);
  check(neural.label(0,700),"falante3");
  check(neural.label(900,1500),SpeakerTimeline.OVERLAP);
  check(neural.label(0,3000),SpeakerTimeline.OVERLAP);
  neural.clearGroups();check(neural.label(900,1500),SpeakerTimeline.OVERLAP);
  neural.clearOverlap();check(neural.label(900,1500),"");
  SpeakerTimeline mixed=new SpeakerTimeline();
  mixed.add(0,700,"falante3");mixed.add(700,800,"falante1");
  mixed.add(800,900,"falante2");mixed.add(900,1000,"falante4");
  check(mixed.label(0,1000),"");
  SpeakerClusters c=new SpeakerClusters();
  check(c.assign(new double[]{1,0,0}),"falante1");check(c.assign(new double[]{0,1,0}),"falante2");
  check(c.assign(new double[]{1,1,0}),"");check(c.assign(new double[]{0,1,0}),"falante2");
  c.clear();check(c.assign(new double[]{0,1,0}),"falante1");
  c.clear();
  check(c.assign(new double[]{1,0,0,0}),"falante1");
  check(c.assign(new double[]{0,1,0,0}),"falante2");
  check(c.assign(new double[]{0,0,1,0}),"falante3");
  check(c.assign(new double[]{0,0,0,1}),"falante4");
  check(c.assign(new double[]{1,0,1,0}),"");
  check(c.assign(new double[]{0,0,0,-1}),"");
  check(c.assign(new double[]{0,0,1,0}),"falante3");
  String checkpoint=c.checkpoint();SpeakerClusters recovered=new SpeakerClusters();recovered.restore(checkpoint);
  check(recovered.checkpoint(),checkpoint);
  check(recovered.assign(new double[]{0,0,0,1}),"falante4");
  check(recovered.assign(new double[]{0,0,1,0}),"falante3");
  try{recovered.restore("1,NaN;-;-;-");throw new AssertionError("invalid checkpoint accepted");}catch(IllegalArgumentException expected){}
  check(recovered.assign(new double[]{1,0,0,0}),"falante1");
  System.out.println("PASS speaker alignment and anonymous cluster logic (synthetic vectors)");
 }
 static void check(String a,String b){if(!b.equals(a))throw new AssertionError(b+" != "+a);}
}
