package com.braseiro.dnd5e;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Motor de áudio da narração 3.5.
 *
 * Regras centrais:
 * - nada é iniciado pela chegada da resposta da IA; prepareScene só é chamado pelo PLAY;
 * - música, ambiência, voz e SFX usam canais independentes;
 * - cada PLAY cria uma linha do tempo nova, portanto a cena pode ser repetida sem limite;
 * - no TTS Android o onRangeStart é a fonte preferencial de sincronização;
 * - no Charon a duração real do arquivo posiciona os efeitos;
 * - nenhum callback de uma execução antiga pode vazar para a próxima.
 */
public final class NarrativeAudioEngine {
    public interface Status { void onStatus(String text); }

    private static final long EVENT_COOLDOWN_MS=2400L;
    private static final long FILE_COOLDOWN_MS=3000L;
    private static final long GLOBAL_MIN_GAP_MS=260L;
    private static final int DEFAULT_TRANSITION_MS=3000;
    private static final long NATIVE_RANGE_WATCHDOG_MS=420L;

    private static final CueFamily[] FAMILIES={
        new CueFamily("wood_knock",96,1.00f,"bate na madeira|batida na madeira|batidas na madeira|bater na madeira|knock on wood|knocking on wood","batida na madeira"),
        new CueFamily("knock",98,1.00f,"batida na porta|batidas na porta|bate a porta|bate à porta|batem a porta|bater na porta|knock at the door|knocking at the door|knocking on the door|pancada na porta","batida na porta"),
        new CueFamily("door",88,.92f,"porta range|porta se abre|a porta abre|porta abriu|abre a porta|abriu a porta|porta se fecha|a porta fecha|fecha a porta|fechou a porta|porta bate|door creak|door opens|door closes|door slams","porta de madeira"),
        new CueFamily("anvil",90,.92f,"martelo bate na bigorna|martelo na bigorna|marteladas na bigorna|batida de bigorna|hammer hits anvil|hammer on anvil","martelo na bigorna"),
        new CueFamily("cart",65,.78f,"carroça se aproxima|carroca se aproxima|carruagem se aproxima|carroça passa|carroca passa|carruagem passa|rodas da carroça|rodas de carroça|stagecoach approaches|carriage approaches|cart approaches|wagon approaches","carroça e rodas"),
        new CueFamily("footsteps",68,.76f,"ouve passos|ouvem passos|passos ecoam|passos se aproximam|passos correm|passos na madeira|passos na terra|footsteps|steps echo|approaching steps","passos"),
        new CueFamily("sword",91,.98f,"desembainha a espada|desembainhou a espada|lâmina raspa|lamina raspa|espadas se chocam|espada corta|sword drawn|swords clash|blade scrapes|sword strikes|ataca com a espada|golpeia com a espada|desfere um golpe de espada|golpe de espada|ataque com a espada|lâmina afiada|lamina afiada|espada atinge|golpe de machado|adaga corta|desembainha sua arma|desembainha a arma|draws weapon|swings sword|strikes with sword|slashes with blade","espada lâmina"),
        new CueFamily("metal",78,.86f,"armadura range|armadura raspa|metal raspa|metal se choca|ferro tilinta|cota de malha tilinta|armor creaks|metal scrapes|clang|clank|placas de metal|portão de ferro|portao de ferro|grade de ferro|trinco de ferro","metal armadura"),
        new CueFamily("chains",80,.86f,"correntes arrastam|corrente arrasta|corrente tilinta|chains drag|chains rattle|grilhão bate|grilhao bate","correntes"),
        new CueFamily("roar",93,1.00f,"solta um rugido|rugido ecoa|rosna alto|monstro rosna|roar echoes|growls|roars","rugido monstro"),
        new CueFamily("scream",89,.96f,"solta um grito|grito ecoa|grita de dor|scream|shriek|screams","grito"),
        new CueFamily("horse",72,.82f,"cavalos galopam|cavalo galopa|cavalo relincha|cascos batem|galope se aproxima|horses gallop|horse neighs|hooves thunder","cavalo galope"),
        new CueFamily("thunder",84,.90f,"trovão ribomba|trovao ribomba|trovão explode|trovao explode|thunder cracks|thunder rumbles|relâmpago estala|relampago estala","trovão"),
        new CueFamily("ignition",84,.88f,"acende a fogueira|acende a lareira|acende a tocha|tocha se acende|chamas irrompem|fogo se acende|fire ignites|torch ignites|flames burst","fogo se acende"),
        new CueFamily("fire_crackle",48,.54f,"fogo crepita|chamas crepitam|lareira crepita|lenha estala|fire crackles|flames crackle","lareira crepitando"),
        new CueFamily("glass",95,1.00f,"vidro se quebra|quebra o vidro|vidro estilhaça|vidro estilhaca|copo se quebra|glass shatters|glass breaks","vidro quebrando"),
        new CueFamily("bell",72,.80f,"sino toca|sinos tocam|sino ressoa|bell rings|bells toll|campainha toca","sino"),
        new CueFamily("coins",58,.68f,"moedas caem|moeda cai|ouro cai|coins drop|coins spill","moedas"),
        new CueFamily("arrow",91,.98f,"flecha é disparada|flecha e disparada|solta a flecha|dispara o arco|arrow flies|bow fires|bow is fired","arco flecha"),
        new CueFamily("explosion",100,1.00f,"explosão sacode|explosao sacode|algo explode|explodiu|explosion|blast|boom","explosão"),
        new CueFamily("magic",84,.90f,"conjura o feitiço|conjura o feitico|lança o feitiço|lanca o feitico|spell cast|magia irrompe|raio arcano dispara","magia feitiço"),
        new CueFamily("fall",80,.88f,"corpo cai no chão|corpo cai no chao|despenca no chão|despenca no chao|queda pesada|body falls|crashes to the ground","queda impacto"),
        new CueFamily("splash",74,.86f,"mergulha na água|mergulha na agua|cai na água|cai na agua|grande salpico|splash","água salpico"),
        new CueFamily("impact",86,.94f,"soco acerta|chute acerta|golpe acerta|impacto seco|punch lands|kick lands|weapon impact","impacto golpe"),
        new CueFamily("writing",52,.56f,"escreve no papel|escreve no pergaminho|lápis risca|lapis risca|pena escreve|writes on paper|pencil writes","escrita lápis papel"),
        new CueFamily("drink",48,.52f,"enche a caneca|cerveja cai na caneca|coloca o copo|canecas brindam|copos brindam|pours the beer|mugs clink","caneca copos bebida"),
        new CueFamily("laugh",54,.60f,"solta uma risada|todos riem|risadas ecoam|laughs|laughter erupts","risadas"),
        new CueFamily("rocks",64,.72f,"pedras caem|pedra raspa|rochas desabam|stones fall|rocks tumble","pedras caindo")
    };

