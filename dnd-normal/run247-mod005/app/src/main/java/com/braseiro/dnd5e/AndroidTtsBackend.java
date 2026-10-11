package com.braseiro.dnd5e;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AndroidTtsBackend {
    public static final String PREF_ENGINE_PACKAGE="tts_android_engine_package_v2";
    private static final String SHERPA_RELEASE="com.k2fsa.sherpa.onnx.tts.engine";
    private static final String SHERPA_DEBUG="com.k2fsa.sherpa.onnx.tts.engine.debug";
    private static final String LEGACY_KOKORO="com.braseiro.kokoro.ptbr";
    private AndroidTtsBackend(){}

    public static final class EngineOption {
        public final String packageName,label; public final boolean kokoro;
        EngineOption(String p,String l,boolean k){packageName=p;label=l;kokoro=k;}
    }
    public static boolean isKokoroPackage(String p){return SHERPA_RELEASE.equals(p)||SHERPA_DEBUG.equals(p);}
    public static List<EngineOption> installedEngines(Context context){
        ArrayList<EngineOption> out=new ArrayList<EngineOption>();if(context==null)return out;
        PackageManager pm=context.getPackageManager();Intent q=new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE);
        List<ResolveInfo> rs;
        try{rs=pm.queryIntentServices(q,0);}catch(RuntimeException unavailable){return out;}
        if(rs==null)return out;
        Set<String> seen=new HashSet<String>();
        for(ResolveInfo r:rs){
            if(r==null||r.serviceInfo==null)continue;
            String p=r.serviceInfo.packageName;
            if(p==null||p.length()==0||!seen.add(p))continue;
            CharSequence label;
            try{label=r.loadLabel(pm);}catch(RuntimeException unavailable){label=null;}
            String display=label==null||label.length()==0?p:label.toString();
            if(SHERPA_RELEASE.equals(p))display="Braseiro Kokoro PT-BR · neural";
            else if(SHERPA_DEBUG.equals(p))display="Braseiro Kokoro PT-BR · depuração";
            out.add(new EngineOption(p,display,isKokoroPackage(p)));
        }
        Collections.sort(out,new Comparator<EngineOption>(){public int compare(EngineOption a,EngineOption b){
            int rankA=SHERPA_RELEASE.equals(a.packageName)?0:SHERPA_DEBUG.equals(a.packageName)?1:2;
            int rankB=SHERPA_RELEASE.equals(b.packageName)?0:SHERPA_DEBUG.equals(b.packageName)?1:2;
            if(rankA!=rankB)return rankA-rankB;
            int labelOrder=a.label.compareToIgnoreCase(b.label);
            return labelOrder!=0?labelOrder:a.packageName.compareTo(b.packageName);
        }});
        return out;
    }
    public static void migratePreferences(Context c,SharedPreferences p){
        if(p==null)return;
        String saved=p.getString(PREF_ENGINE_PACKAGE,"");
        if(saved.length()>0&&!LEGACY_KOKORO.equals(saved))return;
        String legacy=LEGACY_KOKORO.equals(saved)?saved:p.getString("tts_engine","");
        if(legacy.length()==0)legacy=p.getString("tts_engine_package","");
        String lower=legacy.toLowerCase(Locale.US);
        if(LEGACY_KOKORO.equals(legacy)||lower.contains("db next")||lower.contains("braseiro kokoro")){
            for(EngineOption e:installedEngines(c))if(e.kokoro){p.edit().putString(PREF_ENGINE_PACKAGE,e.packageName).apply();return;}
        }
        if(saved.length()==0&&legacy.length()>0)p.edit().putString(PREF_ENGINE_PACKAGE,legacy).apply();
    }
    public static String resolveSavedEngine(Context c,SharedPreferences p){
        List<EngineOption> es=installedEngines(c);
        String saved=p==null?"":p.getString(PREF_ENGINE_PACKAGE,"");
        for(EngineOption e:es)if(e.packageName.equals(saved))return saved;
        String systemDefault="";
        try{if(c!=null)systemDefault=Settings.Secure.getString(c.getContentResolver(),"tts_default_synth");}catch(RuntimeException ignored){}
        if(systemDefault!=null)for(EngineOption e:es)if(e.packageName.equals(systemDefault))return systemDefault;
        return es.isEmpty()?"":es.get(0).packageName;
    }
    private static String voiceKey(String engine){return "tts_voice_name_v2_"+(engine==null?"":engine.replaceAll("[^A-Za-z0-9_.-]","_"));}
    public static String savedVoiceName(SharedPreferences p,String engine){return p==null?"":p.getString(voiceKey(engine),"");}
    public static void persistVoice(SharedPreferences p,String engine,String voice){if(p!=null&&engine!=null&&voice!=null)p.edit().putString(voiceKey(engine),voice).apply();}
    public static List<Voice> realVoices(TextToSpeech tts){ArrayList<Voice> out=new ArrayList<Voice>();if(tts==null)return out;try{Set<Voice> vs=tts.getVoices();if(vs!=null){Set<String> names=new HashSet<String>();for(Voice v:vs)if(v!=null&&v.getName()!=null&&names.add(v.getName()))out.add(v);}}catch(Throwable ignored){}Collections.sort(out,new Comparator<Voice>(){public int compare(Voice a,Voice b){return voiceLabel(a).compareToIgnoreCase(voiceLabel(b));}});return out;}
    public static Voice findVoice(TextToSpeech tts,String name){if(name==null||name.length()==0)return null;for(Voice v:realVoices(tts))if(name.equals(v.getName()))return v;return null;}
    public static Voice bestVoice(TextToSpeech tts){
        List<Voice> voices=realVoices(tts);
        Voice brazil=null,portugueseLocal=null,portuguese=null;
        for(Voice v:voices){
            Locale l=v.getLocale();if(l==null||!"pt".equalsIgnoreCase(l.getLanguage()))continue;
            boolean br="BR".equalsIgnoreCase(l.getCountry());
            if(br&&!v.isNetworkConnectionRequired())return v;
            if(br&&brazil==null)brazil=v;
            if(!v.isNetworkConnectionRequired()&&portugueseLocal==null)portugueseLocal=v;
            if(portuguese==null)portuguese=v;
        }
        if(brazil!=null)return brazil;
        if(portugueseLocal!=null)return portugueseLocal;
        return portuguese!=null?portuguese:(voices.isEmpty()?null:voices.get(0));
    }
    public static String voiceLabel(Voice v){if(v==null)return "";Locale l=v.getLocale();String locale=l==null?"":l.toLanguageTag();return v.getName()+(locale.length()>0?" · "+locale:"")+(v.isNetworkConnectionRequired()?" · requer rede":"");}
}
