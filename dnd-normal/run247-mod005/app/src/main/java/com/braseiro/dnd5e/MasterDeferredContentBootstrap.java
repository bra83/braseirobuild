package com.braseiro.dnd5e;

import android.os.SystemClock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Serial background installation of optional published mechanical content.
 * The Master object and its core 2024 rules do not wait on this catalog
 * during Activity.onCreate. Statblock and supplemental registries are
 * installed once, with independent failure reporting and no skipped tail
 * when an earlier book fails.
 */
public final class MasterDeferredContentBootstrap {
    public interface Task { void install() throws Exception; }
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r -> {
        Thread t=new Thread(r,"braseiro-master-content");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private static final AtomicBoolean STARTED=new AtomicBoolean(false);
    private static volatile boolean ready;
    private static volatile int completed, failures;
    private static volatile String current="not_started";
    private MasterDeferredContentBootstrap(){}

    public static void start(final MasterOrchestrator master) {
        if(master==null || !STARTED.compareAndSet(false,true))return;
        DndStartupTrace.mark("MASTER_CONTENT_SCHEDULED");
        WORKER.execute(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                run("tasha_source",() -> TashaCompiledSourceRulesEngine.install());
                run("tasha_spell",() -> TashaSpellRuntimeEngine.install());
                run("tasha_artificer",() -> TashaArtificerRuntimeEngine.install());
                run("tasha_subclasses",() -> TashaSubclassRuntimeEngine.install());
                run("crossbook_subclasses",() -> CrossBookSubclassRuntimeEngine.install());
                run("eberron_2024_character",() -> Core2024EberronCharacterEngine.install());
                run("core2024_classes",() -> Core2024ClassRuntimeEngine.install());
                run("homebrew_classes",() -> HomebrewClassRuntimeEngine.install());
                run("homebrew_run138",() -> HomebrewClassRuntimeEngine.installRun138());
                run("homebrew_subclasses",() -> HomebrewSubclassRuntimeEngine.install());
                run("volo_handbook",() -> VoloSubraceHandbookEngine.install());
                run("volo_139",() -> VoloSubraceRun139Engine.install());
                run("volo_140",() -> VoloSubraceRun140Engine.install());
                run("volo_racial_feats",() -> VoloRacialFeatRuntimeEngine.install());
                run("run141_species",() -> Run141SpeciesFeatRuntimeEngine.install());
                run("run141_compact",() -> Run141CompactCharacterOptionEngine.install());
                run("run143_secrets",() -> Run143ForgottenSecretsEngine.install());
                run("run149_spells",() -> Run149ForgottenSecretsSpellEngine.install());
                run("eberron_forge",() -> EberronForgeCompleteEngine.install());
                run("forgotten_realms_heroes",() -> ForgottenRealmsHeroesEngine.install());
                run("runeterra_classes",() -> RuneterraClassRuntimeEngine.install());
                run("arcana_143",() -> ArcanaUnleashedRun143Engine.install());
                run("arcana_144",() -> ArcanaUnleashedRun144Engine.install());
                run("arcana_145_items",() -> ArcanaUnleashedRun145ItemEngine.install());
                run("arcana_145_bestiary",() -> ArcanaUnleashedRun145BestiaryEngine.install());
                run("ravenloft_bestiary",() -> RavenloftBestiaryRuntimeEngine.install());
                run("lorwyn_bestiary",() -> LorwynBestiaryRuntimeEngine.install());
                run("arcana_147_items",() -> ArcanaUnleashedRun147EvolvingItemsEngine.install());
                run("tasha_items",() -> TashaItemRuntimeEngine.install());
                run("tasha_artifacts",() -> TashaArtifactRuntimeEngine.install());
                run("tasha_supplemental",() -> TashaSupplementalRuntimeEngine.install());
                DndStartupTrace.mark("MASTER_RULES_REGISTERED");
                run("tasha_feats",() -> TashaFeatCompiledRulesEngine.install());
                run("tasha_mimic",() -> TashaMimicColonyEngine.installStatblocks());
                run("srd_monsters",() -> master.installSrd51BestiaryBatch());
                DndStartupTrace.mark("MASTER_MONSTERS_REGISTERED");
            } finally {
                ready=failures==0;
                current=ready?"ready":"partial_failure";
                DndStartupTrace.mark(ready?"MASTER_CONTENT_READY":"MASTER_CONTENT_FAILED");
            }
        });
    }

    private static void run(String id,Task task) {
        current=id;
        long before=SystemClock.elapsedRealtime();
        try { task.install();completed++; }
        catch(Throwable error){
            failures++;
            AppLog.e("MasterContent","Falha instalando conteudo mecanico "+id,error);
        } finally {
            long duration=SystemClock.elapsedRealtime()-before;
            android.util.Log.i("MasterContent","module="+id+" duration_ms="+duration);
        }
    }

    public static boolean isReady(){return ready;}
    public static boolean isStarted(){return STARTED.get();}
    public static int completed(){return completed;}
    public static int failures(){return failures;}
    public static String current(){return current;}
}
