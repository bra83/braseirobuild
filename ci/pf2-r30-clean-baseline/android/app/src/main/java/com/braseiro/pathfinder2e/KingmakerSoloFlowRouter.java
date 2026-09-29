package com.braseiro.pathfinder2e;

/**
 * Contrato único entre adventure_guide.next_scene e as superfícies jogáveis.
 *
 * Esta classe só traduz intenção de navegação. Ela não lê nem altera estado,
 * não rola dados e não resolve regras; a autoridade continua nos motores e no
 * repositório Kingmaker existentes.
 */
public final class KingmakerSoloFlowRouter {
    public static final String START_ADVENTURE = "start_adventure";
    public static final String CAMP_ENCOUNTER = "camp_encounter";
    public static final String CAMP = "camp";
    public static final String WAR = "war";
    public static final String EXPLORATION_RESOLUTION = "exploration_resolution";
    public static final String COMPANION = "companion";
    public static final String KINGDOM_BRIDGE = "kingdom_bridge";
    public static final String TRAVEL = "travel";
    public static final String FREE_EXPLORATION = "free_exploration";
    public static final String MASTER_FALLBACK = "master_fallback";

    private KingmakerSoloFlowRouter() {}

    public static String actionFor(String nextScene) {
        String scene = nextScene == null ? "" : nextScene.trim();
        if ("adventure_start".equals(scene)) return START_ADVENTURE;
        if ("camp_encounter".equals(scene)) return CAMP_ENCOUNTER;
        if ("camp".equals(scene)) return CAMP;
        if ("war".equals(scene)) return WAR;
        if ("exploration_resolution".equals(scene)) return EXPLORATION_RESOLUTION;
        if ("companion".equals(scene)) return COMPANION;
        if ("kingdom_bridge".equals(scene)) return KINGDOM_BRIDGE;
        if ("travel".equals(scene)) return TRAVEL;
        if ("free_exploration".equals(scene)) return FREE_EXPLORATION;
        return MASTER_FALLBACK;
    }
}
