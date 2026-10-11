package com.braseiro.dnd5e;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Source-bound attack dice limits. Never invent a damage result from prose.
 * An unsupported expression is deliberately blocked rather than treated as
 * permission to submit an arbitrary number. Critical hits double dice only. */
public final class CompiledMonsterDamageGuard {
    private CompiledMonsterDamageGuard() { }
    private static final Pattern TERM=Pattern.compile("([+-]?)(?:(\\d{1,3})d(\\d{1,4})|(\\d{1,6}))",Pattern.CASE_INSENSITIVE);
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
            if(matcher.group(2)!=null){
                long count=Long.parseLong(matcher.group(2)),faces=Long.parseLong(matcher.group(3));
                if(count<1||count>200||faces<2||faces>1000)return null;
                if(critical)count*=2;
                lo=count;hi=count*faces;
            }else{
                lo=hi=Long.parseLong(matcher.group(4));
            }
            if(multiplier<0){min-=hi;max-=lo;}else{min+=lo;max+=hi;}
            pos=matcher.end();
        }
        if(pos!=s.length())return null;
        return new long[]{min,max};
    }
}