    private final SmartAudioLibrary catalog;
    private final AudioController audio;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService scenePlanner=Executors.newSingleThreadExecutor();
    private final ExecutorService cuePlanner=Executors.newSingleThreadExecutor();
    private final LinkedHashMap<String,Long> lastEventAt=new LinkedHashMap<String,Long>();
    private final LinkedHashMap<String,Long> lastFileAt=new LinkedHashMap<String,Long>();
    private final HashMap<String,UtteranceState> utterances=new HashMap<String,UtteranceState>();
    private final HashMap<String,UtteranceState> preparedUtterances=new HashMap<String,UtteranceState>();
    private final HashSet<String> completedNativeUtterances=new HashSet<String>();
    private final ArrayList<Runnable> pending=new ArrayList<Runnable>();
    private final ArrayList<ExplicitCue> explicitCues=new ArrayList<ExplicitCue>();
    private Status status;
    private long generation=0L;
    private long preparationGeneration=0L;
    private long lastSfxAt=0L;
    private boolean narrationActive=false;
    private boolean voiceHasStarted=false;
    private boolean voiceStartedForPreparation=false;
    private PreparedScene pendingPreparedScene;
    private String sceneSignature="";
    private String baseSignature="";
    private String climateSignature="";
    private String musicMood="";
    private String currentDynamicEnv="";
    private String lastPlan="Áudio aguardando o botão PLAY.";
    private JSONObject lastDiagnostic=new JSONObject();

    public NarrativeAudioEngine(SmartAudioLibrary catalog,AudioController audio){
        this.catalog=catalog;this.audio=audio;
        this.audio.setPlaybackFailure(new AudioController.PlaybackFailure(){
            public void onFailure(String channel,String uri,String label){NarrativeAudioEngine.this.catalog.markFailureByUri(uri);}
        });
    }
    public void setStatus(Status value){status=value;}

    /** Chamado exclusivamente depois de um clique em PLAY. */
    public synchronized void prepareScene(String narration,JSONObject aiPlan,JSONObject sceneState){
        long token=++preparationGeneration;
        voiceStartedForPreparation=false;
        ScenePlan plan=buildPlan(narration,aiPlan,sceneState);
        explicitCues.clear();explicitCues.addAll(plan.explicitCues);
        PreparedScene prepared=computePreparedScene(narration,plan);
        pendingPreparedScene=prepared;
        if(voiceStartedForPreparation)activatePreparedSceneLocked(token,prepared);
    }

    /**
     * Analisa catálogo e cena fora da interface sem atrasar o início da voz.
     * O callback ready é liberado assim que o plano leve e os SFX de frase estão
     * prontos; ambiência e música são resolvidas em paralelo e só começam quando
     * a primeira palavra é realmente pronunciada.
     */
    public void prepareSceneAsync(final String narration,final JSONObject aiPlan,final JSONObject sceneState,final Runnable ready){
        final long token;final ScenePlan plan;
        synchronized(this){
            token=++preparationGeneration;pendingPreparedScene=null;voiceStartedForPreparation=false;
            plan=buildPlan(narration,aiPlan,sceneState);
            explicitCues.clear();explicitCues.addAll(plan.explicitCues);
        }
        if(ready!=null)handler.post(ready);
        scenePlanner.execute(new Runnable(){public void run(){
            synchronized(NarrativeAudioEngine.this){if(token!=preparationGeneration)return;}
            PreparedScene value;
            try{value=computePreparedScene(narration,plan);}catch(Throwable failure){
                value=new PreparedScene(plan,null,null,null,detectCues(safe(narration),plan.explicitCues).size());
            }
            final PreparedScene prepared=value;
            handler.post(new Runnable(){public void run(){synchronized(NarrativeAudioEngine.this){
                if(token!=preparationGeneration)return;pendingPreparedScene=prepared;
                if(voiceStartedForPreparation)activatePreparedSceneLocked(token,prepared);
            }}});
        }});
    }

    private PreparedScene computePreparedScene(String narration,JSONObject aiPlan,JSONObject sceneState){
        return computePreparedScene(narration,buildPlan(narration,aiPlan,sceneState));
    }

    private PreparedScene computePreparedScene(String narration,ScenePlan plan){
        String preview=safe(narration);if(preview.length()>900)preview=preview.substring(0,900);
        String query=plan.environment+" "+plan.weather+" "+plan.location+" "+plan.timeOfDay+" "+plan.mood+" "+plan.soundscapeHints+" "+preview;
        JSONObject ambience=catalog.resolveAmbience(query);
        JSONObject climate=catalog.resolveClimate(query);
        JSONObject music="none".equals(plan.mood)?null:catalog.resolveMusic(query,plan.mood);
        int cueCount=detectCues(safe(narration),plan.explicitCues).size();
        return new PreparedScene(plan,ambience,climate,music,cueCount);
    }

