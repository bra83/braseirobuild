package br.com.tutorclinico;
import java.util.ArrayList;
import java.util.List;
/** Maps independently timed acoustic utterances to transcript segments.
 * Ambiguous timing is not proof of overlapping speech; never claim neural OSD here. */
public final class SpeakerTimeline {
 private static final int GROUPS=4;
 private static final class Span {
  final long start,end; final int group;
  Span(long s,long e,int g){start=s;end=e;group=g;}
 }
 private final List<Span> spans=new ArrayList<>();
 private final List<Span> uncertain=new ArrayList<>();
 private final List<Span> overlap=new ArrayList<>();
 public static final String OVERLAP="sobreposicao";
 public void addOverlap(long s,long e){
  if(s>=0&&e>s)overlap.add(new Span(s,e,0));
 }
 public List<long[]> separationWindows(long duration){
  List<long[]> result=new ArrayList<>();long a=-1,b=-1;
  for(Span span:overlap){
   long start=Math.max(0,span.start-350),end=Math.min(duration,span.end+350);
   if(end<=start)continue;
   if(a>=0&&start<=b){b=Math.max(b,end);continue;}
   if(a>=0)splitWindows(result,a,b);a=start;b=end;
  }
  if(a>=0)splitWindows(result,a,b);return result;
 }
 private static void splitWindows(List<long[]> result,long a,long b){
  while(b-a>5000){result.add(new long[]{a,a+5000});a+=5000;}
  if(b-a>=100)result.add(new long[]{a,b});
 }
 public void clearOverlap(){overlap.clear();}
 public void clearGroups(){spans.clear();uncertain.clear();}
 public boolean hasOverlap(long s,long e){
  if(s<0||e<=s)return false;
  long sum=0;for(Span v:overlap)sum+=intersection(s,e,v);
  return sum>=Math.max(60,Math.min(150,(e-s)*0.35));
 }

 private static int group(String label){
  for(int i=1;i<=GROUPS;i++)if(("falante"+i).equals(label))return i;
  return 0;
 }
 public void add(long s,long e,String label){
  if(s<0||e<=s)return;
  if(!spans.isEmpty()&&s<spans.get(spans.size()-1).end){
   // Inconsistent decoder timing: abstain on the affected interval rather
   // than silently ignoring conflicting evidence or assigning a clinical role.
   uncertain.add(new Span(s,e,0));return;
  }
  spans.add(new Span(s,e,group(label)));
 }
 private static long intersection(long s,long e,Span v){
  return Math.max(0,Math.min(e,v.end)-Math.max(s,v.start));
 }
 public String label(long s,long e){
  if(s<0||e<=s)return "";
  if(hasOverlap(s,e))return OVERLAP;
  for(Span v:uncertain)if(intersection(s,e,v)>0)return "";
  long[] duration=new long[GROUPS+1];
  for(Span v:spans)duration[v.group]+=intersection(s,e,v);
  long length=e-s;int best=1;long others=0;
  for(int i=2;i<=GROUPS;i++)if(duration[i]>duration[best])best=i;
  for(int i=1;i<=GROUPS;i++)if(i!=best)others+=duration[i];
  if(duration[best]<0.60*length||others>0.15*length||duration[0]>0.25*length)return "";
  return "falante"+best;
 }
}
