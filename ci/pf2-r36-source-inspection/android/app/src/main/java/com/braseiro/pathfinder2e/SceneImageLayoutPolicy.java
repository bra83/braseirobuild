package com.braseiro.pathfinder2e;

/** Regras puras de layout para o preview vertical das imagens geradas pelo Gemini. */
public final class SceneImageLayoutPolicy {
    private SceneImageLayoutPolicy(){}

    public static int previewWidth(int availablePx,int maxPx,int minPx){
        int safeAvailable=Math.max(1,availablePx);
        int safeMax=Math.max(1,maxPx);
        int safeMin=Math.max(1,Math.min(minPx,safeMax));
        int bounded=Math.min(safeAvailable,safeMax);
        return safeAvailable<safeMin?safeAvailable:Math.max(safeMin,bounded);
    }

    public static int heightForWidth(int widthPx){
        if(widthPx<=0)return 1;
        return Math.max(1,(int)Math.round(widthPx*(16.0/9.0)));
    }
}