    private synchronized void activatePreparedSceneLocked(long token,PreparedScene prepared){
        if(token!=preparationGeneration||prepared==null)return;
        pendingPreparedScene=null;applyPreparedScene(prepared);
    }

    private void applyPreparedScene(PreparedScene prepared){/*BR_JSON_WRAP*/try{
        if(prepared==null)return;ScenePlan plan=prepared.plan;
        String nextBase=fold(plan.environment+"|"+plan.location+"|"+plan.timeOfDay+"|"+plan.soundscapeHints);
        String nextClimate=fold(plan.weather+"|"+plan.environment+"|"+plan.location);
        String nextSignature=nextBase+"|"+nextClimate;
        boolean baseChanged=baseSignature.length()==0||!baseSignature.equals(nextBase);
        boolean climateChanged=climateSignature.length()==0||!climateSignature.equals(nextClimate);
        boolean moodChanged=musicMood.length()==0||!musicMood.equals(plan.mood);
        ArrayList<String> summary=new ArrayList<String>();
        if(prepared.ambience!=null){String uri=prepared.ambience.optString("uri");if(baseChanged||!uri.equals(audio.currentAmbienceUri())){audio.playAmbience(Uri.parse(uri),plan.transitionMs);catalog.markPlayed(prepared.ambience,"ambience","base",nextBase);}summary.add("ambiente-base: "+prepared.ambience.optString("name","selecionado"));}
        else summary.add("ambiente-base não encontrado");
        if(prepared.climate!=null){String uri=prepared.climate.optString("uri");if(climateChanged||!uri.equals(audio.currentClimateUri())){audio.playClimate(Uri.parse(uri),plan.transitionMs);catalog.markPlayed(prepared.climate,"ambience","climate",nextClimate);}summary.add("clima: "+prepared.climate.optString("name","selecionado"));}
        else {if(audio.hasClimate())audio.stopClimate();summary.add("sem camada climática");}
        if("none".equals(plan.mood)){if(audio.hasMusic())audio.stopMusic();summary.add("sem música por plano explícito");}
        else if(prepared.music!=null){String uri=prepared.music.optString("uri");if(moodChanged||!uri.equals(audio.currentMusicUri())){audio.playMusic(Uri.parse(uri),plan.transitionMs);catalog.markPlayed(prepared.music,"music","music",plan.mood);}summary.add("música "+plan.mood+": "+prepared.music.optString("name","selecionada"));}
        else summary.add("música não encontrada na pasta importada");
        baseSignature=nextBase;climateSignature=nextClimate;sceneSignature=nextSignature;musicMood=plan.mood;currentDynamicEnv=plan.environment;explicitCues.clear();explicitCues.addAll(plan.explicitCues);
        try{lastDiagnostic=new JSONObject().put("environment",plan.environment).put("weather",plan.weather).put("location",plan.location).put("time_of_day",plan.timeOfDay).put("music_mood",plan.mood).put("soundscape_hints",plan.soundscapeHints).put("transition_ms",plan.transitionMs).put("detected_cues",prepared.cueCount).put("catalog_sounds",catalog.indexedSoundCount()).put("base_signature",baseSignature).put("climate_signature",climateSignature).put("scene_signature",sceneSignature).put("play_only",true).put("planner_thread","background");}catch(Exception ignored){AppLog.ignored("NarrativeAudioEngine","Falha tolerada; a execução continuou.",ignored);}
        lastPlan=join(summary," • ")+" • "+prepared.cueCount+" SFX posicionados";
        say("Áudio preparado pelo PLAY: "+lastPlan);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** Aguarda o MediaPlayer preparar as camas, sem bloquear a interface. */
    public void waitUntilBedsReady(final Runnable ready){
        final long started=System.currentTimeMillis();final long useGeneration;
        synchronized(this){useGeneration=generation;}
        final Runnable[] poll=new Runnable[1];
        poll[0]=new Runnable(){public void run(){synchronized(NarrativeAudioEngine.this){pending.remove(this);if(useGeneration!=generation)return;if(audio.bedsReady()||System.currentTimeMillis()-started>=3500L){if(ready!=null)ready.run();return;}pending.add(poll[0]);}handler.postDelayed(poll[0],70L);}};
        handler.post(poll[0]);
    }

    public synchronized void beginNarration(List<String> chunks){/*BR_JSON_WRAP*/try{
        generation++;cancelCallbacksLocked();utterances.clear();preparedUtterances.clear();completedNativeUtterances.clear();lastEventAt.clear();lastFileAt.clear();lastSfxAt=0L;narrationActive=true;voiceHasStarted=false;audio.stopSfx();
        final ArrayList<String> chunkSnapshot=new ArrayList<String>();
        if(chunks!=null)for(String chunk:chunks)if(chunk!=null&&chunk.trim().length()>0)chunkSnapshot.add(chunk);
        final ArrayList<ExplicitCue> cueSnapshot=new ArrayList<ExplicitCue>(explicitCues);
        // Nenhum trecho é analisado na thread da interface. Até o primeiro bloco é
        // preparado pelo planejador; a voz pode começar enquanto os SFX são resolvidos.
        // Isso evita ANR em aparelhos lentos e bibliotecas de áudio grandes.
        // Todos os blocos e consultas ao catálogo ficam no planejador.
        final long preloadGeneration=generation;
        cuePlanner.execute(new Runnable(){public void run(){/*BR_JSON_WRAP*/try{
            for(int index=0;index<chunkSnapshot.size();index++){
                String chunk=chunkSnapshot.get(index);UtteranceState state;
                synchronized(NarrativeAudioEngine.this){
                    if(preloadGeneration!=generation||!narrationActive)return;
                    state=preparedUtterances.get(chunkKey(chunk));
                }
                if(state==null){
                    UtteranceState detected=new UtteranceState(chunk,detectCues(chunk,cueSnapshot));
                    synchronized(NarrativeAudioEngine.this){
                        if(preloadGeneration!=generation||!narrationActive)return;
                        state=preparedUtterances.get(chunkKey(chunk));
                        if(state==null){state=detected;preparedUtterances.put(chunkKey(chunk),state);}
                    }
                }
                for(Cue cue:state.cues){
                synchronized(NarrativeAudioEngine.this){if(preloadGeneration!=generation||!narrationActive)return;}
                try{
                    JSONObject prepared=catalog.resolveSfx(cue.event,cue.query);
                    synchronized(NarrativeAudioEngine.this){if(preloadGeneration!=generation)return;cue.preparedSound=prepared;}
                    if(prepared!=null)audio.preloadSfx(Uri.parse(prepared.optString("uri")),prepared.optString("name",cue.event));
                }catch(Throwable ignored){AppLog.ignored("NarrativeAudioEngine","Falha tolerada; a execução continuou.",ignored);}
                }
            }
        }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}});
        // Charon também usa a fila interna do AudioController; este sinal cobre a voz Android.
        audio.setExternalVoiceActive(true);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized void onNativeUtteranceStart(String id,String text){
        onNativeUtteranceStart(id,text,false);
    }

    /**
     * A voz Android usa onRangeStart quando o mecanismo realmente o oferece.
     * Em aparelhos que anunciam a API mas não entregam intervalos, um watchdog
     * ativa a linha do tempo estimada sem duplicar os efeitos.
     */
    public synchronized void onNativeUtteranceStart(final String id,String text,boolean rangeCallbacksExpected){/*BR_JSON_WRAP*/try{
        if(!narrationActive||id==null||completedNativeUtterances.contains(id))return;markVoiceStartedLocked();
        checkDynamicAmbienceShift(text);
        // onRangeStart pode chegar antes de onStart em engines externas.
        // Nesse caso a fala já foi registrada e os efeitos já seguiram a posição real.
        if(utterances.containsKey(id))return;
        final UtteranceState state=stateFor(text);
        state.startedAt=System.currentTimeMillis();
        utterances.put(id,state);
        final int duration=estimateDurationMs(text);
        if(!rangeCallbacksExpected){
            scheduleByDuration(state,duration,generation,110L,true);
            return;
        }
        final long useGeneration=generation;
        final Runnable watchdog=new Runnable(){public void run(){synchronized(NarrativeAudioEngine.this){
            pending.remove(this);state.fallbackTasks.remove(this);
            if(useGeneration!=generation||!narrationActive||state.rangeSeen)return;
            long elapsed=Math.max(0L,System.currentTimeMillis()-state.startedAt);
            scheduleByDurationFromElapsed(state,duration,useGeneration,elapsed);
        }}};
        state.fallbackTasks.add(watchdog);pending.add(watchdog);handler.postDelayed(watchdog,NATIVE_RANGE_WATCHDOG_MS);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    /** API 26+: sincronização preferencial pela posição real pronunciada. */
    public synchronized void onNativeRange(String id,String text,int start,int end){/*BR_JSON_WRAP*/try{
        if(!narrationActive||id==null||completedNativeUtterances.contains(id))return;
        markVoiceStartedLocked();
        UtteranceState state=utterances.get(id);
        if(state==null){state=stateFor(text);state.startedAt=System.currentTimeMillis();utterances.put(id,state);}
        if(!state.rangeSeen){state.rangeSeen=true;cancelFallbackTasks(state);}
        fireDue(state,Math.max(start,end));
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public synchronized void onSyntheticChunkStart(String text,int durationMs){
        if(!narrationActive)return;markVoiceStartedLocked();checkDynamicAmbienceShift(text);scheduleByDuration(stateFor(text),Math.max(500,durationMs),generation,0L,false);
    }

    private void markVoiceStartedLocked(){
        if(voiceHasStarted)return;voiceHasStarted=true;voiceStartedForPreparation=true;
        if(pendingPreparedScene!=null)activatePreparedSceneLocked(preparationGeneration,pendingPreparedScene);
    }

    public synchronized void onUtteranceDone(String id){if(id==null||!narrationActive)return;completedNativeUtterances.add(id);UtteranceState state=utterances.remove(id);if(state!=null)cancelFallbackTasks(state);}
    public synchronized void endNarration(){narrationActive=false;voiceHasStarted=false;generation++;cancelCallbacksLocked();utterances.clear();preparedUtterances.clear();completedNativeUtterances.clear();audio.setExternalVoiceActive(false);}
    public synchronized void cancelNarration(){narrationActive=false;voiceHasStarted=false;voiceStartedForPreparation=false;generation++;preparationGeneration++;pendingPreparedScene=null;cancelCallbacksLocked();utterances.clear();preparedUtterances.clear();completedNativeUtterances.clear();currentDynamicEnv="";audio.setExternalVoiceActive(false);audio.stopSfx();}
    public synchronized void stopAll(){narrationActive=false;voiceHasStarted=false;voiceStartedForPreparation=false;generation++;preparationGeneration++;pendingPreparedScene=null;cancelCallbacksLocked();utterances.clear();preparedUtterances.clear();completedNativeUtterances.clear();explicitCues.clear();lastEventAt.clear();lastFileAt.clear();sceneSignature="";baseSignature="";climateSignature="";musicMood="";currentDynamicEnv="";audio.stopAll();lastPlan="Todos os canais e eventos pendentes foram interrompidos.";}
    public synchronized void shutdown(){generation++;preparationGeneration++;pendingPreparedScene=null;cancelCallbacksLocked();narrationActive=false;voiceHasStarted=false;voiceStartedForPreparation=false;utterances.clear();preparedUtterances.clear();completedNativeUtterances.clear();explicitCues.clear();scenePlanner.shutdownNow();cuePlanner.shutdownNow();audio.setExternalVoiceActive(false);}
    public synchronized String lastPlanSummary(){return lastPlan;}
    public synchronized boolean hasConcreteCue(String text){return !detectCues(text,explicitCues).isEmpty();}
    public synchronized JSONObject diagnosticReport(){/*BR_JSON_WRAP*/try{try{return new JSONObject(lastDiagnostic.toString()).put("active",narrationActive).put("voice_confirmed_for_scene",voiceStartedForPreparation).put("pending_callbacks",pending.size()).put("audio_channels",audio.activeChannels());}catch(Exception e){return new JSONObject();}}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void checkDynamicAmbienceShift(String text){
        if(text==null||!narrationActive)return;
        String detected=inferExplicitEnvironment(text);
        if(detected!=null&&detected.length()>0&&!detected.equalsIgnoreCase(currentDynamicEnv)){
            currentDynamicEnv=detected;
            JSONObject amb=catalog.resolveAmbience(detected);
            if(amb!=null){
                String uri=amb.optString("uri");
                if(uri.length()>0&&!uri.equals(audio.currentAmbienceUri())){
                    audio.playAmbience(Uri.parse(uri),3000);
                    catalog.markPlayed(amb,"ambience","base",detected);
                    say("Ambiência alternada para: "+amb.optString("name",detected));
                }
            }
        }
    }

    private static String inferExplicitEnvironment(String text){
        String n=fold(text);
        if(has(n,"taverna","estalagem","inn","tavern"))return "interior de taverna";
        if(has(n,"masmorra","dungeon","catacumba"))return "masmorra subterrânea";
        if(has(n,"caverna","cave","gruta"))return "caverna";
        if(has(n,"floresta","forest","bosque"))return "floresta";
        if(has(n,"cidade","city","mercado"))return "cidade";
        if(has(n,"aldeia","vila","village"))return "aldeia";
        if(has(n,"navio","ship","mar","porto"))return "costa e porto";
        if(has(n,"pantano","pântano","swamp"))return "pântano";
        if(has(n,"deserto","desert"))return "deserto";
        if(has(n,"montanha","mountain"))return "montanhas";
        if(has(n,"chuva","tempestade"))return "exterior sob chuva";
        return null;
    }

    private UtteranceState stateFor(String text){
        UtteranceState prepared=preparedUtterances.get(chunkKey(text));
        if(prepared==null)return new UtteranceState(text,detectCues(text,explicitCues));
        ArrayList<Cue> copies=new ArrayList<Cue>(prepared.cues.size());
        for(Cue original:prepared.cues){
            Cue copy=new Cue(original.event,original.charIndex,original.priority,original.gain,original.query);
            copy.preparedSound=original.preparedSound;
            copies.add(copy);
        }
        return new UtteranceState(text,copies);
    }
    private void fireDue(UtteranceState state,int charEnd){for(Cue cue:state.cues)if(!cue.fired&&cue.charIndex<=charEnd)fireCue(cue);}

    private void scheduleByDuration(final UtteranceState state,int durationMs,final long useGeneration,long extraDelay,boolean nativeFallback){
        int length=Math.max(1,state.text.length());
        for(final Cue cue:state.cues){
            long delay=extraDelay+Math.max(0,Math.min(Math.max(0,durationMs-80),Math.round(durationMs*(cue.charIndex/(float)length))));
            scheduleCueTask(state,cue,useGeneration,delay,nativeFallback);
        }
    }

    private void scheduleByDurationFromElapsed(final UtteranceState state,int durationMs,final long useGeneration,long elapsed){
        int length=Math.max(1,state.text.length());
        for(final Cue cue:state.cues){
            if(cue.fired)continue;
            long target=Math.max(0,Math.min(Math.max(0,durationMs-80),Math.round(durationMs*(cue.charIndex/(float)length))));
            scheduleCueTask(state,cue,useGeneration,Math.max(0L,target-elapsed),true);
        }
    }

    private void scheduleCueTask(final UtteranceState state,final Cue cue,final long useGeneration,long delay,boolean nativeFallback){
        final Runnable task=new Runnable(){public void run(){synchronized(NarrativeAudioEngine.this){
            pending.remove(this);state.fallbackTasks.remove(this);
            if(useGeneration!=generation||!narrationActive||cue.fired)return;
            fireCue(cue);
        }}};
        pending.add(task);if(nativeFallback)state.fallbackTasks.add(task);handler.postDelayed(task,Math.max(0L,delay));
    }

    private void cancelFallbackTasks(UtteranceState state){
        for(Runnable task:new ArrayList<Runnable>(state.fallbackTasks)){handler.removeCallbacks(task);pending.remove(task);}
        state.fallbackTasks.clear();
    }

    private void fireCue(Cue cue){/*BR_JSON_WRAP*/try{
        if(cue.fired||cue.queued)return;
        long now=System.currentTimeMillis();
        Long lastEvent=lastEventAt.get(cue.event);
        if(lastEvent!=null&&now-lastEvent<EVENT_COOLDOWN_MS){cue.fired=true;return;}
        long remaining=GLOBAL_MIN_GAP_MS-(now-lastSfxAt);
        if(remaining>0L){scheduleDeferred(cue,remaining+20L);return;}
        JSONObject sound=cue.preparedSound!=null?cue.preparedSound:catalog.resolveSfx(cue.event,cue.query);
        if(sound==null){cue.fired=true;say("SFX não encontrado para "+cue.event+".");return;}
        String fileId=sound.optString("id",sound.optString("uri"));
        Long lastFile=lastFileAt.get(fileId);
        if(lastFile!=null&&now-lastFile<FILE_COOLDOWN_MS){cue.fired=true;return;}
        cue.fired=true;
        lastEventAt.put(cue.event,now);lastFileAt.put(fileId,now);lastSfxAt=now;
        audio.playSfx(Uri.parse(sound.optString("uri")),sound.optString("name",cue.event),cue.gain,sound.optInt("max_duration_ms",0));
        catalog.markPlayed(sound,"sfx","sfx",cue.event);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private void scheduleDeferred(final Cue cue,long delay){
        if(cue.fired||cue.queued)return;
        final long useGeneration=generation;
        cue.queued=true;
        final Runnable task=new Runnable(){public void run(){synchronized(NarrativeAudioEngine.this){
            pending.remove(this);cue.queued=false;
            if(useGeneration!=generation||!narrationActive||cue.fired)return;
            fireCue(cue);
        }}};
        pending.add(task);handler.postDelayed(task,Math.max(20L,delay));
    }
    private void cancelCallbacksLocked(){for(Runnable task:new ArrayList<Runnable>(pending))handler.removeCallbacks(task);pending.clear();}

    private ScenePlan buildPlan(String narration,JSONObject aiPlan,JSONObject scene){
        JSONObject plan=aiPlan==null?new JSONObject():aiPlan;JSONObject state=scene==null?new JSONObject():scene;
        String combined=safe(narration)+" "+state.optString("location")+" "+state.optString("weather")+" "+state.optString("time_of_day")+" "+state.optString("danger");
        String environment=clean(plan.optString("environment"));if(environment.length()==0)environment=inferEnvironment(combined);
        String weather=clean(plan.optString("weather",state.optString("weather")));if(weather.length()==0)weather="clima neutro";
        String location=clean(plan.optString("location",state.optString("location")));if(location.length()==0)location=environment;
        String time=clean(plan.optString("time_of_day",state.optString("time_of_day")));if(time.length()==0)time="horário indefinido";
        String mood=normalizeMood(plan.optString("music_mood",plan.optString("mood")),combined);
        String danger=fold(state.optString("danger"));if(danger.length()>0&&!has(danger,"nenhum","baixo","seguro","controlado","sem perigo")&&has(mood,"calma","aventura"))mood="tensa";
        int transition=Math.max(1200,Math.min(7000,plan.optInt("transition_ms",DEFAULT_TRANSITION_MS)));
        StringBuilder hints=new StringBuilder(clean(plan.optString("soundscape_hints")));
        JSONArray layers=plan.optJSONArray("ambient_layers");if(layers!=null)for(int i=0;i<layers.length();i++){JSONObject layer=layers.optJSONObject(i);if(layer==null)continue;String query=clean(layer.optString("query"));if(query.length()==0)continue;if(hints.length()>0)hints.append(' ');hints.append(query);}
        ArrayList<ExplicitCue> cues=new ArrayList<ExplicitCue>();JSONArray a=plan.optJSONArray("cues");if(a!=null)for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c==null)continue;String event=clean(c.optString("event"));String phrase=clean(c.optString("phrase"));if(event.length()==0&&phrase.length()==0)continue;cues.add(new ExplicitCue(event.length()==0?"impact":event,phrase,c.optInt("priority",75),(float)c.optDouble("gain",.85)));}
        return new ScenePlan(environment,weather,location,time,mood,hints.toString(),transition,cues);
    }

    /** Encontra todas as ocorrências, não apenas a primeira de cada família. */
    private List<Cue> detectCues(String text,List<ExplicitCue> extra){
        if(text==null||text.trim().length()==0)return Collections.emptyList();String folded=fold(text);ArrayList<Cue> out=new ArrayList<Cue>();HashSet<String> unique=new HashSet<String>();
        for(CueFamily family:FAMILIES)for(String trigger:family.triggers){String target=fold(trigger);int from=0;while(target.length()>0&&from<folded.length()){int at=folded.indexOf(target,from);if(at<0)break;from=at+Math.max(1,target.length());if(isNegated(folded,at,target.length()))continue;if(unique.add(family.event+"@"+at))out.add(new Cue(family.event,at,family.priority,family.gain,trigger+" "+family.query));}}
        detectSemanticCues(folded,out,unique);
        if(extra!=null)for(ExplicitCue e:extra){String target=fold(e.phrase);if(target.length()==0)continue;int from=0;while(from<folded.length()){int at=folded.indexOf(target,from);if(at<0)break;from=at+Math.max(1,target.length());if(!isNegated(folded,at,target.length())&&unique.add(e.event+"@"+at))out.add(new Cue(e.event,at,e.priority,e.gain,e.phrase+" "+e.event));}}
        Collections.sort(out,new Comparator<Cue>(){public int compare(Cue a,Cue b){int c=Integer.compare(a.charIndex,b.charIndex);return c!=0?c:Integer.compare(b.priority,a.priority);}});
        ArrayList<Cue> compact=new ArrayList<Cue>();for(Cue cue:out){if(!compact.isEmpty()&&Math.abs(compact.get(compact.size()-1).charIndex-cue.charIndex)<3){if(cue.priority>compact.get(compact.size()-1).priority)compact.set(compact.size()-1,cue);}else compact.add(cue);}return compact;
    }


    /**
     * Complementa as frases exatas com relações semânticas locais. Assim,
     * "alguém bate três vezes contra a porta" e "marteladas ressoam na
     * bigorna" continuam funcionando sem depender da redação exata.
     */
    private void detectSemanticCues(String text,List<Cue> out,HashSet<String> unique){
        addSemantic(text,out,unique,"knock",98,1.00f,
                new String[]{"porta","portao","entrada"},
                new String[]{"bat","pancad","toqu","knock"},"batida na porta",false);
        addSemantic(text,out,unique,"wood_knock",96,1.00f,
                new String[]{"madeira","tabua","tábua"},
                new String[]{"bat","pancad","toqu","knock"},"batida na madeira",true);
        addSemantic(text,out,unique,"door",88,.92f,
                new String[]{"porta","portao"},
                new String[]{"abr","fech","rang","slam","creak"},"porta abrindo ou fechando",false);
        addSemantic(text,out,unique,"anvil",90,.92f,
                new String[]{"bigorna","forja","anvil"},
                new String[]{"martel","malho","hammer","bat"},"martelo na bigorna",false);
        addSemantic(text,out,unique,"cart",65,.78f,
                new String[]{"carroca","carroça","carruagem","stagecoach","carriage","wagon","cart"},
                new String[]{"aproxim","pass","rod","rang","avanc","mov"},"carroça e rodas",false);
        addSemantic(text,out,unique,"footsteps",68,.76f,
                new String[]{"passos","pisadas","footsteps","steps"},
                new String[]{"eco","aproxim","corr","caminh","ouve","soam","ritm"},"passos",false);
        addSemantic(text,out,unique,"sword",91,.98f,
                new String[]{"espada","lamina","lâmina","sword","blade","rapier","dagger","adaga","machado","axe","arma","weapon","sabre"},
                new String[]{"desembainh","rasp","choc","cort","golp","strike","draw","swing","atac","ating","brandi"},"espada e lâmina",false);
        addSemantic(text,out,unique,"metal",78,.86f,
                new String[]{"armadura","cota de malha","ferro","armor","chainmail","grade","portao","portão","trinco"},
                new String[]{"rasp","choc","tilint","rang","clang","clank","hit"},"metal e armadura",false);
        addSemantic(text,out,unique,"chains",80,.86f,
                new String[]{"corrente","correntes","grilhao","grilhão","chain","chains"},
                new String[]{"arrast","tilint","bat","rattle","drag"},"correntes",false);
        addSemantic(text,out,unique,"horse",72,.82f,
                new String[]{"cavalo","cavalos","cascos","galope","horse","hooves","gallop"},
                new String[]{"galop","relinch","bat","aproxim","pass","trovej"},"cavalo e galope",false);
        addSemantic(text,out,unique,"thunder",84,.90f,
                new String[]{"trovao","trovão","thunder"},
                new String[]{"ribomb","estour","eco","ronc","crack","rumbl"},"trovão",false);
        addSemantic(text,out,unique,"fire_crackle",48,.54f,
                new String[]{"fogo","chamas","lareira","lenha","fire","flames","fireplace"},
                new String[]{"crepit","estal","crackl"},"lareira crepitando",false);
        addSemantic(text,out,unique,"glass",95,1.00f,
                new String[]{"vidro","copo","glass"},
                new String[]{"quebr","estilhac","crash","shatter","smash"},"vidro quebrando",false);
        addSemantic(text,out,unique,"bell",72,.80f,
                new String[]{"sino","sinos","campainha","bell","bells"},
                new String[]{"toc","resso","ring","toll","chime"},"sino",false);
        addSemantic(text,out,unique,"roar",93,1.00f,
                new String[]{"rugido","rosnado","monstro","fera","roar","growl"},
                new String[]{"solt","eco","rosn","rug","growl","roar"},"rugido de monstro",false);
        addSemantic(text,out,unique,"scream",89,.96f,
                new String[]{"grito","gritos","scream","shriek"},
                new String[]{"solt","eco","dor","medo","scream","shriek"},"grito",false);
        addSemantic(text,out,unique,"rocks",64,.72f,
                new String[]{"pedra","pedras","rocha","rochas","rocks","stones"},
                new String[]{"cai","caem","rasp","desab","tumble","fall"},"pedras caindo",false);
    }

    private void addSemantic(String text,List<Cue> out,HashSet<String> unique,String event,int priority,float gain,
                             String[] anchors,String[] actions,String query,boolean rejectDoor){
        for(String anchor:anchors){
            String target=fold(anchor);int from=0;
            while(target.length()>0&&from<text.length()){
                int at=text.indexOf(target,from);if(at<0)break;from=at+Math.max(1,target.length());
                if(!wordBoundary(text,at,target.length()))continue;
                int start=Math.max(0,at-72),end=Math.min(text.length(),at+target.length()+72);
                String window=text.substring(start,end);
                if(rejectDoor&&has(window,"porta","portao"))continue;
                if("metal".equals(event)&&has(window,"espada","lamina","lâmina","sword","blade","dagger","adaga","machado","axe","weapon","arma"))continue;
                if(!hasStem(window,actions)||isNegated(text,at,target.length()))continue;
                int actionAt=firstStemPosition(window,actions);
                int cueAt=actionAt<0?at:Math.min(at,start+actionAt);
                if(unique.add(event+"@"+cueAt))out.add(new Cue(event,cueAt,priority,gain,window+" "+query));
            }
        }
    }

    private static boolean wordBoundary(String text,int at,int len){
        boolean left=at<=0||!Character.isLetterOrDigit(text.charAt(at-1));
        int end=at+len;boolean right=end>=text.length()||!Character.isLetterOrDigit(text.charAt(end));
        return left&&right;
    }

    private static boolean hasStem(String text,String[] stems){return firstStemPosition(text,stems)>=0;}
    private static int firstStemPosition(String text,String[] stems){
        String[] words=fold(text).split("[^a-z0-9]+");int cursor=0,best=-1;
        for(String word:words){
            int at=text.indexOf(word,cursor);if(at<0)at=cursor;cursor=Math.max(cursor,at+word.length());
            for(String stem:stems)if(word.startsWith(fold(stem))){if(best<0||at<best)best=at;break;}
        }
        return best;
    }

    private static boolean isNegated(String text,int at,int len){int start=Math.max(0,at-42),end=Math.min(text.length(),at+len+24);String window=text.substring(start,end);return has(window,"nao houve","não houve","sem ouvir","sem som de","nao se ouve","não se ouve","nenhuma batida","nenhum passo","silencio em vez de","silêncio em vez de");}
    private static String inferEnvironment(String text){String n=fold(text);if(has(n,"taverna","estalagem","inn","tavern"))return "interior de taverna";if(has(n,"masmorra","dungeon","catacumba"))return "masmorra subterrânea";if(has(n,"caverna","cave","gruta"))return "caverna";if(has(n,"floresta","forest","bosque"))return "floresta";if(has(n,"cidade","city","rua","mercado"))return "cidade";if(has(n,"aldeia","vila","village"))return "aldeia";if(has(n,"navio","ship","mar","oceano","porto"))return "costa e porto";if(has(n,"pantano","pântano","swamp"))return "pântano";if(has(n,"deserto","desert"))return "deserto";if(has(n,"montanha","mountain"))return "montanhas";if(has(n,"chuva","rain","tempestade"))return "exterior sob chuva";if(has(n,"sala","quarto","corredor","interior"))return "interior de construção";return "estrada e campos";}
    private static String normalizeMood(String requested,String text){String m=fold(requested);if(has(m,"none","sem musica","silencio"))m="";if(has(m,"combat","combate","battle","tensa","tension"))return "tensa";if(has(m,"mystery","misterio","misteriosa","suspense"))return "misteriosa";if(has(m,"sad","triste","melancolica","luto"))return "melancólica";if(has(m,"triumph","triunfo","heroica","heroico"))return "heroica";if(has(m,"calm","calma","peace"))return "calma";String n=fold(text);if(has(n,"combate","batalha","emboscada","perseguicao","perigo iminente","ataca"))return "tensa";if(has(n,"segredo","misterio","ruina","investiga","sombras"))return "misteriosa";if(has(n,"luto","funeral","perda","despedida"))return "melancólica";if(has(n,"vitoria","triunfo","celebram"))return "heroica";if(has(n,"descanso","seguro","paz","acolhedora"))return "calma";return "aventura";}
    private static boolean has(String text,String... values){for(String value:values)if(text.contains(fold(value)))return true;return false;}
    private static int estimateDurationMs(String text){int words=safe(text).trim().length()==0?1:safe(text).trim().split("\\s+").length;return Math.max(900,Math.round(words/2.55f*1000f));}
    private static String chunkKey(String text){return fold(safe(text));}
    private static String clean(String value){return safe(value).replaceAll("\\s+"," ").trim();}
    private static String safe(String value){return value==null?"":value;}
    private static String fold(String value){String n=Normalizer.normalize(safe(value).toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replace('’','\'').replaceAll("\\s+"," ");}
    private static String join(List<String> values,String separator){StringBuilder b=new StringBuilder();for(String value:values){if(value==null||value.length()==0)continue;if(b.length()>0)b.append(separator);b.append(value);}return b.length()==0?"nenhuma faixa compatível":b.toString();}
    private final AudioStatusGate footerStatusGate = new AudioStatusGate();
    private void say(String text){
        if(status!=null && footerStatusGate.allow(text,android.os.SystemClock.elapsedRealtime()))
            status.onStatus(text);
    }

    private static final class PreparedScene{final ScenePlan plan;final JSONObject ambience,climate,music;final int cueCount;PreparedScene(ScenePlan p,JSONObject a,JSONObject c,JSONObject m,int cues){plan=p;ambience=a;climate=c;music=m;cueCount=cues;}}
    private static final class ScenePlan{final String environment,weather,location,timeOfDay,mood,soundscapeHints;final int transitionMs;final ArrayList<ExplicitCue> explicitCues;ScenePlan(String e,String w,String l,String t,String m,String hints,int transition,ArrayList<ExplicitCue> cues){environment=e;weather=w;location=l;timeOfDay=t;mood=m;soundscapeHints=hints==null?"":hints;transitionMs=transition;explicitCues=cues;}}
    private static final class CueFamily{final String event;final int priority;final float gain;final String[] triggers;final String query;CueFamily(String e,int p,float g,String triggerPipe,String q){event=e;priority=p;gain=g;triggers=triggerPipe.split("\\|");query=q;}}
    private static final class ExplicitCue{final String event,phrase;final int priority;final float gain;ExplicitCue(String e,String p,int pri,float g){event=e;phrase=p;priority=Math.max(1,Math.min(100,pri));gain=Math.max(.15f,Math.min(1.25f,g));}}
    private static final class Cue{final String event,query;final int charIndex,priority;final float gain;boolean fired,queued;JSONObject preparedSound;Cue(String e,int at,int p,float g,String q){event=e;charIndex=at;priority=p;gain=g;query=q;}}
    private static final class UtteranceState{final String text;final List<Cue> cues;final ArrayList<Runnable> fallbackTasks=new ArrayList<Runnable>();boolean rangeSeen=false;long startedAt=0L;UtteranceState(String value,List<Cue> list){text=safe(value);cues=list==null?new ArrayList<Cue>():list;}}
}
