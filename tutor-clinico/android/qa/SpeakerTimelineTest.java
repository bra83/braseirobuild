package br.com.tutorclinico;
public final class SpeakerTimelineTest {
 public static void main(String[] args){
  SpeakerTimeline t=new SpeakerTimeline();
  t.add(1000,5000,"falante1");t.add(5000,9000,"falante2");t.add(9000,12000,"");
  check(t.label(1800,2200),"falante1");check(t.label(5300,5800),"falante2");
  check(t.label(4500,5500),"");check(t.label(9000,11000),"");check(t.label(0,1800),"");
  check(t.label(5000,9000),"falante2");check(t.label(10000,10000),"");
  t.add(11000,13000,"falante1");check(t.label(12000,13000),"");
  SpeakerClusters c=new SpeakerClusters();
  check(c.assign(new double[]{1,0,0}),"falante1");check(c.assign(new double[]{0,1,0}),"falante2");
  check(c.assign(new double[]{1,1,0}),"");check(c.assign(new double[]{0,1,0}),"falante2");
  c.clear();check(c.assign(new double[]{0,1,0}),"falante1");
  System.out.println("PASS speaker alignment and anonymous cluster logic (synthetic vectors)");
 }
 static void check(String a,String b){if(!b.equals(a))throw new AssertionError(b+" != "+a);}
}
