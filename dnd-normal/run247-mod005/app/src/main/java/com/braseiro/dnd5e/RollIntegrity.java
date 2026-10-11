package com.braseiro.dnd5e;

import java.util.List;

/** Assinatura lógica reprodutível de uma rolagem física. */
public final class RollIntegrity {
    private RollIntegrity(){}
    public static String signature(String formula,List<Integer> physicalFaces,int modifier,long seed,int total){
        StringBuilder b=new StringBuilder();b.append(formula==null?"":formula).append('|');if(physicalFaces!=null)for(int i=0;i<physicalFaces.size();i++){if(i>0)b.append(',');b.append(physicalFaces.get(i));}b.append('|').append(modifier).append('|').append(seed).append('|').append(total);return IntegrityHash.sha256(b.toString());
    }
}
