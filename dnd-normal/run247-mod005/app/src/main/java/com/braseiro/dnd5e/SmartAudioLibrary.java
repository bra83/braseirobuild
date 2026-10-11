package com.braseiro.dnd5e;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Biblioteca de áudio orientada por cena.
 * Ambiência, música e SFX são escolhidos separadamente; um tipo não compete com o outro.
 */
public final class SmartAudioLibrary {
    private final Context context;
    private final ContentResolver resolver;
    private final AudioController audio;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService audioPlanner=Executors.newSingleThreadExecutor();
    private final ExecutorService sfxPlanner=Executors.newSingleThreadExecutor();
    private final ExecutorService persistence=Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<String,JSONObject> preparedVoiceSfx=new ConcurrentHashMap<String,JSONObject>();
    private final File indexFile,keywordsFile;
    private final AudioCatalogDb catalogDb;
    private final Random rotationRandom=new Random();
    private JSONArray sounds=new JSONArray();
    private JSONObject keywordOverrides=new JSONObject();
    private final HashMap<String,Long> lastPlayed=new HashMap<String,Long>();
    private int cooldownSeconds=15;
    private String catalogSource="";
    private int catalogEntryCount=0;
    private int catalogMatchedCount=0;
    private String lastPlan="Nenhum plano de áudio executado.";
    private String lastRotationDebug="A rotação ainda não selecionou nenhuma faixa.";
    private boolean automaticSfxEnabled=false;
    private volatile boolean voiceNarrationActive=false;
    private volatile long audioPlanGeneration=0L;
    private volatile long continuousPlanGeneration=0L;
    private volatile long voiceCueGeneration=0L;
    private final ArrayList<Runnable> pendingSfx=new ArrayList<Runnable>();
    private long lastAutomaticSfxAt=0L;
    private int globalSfxGapSeconds=3;

    private static final String[] AUDIO_EXT={".mp3",".wav",".ogg",".m4a",".flac",".aac",".opus",".aiff",".aif"};
    private static final String[][] SYNONYMS={
        {"porta","door,portao,entrada,fechadura,dobradiça,dobradica"},{"batida","knock,batida,bater,pancada"},
        {"armadura","armor,armadura,metal,ferro,cota,couraça,couraca"},{"espada","sword,espada,lamina,lâmina,blade"},
        {"chuva","rain,chuva,tempestade,storm,trovao,trovão"},{"floresta","forest,woods,floresta,mata,bosque,selva"},
        {"taverna","tavern,inn,taverna,estalagem,pub,bebida,caneca"},{"fogo","fire,fogo,lareira,bonfire,fogueira,chamas"},
        {"vento","wind,vento,brisa,vendaval"},{"passos","steps,footsteps,passos,caminhada,correr,corrida"},
        {"monstro","monster,monstro,criatura,beast,fera"},{"rosnado","growl,roar,rosnado,rugido"},
        {"agua","water,agua,água,rio,river,mar,sea,ocean,oceano"},{"cavalo","horse,cavalo,hoof,casco,galope"},
        {"cidade","city,town,village,cidade,vila,aldeia,mercado,rua,multidao,multidão"},
        {"masmorra","dungeon,masmorra,calabouco,cripta,ruina,ruína"},{"caverna","cave,cavern,caverna,gruta,eco"},
        {"combate","combat,battle,fight,combate,batalha,luta,war,guerra"},{"misterio","mystery,mistério,misterio,suspense,investigacao,investigação"},
        {"tensao","tension,tensão,tensao,danger,perigo,threat,ameaça,ameaca"},{"calma","calm,peace,calma,paz,serene,sereno"},
        {"triste","sad,sorrow,triste,tristeza,lamento,mourning"},{"heroico","heroic,heroico,heróico,triunfo,victory,vitoria,vitória"},
        {"pantano","swamp,swamps,pantano,pântano,brejo"},{"trovao","thunder,trovao,trovão,rumble,storm"},
        {"risada","laugh,laughter,risada,risadas,cheer"},{"escrita","write,writing,pencil,lapis,lápis,paper,papel"},
        {"multidao","crowd,crowds,walla,mob,people,multidao,multidão"},{"montanha","mountain,canyon,montanha,desfiladeiro"},
        {"vegetacao","vegetation,vegetacao,vegetação,leaves,folhas"},{"carruagem","stagecoach,carriage,cart,wagon,coach,carroca,carroça,carruagem"},
        {"bigorna","anvil,bigorna,forge,forja,blacksmith"},{"martelo","hammer,martelo,malho"},
        {"respiracao","breath,breathing,respiracao,respiração"},{"tecido","cloth,jacket,jeans,tecido,roupa"},
        {"madeira","wood,wooden,madeira"},{"impacto","impact,impacto,hit"}
    };

    /** Famílias concretas: SFX só entra quando a cena descreve um acontecimento correspondente. */
    private static final String[][] SFX_FAMILIES={
        {"madeira","bate na madeira,batida na madeira,batidas na madeira,bater na madeira,knock on wood,knocking on wood,wood knock","madeira,wood,wooden,knock,knocking,door knock"},
        {"bigorna","martelo bate na bigorna,martelo na bigorna,batida de bigorna,hammer hits anvil,hammer on anvil","bigorna,anvil,hammer,blacksmith,forge,cymbal anvil"},
        {"carroca","carroca,carroça,carruagem,carroca se aproxima,carroça se aproxima,carruagem se aproxima,carroca passa,carroça passa,carruagem passa,rodas de carroca,rodas de carroça,stagecoach approaches,carriage approaches,cart approaches,wagon approaches","carroca,carroça,carruagem,stagecoach,carriage,cart,wagon,coach"},
        {"porta","porta range,porta se abre,porta abriu,abre a porta,abriu a porta,fecha a porta,fechou a porta,porta bate,porta se fecha,door creak,door opens,door closes,door slams","porta,portao,door,gate,lock,hinge,creak"},
        {"batida","batida na porta,batidas na porta,bate a porta,batem a porta,bater na porta,knock,knocking,pancada na porta","batida,knock,knocking,pancada,door hit"},
        {"passos","ouve passos,ouvem passos,passos ecoam,passos se aproximam,passos correm,footsteps,steps echo,approaching steps","passos,passo,footsteps,steps,walk,walking,run,running"},
        {"espada","desembainha a espada,desembainhou a espada,lamina raspa,lâmina raspa,espadas se chocam,espada colide,golpe de espada acerta,sword drawn,swords clash,blade scrapes","espada,sword,lamina,blade,slash,unsheathe,clash"},
        {"metal","armadura range,armadura raspa,metal raspa,metal se choca,ferro tilinta,cota de malha tilinta,clang,clank,armor creaks,metal scrapes","armadura,armor,armour,metal,ferro,chainmail,clang,clank,scrape"},
        {"corrente","correntes arrastam,corrente arrasta,corrente tilinta,chains drag,chains rattle,grilhao bate,grilhão bate","corrente,chain,chains,shackle,rattle,drag"},
        {"rugido","solta um rugido,rugido ecoa,rosna alto,roar echoes,growls,roars","rugido,roar,rosnado,growl"},
        {"grito","solta um grito,grito ecoa,grita de dor,scream,shriek,shout","grito,scream,shriek,shout"},
        {"cavalo","cavalos galopam,cavalo galopa,cascos batem,galope se aproxima,horses gallop,hooves thunder","cavalo,horse,galope,gallop,casco,hoof"},
        {"trovao","trovao ribomba,trovão ribomba,trovao explode,trovão explode,thunder cracks,thunder rumbles,relampago estala,relâmpago estala","trovao,thunder,lightning,rumble"},
        {"fogo","fogo crepita,chamas crepitam,chamas estalam,lareira crepita,fire crackles,flames crackle","fogo,fire,flame,crackle,crepitar"},
        {"vidro","vidro se quebra,quebra o vidro,vidro estilhaca,vidro estilhaça,glass shatters,glass breaks","vidro,glass,shatter,break"},
        {"sino","sino toca,sinos tocam,sino ressoa,bell rings,bells toll,campainha toca","sino,bell,campainha,chime,toll"},
        {"moeda","moedas caem,moeda cai,ouro cai,coins drop,coins spill","moeda,coin,coins,gold drop"},
        {"flecha","flecha e disparada,flecha é disparada,solta a flecha,dispara o arco,arrow flies,bow fires","flecha,arrow,bow shot,archery"},
        {"explosao","explosao sacode,explosão sacode,algo explode,explodiu,explosion,blast,boom","explosao,explosion,blast,boom"},
        {"magia","conjura o feitico,conjura o feitiço,lança o feitiço,lanca o feitico,spell cast,magia irrompe,raio arcano dispara","spell,magic,magia,feitico,feitiço,arcane,cast"},
        {"queda","corpo cai no chao,corpo cai no chão,despenca no chão,queda pesada,body falls,crashes to the ground","queda,fall,body fall,crash"},
        {"agua","mergulha na agua,mergulha na água,cai na agua,cai na água,grande salpico,grande salpico,splash","splash,salpico,agua,water"},
        {"combate","soco acerta,chute acerta,golpe acerta,impacto seco,punch lands,kick lands,weapon impact","soco,punch,chute,kick,hit,impact"}
    };

    /**
     * Mapeamento semântico independente do idioma do nome do arquivo.
     * O primeiro campo é o evento do motor; o segundo são palavras esperadas
     * no arquivo; o terceiro contém reforços e o quarto bloqueios.
     */
    private static final String[][] EVENT_FILE_RULES={
        {"wood_knock","knock,knocking,wood,madeira,prego","door knock,knocking on wood","music,ambience,tavern"},
        {"knock","knock,knocking,door,doors,porta","knocking on wood","music,ambience"},
        {"door","door,porta,opening,open,closing,close,creak","old door,wooden door","music,ambience"},
        {"anvil","anvil,bigorna,blacksmith,armory,forge,martelo,hammer","blacksmith 01,blacksmith 02","music"},
        {"cart","stagecoach,carriage,cart,wagon,coach,carroca,carruagem","horse hoof,wheels","music,ambience"},
        {"footsteps","footstep,footsteps,passos,walk,walking","wood,dirt,forest,grass,gravel,mud,rocks,snow,water","music,ambience"},
        {"sword","sword,blade,espada,dagger,rapier,combat","draw,scrape,clash,hit,swing,slash,strike","music,ambience"},
        {"metal","metal,armor,armour,armadura,shield,blade,chain","hit,scrape,block,break,clang","music,ambience"},
        {"chains","chain,chains,corrente,heavy chains","rattle,drag","music,ambience"},
        {"roar","roar,growl,monster,dragon,orc,goblin,troll,rugido,rosnado","vocal","music,ambience"},
        {"scream","scream,screams,grito,voices,pain,death","male,female,fearful","music,ambience"},
        {"horse","horse,cavalo,neigh,gallop,hoof,whinny,clydesdale","pass by,walk","music,ambience"},
        {"thunder","thunder,trovao,storm,weather,rumble","distant,cracks","music,ambience"},
        {"ignition","fire,activate,magic,flame,fogo","ignite,burst","music,ambience,fireplace,crackle"},
        {"fire_crackle","fireplace,crackle,crackling,lareira,wood","fire","music"},
        {"glass","glass,vidro,crash,debris,smash,break","drop,impact","music,ambience"},
        {"bell","bell,church,sino","distant","music,ambience"},
        {"coins","coin,coins,moeda,gold","drop,spill","music,ambience"},
        {"arrow","bow,arrow,flecha,combat","fire,impact,draw,pull","music,ambience"},
        {"explosion","explosion,blast,boom,impact,burst","fire","music,ambience"},
        {"magic","magic,magical,spell,arcane,magia,sting,invisibility","activate,buff","music,ambience"},
        {"fall","fall,landing,jump,impact,body,queda","footsteps","music,ambience"},
        {"splash","water,splash,watering,agua,waves","footsteps","music,ambience"},
        {"impact","impact,hit,punch,kick,strike,attack,combat","flesh,wood,metal,shield","music,ambience"},
        {"writing","writing,write,pencil,crayon,lapis,escrita,paper,pergaminho","objects","music,ambience"},
        {"drink","glassware,mug,bottle,caneca,beer,cerveja,enchendo,copos","food drink","music,ambience"},
        {"laugh","laugh,laughter,risada,risadas,cheer","crowd","music,ambience"},
        {"rocks","rock,rocks,stone,stones,pedra,pedras","fall,handle,movement","music,ambience"}
    };

    private static final String[] MUSIC_FILE_MARKERS={
        "song","theme","track","melody","score","bgm","soundtrack","basic rpg intro",
        "calm loop","chill rpg","chill song","crystal mountain secret","cursed lands",
        "dungeon of madness","dungeon of sorrow","from dusk till dawn","in the arena",
        "bardic melody","medievalfantasy rpg loop","the stronghold"
    };
    private static final String[] AMBIENCE_FILE_MARKERS={
        "ambience","ambiencia","atmosphere","soundscape","forest","rain","village","town",
        "city","cave","cavern","river","shore","lake","swamp","desert","nature","crickets",
        "crowd","walla","tavern","estalagem","blacksmith armory","ship","port","barn",
        "farm","countryside","sewer","subterraneo","beach","waves","mine","library",
        "corridor","shop","store","chambers of hell","wind","vento","night","day in nature",
        "lareira","fireplace"
    };

