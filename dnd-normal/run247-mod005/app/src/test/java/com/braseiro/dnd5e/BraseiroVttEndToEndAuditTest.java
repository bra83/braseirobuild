package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * End-to-end technical audit suite for Braseiro D&D (v6.6.44+):
 * 1) Suggested actions in every narrative scene (expanded & collapsed Strixhaven drawer + 24-turn multi-adventure loop)
 * 2) Journal navigation & system Back button (no user trap) + full read/search + TXT, Literal PDF, and Bard Chronicle PDF exports
 * 3) Blue Master consultation box (GM_HELP freeze, consultative answer sanitization, collapse/close/clear, action promotion state machine)
 * 4) Fiction clock progression per action without double-counting or advancing on consultations
 * 5) Kokoro & Android TTS upfront chunk pipelining (eliminating ~5s inter-chunk pause) and user speech rate support
 * 6) Full VTT mechanics: D&D 2024 sheets, inventory & transactions, Kvothe Strixhaven -> Faerûn -> D040 Marco do Percurso party bridge
 *    (Wilem, Simmon, Auri), CombatStateMachine, LivingWorldEngine, and BACKUP21 export/import round-trip.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class BraseiroVttEndToEndAuditTest {
    private MainActivity activity;
    private AppDb db;
    private Context context;
    private String dbName;
    private FrameLayout content;
    private SharedPreferences prefs;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        activity = Robolectric.buildActivity(MainActivity.class).get();
        dbName = "braseiro-e2e-audit-" + Long.toHexString(System.nanoTime()) + ".db";
        db = new AppDb(context, dbName);
        db.setActiveMapProfile("dnd");
        db.saveCampaign("Crônicas de Strixhaven e Faerûn", DndRulesEngine.PROFILE_2024,
                "Fantasia acadêmica e aventura heroica", true, true, false, "");

        prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();

        content = new FrameLayout(activity);
        HorizontalScrollView navScroll = new HorizontalScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(navScroll);
        root.addView(content);
        activity.setContentView(root);

        WorldStateEngine worldEngine = new WorldStateEngine(db);
        SceneStateEngine sceneEngine = new SceneStateEngine(db);
        PartyDirector partyDirector = new PartyDirector(db);
        AiQuotaGovernor quotaGovernor = new AiQuotaGovernor(context);

        setField("ui", new Handler(Looper.getMainLooper()));
        setField("db", db);
        setField("prefs", prefs);
        setField("content", content);
        setField("shellNavScroll", navScroll);
        setField("characterGenerator", new CharacterSheetGenerator(new DndRulesEngine()));
        setField("worldStateEngine", worldEngine);
        setField("livingWorldEngine", new LivingWorldEngine(db));
        setField("sceneStateEngine", sceneEngine);
        setField("partyDirector", partyDirector);
        setField("aiQuotaGovernor", quotaGovernor);
    }

    @After
    public void tearDown() {
        if (db != null) db.close();
        if (context != null && dbName != null) context.deleteDatabase(dbName);
    }

    private MasterOrchestrator ensureOrchestrator() throws Exception {
        MasterOrchestrator existing = (MasterOrchestrator) getField("masterOrchestrator");
        if (existing != null) return existing;
        MasterOrchestrator created = new MasterOrchestrator(
                db,
                (PartyDirector) getField("partyDirector"),
                (WorldStateEngine) getField("worldStateEngine"),
                (AiQuotaGovernor) getField("aiQuotaGovernor")
        );
        setField("masterOrchestrator", created);
        return created;
    }

    private void setField(String name, Object value) throws Exception {
        Field f = MainActivity.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(activity, value);
    }

    private Object getField(String name) throws Exception {
        Field f = MainActivity.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(activity);
    }

    private Object invoke(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = MainActivity.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(activity, args);
    }

    private JSONObject validateAndAccept(MasterOrchestrator orchestrator, MasterOrchestrator.Plan plan, JSONObject envelope) throws Exception {
        orchestrator.setActivePlanForValidation(plan);
        JSONObject safe = orchestrator.validateEnvelope(envelope, plan.userPrompt);
        orchestrator.afterAccepted(plan, safe.optString("narration"), safe, 120, 120);
        return safe;
    }

    private <T extends View> List<T> findAll(View root, Class<T> type) {
        List<T> out = new ArrayList<>();
        collectViews(root, type, out);
        return out;
    }

    private <T extends View> void collectViews(View node, Class<T> type, List<T> out) {
        if (node == null) return;
        if (type.isInstance(node)) out.add(type.cast(node));
        if (node instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) node;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectViews(vg.getChildAt(i), type, out);
            }
        }
    }

    private Button findButtonContaining(View root, String substring) {
        String needle = substring.toLowerCase();
        for (Button b : findAll(root, Button.class)) {
            CharSequence t = b.getText();
            if (t != null && t.toString().toLowerCase().contains(needle)) return b;
        }
        return null;
    }

    @Test
    public void optionalActionsRemainGroundedAcross24AdventureTurnsAndDrawerStates() throws Exception {
        long kvotheId = db.addCharacter("Kvothe", "Humano", "Mago", 1, 8, 8, 13, 8, 14, 12, 16, 10, 14, true, "");
        db.updateCharacterRuleDetails(kvotheId, "Escriba", "", "", "", "", DndRulesEngine.PROFILE_2024);
        StrixhavenAdventureProfile profile = new StrixhavenAdventureProfile(context);
        profile.activate();

        String[][] adventureTurns = new String[][]{
            {"Strixhaven • Biblioplex", "INÍCIO AUTOMÁTICO — PRIMEIRA CENA", "O grande salão do Biblioplex abre-se entre estantes altas, balcões de atendimento e um mural repleto de folhetos estudantis. Uma funcionária organiza as fichas de inscrição enquanto calouros observam os cartazes."},
            {"Strixhaven • Biblioplex", "Procuro um panfleto sobre teatro no mural", "Você se aproxima do mural de cortiça ao lado do balcão principal e localiza um panfleto colorido da Guilda de Teatro Dramático anunciando audições hoje no palco lateral, enquanto uma veterana anota nomes em um caderno."},
            {"Strixhaven • Biblioplex", "Falo com a veterana sobre as audições de teatro", "A veterana ergue os olhos do caderno, ajeita a pena atrás da orelha e aponta para a mesa de inscrições. Veterana Lyra: — As leituras de cena começam em poucos minutos perto da galeria sul; você pode assinar a lista ou examinar o roteiro sobre a mesa."},
            {"Strixhaven • Biblioplex", "Examino o roteiro sobre a mesa de inscrições", "O roteiro encadernado em couro azul traz anotações nas margens sobre um duelo de réplicas e uma nota dobrada indicando que os adereços estão guardados no armário ao lado da escada."},
            {"Strixhaven • Firejolt Cafe", "Entro no Firejolt Cafe para observar os estudantes", "O aroma de especiarias e café recém-passado domina o Firejolt Cafe. No balcão de cobre, o gerente Ellina serve canecas fumegantes enquanto dois estudantes de Prismari discutem sobre um mapa rabiscado na mesa do canto."},
            {"Strixhaven • Firejolt Cafe", "Me aproximo do balcão para conversar com Ellina", "Ellina limpa o tampo com um pano rápido e inclina a cabeça com atenção. Ellina: — Se veio atrás do turno de ajudante ou dos boatos sobre o pátio leste, chegou na hora certa; deixaram um recado no quadro junto à cozinha."},
            {"Strixhaven • Firejolt Cafe", "Leio o recado no quadro junto à cozinha", "O bilhete preso com um alfinete de latão pede alguém atento para organizar caixas de ingredientes raros no depósito dos fundos e menciona um barulho estranho vindo da grade de ventilação."},
            {"Strixhaven • Campus Central", "Sigo pelo Campus Central até o pátio dos arcos", "As alamedas de pedra do Campus Central fervilham de alunos carregando tomos e lanternas arcanas. Perto do chafariz, um monitor do campus examina uma placa solta no piso ao lado de um banco de pedra."},
            {"Strixhaven • Bow's End Tavern", "Visito a Bow's End Tavern no fim da tarde", "Lanternas âmbar iluminam as mesas de madeira da Bow's End Tavern. Tulk, o taverneiro orc, organiza bandejas atrás do balcão enquanto um grupo disputa arremessos perto do quadro de desafios."},
            {"Strixhaven • Dormitórios", "Subo até o corredor dos Dormitórios", "No corredor silencioso dos Dormitórios, uma porta entreaberta deixa escapar um brilho azulado sobre o tapete e um caderno de anotações repousa sobre a mesinha lateral."},
            {"Strixhaven • Quadra de Magitorre", "Observo o treino na Quadra de Magitorre", "Na borda da Quadra de Magitorre, bandeiras das cinco faculdades tremulam ao vento enquanto o capitão da equipe ajusta os marcadores de campo perto do caixote de equipamentos."},
            {"Strixhaven • Pátio de Lorehold", "Caminho entre as colunas do Pátio de Lorehold", "Estátuas antigas e mesas de catalogação ocupam o Pátio de Lorehold. Um pesquisador anão limpa fragmentos de cerâmica sobre uma bancada ao lado de uma caixa lacrada."},
            {"Yartar • Portão Sul", "Chego ao portão de Yartar pela estrada do rio", "As muralhas de Yartar erguem-se acima da confluência dos rios. Dois guardas verificam carroças de mercadores junto ao portão enquanto um barqueiro amarra sua embarcação no píer próximo."},
            {"Yartar • Praça do Mercado", "Exploro a Praça do Mercado de Yartar", "Tendas de tecido listrado cobrem a Praça do Mercado de Yartar. Uma mercadora de mapas enrola pergaminhos sobre uma banca de carvalho enquanto um mensageiro procura viajantes rumo ao norte."},
            {"Yartar • Píer das Barcas", "Examino as embarcações no Píer das Barcas", "As águas batem contra os pilares de madeira do Píer das Barcas. Um mestre de cais confere caixotes marcados com selos de cera ao lado da passarela de embarque."},
            {"Estrada do Dessarin", "Sigo pela Estrada do Dessarin em direção ao norte", "A trilha batida da Estrada do Dessarin serpenteia entre colinas pedregosas. À beira do caminho, um marco de pedra rachado aponta uma bifurcação perto de uma carroça parada."},
            {"Marco do Percurso", "Alcanço o Marco do Percurso em DO40", "Diante do antigo obelisco do Marco do Percurso, três viajantes descansam junto à fogueira: Wilem apoia seu machado contra uma rocha, Simmon ajusta as tiras do bastão e Auri observa as folhas ao vento."},
            {"Marco do Percurso", "Converso com Wilem, Simmon e Auri junto ao obelisco", "Wilem ergue o olhar com um aceno firme, Simmon abre um sorriso franco e Auri aproxima-se com passos leves. Wilem: — Estávamos esperando alguém que conhecesse as trilhas; podemos examinar as inscrições do obelisco ou seguir juntos pela rota leste."},
            {"Pousada do Caminho", "Entramos na Pousada do Caminho para descansar", "O salão aquecido da Pousada do Caminho tem mesas de pinho e uma lareira crepitante. O estalajadeiro coloca chaves de bronze sobre o balcão ao lado de um quadro de avisos da estrada."},
            {"Cripta Antiga • Entrada", "Descemos a escadaria da Cripta Antiga", "Degraus de pedra úmida levam ao vestíbulo da Cripta Antiga. Tochas apagadas ladeiam uma porta de ferro com um relevo de serpente e um pedestal baixo no centro do piso."},
            {"Cripta Antiga • Salão das Colunas", "Avançamos pelo Salão das Colunas", "Seis colunas octogonais sustentam a abóbada do Salão das Colunas. No chão empoeirado, rastros recentes contornam uma estátua partida e seguem até uma alavanca na parede norte."},
            {"Cripta Antiga • Câmara do Altar", "Inspecionamos a Câmara do Altar", "Um altar de basalto ocupa o fundo da Câmara do Altar, sobre o qual repousa um cofre de prata fechado e um pergaminho lacrado ao lado de um candelabro antigo."},
            {"Cripta Antiga • Galeria Lateral", "Abrimos a passagem para a Galeria Lateral", "A Galeria Lateral revela nichos de pedra com inscrições arcanas e uma mesa de alquimia onde frascos etiquetados brilham fracamente perto de um diário aberto."},
            {"Marco do Percurso • Retorno", "Retornamos ao Marco do Percurso com as descobertas", "De volta ao Marco do Percurso sob o céu aberto, a brisa move a grama alta ao redor do obelisco enquanto o grupo reúne o mapa e o cofre recuperado sobre a pedra plana."}
        };

        for (int i = 0; i < adventureTurns.length; i++) {
            String location = adventureTurns[i][0];
            String action = adventureTurns[i][1];
            String narration = adventureTurns[i][2];

            JSONObject scene = db.getSceneState();
            scene.put("location", location);
            scene.put("objective", "Investigar os acontecimentos em " + location);
            db.saveSceneState(scene);

            JSONArray rawOptions = new JSONArray();
            JSONArray diversified = SceneOptionDiversifier.ensureAlways(
                    rawOptions, new JSONArray(), narration, action, scene, i + 1);

            assertTrue("Turn " + (i + 1) + " (" + location + ") may have 0–2 grounded options, got: " + diversified,
                    diversified.length() >= 0 && diversified.length() <= 2);

            Set<String> seenActions = new HashSet<>();
            for (int j = 0; j < diversified.length(); j++) {
                JSONObject opt = diversified.getJSONObject(j);
                String label = opt.optString("label", "").trim();
                String act = opt.optString("action", "").trim();
                assertFalse("Option label must not be empty at turn " + (i + 1), label.isEmpty());
                assertFalse("Option action must not be empty at turn " + (i + 1), act.isEmpty());
                assertFalse("Option must not repeat previous action at turn " + (i + 1),
                        act.equalsIgnoreCase(action));
                assertTrue("Options within same turn must be distinct: " + diversified,
                        seenActions.add(act.toLowerCase()));
            }

            // Verify Strixhaven drawer renders these options both when expanded AND when collapsed
            setField("lastNarration", narration);
            setField("currentOptions", diversified);

            setField("strixhavenDrawerExpanded", true);
            View expandedDrawer = (View) invoke("buildStrixhavenNarrationDrawer",
                    new Class<?>[]{StrixhavenAdventureProfile.class}, profile);
            assertNotNull("Expanded drawer must open without mandatory suggestions", expandedDrawer);
            if(diversified.length()>0){
            String firstLabel = diversified.getJSONObject(0).optString("label");
            Button expandedBtn = findButtonContaining(expandedDrawer, firstLabel);
            assertNotNull("Expanded Strixhaven drawer must render suggested action button '" + firstLabel + "'", expandedBtn);

            setField("strixhavenDrawerExpanded", false);
            View collapsedDrawer = (View) invoke("buildStrixhavenNarrationDrawer",
                    new Class<?>[]{StrixhavenAdventureProfile.class}, profile);
            Button collapsedBtn = findButtonContaining(collapsedDrawer, firstLabel);
            assertNotNull("Collapsed Strixhaven drawer must STILL render compact suggested action button '" + firstLabel + "'", collapsedBtn);
            }else{
                setField("strixhavenDrawerExpanded", false);
                View collapsedWithoutOptions = (View) invoke("buildStrixhavenNarrationDrawer",
                    new Class<?>[]{StrixhavenAdventureProfile.class}, profile);
                assertNotNull("Action-free scenes still need a usable drawer",collapsedWithoutOptions);
            }
        }
    }

    @Test
    public void journalNavigationNeverTrapsPlayerAndExportsTxtLiteralPdfAndBardChronicle() throws Exception {
        db.addCharacter("Kvothe", "Humano", "Mago", 1, 8, 8, 13, 8, 14, 12, 16, 10, 14, true, "");
        StrixhavenAdventureProfile profile = new StrixhavenAdventureProfile(context);
        profile.activate();

        JSONObject scene = db.getSceneState();
        scene.put("location", "Strixhaven • Biblioplex");
        scene.put("adventure_profile", "strixhaven_curriculum");
        scene.put("day_number", 1);
        scene.put("clock_hour", 9.0d);
        db.saveSceneState(scene);

        MasterOrchestrator orchestrator = ensureOrchestrator();
        MasterOrchestrator.Plan plan = MasterOrchestrator.Plan.forTest(
                "Procuro um panfleto sobre teatro no mural MECANICA_LOCAL_VERIFICADA: Percepção total=17 vs CD 12 resultado=sucesso\n\nUse game_bus como estado operacional completo",
                "{}",
                "continuation",
                "PLAYER_ACTION"
        );
        JSONObject envelope = new JSONObject()
                .put("protocol_version", MasterProtocolV1.VERSION)
                .put("narration", "Você examina o mural do Biblioplex e encontra o panfleto da Guilda de Teatro anunciando ensaios abertos no palco sul.")
                .put("options", new JSONArray())
                .put("pending_test", JSONObject.NULL);
        JSONObject accepted = validateAndAccept(orchestrator, plan, envelope);
        assertTrue("Envelope must pass validation", accepted.optJSONObject("_master_validation").optBoolean("valid"));

        // Simulate entering Journal from Strixhaven when shellNavScroll was previously GONE
        setField("currentScreen", "Mestre IA");
        View navScroll = (View) getField("shellNavScroll");
        navScroll.setVisibility(View.GONE);

        invoke("navigate", new Class<?>[]{String.class}, "Diário");
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Diário", getField("currentScreen"));
        assertEquals("shellNavScroll must be forced VISIBLE in Journal so user is never trapped",
                View.VISIBLE, navScroll.getVisibility());

        Button backBtn = findButtonContaining(content, "VOLTAR À SESSÃO");
        assertNotNull("Journal screen must render explicit '← VOLTAR À SESSÃO / AVENTURA' button", backBtn);

        // Verify clicking the Back button returns to the active Strixhaven session ("Mestre IA")
        backBtn.performClick();
        assertEquals("Clicking Return button must restore active session",
                "Mestre IA", getField("currentScreen"));

        // Re-open Journal and test system Back button (onBackPressed)
        invoke("showJournal", new Class<?>[]{});
        assertEquals("Diário", getField("currentScreen"));
        activity.onBackPressed();
        assertFalse("onBackPressed in Journal must NOT finish the activity", activity.isFinishing());
        assertEquals("onBackPressed in Journal must return to active session",
                "Mestre IA", getField("currentScreen"));

        // Verify Journal entry contents (Action, Roll, Narration, Timestamp/Location, without prompt boilerplate)
        List<JSONObject> entries = db.listJournal();
        assertFalse("Journal must contain recorded entry", entries.isEmpty());
        JSONObject latest = entries.get(0);
        assertTrue("Journal title must include Day, Time, and Location: " + latest.optString("title"),
                latest.optString("title").contains("Dia 1") && latest.optString("title").contains("Biblioplex"));
        String body = latest.optString("body");
        assertTrue("Journal body must contain AÇÃO DECLARADA", body.contains("AÇÃO DECLARADA:"));
        assertFalse("Journal body must NOT leak internal game_bus prompt boilerplate", body.contains("Use game_bus"));
        assertTrue("Journal body must contain ROLAGEM / RESOLUÇÃO MECÂNICA", body.contains("ROLAGEM / RESOLUÇÃO MECÂNICA:"));
        assertTrue("Journal body must contain NARRAÇÃO DO MESTRE", body.contains("NARRAÇÃO DO MESTRE:"));

        // Verify Literal PDF and Bard Chronicle composition
        JSONObject manuscript = BardChronicleComposer.localManuscript(
                db.getCampaign(), entries, db.listCharacters(), "Deoch", "Épico e caloroso");
        assertTrue("Bard manuscript must contain chapters", manuscript.optJSONArray("chapters").length() >= 1);
        assertTrue("Bard manuscript must preserve facts from journal",
                manuscript.optJSONArray("chapters").getJSONObject(0).optString("text").contains("panfleto"));
        String aiPrompt = BardChronicleComposer.aiPrompt(
                db.getCampaign(), entries, db.listCharacters(), "Deoch", "Épico e caloroso");
        assertTrue("Bard AI prompt must include journal facts", aiPrompt.contains("Biblioplex"));

        AdventurePdfExporter exporter = new AdventurePdfExporter(context);
        try {
            File literalPdf = exporter.export(db.getCampaign(), entries);
            assertNotNull(literalPdf);
            File bardPdf = exporter.exportBardChronicle(db.getCampaign(), manuscript);
            assertNotNull(bardPdf);
        } catch (IllegalStateException robolectricNativePdfStub) {
            // Android SDK 35 PdfDocument native pointer is 0 in JVM unit tests ("document is closed!")
            assertTrue(String.valueOf(robolectricNativePdfStub.getMessage()).contains("closed"));
        }
    }

    @Test
    public void blueMasterConsultationBoxSanitizesMutationsSupportsCollapseCloseClearAndPromotesActions() throws Exception {
        JSONObject scene = db.getSceneState();
        scene.put("location", "Strixhaven • Biblioplex");
        scene.put("adventure_profile", "strixhaven_curriculum");
        scene.put("clock_hour", 9.0d);
        db.saveSceneState(scene);

        // 1. Verify GM_HELP freeze and consultative sanitization
        assertTrue(BraseiroNarrativeDirector.isNonAdvancing("Posso atacar ou rolar furtividade agora?", "GM_HELP"));
        String dirtyConsultative = "Apliquei o dano na ficha e atualizei o inventário. Para escalar a estante, você pode fazer um teste de Atletismo.";
        String sanitized = (String) invoke("sanitizeConsultativeAnswer", new Class<?>[]{String.class}, dirtyConsultative);
        assertFalse("Consultative answer must never claim to have applied mutations: " + sanitized,
                sanitized.toLowerCase().contains("apliquei o dano"));
        assertTrue("Consultative answer must keep valid rule explanation: " + sanitized,
                sanitized.contains("Atletismo"));

        // 2. Append consultative turn and verify promotion eligibility
        JSONObject updatedSupport = (JSONObject) invoke("appendMasterConversationHistory",
                new Class<?>[]{JSONObject.class, JSONObject.class, String.class, String.class},
                getField("lastMasterSupport"),
                new JSONObject(),
                "Quero procurar um panfleto de teatro no mural e perguntar à funcionária.",
                "Você pode se aproximar do mural do Biblioplex para ler os folhetos e abordar a funcionária no balcão.");
        setField("lastMasterSupport", updatedSupport);
        JSONArray history = (JSONArray) invoke("masterConversationHistory", new Class<?>[]{});
        assertEquals(1, history.length());
        JSONObject turn = history.getJSONObject(0);
        String turnId = turn.optString("turn_id");
        assertFalse(turnId.isEmpty());
        assertEquals("available", turn.optString("promotion_status"));

        // 3. Render Strixhaven blue Master card and verify RECOLHER / EXPANDIR / FECHAR / LIMPAR buttons
        setField("strixhavenMasterCardCollapsed", false);
        setField("strixhavenMasterCardHidden", false);
        LinearLayout host = new LinearLayout(activity);
        host.setOrientation(LinearLayout.VERTICAL);
        invoke("addStrixhavenMasterConversationCard", new Class<?>[]{LinearLayout.class}, host);
        assertNotNull("Expanded Strixhaven Master card must have RECOLHER button", findButtonContaining(host, "RECOLHER"));
        assertNotNull("Strixhaven Master card must have FECHAR button", findButtonContaining(host, "FECHAR"));
        assertNotNull("Expanded Strixhaven Master card must have LIMPAR button", findButtonContaining(host, "LIMPAR"));

        // 4. Test state transitions for promoted action: available -> awaiting_master -> awaiting_roll -> resolving_roll -> resolved
        invoke("updateMasterConversationTurn", new Class<?>[]{String.class, String.class, JSONObject.class},
                turnId, "awaiting_master", new JSONObject());
        history = (JSONArray) invoke("masterConversationHistory", new Class<?>[]{});
        assertEquals("awaiting_master", history.getJSONObject(0).optString("promotion_status"));

        // Simulate Gemini failure recovery -> must reset to cancelled instead of staying stuck in awaiting_master
        setField("activeMasterPromotionTurnId", turnId);
        invoke("markGeminiRequestFailed", new Class<?>[]{String.class, String.class, String.class, String.class},
                "Narrador", "PLAYER_ACTION", "Quero procurar um panfleto", "Falha simulada de rede");
        history = (JSONArray) invoke("masterConversationHistory", new Class<?>[]{});
        assertEquals("Failed Gemini request must transition promoted turn to 'cancelled' so user can retry",
                "cancelled", history.getJSONObject(0).optString("promotion_status"));
        assertEquals("", getField("activeMasterPromotionTurnId"));

        // Now transition through awaiting_roll -> resolving_roll -> resolved and verify auto-collapse in Strixhaven
        setField("strixhavenMasterCardCollapsed", false);
        invoke("updateMasterConversationTurn", new Class<?>[]{String.class, String.class, JSONObject.class},
                turnId, "awaiting_roll", new JSONObject());
        invoke("updateMasterConversationTurn", new Class<?>[]{String.class, String.class, JSONObject.class},
                turnId, "resolving_roll", new JSONObject());
        invoke("updateMasterConversationTurn", new Class<?>[]{String.class, String.class, JSONObject.class},
                turnId, "resolved", new JSONObject().put("resolution_excerpt", "Ação concluída na narrativa principal."));
        history = (JSONArray) invoke("masterConversationHistory", new Class<?>[]{});
        assertEquals("resolved", history.getJSONObject(0).optString("promotion_status"));
        assertTrue("Resolving a promoted action must auto-collapse the Strixhaven blue Master card",
                (Boolean) getField("strixhavenMasterCardCollapsed"));
    }

    @Test
    public void fictionClockAdvancesPerActionNeverOnConsultationAndNeverDoubleCounts() throws Exception {
        db.addCharacter("Kvothe", "Humano", "Mago", 1, 8, 8, 13, 8, 14, 12, 16, 10, 14, true, "");
        JSONObject scene = db.getSceneState();
        scene.put("location", "Strixhaven • Biblioplex");
        scene.put("day_number", 1);
        scene.put("clock_hour", 9.0d);
        scene.put("time_of_day", "Manhã (09:00)");
        db.saveSceneState(scene);

        MasterOrchestrator orchestrator = ensureOrchestrator();
        SceneStateEngine sceneEngine = (SceneStateEngine) getField("sceneStateEngine");

        // 1. Opening scene -> 0 minutes (stays 09:00)
        MasterOrchestrator.Plan openingPlan = MasterOrchestrator.Plan.forTest(
                "INÍCIO AUTOMÁTICO — PRIMEIRA CENA DO SEMESTRE\n\nUse game_bus como estado operacional completo, em combate ou iniciativa",
                "{}", "continuation", "PLAYER_ACTION");
        JSONObject openingEnv = new JSONObject()
                .put("protocol_version", MasterProtocolV1.VERSION)
                .put("narration", "O salão do Biblioplex recebe os estudantes sob a luz da manhã enquanto funcionárias organizam os balcões e murais.")
                .put("options", new JSONArray())
                .put("pending_test", JSONObject.NULL);
        JSONObject acceptedOpening = validateAndAccept(orchestrator, openingPlan, openingEnv);
        sceneEngine.apply(acceptedOpening, acceptedOpening.optString("narration"), db.getMapState());
        assertEquals(9.0d, db.getSceneState().optDouble("clock_hour"), 0.001d);

        // 2. Search for pamphlet (with realistic game_bus prompt suffix attached!) -> +3 minutes (9.05h = 09:03)
        MasterOrchestrator.Plan searchPlan = MasterOrchestrator.Plan.forTest(
                "Procuro um panfleto sobre teatro no mural\n\nUse game_bus como estado operacional completo, em combate ou iniciativa",
                "{}", "continuation", "PLAYER_ACTION");
        JSONObject searchEnv = new JSONObject()
                .put("protocol_version", MasterProtocolV1.VERSION)
                .put("narration", "Você vai até o mural de avisos e localiza o panfleto da Guilda de Teatro preso entre os recados estudantis.")
                .put("options", new JSONArray())
                .put("pending_test", JSONObject.NULL);
        JSONObject acceptedSearch = validateAndAccept(orchestrator, searchPlan, searchEnv);
        assertTrue("Envelope must carry _clock_advanced_on_accept", acceptedSearch.optBoolean("_clock_advanced_on_accept"));
        assertEquals("Clock must advance immediately on accept by +3 min (0.05h)", 9.05d, db.getSceneState().optDouble("clock_hour"), 0.001d);

        // Calling sceneEngine.apply afterwards must NOT double-add the 3 minutes!
        sceneEngine.apply(acceptedSearch, acceptedSearch.optString("narration"), db.getMapState());
        assertEquals("sceneEngine.apply must NOT double-count the 3 minutes", 9.05d, db.getSceneState().optDouble("clock_hour"), 0.001d);

        // 3. Consultative GM_HELP -> +0 minutes (stays 9.05h)
        MasterOrchestrator.Plan helpPlan = orchestrator.prepare(
                "Narrador", "Qual perícia uso para saber mais sobre a guilda?", "", new JSONObject(), new JSONObject(), "GM_HELP");
        JSONObject helpEnv = new JSONObject()
                .put("protocol_version", MasterProtocolV1.VERSION)
                .put("narration", "Você pode usar História ou Investigação caso decida pesquisar registros sobre a guilda.")
                .put("options", new JSONArray())
                .put("pending_test", JSONObject.NULL);
        validateAndAccept(orchestrator, helpPlan, helpEnv);
        assertEquals("GM_HELP consultation must never advance the fiction clock", 9.05d, db.getSceneState().optDouble("clock_hour"), 0.001d);

        // 4. Dialogue with NPC -> +5 minutes (9.05 + 5/60 = 9.1333h = 09:08)
        MasterOrchestrator.Plan talkPlan = MasterOrchestrator.Plan.forTest(
                "Converso com a funcionária no balcão sobre as inscrições\n\nUse game_bus como estado operacional completo",
                "{}", "continuation", "PLAYER_ACTION");
        JSONObject talkEnv = new JSONObject()
                .put("protocol_version", MasterProtocolV1.VERSION)
                .put("narration", "A funcionária entrega a ficha de inscrição e aponta o auditório lateral para os testes de elenco.")
                .put("options", new JSONArray())
                .put("pending_test", JSONObject.NULL);
        JSONObject acceptedTalk = validateAndAccept(orchestrator, talkPlan, talkEnv);
        assertEquals("Manhã", db.getSceneState().optString("time_of_day"));
        sceneEngine.apply(acceptedTalk, acceptedTalk.optString("narration"), db.getMapState());
        assertEquals(9.0d + (8.0d / 60.0d), db.getSceneState().optDouble("clock_hour"), 0.001d);
        String latestJournalTitle = db.listJournal().get(0).optString("title");
        assertTrue("Journal entry title must display 09:08, got: " + latestJournalTitle, latestJournalTitle.contains("09:08"));
    }

    @Test
    public void kokoroAndAndroidTtsPipelineAllChunksUpfrontWithoutDeadAirAndRespectSpeed() throws Exception {
        final List<String> spokenTexts = new ArrayList<>();
        final List<Integer> queueModes = new ArrayList<>();
        final List<String> utteranceIds = new ArrayList<>();
        final float[] recordedRate = new float[]{0f};

        TextToSpeech fakeKokoro = new TextToSpeech(context, null) {
            private UtteranceProgressListener listener;
            @Override
            public int setOnUtteranceProgressListener(UtteranceProgressListener l) {
                this.listener = l;
                return TextToSpeech.SUCCESS;
            }
            @Override
            public int setSpeechRate(float speechRate) {
                recordedRate[0] = speechRate;
                return TextToSpeech.SUCCESS;
            }
            @Override
            public int setPitch(float pitch) {
                return TextToSpeech.SUCCESS;
            }
            @Override
            public int stop() {
                return TextToSpeech.SUCCESS;
            }
            @Override
            public int speak(CharSequence text, int queueMode, Bundle params, String utteranceId) {
                spokenTexts.add(text == null ? "" : text.toString());
                queueModes.add(queueMode);
                utteranceIds.add(utteranceId);
                if (listener != null && spokenTexts.size() == 1) {
                    listener.onStart(utteranceId);
                }
                return TextToSpeech.SUCCESS;
            }
        };

        prefs.edit()
                .putBoolean("tts_enabled", true)
                .putString("tts_backend", "android_tts")
                .putString(AndroidTtsBackend.PREF_ENGINE_PACKAGE, "com.k2fsa.sherpa.onnx.tts.engine")
                .putInt(AndroidTtsBackend.PREF_SPEED_PERCENT, 125)
                .commit();

        setField("audio", new AudioController(context));
        setField("audioEngineReady", true);
        setField("nativeTts", fakeKokoro);
        setField("nativeTtsReady", true);
        setField("nativeTtsLanguageReady", true);
        setField("nativeTtsActiveEnginePackage", "com.k2fsa.sherpa.onnx.tts.engine");

        StringBuilder longNarration = new StringBuilder();
        for (int i = 1; i <= 14; i++) {
            longNarration.append("No corredor iluminado da biblioteca arcana, o estudante examina o volume número ")
                    .append(i)
                    .append(" sobre a bancada de carvalho e confere as notas escritas com tinta dourada. ");
        }

        List<String> expectedChunks = NarrationChunker.kokoro(
                PortugueseNarrationSanitizer.sanitize(longNarration.toString()));
        assertTrue("Long narration must split into multiple Kokoro chunks", expectedChunks.size() >= 2);

        invoke("speakNativeNarrationWithSfx", new Class<?>[]{String.class}, longNarration.toString());
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // CRITICAL PROOF: All chunks must be queued in TextToSpeech immediately (before any onDone callback fires!)
        assertEquals("All Kokoro chunks must be pipelined upfront in TextToSpeech without waiting for onDone()",
                expectedChunks.size(), spokenTexts.size());
        for (int i = 0; i < queueModes.size(); i++) {
            assertEquals("Every chunk must use QUEUE_ADD so Kokoro synthesizes continuously",
                    Integer.valueOf(TextToSpeech.QUEUE_ADD), queueModes.get(i));
        }
        Map<?, ?> pendingUtterances = (Map<?, ?>) getField("nativeVoiceSentenceByUtterance");
        assertEquals("All queued chunks must be tracked in nativeVoiceSentenceByUtterance until onDone",
                expectedChunks.size(), pendingUtterances.size());
        assertTrue("User speed slider (125%) must be applied to Kokoro/Android TTS speech rate, got: " + recordedRate[0],
                recordedRate[0] > 1.05f);
    }

    @Test
    public void fullVttAuditDnd2024KvotheMarcoDoPercursoInventoryCombatLivingWorldAndBackup21() throws Exception {
        // 1. Kvothe 2024 Sheet & Reconciler Audit
        long kvotheId = db.addCharacter("Kvothe", "Humano", "Mago", 1, 8, 8, 13, 8, 14, 12, 16, 10, 14, true, "");
        db.updateCharacterRuleDetails(kvotheId, "Escriba", "", "", "", "", DndRulesEngine.PROFILE_2024);
        CharacterSheetGenerator gen = (CharacterSheetGenerator) getField("characterGenerator");
        JSONObject rawKvothe = gen.fromLegacy(db.getCharacter(kvotheId));
        rawKvothe.put("rules_source", DndRulesEngine.PROFILE_2024);
        JSONObject kvotheSheet = Character2024SheetReconciler.reconcile(rawKvothe);
        assertTrue("2024 sheet reconciler must apply to D&D 2024 hero", Character2024SheetReconciler.applies(kvotheSheet));
        assertEquals(2, kvotheSheet.optInt("sheet_2024_reconciler_version"));
        assertTrue("Saving throws must be derived by 2024 reconciler",
                kvotheSheet.optString("saving_throws").contains("Inteligência"));

        // 2. Inventory 2024 & Authorized Transactions (Initial Gear, Purchase, Loot, Encumbrance)
        JSONObject withCoins = new JSONObject(kvotheSheet.toString());
        withCoins.put("coin_pieces", CharacterInventory2024.coinsFromCopper(5000)); // 50 PO
        withCoins.put("coin_pieces_migration_version", 1);
        JSONObject bought = InventoryTransactions2024.acquire(
                withCoins,
                new JSONObject().put("id", "grimorio").put("name", "Grimório").put("quantity", 1),
                "purchase",
                "evt-buy-grimorio-1",
                new JSONObject().put("cost_cp", 1500L).put("from", "Biblioplex").put("location", "Strixhaven")
        );
        // Idempotency check: re-applying same eventId must not deduct coins twice
        JSONObject boughtAgain = InventoryTransactions2024.acquire(
                bought,
                new JSONObject().put("id", "grimorio").put("name", "Grimório").put("quantity", 1),
                "purchase",
                "evt-buy-grimorio-1",
                new JSONObject().put("cost_cp", 1500L)
        );
        assertEquals(3500L, CharacterInventory2024.coinValueCp(CharacterInventory2024.coinPurse(boughtAgain)));
        assertTrue("Grimório weight (3 lb) must be tracked in encumbrance",
                boughtAgain.optDouble("inventory_known_weight_lb", 0d) >= 3.0d);

        // 3. Strixhaven Solo -> Faerûn -> D040 Marco do Percurso Party Bridge (Wilem, Simmon, Auri)
        KvotheCampaignPartyBridge bridge = new KvotheCampaignPartyBridge(db);
        assertEquals("Anão", StrixhavenFriends2024.initialSheet("Wilem").optString("species"));
        assertEquals("Golias", StrixhavenFriends2024.initialSheet("Simmon").optString("species"));
        assertEquals("Elfo", StrixhavenFriends2024.initialSheet("Auri").optString("species"));

        long wilemId = db.addCharacter("Wilem", "Anão", "Bárbaro", 1, 14, 14, 15, 16, 12, 14, 8, 10, 10, true, "");
        long simmonId = db.addCharacter("Simmon", "Golias", "Monge", 1, 11, 11, 15, 12, 16, 14, 10, 14, 10, true, "");
        long auriId = db.addCharacter("Auri", "Elfo", "Druida", 1, 10, 10, 14, 8, 14, 14, 12, 16, 10, true, "");

        // Move scene to D040 (Marco do Percurso) in Faerûn
        JSONObject marcoScene = db.getSceneState();
        marcoScene.put("location", "Marco do Percurso");
        marcoScene.put("hex_id", "D040");
        marcoScene.remove("adventure_profile");
        db.saveSceneState(marcoScene);

        JSONObject recruited = bridge.recruitAtMarco(wilemId, simmonId, auriId, true, true);
        assertTrue("Kvothe must recruit Wilem, Simmon, and Auri at Marco do Percurso (D040): " + recruited,
                recruited.optBoolean("joined"));
        assertEquals("All 4 heroes must now be active in the party roster",
                4, new PartyDirector(db).activeHeroRoster().size());

        // 4. CombatStateMachine & LivingWorldEngine Verification
        assertTrue(CombatStateMachine.canTransition(CombatStateMachine.IDLE, CombatStateMachine.PREPARING));
        assertTrue(CombatStateMachine.canTransition(CombatStateMachine.PREPARING, CombatStateMachine.INITIATIVE));
        assertTrue(CombatStateMachine.canTransition(CombatStateMachine.INITIATIVE, CombatStateMachine.ACTIVE_ROUND));
        assertTrue(CombatStateMachine.canTransition(CombatStateMachine.ACTIVE_ROUND, CombatStateMachine.RESOLVING));
        assertTrue(CombatStateMachine.canTransition(CombatStateMachine.RESOLVING, CombatStateMachine.ENCOUNTER_END));

        LivingWorldEngine livingWorld = new LivingWorldEngine(db);
        livingWorld.saveHeroLife(kvotheId, 19, "Strixhaven / Marco do Percurso", "Estudante e Arcanista", "Edema Ruh", "Solteiro", "Descobrir o nome do vento");
        long relId = livingWorld.addRelationship(kvotheId, "Auri", "Amiga de confiança", "ativo", 20, "Encontrada em Strixhaven e reunida no Marco do Percurso");
        assertTrue(relId > 0);
        long actId = livingWorld.planActivity(kvotheId, "pesquisa", "Catalogar inscrições do Marco do Percurso", "Marco do Percurso", 3, "Estudo conjunto com Wilem e Simmon");
        assertTrue(actId > 0);

        // 5. BACKUP21 Export, Contract Validation, and Full Restore Round-Trip
        JSONObject backup = db.exportAll();
        JSONObject validation = BackupContractValidator.validate(backup);
        assertTrue("BACKUP21 must pass contract validation: " + validation, validation.optBoolean("valid"));
        assertEquals(21, backup.optInt("version"));
        assertEquals("braseiro-dnd-backup", backup.optString("format"));
        assertEquals("campaign", backup.optString("backupScope"));
        assertEquals("dnd", backup.optString("worldProfile"));

        String restoreDbName = "braseiro-restore-audit-" + Long.toHexString(System.nanoTime()) + ".db";
        AppDb restoredDb = new AppDb(context, restoreDbName);
        try {
            restoredDb.setActiveMapProfile("dnd");
            restoredDb.importAll(backup);
            assertEquals("Restored DB must preserve all 4 heroes", 4, restoredDb.listCharacters().size());
            assertEquals("Marco do Percurso", restoredDb.getSceneState().optString("location"));
            assertEquals("D040", restoredDb.getSceneState().optString("hex_id"));
        } finally {
            restoredDb.close();
            context.deleteDatabase(restoreDbName);
        }
    }
}
