package com.braseiro.pathfinder2e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Regras puras de fórmula/resultados. A animação 3D apenas fornece as faces físicas. */
public final class DiceRollSpec {
    private static final Pattern FORMULA=Pattern.compile("^(\\d*)d(\\d+)([+-]\\d+)?$");
    public final int count,sides,modifier;
    public final String formula,mode;
    private DiceRollSpec(int count,int sides,int modifier,String formula,String mode){this.count=count;this.sides=sides;this.modifier=modifier;this.formula=formula;this.mode=normalizeMode(mode);}
    public static DiceRollSpec parse(String raw,String mode){
        String f=(raw==null?"":raw).toLowerCase(Locale.US).replace(" ","");Matcher m=FORMULA.matcher(f);
        if(!m.matches())throw new IllegalArgumentException("Use o formato NdM+K, por exemplo 2d6+3.");
        int c=m.group(1).length()==0?1:Integer.parseInt(m.group(1));int s=Integer.parseInt(m.group(2));int mod=m.group(3)==null?0:Integer.parseInt(m.group(3));
        if(c<1||c>100||s<2||s>1000)throw new IllegalArgumentException("Quantidade ou dado fora do limite.");
        return new DiceRollSpec(c,s,mod,f,mode);
    }
    public boolean isFortuneRoll(){return count==1&&sides==20&&!"Normal".equals(mode);}
    public int physicalBodyCount(){return sides==100?count*2:(isFortuneRoll()?2:count);}
    public boolean supportsPhysical3d(){return (sides==4||sides==6||sides==8||sides==10||sides==12||sides==20||sides==100)&&physicalBodyCount()<=20;}
    public Result resolve(List<Integer> faces){
        if(faces==null)faces=Collections.emptyList();
        ArrayList<Integer> vals=new ArrayList<Integer>(faces);
        if(isFortuneRoll()){
            if(vals.size()!=2)throw new IllegalArgumentException("A rolagem física de fortuna/infortúnio precisa de dois d20.");
            int chosen="Fortuna".equals(mode)?Math.max(vals.get(0),vals.get(1)):Math.min(vals.get(0),vals.get(1));
            check(chosen,20);int total=chosen+modifier;String detail="["+vals.get(0)+", "+vals.get(1)+"] escolhe "+chosen+modifierText();return new Result(total,chosen,detail,critical(chosen));
        }
        if(vals.size()!=count)throw new IllegalArgumentException("A física devolveu "+vals.size()+" resultado(s), mas a fórmula exige "+count+".");
        int total=modifier;for(int v:vals){check(v,sides);total+=v;}int natural=count==1&&sides==20?vals.get(0):0;
        return new Result(total,natural,vals.toString()+modifierText(),critical(natural));
    }
    private String modifierText(){return modifier==0?"":(modifier>0?" + ":" - ")+Math.abs(modifier);}
    private static void check(int v,int sides){if(v<1||v>sides)throw new IllegalArgumentException("Face física inválida: "+v+" em d"+sides+".");}
    private static String critical(int natural){if(natural==20)return "20 natural.";if(natural==1)return "1 natural.";return "Rolagem concluída.";}
    public static String normalizeMode(String raw){String s=raw==null?"":raw.toLowerCase(Locale.US);if(s.contains("infort")||s.contains("misfortun")||s.contains("desvantagem"))return "Infortúnio";if(s.contains("fortun")||s.contains("vantagem"))return "Fortuna";return "Normal";}
    public static final class Result {public final int total,natural;public final String detail,critical;Result(int total,int natural,String detail,String critical){this.total=total;this.natural=natural;this.detail=detail;this.critical=critical;}}
}