    private static final String GENERIC_SFX_TERMS="sfx,sound,sounds,audio,effect,effects,efeito,efeitos,foley,pack,collection,biblioteca,library,fantasy,fantasia,dnd,rpg,game,jogo,track,loop,ambient,ambience,ambiencia,music,musica,trilha,theme,tema,default,generic,aventura,adventure,misterio,mystery,tensao,tension,calma,calm,heroico,heroic,triste,sad,floresta,forest,cidade,city,taverna,tavern,masmorra,dungeon,caverna,cave";

    public SmartAudioLibrary(Context c,AudioController a){
        context=c.getApplicationContext();resolver=context.getContentResolver();audio=a;
        indexFile=new File(context.getFilesDir(),"audio_index_v3.json");
        keywordsFile=new File(context.getFilesDir(),"audio_keywords_v3.json");
        AudioCatalogDb.recoverIfNeeded(context);
        catalogDb=new AudioCatalogDb(context);
        load();
        ensureBundledCatalog();
        rebuildCatalogIndex();
    }

    public void setCooldownSeconds(int s){cooldownSeconds=Math.max(5,s);}
    public void setGlobalSfxGapSeconds(int s){globalSfxGapSeconds=Math.max(1,Math.min(30,s));}
    public void setAutomaticSfxEnabled(boolean enabled){automaticSfxEnabled=enabled;if(!enabled){voiceNarrationActive=false;cancelPendingSfx();audio.stopSfx();}}
    public void beginVoiceNarration(){voiceNarrationActive=true;voiceCueGeneration++;preparedVoiceSfx.clear();synchronized(lastPlayed){lastPlayed.clear();}lastAutomaticSfxAt=0L;cancelPendingSfx();audio.stopSfx();}
    public void endVoiceNarration(){voiceNarrationActive=false;voiceCueGeneration++;preparedVoiceSfx.clear();}
    public void cancelVoiceNarration(){voiceNarrationActive=false;voiceCueGeneration++;preparedVoiceSfx.clear();cancelPendingSfx();audio.stopSfx();}
    public boolean isAutomaticSfxEnabled(){return automaticSfxEnabled;}
    public String summary(){
        int amb=0,mus=0,sfx=0,unknown=0,manualOnly=0;for(int i=0;i<sounds.length();i++){JSONObject sound=sounds.optJSONObject(i);if(isAutoBlockedSound(sound))manualOnly++;String t=typeOf(sound);if("ambience".equals(t))amb++;else if("music".equals(t))mus++;else if("sfx".equals(t))sfx++;else unknown++;}
        String extra=unknown>0?" • "+unknown+" sem categoria (não tocam automaticamente)":"";
        String blocked=manualOnly>0?" • "+manualOnly+" vinhetas somente manual":"";
        String catalog=catalogEntryCount>0?" • biblioteca_sons.json: "+catalogMatchedCount+" de "+catalogEntryCount+" nomes encontrados":" • catálogo JSON não encontrado";
        return sounds.length()+" sons indexados • "+amb+" ambiências • "+mus+" músicas • "+sfx+" SFX"+blocked+extra+catalog;
    }
    public String lastPlanSummary(){return lastPlan;}
    public String catalogReport(){/*BR_JSON_WRAP*/try{
        if(catalogEntryCount<=0)return "Nenhum biblioteca_sons.json foi carregado.";
        HashSet<String> present=new HashSet<String>();
        for(int i=0;i<sounds.length();i++){
            JSONObject sound=sounds.optJSONObject(i);
            if(sound!=null)present.add(normalizedFilename(sound.optString("name")));
        }
        LinkedHashMap<String,Boolean> missing=new LinkedHashMap<String,Boolean>();
        java.util.Iterator<String> it=keywordOverrides.keys();
        while(it.hasNext()){
            String key=it.next();
            if(!key.startsWith("__filename:"))continue;
            JSONObject rule=keywordOverrides.optJSONObject(key);
            String filename=rule==null?key.substring(11):rule.optString("filename",key.substring(11));
            if(!present.contains(key.substring(11)))missing.put(filename,true);
        }
        StringBuilder b=new StringBuilder();
        b.append("Catálogo: ").append(catalogSource.length()>0?catalogSource:"biblioteca_sons.json").append('\n');
        b.append("Entradas de áudio: ").append(catalogEntryCount).append("\nEncontradas na pasta: ").append(catalogMatchedCount).append("\nNão encontradas: ").append(missing.size());
        if(!missing.isEmpty()){
            b.append("\n\nNomes não encontrados (até 60):");
            int n=0;
            for(String name:missing.keySet()){
                b.append("\n• ").append(name);
                if(++n>=60){
                    if(missing.size()>n)b.append("\n• … e mais ").append(missing.size()-n);
                    break;
                }
            }
        }
        return b.toString();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}


    public String catalogV2Report(){
        return catalogDb.report()+"\n\nÚltima rotação:\n"+lastRotationDebug;
    }

    /** Recria somente o índice de áudio. O banco da campanha nunca é aberto aqui. */
    private synchronized void rebuildCatalogIndex(){
        try{
            for(int i=0;i<sounds.length();i++){
                JSONObject sound=sounds.optJSONObject(i);if(sound==null)continue;
                enrichCatalogMetadata(sound);
            }
            catalogDb.synchronize(sounds);
        }catch(Throwable error){AppLog.w("SmartAudioLibrary","O índice SQLite de áudio não pôde ser reconstruído; o índice JSON continuará disponível.",error);}
    }

    private void enrichCatalogMetadata(JSONObject sound)throws Exception{/*BR_JSON_WRAP*/try{
        String channel=typeOf(sound);String corpus=soundCorpus(sound);JSONObject rule=ruleFor(sound);
        String role="sfx".equals(channel)?"sfx":("music".equals(channel)?"music":(hasWeatherMarker(corpus)?"climate":"base"));
        if(rule!=null&&rule.optString("role").length()>0)role=normalize(rule.optString("role"));
        String group=rule!=null&&rule.optString("group").length()>0?normalize(rule.optString("group")).replace(' ','_'):deriveVariantGroup(channel,role,corpus);
        String environment=rule!=null&&rule.optString("environment").length()>0?normalize(rule.optString("environment")):deriveEnvironment(corpus);
        String weather=rule!=null&&rule.optString("weather").length()>0?normalize(rule.optString("weather")):deriveWeather(corpus);
        String intensity=rule!=null&&rule.optString("intensity").length()>0?normalize(rule.optString("intensity")):deriveIntensity(corpus);
        String daypart=rule!=null&&rule.optString("daypart").length()>0?normalize(rule.optString("daypart")):deriveDaypart(corpus);
        boolean loopable=rule!=null&&rule.has("loop")?rule.optBoolean("loop",true):!"sfx".equals(channel)&&!isContinuousCandidateBlocked(sound,channel);
        sound.put("catalog_channel",channel);
        sound.put("catalog_role",role);
        sound.put("variant_group",group);
        sound.put("duplicate_family",duplicateFamily(sound.optString("name",sound.optString("path",""))));
        sound.put("environment",environment);
        sound.put("weather",weather);
        sound.put("intensity",intensity);
        sound.put("daypart",daypart);
        sound.put("loopable",loopable);
        double weight=rule==null?1.0:Math.max(.1,Math.min(5.0,rule.optDouble("weight",1.0)));
        sound.put("catalog_weight",weight);
        AudioCatalogDb.Stats stats=catalogDb.stats(AudioCatalogDb.soundId(sound));
        if(stats.durationMs>0L)sound.put("duration_ms",stats.durationMs);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static boolean hasWeatherMarker(String corpus){return containsAny(corpus,"rain,chuva,drizzle,garoa,downpour,tempestade,storm,thunder,trovao,trovão,wind,vento,gust,vendaval,snow,neve");}
    private static String deriveVariantGroup(String channel,String role,String corpus){
        String n=normalize(corpus);
        if("music".equals(channel)){
            if(containsAny(n,"battle,combat,arena,tense,tension,dark,cursed,madness"))return "music_tense";
            if(containsAny(n,"mystery,misterio,secret,shadow,sorrow,dungeon"))return "music_mystery";
            if(containsAny(n,"sad,triste,sorrow,mourning,melancholy"))return "music_sad";
            if(containsAny(n,"calm,chill,happy,light,peace,serene"))return "music_calm";
            return "music_adventure";
        }
        if("sfx".equals(channel)){
            if(containsAny(n,"knock,knocking,porta,door"))return "sfx_door";
            if(containsAny(n,"footstep,footsteps,passos"))return "sfx_footsteps";
            if(containsAny(n,"sword,blade,espada,dagger,rapier"))return "sfx_blade";
            if(containsAny(n,"horse,cavalo,gallop,neigh"))return "sfx_horse";
            if(containsAny(n,"thunder,trovao,trovão"))return "sfx_thunder";
            return "sfx_generic";
        }
        if("climate".equals(role)){
            boolean rain=containsAny(n,"rain,chuva,drizzle,garoa,downpour");
            if(containsAny(n,"thunder,trovao,trovão,rumbling")&&!rain)return "weather_thunder";
            if(containsAny(n,"wind,vento,gust,vendaval")&&!rain)return "weather_wind";
            if(containsAny(n,"snow,neve,frost")&&!rain)return "weather_snow";
            if(!rain)return "weather_general";
            if(containsAny(n,"roof,telhado,gutter,calha"))return "rain_roof";
            if(containsAny(n,"forest,floresta,vegetation,vegetacao,vegetação,leaves,folhas,tree"))return "rain_forest";
            if(containsAny(n,"downpour,tempestade,storm,heavy,tropical,thunder"))return "rain_heavy";
            if(containsAny(n,"light,drizzle,garoa,calm"))return "rain_light";
            return "rain_general";
        }
        String env=deriveEnvironment(n);
        return "ambience_"+(env.length()==0?"generic":env);
    }
    private static String deriveEnvironment(String corpus){
        String n=normalize(corpus);
        if(containsAny(n,"tavern,taverna,estalagem,hospedaria,inn,pub"))return "tavern";
        if(containsAny(n,"forest,floresta,woods,bosque,nature,natureza"))return "forest";
        if(containsAny(n,"cave,cavern,caverna,gruta"))return "cave";
        if(containsAny(n,"dungeon,masmorra,subterraneo,mine,mineshaft,cripta"))return "dungeon";
        if(containsAny(n,"city,cidade,capital,town,medieval town"))return "city";
        if(containsAny(n,"village,vila,aldeia,countryside"))return "village";
        if(containsAny(n,"port,porto,ship,navio,onboard"))return "port";
        if(containsAny(n,"river,rio,lake,lago,shore,praia,waves,water"))return "water";
        if(containsAny(n,"swamp,swamps,pantano,pântano,brejo"))return "swamp";
        if(containsAny(n,"desert,deserto"))return "desert";
        if(containsAny(n,"mountain,montanha,canyon,cliff"))return "mountain";
        if(containsAny(n,"farm,fazenda,barn,celeiro"))return "farm";
        if(containsAny(n,"library,biblioteca"))return "library";
        if(containsAny(n,"blacksmith,ferreiro,armory,forge,forja"))return "blacksmith";
        if(containsAny(n,"indoor,indoors,interior,room,corridor"))return "interior";
        if(containsAny(n,"outdoor,outdoors,exterior,road,estrada"))return "outdoor";
        return "generic";
    }
    private static String deriveWeather(String corpus){String n=normalize(corpus);if(containsAny(n,"downpour,tempestade,storm,thunder,trovao,trovão"))return "storm";if(containsAny(n,"rain,chuva,drizzle,garoa"))return "rain";if(containsAny(n,"wind,vento,gust,vendaval"))return "wind";if(containsAny(n,"snow,neve,frost"))return "snow";return "";}
    private static String deriveIntensity(String corpus){String n=normalize(corpus);if(containsAny(n,"heavy,loud,downpour,tempestade,storm,intense,forte"))return "heavy";if(containsAny(n,"light,drizzle,calm,quiet,leve,garoa"))return "light";return "medium";}
    private static String deriveDaypart(String corpus){String n=normalize(corpus);if(containsAny(n,"night,noite,crickets,madrugada"))return "night";if(containsAny(n,"day,dia,morning,manha,manhã,summer day"))return "day";return "any";}

    private long ensureDuration(JSONObject sound){/*BR_JSON_WRAP*/try{
        long known=sound==null?0L:sound.optLong("duration_ms",0L);if(known>0L)return known;
        if(sound==null||"sfx".equals(sound.optString("catalog_channel")))return 0L;
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        try{
            retriever.setDataSource(context,Uri.parse(sound.optString("uri")));
            String raw=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long value=raw==null?0L:Long.parseLong(raw);
            if(value>0L){sound.put("duration_ms",value);catalogDb.updateDuration(sound,value);return value;}
        }catch(Exception ignored){AppLog.ignored("SmartAudioLibrary","Falha tolerada; a execução continuou.",ignored);}finally{try{retriever.release();}catch(Exception ignored){AppLog.ignored("SmartAudioLibrary","Falha tolerada; a execução continuou.",ignored);}}
        return 0L;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public boolean markPlayed(JSONObject sound,final String channel,final String role,final String sceneKey){
        if(sound==null)return false;
        final JSONObject copy;
        try{copy=new JSONObject(sound.toString());}
        catch(Throwable error){return false;}
        persist(new Runnable(){public void run(){try{catalogDb.markPlayed(copy,channel,role,sceneKey);}catch(Throwable error){AppLog.w("SmartAudioLibrary","Falha não fatal ao registrar rotação de áudio.",error);}}});
        return true;
    }
    public void markFailure(JSONObject sound){if(sound==null)return;final JSONObject copy;try{copy=new JSONObject(sound.toString());}catch(Throwable error){return;}persist(new Runnable(){public void run(){try{catalogDb.markFailure(copy);}catch(Throwable failure){AppLog.w("SmartAudioLibrary","Falha não fatal ao registrar arquivo de áudio inválido.",failure);}}});}
    public synchronized void markFailureByUri(String uri){
        if(uri==null||uri.length()==0)return;
        for(int i=0;i<sounds.length();i++){JSONObject sound=sounds.optJSONObject(i);if(sound!=null&&uri.equals(sound.optString("uri"))){markFailure(sound);return;}}
    }

    private int ruleCount(){
        int n=0;java.util.Iterator<String> it=keywordOverrides.keys();while(it.hasNext()){String k=it.next();if(!k.startsWith("__"))n++;}
        JSONArray a=keywordOverrides.optJSONArray("__aliases");return n+(a==null?0:a.length());
    }

    private void load(){
        try{if(indexFile.exists())sounds=new JSONArray(read(indexFile));if(keywordsFile.exists())keywordOverrides=new JSONObject(read(keywordsFile));catalogSource=keywordOverrides.optString("__catalog_source","");catalogEntryCount=keywordOverrides.optInt("__catalog_count",ruleCount());catalogMatchedCount=keywordOverrides.optInt("__catalog_matched",countCatalogMatches());}
        catch(Exception ignored){sounds=new JSONArray();keywordOverrides=new JSONObject();catalogSource="";catalogEntryCount=0;catalogMatchedCount=0;}
    }

    /** Reabre o índice produzido pelo WorkManager e aplica os JSONs encontrados. */
    public synchronized int reloadAfterInternalImport() throws Exception {
        catalogDb.clearAll();
        sounds=new JSONArray();keywordOverrides=new JSONObject();catalogSource="";catalogEntryCount=0;catalogMatchedCount=0;
        load();
        File catalogSourceFile=new File(context.getFilesDir(),"audio_catalog_source_v3.json");
        File overridesSourceFile=new File(context.getFilesDir(),"audio_overrides_source_v3.json");
        if(catalogSourceFile.isFile()){
            InputStream in=new FileInputStream(catalogSourceFile);try{importJsonCatalog(in,"biblioteca_sons.json importado");}finally{in.close();}
        }else ensureBundledCatalog();
        if(overridesSourceFile.isFile()){
            InputStream in=new FileInputStream(overridesSourceFile);try{importJsonOverrides(in,"audio_overrides.json importado");}finally{in.close();}
        }
        save();catalogDb.reactivateAll();rebuildCatalogIndex();purgeInvalidContinuousAudio();return sounds.length();
    }

    /** O worker é o caminho padrão. A varredura SAF direta fica apenas como compatibilidade. */
    public File internalAudioDirectory(){return new File(context.getFilesDir(),"audio_library_v3");}

    public void startPlaybackSession(final String sessionId,final String sceneId,final String backend){
        persist(new Runnable(){public void run(){try{catalogDb.startSession(sessionId,sceneId,backend);}catch(Throwable error){AppLog.w("SmartAudioLibrary","Falha não fatal ao abrir sessão de áudio.",error);}}});
    }

    public void finishPlaybackSession(final String sessionId,final String status){
        persist(new Runnable(){public void run(){try{catalogDb.finishSession(sessionId,status);}catch(Throwable error){AppLog.w("SmartAudioLibrary","Falha não fatal ao encerrar sessão de áudio.",error);}}});
    }

    private void persist(Runnable task){
        try{if(!persistence.isShutdown())persistence.execute(task);}
        catch(RejectedExecutionException ignored){/* encerramento do aplicativo */}
    }

    /**
     * O catálogo enviado com o projeto serve de dicionário inicial. Quando a
     * pasta escolhida contém outro biblioteca_sons.json, ele o substitui.
     */
    private void ensureBundledCatalog(){
        if(catalogEntryCount>0&&keywordOverrides.length()>0)return;
        InputStream in=null;
        try{
            in=context.getAssets().open("audio/biblioteca_sons.json");
            importJsonCatalog(in,"catálogo incorporado");
            save();
        }catch(Exception ignored){
            // A biblioteca continua funcional pela inferência dos nomes.
        }finally{try{if(in!=null)in.close();}catch(Exception ignored){AppLog.ignored("SmartAudioLibrary","Falha tolerada; a execução continuou.",ignored);}}
    }

    private void save()throws Exception{write(indexFile,sounds.toString());write(keywordsFile,keywordOverrides.toString());}

    public int scan(Uri tree)throws Exception{/*BR_JSON_WRAP*/try{
        if(tree==null)throw new Exception("Selecione uma pasta de sons.");
        String root=DocumentsContract.getTreeDocumentId(tree);List<DocRow> rootRows=queryChildrenStable(tree,root);
        CatalogCandidate catalog=catalogFromRows(tree,rootRows,"",0);
        CatalogCandidate overrides=overrideFromRows(tree,rootRows,"",0);
        // O formato oficial fica na raiz. Só faz a busca recursiva mais cara quando necessário.
        if(catalog==null)catalog=findCatalog(tree,root,"",new HashSet<String>(),0);
        if(overrides==null)overrides=findOverrides(tree,root,"",new HashSet<String>(),0);
        if(catalog!=null){InputStream in=resolver.openInputStream(catalog.uri);if(in!=null){try{importJsonCatalog(in,catalog.path);}finally{in.close();}}}
        if(overrides!=null){InputStream in=resolver.openInputStream(overrides.uri);if(in!=null){try{importJsonOverrides(in,overrides.path);}finally{in.close();}}}
        JSONArray out=new JSONArray();Set<String> seenDocuments=new HashSet<String>();Set<String> seenFolders=new HashSet<String>();seenFolders.add(root);
        scanRows(tree,rootRows,"",out,seenDocuments,seenFolders,0);
        sounds=out;catalogMatchedCount=countCatalogMatches();keywordOverrides.put("__catalog_matched",catalogMatchedCount);save();rebuildCatalogIndex();return sounds.length();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private CatalogCandidate catalogFromRows(Uri tree,List<DocRow> rows,String path,int depth){for(DocRow row:rows)if(!isDirectory(row.mime)&&"biblioteca_sons.json".equalsIgnoreCase(row.name)){CatalogCandidate c=new CatalogCandidate();c.uri=DocumentsContract.buildDocumentUriUsingTree(tree,row.id);c.path=path.length()==0?row.name:path+"/"+row.name;c.depth=depth;return c;}return null;}
    private CatalogCandidate overrideFromRows(Uri tree,List<DocRow> rows,String path,int depth){for(DocRow row:rows)if(!isDirectory(row.mime)&&"audio_overrides.json".equalsIgnoreCase(row.name)){CatalogCandidate c=new CatalogCandidate();c.uri=DocumentsContract.buildDocumentUriUsingTree(tree,row.id);c.path=path.length()==0?row.name:path+"/"+row.name;c.depth=depth;return c;}return null;}

    private void scanFolder(Uri tree,String parent,String path,JSONArray out,Set<String> seenDocuments,Set<String> seenFolders,int depth)throws Exception{
        if(depth>20||parent==null||parent.length()==0||!seenFolders.add(parent))return;scanRows(tree,queryChildrenStable(tree,parent),path,out,seenDocuments,seenFolders,depth);
    }

    private void scanRows(Uri tree,List<DocRow> rows,String path,JSONArray out,Set<String> seenDocuments,Set<String> seenFolders,int depth)throws Exception{/*BR_JSON_WRAP*/try{
        for(DocRow row:rows){
            if(row.id==null||row.id.length()==0)continue;String childPath=path.length()==0?row.name:path+"/"+row.name;
            if(isDirectory(row.mime)){scanFolder(tree,row.id,childPath,out,seenDocuments,seenFolders,depth+1);continue;}
            if(!seenDocuments.add(row.id)||!isAudio(row.name,row.mime))continue;
            JSONObject rule=catalogRuleFor(row.name,childPath);String keywords=rule==null?keywordsFor(childPath):mergeKeywords(rule.optString("keywords"),keywordsFor(childPath));String type=rule==null?guessType(childPath,keywords):normalizeType(rule.optString("type",guessType(childPath,keywords)));if(type.length()==0)type=guessType(childPath,keywords);
            JSONObject o=new JSONObject();o.put("id",row.id);o.put("uri",DocumentsContract.buildDocumentUriUsingTree(tree,row.id).toString());o.put("name",row.name);o.put("path",childPath);o.put("mime",row.mime);o.put("size",row.size);o.put("modified",row.modified);o.put("keywords",keywords);o.put("type",type);if(rule!=null)o.put("catalog",true);out.put(o);
        }
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private CatalogCandidate findCatalog(Uri tree,String parent,String path,Set<String> visited,int depth)throws Exception{
        if(depth>20||parent==null||!visited.add(parent))return null;CatalogCandidate best=null;for(DocRow row:queryChildrenStable(tree,parent)){String child=path.length()==0?row.name:path+"/"+row.name;if(isDirectory(row.mime)){CatalogCandidate nested=findCatalog(tree,row.id,child,visited,depth+1);if(nested!=null&&(best==null||nested.depth<best.depth))best=nested;continue;}if("biblioteca_sons.json".equalsIgnoreCase(row.name)){CatalogCandidate c=new CatalogCandidate();c.uri=DocumentsContract.buildDocumentUriUsingTree(tree,row.id);c.path=child;c.depth=depth;if(best==null||c.depth<best.depth)best=c;}}return best;
    }

    private CatalogCandidate findOverrides(Uri tree,String parent,String path,Set<String> visited,int depth)throws Exception{
        if(depth>20||parent==null||!visited.add(parent))return null;CatalogCandidate best=null;for(DocRow row:queryChildrenStable(tree,parent)){String child=path.length()==0?row.name:path+"/"+row.name;if(isDirectory(row.mime)){CatalogCandidate nested=findOverrides(tree,row.id,child,visited,depth+1);if(nested!=null&&(best==null||nested.depth<best.depth))best=nested;continue;}if("audio_overrides.json".equalsIgnoreCase(row.name)){CatalogCandidate c=new CatalogCandidate();c.uri=DocumentsContract.buildDocumentUriUsingTree(tree,row.id);c.path=child;c.depth=depth;if(best==null||c.depth<best.depth)best=c;}}return best;
    }

    private JSONObject catalogRuleFor(String name,String path){JSONObject r=keywordOverrides.optJSONObject("__filename:"+normalizedFilename(name));if(r==null)r=keywordOverrides.optJSONObject(fileKey(name));if(r==null)r=keywordOverrides.optJSONObject(fileKey(path));return r;}
    private static String normalizedFilename(String name){String n=name==null?"":name.replace('\\','/');int slash=n.lastIndexOf('/');if(slash>=0)n=n.substring(slash+1);return normalize(n);}
    private static String cleanKeywordArray(JSONArray words){/*BR_JSON_WRAP*/try{if(words==null)return "";LinkedHashMap<String,Boolean> unique=new LinkedHashMap<String,Boolean>();for(int i=0;i<words.length();i++){String w=normalize(words.optString(i));if(w.length()<2||isStopWord(w))continue;unique.put(w,true);}return join(new ArrayList<String>(unique.keySet()),",");}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean isStopWord(String w){return w.equals("the")||w.equals("of")||w.equals("a")||w.equals("an")||w.equals("at")||w.equals("on")||w.equals("in")||w.equals("to")||w.equals("from")||w.equals("with")||w.equals("no")||w.equals("gj")||w.equals("rpg")||w.equals("and")||w.equals("by")||w.equals("mais")||w.equals("som")||w.equals("sons");}
    private static String mergeKeywords(String a,String b){/*BR_JSON_WRAP*/try{LinkedHashMap<String,Boolean> set=new LinkedHashMap<String,Boolean>();for(String source:new String[]{a,b})for(String raw:(source==null?"":source).split("[,;|]")){String w=normalize(raw);if(w.length()>=2&&!isStopWord(w))set.put(w,true);}return join(new ArrayList<String>(set.keySet()),",");}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String readStream(InputStream in)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line).append('\n');return b.toString();}

    /** Lê a pasta sem depender de OFFSET, que é ignorado por alguns provedores do Drive. */
    private List<DocRow> queryChildrenStable(Uri tree,String parent)throws Exception{/*BR_JSON_WRAP*/try{
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent);
        String[] pr={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED};
        LinkedHashMap<String,DocRow> all=new LinkedHashMap<String,DocRow>();String lastSignature="";int stable=0;
        for(int attempt=0;attempt<12&&stable<2;attempt++){
            Cursor c=null;boolean loading=false;try{
                c=resolver.query(children,pr,null,null,null);if(c==null)throw new Exception("O provedor não permitiu ler a pasta de áudio.");
                int ic=c.getColumnIndex(pr[0]),nc=c.getColumnIndex(pr[1]),mc=c.getColumnIndex(pr[2]),sc=c.getColumnIndex(pr[3]),tc=c.getColumnIndex(pr[4]);
                while(c.moveToNext()){
                    String id=ic>=0?c.getString(ic):"";if(id==null||id.length()==0)continue;
                    DocRow r=new DocRow();r.id=id;r.name=nc>=0&&!c.isNull(nc)?c.getString(nc):"";r.mime=mc>=0&&!c.isNull(mc)?c.getString(mc):"";r.size=sc>=0&&!c.isNull(sc)?c.getLong(sc):0;r.modified=tc>=0&&!c.isNull(tc)?c.getLong(tc):0;all.put(id,r);
                }
                Bundle extras=c.getExtras();loading=extras!=null&&extras.getBoolean(DocumentsContract.EXTRA_LOADING,false);
            }finally{if(c!=null)c.close();}
            String signature=all.size()+":"+(all.isEmpty()?"":all.keySet().iterator().next())+":"+(all.isEmpty()?"":new ArrayList<String>(all.keySet()).get(all.size()-1));
            if(signature.equals(lastSignature)&&!loading)stable++;else stable=0;lastSignature=signature;
            if(loading||stable<2)try{Thread.sleep(250L);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
        }
        // Alguns provedores só expõem lotes quando recebem LIMIT/OFFSET. Acrescenta páginas sem confiar nelas.
        if(android.os.Build.VERSION.SDK_INT>=26){
            Set<String> pageSignatures=new HashSet<String>();
            for(int offset=0;offset<50000;offset+=100){
                Cursor c=null;ArrayList<String> pageIds=new ArrayList<String>();try{
                    Bundle args=new Bundle();args.putInt(ContentResolver.QUERY_ARG_LIMIT,100);args.putInt(ContentResolver.QUERY_ARG_OFFSET,offset);
                    c=resolver.query(children,pr,args,new CancellationSignal());if(c==null)break;
                    int ic=c.getColumnIndex(pr[0]),nc=c.getColumnIndex(pr[1]),mc=c.getColumnIndex(pr[2]),sc=c.getColumnIndex(pr[3]),tc=c.getColumnIndex(pr[4]);
                    while(c.moveToNext()){
                        String id=ic>=0?c.getString(ic):"";if(id==null||id.length()==0)continue;pageIds.add(id);
                        DocRow r=new DocRow();r.id=id;r.name=nc>=0&&!c.isNull(nc)?c.getString(nc):"";r.mime=mc>=0&&!c.isNull(mc)?c.getString(mc):"";r.size=sc>=0&&!c.isNull(sc)?c.getLong(sc):0;r.modified=tc>=0&&!c.isNull(tc)?c.getLong(tc):0;all.put(id,r);
                    }
                }finally{if(c!=null)c.close();}
                if(pageIds.isEmpty())break;String sig=pageIds.size()+":"+pageIds.get(0)+":"+pageIds.get(pageIds.size()-1);if(!pageSignatures.add(sig))break;if(pageIds.size()<100)break;
            }
        }
        return new ArrayList<DocRow>(all.values());
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private int countCatalogMatches(){int count=0;for(int i=0;i<sounds.length();i++)if(ruleFor(sounds.optJSONObject(i))!=null)count++;return count;}

    public int importKeywordIntent(Intent data)throws Exception{
        ArrayList<Uri> uris=new ArrayList<Uri>();
        if(data!=null&&data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)uris.add(data.getClipData().getItemAt(i).getUri());}
        else if(data!=null&&data.getData()!=null)uris.add(data.getData());
        if(uris.isEmpty())throw new Exception("Nenhum biblioteca_sons.json foi selecionado.");
        int total=0;for(Uri u:uris){InputStream in=resolver.openInputStream(u);if(in==null)continue;try{String name=displayName(u);if(name.toLowerCase(Locale.US).contains("override")&&name.toLowerCase(Locale.US).endsWith(".json"))total+=importJsonOverrides(in,name);else if(name.toLowerCase(Locale.US).endsWith(".json"))total+=importJsonCatalog(in,name);else total+=importLegacyTxt(in,name);}finally{in.close();}}
        save();rebuildCatalogIndex();return total;
    }

    private int importJsonCatalog(InputStream in,String source)throws Exception{/*BR_JSON_WRAP*/try{
        String raw=readStream(in);JSONObject parsed=new JSONObject(raw);JSONObject fresh=new JSONObject();int count=0;java.util.Iterator<String> keys=parsed.keys();
        while(keys.hasNext()){String filename=keys.next();if(!isAudio(filename,""))continue;Object value=parsed.opt(filename);JSONObject rule=new JSONObject();JSONArray words=null;
            if(value instanceof JSONArray)words=(JSONArray)value;else if(value instanceof JSONObject){JSONObject o=(JSONObject)value;words=o.optJSONArray("keywords");String type=normalizeType(o.optString("type",o.optString("channel")));if(type.length()>0)rule.put("type",type);if(o.has("automatic"))rule.put("automatic",o.optBoolean("automatic",true));if(o.has("weight"))rule.put("weight",o.optDouble("weight",1));for(String field:new String[]{"role","group","environment","weather","intensity","daypart"})if(o.has(field))rule.put(field,o.optString(field));if(o.has("loop"))rule.put("loop",o.optBoolean("loop",true));}
            String clean=cleanKeywordArray(words);if(clean.length()==0)clean=keywordsFor(filename);rule.put("keywords",clean);String inferred=normalizeType(rule.optString("type",guessType(filename,clean)));if(inferred.length()>0)rule.put("type",inferred);rule.put("filename",filename);
            fresh.put(fileKey(filename),rule);fresh.put("__filename:"+normalizedFilename(filename),rule);count++;
        }
        fresh.put("__catalog_source",source==null?"biblioteca_sons.json":source);fresh.put("__catalog_count",count);fresh.put("__catalog_matched",0);keywordOverrides=fresh;catalogSource=fresh.optString("__catalog_source");catalogEntryCount=count;catalogMatchedCount=0;return count;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private int importJsonOverrides(InputStream in,String source)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject parsed=new JSONObject(readStream(in));int count=0;java.util.Iterator<String> keys=parsed.keys();
        while(keys.hasNext()){
            String filename=keys.next();JSONObject supplied=parsed.optJSONObject(filename);if(supplied==null||!isAudio(filename,""))continue;
            String normalizedKey="__filename:"+normalizedFilename(filename);JSONObject rule=keywordOverrides.optJSONObject(normalizedKey);
            if(rule==null)rule=keywordOverrides.optJSONObject(fileKey(filename));
            if(rule==null)rule=new JSONObject().put("filename",filename).put("keywords",keywordsFor(filename));
            JSONArray words=supplied.optJSONArray("keywords");if(words!=null)rule.put("keywords",mergeKeywords(cleanKeywordArray(words),rule.optString("keywords")));
            String type=normalizeType(supplied.optString("type",supplied.optString("channel")));if(type.length()>0)rule.put("type",type);
            if(supplied.has("automatic"))rule.put("automatic",supplied.optBoolean("automatic",true));
            if(supplied.has("weight"))rule.put("weight",supplied.optDouble("weight",1.0));
            for(String field:new String[]{"role","group","environment","weather","intensity","daypart"})if(supplied.has(field))rule.put(field,supplied.optString(field));
            if(supplied.has("loop"))rule.put("loop",supplied.optBoolean("loop",true));
            keywordOverrides.put(fileKey(filename),rule);keywordOverrides.put(normalizedKey,rule);count++;
        }
        keywordOverrides.put("__overrides_source",source==null?"audio_overrides.json":source);
        keywordOverrides.put("__overrides_count",count);
        return count;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private int importLegacyTxt(InputStream in,String source)throws Exception{/*BR_JSON_WRAP*/try{
        BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));int count=0;String sourceType=typeHintFromFilename(source);JSONArray aliases=keywordOverrides.optJSONArray("__aliases");if(aliases==null)aliases=new JSONArray();String line;
        while((line=br.readLine())!=null){line=line.replace("\uFEFF","").trim();if(line.length()==0||line.startsWith("#")||line.startsWith("//"))continue;ParsedRule parsed=parseRule(line,sourceType);if(parsed==null)continue;if(parsed.fileKey.length()>0){JSONObject rule=new JSONObject().put("keywords",parsed.keywords);if(parsed.type.length()>0)rule.put("type",parsed.type);keywordOverrides.put(parsed.fileKey,rule);count++;}else if(parsed.trigger.length()>0){aliases.put(new JSONObject().put("trigger",parsed.trigger).put("target",parsed.keywords).put("type",parsed.type));count++;}}keywordOverrides.put("__aliases",aliases);return count;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /**
     * Aceita tanto regras estruturadas quanto os catálogos reais usados pelo usuário,
     * que possuem somente um nome de áudio por linha (sem |, =, : ou ;).
     */
    private ParsedRule parseRule(String line,String sourceType){
        String cleaned=line.replace('\t','|').trim();String[] p=cleaned.split("\\s*\\|\\s*");
        if(p.length<2&&cleaned.contains("="))p=cleaned.split("\\s*=\\s*",2);
        if(p.length<2&&cleaned.contains(":"))p=cleaned.split("\\s*:\\s*",2);
        if(p.length<2&&cleaned.contains(";"))p=cleaned.split("\\s*;\\s*");

        // Formato simples: uma entrada por linha. O texto normalmente é o próprio
        // nome do arquivo sem extensão. Ele passa a ser uma regra válida e contável.
        if(p.length<2){
            ParsedRule r=new ParsedRule();r.fileKey=fileKey(cleaned);if(r.fileKey.length()==0)return null;
            JSONObject matched=findSoundByKey(r.fileKey);
            String baseKeywords=keywordsFor(cleaned);
            if(matched!=null){
                String indexed=matched.optString("keywords","");
                r.keywords=indexed.length()>0?baseKeywords+","+indexed:baseKeywords;
                r.type=sourceType.length()>0?sourceType:normalizeType(matched.optString("type",guessType(matched.optString("path",cleaned))));
            }else{
                r.keywords=baseKeywords;
                r.type=sourceType.length()>0?sourceType:explicitTypeFromText(cleaned);
            }
            return r;
        }

        ParsedRule r=new ParsedRule();String first=normalize(p[0]),second=p[1].trim(),third=p.length>2?normalizeType(p[2]):sourceType;
        if(isType(first)&&p.length>=3){r.type=normalizeType(first);r.fileKey=fileKey(p[1]);r.keywords=p[2].trim();return r;}
        boolean looksFile=looksLikeFilename(p[0])||findSoundByKey(fileKey(p[0]))!=null;
        if(looksFile){r.fileKey=fileKey(p[0]);r.keywords=second.length()>0?second:keywordsFor(p[0]);r.type=third;return r;}
        r.trigger=first;r.keywords=second;r.type=third;return r;
    }

    private String displayName(Uri uri){
        Cursor c=null;try{c=resolver.query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null);if(c!=null&&c.moveToFirst())return c.getString(0);}catch(Exception ignored){AppLog.ignored("SmartAudioLibrary","Falha tolerada; a execução continuou.",ignored);}finally{if(c!=null)c.close();}
        String last=uri==null?"":uri.getLastPathSegment();return last==null?"":last;
    }

    private String typeHintFromFilename(String filename){
        String n=normalize(filename);
        if(n.contains("ambiencia")||n.contains("ambience")||n.contains("ambient"))return "ambience";
        if(n.contains("musica")||n.contains("music")||n.contains("trilha")||n.contains("bgm")||n.contains("score"))return "music";
        if(n.contains("sfx")||n.contains("efeito")||n.contains("effect")||n.contains("foley"))return "sfx";
        return "";
    }

    private String explicitTypeFromText(String text){
        String n=normalize(text);
        if(n.contains("ambience")||n.contains("ambiencia")||n.contains("ambient")||n.contains("atmosphere")||n.contains("soundscape"))return "ambience";
        if(n.contains("music")||n.contains("musica")||n.contains("theme")||n.contains("trilha")||n.contains("bgm")||n.contains("score"))return "music";
        if(n.contains("sfx")||n.contains("effect")||n.contains("efeito")||n.contains("foley"))return "sfx";
        return "";
    }

    /** Executa apenas faixas contínuas enquanto a primeira cena é composta. */
    public void playOpeningPrelude(String campaignContext){String text=(campaignContext==null?"":campaignContext)+" aventura fantasia estrada cidade floresta calma misterio";playSceneBedForNarration(text);}

    public void playSceneBedForNarration(final String text){
        final long generation=++continuousPlanGeneration;
        audioPlanner.execute(new Runnable(){public void run(){if(generation!=continuousPlanGeneration)return;playPlan(text,false,true);}});
    }
    public void playForNarration(final String text){
        final long generation=++continuousPlanGeneration;
        audioPlanner.execute(new Runnable(){public void run(){if(generation!=continuousPlanGeneration)return;playPlan(text,voiceNarrationActive,true);if(!voiceNarrationActive)lastPlan+=" • SFX pontual bloqueado: não há voz narrando";}});
    }
    public boolean hasSfxCue(String text){
        if(text==null||text.trim().length()==0||!automaticSfxEnabled||sounds.length()==0)return false;
        try{return containsConcreteSfxTrigger(normalize(expandAliases(text)));}catch(Exception ignored){return false;}
    }

    public void prepareVoiceSfx(final List<String> chunks){/*BR_JSON_WRAP*/try{
        if(chunks==null||chunks.isEmpty()||!automaticSfxEnabled||!voiceNarrationActive||sounds.length()==0)return;
        final long generation=voiceCueGeneration;final ArrayList<String> copy=new ArrayList<String>(chunks);
        sfxPlanner.execute(new Runnable(){public void run(){/*BR_JSON_WRAP*/try{for(String sentence:copy){if(generation!=voiceCueGeneration||!voiceNarrationActive)return;try{String normalized=normalize(expandAliases(sentence));if(normalized.length()<3||!containsConcreteSfxTrigger(normalized))continue;List<Scored> effects=selectSfx(normalized,1);if(!effects.isEmpty())preparedVoiceSfx.put(cueKey(sentence),effects.get(0).sound);}catch(Exception ignored){AppLog.ignored("SmartAudioLibrary","Falha tolerada; a execução continuou.",ignored);}}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}});
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public void playSfxForSentence(final String text){
        if(text==null||text.trim().length()==0||!automaticSfxEnabled||!voiceNarrationActive||sounds.length()==0)return;
        final long generation=voiceCueGeneration;final String key=cueKey(text);final JSONObject prepared=preparedVoiceSfx.remove(key);
        if(prepared!=null){handler.post(new Runnable(){public void run(){if(generation==voiceCueGeneration&&voiceNarrationActive)playSfxIfAllowed(prepared);}});return;}
        sfxPlanner.execute(new Runnable(){public void run(){if(generation!=voiceCueGeneration||!voiceNarrationActive)return;try{String normalized=normalize(expandAliases(text));if(normalized.length()<3||!containsConcreteSfxTrigger(normalized))return;List<Scored> effects=selectSfx(normalized,1);if(effects.isEmpty())return;final JSONObject sound=effects.get(0).sound;handler.post(new Runnable(){public void run(){if(generation==voiceCueGeneration&&voiceNarrationActive)playSfxIfAllowed(sound);}});}catch(Exception ignored){AppLog.ignored("SmartAudioLibrary","Falha tolerada; a execução continuou.",ignored);}}});
    }

    private static String cueKey(String text){return normalize(text==null?"":text);}
    private static boolean containsConcreteSfxTrigger(String normalizedText){if(normalizedText==null||normalizedText.length()<3)return false;for(String[] family:SFX_FAMILIES)if(containsAny(normalizedText,family[1]))return true;return false;}

    /**
     * API de catálogo para o motor narrativo novo. Estes métodos apenas
     * resolvem arquivos; não agendam, não aplicam cooldown e não interrompem
     * nenhum canal. Toda decisão temporal fica em NarrativeAudioEngine.
     */
    public synchronized JSONObject resolveAmbience(String query){
        try{
            String normalized=normalize(expandAliases(query==null?"":query));
            Scored best=selectSceneBedRole("ambience",normalized,"","base");
            return best==null?null:decorateMatch(best,"ambience","base",0);
        }catch(Exception ignored){return null;}
    }

    /** Camada climática independente: chuva, vento, tempestade ou neve. */
    public synchronized JSONObject resolveClimate(String query){
        try{
            String normalized=normalize(expandAliases(query==null?"":query));
            if(!containsAny(normalized,"rain,chuva,drizzle,garoa,downpour,tempestade,storm,thunder,trovao,trovão,wind,vento,gust,vendaval,snow,neve"))return null;
            Scored best=selectSceneBedRole("ambience",normalized,"","climate");
            return best==null?null:decorateMatch(best,"ambience","climate",0);
        }catch(Exception ignored){return null;}
    }

    /**
     * Diagnóstico totalmente isolado: não toca arquivos, não abre metadados
     * nativos e não grava o histórico SQLite. A rotação é simulada somente em
     * memória para que um teste jamais altere a campanha ou impeça a próxima
     * inicialização.
     */
    public synchronized JSONArray runClimateRotationDiagnostic(String[] scenes){/*BR_JSON_WRAP*/try{
        JSONArray out=new JSONArray();
        HashSet<String> recent=new HashSet<String>();
        if(scenes==null)return out;
        int maximum=Math.min(12,scenes.length);
        for(int i=0;i<maximum;i++){
            String scene=scenes[i]==null?"":scenes[i];
            try{
                String normalized=normalize(expandAliases(scene));
                Scored best=selectSceneBedRole("ambience",normalized,"","climate",recent,false);
                JSONObject row=new JSONObject();
                row.put("scene",scene);
                if(best==null){row.put("found",false);row.put("name","nenhuma camada compatível");}
                else{String id=AudioCatalogDb.soundId(best.sound);row.put("found",true);row.put("name",best.sound.optString("name","arquivo sem nome"));row.put("sound_id",id);row.put("score",best.score);if(id.length()>0)recent.add(id);}
                out.put(row);
            }catch(Throwable error){
                try{out.put(new JSONObject().put("scene",scene).put("found",false).put("name","falha isolada: "+String.valueOf(error.getMessage())));}catch(Exception ignored){AppLog.ignored("SmartAudioLibrary","Falha tolerada; a execução continuou.",ignored);}
            }
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized JSONObject resolveMusic(String query,String mood){
        try{
            String normalized=normalize(expandAliases((query==null?"":query)+" "+(mood==null?"":mood)));
            Scored best=selectSceneBedRole("music",normalized,normalize(mood),"music");
            return best==null?null:decorateMatch(best,"music",mood,0);
        }catch(Exception ignored){return null;}
    }

    public synchronized JSONObject resolveSfx(String concretePhrase){
        return resolveSfx(inferEventFromPhrase(concretePhrase),concretePhrase);
    }

    /**
     * Resolve SFX pelo evento semântico e pela frase. O nome pode estar em
     * português ou inglês; a seleção usa o catálogo JSON e o nome real.
     */
    public synchronized JSONObject resolveSfx(String event,String concretePhrase){
        try{
            String normalized=normalize(expandAliases(concretePhrase==null?"":concretePhrase));
            String useEvent=normalize(event);
            Scored best=selectEventSfx(useEvent,normalized);
            if(best==null)return null;
            int maxDuration=isLongSfxFallback(best.sound,useEvent)?5000:0;
            return decorateMatch(best,"sfx",useEvent,maxDuration);
        }catch(Exception ignored){return null;}
    }


    /** Seleciona uma cama sonora sem misturar efeitos pontuais. */
    private Scored selectSceneBed(String requestedType,String normalizedQuery,String mood){
        String role="music".equals(requestedType)?"music":"base";
        return selectSceneBedRole(requestedType,normalizedQuery,mood,role);
    }

    private Scored selectSceneBedRole(String requestedType,String normalizedQuery,String mood,String requestedRole){
        return selectSceneBedRole(requestedType,normalizedQuery,mood,requestedRole,null,true);
    }

    private Scored selectSceneBedRole(String requestedType,String normalizedQuery,String mood,String requestedRole,Set<String> extraBlocked,boolean usePersistentHistory){
        ArrayList<Scored> candidates=new ArrayList<Scored>();
        for(int i=0;i<sounds.length();i++){
            JSONObject sound=sounds.optJSONObject(i);
            if(sound==null||isAutoBlockedSound(sound)||!isPlayableAutomaticFormat(sound))continue;
            String actual=typeOf(sound);
            String hard=hardType(sound.optString("path",sound.optString("name")),sound.optString("keywords",""));
            if(hard.length()>0)actual=hard;
            if(!requestedType.equals(actual)||isContinuousCandidateBlocked(sound,requestedType))continue;
            String role=sound.optString("catalog_role",actual.equals("music")?"music":"base");
            if(!requestedRole.equals(role))continue;
            int score="ambience".equals(requestedType)?scoreAmbienceScene(sound,normalizedQuery):scoreMusicScene(sound,normalizedQuery,mood);
            candidates.add(new Scored(sound,score));
        }
        Scored rotated=chooseRotated(requestedType,requestedRole,normalizedQuery,candidates,extraBlocked,usePersistentHistory);
        if(rotated!=null&&rotated.score>=2)return rotated;

        // Fallback seguro dentro do mesmo papel. Clima nunca substitui o cenário-base.
        ArrayList<Scored> fallback=new ArrayList<Scored>();
        for(int i=0;i<sounds.length();i++){
            JSONObject sound=sounds.optJSONObject(i);
            if(sound==null||isAutoBlockedSound(sound)||!isPlayableAutomaticFormat(sound))continue;
            String actual=typeOf(sound);
            String hard=hardType(sound.optString("path",sound.optString("name")),sound.optString("keywords",""));
            if(hard.length()>0)actual=hard;
            if(!requestedType.equals(actual)||isContinuousCandidateBlocked(sound,requestedType))continue;
            String role=sound.optString("catalog_role",actual.equals("music")?"music":"base");
            if(!requestedRole.equals(role))continue;
            String corpus=soundCorpus(sound);int score=0;
            if("ambience".equals(requestedType)){
                if("climate".equals(requestedRole)){
                    if(hasWeatherMarker(corpus))score+=6;
                }else{
                    if(containsAny(corpus,"nature,natureza,forest,floresta,wind,vento,room,interior,ambience,ambiencia,tavern,taverna,city,town,village,cave"))score+=5;
                    if(hasWeatherMarker(corpus))score-=20;
                    if(containsAny(corpus,"combat,battle,hell,evil"))score-=3;
                }
            }else{
                if(containsAny(corpus,"calm,chill,adventure,aventura,rpg,theme,track,melody"))score+=5;
                score+=musicMoodScore(sound,mood==null||mood.length()==0?"aventura":mood);
            }
            fallback.add(new Scored(sound,score));
        }
        return chooseRotated(requestedType,requestedRole,normalizedQuery,fallback,extraBlocked,usePersistentHistory);
    }

    /**
     * Rotação ponderada persistente. O arquivo mais curto ou o primeiro da lista
     * não ganha automaticamente. As variantes melhores são colocadas numa sacola;
     * as usadas recentemente ficam fora até as demais terem oportunidade.
     */
    private Scored chooseRotated(String channel,String role,String query,List<Scored> all){
        return chooseRotated(channel,role,query,all,null,true);
    }

    private Scored chooseRotated(String channel,String role,String query,List<Scored> all,Set<String> extraBlocked,boolean usePersistentHistory){/*BR_JSON_WRAP*/try{
        if(all==null||all.isEmpty())return null;
        int best=Integer.MIN_VALUE;
        for(Scored item:all)if(item.score>best)best=item.score;
        ArrayList<Choice> shortlist=new ArrayList<Choice>();
        for(Scored item:all){
            if(item.score<best-14)continue;
            JSONObject sound=item.sound;
            String id=AudioCatalogDb.soundId(sound);
            AudioCatalogDb.Stats stats=usePersistentHistory?catalogDb.stats(id):new AudioCatalogDb.Stats(0L,0,0,sound.optLong("duration_ms",0L));
            long duration=stats.durationMs;
            long size=sound.optLong("size",0L);
            int adjusted=item.score;
            if(!"sfx".equals(channel)){
                if(duration>=180000L)adjusted+=10;
                else if(duration>=60000L)adjusted+=7;
                else if(duration>0L&&duration<18000L)adjusted-=22;
                if(size>=12L*1024L*1024L)adjusted+=4;
                else if(size>0L&&size<350L*1024L)adjusted-=10;
            }
            adjusted-=Math.min(45,stats.failureCount*15);
            shortlist.add(new Choice(item,adjusted,stats,duration));
        }
        if(shortlist.isEmpty())return null;
        Collections.sort(shortlist,new Comparator<Choice>(){public int compare(Choice a,Choice b){return b.adjusted-a.adjusted;}});
        LinkedHashMap<String,Choice> uniqueFamilies=new LinkedHashMap<String,Choice>();
        for(Choice choice:shortlist){
            String family=choice.scored.sound.optString("duplicate_family",duplicateFamily(choice.scored.sound.optString("name",choice.scored.sound.optString("path",""))));
            if(!uniqueFamilies.containsKey(family))uniqueFamilies.put(family,choice);
        }
        shortlist=new ArrayList<Choice>(uniqueFamilies.values());

        String dominantGroup=dominantGroup(shortlist,query,role);
        List<String> recent=usePersistentHistory?catalogDb.recentIds(channel,role,dominantGroup,6):new ArrayList<String>();
        if(usePersistentHistory&&recent.isEmpty())recent=catalogDb.recentIds(channel,role,"",6);
        long now=System.currentTimeMillis();
        long minInterval="music".equals(channel)?20L*60L*1000L:("sfx".equals(channel)?45L*1000L:15L*60L*1000L);
        boolean alternatives=shortlist.size()>1;
        for(Choice choice:shortlist){
            String id=AudioCatalogDb.soundId(choice.scored.sound);
            if(extraBlocked!=null&&extraBlocked.contains(id))choice.adjusted-=1200;
            int pos=recent.indexOf(id);
            if(alternatives&&pos==0)choice.adjusted-=1000;
            else if(pos==1)choice.adjusted-=90;
            else if(pos==2)choice.adjusted-=55;
            else if(pos==3)choice.adjusted-=30;
            else if(pos>=4)choice.adjusted-=12;
            if(alternatives&&choice.stats.lastPlayedAt>0L&&now-choice.stats.lastPlayedAt<minInterval)choice.adjusted-=500;
        }
        Collections.sort(shortlist,new Comparator<Choice>(){public int compare(Choice a,Choice b){return b.adjusted-a.adjusted;}});
        int top=shortlist.get(0).adjusted;
        ArrayList<Choice> bag=new ArrayList<Choice>();
        for(Choice choice:shortlist){if(choice.adjusted>=top-10&&bag.size()<8)bag.add(choice);}
        if(bag.isEmpty())bag.add(shortlist.get(0));

        int total=0;int[] weights=new int[bag.size()];
        for(int i=0;i<bag.size();i++){
            Choice choice=bag.get(i);double catalogWeight=choice.scored.sound.optDouble("catalog_weight",1.0);
            int relative=Math.max(1,12-(top-choice.adjusted));
            int weight=Math.max(1,(int)Math.round(relative*Math.max(.2,Math.min(4.0,catalogWeight))));
            weights[i]=weight;total+=weight;
        }
        int pick=total<=1?0:rotationRandom.nextInt(total);Choice chosen=bag.get(0);
        for(int i=0;i<bag.size();i++){pick-=weights[i];if(pick<0){chosen=bag.get(i);break;}}
        StringBuilder debug=new StringBuilder();
        debug.append(channel).append('/').append(role).append(" • grupo ").append(dominantGroup)
                .append(" • escolhida: ").append(chosen.scored.sound.optString("name"))
                .append(" • nota ").append(chosen.adjusted)
                .append(" • sacola ").append(bag.size()).append(" de ").append(shortlist.size())
                .append(" • recentes bloqueadas: ").append(recent.size());
        long selectedDuration=chosen.duration;
        if(selectedDuration>0L)debug.append(" • duração conhecida ").append(selectedDuration/1000L).append('s');
        else debug.append(" • duração não consultada durante a seleção segura");
        lastRotationDebug=debug.toString();
        return new Scored(chosen.scored.sound,chosen.adjusted);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private String dominantGroup(List<Choice> choices,String query,String role){/*BR_JSON_WRAP*/try{
        String n=normalize(query);
        if("climate".equals(role)){
            boolean rain=containsAny(n,"rain,chuva,garoa,drizzle,downpour");
            if(containsAny(n,"thunder,trovao,trovão,rumbling")&&!rain)return "weather_thunder";
            if(containsAny(n,"telhado,roof,interior,taverna,estalagem,house,casa"))return "rain_roof";
            if(containsAny(n,"floresta,forest,mata,bosque,vegetacao,vegetação,folhas,leaves"))return "rain_forest";
            if(containsAny(n,"tempestade,storm,downpour,forte,heavy,trovao,trovão"))return "rain_heavy";
            if(containsAny(n,"garoa,drizzle,chuva leve,light rain"))return "rain_light";
            if(containsAny(n,"vento,wind,vendaval,gust"))return "weather_wind";
        }
        HashMap<String,Integer> counts=new HashMap<String,Integer>();
        for(Choice choice:choices){String group=choice.scored.sound.optString("variant_group","generic");Integer c=counts.get(group);counts.put(group,c==null?1:c+1);}
        String best="generic";int bestCount=-1;
        for(Map.Entry<String,Integer> e:counts.entrySet())if(e.getValue()>bestCount){best=e.getKey();bestCount=e.getValue();}
        return best;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private int scoreAmbienceScene(JSONObject sound,String query){
        String corpus=soundCorpus(sound);int score=scoreContinuous(sound,query);
        String[][] environments={
            {"taverna,estalagem,hospedaria,inn,pub","tavern,taverna,estalagem,hospedaria,inn,crowd,walla"},
            {"floresta,mata,bosque,forest,woods","forest,floresta,nature,natureza,birds,vegetation,leaves"},
            {"caverna,gruta,cave,cavern","cave,cavern,caverna,drips,goteiras,damp"},
            {"masmorra,calabouco,dungeon,cripta,ruina,ruína","dungeon,cave,cavern,subterraneo,mine,corridor,dark"},
            {"cidade,vila,aldeia,mercado,city,town,village","city,town,village,crowd,walla,medieval,people"},
            {"porto,navio,mar,oceano,port,ship,sea,ocean","port,ship,shore,waves,beach,onboard"},
            {"rio,lago,river,lake","river,lake,shore,water,waves"},
            {"pantano,pântano,brejo,swamp","swamp,swamps,pantano"},
            {"deserto,desert","desert,wind"},
            {"montanha,desfiladeiro,mountain,canyon","mountain,canyon,wind,cliff"},
            {"fazenda,celeiro,campo,farm,barn,countryside","farm,barn,countryside,nature,animals"},
            {"biblioteca,library","library,corridor,quiet"},
            {"forja,ferreiro,blacksmith,forge","blacksmith,armory,forge"}
        };
        for(String[] group:environments){boolean scene=containsAny(query,group[0]);boolean file=containsAny(corpus,group[1]);if(scene&&file)score+=35;else if(scene&&containsAny(corpus,"combat,battle,hell,evil"))score-=12;}
        if(containsAny(query,"chuva,rain,garoa,drizzle,tempestade,storm")){
            if(containsAny(corpus,"rain,chuva,drizzle,storm,downpour"))score+=30;else score-=5;
            boolean indoorRain=containsAny(query,"taverna,estalagem,hospedaria,interior,dentro,casa,house,indoors");
            boolean forestRain=containsAny(query,"floresta,mata,bosque,forest,woods,vegetacao,vegetação,folhas,leaves");
            if(indoorRain){if(containsAny(corpus,"roof,telhado,gutter,calha"))score+=28;else if(containsAny(corpus,"forest,vegetation,leaves,tree"))score-=14;}
            if(forestRain){if(containsAny(corpus,"forest,floresta,vegetation,leaves,tree"))score+=28;else if(containsAny(corpus,"roof,telhado,gutter"))score-=14;}
            if(containsAny(query,"garoa,drizzle,chuva leve,light rain")){if(containsAny(corpus,"light,drizzle,calm"))score+=12;else if(containsAny(corpus,"downpour,storm,heavy"))score-=12;}
            if(containsAny(query,"tempestade,storm,chuva forte,heavy rain")){if(containsAny(corpus,"downpour,storm,heavy,thunder"))score+=14;else if(containsAny(corpus,"light,drizzle"))score-=12;}
        }
        else if(containsAny(corpus,"rain,chuva,downpour,storm"))score-=10;
        if(containsAny(query,"noite,noturno,night,madrugada")){if(containsAny(corpus,"night,noite,crickets,grilos,owls"))score+=18;else if(containsAny(corpus,"day,dia,summer day"))score-=10;}
        if(containsAny(query,"dia,manha,manhã,tarde,day,morning")){if(containsAny(corpus,"day,dia,birds,nature"))score+=12;else if(containsAny(corpus,"night,noite,crickets"))score-=12;}
        if(containsAny(query,"interior,dentro,sala,quarto,indoor")){if(containsAny(corpus,"indoor,indoors,interior,room,tavern,library,cave"))score+=9;}
        if(containsAny(query,"exterior,fora,outdoor,estrada,road")){if(containsAny(corpus,"outdoor,outdoors,exterior,nature,wind,countryside"))score+=9;}
        return score;
    }

    private int scoreMusicScene(JSONObject sound,String query,String mood){
        String corpus=soundCorpus(sound);String useMood=mood==null||mood.length()==0?musicMood(query):mood;
        int score=scoreContinuous(sound,query)+musicMoodScore(sound,useMood);
        if(containsAny(corpus,"theme,track,song,melody,score,bgm,bardic,rpg"))score+=12;
        if(containsAny(query,"combate,batalha,perigo,tensao,tensão,combat,battle,danger")&&containsAny(corpus,"battle,combat,arena,dark,tense,cursed,madness"))score+=24;
        if(containsAny(query,"descanso,calma,seguro,paz,rest,calm,safe")&&containsAny(corpus,"calm,chill,happy,light,peace"))score+=18;
        if(containsAny(query,"misterio,mistério,segredo,ruina,ruína,mystery,secret")&&containsAny(corpus,"secret,mystery,dark,sorrow,dungeon"))score+=18;
        return score;
    }

    private Scored selectEventSfx(String event,String phrase){
        String useEvent=event==null?"":normalize(event);String[] rule=eventRule(useEvent);
        ArrayList<Scored> primary=new ArrayList<Scored>();ArrayList<Scored> fallback=new ArrayList<Scored>();
        for(int i=0;i<sounds.length();i++){
            JSONObject sound=sounds.optJSONObject(i);if(sound==null||isAutoBlockedSound(sound)||!isPlayableAutomaticFormat(sound))continue;
            JSONObject catalogRule=ruleFor(sound);if(catalogRule!=null&&!catalogRule.optBoolean("automatic",true))continue;
            String corpus=soundCorpus(sound);String actual=typeOf(sound);int score=0;
            if(rule!=null){
                if(containsAny(corpus,rule[1]))score+=45;else continue;
                if(containsAny(corpus,rule[2]))score+=18;
                if(containsAny(corpus,rule[3]))score-=35;
            }else score=scoreSfx(sound,phrase);
            score+=scoreSfx(sound,phrase);
            for(String word:phrase.split(" "))if(word.length()>=4&&containsPhrase(corpus,word))score+=2;
            if("sfx".equals(actual)&&score>=24)primary.add(new Scored(sound,score));
            else if(isLongSfxFallback(sound,useEvent)&&score-12>=20)fallback.add(new Scored(sound,score-12));
        }
        Scored rotated=chooseRotated("sfx","sfx",useEvent+" "+phrase,primary);
        if(rotated!=null)return rotated;
        return chooseRotated("sfx","sfx",useEvent+" "+phrase,fallback);
    }

    private String[] eventRule(String event){
        for(String[] rule:EVENT_FILE_RULES)if(normalize(rule[0]).equals(normalize(event)))return rule;
        return null;
    }

    private String inferEventFromPhrase(String phrase){
        String n=normalize(phrase);
        if(containsAny(n,"batida na madeira,bate na madeira,knock on wood,knocking on wood"))return "wood_knock";
        if(containsAny(n,"batida na porta,batidas na porta,bater na porta,knock,knocking"))return "knock";
        if(containsAny(n,"porta se abre,porta abre,abriu a porta,porta range,porta fecha,door opens,door closes,door creak"))return "door";
        if(containsAny(n,"bigorna,anvil,martelo na forja,hammer on anvil"))return "anvil";
        if(containsAny(n,"carroca,carroça,carruagem,stagecoach,carriage,wagon,cart"))return "cart";
        if(containsAny(n,"passos,footsteps,steps"))return "footsteps";
        if(containsAny(n,"espada,lamina,lâmina,sword,blade,rapier,dagger"))return "sword";
        if(containsAny(n,"armadura,metal,ferro,armor,chainmail,clang"))return "metal";
        if(containsAny(n,"corrente,correntes,chain,chains"))return "chains";
        if(containsAny(n,"rugido,rosnado,roar,growl"))return "roar";
        if(containsAny(n,"grito,grita,scream,shriek"))return "scream";
        if(containsAny(n,"cavalo,galope,cascos,horse,gallop,hooves"))return "horse";
        if(containsAny(n,"trovao,trovão,thunder"))return "thunder";
        if(containsAny(n,"fogo crepita,lareira,chamas crepitam,fire crackles"))return "fire_crackle";
        if(containsAny(n,"acende,incendeia,ignites,fire burst"))return "ignition";
        if(containsAny(n,"vidro,glass"))return "glass";
        if(containsAny(n,"sino,bell"))return "bell";
        if(containsAny(n,"flecha,arco,arrow,bow"))return "arrow";
        if(containsAny(n,"explosao,explosão,explode,explosion,blast"))return "explosion";
        if(containsAny(n,"magia,feitico,feitiço,spell,magic,arcane"))return "magic";
        if(containsAny(n,"escreve,escrita,lapis,lápis,pergaminho,write,writing,pencil"))return "writing";
        if(containsAny(n,"caneca,copo,bebe,cerveja,mug,glassware,drink"))return "drink";
        if(containsAny(n,"risada,risadas,ri,laugh,laughter"))return "laugh";
        if(containsAny(n,"pedra,pedras,rocha,rocks,stones"))return "rocks";
        if(containsAny(n,"cai,queda,despenca,fall,landing"))return "fall";
        if(containsAny(n,"agua,água,salpico,splash,water"))return "splash";
        return "impact";
    }

    private JSONObject decorateMatch(Scored best,String type,String semantic,int maxDuration)throws Exception{/*BR_JSON_WRAP*/try{
        JSONObject out=new JSONObject(best.sound.toString());out.put("match_type",type);out.put("match_semantic",semantic==null?"":semantic);out.put("match_score",best.score);out.put("rotation_debug",lastRotationDebug);if(maxDuration>0)out.put("max_duration_ms",maxDuration);return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private boolean isLongSfxFallback(JSONObject sound,String event){
        if(sound==null)return false;String actual=typeOf(sound);if("sfx".equals(actual))return false;
        String n=soundCorpus(sound);String e=normalize(event);
        return ("anvil".equals(e)&&containsAny(n,"blacksmith,armory,forge,bigorna,anvil"))||("cart".equals(e)&&containsAny(n,"stagecoach,carriage,cart,wagon,coach"));
    }

    private String hardType(String path,String keywords){
        String n=normalize(path+" "+keywords);
        for(String marker:MUSIC_FILE_MARKERS)if(containsPhrase(n,normalize(marker)))return "music";
        if(containsAny(n,"combat,hit,swing,draw,impact,footstep,footsteps,scream,growl,roar,death,knock,knocking,scrape,break,chains,neigh,gallop,punch,dagger,sword,bow,crossbow,staff,slash,strike,door,glass,ui ping,ui open,ui close,jump,landing,cuspe,spit,pergaminho,prego,weapon,armor,blade,fire activate,grunt,ouch,pain,dragon,vocalization,risada,enchendo,bell,rock,stones,magic sting,buff,pickup,rage,anvil,hammer,stagecoach"))return "sfx";
        for(String marker:AMBIENCE_FILE_MARKERS)if(containsPhrase(n,normalize(marker)))return "ambience";
        return "";
    }

    private String soundCorpus(JSONObject sound){
        JSONObject rule=ruleFor(sound);String base=sound.optString("name","")+" "+sound.optString("path","")+" "+sound.optString("keywords","");if(rule!=null)base+=" "+rule.optString("keywords","");return normalize(base);
    }

    private boolean isPlayableAutomaticFormat(JSONObject sound){
        String n=sound.optString("name",sound.optString("path","")).toLowerCase(Locale.US);
        // MediaPlayer varia por aparelho para AIFF; evita escolher automaticamente
        // algo que pode falhar, sem impedir o teste manual do arquivo.
        if(n.endsWith(".aiff")||n.endsWith(".aif"))return false;
        String base=n.replace('\\','/');int slash=base.lastIndexOf('/');if(slash>=0)base=base.substring(slash+1);
        // Arquivos .L/.R são metades de uma gravação estéreo e não devem tocar sozinhos.
        return !(base.matches(".*[._ -][lr](?:_[0-9]+)?\\.[a-z0-9]+$")||base.contains("ambience.l")||base.contains("ambience.r"));
    }

    private static String duplicateFamily(String value){
        String n=value==null?"":value.replace('\\','/');int slash=n.lastIndexOf('/');if(slash>=0)n=n.substring(slash+1);
        int dot=n.lastIndexOf('.');if(dot>0)n=n.substring(0,dot);
        n=normalize(n);
        // Apenas o sufixo de cópia gerado na biblioteca (_1) é removido; números do nome original permanececem no corpus quando distinguem variantes.
        String raw=value==null?"":value.replace('\\','/').toLowerCase(Locale.US);int rawSlash=raw.lastIndexOf('/');if(rawSlash>=0)raw=raw.substring(rawSlash+1);int rawDot=raw.lastIndexOf('.');if(rawDot>0)raw=raw.substring(0,rawDot);
        raw=raw.replaceFirst("_1$","").replaceFirst("[._ -][lr]$","");
        return normalize(raw);
    }

    public synchronized int indexedSoundCount(){return sounds.length();}

    public String diagnoseSfx(String text){
        String normalized=normalize(expandAliases(text==null?"":text));
        if(!containsConcreteSfxTrigger(normalized))return "Nenhum gatilho concreto de SFX foi reconhecido.";
        Scored best=null;for(int i=0;i<sounds.length();i++){JSONObject sound=sounds.optJSONObject(i);if(sound==null||!"sfx".equals(typeOf(sound))||isAutoBlockedSound(sound))continue;int score=scoreSfx(sound,normalized);if(score>=20&&(best==null||score>best.score))best=new Scored(sound,score);}
        if(best==null)return "Gatilho reconhecido, mas nenhum arquivo compatível foi encontrado na pasta efetivamente indexada.";
        return "Gatilho reconhecido: "+best.sound.optString("name","SFX compatível");
    }

    /** Teste/pedido manual em modo SOLO: nenhum outro canal ou carregamento antigo permanece. */
    public void playManualSfx(Uri uri,String label){
        cancelPendingSfx();
        String shown=label==null||label.trim().length()==0?"arquivo selecionado":label.trim();
        lastPlan="SFX SOLO: "+shown;
        audio.playSfxSolo(uri,shown);
    }

    /** Remove callbacks antigos. Sem isto, efeitos de cenas/testes anteriores tocavam depois de PARAR. */
    public void cancelPendingSfx(){
        synchronized(pendingSfx){audioPlanGeneration++;for(Runnable r:new ArrayList<Runnable>(pendingSfx))handler.removeCallbacks(r);pendingSfx.clear();}
    }

    public void stopAll(){voiceNarrationActive=false;cancelPendingSfx();audio.stopAll();lastMusicMood="";lastPlan="Todos os canais interrompidos.";}

    private String lastMusicMood="";

    private void playPlan(String text,boolean includeSfx,boolean fallback){
        // Recalcular apenas a cama sonora não deve cortar o efeito pontual que
        // acabou de começar junto com a frase narrada.
        if(includeSfx){cancelPendingSfx();audio.stopSfx();}purgeInvalidContinuousAudio();
        final long generation=audioPlanGeneration;
        try{
            String n=normalize(expandAliases(text));if(n.length()<3||sounds.length()==0){lastPlan=sounds.length()==0?"Nenhum som indexado.":"A cena não contém palavras reconhecíveis.";return;}
            Scored amb=selectBest("ambience",n,fallback);Scored mus=selectBest("music",n,fallback);String mood=musicMood(n);
            List<Scored> effects=includeSfx&&automaticSfxEnabled?selectSfx(n,3):Collections.<Scored>emptyList();
            ArrayList<String> chosen=new ArrayList<String>();
            if(amb!=null&&(!audio.hasAmbience()||hasAmbienceChangeCue(n))){audio.playAmbience(Uri.parse(amb.sound.optString("uri")));chosen.add("ambiência: "+amb.sound.optString("name"));}else if(audio.hasAmbience())chosen.add("ambiência mantida");
            if(mus!=null&&(!audio.hasMusic()||!mood.equals(lastMusicMood))){audio.playMusic(Uri.parse(mus.sound.optString("uri")));lastMusicMood=mood;chosen.add("música "+mood+": "+mus.sound.optString("name"));}else if(mus==null&&"tensa".equals(mood)&&audio.hasMusic()){audio.stopMusic();lastMusicMood="";chosen.add("música calma removida: nenhuma faixa tensa compatível");}else if(audio.hasMusic())chosen.add("música mantida ("+mood+")");
            for(int i=0;i<effects.size();i++){Scored e=effects.get(i);scheduleSfx(e.sound,450L+(long)i*globalSfxGapSeconds*1000L,generation);chosen.add("SFX: "+e.sound.optString("name"));}
            if(includeSfx&&!automaticSfxEnabled)chosen.add("SFX automático desligado");
            else if(includeSfx&&effects.isEmpty())chosen.add("sem acontecimento concreto para SFX");
            lastPlan=chosen.isEmpty()?"Nenhum áudio compatível encontrado.":join(chosen," • ");
        }catch(Exception e){lastPlan="Falha ao montar áudio: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
    }

    private void scheduleSfx(JSONObject sound,long delay,long generation){
        PendingEffect effect=new PendingEffect(sound,generation);synchronized(pendingSfx){pendingSfx.add(effect);}handler.postDelayed(effect,delay);
    }

    private void playSfxIfAllowed(JSONObject sound){/*BR_JSON_WRAP*/try{
        if(isAutoBlockedSound(sound))return;
        String id=sound.optString("id",sound.optString("uri"));long now=System.currentTimeMillis();
        if(now-lastAutomaticSfxAt<globalSfxGapSeconds*1000L)return;
        synchronized(lastPlayed){Long last=lastPlayed.get(id);if(last!=null&&now-last<cooldownSeconds*1000L)return;lastPlayed.put(id,now);}lastAutomaticSfxAt=now;
        audio.playSfx(Uri.parse(sound.optString("uri")),sound.optString("name","SFX automático"));
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public void purgeInvalidContinuousAudio(){
        String activeAmbience=audio.currentAmbienceUri();if(activeAmbience.length()>0){for(int i=0;i<sounds.length();i++){JSONObject s=sounds.optJSONObject(i);if(s!=null&&activeAmbience.equals(s.optString("uri"))&&isContinuousCandidateBlocked(s,"ambience")){audio.stopAmbience();lastPlan="Efeito pontual removido da ambiência: "+s.optString("name","arquivo de efeito");break;}}}
        String activeMusic=audio.currentMusicUri();if(activeMusic.length()>0){for(int i=0;i<sounds.length();i++){JSONObject s=sounds.optJSONObject(i);if(s!=null&&activeMusic.equals(s.optString("uri"))&&isContinuousCandidateBlocked(s,"music")){audio.stopMusic();lastMusicMood="";break;}}}
    }

    private Scored selectBest(String type,String text,boolean fallback){
        Scored best=null;for(int i=0;i<sounds.length();i++){JSONObject s=sounds.optJSONObject(i);if(s==null||isAutoBlockedSound(s)||isContinuousCandidateBlocked(s,type)||!type.equals(typeOf(s)))continue;if(("ambience".equals(type)||"music".equals(type))&&scoreSfx(s,text)>=20)continue;int score=scoreContinuous(s,text);if("music".equals(type))score+=musicMoodScore(s,musicMood(text));if(best==null||score>best.score)best=new Scored(s,score);}
        if(best==null)return null;if(best.score>=2)return best;
        if(!fallback)return null;
        // Evita silêncio: prefere uma faixa genérica, não um SFX aleatório.
        Scored generic=null;for(int i=0;i<sounds.length();i++){JSONObject s=sounds.optJSONObject(i);if(s==null||isAutoBlockedSound(s)||isContinuousCandidateBlocked(s,type)||!type.equals(typeOf(s)))continue;String k=normalize(s.optString("path",s.optString("name")));int g=0;if(k.contains("generic")||k.contains("default")||k.contains("fantasy")||k.contains("ambient"))g+=3;if("ambience".equals(type)&&(k.contains("forest")||k.contains("city")||k.contains("night")||k.contains("wind")||k.contains("room")))g+=2;if("music".equals(type)){if(k.contains("calm")||k.contains("adventure")||k.contains("mystery")||k.contains("explore"))g+=2;g+=musicMoodScore(s,musicMood(text));}if(generic==null||g>generic.score)generic=new Scored(s,g);}
        if("music".equals(type)&&"tensa".equals(musicMood(text))&&generic!=null&&generic.score<0)return null;
        return generic;
    }

    private String musicMood(String text){if(containsAny(text,"combate,batalha,ataque,emboscada,perigo,ameaça,ameaca,tensão,tensao,terror,perseguição,perseguicao,monstro,inimigo,duelo,morte iminente"))return "tensa";if(containsAny(text,"triste,luto,funeral,melancolia,perda,despedida"))return "melancólica";if(containsAny(text,"mistério,misterio,segredo,suspeita,ruína,ruina,sombras,investiga"))return "misteriosa";if(containsAny(text,"descanso,seguro,paz,calma,tranquilo,taverna acolhedora"))return "calma";return "aventura";}
    private int musicMoodScore(JSONObject sound,String mood){String file=normalize(sound.optString("keywords","")+","+sound.optString("path","")+","+sound.optString("name",""));boolean calm=containsAny(file,"calm,calma,peace,peaceful,relax,gentle,serene,quiet,tranquil");boolean tense=containsAny(file,"tension,tensao,tense,danger,combat,battle,dark,threat,suspense,dramatic,action,peril");boolean sad=containsAny(file,"sad,triste,sorrow,mourning,melancholy,melancol");boolean mystery=containsAny(file,"mystery,misterio,mysterious,suspense,secret,shadow");if("tensa".equals(mood))return tense?40:(calm?-100:0);if("calma".equals(mood))return calm?30:(tense?-35:0);if("melancólica".equals(mood))return sad?35:(calm?5:0);if("misteriosa".equals(mood))return mystery?35:(calm?-10:0);return containsAny(file,"adventure,aventura,heroic,heroico,journey,travel,explore")?20:0;}
    private boolean hasAmbienceChangeCue(String text){return containsAny(text,"floresta,forest,cidade,city,taverna,tavern,caverna,cave,masmorra,dungeon,montanha,mountain,pântano,pantano,swamp,deserto,desert,praia,mar,sea,navio,ship,rio,river,chuva,rain,tempestade,storm,noite,night,interior,sala,room,estrada,road,campo,vila,aldeia,porto");}

    private List<Scored> selectSfx(String text,int max){
        ArrayList<Scored> list=new ArrayList<Scored>();long now=System.currentTimeMillis();
        for(int i=0;i<sounds.length();i++){
            JSONObject s=sounds.optJSONObject(i);if(s==null)continue;JSONObject catalogRule=ruleFor(s);if(!"sfx".equals(typeOf(s))||isAutoBlockedSound(s)||(catalogRule!=null&&!catalogRule.optBoolean("automatic",true)))continue;
            String id=s.optString("id",s.optString("uri"));Long last;synchronized(lastPlayed){last=lastPlayed.get(id);}if(last!=null&&now-last<cooldownSeconds*1000L)continue;
            int score=scoreSfx(s,text);if(score>=20)list.add(new Scored(s,score));
        }
        Collections.sort(list,new Comparator<Scored>(){public int compare(Scored a,Scored b){return b.score-a.score;}});
        if(list.size()>max)return new ArrayList<Scored>(list.subList(0,max));return list;
    }

    /** Pontuação ampla somente para faixas contínuas. */
    private int scoreContinuous(JSONObject s,String text){
        String words=s.optString("keywords")+","+s.optString("path")+","+s.optString("name");JSONObject rule=ruleFor(s);if(rule!=null)words=rule.optString("keywords")+","+words;
        int score=0;HashSet<String> used=new HashSet<String>();for(String raw:words.split("[,;|]")){String w=normalize(raw);if(w.length()<3||!used.add(w))continue;if(containsPhrase(text,w))score+=w.length()>9?7:(w.length()>5?5:3);}
        String file=normalize(s.optString("name"));for(String[] syn:SYNONYMS){if(text.contains(normalize(syn[0]))){for(String alt:syn[1].split(","))if(file.contains(normalize(alt))){score+=5;break;}}}
        JSONObject weighted=ruleFor(s);if(weighted!=null)score=(int)Math.round(score*Math.max(0.1,Math.min(5.0,weighted.optDouble("weight",1.0))));return score;
    }

    /** SFX exige uma ação/acontecimento concreto; clima, cenário e palavras genéricas não bastam. */
    private int scoreSfx(JSONObject s,String text){
        JSONObject rule=ruleFor(s);String corpus=s.optString("name","")+","+directParent(s.optString("path",""));if(rule!=null)corpus=rule.optString("keywords","")+","+corpus;corpus=normalize(corpus);
        int score=0;
        for(String[] family:SFX_FAMILIES){if(containsAny(text,family[1])&&containsAny(corpus,family[2]))score+=20;}
        if(rule!=null){HashSet<String> used=new HashSet<String>();for(String raw:rule.optString("keywords","").split("[,;|]")){String phrase=normalize(raw);if(phrase.length()<4||!used.add(phrase)||isGenericSfxPhrase(phrase))continue;if(containsPhrase(text,phrase))score+=10;}}
        if(rule!=null)score=(int)Math.round(score*Math.max(0.1,Math.min(5.0,rule.optDouble("weight",1.0))));return score;
    }

    /** Vinhetas musicais curtas não representam cenário nem acontecimento. O arquivo
     * conhecido como "FX Stab Musical" e variações ficam somente no teste manual,
     * mesmo que o índice antigo o tenha classificado como música em vez de SFX. */
    /** Impede que efeitos pontuais classificados incorretamente como ambiência
     * virem loops. Porta, batida, passos, arma, impacto e voz nunca são cama sonora. */
    private boolean isContinuousCandidateBlocked(JSONObject sound,String requestedType){
        if(sound==null)return false;String corpus=sound.optString("name","")+" "+sound.optString("path","")+" "+sound.optString("keywords","");JSONObject rule=ruleFor(sound);if(rule!=null)corpus+=" "+rule.optString("keywords","");String n=normalize(corpus);
        String eventMarkers="porta,door,open door,close door,door opens,door closes,opening door,closing door,creak,slam,hinge,abre,abriu,fecha,fechou,knock,knocking,batida,pancada,footstep,footsteps,passos,sword,espada,blade,lamina,weapon,arma,armor,armadura,chain,corrente,clang,clank,scrape,raspa,scream,grito,growl,rosnado,roar,rugido,glass,vidro,bell,sino,coin,moeda,arrow,flecha,explosion,explosao,punch,soco,kick,chute,impact,impacto,body fall,queda,anvil,bigorna,hammer,martelo,stagecoach,carriage,cart,wagon,coach,carroca,carruagem,ui open,ui close,open door,close door";
        boolean event=containsAny(n,eventMarkers);if(!event)return false;
        if("ambience".equals(requestedType))return true;
        return "music".equals(requestedType)&&containsAny(n,"sfx,foley,effect,efeito,fx");
    }

    private static boolean isAutoBlockedSound(JSONObject sound){
        if(sound==null)return false;String n=normalize(sound.optString("name","")+" "+sound.optString("path",""));
        return n.contains("fx stab musical")||n.contains("musical stab")||n.contains("music stab")||n.contains("fx music stab")||n.contains("musical stinger")||n.contains("music stinger");
    }

    private static String directParent(String path){String n=path==null?"":path.replace('\\','/');int last=n.lastIndexOf('/');if(last<=0)return "";int before=n.lastIndexOf('/',last-1);return n.substring(before+1,last);}
    private static boolean containsAny(String text,String csv){for(String raw:csv.split(",")){String p=normalize(raw);if(p.length()>0&&containsPhrase(text,p))return true;}return false;}
    private static boolean isGenericSfxPhrase(String phrase){for(String raw:GENERIC_SFX_TERMS.split(","))if(phrase.equals(normalize(raw)))return true;return false;}

    private String expandAliases(String text){
        StringBuilder b=new StringBuilder(text==null?"":text);JSONArray aliases=keywordOverrides.optJSONArray("__aliases");if(aliases!=null){String n=normalize(text);for(int i=0;i<aliases.length();i++){JSONObject a=aliases.optJSONObject(i);if(a!=null&&n.contains(normalize(a.optString("trigger"))))b.append(' ').append(a.optString("target"));}}return b.toString();
    }

    private JSONObject ruleFor(JSONObject s){if(s==null)return null;JSONObject exact=keywordOverrides.optJSONObject("__filename:"+normalizedFilename(s.optString("name")));if(exact!=null)return exact;String[] keys={fileKey(s.optString("name")),normalize(s.optString("name")),fileKey(s.optString("path"))};for(String k:keys){JSONObject r=keywordOverrides.optJSONObject(k);if(r!=null)return r;}return null;}
    private String typeOf(JSONObject s){if(s==null)return "unknown";String hard=hardType(s.optString("path",s.optString("name","")),s.optString("keywords",""));if(hard.length()>0)return hard;JSONObject r=ruleFor(s);String t=r==null?s.optString("type",""):r.optString("type",s.optString("type",""));String normalized=normalizeType(t);return normalized.length()==0?"unknown":normalized;}

    private String keywordsFor(String path){
        String n=normalize(path);StringBuilder b=new StringBuilder(n.replace(' ',','));for(String[] x:SYNONYMS){boolean match=n.contains(normalize(x[0]));if(!match)for(String alt:x[1].split(","))if(n.contains(normalize(alt))){match=true;break;}if(match)b.append(',').append(x[0]).append(',').append(x[1]);}return b.toString();
    }

    private String guessType(String path){return guessType(path,keywordsFor(path));}
    private String guessType(String path,String keywords){
        String hard=hardType(path,keywords);if(hard.length()>0)return hard;
        String n=normalize(path+" "+keywords);
        if(containsAny(n,"song,theme,track,melody,score,bgm,soundtrack,musica,trilha,bardic,dungeon of,cursed lands,stronghold,chill rpg,intro track,from dusk till dawn,in the arena"))return "music";
        int ambienceScore=countMarkers(n,"ambience,ambiencia,ambient,atmosphere,soundscape,forest,floresta,cave,cavern,caverna,rain,chuva,village,town,city,cidade,river,shore,lake,swamp,pantano,desert,nature,natureza,crickets,crowd,walla,tavern,taverna,blacksmith,armory,wind,vento,night,noite,ship,port,barn,farm,countryside,sewer,subterraneo,beach,waves,mine,mineshaft,mina,library,biblioteca,corridor,corredor,shop,store,loja,chamber,camara,hell,inferno,fireplace,lareira,crackling,crepitar");
        int sfxScore=countMarkers(n,"combat,hit,swing,draw,impact,footstep,passos,scream,grito,growl,roar,death,knock,scrape,break,chains,horse,neigh,gallop,punch,dagger,sword,bow,crossbow,staff,slash,strike,door,porta,glass,vidro,ui ping,ui open,ui close,jump,landing,cuspe,spit,pergaminho,prego,weapon,armor,blade,fire activate,npc voice,voice,grunt,ouch,pain,dragon,creature,criatura,monster,monstro,vocal,vocalization,laugh,laughter,risada,risadas,pour,encher,enchendo,caneca,bell,sino,rock,rocks,stone,stones,pedra,pedras,fall,queda,smoke,fumaca,invisibility,invisibilidade,magical,magic,magia,sting,buff,pickup,level up,rage,watering,regar,seed,semente,anvil,bigorna,hammer,martelo,stagecoach,carriage,cart,wagon,coach,carroca,carruagem");
        if(n.contains(" loop"))ambienceScore+=2;if(sfxScore>ambienceScore&&sfxScore>0)return "sfx";if(ambienceScore>0)return "ambience";if(sfxScore>0)return "sfx";return "";
    }
    private static int countMarkers(String text,String csv){int count=0;for(String raw:csv.split(",")){String p=normalize(raw);if(p.length()>0&&containsPhrase(text,p))count++;}return count;}

    public void shutdown(){
        audioPlanner.shutdownNow();
        sfxPlanner.shutdownNow();
        persistence.shutdown();
    }

    private JSONObject findSoundByKey(String key){for(int i=0;i<sounds.length();i++){JSONObject s=sounds.optJSONObject(i);if(s!=null&&fileKey(s.optString("name")).equals(key))return s;}return null;}
    private static boolean isDirectory(String mime){String n=normalize(mime);return DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)||n.contains("google apps folder")||n.endsWith(" folder");}
    private static boolean isAudio(String name,String mime){if(mime!=null&&mime.toLowerCase(Locale.US).startsWith("audio/"))return true;String n=(name==null?"":name).toLowerCase(Locale.US);for(String ext:AUDIO_EXT)if(n.endsWith(ext))return true;return false;}
    private static boolean looksLikeFilename(String value){String n=value==null?"":value.toLowerCase(Locale.US);for(String ext:AUDIO_EXT)if(n.contains(ext))return true;return n.contains("/")||n.contains("\\");}
    private static boolean isType(String value){String n=normalize(value);return n.equals("ambience")||n.equals("ambient")||n.equals("ambiencia")||n.equals("music")||n.equals("musica")||n.equals("trilha")||n.equals("bgm")||n.equals("sfx")||n.equals("efeito")||n.equals("effects");}
    private static String normalizeType(String type){String n=normalize(type);if(n.contains("amb"))return "ambience";if(n.contains("music")||n.contains("musica")||n.contains("trilha")||n.contains("bgm"))return "music";if(n.equals("sfx")||n.contains("efeito")||n.contains("effect")||n.contains("foley"))return "sfx";return "";}
    private static String fileKey(String value){String n=value==null?"":value.replace('\\','/');int slash=n.lastIndexOf('/');if(slash>=0)n=n.substring(slash+1);int dot=n.lastIndexOf('.');if(dot>0)n=n.substring(0,dot);return normalize(n);}
    private static boolean containsPhrase(String text,String phrase){return text.equals(phrase)||text.startsWith(phrase+" ")||text.endsWith(" "+phrase)||text.contains(" "+phrase+" ");}
    private static String normalize(String v){if(v==null)return "";String n=Normalizer.normalize(v.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
    private static String join(List<String> values,String sep){StringBuilder b=new StringBuilder();for(String v:values){if(b.length()>0)b.append(sep);b.append(v);}return b.toString();}
    private static String read(File f)throws Exception{BufferedReader r=new BufferedReader(new FileReader(f));StringBuilder b=new StringBuilder();String l;while((l=r.readLine())!=null)b.append(l);r.close();return b.toString();}
    private static void write(File f,String s)throws Exception{FileOutputStream o=new FileOutputStream(f);o.write(s.getBytes("UTF-8"));o.close();}

    private final class PendingEffect implements Runnable{
        final JSONObject sound;final long generation;PendingEffect(JSONObject s,long g){sound=s;generation=g;}
        public void run(){synchronized(pendingSfx){pendingSfx.remove(this);}if(generation!=audioPlanGeneration)return;playSfxIfAllowed(sound);}
    }
    private static final class DocRow{String id,name,mime;long size,modified;}
    private static final class CatalogCandidate{Uri uri;String path;int depth;}
    private static final class ParsedRule{String fileKey="",keywords="",type="",trigger="";}
    private static final class Choice{final Scored scored;int adjusted;final AudioCatalogDb.Stats stats;final long duration;Choice(Scored value,int score,AudioCatalogDb.Stats st,long d){scored=value;adjusted=score;stats=st;duration=d;}}
    private static final class Scored{final JSONObject sound;final int score;Scored(JSONObject s,int value){sound=s;score=value;}}
}
