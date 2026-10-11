package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/** Núcleo puro/testável das transições e travas causais do clima. */
public final class WeatherCausalityKernel {
    private WeatherCausalityKernel(){}

    public static final class Transition {
        public final String event;
        public final int stage;
        public final int activeBeats;
        public final boolean cleared;
        public final boolean boundaryTransition;
        Transition(String e,int s,int b,boolean c,boolean boundary){event=e;stage=s;activeBeats=b;cleared=c;boundaryTransition=boundary;}
    }

    public static Transition advance(String previousEvent,int previousStage,int previousActiveBeats,String previousClimate,String climate,int progressSteps,String forcedEvent,String candidate,boolean candidateSelected){
        String event="";int stage=0;int active=Math.max(0,previousActiveBeats);boolean cleared=false,boundary=false;
        boolean previousActive=previousEvent!=null&&!previousEvent.trim().isEmpty()&&previousStage>0;
        boolean climateChanged=previousClimate!=null&&!previousClimate.trim().isEmpty()&&!safe(climate).equals(previousClimate);
        if(previousActive){
            event=previousEvent;stage=Math.max(1,previousStage);
            if(climateChanged){boundary=true;stage=stage>=3?4:Math.max(2,stage);}
            else if(progressSteps>0){
                for(int i=0;i<progressSteps&&stage<5;i++){
                    if(stage<3)stage++;
                    else if(stage==3){active++;if(active>=3)stage=4;}
                    else if(stage==4)stage=5;
                }
            }
        }else if(forcedEvent!=null&&!forcedEvent.trim().isEmpty()){
            event=forcedEvent;stage=3;
        }else if(candidateSelected&&candidate!=null&&!candidate.trim().isEmpty()){
            event=candidate;stage=1;
        }
        if(stage>=5){event="";stage=0;active=0;cleared=true;}
        if(stage==0&&!cleared)event="";
        return new Transition(event,stage,active,cleared,boundary);
    }

    public static String aftermathFor(String event){
        String e=fold(event);
        if(contains(e,"chuva","tempestade"))return "Lama, superfícies molhadas, galhos e detritos continuam afetando circulação e rastros depois da chuva.";
        if(contains(e,"nevasca","neve"))return "Neve acumulada, referências cobertas e novos rastros persistem depois que as rajadas cessam.";
        if(contains(e,"areia"))return "Poeira e areia depositadas continuam redesenhando superfícies, trilhas e rastros.";
        if(contains(e,"nevoa"))return "Umidade e condensação permanecem por algum tempo nas áreas baixas e superfícies expostas.";
        return "";
    }

    public static boolean isSevereEvent(String event){String e=fold(event);return contains(e,"tempestade","chuva forte","nevasca","areia");}

    public static boolean isOutdoorCommitment(String text){
        String t=fold(text);
        return contains(t,"patrulh","caravana","viaj","viagem","marcha","entrega","mensage","correio","caca","exped","escolta","transporte","colheita","campo","estrada","rota","trilha","porto","carga","feira externa","mercado externo","construcao externa","exploracao","batedor");
    }

    public static boolean terrainImpliesWater(String text){
        String t=fold(text);
        return contains(t,"rio","river","lago","lake","mar","ocean","costa","coast","pantano","swamp","marsh","riacho","stream");
    }

    private static boolean contains(String text,String... terms){String t=fold(text);for(String x:terms)if(t.contains(fold(x)))return true;return false;}
    private static String safe(String s){return s==null?"":s;}
    private static String fold(String value){if(value==null)return "";String s=Normalizer.normalize(value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return s.replaceAll("[^a-z0-9]+"," ").trim();}
}
