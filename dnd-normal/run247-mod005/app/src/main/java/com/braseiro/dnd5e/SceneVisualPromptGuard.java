package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Pure, deterministic scene-frame focus. No Gemini call or state mutation. */
public final class SceneVisualPromptGuard {
    private SceneVisualPromptGuard(){}

    static String normalize(String text){
        return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }

    public static String latestBeat(String narration){
        String value=narration==null?"":narration.replaceAll("\\s+"," ").trim();
        if(value.length()<=740)return value;
        int start=value.length()-740;
        int sentence=Math.max(value.indexOf(". ",start),Math.max(value.indexOf("! ",start),value.indexOf("? ",start)));
        if(sentence>=0&&sentence-start<180)start=sentence+2;
        return value.substring(start).trim();
    }

    static boolean sameFrame(String a,String b){
        String x=normalize(a),y=normalize(b);
        if(x.isEmpty()||y.isEmpty())return false;
        if(x.equals(y))return true;
        if(x.length()>90&&y.length()>90&&(x.contains(y)||y.contains(x)))return true;
        Set<String> left=frameTokens(x),right=frameTokens(y);
        if(left.size()<8||right.size()<8)return false;
        int intersection=0;
        for(String t:left)if(right.contains(t))intersection++;
        int union=left.size()+right.size()-intersection;
        return union>0&&((double)intersection/union)>=.82d;
    }

    private static Set<String> frameTokens(String text){
        Set<String> out=new HashSet<String>();
        for(String token:text.split(" "))if(token.length()>3)out.add(token);
        return out;
    }

    /** A stale model-supplied frame cannot silently become the next frame. */
    public static String choose(String candidate,String previous,String narration){
        String visual=candidate==null?"":candidate.trim();
        String moment=latestBeat(narration);
        if(visual.isEmpty()||sameFrame(visual,previous))
            visual=moment;
        if(visual.length()>900)visual=visual.substring(visual.length()-900);
        return visual.trim();
    }
}
