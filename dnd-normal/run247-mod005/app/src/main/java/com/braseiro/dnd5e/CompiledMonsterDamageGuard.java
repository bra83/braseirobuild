package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Source-bound attack dice limits. Never invent a damage result from prose.
 * An unsupported expression is deliberately blocked rather than treated as
 * permission to submit an arbitrary number. Critical hits double dice only. */
public final class CompiledMonsterDamageGuard {
    private CompiledMonsterDamageGuard() { }
    private static final Pattern TERM=Pattern.compile("([+-]?)(?:(\\d{1,3})?d(\\d{1,4})|(\\d{1,6}))",Pattern.CASE_INSENSITIVE);
    public static final class Range {
        public final int min,max;
        private Range(int low,int high){min=low;max=high;}
    }
    public static Range range(String primary,String extra,boolean critical) {
        long[] a=parse(primary,critical),b=parse(extra,critical);
        if(a==null||b==null)return null;
        long min=a[0]+b[0],max=a[1]+b[1];
        if(max<0||max>100000||min>100000)return null;
        return new Range((int)Math.max(0,min),(int)Math.max(0,max));
    }
    public static boolean accepts(String primary,String extra,int total,boolean critical){
        Range r=range(primary,extra,critical);
        return r!=null&&total>=r.min&&total<=r.max;
    }
    private static final class Term implements Comparable<Term> {
        final boolean isDice;
        final boolean negative;
        final long count;
        final long faces;
        final long constant;

        Term(boolean isDice, boolean negative, long count, long faces, long constant) {
            this.isDice = isDice;
            this.negative = negative;
            this.count = count;
            this.faces = faces;
            this.constant = constant;
        }

        @Override
        public int compareTo(Term o) {
            if (this.isDice != o.isDice) {
                return this.isDice ? -1 : 1;
            }
            if (this.isDice) {
                if (this.faces != o.faces) {
                    return Long.compare(o.faces, this.faces);
                }
                if (this.count != o.count) {
                    return Long.compare(o.count, this.count);
                }
                if (this.negative != o.negative) {
                    return this.negative ? 1 : -1;
                }
                return 0;
            } else {
                if (this.negative != o.negative) {
                    return this.negative ? 1 : -1;
                }
                return Long.compare(o.constant, this.constant);
            }
        }
    }
    public static String canonical(String source){
        if(source==null)return "";
        String s=source.replaceAll("\\s+","");
        if(s.isEmpty())return "";
        Matcher matcher=TERM.matcher(s);
        int pos=0;
        List<Term> terms=new ArrayList<>();
        while(matcher.find()){
            if(matcher.start()!=pos)return "";
            String sign=matcher.group(1);
            if(pos>0&&sign.isEmpty())return "";
            boolean negative="-".equals(sign);
            if(matcher.group(3)!=null){
                long count=matcher.group(2)!=null&&!matcher.group(2).isEmpty()?Long.parseLong(matcher.group(2)):1;
                long faces=Long.parseLong(matcher.group(3));
                if(count<1||count>200||faces<2||faces>1000)return "";
                terms.add(new Term(true,negative,count,faces,0));
            }else if(matcher.group(4)!=null){
                long val=Long.parseLong(matcher.group(4));
                terms.add(new Term(false,negative,0,0,val));
            }else{
                return "";
            }
            pos=matcher.end();
        }
        if(pos!=s.length()||terms.isEmpty())return "";
        Collections.sort(terms);
        StringBuilder out=new StringBuilder();
        for(int i=0;i<terms.size();i++){
            Term t=terms.get(i);
            if(i>0){
                out.append(" ").append(t.negative?"-":"+").append(" ");
            }else if(t.negative){
                out.append("-");
            }
            if(t.isDice){
                out.append(t.count).append("d").append(t.faces);
            }else{
                out.append(t.constant);
            }
        }
        return out.toString();
    }
    public static boolean equivalent(String a,String b){
        if(a==null||b==null)return false;
        String ca=canonical(a),cb=canonical(b);
        if(ca.isEmpty()||cb.isEmpty())return false;
        return ca.equals(cb);
    }
    private static long[] parse(String source,boolean critical){
        String s=source==null?"":source.replaceAll("\\s+","");
        if(s.isEmpty())return new long[]{0,0};
        long min=0,max=0;int pos=0;
        Matcher matcher=TERM.matcher(s);
        while(matcher.find()){
            if(matcher.start()!=pos)return null;
            String sign=matcher.group(1);
            if(pos>0&&sign.isEmpty())return null;
            int multiplier="-".equals(sign)?-1:1;
            long lo,hi;
            if(matcher.group(3)!=null){
                long count=matcher.group(2)!=null&&!matcher.group(2).isEmpty()?Long.parseLong(matcher.group(2)):1;
                long faces=Long.parseLong(matcher.group(3));
                if(count<1||count>200||faces<2||faces>1000)return null;
                if(critical)count*=2;
                lo=count;hi=count*faces;
            }else if(matcher.group(4)!=null){
                lo=hi=Long.parseLong(matcher.group(4));
            }else{
                return null;
            }
            if(multiplier<0){min-=hi;max-=lo;}else{min+=lo;max+=hi;}
            pos=matcher.end();
        }
        if(pos!=s.length())return null;
        return new long[]{min,max};
    }
}
