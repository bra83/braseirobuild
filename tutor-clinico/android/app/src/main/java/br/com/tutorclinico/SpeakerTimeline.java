package br.com.tutorclinico;
import java.util.ArrayList;
import java.util.List;
/** Maps independently timed acoustic utterances to transcript segments. */
public final class SpeakerTimeline {
 private static final class Span {final long start,end;final String label;Span(long s,long e,String l){start=s;end=e;label=l;}}
 private final List<Span> spans=new ArrayList<>();
 public void add(long s,long e,String l){
  if(s<0||e<=s||(!spans.isEmpty()&&s<spans.get(spans.size()-1).end))return;
  spans.add(new Span(s,e,"falante1".equals(l)||"falante2".equals(l)?l:""));
 }
 public String label(long s,long e){
  if(s<0||e<=s)return "";
  long a=0,b=0,u=0;
  for(Span v:spans){long n=Math.max(0,Math.min(e,v.end)-Math.max(s,v.start));if("falante1".equals(v.label))a+=n;else if("falante2".equals(v.label))b+=n;else u+=n;}
  long length=e-s;
  if(Math.max(a,b)<0.60*length||Math.min(a,b)>0.15*length||u>0.25*length)return "";
  return a>b?"falante1":"falante2";
 }
}
